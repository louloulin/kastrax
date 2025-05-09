package ai.kastrax.rag.vectorstore

import ai.kastrax.rag.embedding.EmbeddingService

/**
 * RAG 向量存储接口，用于存储和检索文档及其嵌入向量。
 */
interface RagVectorStore {
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
     * 添加文档到向量存储。
     *
     * @param document 文档
     * @param embedding 文档的嵌入向量
     * @param metadata 文档元数据
     * @return 文档 ID
     */
    suspend fun addDocument(
        document: String,
        embedding: FloatArray,
        metadata: Map<String, String> = emptyMap()
    ): String

    /**
     * 添加文档到向量存储，并使用嵌入服务计算嵌入向量。
     *
     * @param document 文档
     * @param embeddingService 嵌入服务
     * @param metadata 文档元数据
     * @return 文档 ID
     */
    suspend fun addDocument(
        document: String,
        embeddingService: EmbeddingService,
        metadata: Map<String, String> = emptyMap()
    ): String

    /**
     * 批量添加文档到向量存储。
     *
     * @param documents 文档列表
     * @param embeddings 嵌入向量列表
     * @param metadataList 元数据列表
     * @return 文档 ID 列表
     */
    suspend fun addDocuments(
        documents: List<String>,
        embeddings: List<FloatArray>,
        metadataList: List<Map<String, String>> = List(documents.size) { emptyMap() }
    ): List<String>

    /**
     * 批量添加文档到向量存储，并使用嵌入服务计算嵌入向量。
     *
     * @param documents 文档列表
     * @param embeddingService 嵌入服务
     * @param metadataList 元数据列表
     * @return 文档 ID 列表
     */
    suspend fun addDocuments(
        documents: List<String>,
        embeddingService: EmbeddingService,
        metadataList: List<Map<String, String>> = List(documents.size) { emptyMap() }
    ): List<String>

    /**
     * 根据 ID 获取文档。
     *
     * @param id 文档 ID
     * @return 文档，如果不存在则返回 null
     */
    suspend fun getDocument(id: String): RagDocument?

    /**
     * 根据内容获取文档。
     *
     * @param content 文档内容
     * @return 文档，如果不存在则返回 null
     */
    suspend fun getDocumentByContent(content: String): RagDocument?

    /**
     * 根据 ID 获取文档的嵌入向量。
     *
     * @param id 文档 ID
     * @return 嵌入向量，如果不存在则返回 null
     */
    suspend fun getEmbedding(id: String): FloatArray?

    /**
     * 根据 ID 删除文档。
     *
     * @param id 文档 ID
     * @return 是否成功删除
     */
    suspend fun deleteDocument(id: String): Boolean

    /**
     * 清空向量存储。
     */
    suspend fun clear()

    /**
     * 获取向量存储中的文档数量。
     *
     * @return 文档数量
     */
    suspend fun size(): Int

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
    ): List<ai.kastrax.rag.document.RagDocument>

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
    ): List<ai.kastrax.rag.document.RagDocument>

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
    ): List<ai.kastrax.rag.document.RagDocument>

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
    ): List<ai.kastrax.rag.document.RagDocument>

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
    ): List<ai.kastrax.rag.document.RagDocument>
}
