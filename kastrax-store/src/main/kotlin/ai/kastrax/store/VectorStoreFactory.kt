package ai.kastrax.store

import ai.kastrax.store.adapter.DocumentVectorStoreAdapter
import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.lancedb.LanceDBVectorStore
import ai.kastrax.store.metrics.MetricsVectorStore
import ai.kastrax.store.mongodb.MongoDBVectorStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 向量存储工厂类，用于创建不同类型的向量存储实现。
 */
object VectorStoreFactory {

    /**
     * 创建内存向量存储。
     *
     * @return 内存向量存储
     */
    fun createInMemoryVectorStore(): VectorStore {
        logger.debug { "Creating in-memory vector store" }
        return Class.forName("ai.kastrax.store.memory.InMemoryVectorStore").getDeclaredConstructor().newInstance() as VectorStore
    }

    /**
     * 创建 Chroma 向量存储。
     *
     * @param host Chroma 服务器主机
     * @param port Chroma 服务器端口
     * @param apiPath API 路径
     * @param tenant 租户
     * @param database 数据库
     * @return Chroma 向量存储
     */
    fun createChromaVectorStore(
        host: String = "localhost",
        port: Int = 8000,
        apiPath: String = "",
        tenant: String = "default_tenant",
        database: String = "default_database"
    ): VectorStore {
        logger.debug { "Creating Chroma vector store with host=$host, port=$port" }
        return Class.forName("ai.kastrax.store.chroma.ChromaVectorStore")
            .getDeclaredConstructor(String::class.java, Int::class.java, String::class.java, String::class.java, String::class.java)
            .newInstance(host, port, apiPath, tenant, database) as VectorStore
    }

    /**
     * 创建 Pinecone 向量存储。
     *
     * @param apiKey Pinecone API 密钥
     * @param environment Pinecone 环境
     * @param projectId Pinecone 项目 ID
     * @return Pinecone 向量存储
     */
    fun createPineconeVectorStore(
        apiKey: String,
        environment: String,
        projectId: String
    ): VectorStore {
        logger.debug { "Creating Pinecone vector store with environment=$environment, projectId=$projectId" }
        return Class.forName("ai.kastrax.store.pinecone.PineconeVectorStore")
            .getDeclaredConstructor(String::class.java, String::class.java, String::class.java)
            .newInstance(apiKey, environment, projectId) as VectorStore
    }

    /**
     * 创建 MongoDB 向量存储。
     *
     * @param connectionString MongoDB 连接字符串
     * @param databaseName 数据库名称
     * @return MongoDB 向量存储
     */
    fun createMongoDBVectorStore(
        connectionString: String,
        databaseName: String
    ): VectorStore {
        logger.debug { "Creating MongoDB vector store with database=$databaseName" }
        return MongoDBVectorStore(connectionString, databaseName)
    }

    /**
     * 创建 LanceDB 向量存储。
     *
     * @param uri LanceDB URI，可以是本地路径或远程 URI
     * @return LanceDB 向量存储
     */
    fun createLanceDBVectorStore(
        uri: String
    ): LanceDBVectorStore {
        logger.debug { "Creating LanceDB vector store with uri=$uri" }
        return LanceDBVectorStore(uri)
    }

    /**
     * 创建 ANN 索引。
     * 扩展方法，用于创建 LanceDB 的 ANN 索引。
     *
     * @param indexName 索引名称
     * @param indexType 索引类型
     * @param params 索引参数
     * @return 是否成功创建
     */
    suspend fun VectorStore.createAnnIndex(
        indexName: String,
        indexType: String = "ivf_pq",
        params: Map<String, Any> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        if (this@createAnnIndex is LanceDBVectorStore) {
            return@withContext this@createAnnIndex.createAnnIndex(indexName, indexType, params)
        } else {
            logger.warn { "createAnnIndex is only supported for LanceDBVectorStore" }
            return@withContext false
        }
    }

    /**
     * 创建 Qdrant 向量存储。
     *
     * @param host Qdrant 服务器主机
     * @param port Qdrant 服务器端口
     * @param apiKey Qdrant API 密钥
     * @param useTls 是否使用 TLS
     * @return Qdrant 向量存储
     */
    fun createQdrantVectorStore(
        host: String = "localhost",
        port: Int = 6333,
        apiKey: String? = null,
        useTls: Boolean = false
    ): VectorStore {
        logger.debug { "Creating Qdrant vector store with host=$host, port=$port" }
        return Class.forName("ai.kastrax.store.qdrant.QdrantVectorStore")
            .getDeclaredConstructor(String::class.java, Int::class.java, String::class.java, Boolean::class.java)
            .newInstance(host, port, apiKey, useTls) as VectorStore
    }

    /**
     * 创建 PostgreSQL 向量存储。
     *
     * @param jdbcUrl JDBC URL
     * @param username 用户名
     * @param password 密码
     * @param schema 模式
     * @return PostgreSQL 向量存储
     */
    fun createPostgresVectorStore(
        jdbcUrl: String,
        username: String,
        password: String,
        schema: String = "public"
    ): VectorStore {
        logger.debug { "Creating PostgreSQL vector store with jdbcUrl=$jdbcUrl, schema=$schema" }
        return Class.forName("ai.kastrax.store.postgres.PostgresVectorStore")
            .getDeclaredConstructor(String::class.java, String::class.java, String::class.java, String::class.java)
            .newInstance(jdbcUrl, username, password, schema) as VectorStore
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
        return DocumentVectorStoreAdapter(vectorStore, indexName, dimension)
    }

    /**
     * 创建带有指标收集功能的向量存储。
     *
     * @param vectorStore 向量存储
     * @return 带有指标收集功能的向量存储
     */
    fun withMetrics(vectorStore: VectorStore): VectorStore {
        logger.debug { "Adding metrics collection to vector store" }
        return MetricsVectorStore(vectorStore)
    }
}
