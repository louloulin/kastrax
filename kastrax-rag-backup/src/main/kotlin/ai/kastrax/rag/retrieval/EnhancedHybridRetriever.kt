package ai.kastrax.rag.retrieval

import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.vectorstore.RagVectorStore
import ai.kastrax.rag.vectorstore.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private val logger = KotlinLogging.logger {}

/**
 * 混合策略，定义如何组合不同的检索结果。
 */
enum class HybridStrategy {
    /**
     * 加权组合，根据权重组合不同的检索结果。
     */
    WEIGHTED,
    
    /**
     * 交错组合，从每个检索结果中依次选择一个文档。
     */
    INTERLEAVE,
    
    /**
     * 分层组合，先使用一种检索方法，如果结果不足，再使用另一种方法。
     */
    HIERARCHICAL,
    
    /**
     * 并集组合，取所有检索结果的并集。
     */
    UNION,
    
    /**
     * 交集组合，取所有检索结果的交集。
     */
    INTERSECTION
}

/**
 * 增强混合检索器配置。
 *
 * @property vectorWeight 向量搜索的权重，默认为 0.7
 * @property keywordWeight 关键词搜索的权重，默认为 0.3
 * @property metadataWeight 元数据搜索的权重，默认为 0.0
 * @property expandLimit 扩展检索限制的因子，默认为 2.0
 * @property minKeywordMatches 最小关键词匹配数，默认为 1
 * @property maxKeywords 最大关键词数，默认为 5
 * @property keywordBoostFactor 关键词提升因子，默认为 1.2
 * @property hybridStrategy 混合策略，默认为加权组合
 * @property metadataFilters 元数据过滤器，默认为空
 * @property useSemanticSearch 是否使用语义搜索，默认为 true
 * @property useKeywordSearch 是否使用关键词搜索，默认为 true
 * @property useMetadataSearch 是否使用元数据搜索，默认为 false
 */
data class EnhancedHybridRetrieverConfig(
    val vectorWeight: Double = 0.7,
    val keywordWeight: Double = 0.3,
    val metadataWeight: Double = 0.0,
    val expandLimit: Double = 2.0,
    val minKeywordMatches: Int = 1,
    val maxKeywords: Int = 5,
    val keywordBoostFactor: Double = 1.2,
    val hybridStrategy: HybridStrategy = HybridStrategy.WEIGHTED,
    val metadataFilters: Map<String, Any> = emptyMap(),
    val useSemanticSearch: Boolean = true,
    val useKeywordSearch: Boolean = true,
    val useMetadataSearch: Boolean = false
) {
    init {
        require(vectorWeight >= 0) { "Vector weight must be non-negative" }
        require(keywordWeight >= 0) { "Keyword weight must be non-negative" }
        require(metadataWeight >= 0) { "Metadata weight must be non-negative" }
        require(vectorWeight + keywordWeight + metadataWeight > 0) { "At least one weight must be positive" }
        require(expandLimit >= 1.0) { "Expand limit must be at least 1.0" }
    }
}

/**
 * 增强混合检索器，结合向量相似度搜索、关键词搜索和元数据搜索。
 *
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 * @property keywordExtractor 关键词提取器
 * @property config 增强混合检索器配置
 */
