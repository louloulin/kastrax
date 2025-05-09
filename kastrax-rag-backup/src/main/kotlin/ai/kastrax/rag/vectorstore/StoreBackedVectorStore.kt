package ai.kastrax.rag.vectorstore

import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.store.VectorStore
import ai.kastrax.rag.adapter.RagVectorStoreAdapter
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 基于 kastrax-store 模块的向量存储实现。
 * 这个类是一个桥接器，将 RAG 模块与 store 模块连接起来。
 *
 * @property vectorStore 向量存储
 * @property indexName 索引名称
 * @property dimension 向量维度
 */
class StoreBackedVectorStore(
    private val vectorStore: VectorStore,
    private val indexName: String = "rag_index",
    override val dimension: Int = 1536
) : RagVectorStore {

    // 内部使用的 RagVectorStore 实例
    private val delegate: RagVectorStore

    init {
        // 创建适配器
        delegate = RagVectorStoreAdapter(vectorStore, indexName, dimension)
        logger.info { "Created StoreBackedVectorStore with index: $indexName, dimension: $dimension" }
    }

    override suspend fun addDocument(
        document: String,
        embedding: FloatArray,
        metadata: Map<String, String>
    ): String {
        return delegate.addDocument(document, embedding, metadata)
    }

    override suspend fun addDocument(
        document: String,
        embeddingService: EmbeddingService,
        metadata: Map<String, String>
    ): String {
        return delegate.addDocument(document, embeddingService, metadata)
    }

    override suspend fun addDocuments(
        documents: List<String>,
        embeddings: List<FloatArray>,
        metadataList: List<Map<String, String>>
    ): List<String> {
        return delegate.addDocuments(documents, embeddings, metadataList)
    }

    override suspend fun addDocuments(
        documents: List<String>,
        embeddingService: EmbeddingService,
        metadataList: List<Map<String, String>>
    ): List<String> {
        return delegate.addDocuments(documents, embeddingService, metadataList)
    }

    override suspend fun getDocument(id: String): RagDocument? {
        return delegate.getDocument(id)
    }

    override suspend fun getDocumentByContent(content: String): RagDocument? {
        return delegate.getDocumentByContent(content)
    }

    override suspend fun getEmbedding(id: String): FloatArray? {
        return delegate.getEmbedding(id)
    }

    override suspend fun deleteDocument(id: String): Boolean {
        return delegate.deleteDocument(id)
    }

    override suspend fun clear() {
        delegate.clear()
    }

    override suspend fun size(): Int {
        return delegate.size()
    }

    /**
     * 获取底层向量存储。
     *
     * @return 向量存储
     */
    override fun getVectorStore(): ai.kastrax.store.VectorStore {
        return vectorStore
    }

    override suspend fun similaritySearch(
        query: String,
        embeddingService: EmbeddingService,
        limit: Int
    ): List<ai.kastrax.rag.document.RagDocument> {
        // 获取查询的嵌入向量
        val embedding = embeddingService.embed(query)
        return similaritySearch(embedding, limit)
    }

    override suspend fun similaritySearch(
        embedding: FloatArray,
        limit: Int
    ): List<ai.kastrax.rag.document.RagDocument> {
        // 使用向量存储进行查询
        val results = vectorStore.query(indexName, embedding, limit)

        // 将查询结果转换为 RagDocument
        return results.map { result ->
            ai.kastrax.rag.document.RagDocument(
                id = result.id,
                content = result.metadata?.get("content") as? String ?: "",
                metadata = result.metadata?.mapValues { it.value } ?: emptyMap(),
                embedding = result.vector
            )
        }
    }

    override suspend fun similaritySearchWithFilter(
        embedding: FloatArray,
        filter: Map<String, Any>,
        limit: Int
    ): List<ai.kastrax.rag.document.RagDocument> {
        // 使用向量存储进行查询
        val results = vectorStore.query(indexName, embedding, limit, filter)

        // 将查询结果转换为 RagDocument
        return results.map { result ->
            ai.kastrax.rag.document.RagDocument(
                id = result.id,
                content = result.metadata?.get("content") as? String ?: "",
                metadata = result.metadata?.mapValues { it.value } ?: emptyMap(),
                embedding = result.vector
            )
        }
    }

    override suspend fun keywordSearch(
        keywords: List<String>,
        limit: Int
    ): List<ai.kastrax.rag.document.RagDocument> {
        // 将关键词转换为过滤器
        val filter = mapOf("keywords" to keywords)

        // 使用元数据搜索
        return metadataSearch(filter, limit)
    }

    override suspend fun metadataSearch(
        filter: Map<String, Any>,
        limit: Int
    ): List<ai.kastrax.rag.document.RagDocument> {
        // 创建测试向量（用于元数据搜索）
        val testVector = FloatArray(dimension) { 0f }
        testVector[0] = 1f

        // 使用向量存储进行查询
        val results = vectorStore.query(indexName, testVector, limit, filter)

        // 将查询结果转换为 RagDocument
        return results.map { result ->
            ai.kastrax.rag.document.RagDocument(
                id = result.id,
                content = result.metadata?.get("content") as? String ?: "",
                metadata = result.metadata?.mapValues { it.value } ?: emptyMap(),
                embedding = result.vector
            )
        }
    }
}


