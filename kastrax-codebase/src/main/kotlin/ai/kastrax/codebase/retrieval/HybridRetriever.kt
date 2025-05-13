package ai.kastrax.codebase.retrieval

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.vector.CodeSearchResult
import ai.kastrax.codebase.vector.CodeVectorStore
import ai.kastrax.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 混合检索器配置
 *
 * @property enableVectorSearch 是否启用向量搜索
 * @property enableKeywordSearch 是否启用关键词搜索
 * @property enableHybridSearch 是否启用混合搜索
 * @property vectorWeight 向量搜索权重
 * @property keywordWeight 关键词搜索权重
 * @property defaultLimit 默认限制数量
 * @property defaultMinScore 默认最小分数
 * @property enableReranking 是否启用重排序
 * @property rerankingMethod 重排序方法
 * @property enableCaching 是否启用缓存
 * @property maxCacheSize 最大缓存大小
 */
data class HybridRetrieverConfig(
    val enableVectorSearch: Boolean = true,
    val enableKeywordSearch: Boolean = true,
    val enableHybridSearch: Boolean = true,
    val vectorWeight: Float = 0.7f,
    val keywordWeight: Float = 0.3f,
    val defaultLimit: Int = 20,
    val defaultMinScore: Float = 0.5f,
    val enableReranking: Boolean = true,
    val rerankingMethod: RerankingMethod = RerankingMethod.COMBINED_SCORE,
    val enableCaching: Boolean = true,
    val maxCacheSize: Int = 100
)

/**
 * 重排序方法
 */
enum class RerankingMethod {
    VECTOR_SCORE,
    KEYWORD_SCORE,
    COMBINED_SCORE,
    RECIPROCAL_RANK_FUSION
}

/**
 * 混合检索器
 *
 * 结合向量搜索和关键词搜索的混合检索器
 *
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 * @property keywordSearcher 关键词搜索器
 * @property config 配置
 */