class EnhancedHybridRetriever(
    private val vectorStore: RagVectorStore,
    private val embeddingService: EmbeddingService,
    private val keywordExtractor: KeywordExtractor,
    private val config: EnhancedHybridRetrieverConfig = EnhancedHybridRetrieverConfig()
) : Retriever {

    /**
     * 使用查询文本检索文档。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 检索结果列表，按组合分数降序排序
     */
    override suspend fun retrieve(query: String, limit: Int, minScore: Double): List<SearchResult> {
        logger.debug { "Retrieving documents for query: $query, limit: $limit, minScore: $minScore" }

        try {
            // 计算扩展的检索限制
            val expandedLimit = (limit * config.expandLimit).toInt().coerceAtLeast(limit)

            // 并行执行不同的检索方法
            val results = coroutineScope {
                val tasks = mutableListOf<kotlinx.coroutines.Deferred<List<SearchResult>>>()

                // 向量相似度搜索
                if (config.useSemanticSearch) {
                    tasks.add(async {
                        vectorStore.similaritySearch(query, embeddingService, expandedLimit, minScore)
                    })
                }

                // 关键词搜索
                if (config.useKeywordSearch) {
                    tasks.add(async {
                        val keywords = keywordExtractor.extractKeywords(query, config.maxKeywords)
                        if (keywords.isNotEmpty()) {
                            vectorStore.keywordSearch(keywords, expandedLimit)
                        } else {
                            emptyList()
                        }
                    })
                }

                // 元数据搜索
                if (config.useMetadataSearch && config.metadataFilters.isNotEmpty()) {
                    tasks.add(async {
                        vectorStore.metadataSearch(config.metadataFilters, expandedLimit)
                    })
                }

                // 等待所有任务完成
                tasks.awaitAll()
            }

            // 根据混合策略组合结果
            val combinedResults = when (config.hybridStrategy) {
                HybridStrategy.WEIGHTED -> combineResultsWeighted(results, limit)
                HybridStrategy.INTERLEAVE -> combineResultsInterleave(results, limit)
                HybridStrategy.HIERARCHICAL -> combineResultsHierarchical(results, limit)
                HybridStrategy.UNION -> combineResultsUnion(results, limit)
                HybridStrategy.INTERSECTION -> combineResultsIntersection(results, limit)
            }

            return combinedResults
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving documents for query: $query" }
            return emptyList()
        }
    }

    /**
     * 使用加权组合策略组合结果。
     *
     * @param resultsList 检索结果列表的列表
     * @param limit 返回结果的最大数量
     * @return 组合后的检索结果列表
     */
    private fun combineResultsWeighted(resultsList: List<List<SearchResult>>, limit: Int): List<SearchResult> {
        if (resultsList.isEmpty() || resultsList.all { it.isEmpty() }) {
            return emptyList()
        }

        // 创建文档 ID 到结果的映射
        val resultMap = mutableMapOf<String, CombinedResult>()

        // 处理向量搜索结果
        if (config.useSemanticSearch && resultsList.size > 0 && resultsList[0].isNotEmpty()) {
            for (result in resultsList[0]) {
                val docId = result.document.id
                resultMap[docId] = CombinedResult(
                    result = result,
                    vectorScore = result.score,
                    keywordScore = 0.0,
                    metadataScore = 0.0
                )
            }
        }

        // 处理关键词搜索结果
        if (config.useKeywordSearch && resultsList.size > 1 && resultsList[1].isNotEmpty()) {
            for (result in resultsList[1]) {
                val docId = result.document.id
                val existing = resultMap[docId]

                if (existing != null) {
                    // 更新现有结果的关键词分数
                    resultMap[docId] = existing.copy(keywordScore = result.score)
                } else {
                    // 添加新结果
                    resultMap[docId] = CombinedResult(
                        result = result,
                        vectorScore = 0.0,
                        keywordScore = result.score,
                        metadataScore = 0.0
                    )
                }
            }
        }

        // 处理元数据搜索结果
        if (config.useMetadataSearch && resultsList.size > 2 && resultsList[2].isNotEmpty()) {
            for (result in resultsList[2]) {
                val docId = result.document.id
                val existing = resultMap[docId]

                if (existing != null) {
                    // 更新现有结果的元数据分数
                    resultMap[docId] = existing.copy(metadataScore = result.score)
                } else {
                    // 添加新结果
                    resultMap[docId] = CombinedResult(
                        result = result,
                        vectorScore = 0.0,
                        keywordScore = 0.0,
                        metadataScore = result.score
                    )
                }
            }
        }

        // 计算组合分数
        val totalWeight = config.vectorWeight + config.keywordWeight + config.metadataWeight
        val combinedResults = resultMap.values.map { combined ->
            val combinedScore = (combined.vectorScore * config.vectorWeight +
                combined.keywordScore * config.keywordWeight +
                combined.metadataScore * config.metadataWeight) / totalWeight

            SearchResult(combined.result.document, combinedScore)
        }

        // 按组合分数降序排序并限制结果数量
        return combinedResults.sortedByDescending { it.score }.take(limit)
    }

    /**
     * 使用交错组合策略组合结果。
     *
     * @param resultsList 检索结果列表的列表
     * @param limit 返回结果的最大数量
     * @return 组合后的检索结果列表
     */
    private fun combineResultsInterleave(resultsList: List<List<SearchResult>>, limit: Int): List<SearchResult> {
        if (resultsList.isEmpty() || resultsList.all { it.isEmpty() }) {
            return emptyList()
        }

        val combined = mutableListOf<SearchResult>()
        val seenDocIds = mutableSetOf<String>()
        var index = 0

        // 交错合并结果
        while (combined.size < limit) {
            var addedAny = false

            for (results in resultsList) {
                if (index < results.size) {
                    val result = results[index]
                    val docId = result.document.id

                    if (docId !in seenDocIds) {
                        combined.add(result)
                        seenDocIds.add(docId)
                        addedAny = true

                        if (combined.size >= limit) {
                            break
                        }
                    }
                }
            }

            if (!addedAny) {
                break
            }

            index++
        }

        return combined
    }

    /**
     * 使用分层组合策略组合结果。
     *
     * @param resultsList 检索结果列表的列表
     * @param limit 返回结果的最大数量
     * @return 组合后的检索结果列表
     */
    private fun combineResultsHierarchical(resultsList: List<List<SearchResult>>, limit: Int): List<SearchResult> {
        if (resultsList.isEmpty() || resultsList.all { it.isEmpty() }) {
            return emptyList()
        }

        val combined = mutableListOf<SearchResult>()
        val seenDocIds = mutableSetOf<String>()

        // 按优先级顺序处理结果
        for (results in resultsList) {
            for (result in results) {
                val docId = result.document.id

                if (docId !in seenDocIds) {
                    combined.add(result)
                    seenDocIds.add(docId)

                    if (combined.size >= limit) {
                        return combined
                    }
                }
            }
        }

        return combined
    }

    /**
     * 使用并集组合策略组合结果。
     *
     * @param resultsList 检索结果列表的列表
     * @param limit 返回结果的最大数量
     * @return 组合后的检索结果列表
     */
    private fun combineResultsUnion(resultsList: List<List<SearchResult>>, limit: Int): List<SearchResult> {
        if (resultsList.isEmpty() || resultsList.all { it.isEmpty() }) {
            return emptyList()
        }

        // 创建文档 ID 到结果的映射
        val resultMap = mutableMapOf<String, SearchResult>()

        // 处理所有结果
        for (results in resultsList) {
            for (result in results) {
                val docId = result.document.id
                val existing = resultMap[docId]

                // 保留分数较高的结果
                if (existing == null || result.score > existing.score) {
                    resultMap[docId] = result
                }
            }
        }

        // 按分数降序排序并限制结果数量
        return resultMap.values.sortedByDescending { it.score }.take(limit)
    }

    /**
     * 使用交集组合策略组合结果。
     *
     * @param resultsList 检索结果列表的列表
     * @param limit 返回结果的最大数量
     * @return 组合后的检索结果列表
     */
    private fun combineResultsIntersection(resultsList: List<List<SearchResult>>, limit: Int): List<SearchResult> {
        if (resultsList.isEmpty() || resultsList.any { it.isEmpty() }) {
            return emptyList()
        }

        // 获取所有结果集中的文档 ID
        val docIdSets = resultsList.map { results ->
            results.map { it.document.id }.toSet()
        }

        // 计算交集
        val intersectionDocIds = docIdSets.reduce { acc, set -> acc.intersect(set) }

        // 创建文档 ID 到结果的映射
        val resultMap = mutableMapOf<String, MutableList<SearchResult>>()

        // 收集所有结果
        for (results in resultsList) {
            for (result in results) {
                val docId = result.document.id
                if (docId in intersectionDocIds) {
                    resultMap.getOrPut(docId) { mutableListOf() }.add(result)
                }
            }
        }

        // 计算每个文档的平均分数
        val combinedResults = resultMap.map { (docId, results) ->
            val avgScore = results.map { it.score }.average()
            SearchResult(results.first().document, avgScore)
        }

        // 按分数降序排序并限制结果数量
        return combinedResults.sortedByDescending { it.score }.take(limit)
    }

    /**
     * 组合结果，包含向量分数、关键词分数和元数据分数。
     *
     * @property result 原始搜索结果
     * @property vectorScore 向量相似度分数
     * @property keywordScore 关键词匹配分数
     * @property metadataScore 元数据匹配分数
     */
    private data class CombinedResult(
        val result: SearchResult,
        val vectorScore: Double,
        val keywordScore: Double,
        val metadataScore: Double
    )
}
