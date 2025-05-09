package ai.kastrax.rag.retrieval

import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.vectorstore.RagDocument
import ai.kastrax.rag.vectorstore.RagVectorStore
import ai.kastrax.rag.vectorstore.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 基于向量存储的检索器，使用向量相似度搜索检索文档。
 *
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 * @property defaultLimit 默认返回结果的最大数量
 * @property defaultMinScore 默认最小相似度分数
 */
class VectorStoreRetriever(
    private val vectorStore: RagVectorStore,
    private val embeddingService: EmbeddingService,
    private val defaultLimit: Int = 5,
    private val defaultMinScore: Double = 0.0
) : Retriever {
    
    /**
     * 使用查询文本检索文档。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 检索结果列表，按相似度降序排序
     */
    override suspend fun retrieve(query: String, limit: Int, minScore: Double): List<RagDocument> {
        val actualLimit = if (limit <= 0) defaultLimit else limit
        val actualMinScore = if (minScore < 0.0) defaultMinScore else minScore
        
        logger.debug { "Retrieving documents for query: $query, limit: $actualLimit, minScore: $actualMinScore" }
        
        return try {
            // 使用向量存储的相似度搜索功能
            val results = vectorStore.similaritySearch(query, embeddingService, actualLimit, actualMinScore)
            
            // 返回文档
            results.map { it.document }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving documents for query: $query" }
            emptyList()
        }
    }
}