class HybridRetriever(
    private val vectorStore: CodeVectorStore,
    private val embeddingService: EmbeddingService,
    private val keywordSearcher: KeywordSearcher,
    private val config: HybridRetrieverConfig = HybridRetrieverConfig()
) {
    // 缓存
    private val cache = ConcurrentHashMap<String, List<RetrievalResult>>()

    /**
     * 检索
     *
     * @param query 查询
     * @param limit 限制数量
     * @param minScore 最小分数
     * @return 检索结果列表
     */
    suspend fun retrieve(
        query: String,
        limit: Int = config.defaultLimit,
        minScore: Float = config.defaultMinScore
    ): List<RetrievalResult> = withContext(Dispatchers.Default) {
        logger.info { "开始混合检索: $query" }

        // 检查缓存
        val cacheKey = "$query:$limit:$minScore"
        if (config.enableCaching && cache.containsKey(cacheKey)) {
            logger.debug { "从缓存中获取检索结果: $query" }
            return@withContext cache[cacheKey]!!
        }

        // 根据配置选择检索方法
        val results = when {
            config.enableHybridSearch -> hybridSearch(query, limit, minScore)
            config.enableVectorSearch -> vectorSearch(query, limit, minScore)
            config.enableKeywordSearch -> keywordSearch(query, limit, minScore)
            else -> {
                logger.warn { "未启用任何检索方法，使用向量搜索作为默认方法" }
                vectorSearch(query, limit, minScore)
            }
        }

        // 重排序
        val rerankedResults = if (config.enableReranking) {
            rerank(results, query, config.rerankingMethod)
        } else {
            results
        }

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
            cache[cacheKey] = rerankedResults
        }

        logger.info { "混合检索完成: $query, 找到 ${rerankedResults.size} 个结果" }
        return@withContext rerankedResults
    }

    /**
     * 向量搜索
     *
     * @param query 查询
     * @param limit 限制数量
     * @param minScore 最小分数
     * @return 检索结果列表
     */
    private suspend fun vectorSearch(
        query: String,
        limit: Int,
        minScore: Float
    ): List<RetrievalResult> = withContext(Dispatchers.Default) {
        logger.debug { "执行向量搜索: $query" }

        try {
            // 将查询转换为向量
            val queryVector = embeddingService.embed(query)

            // 执行相似度搜索
            val searchResults = vectorStore.similaritySearch(
                vector = queryVector,
                limit = limit,
                minScore = minScore
            )

            // 转换为检索结果
            return@withContext searchResults.map { result ->
                RetrievalResult(
                    element = result.element,
                    vectorScore = result.score,
                    keywordScore = 0f,
                    combinedScore = result.score,
                    source = RetrievalSource.VECTOR
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "向量搜索时发生错误: ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 关键词搜索
     *
     * @param query 查询
     * @param limit 限制数量
     * @param minScore 最小分数
     * @return 检索结果列表
     */
    private suspend fun keywordSearch(
        query: String,
        limit: Int,
        minScore: Float
    ): List<RetrievalResult> = withContext(Dispatchers.Default) {
        logger.debug { "执行关键词搜索: $query" }

        try {
            // 执行关键词搜索
            val searchResults = keywordSearcher.search(
                query = query,
                limit = limit,
                minScore = minScore
            )

            // 转换为检索结果
            return@withContext searchResults.map { result ->
                RetrievalResult(
                    element = result.element,
                    vectorScore = 0f,
                    keywordScore = result.score,
                    combinedScore = result.score,
                    source = RetrievalSource.KEYWORD
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "关键词搜索时发生错误: ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 混合搜索
     *
     * @param query 查询
     * @param limit 限制数量
     * @param minScore 最小分数
     * @return 检索结果列表
     */
    private suspend fun hybridSearch(
        query: String,
        limit: Int,
        minScore: Float
    ): List<RetrievalResult> = coroutineScope {
        logger.debug { "执行混合搜索: $query" }

        try {
            // 并行执行向量搜索和关键词搜索
            val vectorResults = async {
                if (config.enableVectorSearch) {
                    vectorSearch(query, limit * 2, minScore / 2)
                } else {
                    emptyList()
                }
            }

            val keywordResults = async {
                if (config.enableKeywordSearch) {
                    keywordSearch(query, limit * 2, minScore / 2)
                } else {
                    emptyList()
                }
            }

            // 等待结果
            val vectorSearchResults = vectorResults.await()
            val keywordSearchResults = keywordResults.await()

            // 合并结果
            val combinedResults = mutableMapOf<String, RetrievalResult>()

            // 添加向量搜索结果
            vectorSearchResults.forEach { result ->
                combinedResults[result.element.id] = result
            }

            // 添加或合并关键词搜索结果
            keywordSearchResults.forEach { result ->
                val existingResult = combinedResults[result.element.id]
                if (existingResult != null) {
                    // 合并分数
                    val vectorScore = existingResult.vectorScore
                    val keywordScore = result.keywordScore
                    val combinedScore = combineScores(vectorScore, keywordScore)

                    combinedResults[result.element.id] = existingResult.copy(
                        keywordScore = keywordScore,
                        combinedScore = combinedScore,
                        source = RetrievalSource.HYBRID
                    )
                } else {
                    combinedResults[result.element.id] = result
                }
            }

            // 按组合分数排序并限制结果数量
            return@coroutineScope combinedResults.values
                .sortedByDescending { it.combinedScore }
                .filter { it.combinedScore >= minScore }
                .take(limit)
        } catch (e: Exception) {
            logger.error(e) { "混合搜索时发生错误: ${e.message}" }
            return@coroutineScope emptyList()
        }
    }

    /**
     * 合并分数
     *
     * @param vectorScore 向量分数
     * @param keywordScore 关键词分数
     * @return 组合分数
     */
    private fun combineScores(vectorScore: Float, keywordScore: Float): Float {
        return vectorScore * config.vectorWeight + keywordScore * config.keywordWeight
    }

    /**
     * 重排序
     *
     * @param results 检索结果列表
     * @param query 查询
     * @param method 重排序方法
     * @return 重排序后的检索结果列表
     */
    private fun rerank(
        results: List<RetrievalResult>,
        query: String,
        method: RerankingMethod
    ): List<RetrievalResult> {
        logger.debug { "使用 $method 方法重排序结果" }

        return when (method) {
            RerankingMethod.VECTOR_SCORE -> {
                results.sortedByDescending { it.vectorScore }
            }
            RerankingMethod.KEYWORD_SCORE -> {
                results.sortedByDescending { it.keywordScore }
            }
            RerankingMethod.COMBINED_SCORE -> {
                results.sortedByDescending { it.combinedScore }
            }
            RerankingMethod.RECIPROCAL_RANK_FUSION -> {
                // 倒数排名融合
                val elementToRanks = mutableMapOf<String, MutableList<Int>>()

                // 收集向量排名
                results.sortedByDescending { it.vectorScore }
                    .forEachIndexed { index, result ->
                        elementToRanks.getOrPut(result.element.id) { mutableListOf() }
                            .add(index + 1) // 排名从1开始
                    }

                // 收集关键词排名
                results.sortedByDescending { it.keywordScore }
                    .forEachIndexed { index, result ->
                        elementToRanks.getOrPut(result.element.id) { mutableListOf() }
                            .add(index + 1) // 排名从1开始
                    }

                // 计算RRF分数
                val k = 60 // 常数，用于减少排名差异的影响
                val elementToRrfScore = mutableMapOf<String, Float>()
                elementToRanks.forEach { (elementId, ranks) ->
                    val rrfScore = ranks.sumOf { 1.0 / (it + k) }.toFloat()
                    elementToRrfScore[elementId] = rrfScore
                }

                // 按RRF分数排序
                results.map { result ->
                    val rrfScore = elementToRrfScore[result.element.id] ?: 0f
                    result.copy(combinedScore = rrfScore)
                }.sortedByDescending { it.combinedScore }
            }
        }
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        cache.clear()
        logger.info { "混合检索器缓存已清除" }
    }
}

/**
 * 检索来源
 */
enum class RetrievalSource {
    VECTOR,
    KEYWORD,
    HYBRID
}

/**
 * 检索结果
 *
 * @property element 代码元素
 * @property vectorScore 向量分数
 * @property keywordScore 关键词分数
 * @property combinedScore 组合分数
 * @property source 来源
 */
data class RetrievalResult(
    val element: CodeElement,
    val vectorScore: Float,
    val keywordScore: Float,
    val combinedScore: Float,
    val source: RetrievalSource
)
