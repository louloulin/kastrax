package ai.kastrax.rag.reranker

import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

/**
 * 上下文感知重排序器配置。
 *
 * @property contextWeight 上下文权重
 * @property queryWeight 查询权重
 * @property originalScoreWeight 原始分数权重
 */
data class ContextAwareRerankerConfig(
    val contextWeight: Double = 0.3,
    val queryWeight: Double = 0.5,
    val originalScoreWeight: Double = 0.2
)

/**
 * 上下文感知重排序器，基于上下文、查询和文档内容的相似度重排序。
 *
 * @property embeddingService 嵌入服务
 * @property config 配置
 */
class ContextAwareReranker(
    private val embeddingService: EmbeddingService,
    private val config: ContextAwareRerankerConfig = ContextAwareRerankerConfig()
) : Reranker {
    init {
        val sum = config.contextWeight + config.queryWeight + config.originalScoreWeight
        require(sum > 0.99 && sum < 1.01) {
            "Context weight (${config.contextWeight}) + query weight (${config.queryWeight}) + " +
                    "original score weight (${config.originalScoreWeight}) must be approximately 1.0"
        }
    }
    
    /**
     * 重排序文档。
     *
     * @param query 查询文本
     * @param results 检索结果列表
     * @return 重排序后的结果列表
     */
    override suspend fun rerank(
        query: String,
        results: List<DocumentSearchResult>
    ): List<DocumentSearchResult> = withContext(Dispatchers.IO) {
        // 没有上下文时，使用普通重排序
        return@withContext rerankWithContext(query, results, "")
    }
    
    /**
     * 使用上下文重排序文档。
     *
     * @param query 查询文本
     * @param results 检索结果列表
     * @param context 上下文
     * @return 重排序后的结果列表
     */
    suspend fun rerankWithContext(
        query: String,
        results: List<DocumentSearchResult>,
        context: String
    ): List<DocumentSearchResult> = withContext(Dispatchers.IO) {
        if (results.isEmpty()) {
            return@withContext results
        }
        
        try {
            // 计算查询嵌入向量
            val queryEmbedding = embeddingService.embed(query)
            
            // 计算上下文嵌入向量（如果有）
            val contextEmbedding = if (context.isNotBlank()) {
                embeddingService.embed(context)
            } else {
                null
            }
            
            // 计算每个文档的相关性分数
            val rerankedResults = results.map { result ->
                // 计算文档内容的嵌入向量
                val contentEmbedding = embeddingService.embed(result.document.content)
                
                // 计算与查询的相似度
                val querySimilarity = calculateCosineSimilarity(queryEmbedding, contentEmbedding)
                
                // 计算与上下文的相似度（如果有）
                val contextSimilarity = if (contextEmbedding != null) {
                    calculateCosineSimilarity(contextEmbedding, contentEmbedding)
                } else {
                    0.0
                }
                
                // 计算最终分数
                val finalScore = (config.queryWeight * querySimilarity) +
                        (config.contextWeight * contextSimilarity) +
                        (config.originalScoreWeight * result.score)
                
                // 创建新的搜索结果
                DocumentSearchResult(
                    document = result.document,
                    score = finalScore
                )
            }
            
            // 按分数降序排序
            return@withContext rerankedResults.sortedByDescending { it.score }
        } catch (e: Exception) {
            logger.error(e) { "Error reranking documents with context" }
            return@withContext results
        }
    }
    
    /**
     * 计算余弦相似度。
     *
     * @param vec1 向量 1
     * @param vec2 向量 2
     * @return 余弦相似度
     */
    private fun calculateCosineSimilarity(vec1: FloatArray, vec2: FloatArray): Double {
        require(vec1.size == vec2.size) { "Vectors must have the same dimension" }
        
        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0
        
        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }
        
        return if (norm1 > 0 && norm2 > 0) {
            dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2))
        } else {
            0.0
        }
    }
}
