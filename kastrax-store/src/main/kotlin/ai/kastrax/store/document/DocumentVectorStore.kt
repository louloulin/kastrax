package ai.kastrax.store.document

import ai.kastrax.store.VectorStore
import ai.kastrax.store.embedding.EmbeddingService

/**
 * 文档向量存储接口，提供文档操作的统一接口。
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
    fun getVectorStore(): VectorStore

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
     * 添加文档（已嵌入）。
     *
     * @param documents 文档列表
     * @return 是否成功添加
     */
    suspend fun addDocuments(
        documents: List<Document>
    ): Boolean

    /**
     * 删除文档。
     *
     * @param ids 文档 ID 列表
     * @return 是否成功删除
     */
    suspend fun deleteDocuments(
        ids: List<String>
    ): Boolean

    /**
     * 相似度搜索。
     *
     * @param query 查询文本
     * @param embeddingService 嵌入服务
     * @param limit 返回结果的最大数量
     * @return 搜索结果列表
     */
    suspend fun similaritySearch(
        query: String,
        embeddingService: EmbeddingService,
        limit: Int = 5
    ): List<DocumentSearchResult>

    /**
     * 相似度搜索（已嵌入）。
     *
     * @param embedding 查询嵌入
     * @param limit 返回结果的最大数量
     * @return 搜索结果列表
     */
    suspend fun similaritySearch(
        embedding: FloatArray,
        limit: Int = 5
    ): List<DocumentSearchResult>

    /**
     * 带过滤器的相似度搜索。
     *
     * @param embedding 查询嵌入
     * @param filter 过滤条件
     * @param limit 返回结果的最大数量
     * @return 搜索结果列表
     */
    suspend fun similaritySearchWithFilter(
        embedding: FloatArray,
        filter: Map<String, Any>,
        limit: Int = 5
    ): List<DocumentSearchResult>

    /**
     * 关键词搜索。
     *
     * @param keywords 关键词列表
     * @param limit 返回结果的最大数量
     * @return 搜索结果列表
     */
    suspend fun keywordSearch(
        keywords: List<String>,
        limit: Int = 5
    ): List<DocumentSearchResult>

    /**
     * 元数据搜索。
     *
     * @param filter 过滤条件
     * @param limit 返回结果的最大数量
     * @return 搜索结果列表
     */
    suspend fun metadataSearch(
        filter: Map<String, Any>,
        limit: Int = 5
    ): List<DocumentSearchResult>
}


