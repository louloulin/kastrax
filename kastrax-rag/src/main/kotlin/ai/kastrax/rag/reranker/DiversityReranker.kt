package ai.kastrax.rag.reranker

import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

/**
 * 多样性重排序器，增加结果的多样性。
 *
 * @property embeddingService 嵌入服务
 * @property diversityWeight 多样性权重
 * @property relevanceWeight 相关性权重
 * @property similarityThreshold 相似度阈值
 */
class DiversityReranker(
    private val embeddingService: EmbeddingService,
    private val diversityWeight: Double = 0.3,
    private val relevanceWeight: Double = 0.7,
    private val similarityThreshold: Double = 0.9
) : Reranker {
    init {
        require(diversityWeight + relevanceWeight > 0.99 && diversityWeight + relevanceWeight < 1.01) {
            "Diversity weight ($diversityWeight) + relevance weight ($relevanceWeight) must be approximately 1.0"
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
        if (results.size <= 1) {
            return@withContext results
        }
        
        try {
            // 计算每个文档的嵌入向量
            val embeddings = results.map { result ->
                embeddingService.embed(result.document.content)
            }
            
            // 计算文档之间的相似度矩阵
            val similarityMatrix = calculateSimilarityMatrix(embeddings)
            
            // 使用最大边缘相关性算法重排序
            val rerankedIndices = maximalMarginalRelevance(
                similarityMatrix = similarityMatrix,
                scores = results.map { it.score },
                diversityWeight = diversityWeight,
                relevanceWeight = relevanceWeight
            )
            
            // 创建重排序后的结果列表
            return@withContext rerankedIndices.map { index ->
                results[index]
            }
        } catch (e: Exception) {
            logger.error(e) { "Error reranking documents for diversity" }
            return@withContext results
        }
    }
    
    /**
     * 计算相似度矩阵。
     *
     * @param embeddings 嵌入向量列表
     * @return 相似度矩阵
     */
    private fun calculateSimilarityMatrix(embeddings: List<FloatArray>): Array<DoubleArray> {
        val n = embeddings.size
        val matrix = Array(n) { DoubleArray(n) }
        
        for (i in 0 until n) {
            for (j in i until n) {
                val similarity = calculateCosineSimilarity(embeddings[i], embeddings[j])
                matrix[i][j] = similarity
                matrix[j][i] = similarity
            }
        }
        
        return matrix
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
    
    /**
     * 最大边缘相关性算法。
     *
     * @param similarityMatrix 相似度矩阵
     * @param scores 分数列表
     * @param diversityWeight 多样性权重
     * @param relevanceWeight 相关性权重
     * @return 重排序后的索引列表
     */
    private fun maximalMarginalRelevance(
        similarityMatrix: Array<DoubleArray>,
        scores: List<Double>,
        diversityWeight: Double,
        relevanceWeight: Double
    ): List<Int> {
        val n = scores.size
        val selected = mutableListOf<Int>()
        val remaining = (0 until n).toMutableList()
        
        // 选择第一个文档（分数最高的）
        val firstIndex = scores.indices.maxByOrNull { scores[it] } ?: 0
        selected.add(firstIndex)
        remaining.remove(firstIndex)
        
        // 选择剩余的文档
        while (selected.size < n && remaining.isNotEmpty()) {
            var bestIndex = -1
            var bestScore = Double.NEGATIVE_INFINITY
            
            for (i in remaining) {
                // 计算与已选文档的最大相似度
                val maxSimilarity = selected.maxOfOrNull { j -> similarityMatrix[i][j] } ?: 0.0
                
                // 计算多样性分数
                val diversityScore = 1.0 - maxSimilarity
                
                // 计算最终分数
                val finalScore = (relevanceWeight * scores[i]) + (diversityWeight * diversityScore)
                
                if (finalScore > bestScore) {
                    bestScore = finalScore
                    bestIndex = i
                }
            }
            
            if (bestIndex != -1) {
                selected.add(bestIndex)
                remaining.remove(bestIndex)
            } else {
                break
            }
        }
        
        return selected
    }
}
