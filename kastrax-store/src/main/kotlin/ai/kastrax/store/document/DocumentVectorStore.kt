package ai.kastrax.store.document

import ai.kastrax.store.embedding.EmbeddingService

/**
 * 文档向量存储接口，用于存储和检索文档。
 */
interface DocumentVectorStore {

    /**
     * 向量维度。
     */
    val dimension: Int

    /**
     * 获取底层向量存储。
     * 
     * @return 向量存储
     */
    fun getVectorStore(): ai.kastrax.store.VectorStore

    /**
     * 添加文档。
     *
     * @param documents 文档列表
     * @param embeddingService 嵌入服务
     * @return 是否成功添加
     */
    suspend fun addDocuments(
        documents: List<Document>,
        embeddingService: EmbeddingService
    ): Boolean

    /**
     * 添加文档。
     *
     * @param documents 文档列表
     * @return 是否成功添加
     */
    suspend fun addDocuments(
        documents: List<Document>
    ): Boolean

    /**
     * 从索引中删除文档。
     *
     * @param ids 文档 ID 列表
     * @return 是否成功删除
     */
    suspend fun deleteDocuments(ids: List<String>): Boolean

    /**
     * 使用查询文本进行相似度搜索。
     *
     * @param query 查询文本
     * @param embeddingService 嵌入服务
     * @param limit 返回结果的最大数量
     * @return 文档列表，按相似度降序排序
     */
    suspend fun similaritySearch(
        query: String,
        embeddingService: EmbeddingService,
        limit: Int = 5
    ): List<Document>
    
    /**
     * 使用嵌入向量进行相似度搜索。
     *
     * @param embedding 嵌入向量
     * @param limit 返回结果的最大数量
     * @return 文档列表，按相似度降序排序
     */
    suspend fun similaritySearch(
        embedding: FloatArray,
        limit: Int = 5
    ): List<Document>
    
    /**
     * 使用嵌入向量和过滤器进行相似度搜索。
     *
     * @param embedding 嵌入向量
     * @param filter 过滤器
     * @param limit 返回结果的最大数量
     * @return 文档列表，按相似度降序排序
     */
    suspend fun similaritySearchWithFilter(
        embedding: FloatArray,
        filter: Map<String, Any>,
        limit: Int = 5
    ): List<Document>

    /**
     * 使用关键词进行搜索。
     *
     * @param keywords 关键词列表
     * @param limit 返回结果的最大数量
     * @return 文档列表，按匹配度降序排序
     */
    suspend fun keywordSearch(
        keywords: List<String>,
        limit: Int = 5
    ): List<Document>

    /**
     * 使用元数据过滤器进行搜索。
     *
     * @param filter 元数据过滤器
     * @param limit 返回结果的最大数量
     * @return 文档列表
     */
    suspend fun metadataSearch(
        filter: Map<String, Any>,
        limit: Int = 5
    ): List<Document>
}
