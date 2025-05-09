package ai.kastrax.rag.vectorstore

import ai.kastrax.store.VectorStoreFactory
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * RAG 向量存储工厂类。
 * 用于创建各种类型的 RAG 向量存储。
 */
object RagVectorStoreFactory {

    /**
     * 创建内存 RAG 向量存储。
     *
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @return 内存 RAG 向量存储
     */
    fun createInMemoryVectorStore(
        indexName: String = "rag_index",
        dimension: Int = 1536
    ): RagVectorStore {
        logger.debug { "Creating in-memory RAG vector store with indexName=$indexName, dimension=$dimension" }
        val vectorStore = VectorStoreFactory.createInMemoryVectorStore()
        return StoreBackedVectorStore(vectorStore, indexName, dimension)
    }

    /**
     * 创建 Chroma RAG 向量存储。
     *
     * @param host 主机地址
     * @param port 端口
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @return Chroma RAG 向量存储
     */
    fun createChromaVectorStore(
        host: String = "localhost",
        port: Int = 8000,
        indexName: String = "rag_index",
        dimension: Int = 1536
    ): RagVectorStore {
        logger.debug { "Creating Chroma RAG vector store with host=$host, port=$port, indexName=$indexName, dimension=$dimension" }
        val vectorStore = VectorStoreFactory.createChromaVectorStore(host, port)
        return StoreBackedVectorStore(vectorStore, indexName, dimension)
    }

    /**
     * 创建 Qdrant RAG 向量存储。
     *
     * @param host 主机地址
     * @param port 端口
     * @param apiKey API 密钥
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @return Qdrant RAG 向量存储
     */
    fun createQdrantVectorStore(
        host: String = "localhost",
        port: Int = 6333,
        apiKey: String? = null,
        indexName: String = "rag_index",
        dimension: Int = 1536
    ): RagVectorStore {
        logger.debug { "Creating Qdrant RAG vector store with host=$host, port=$port, indexName=$indexName, dimension=$dimension" }
        val vectorStore = VectorStoreFactory.createQdrantVectorStore(host, port, apiKey)
        return StoreBackedVectorStore(vectorStore, indexName, dimension)
    }

    /**
     * 创建 PostgreSQL RAG 向量存储。
     *
     * @param url 数据库 URL
     * @param username 用户名
     * @param password 密码
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @return PostgreSQL RAG 向量存储
     */
    fun createPostgresVectorStore(
        url: String,
        username: String,
        password: String,
        indexName: String = "rag_index",
        dimension: Int = 1536
    ): RagVectorStore {
        logger.debug { "Creating PostgreSQL RAG vector store with url=$url, indexName=$indexName, dimension=$dimension" }
        val vectorStore = VectorStoreFactory.createPostgresVectorStore(url, username, password)
        return StoreBackedVectorStore(vectorStore, indexName, dimension)
    }

    /**
     * 创建 Pinecone RAG 向量存储。
     *
     * @param apiKey API 密钥
     * @param environment 环境
     * @param projectId 项目 ID
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @return Pinecone RAG 向量存储
     */
    fun createPineconeVectorStore(
        apiKey: String,
        environment: String,
        projectId: String,
        indexName: String = "rag_index",
        dimension: Int = 1536
    ): RagVectorStore {
        logger.debug { "Creating Pinecone RAG vector store with environment=$environment, projectId=$projectId, indexName=$indexName, dimension=$dimension" }
        val vectorStore = VectorStoreFactory.createPineconeVectorStore(apiKey, environment, projectId)
        return StoreBackedVectorStore(vectorStore, indexName, dimension)
    }

    /**
     * 创建 MongoDB RAG 向量存储。
     *
     * @param connectionString 连接字符串
     * @param databaseName 数据库名称
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @return MongoDB RAG 向量存储
     */
    fun createMongoDBVectorStore(
        connectionString: String,
        databaseName: String,
        indexName: String = "rag_index",
        dimension: Int = 1536
    ): RagVectorStore {
        logger.debug { "Creating MongoDB RAG vector store with databaseName=$databaseName, indexName=$indexName, dimension=$dimension" }
        val vectorStore = VectorStoreFactory.createMongoDBVectorStore(connectionString, databaseName)
        return StoreBackedVectorStore(vectorStore, indexName, dimension)
    }

    /**
     * 创建 LanceDB RAG 向量存储。
     *
     * @param uri LanceDB URI
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @return LanceDB RAG 向量存储
     */
    fun createLanceDBVectorStore(
        uri: String,
        indexName: String = "rag_index",
        dimension: Int = 1536
    ): RagVectorStore {
        logger.debug { "Creating LanceDB RAG vector store with uri=$uri, indexName=$indexName, dimension=$dimension" }
        val vectorStore = VectorStoreFactory.createLanceDBVectorStore(uri)
        return StoreBackedVectorStore(vectorStore, indexName, dimension)
    }

    /**
     * 从 VectorStore 创建 RAG 向量存储。
     *
     * @param vectorStore 向量存储
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @return RAG 向量存储
     */
    fun fromVectorStore(
        vectorStore: ai.kastrax.store.VectorStore,
        indexName: String = "rag_index",
        dimension: Int = 1536
    ): RagVectorStore {
        logger.debug { "Creating RAG vector store from VectorStore with indexName=$indexName, dimension=$dimension" }
        return StoreBackedVectorStore(vectorStore, indexName, dimension)
    }
}
