package ai.kastrax.rag.retrieval

import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.vectorstore.RagVectorStore
import ai.kastrax.rag.vectorstore.RagVectorStoreFactory
import ai.kastrax.rag.vectorstore.StoreType
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 检索器工厂类，用于创建各种类型的检索器。
 */
object RetrieverFactory {

    /**
     * 创建基本的 Top-K 检索器。
     *
     * @param vectorStore 向量存储
     * @param embeddingService 嵌入服务
     * @return Top-K 检索器
     */
    fun createTopKRetriever(
        vectorStore: RagVectorStore,
        embeddingService: EmbeddingService
    ): Retriever {
        logger.debug { "Creating TopKRetriever" }
        return TopKRetriever(vectorStore, embeddingService)
    }

    /**
     * 创建混合检索器。
     *
     * @param vectorStore 向量存储
     * @param embeddingService 嵌入服务
     * @param config 混合检索配置
     * @return 混合检索器
     */
    fun createHybridRetriever(
        vectorStore: RagVectorStore,
        embeddingService: EmbeddingService,
        config: HybridRetrieverConfig = HybridRetrieverConfig()
    ): Retriever {
        logger.debug { "Creating HybridRetriever with config: $config" }
        return HybridRetriever(vectorStore, embeddingService, config)
    }

    /**
     * 创建向量存储检索器。
     *
     * @param vectorStore 向量存储
     * @param embeddingService 嵌入服务
     * @param defaultLimit 默认返回结果的最大数量
     * @param defaultMinScore 默认最小相似度分数
     * @return 向量存储检索器
     */
    fun createVectorStoreRetriever(
        vectorStore: RagVectorStore,
        embeddingService: EmbeddingService,
        defaultLimit: Int = 5,
        defaultMinScore: Double = 0.0
    ): Retriever {
        logger.debug { "Creating VectorStoreRetriever" }
        return VectorStoreRetriever(vectorStore, embeddingService, defaultLimit, defaultMinScore)
    }

    /**
     * 创建基于内存向量存储的检索器。
     *
     * @param embeddingService 嵌入服务
     * @param retrieverType 检索器类型
     * @return 检索器
     */
    fun createWithInMemoryVectorStore(
        embeddingService: EmbeddingService,
        retrieverType: RetrieverType = RetrieverType.TOP_K
    ): Retriever {
        logger.debug { "Creating retriever with in-memory vector store, type: $retrieverType" }
        val vectorStore = RagVectorStoreFactory.createInMemoryVectorStore()
        return createWithVectorStore(vectorStore, embeddingService, retrieverType)
    }

    /**
     * 创建基于 Chroma 向量存储的检索器。
     *
     * @param embeddingService 嵌入服务
     * @param host Chroma 服务器主机
     * @param port Chroma 服务器端口
     * @param retrieverType 检索器类型
     * @return 检索器
     */
    fun createWithChromaVectorStore(
        embeddingService: EmbeddingService,
        host: String = "localhost",
        port: Int = 8000,
        retrieverType: RetrieverType = RetrieverType.TOP_K
    ): Retriever {
        logger.debug { "Creating retriever with Chroma vector store, type: $retrieverType" }
        val vectorStore = RagVectorStoreFactory.createChromaVectorStore(host, port)
        return createWithVectorStore(vectorStore, embeddingService, retrieverType)
    }

    /**
     * 创建基于 Qdrant 向量存储的检索器。
     *
     * @param embeddingService 嵌入服务
     * @param host Qdrant 服务器主机
     * @param port Qdrant 服务器端口
     * @param retrieverType 检索器类型
     * @return 检索器
     */
    fun createWithQdrantVectorStore(
        embeddingService: EmbeddingService,
        host: String = "localhost",
        port: Int = 6333,
        retrieverType: RetrieverType = RetrieverType.TOP_K
    ): Retriever {
        logger.debug { "Creating retriever with Qdrant vector store, type: $retrieverType" }
        val vectorStore = RagVectorStoreFactory.createQdrantVectorStore(host, port)
        return createWithVectorStore(vectorStore, embeddingService, retrieverType)
    }

    /**
     * 创建基于 PostgreSQL 向量存储的检索器。
     *
     * @param embeddingService 嵌入服务
     * @param jdbcUrl JDBC URL
     * @param username 用户名
     * @param password 密码
     * @param retrieverType 检索器类型
     * @return 检索器
     */
    fun createWithPostgresVectorStore(
        embeddingService: EmbeddingService,
        jdbcUrl: String,
        username: String,
        password: String,
        retrieverType: RetrieverType = RetrieverType.TOP_K
    ): Retriever {
        logger.debug { "Creating retriever with PostgreSQL vector store, type: $retrieverType" }
        val vectorStore = RagVectorStoreFactory.createPostgresVectorStore(jdbcUrl, username, password)
        return createWithVectorStore(vectorStore, embeddingService, retrieverType)
    }

    /**
     * 创建基于指定向量存储的检索器。
     *
     * @param vectorStore 向量存储
     * @param embeddingService 嵌入服务
     * @param retrieverType 检索器类型
     * @return 检索器
     */
    fun createWithVectorStore(
        vectorStore: RagVectorStore,
        embeddingService: EmbeddingService,
        retrieverType: RetrieverType = RetrieverType.TOP_K
    ): Retriever {
        logger.debug { "Creating retriever with vector store, type: $retrieverType" }
        return when (retrieverType) {
            RetrieverType.TOP_K -> createTopKRetriever(vectorStore, embeddingService)
            RetrieverType.HYBRID -> createHybridRetriever(vectorStore, embeddingService)
            RetrieverType.VECTOR_STORE -> createVectorStoreRetriever(vectorStore, embeddingService)
        }
    }

    /**
     * 创建基于指定存储类型的检索器。
     *
     * @param storeType 存储类型
     * @param embeddingService 嵌入服务
     * @param retrieverType 检索器类型
     * @param storeConfig 存储配置
     * @return 检索器
     */
    fun createWithStoreType(
        storeType: StoreType,
        embeddingService: EmbeddingService,
        retrieverType: RetrieverType = RetrieverType.TOP_K,
        storeConfig: Map<String, Any> = emptyMap()
    ): Retriever {
        logger.debug { "Creating retriever with store type: $storeType, retriever type: $retrieverType" }
        val vectorStore = RagVectorStoreFactory.createStoreBackedVectorStore(storeType, storeConfig = storeConfig)
        return createWithVectorStore(vectorStore, embeddingService, retrieverType)
    }
}

/**
 * 检索器类型。
 */
enum class RetrieverType {
    /**
     * 基本的 Top-K 检索器。
     */
    TOP_K,

    /**
     * 混合检索器。
     */
    HYBRID,

    /**
     * 向量存储检索器。
     */
    VECTOR_STORE
}
