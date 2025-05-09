package ai.kastrax.rag.retriever

import ai.kastrax.rag.document.RagDocument
import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.vectorstore.RagVectorStore
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 基于向量存储的检索器。
 *
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 */
class VectorStoreRetriever(
    private val vectorStore: RagVectorStore,
    private val embeddingService: EmbeddingService
) : Retriever {

    /**
     * 检索文档。
     *
     * @param query 查询
     * @param limit 返回结果数量
     * @return 文档列表
     */
    override suspend fun retrieve(query: String, limit: Int): List<RagDocument> {
        logger.debug { "Retrieving documents for query: $query, limit: $limit" }
        
        // 获取查询的嵌入向量
        val embedding = embeddingService.embed(query)
        
        // 使用向量存储进行检索
        val results = vectorStore.similaritySearch(embedding, limit)
        
        logger.debug { "Retrieved ${results.size} documents" }
        return results
    }

    /**
     * 使用过滤器检索文档。
     *
     * @param query 查询
     * @param filter 过滤条件
     * @param limit 返回结果数量
     * @return 文档列表
     */
    suspend fun retrieveWithFilter(
        query: String,
        filter: Map<String, Any>,
        limit: Int = 5
    ): List<RagDocument> {
        logger.debug { "Retrieving documents for query: $query, filter: $filter, limit: $limit" }
        
        // 获取查询的嵌入向量
        val embedding = embeddingService.embed(query)
        
        // 使用向量存储进行检索
        val results = vectorStore.similaritySearchWithFilter(embedding, filter, limit)
        
        logger.debug { "Retrieved ${results.size} documents" }
        return results
    }

    /**
     * 使用元数据过滤器检索文档。
     *
     * @param filter 过滤条件
     * @param limit 返回结果数量
     * @return 文档列表
     */
    suspend fun retrieveByMetadata(
        filter: Map<String, Any>,
        limit: Int = 5
    ): List<RagDocument> {
        logger.debug { "Retrieving documents by metadata: $filter, limit: $limit" }
        
        // 使用向量存储进行检索
        val results = vectorStore.metadataSearch(filter, limit)
        
        logger.debug { "Retrieved ${results.size} documents" }
        return results
    }
}
