package ai.kastrax.codebase.search

import ai.kastrax.codebase.embedding.CodeEmbeddingService
import ai.kastrax.codebase.indexing.CodeIndexer
import ai.kastrax.codebase.retrieval.CodeRelevanceRanker
import ai.kastrax.codebase.retrieval.HybridRetriever
import ai.kastrax.codebase.retrieval.KeywordSearcher
import ai.kastrax.codebase.retrieval.model.RetrievalResult
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
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
    SYMBOL
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
    val metadata: Map<String, Any> = emptyMap()
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
    val maxParallelSearches: Int = 4
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
        }

        val endTime = System.currentTimeMillis()
        val searchTime = endTime - startTime

        // 创建响应
        val response = SearchResponse(
            query = request.query,
            results = results,
            metadata = mapOf(
                "searchTime" to searchTime,
                "totalResults" to results.size,
                "searchType" to request.type
            )
        )

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

        logger.info { "搜索完成: ${request.query}, 找到 ${results.size} 个结果, 耗时 ${searchTime}ms" }
        return@withContext response
    }

    /**
     * 文本搜索
     *
     * @param request 搜索请求
     * @return 搜索结果列表
     */
    private suspend fun textSearch(request: SearchRequest): List<RetrievalResult> = withContext(Dispatchers.Default) {
        logger.debug { "执行文本搜索: ${request.query}" }

        try {
            // 获取选项
            val limit = request.options["limit"] as? Int ?: config.defaultLimit
            val minScore = request.options["minScore"] as? Double ?: config.defaultMinScore

            // 执行 ripgrep 搜索
            val results = mutableListOf<RetrievalResult>()

            ripgrepSearcher.search(request.query, request.paths, request.options).collect { result ->
                val retrievalResult = ripgrepSearcher.convertToRetrievalResult(result)
                results.add(retrievalResult)

                // 限制结果数量
                if (results.size >= limit) {
                    return@collect
                }
            }

            // 重排序
            val rerankedResults = if (config.enableReranking) {
                relevanceRanker.rankResults(results, request.query)
            } else {
                results
            }

            return@withContext rerankedResults
                .filter { it.score >= minScore }
                .take(limit)
        } catch (e: Exception) {
            logger.error(e) { "文本搜索时发生错误: ${e.message}" }
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
        logger.debug { "执行向量搜索: ${request.query}" }

        try {
            // 获取选项
            val limit = request.options["limit"] as? Int ?: config.defaultLimit
            val minScore = request.options["minScore"] as? Double ?: config.defaultMinScore

            // 将查询转换为向量
            val queryVector = embeddingService.embed(request.query).toList()

            // 执行相似度搜索
            val searchResults = vectorStore.similaritySearch(
                vector = queryVector,
                limit = limit,
                minScore = minScore.toFloat()
            )

            // 转换为检索结果
            val results = searchResults.map { result ->
                RetrievalResult(
                    element = result.element,
                    score = result.score,
                    explanation = "向量搜索结果，相似度: ${result.score}"
                )
            }

            return@withContext results
        } catch (e: Exception) {
            logger.error(e) { "向量搜索时发生错误: ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 混合搜索
     *
     * @param request 搜索请求
     * @return 搜索结果列表
     */
    private suspend fun hybridSearch(request: SearchRequest): List<RetrievalResult> = coroutineScope {
        logger.debug { "执行混合搜索: ${request.query}" }

        try {
            // 获取选项
            val limit = request.options["limit"] as? Int ?: config.defaultLimit
            val minScore = request.options["minScore"] as? Double ?: config.defaultMinScore

            // 并行执行文本搜索和向量搜索
            val textResults = if (config.enableParallelSearch) {
                async { textSearch(request.copy(type = SearchType.TEXT)) }
            } else {
                null
            }

            // 执行混合检索
            val hybridResults = hybridRetriever.retrieve(
                query = request.query,
                limit = limit,
                minScore = minScore
            )

            // 合并结果
            val combinedResults = mutableListOf<RetrievalResult>()
            combinedResults.addAll(hybridResults)

            // 添加文本搜索结果（如果有）
            if (textResults != null) {
                val textSearchResults = textResults.await()

                // 去重
                val existingIds = combinedResults.map { it.element.id }.toSet()
                val newTextResults = textSearchResults.filter { it.element.id !in existingIds }

                combinedResults.addAll(newTextResults)
            }

            // 重排序
            val rerankedResults = if (config.enableReranking) {
                relevanceRanker.rankResults(combinedResults, request.query)
            } else {
                combinedResults.sortedByDescending { it.score }
            }

            return@coroutineScope rerankedResults
                .filter { it.score >= minScore }
                .take(limit)
        } catch (e: Exception) {
            logger.error(e) { "混合搜索时发生错误: ${e.message}" }
            return@coroutineScope emptyList()
        }
    }

    /**
     * 符号搜索
     *
     * @param request 搜索请求
     * @return 搜索结果列表
     */
    private suspend fun symbolSearch(request: SearchRequest): List<RetrievalResult> = withContext(Dispatchers.Default) {
        logger.debug { "执行符号搜索: ${request.query}" }

        try {
            // 获取选项
            val limit = request.options["limit"] as? Int ?: config.defaultLimit
            val minScore = request.options["minScore"] as? Double ?: config.defaultMinScore
            val types = request.options["types"] as? Set<CodeElementType>

            // 获取所有代码元素
            val allElements = codeIndexer.getAllElements()

            // 过滤符号类型
            val filteredElements = if (types != null && types.isNotEmpty()) {
                allElements.filter { it.type in types }
            } else {
                allElements
            }

            // 执行关键词搜索
            val searchResults = keywordSearcher.search(
                query = request.query,
                limit = limit,
                minScore = minScore
            )

            // 转换为检索结果
            val results = searchResults.map { result ->
                RetrievalResult(
                    element = result.element,
                    score = result.score.toDouble(),
                    explanation = "符号搜索结果，相关度: ${result.score}"
                )
            }

            return@withContext results
        } catch (e: Exception) {
            logger.error(e) { "符号搜索时发生错误: ${e.message}" }
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
        logger.debug { "按文件路径搜索: $filePath" }

        try {
            // 获取所有元素
            val allElements = codeIndexer.getAllElements()

            // 过滤指定文件路径的元素
            return@withContext allElements.filter { it.location.filePath.equals(filePath.toString()) }.toList()
        } catch (e: Exception) {
            logger.error(e) { "按文件路径搜索时发生错误: ${e.message}" }
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
        logger.debug { "按元素类型搜索: $type" }

        try {
            return@withContext codeIndexer.getElementsByType(type.name).toList().take(limit)
        } catch (e: Exception) {
            logger.error(e) { "按元素类型搜索时发生错误: ${e.message}" }
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
        logger.debug { "按元素名称搜索: $name (精确匹配: $exactMatch)" }

        try {
            val allElements = codeIndexer.getAllElements()

            val results = if (exactMatch) {
                allElements.filter { it.name == name }
            } else {
                allElements.filter { it.name.contains(name, ignoreCase = true) }
            }

            return@withContext results.take(limit)
        } catch (e: Exception) {
            logger.error(e) { "按元素名称搜索时发生错误: ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        cache.clear()
        logger.info { "搜索门面缓存已清除" }
    }

    /**
     * 流式搜索
     *
     * @param request 搜索请求
     * @return 搜索结果流
     */
    fun streamSearch(request: SearchRequest): Flow<RetrievalResult> = flow {
        logger.info { "开始流式搜索: ${request.query} (类型: ${request.type})" }

        try {
            when (request.type) {
                SearchType.TEXT -> {
                    // 执行 ripgrep 搜索
                    ripgrepSearcher.search(request.query, request.paths, request.options).collect { result ->
                        val retrievalResult = ripgrepSearcher.convertToRetrievalResult(result)
                        emit(retrievalResult)
                    }
                }
                else -> {
                    // 对于其他类型，执行普通搜索并发送结果
                    val results = search(request).results
                    results.forEach { emit(it) }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "流式搜索时发生错误: ${e.message}" }
        }
    }
}
