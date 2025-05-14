package ai.kastrax.codebase.search

import ai.kastrax.codebase.embedding.CodeEmbeddingService
import ai.kastrax.codebase.indexing.CodeIndexer
import ai.kastrax.codebase.retrieval.CodeRelevanceRanker
import ai.kastrax.codebase.retrieval.HybridRetriever
import ai.kastrax.codebase.retrieval.KeywordSearcher
import ai.kastrax.codebase.retrieval.KeywordSearchResult
import ai.kastrax.codebase.retrieval.model.RetrievalResult
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Visibility
import ai.kastrax.codebase.vector.CodeVectorStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * 搜索请求
 *
 * @property query 查询字符串
 * @property paths 搜索路径列表
 * @property type 搜索类型
 * @property options 搜索选项
 */
data class SearchRequest(
    val query: String,
    val paths: List<Path>,
    val type: SearchType = SearchType.HYBRID,
    val options: Map<String, Any> = emptyMap()
)

/**
 * 搜索类型
 */
enum class SearchType {
    /**
     * 文本搜索
     */
    TEXT,

    /**
     * 向量搜索
     */
    VECTOR,

    /**
     * 混合搜索
     */
    HYBRID,

    /**
     * 符号搜索
     */
    SYMBOL,

    /**
     * 结构感知搜索
     */
    STRUCTURE
}

/**
 * 搜索响应
 *
 * @property query 查询字符串
 * @property results 搜索结果列表
 * @property metadata 元数据
 */
