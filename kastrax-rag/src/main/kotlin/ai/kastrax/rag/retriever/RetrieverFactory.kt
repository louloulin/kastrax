package ai.kastrax.rag.retriever

import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.vectorstore.RagVectorStore
import ai.kastrax.rag.vectorstore.RagVectorStoreFactory
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 检索器工厂类。
 * 用于创建各种类型的检索器。
 */
object RetrieverFactory {

    /**
     * 创建向量存储检索器。
     *
     * @param vectorStore 向量存储
     * @param embeddingService 嵌入服务
     * @return 向量存储检索器
     */
    fun createVectorStoreRetriever(
        vectorStore: RagVectorStore,
        embeddingService: EmbeddingService
    ): VectorStoreRetriever {
        logger.debug { "Creating vector store retriever" }
        return VectorStoreRetriever(vectorStore, embeddingService)
    }

    /**
     * 创建内存向量存储检索器。
     *
     * @param embeddingService 嵌入服务
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @return 向量存储检索器
     */
    fun createInMemoryRetriever(
        embeddingService: EmbeddingService,
        indexName: String = "rag_index",
        dimension: Int = 1536
    ): VectorStoreRetriever {
        logger.debug { "Creating in-memory retriever with indexName=$indexName, dimension=$dimension" }
        val vectorStore = RagVectorStoreFactory.createInMemoryVectorStore(indexName, dimension)
        return VectorStoreRetriever(vectorStore, embeddingService)
    }

    /**
     * 创建 Chroma 向量存储检索器。
     *
     * @param embeddingService 嵌入服务
     * @param host 主机地址
     * @param port 端口
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @return 向量存储检索器
     */
    fun createChromaRetriever(
        embeddingService: EmbeddingService,
        host: String = "localhost",
        port: Int = 8000,
        indexName: String = "rag_index",
        dimension: Int = 1536
    ): VectorStoreRetriever {
        logger.debug { "Creating Chroma retriever with host=$host, port=$port, indexName=$indexName, dimension=$dimension" }
        val vectorStore = RagVectorStoreFactory.createChromaVectorStore(host, port, indexName, dimension)
        return VectorStoreRetriever(vectorStore, embeddingService)
    }

    /**
     * 创建 Qdrant 向量存储检索器。
     *
     * @param embeddingService 嵌入服务
     * @param host 主机地址
     * @param port 端口
     * @param apiKey API 密钥
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @return 向量存储检索器
     */
    fun createQdrantRetriever(
        embeddingService: EmbeddingService,
        host: String = "localhost",
        port: Int = 6333,
        apiKey: String? = null,
        indexName: String = "rag_index",
        dimension: Int = 1536
    ): VectorStoreRetriever {
        logger.debug { "Creating Qdrant retriever with host=$host, port=$port, indexName=$indexName, dimension=$dimension" }
        val vectorStore = RagVectorStoreFactory.createQdrantVectorStore(host, port, apiKey, indexName, dimension)
        return VectorStoreRetriever(vectorStore, embeddingService)
    }

    /**
     * 创建 PostgreSQL 向量存储检索器。
     *
     * @param embeddingService 嵌入服务
     * @param url 数据库 URL
     * @param username 用户名
     * @param password 密码
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @return 向量存储检索器
     */
    fun createPostgresRetriever(
        embeddingService: EmbeddingService,
        url: String,
        username: String,
        password: String,
        indexName: String = "rag_index",
        dimension: Int = 1536
    ): VectorStoreRetriever {
        logger.debug { "Creating PostgreSQL retriever with url=$url, indexName=$indexName, dimension=$dimension" }
        val vectorStore = RagVectorStoreFactory.createPostgresVectorStore(url, username, password, indexName, dimension)
        return VectorStoreRetriever(vectorStore, embeddingService)
    }

    /**
     * 创建 Pinecone 向量存储检索器。
     *
     * @param embeddingService 嵌入服务
     * @param apiKey API 密钥
     * @param environment 环境
     * @param projectId 项目 ID
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @return 向量存储检索器
     */
    fun createPineconeRetriever(
        embeddingService: EmbeddingService,
        apiKey: String,
        environment: String,
        projectId: String,
        indexName: String = "rag_index",
        dimension: Int = 1536
    ): VectorStoreRetriever {
        logger.debug { "Creating Pinecone retriever with environment=$environment, projectId=$projectId, indexName=$indexName, dimension=$dimension" }
        val vectorStore = RagVectorStoreFactory.createPineconeVectorStore(apiKey, environment, projectId, indexName, dimension)
        return VectorStoreRetriever(vectorStore, embeddingService)
    }

    /**
     * 创建 MongoDB 向量存储检索器。
     *
     * @param embeddingService 嵌入服务
     * @param connectionString 连接字符串
     * @param databaseName 数据库名称
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @return 向量存储检索器
     */
    fun createMongoDBRetriever(
        embeddingService: EmbeddingService,
        connectionString: String,
        databaseName: String,
        indexName: String = "rag_index",
        dimension: Int = 1536
    ): VectorStoreRetriever {
        logger.debug { "Creating MongoDB retriever with databaseName=$databaseName, indexName=$indexName, dimension=$dimension" }
        val vectorStore = RagVectorStoreFactory.createMongoDBVectorStore(connectionString, databaseName, indexName, dimension)
        return VectorStoreRetriever(vectorStore, embeddingService)
    }

    /**
     * 创建 LanceDB 向量存储检索器。
     *
     * @param embeddingService 嵌入服务
     * @param uri LanceDB URI
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @return 向量存储检索器
     */
    fun createLanceDBRetriever(
        embeddingService: EmbeddingService,
        uri: String,
        indexName: String = "rag_index",
        dimension: Int = 1536
    ): VectorStoreRetriever {
        logger.debug { "Creating LanceDB retriever with uri=$uri, indexName=$indexName, dimension=$dimension" }
        val vectorStore = RagVectorStoreFactory.createLanceDBVectorStore(uri, indexName, dimension)
        return VectorStoreRetriever(vectorStore, embeddingService)
    }

    /**
     * 创建混合检索器。
     *
     * @param vectorStoreRetriever 向量存储检索器
     * @param keywordRetriever 关键词检索器
     * @param vectorWeight 向量权重
     * @param keywordWeight 关键词权重
     * @return 混合检索器
     */
    fun createHybridRetriever(
        vectorStoreRetriever: VectorStoreRetriever,
        keywordRetriever: KeywordRetriever,
        vectorWeight: Double = 0.7,
        keywordWeight: Double = 0.3
    ): HybridRetriever {
        logger.debug { "Creating hybrid retriever with vectorWeight=$vectorWeight, keywordWeight=$keywordWeight" }
        return HybridRetriever(vectorStoreRetriever, keywordRetriever, vectorWeight, keywordWeight)
    }
}
