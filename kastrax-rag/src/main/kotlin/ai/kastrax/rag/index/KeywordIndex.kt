package ai.kastrax.rag.index

import ai.kastrax.rag.document.RagDocument

/**
 * 关键词索引接口。
 */
interface KeywordIndex {

    /**
     * 添加文档到索引。
     *
     * @param documents 文档列表
     * @return 是否成功添加
     */
    suspend fun addDocuments(documents: List<RagDocument>): Boolean

    /**
     * 从索引中删除文档。
     *
     * @param ids 文档 ID 列表
     * @return 是否成功删除
     */
    suspend fun deleteDocuments(ids: List<String>): Boolean

    /**
     * 搜索文档。
     *
     * @param query 查询
     * @param limit 返回结果数量
     * @return 文档列表
     */
    suspend fun search(query: String, limit: Int = 5): List<RagDocument>

    /**
     * 使用过滤器搜索文档。
     *
     * @param query 查询
     * @param filter 过滤条件
     * @param limit 返回结果数量
     * @return 文档列表
     */
    suspend fun searchWithFilter(query: String, filter: Map<String, Any>, limit: Int = 5): List<RagDocument>

    /**
     * 使用元数据过滤器搜索文档。
     *
     * @param filter 过滤条件
     * @param limit 返回结果数量
     * @return 文档列表
     */
    suspend fun searchByMetadata(filter: Map<String, Any>, limit: Int = 5): List<RagDocument>
}
