package ai.kastrax.rag.reranker

import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.model.SearchResult
import ai.kastrax.rag.util.cosineSimilarity
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 上下文感知重排序器配置。
 *
 * @property contextWeight 上下文的权重，默认为 0.6
 * @property queryWeight 查询的权重，默认为 0.4
 * @property originalScoreWeight 原始分数的权重，默认为 0.3
 * @property maxContextLength 最大上下文长度，默认为 1000
 */
data class ContextAwareRerankerConfig(
    val contextWeight: Double = 0.6,
    val queryWeight: Double = 0.4,
    val originalScoreWeight: Double = 0.3,
    val maxContextLength: Int = 1000
) {
    init {
        require(contextWeight >= 0) { "Context weight must be non-negative" }
        require(queryWeight >= 0) { "Query weight must be non-negative" }
        require(originalScoreWeight >= 0) { "Original score weight must be non-negative" }
        require(contextWeight + queryWeight + originalScoreWeight > 0) { "At least one weight must be positive" }
        require(maxContextLength > 0) { "Max context length must be positive" }
    }
}

/**
 * 上下文感知重排序器，考虑查询的上下文信息来重排序检索结果。
 *
 * @property embeddingService 嵌入服务
 * @property config 重排序器配置
 */
/**
 * 上下文感知重排序器接口。
 * 用于根据上下文对检索结果进行重排序。
 */
interface ContextAwareReranker : Reranker {

    /**
     * 根据上下文对检索结果进行重排序。
     *
     * @param query 查询
     * @param results 检索结果
     * @param context 上下文
     * @return 重排序后的结果
     */
    suspend fun rerank(query: String, results: List<SearchResult>, context: Map<String, Any>): List<SearchResult>
}

/**
 * 上下文感知重排序器，考虑查询的上下文信息来重排序检索结果。
 *
 * @property embeddingService 嵌入服务
 * @property config 重排序器配置
 */
class DefaultContextAwareReranker(
    private val embeddingService: EmbeddingService,
    private val config: ContextAwareRerankerConfig = ContextAwareRerankerConfig()
) : ContextAwareReranker {

    /**
     * 对搜索结果进行重排序，考虑查询的上下文信息。
     *
     * @param query 查询文本
     * @param results 原始搜索结果
     * @param context 查询的上下文信息
     * @return 重排序后的搜索结果
     */
    override suspend fun rerank(query: String, results: List<SearchResult>, context: Map<String, Any>): List<SearchResult> {
        // 获取上下文字符串
        val contextStr = context["text"] as? String ?: ""
        if (results.isEmpty() || contextStr.isEmpty()) {
            return rerank(query, results)
        }

        logger.debug { "Reranking ${results.size} results using context-aware strategy" }

        try {
            // 截断上下文，确保不超过最大长度
            val truncatedContext = if (contextStr.length > config.maxContextLength) {
                contextStr.substring(0, config.maxContextLength)
            } else {
                contextStr
            }

            // 计算查询和上下文的嵌入向量
            val queryEmbedding = embeddingService.embed(query)
            val contextEmbedding = embeddingService.embed(truncatedContext)

            // 重排序结果
            val rerankedResults = results.map { result ->
                // 计算文档与查询的相似度
                val docEmbedding = embeddingService.embed(result.document.content)
                val queryDocSimilarity = cosineSimilarity(queryEmbedding, docEmbedding)

                // 计算文档与上下文的相似度
                val contextDocSimilarity = cosineSimilarity(contextEmbedding, docEmbedding)

                // 计算组合分数
                val combinedScore = (
                    queryDocSimilarity * config.queryWeight +
                    contextDocSimilarity * config.contextWeight +
                    result.score * config.originalScoreWeight
                ) / (config.queryWeight + config.contextWeight + config.originalScoreWeight)

                SearchResult(result.document, combinedScore)
            }

            // 按组合分数降序排序
            return rerankedResults.sortedByDescending { it.score }
        } catch (e: Exception) {
            logger.error(e) { "Error reranking results using context-aware strategy" }
            return results
        }
    }

    /**
     * 对搜索结果进行重排序，不考虑上下文信息。
     *
     * @param query 查询文本
     * @param results 原始搜索结果
     * @return 重排序后的搜索结果
     */
    override suspend fun rerank(query: String, results: List<SearchResult>): List<SearchResult> {
        if (results.isEmpty()) {
            return results
        }

        logger.debug { "Reranking ${results.size} results using query-only strategy" }

        try {
            // 计算查询的嵌入向量
            val queryEmbedding = embeddingService.embed(query)

            // 重排序结果
            val rerankedResults = results.map { result ->
                // 计算文档与查询的相似度
                val docEmbedding = embeddingService.embed(result.document.content)
                val queryDocSimilarity = cosineSimilarity(queryEmbedding, docEmbedding)

                // 计算组合分数
                val combinedScore = (
                    queryDocSimilarity * config.queryWeight +
                    result.score * config.originalScoreWeight
                ) / (config.queryWeight + config.originalScoreWeight)

                SearchResult(result.document, combinedScore)
            }

            // 按组合分数降序排序
            return rerankedResults.sortedByDescending { it.score }
        } catch (e: Exception) {
            logger.error(e) { "Error reranking results using query-only strategy" }
            return results
        }
    }
}
