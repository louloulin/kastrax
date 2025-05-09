package ai.kastrax.rag.retrieval

import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.vectorstore.RagVectorStore
import ai.kastrax.rag.vectorstore.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 语义检索器，使用语义理解增强检索结果。
 *
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 * @property config 语义检索器配置
 */
class SemanticRetriever(
    private val vectorStore: RagVectorStore,
    private val embeddingService: EmbeddingService,
    private val config: SemanticRetrieverConfig = SemanticRetrieverConfig()
) : Retriever {

    /**
     * 使用查询文本检索文档。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 检索结果列表，按相似度降序排序
     */
    override suspend fun retrieve(query: String, limit: Int, minScore: Double): List<SearchResult> {
        logger.debug { "Retrieving documents for query: $query, limit: $limit, minScore: $minScore" }

        try {
            // 如果启用了查询扩展，则检索更多文档
            val actualLimit = if (config.expandQuery) {
                (limit * config.queryExpansionFactor).toInt().coerceAtLeast(limit + 1)
            } else {
                limit
            }

            // 使用向量存储的相似度搜索功能
            val results = vectorStore.similaritySearch(query, embeddingService, actualLimit, minScore)

            // 如果没有启用查询扩展，直接返回结果
            if (!config.expandQuery || results.size <= limit) {
                return results
            }

            // 对结果进行语义聚类，确保多样性
            return semanticClustering(results, limit)
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving documents for query: $query" }
            return emptyList()
        }
    }

    /**
     * 对搜索结果进行语义聚类，确保结果的多样性。
     *
     * @param results 原始搜索结果
     * @param limit 返回结果的最大数量
     * @return 聚类后的搜索结果
     */
    private suspend fun semanticClustering(results: List<SearchResult>, limit: Int): List<SearchResult> {
        if (results.size <= limit) {
            return results
        }

        // 计算结果之间的相似度矩阵
        val similarityMatrix = calculateSimilarityMatrix(results)

        // 使用贪婪算法选择多样化的结果
        val selectedIndices = greedyDiverseSelection(similarityMatrix, limit)

        // 按原始顺序返回选定的结果
        return selectedIndices.sorted().map { results[it] }
    }

    /**
     * 计算搜索结果之间的相似度矩阵。
     *
     * @param results 搜索结果
     * @return 相似度矩阵
     */
    private suspend fun calculateSimilarityMatrix(results: List<SearchResult>): Array<DoubleArray> {
        val n = results.size
        val matrix = Array(n) { DoubleArray(n) }

        // 计算每个文档的嵌入向量
        val embeddings = results.map { result ->
            embeddingService.embed(result.document.content)
        }

        // 计算相似度矩阵
        for (i in 0 until n) {
            for (j in i until n) {
                val similarity = if (i == j) {
                    1.0 // 自身相似度为 1
                } else {
                    ai.kastrax.rag.util.cosineSimilarity(embeddings[i], embeddings[j])
                }

                matrix[i][j] = similarity
                matrix[j][i] = similarity // 矩阵是对称的
            }
        }

        return matrix
    }

    /**
     * 使用贪婪算法选择多样化的结果。
     *
     * @param similarityMatrix 相似度矩阵
     * @param limit 选择的结果数量
     * @return 选定的结果索引
     */
    private fun greedyDiverseSelection(similarityMatrix: Array<DoubleArray>, limit: Int): List<Int> {
        val n = similarityMatrix.size
        val selected = mutableListOf<Int>()

        // 首先选择第一个结果
        selected.add(0)

        // 贪婪选择剩余的结果
        while (selected.size < limit && selected.size < n) {
            var bestIndex = -1
            var minMaxSimilarity = Double.MAX_VALUE

            // 对于每个未选择的结果
            for (i in 0 until n) {
                if (i in selected) continue

                // 计算与已选择结果的最大相似度
                var maxSimilarity = Double.MIN_VALUE
                for (j in selected) {
                    maxSimilarity = maxOf(maxSimilarity, similarityMatrix[i][j])
                }

                // 选择与已选择结果最不相似的结果
                if (maxSimilarity < minMaxSimilarity) {
                    minMaxSimilarity = maxSimilarity
                    bestIndex = i
                }
            }

            if (bestIndex != -1) {
                selected.add(bestIndex)
            } else {
                break
            }
        }

        return selected
    }
}
