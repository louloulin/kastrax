package ai.kastrax.rag.retrieval

import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.vectorstore.RagDocument
import ai.kastrax.rag.vectorstore.RagVectorStore
import ai.kastrax.rag.vectorstore.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 检索器接口，用于从向量存储中检索文档。
 */
interface Retriever {
    /**
     * 使用查询文本检索文档。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 文档列表
     */
    suspend fun retrieve(query: String, limit: Int = 5, minScore: Double = 0.0): List<RagDocument>
}

/**
 * 基本的 Top-K 检索器，使用向量相似度搜索检索文档。
 *
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 */
class TopKRetriever(
    private val vectorStore: RagVectorStore,
    private val embeddingService: EmbeddingService
) : Retriever {

    /**
     * 使用查询文本检索文档。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 文档列表，按相似度降序排序
     */
    override suspend fun retrieve(query: String, limit: Int, minScore: Double): List<RagDocument> {
        logger.debug { "Retrieving documents for query: $query, limit: $limit, minScore: $minScore" }

        return try {
            // 使用向量存储的相似度搜索功能
            val results = vectorStore.similaritySearch(query, embeddingService, limit, minScore)

            // 返回文档
            results.map { it.document }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving documents for query: $query" }
            emptyList()
        }
    }
}
