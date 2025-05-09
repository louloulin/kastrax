package ai.kastrax.rag.reranker

import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

/**
 * 相关性重排序器，基于查询和文档内容的相似度重排序。
 *
 * @property embeddingService 嵌入服务
 * @property useContentOnly 是否只使用内容进行重排序
 * @property contentWeight 内容权重
 * @property scoreWeight 原始分数权重
 */
class RelevanceReranker(
    private val embeddingService: EmbeddingService,
    private val useContentOnly: Boolean = false,
    private val contentWeight: Double = 0.7,
    private val scoreWeight: Double = 0.3
) : Reranker {
    init {
        require(contentWeight + scoreWeight > 0.99 && contentWeight + scoreWeight < 1.01) {
            "Content weight ($contentWeight) + score weight ($scoreWeight) must be approximately 1.0"
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
        if (results.isEmpty()) {
            return@withContext results
        }
        
        try {
            // 计算查询嵌入向量
            val queryEmbedding = embeddingService.embed(query)
            
            // 计算每个文档的相关性分数
            val rerankedResults = results.map { result ->
                // 计算文档内容的嵌入向量
                val contentEmbedding = embeddingService.embed(result.document.content)
                
                // 计算余弦相似度
                val similarity = calculateCosineSimilarity(queryEmbedding, contentEmbedding)
                
                // 计算最终分数
                val finalScore = if (useContentOnly) {
                    similarity
                } else {
                    (contentWeight * similarity) + (scoreWeight * result.score)
                }
                
                // 创建新的搜索结果
                DocumentSearchResult(
                    document = result.document,
                    score = finalScore
                )
            }
            
            // 按分数降序排序
            return@withContext rerankedResults.sortedByDescending { it.score }
        } catch (e: Exception) {
            logger.error(e) { "Error reranking documents" }
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
