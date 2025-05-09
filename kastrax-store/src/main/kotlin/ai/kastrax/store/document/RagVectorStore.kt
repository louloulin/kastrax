package ai.kastrax.store.document

import ai.kastrax.store.embedding.EmbeddingService

/**
 * RAG 向量存储接口，提供 RAG 操作的统一接口。
 */
interface RagVectorStore {
    /**
     * 添加文档。
     *
     * @param document 文档内容
     * @param embedding 文档嵌入向量
     * @param metadata 文档元数据
     * @return 文档 ID
     */
    suspend fun addDocument(
        document: String,
        embedding: FloatArray,
        metadata: Map<String, Any> = emptyMap()
    ): String

    /**
     * 添加文档。
     *
     * @param document 文档内容
     * @param embeddingService 嵌入服务
     * @param metadata 文档元数据
     * @return 文档 ID
     */
    suspend fun addDocument(
        document: String,
        embeddingService: EmbeddingService,
        metadata: Map<String, Any> = emptyMap()
    ): String

    /**
     * 批量添加文档。
     *
     * @param documents 文档内容列表
     * @param embeddings 文档嵌入向量列表
     * @param metadata 文档元数据列表
     * @return 文档 ID 列表
     */
    suspend fun addDocuments(
        documents: List<String>,
        embeddings: List<FloatArray>,
        metadata: List<Map<String, Any>> = emptyList()
    ): List<String>

    /**
     * 批量添加文档。
     *
     * @param documents 文档内容列表
     * @param embeddingService 嵌入服务
     * @param metadata 文档元数据列表
     * @return 文档 ID 列表
     */
    suspend fun addDocuments(
        documents: List<String>,
        embeddingService: EmbeddingService,
        metadata: List<Map<String, Any>> = emptyList()
    ): List<String>

    /**
     * 获取文档。
     *
     * @param id 文档 ID
     * @return 文档
     */
    suspend fun getDocument(id: String): RagDocument?

    /**
     * 根据内容获取文档。
     *
     * @param content 文档内容
     * @return 文档
     */
    suspend fun getDocumentByContent(content: String): RagDocument?

    /**
     * 删除文档。
     *
     * @param id 文档 ID
     * @return 是否成功删除
     */
    suspend fun deleteDocument(id: String): Boolean

    /**
     * 批量删除文档。
     *
     * @param ids 文档 ID 列表
     * @return 是否成功删除
     */
    suspend fun deleteDocuments(ids: List<String>): Boolean

    /**
     * 相似度搜索。
     *
     * @param query 查询文本
     * @param embeddingService 嵌入服务
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 搜索结果列表
     */
    suspend fun similaritySearch(
        query: String,
        embeddingService: EmbeddingService,
        limit: Int = 5,
        minScore: Double = 0.0
    ): List<SearchResult>

    /**
     * 相似度搜索。
     *
     * @param embedding 查询嵌入向量
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 搜索结果列表
     */
    suspend fun similaritySearch(
        embedding: FloatArray,
        limit: Int = 5,
        minScore: Double = 0.0
    ): List<SearchResult>

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
    ): List<SearchResult>

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
    ): List<SearchResult>

    /**
     * 清空存储。
     */
    suspend fun clear()
}

/**
 * 搜索结果类，表示一个 RAG 搜索的结果。
 *
 * @property document 文档
 * @property score 相似度分数
 */
data class SearchResult(
    val document: RagDocument,
    val score: Double
)