data class SearchResponse(
    val query: String,
    val results: List<RetrievalResult>,
    val metadata: Map<String, Any> = emptyMap(),
    val highlightResults: List<HighlightResult> = emptyList(),
    val pagedResult: PagedResult<RetrievalResult>? = null,
    val executionTimeMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 搜索门面配置
 *
 * @property defaultLimit 默认限制结果数量
 * @property defaultMinScore 默认最小分数
 * @property enableCaching 是否启用缓存
 * @property maxCacheSize 最大缓存大小
 * @property enableReranking 是否启用重排序
 * @property enableParallelSearch 是否启用并行搜索
 * @property maxParallelSearches 最大并行搜索数量
 */
data class SearchFacadeConfig(
    val defaultLimit: Int = 20,
    val defaultMinScore: Double = 0.5,
    val enableCaching: Boolean = true,
    val maxCacheSize: Int = 100,
    val enableReranking: Boolean = true,
    val enableParallelSearch: Boolean = true,
    val maxParallelSearches: Int = 4,
    val enableHighlighting: Boolean = true,
    val enablePagination: Boolean = true,
    val enableFiltering: Boolean = true,
    val enableHistoryTracking: Boolean = true,
    val enableStructureAwareSearch: Boolean = true
)

/**
 * 处理后的搜索结果
 *
 * @property results 搜索结果列表
 * @property highlightResults 高亮结果列表
 * @property pagedResult 分页结果
 */
data class ProcessedSearchResults(
    val results: List<RetrievalResult>,
    val highlightResults: List<HighlightResult> = emptyList(),
    val pagedResult: PagedResult<RetrievalResult>? = null
)

/**
 * 搜索门面
 *
 * 提供统一的搜索入口
 *
 * @property codeIndexer 代码索引器
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 * @property config 配置
 */
class SearchFacade(
    private val codeIndexer: CodeIndexer,
    private val vectorStore: CodeVectorStore,
    private val embeddingService: CodeEmbeddingService,
    private val config: SearchFacadeConfig = SearchFacadeConfig()
) {
    // 缓存
    private val cache = ConcurrentHashMap<String, SearchResponse>()

    // 搜索器
    private val ripgrepSearcher = RipgrepSearcher()

    // 关键词搜索器
    private val keywordSearcher = KeywordSearcher(codeIndexer)

    // 混合检索器
    private val hybridRetriever = HybridRetriever(
        vectorStore = vectorStore,
        embeddingService = embeddingService,
        keywordSearcher = keywordSearcher
    )

    // 相关性排序器
    private val relevanceRanker = CodeRelevanceRanker()

    // 结构感知搜索器
    private val structureAwareSearcher = if (config.enableStructureAwareSearch) {
        StructureAwareSearcher(codeIndexer)
    } else null

    // 搜索结果高亮器
    private val resultHighlighter = if (config.enableHighlighting) {
        SearchResultHighlighter()
    } else null

    // 搜索结果分页器
    private val resultPaginator = if (config.enablePagination) {
        SearchResultPaginator()
    } else null

    // 搜索结果过滤器
    private val resultFilter = if (config.enableFiltering) {
        SearchResultFilter()
    } else null

    // 搜索历史管理器
    private val historyManager = if (config.enableHistoryTracking) {
        SearchHistoryManager()
    } else null

    /**
     * 搜索
     *
     * @param request 搜索请求
     * @return 搜索响应
     */
    suspend fun search(request: SearchRequest): SearchResponse = withContext(Dispatchers.Default) {
        logger.info { "开始搜索: ${request.query} (类型: ${request.type})" }

        // 检查缓存
        val cacheKey = "${request.query}:${request.type}:${request.paths.joinToString(",")}:${request.options}"
        if (config.enableCaching && cache.containsKey(cacheKey)) {
            logger.debug { "从缓存中获取搜索结果: ${request.query}" }
            return@withContext cache[cacheKey]!!
        }

        val startTime = System.currentTimeMillis()

        // 执行搜索
        val results = when (request.type) {
            SearchType.TEXT -> textSearch(request)
            SearchType.VECTOR -> vectorSearch(request)
            SearchType.HYBRID -> hybridSearch(request)
            SearchType.SYMBOL -> symbolSearch(request)
            SearchType.STRUCTURE -> structureAwareSearch(request)
        }

        val endTime = System.currentTimeMillis()
        val searchTime = endTime - startTime

        // 处理搜索结果
        val processedResults = processSearchResults(request, results)

        // 创建响应
        val response = SearchResponse(
            query = request.query,
            results = processedResults.results,
            metadata = mapOf(
                "searchTime" to searchTime,
                "totalResults" to processedResults.results.size,
                "searchType" to request.type,
                "filters" to (request.options["filters"] ?: emptyMap<String, Any>())
            ),
            highlightResults = processedResults.highlightResults,
            pagedResult = processedResults.pagedResult,
            executionTimeMs = searchTime,
            timestamp = System.currentTimeMillis()
        )

        // 记录搜索历史
        recordSearchHistory(request, processedResults.results.size, searchTime)

        // 缓存结果
        if (config.enableCaching) {
            // 维护缓存大小
            if (cache.size >= config.maxCacheSize) {
                // 简单的缓存淘汰策略：随机移除一个
                val keyToRemove = cache.keys.firstOrNull()
                if (keyToRemove != null) {
                    cache.remove(keyToRemove)
                }
            }
            cache[cacheKey] = response
        }

        return@withContext response
    }

    /**
     * 处理搜索结果
     *
     * @param request 搜索请求
     * @param results 搜索结果列表
     * @return 处理后的搜索结果
     */
    private suspend fun processSearchResults(
        request: SearchRequest,
        results: List<RetrievalResult>
    ): ProcessedSearchResults = withContext(Dispatchers.Default) {
        var processedResults = results
        var highlightResults = emptyList<HighlightResult>()
        var pagedResult: PagedResult<RetrievalResult>? = null

        // 应用过滤
        if (config.enableFiltering && resultFilter != null) {
            val filterCriteria = createFilterCriteria(request)
            processedResults = resultFilter.filterResults(processedResults, filterCriteria)
        }

        // 应用高亮
        if (config.enableHighlighting && resultHighlighter != null) {
            highlightResults = resultHighlighter.highlight(processedResults, request.query)
        }

        // 应用分页
        if (config.enablePagination && resultPaginator != null) {
            val pageNumber = request.options["page"] as? Int ?: 1
            val pageSize = request.options["pageSize"] as? Int ?: config.defaultLimit
            pagedResult = resultPaginator.paginateRetrievalResults(processedResults, pageNumber, pageSize)
            processedResults = pagedResult.items
        }

        return@withContext ProcessedSearchResults(
            results = processedResults,
            highlightResults = highlightResults,
            pagedResult = pagedResult
        )
    }

    /**
     * 创建过滤条件
     *
     * @param request 搜索请求
     * @return 过滤条件
     */
    private fun createFilterCriteria(request: SearchRequest): FilterCriteria {
        val options = request.options

        // 从选项中提取过滤条件
        val types = options["types"] as? Set<CodeElementType>
        val visibilities = options["visibilities"] as? Set<Visibility>
        val includePaths = options["includePaths"] as? List<String>
        val excludePaths = options["excludePaths"] as? List<String>
        val minScore = options["minScore"] as? Double ?: config.defaultMinScore
        val maxScore = options["maxScore"] as? Double
        val includePatterns = options["includePatterns"] as? List<String>
        val excludePatterns = options["excludePatterns"] as? List<String>

        return resultFilter?.createFilterCriteria(
            types = types,
            visibilities = visibilities,
            includePaths = includePaths,
            excludePaths = excludePaths,
            minScore = minScore,
            maxScore = maxScore,
            includePatterns = includePatterns,
            excludePatterns = excludePatterns
        ) ?: FilterCriteria(minScore = minScore)
    }

    /**
     * 记录搜索历史
     *
     * @param request 搜索请求
     * @param resultCount 结果数量
     * @param executionTimeMs 执行时间（毫秒）
     */
    private suspend fun recordSearchHistory(
        request: SearchRequest,
        resultCount: Int,
        executionTimeMs: Long
    ) {
        if (config.enableHistoryTracking && historyManager != null) {
            val filters = request.options.filterKeys { it.startsWith("filter") || it in setOf("types", "visibilities", "includePaths", "excludePaths") }
                .mapValues { it.value.toString() }

            historyManager.addHistory(
                query = request.query,
                resultCount = resultCount,
                searchType = request.type.name,
                filters = filters,
                executionTimeMs = executionTimeMs
            )
        }
    }

    /**
     * 结构感知搜索
     *
     * @param request 搜索请求
     * @return 搜索结果列表
     */
    private suspend fun structureAwareSearch(request: SearchRequest): List<RetrievalResult> = withContext(Dispatchers.Default) {
        if (!config.enableStructureAwareSearch || structureAwareSearcher == null) {
            logger.warn { "结构感知搜索未启用，回退到混合搜索" }
            return@withContext hybridSearch(request)
        }

        logger.info { "执行结构感知搜索: ${request.query}" }

        try {
            val limit = request.options["limit"] as? Int ?: config.defaultLimit
            val minScore = request.options["minScore"] as? Float ?: config.defaultMinScore.toFloat()
            val types = request.options["types"] as? Set<CodeElementType>

            val structureResults = structureAwareSearcher.search(
                query = request.query,
                limit = limit,
                minScore = minScore,
                types = types
            )

            // 转换为检索结果
            val retrievalResults = structureResults.map {
                structureAwareSearcher.convertToRetrievalResult(it)
            }

            // 应用重排序
            val finalResults = if (config.enableReranking) {
                relevanceRanker.rerank(retrievalResults, request.query)
            } else {
                retrievalResults
            }

            return@withContext finalResults
        } catch (e: Exception) {
            logger.error(e) { "结构感知搜索失败: ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 文本搜索
     *
     * @param request 搜索请求
     * @return 搜索结果列表
     */
    private suspend fun textSearch(request: SearchRequest): List<RetrievalResult> = withContext(Dispatchers.Default) {
        logger.info { "执行文本搜索: ${request.query}" }

        try {
            val limit = request.options["limit"] as? Int ?: config.defaultLimit
            val minScore = request.options["minScore"] as? Double ?: config.defaultMinScore

            // 使用 ripgrep 搜索
            val searchResults = mutableListOf<RipgrepSearchResult>()
            if (request.paths.isEmpty()) {
                ripgrepSearcher.search(request.query, emptyList()).collect { result ->
                    searchResults.add(result)
                }
            } else {
                ripgrepSearcher.search(request.query, request.paths).collect { result ->
                    searchResults.add(result)
                }
            }

            // 转换为检索结果
            val retrievalResults = searchResults.map { result ->
                val fileName = result.filePath.substringAfterLast("/").substringAfterLast("\\")
                val element = CodeElement(
                    id = UUID.randomUUID().toString(),
                    name = fileName,
                    qualifiedName = result.filePath,
                    type = CodeElementType.FILE,
                    visibility = Visibility.PUBLIC,
                    location = Location(
                        filePath = result.filePath,
                        startLine = result.lineNumber,
                        endLine = result.lineNumber,
                        startColumn = 0,
                        endColumn = 0
                    ),
                    content = result.lineText,
                    metadata = mutableMapOf(
                        "match" to result.matchText,
                        "lineNumber" to result.lineNumber,
                        "columnNumber" to result.columnNumber
                    )
                )

                RetrievalResult(
                    element = element,
                    score = 1.0, // 文本搜索默认分数
                    explanation = "文本匹配: ${result.matchText}"
                )
            }

            // 限制结果数量
            return@withContext retrievalResults.take(limit)
        } catch (e: Exception) {
            logger.error(e) { "文本搜索失败: ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 向量搜索
     *
     * @param request 搜索请求
     * @return 搜索结果列表
     */
    private suspend fun vectorSearch(request: SearchRequest): List<RetrievalResult> = withContext(Dispatchers.Default) {
        logger.info { "执行向量搜索: ${request.query}" }

        try {
            val limit = request.options["limit"] as? Int ?: config.defaultLimit
            val minScore = request.options["minScore"] as? Double ?: config.defaultMinScore

            // 生成查询嵌入
            val queryEmbedding = embeddingService.embed(request.query)

            // 搜索向量存储
            val searchResults = vectorStore.search(
                embedding = queryEmbedding,
                limit = limit,
                minScore = minScore
            )

            // 应用重排序
            val finalResults = if (config.enableReranking) {
                relevanceRanker.rerank(searchResults, request.query)
            } else {
                searchResults
            }

            return@withContext finalResults
        } catch (e: Exception) {
            logger.error(e) { "向量搜索失败: ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 混合搜索
     *
     * @param request 搜索请求
     * @return 搜索结果列表
     */
    private suspend fun hybridSearch(request: SearchRequest): List<RetrievalResult> = withContext(Dispatchers.Default) {
        logger.info { "执行混合搜索: ${request.query}" }

        try {
            val limit = request.options["limit"] as? Int ?: config.defaultLimit
            val minScore = request.options["minScore"] as? Double ?: config.defaultMinScore
            val vectorWeight = request.options["vectorWeight"] as? Double ?: 0.7
            val keywordWeight = request.options["keywordWeight"] as? Double ?: 0.3

            // 使用混合检索器
            val searchResults = hybridRetriever.search(
                query = request.query,
                limit = limit,
                minScore = minScore,
                vectorWeight = vectorWeight,
                keywordWeight = keywordWeight
            )

            // 应用重排序
            val finalResults = if (config.enableReranking) {
                relevanceRanker.rerank(searchResults, request.query)
            } else {
                searchResults
            }

            return@withContext finalResults
        } catch (e: Exception) {
            logger.error(e) { "混合搜索失败: ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 符号搜索
     *
     * @param request 搜索请求
     * @return 搜索结果列表
     */
    private suspend fun symbolSearch(request: SearchRequest): List<RetrievalResult> = withContext(Dispatchers.Default) {
        logger.info { "执行符号搜索: ${request.query}" }

        try {
            val limit = request.options["limit"] as? Int ?: config.defaultLimit
            val minScore = request.options["minScore"] as? Double ?: config.defaultMinScore
            val exactMatch = request.options["exactMatch"] as? Boolean ?: false

            // 使用关键词搜索器
            val searchResults = keywordSearcher.search(
                query = request.query,
                limit = limit,
                minScore = minScore,
                exactMatch = exactMatch
            )

            // 转换为检索结果
            val retrievalResults = searchResults.map { result ->
                RetrievalResult(
                    element = result.element,
                    score = result.score.toDouble(),
                    explanation = "符号匹配: ${result.element.name}"
                )
            }

            // 应用重排序
            val finalResults = if (config.enableReranking) {
                relevanceRanker.rerank(retrievalResults, request.query)
            } else {
                retrievalResults
            }

            return@withContext finalResults
        } catch (e: Exception) {
            logger.error(e) { "符号搜索失败: ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 按文件路径搜索
     *
     * @param filePath 文件路径
     * @return 元素列表
     */
    suspend fun searchByFilePath(filePath: Path): List<CodeElement> = withContext(Dispatchers.Default) {
        logger.info { "按文件路径搜索: $filePath" }

        try {
            val elements = codeIndexer.getElementsByFilePath(filePath)
            return@withContext elements.toList()
        } catch (e: Exception) {
            logger.error(e) { "按文件路径搜索失败: ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 按元素类型搜索
     *
     * @param type 元素类型
     * @param limit 限制结果数量
     * @return 元素列表
     */
    suspend fun searchByType(type: CodeElementType, limit: Int = config.defaultLimit): List<CodeElement> = withContext(Dispatchers.Default) {
        logger.info { "按元素类型搜索: $type" }

        try {
            val elements = codeIndexer.getElementsByType(type)
            return@withContext elements.toList().take(limit)
        } catch (e: Exception) {
            logger.error(e) { "按元素类型搜索失败: ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 按元素名称搜索
     *
     * @param name 元素名称
     * @param exactMatch 是否精确匹配
     * @param limit 限制结果数量
     * @return 元素列表
     */
    suspend fun searchByName(name: String, exactMatch: Boolean = false, limit: Int = config.defaultLimit): List<CodeElement> = withContext(Dispatchers.Default) {
        logger.info { "按元素名称搜索: $name (精确匹配: $exactMatch)" }

        try {
            val allElements = codeIndexer.getAllElements()

            val matchedElements = if (exactMatch) {
                allElements.filter { it.name == name }
            } else {
                allElements.filter { it.name.contains(name, ignoreCase = true) }
            }

            return@withContext matchedElements.take(limit)
        } catch (e: Exception) {
            logger.error(e) { "按元素名称搜索失败: ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 获取搜索历史
     *
     * @param limit 限制数量
     * @return 历史记录列表
     */
    suspend fun getSearchHistory(limit: Int = 20): List<SearchHistoryEntry> {
        return historyManager?.getHistory(limit) ?: emptyList()
    }

    /**
     * 搜索历史记录
     *
     * @param query 查询
     * @param limit 限制数量
     * @return 历史记录列表
     */
    suspend fun searchHistory(query: String, limit: Int = 20): List<SearchHistoryEntry> {
        return historyManager?.searchHistory(query, limit) ?: emptyList()
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        cache.clear()
        structureAwareSearcher?.clearCache()
        logger.info { "搜索缓存已清除" }
    }

    /**
     * 清除历史记录
     */
    suspend fun clearHistory() {
        historyManager?.clearHistory()
        logger.info { "搜索历史记录已清除" }
    }

    /**
     * 流式搜索
     *
     * @param request 搜索请求
     * @return 结果流
     */
    fun streamSearch(request: SearchRequest): Flow<RetrievalResult> = flow {
        logger.info { "开始流式搜索: ${request.query} (类型: ${request.type})" }

        val results = when (request.type) {
            SearchType.TEXT -> textSearch(request)
            SearchType.VECTOR -> vectorSearch(request)
            SearchType.HYBRID -> hybridSearch(request)
            SearchType.SYMBOL -> symbolSearch(request)
            SearchType.STRUCTURE -> structureAwareSearch(request)
        }

        for (result in results) {
            emit(result)
        }
    }
}
