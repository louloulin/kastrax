package ai.kastrax.rag.realtime

import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.rag.reranker.Reranker
import ai.kastrax.rag.vectorstore.RagVectorStore

/**
 * 实时 RAG 构建器
 */
class RealTimeRagBuilder {
    private var vectorStore: RagVectorStore? = null
    private var embeddingService: EmbeddingService? = null
    private var reranker: Reranker = IdentityReranker()
    private var config: RealTimeRagConfig = RealTimeRagConfig()

    /**
     * 设置向量存储
     *
     * @param vectorStore 向量存储
     * @return 构建器实例
     */
    fun vectorStore(vectorStore: RagVectorStore): RealTimeRagBuilder {
        this.vectorStore = vectorStore
        return this
    }

    /**
     * 设置嵌入服务
     *
     * @param embeddingService 嵌入服务
     * @return 构建器实例
     */
    fun embeddingService(embeddingService: EmbeddingService): RealTimeRagBuilder {
        this.embeddingService = embeddingService
        return this
    }

    /**
     * 设置重排序器
     *
     * @param reranker 重排序器
     * @return 构建器实例
     */
    fun reranker(reranker: Reranker): RealTimeRagBuilder {
        this.reranker = reranker
        return this
    }

    /**
     * 设置配置
     *
     * @param config 配置
     * @return 构建器实例
     */
    fun config(config: RealTimeRagConfig): RealTimeRagBuilder {
        this.config = config
        return this
    }

    /**
     * 配置实时 RAG
     *
     * @param configure 配置函数
     * @return 构建器实例
     */
    fun config(configure: RealTimeRagConfig.() -> Unit): RealTimeRagBuilder {
        val newConfig = config.copy()
        newConfig.configure()
        this.config = newConfig
        return this
    }

    /**
     * 构建实时 RAG 实例
     *
     * @return 实时 RAG 实例
     * @throws IllegalStateException 如果缺少必要的组件
     */
    fun build(): RealTimeRag {
        val vectorStore = this.vectorStore
            ?: throw IllegalStateException("向量存储不能为空")
        
        val embeddingService = this.embeddingService
            ?: throw IllegalStateException("嵌入服务不能为空")
        
        return RealTimeRag(
            vectorStore = vectorStore,
            embeddingService = embeddingService,
            reranker = reranker,
            config = config
        )
    }
}

/**
 * 创建实时 RAG 构建器
 *
 * @return 实时 RAG 构建器
 */
fun realTimeRag(): RealTimeRagBuilder {
    return RealTimeRagBuilder()
}

/**
 * 创建实时 RAG 实例
 *
 * @param configure 配置函数
 * @return 实时 RAG 实例
 */
fun realTimeRag(configure: RealTimeRagBuilder.() -> Unit): RealTimeRag {
    val builder = RealTimeRagBuilder()
    builder.configure()
    return builder.build()
}
