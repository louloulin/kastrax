package ai.kastrax.rag.reranker

import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.vectorstore.SearchResult
import ai.kastrax.rag.util.cosineSimilarity
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private val logger = KotlinLogging.logger {}

/**
 * 多样性重排序器配置。
 *
 * @property diversityWeight 多样性的权重，默认为 0.5
 * @property originalScoreWeight 原始分数的权重，默认为 0.5
 * @property similarityThreshold 相似度阈值，默认为 0.8
 * @property maxIterations 最大迭代次数，默认为 100
 */
data class DiversityRerankerConfig(
    val diversityWeight: Double = 0.5,
    val originalScoreWeight: Double = 0.5,
    val similarityThreshold: Double = 0.8,
    val maxIterations: Int = 100
) {
    init {
        require(diversityWeight >= 0) { "Diversity weight must be non-negative" }
        require(originalScoreWeight >= 0) { "Original score weight must be non-negative" }
        require(diversityWeight + originalScoreWeight > 0) { "At least one weight must be positive" }
        require(similarityThreshold in 0.0..1.0) { "Similarity threshold must be between 0.0 and 1.0" }
        require(maxIterations > 0) { "Max iterations must be positive" }
    }
}

/**
 * 多样性重排序器，确保检索结果的多样性。
 *
 * @property embeddingService 嵌入服务
 * @property config 重排序器配置
 */
class DiversityReranker(
    private val embeddingService: EmbeddingService,
    private val config: DiversityRerankerConfig = DiversityRerankerConfig()
) : Reranker {

    /**
     * 对搜索结果进行重排序，确保多样性。
     *
     * @param query 查询文本
     * @param results 原始搜索结果
     * @return 重排序后的搜索结果
     */
    override suspend fun rerank(query: String, results: List<SearchResult>): List<SearchResult> {
        if (results.isEmpty()) {
            return results
        }

        logger.debug { "Reranking ${results.size} results using diversity strategy" }

        try {
            // 计算每个文档的嵌入向量
            val embeddings = coroutineScope {
                results.map { result ->
                    async {
                        embeddingService.embed(result.document.content).map { it.toDouble() }
                    }
                }.awaitAll()
            }

            // 计算相似度矩阵
            val similarityMatrix = calculateSimilarityMatrix(embeddings)

            // 使用最大边缘相关性算法选择多样化的结果
            val selectedIndices = maximalMarginalRelevance(
                similarityMatrix,
                results.map { it.score },
                results.size
            )

            // 按选择顺序返回结果
            return selectedIndices.map { results[it] }
        } catch (e: Exception) {
            logger.error(e) { "Error reranking results using diversity strategy" }
            return results
        }
    }

    /**
     * 计算嵌入向量之间的相似度矩阵。
     *
     * @param embeddings 嵌入向量列表
     * @return 相似度矩阵
     */
    private fun calculateSimilarityMatrix(embeddings: List<List<Double>>): Array<DoubleArray> {
        val n = embeddings.size
        val matrix = Array(n) { DoubleArray(n) }

        for (i in 0 until n) {
            for (j in i until n) {
                val similarity = if (i == j) {
                    1.0 // 自身相似度为 1
                } else {
                    val vec1 = embeddings[i]
                    val vec2 = embeddings[j]
                    // 使用列表版本的余弦相似度计算
                    calculateCosineSimilarity(vec1, vec2)
                }

                matrix[i][j] = similarity
                matrix[j][i] = similarity // 矩阵是对称的
            }
        }

        return matrix
    }

    /**
     * 计算两个向量的余弦相似度。
     *
     * @param vec1 第一个向量
     * @param vec2 第二个向量
     * @return 余弦相似度
     */
    private fun calculateCosineSimilarity(vec1: List<Double>, vec2: List<Double>): Double {
        if (vec1.isEmpty() || vec2.isEmpty() || vec1.size != vec2.size) {
            return 0.0
        }

        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0

        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }

        if (norm1 <= 0.0 || norm2 <= 0.0) {
            return 0.0
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2))
    }

    /**
     * 使用最大边缘相关性算法选择多样化的结果。
     *
     * @param similarityMatrix 相似度矩阵
     * @param scores 原始分数列表
     * @param limit 选择的结果数量
     * @return 选定的结果索引
     */
    private fun maximalMarginalRelevance(
        similarityMatrix: Array<DoubleArray>,
        scores: List<Double>,
        limit: Int
    ): List<Int> {
        val n = similarityMatrix.size
        val selected = mutableListOf<Int>()
        val remaining = (0 until n).toMutableSet()

        // 首先选择分数最高的结果
        val firstIndex = scores.indices.maxByOrNull { scores[it] } ?: 0
        selected.add(firstIndex)
        remaining.remove(firstIndex)

        // 迭代选择剩余的结果
        var iterations = 0
        while (selected.size < limit && remaining.isNotEmpty() && iterations < config.maxIterations) {
            var bestIndex = -1
            var bestScore = Double.NEGATIVE_INFINITY

            // 对于每个未选择的结果
            for (i in remaining) {
                // 计算原始相关性分数
                val relevanceScore = scores[i] * config.originalScoreWeight

                // 计算与已选择结果的最大相似度
                var maxSimilarity = 0.0
                for (j in selected) {
                    maxSimilarity = maxOf(maxSimilarity, similarityMatrix[i][j])
                }

                // 计算多样性分数（1 - 最大相似度）
                val diversityScore = (1.0 - maxSimilarity) * config.diversityWeight

                // 计算组合分数
                val combinedScore = relevanceScore + diversityScore

                if (combinedScore > bestScore) {
                    bestScore = combinedScore
                    bestIndex = i
                }
            }

            if (bestIndex != -1) {
                selected.add(bestIndex)
                remaining.remove(bestIndex)
            } else {
                break
            }

            iterations++
        }

        return selected
    }
}
