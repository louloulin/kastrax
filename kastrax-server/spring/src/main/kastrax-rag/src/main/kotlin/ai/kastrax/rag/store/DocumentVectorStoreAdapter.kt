package ai.kastrax.rag.store

import ai.kastrax.store.VectorStore
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 文档向量存储适配器，将 DocumentVectorStore 适配为 DocumentStore。
 *
 * @property documentVectorStore 文档向量存储
 */
class DocumentVectorStoreAdapter(
    private val documentVectorStore: DocumentVectorStore
) : DocumentStore {

    /**
     * 添加文档。
     *
     * @param documents 文档列表
     * @param embeddingService 嵌入服务
     * @return 是否成功添加
     */
    override suspend fun addDocuments(
        documents: List<Document>,
        embeddingService: EmbeddingService
    ): Boolean {
        return documentVectorStore.addDocuments(documents, embeddingService)
    }

    /**
     * 添加文档（已嵌入）。
     *
     * @param documents 文档列表
     * @param embeddings 嵌入向量列表
     * @return 是否成功添加
     */
    override suspend fun addDocuments(
        documents: List<Document>,
        embeddings: List<FloatArray>
    ): Boolean {
        // 将文档和嵌入向量添加到文档向量存储
        // 注意：这里需要实现一个自定义方法，因为 DocumentVectorStore 接口没有直接提供这个方法
        // 这里我们使用一个简单的实现，实际应用中可能需要更复杂的逻辑
        if (documents.size != embeddings.size) {
            throw IllegalArgumentException("Documents and embeddings must have the same size")
        }

        // 创建已嵌入的文档
        val embeddedDocuments = documents.mapIndexed { index, document ->
            Document(
                id = document.id,
                content = document.content,
                metadata = document.metadata + mapOf("embedding" to embeddings[index])
            )
        }

        // 添加文档
        return documentVectorStore.addDocuments(embeddedDocuments)
    }

    /**
     * 删除文档。
     *
     * @param ids 文档 ID 列表
     * @return 是否成功删除
     */
    override suspend fun deleteDocuments(ids: List<String>): Boolean {
        return documentVectorStore.deleteDocuments(ids)
    }

    /**
     * 相似度搜索。
     *
     * @param query 查询文本
     * @param embeddingService 嵌入服务
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 搜索结果列表
     */
    override suspend fun search(
        query: String,
        embeddingService: EmbeddingService,
        limit: Int,
        minScore: Double
    ): List<DocumentSearchResult> {
        return documentVectorStore.similaritySearch(query, embeddingService, limit)
            .filter { it.score >= minScore }
    }

    /**
     * 相似度搜索。
     *
     * @param embedding 查询嵌入向量
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 搜索结果列表
     */
    override suspend fun search(
        embedding: FloatArray,
        limit: Int,
        minScore: Double
    ): List<DocumentSearchResult> {
        return documentVectorStore.similaritySearch(embedding, limit)
            .filter { it.score >= minScore }
    }

    /**
     * 关键词搜索。
     *
     * @param keywords 关键词列表
     * @param limit 返回结果的最大数量
     * @return 搜索结果列表
     */
    override suspend fun keywordSearch(
        keywords: List<String>,
        limit: Int
    ): List<DocumentSearchResult> {
        return documentVectorStore.keywordSearch(keywords, limit)
    }

    /**
     * 元数据搜索。
     *
     * @param filter 过滤条件
     * @param limit 返回结果的最大数量
     * @return 搜索结果列表
     */
    override suspend fun metadataSearch(
        filter: Map<String, Any>,
        limit: Int
    ): List<DocumentSearchResult> {
        // 使用向量存储的元数据搜索
        // 注意：这里我们使用一个简单的实现，实际应用中可能需要更复杂的逻辑
        val embedding = FloatArray(documentVectorStore.dimension) { 0f }
        return documentVectorStore.similaritySearchWithFilter(embedding, filter, limit)
    }

    /**
     * 清空存储。
     */
    override suspend fun clear() {
        // 清空存储
        // 注意：DocumentVectorStore 接口没有直接提供 clear 方法
        // 这里我们可能需要使用底层的 VectorStore 来实现
        val vectorStore = documentVectorStore.getVectorStore()
        // 假设 VectorStore 有一个 clear 方法
        // vectorStore.clear()
        logger.warn { "Clear operation is not fully implemented for DocumentVectorStoreAdapter" }
    }
}
