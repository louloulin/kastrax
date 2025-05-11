package ai.kastrax.rag.realtime

import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.rag.reranker.Reranker
import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.embedding.EmbeddingService

/**
 * 实时 RAG 构建器，用于构建 RealTimeRag 实例。
 */
class RealTimeRagBuilder {
    private var documentStore: DocumentVectorStore? = null
    private var embeddingService: EmbeddingService? = null
    private var reranker: Reranker? = null
    private var config: RealTimeRagConfig? = null

    /**
     * 设置文档向量存储。
     *
     * @param documentStore 文档向量存储
     * @return 构建器实例
     */
    fun withDocumentStore(documentStore: DocumentVectorStore): RealTimeRagBuilder {
        this.documentStore = documentStore
        return this
    }

    /**
     * 设置嵌入服务。
     *
     * @param embeddingService 嵌入服务
     * @return 构建器实例
     */
    fun withEmbeddingService(embeddingService: EmbeddingService): RealTimeRagBuilder {
        this.embeddingService = embeddingService
        return this
    }

    /**
     * 设置重排序器。
     *
     * @param reranker 重排序器
     * @return 构建器实例
     */
    fun withReranker(reranker: Reranker): RealTimeRagBuilder {
        this.reranker = reranker
        return this
    }

    /**
     * 设置配置。
     *
     * @param config 实时 RAG 配置
     * @return 构建器实例
     */
    fun withConfig(config: RealTimeRagConfig): RealTimeRagBuilder {
        this.config = config
        return this
    }

    /**
     * 构建 RealTimeRag 实例。
     *
     * @return RealTimeRag 实例
     * @throws IllegalStateException 如果必要的组件未设置
     */
    fun build(): RealTimeRag {
        val store = documentStore ?: throw IllegalStateException("DocumentVectorStore must be set")
        val embedding = embeddingService ?: throw IllegalStateException("EmbeddingService must be set")
        val rerankService = reranker ?: IdentityReranker()
        val ragConfig = config ?: RealTimeRagConfig()

        return RealTimeRag(store, embedding, rerankService, ragConfig)
    }
}
