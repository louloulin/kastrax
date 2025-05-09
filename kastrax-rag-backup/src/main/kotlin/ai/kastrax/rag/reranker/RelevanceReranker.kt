package ai.kastrax.rag.reranker

import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.vectorstore.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private val logger = KotlinLogging.logger {}

// Reranker interface moved to Reranker.kt

/**
 * 基于相关性的重排序器配置。
 *
 * @property chunkSize 文档分块大小，默认为 200
 * @property chunkOverlap 文档分块重叠大小，默认为 50
 * @property originalScoreWeight 原始分数的权重，默认为 0.3
 * @property relevanceScoreWeight 相关性分数的权重，默认为 0.7
 */
data class RelevanceRerankerConfig(
    val chunkSize: Int = 200,
    val chunkOverlap: Int = 50,
    val originalScoreWeight: Double = 0.3,
    val relevanceScoreWeight: Double = 0.7
) {
    init {
        require(chunkSize > 0) { "Chunk size must be positive" }
        require(chunkOverlap >= 0) { "Chunk overlap must be non-negative" }
        require(chunkOverlap < chunkSize) { "Chunk overlap must be less than chunk size" }
        require(originalScoreWeight >= 0) { "Original score weight must be non-negative" }
        require(relevanceScoreWeight >= 0) { "Relevance score weight must be non-negative" }
        require(originalScoreWeight + relevanceScoreWeight > 0) { "At least one weight must be positive" }
    }
}

/**
 * 基于相关性的重排序器，使用嵌入模型计算查询和文档的相关性。
 *
 * @property embeddingService 嵌入服务
 * @property config 重排序器配置
 */
class RelevanceReranker(
    private val embeddingService: EmbeddingService,
    private val config: RelevanceRerankerConfig = RelevanceRerankerConfig()
) : Reranker {

    /**
     * 对搜索结果进行重排序。
     *
     * @param query 查询文本
     * @param results 原始搜索结果
     * @return 重排序后的搜索结果
     */
    override suspend fun rerank(query: String, results: List<SearchResult>): List<SearchResult> {
        if (results.isEmpty()) {
            return results
        }

        logger.debug { "Reranking ${results.size} results using relevance" }

        try {
            // 计算查询的嵌入向量
            val queryEmbedding = embeddingService.embed(query)

            // 并行处理每个文档
            val rerankedResults = coroutineScope {
                results.map { result ->
                    async {
                        // 计算文档的相关性分数
                        val relevanceScore = calculateRelevanceScore(result.document.content, queryEmbedding)

                        // 计算组合分数
                        val combinedScore = (result.score * config.originalScoreWeight +
                            relevanceScore * config.relevanceScoreWeight) /
                            (config.originalScoreWeight + config.relevanceScoreWeight)

                        SearchResult(result.document, combinedScore)
                    }
                }.awaitAll()
            }

            // 按组合分数降序排序
            return rerankedResults.sortedByDescending { it.score }
        } catch (e: Exception) {
            logger.error(e) { "Error reranking results using relevance" }
            return results
        }
    }

    /**
     * 计算文档与查询的相关性分数。
     *
     * @param documentContent 文档内容
     * @param queryEmbedding 查询嵌入向量
     * @return 相关性分数
     */
    private suspend fun calculateRelevanceScore(documentContent: String, queryEmbedding: FloatArray): Double {
        // 将文档分成小块
        val chunks = splitTextIntoChunks(documentContent)

        if (chunks.isEmpty()) {
            return 0.0
        }

        // 计算每个块的嵌入向量
        val chunkEmbeddings = chunks.map { chunk ->
            embeddingService.embed(chunk)
        }

        // 计算每个块与查询的相似度
        val similarities = chunkEmbeddings.map { embedding ->
            ai.kastrax.rag.util.cosineSimilarity(embedding, queryEmbedding)
        }

        // 返回最大相似度作为相关性分数
        return if (similarities.isEmpty()) 0.0 else similarities.maxOrNull() ?: 0.0
    }

    /**
     * 将文本分成小块。
     *
     * @param text 输入文本
     * @return 文本块列表
     */
    private fun splitTextIntoChunks(text: String): List<String> {
        if (text.length <= config.chunkSize) {
            return listOf(text)
        }

        val chunks = mutableListOf<String>()
        var startIndex = 0

        while (startIndex < text.length) {
            val endIndex = minOf(startIndex + config.chunkSize, text.length)
            chunks.add(text.substring(startIndex, endIndex))
            startIndex += config.chunkSize - config.chunkOverlap
        }

        return chunks
    }
}
