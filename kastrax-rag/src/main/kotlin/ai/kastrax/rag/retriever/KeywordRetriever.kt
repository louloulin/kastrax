package ai.kastrax.rag.retriever

import ai.kastrax.rag.document.RagDocument
import ai.kastrax.rag.index.KeywordIndex
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 基于关键词的检索器。
 *
 * @property keywordIndex 关键词索引
 */
class KeywordRetriever(
    private val keywordIndex: KeywordIndex
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
        
        // 使用关键词索引进行检索
        val results = keywordIndex.search(query, limit)
        
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
        
        // 使用关键词索引进行检索
        val results = keywordIndex.searchWithFilter(query, filter, limit)
        
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
        
        // 使用关键词索引进行检索
        val results = keywordIndex.searchByMetadata(filter, limit)
        
        logger.debug { "Retrieved ${results.size} documents" }
        return results
    }
}
