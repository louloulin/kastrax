package ai.kastrax.rag.store

import ai.kastrax.store.VectorStore
import ai.kastrax.store.VectorStoreFactory as StoreVectorStoreFactory
import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.adapter.DocumentVectorStoreAdapter as StoreDocumentVectorStoreAdapter
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 向量存储工厂类，用于创建向量存储实例。
 */
object VectorStoreFactory {

    /**
     * 创建内存向量存储。
     *
     * @return 内存向量存储
     */
    fun createInMemoryVectorStore(): VectorStore {
        logger.debug { "Creating in-memory vector store" }
        return StoreVectorStoreFactory.createInMemoryVectorStore()
    }

    /**
     * 创建 Pinecone 向量存储。
     *
     * @param apiKey API 密钥
     * @param environment 环境
     * @param projectId 项目 ID
     * @return Pinecone 向量存储
     */
    fun createPineconeVectorStore(
        apiKey: String,
        environment: String,
        projectId: String
    ): VectorStore {
        logger.debug { "Creating Pinecone vector store with environment=$environment, projectId=$projectId" }
        return StoreVectorStoreFactory.createPineconeVectorStore(apiKey, environment, projectId)
    }

    /**
     * 创建 Qdrant 向量存储。
     *
     * @param url Qdrant 服务器 URL
     * @param apiKey API 密钥
     * @return Qdrant 向量存储
     */
    fun createQdrantVectorStore(
        url: String,
        apiKey: String? = null
    ): VectorStore {
        logger.debug { "Creating Qdrant vector store with url=$url" }
        return StoreVectorStoreFactory.createQdrantVectorStore(url, apiKey)
    }

    /**
     * 创建 Chroma 向量存储。
     *
     * @param url Chroma 服务器 URL
     * @return Chroma 向量存储
     */
    fun createChromaVectorStore(
        url: String
    ): VectorStore {
        logger.debug { "Creating Chroma vector store with url=$url" }
        return StoreVectorStoreFactory.createChromaVectorStore(url)
    }

    /**
     * 创建 Milvus 向量存储。
     *
     * @param host Milvus 主机
     * @param port Milvus 端口
     * @return Milvus 向量存储
     */
    fun createMilvusVectorStore(
        host: String,
        port: Int
    ): VectorStore {
        logger.debug { "Creating Milvus vector store with host=$host, port=$port" }
        return StoreVectorStoreFactory.createMilvusVectorStore(host, port)
    }

    /**
     * 创建 Weaviate 向量存储。
     *
     * @param url Weaviate 服务器 URL
     * @param apiKey API 密钥
     * @return Weaviate 向量存储
     */
    fun createWeaviateVectorStore(
        url: String,
        apiKey: String? = null
    ): VectorStore {
        logger.debug { "Creating Weaviate vector store with url=$url" }
        return StoreVectorStoreFactory.createWeaviateVectorStore(url, apiKey)
    }

    /**
     * 将向量存储适配为文档向量存储。
     *
     * @param vectorStore 向量存储
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @return 文档向量存储
     */
    fun adaptToDocumentVectorStore(
        vectorStore: VectorStore,
        indexName: String = "document_index",
        dimension: Int = 1536
    ): DocumentVectorStore {
        logger.debug { "Adapting vector store to document vector store with indexName=$indexName, dimension=$dimension" }
        return StoreDocumentVectorStoreAdapter(vectorStore, indexName, dimension)
    }

    /**
     * 将向量存储适配为文档存储。
     *
     * @param vectorStore 向量存储
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @return 文档存储
     */
    fun adaptToDocumentStore(
        vectorStore: VectorStore,
        indexName: String = "document_index",
        dimension: Int = 1536
    ): DocumentStore {
        logger.debug { "Adapting vector store to document store with indexName=$indexName, dimension=$dimension" }
        val documentVectorStore = adaptToDocumentVectorStore(vectorStore, indexName, dimension)
        return DocumentVectorStoreAdapter(documentVectorStore)
    }
}
