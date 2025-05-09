package ai.kastrax.rag.multimodal

import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.rag.reranker.Reranker
import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.VectorStore
import ai.kastrax.store.VectorStoreFactory
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 多模态 RAG 工厂类，用于创建多模态 RAG 实例。
 */
object MultimodalRagFactory {

    /**
     * 创建基于 OpenAI 的多模态 RAG 实例。
     *
     * @param apiKey OpenAI API 密钥
     * @param embeddingModel 嵌入模型
     * @param dimensions 嵌入向量维度
     * @param visionModel 视觉模型
     * @param audioModel 音频模型
     * @param vectorStore 向量存储
     * @param reranker 重排序器
     * @param defaultOptions 默认选项
     * @return 多模态 RAG 实例
     */
    fun createOpenAIMultimodalRag(
        apiKey: String,
        embeddingModel: String = "text-embedding-3-small",
        dimensions: Int = 1536,
        visionModel: String = "gpt-4-vision-preview",
        audioModel: String = "whisper-1",
        vectorStore: VectorStore? = null,
        reranker: Reranker = IdentityReranker(),
        defaultOptions: RagProcessOptions = RagProcessOptions()
    ): MultimodalRAG {
        logger.info { "Creating OpenAI multimodal RAG with embedding model: $embeddingModel, vision model: $visionModel, audio model: $audioModel" }

        // 创建多模态嵌入服务
        val embeddingService = OpenAIMultimodalEmbeddingService(
            apiKey = apiKey,
            model = embeddingModel,
            dimensions = dimensions,
            visionModel = visionModel,
            audioModel = audioModel
        )

        // 创建向量存储
        val store = vectorStore ?: VectorStoreFactory.createInMemoryVectorStore()

        // 创建文档向量存储适配器
        val documentStore = VectorStoreFactory.adaptToDocumentVectorStore(store)

        // 创建多模态 RAG 实例
        return MultimodalRAG(
            documentStore = documentStore,
            embeddingService = embeddingService,
            reranker = reranker,
            defaultOptions = defaultOptions
        )
    }

    /**
     * 创建自定义多模态 RAG 实例。
     *
     * @param embeddingService 多模态嵌入服务
     * @param documentStore 文档存储
     * @param reranker 重排序器
     * @param defaultOptions 默认选项
     * @return 多模态 RAG 实例
     */
    fun createCustomMultimodalRag(
        embeddingService: MultimodalEmbeddingService,
        documentStore: DocumentVectorStore,
        reranker: Reranker = IdentityReranker(),
        defaultOptions: RagProcessOptions = RagProcessOptions()
    ): MultimodalRAG {
        logger.info { "Creating custom multimodal RAG" }

        // 创建多模态 RAG 实例
        return MultimodalRAG(
            documentStore = documentStore,
            embeddingService = embeddingService,
            reranker = reranker,
            defaultOptions = defaultOptions
        )
    }

    /**
     * 创建自定义多模态 RAG 实例。
     *
     * @param embeddingService 多模态嵌入服务
     * @param vectorStore 向量存储
     * @param reranker 重排序器
     * @param defaultOptions 默认选项
     * @return 多模态 RAG 实例
     */
    fun createCustomMultimodalRag(
        embeddingService: MultimodalEmbeddingService,
        vectorStore: VectorStore? = null,
        reranker: Reranker = IdentityReranker(),
        defaultOptions: RagProcessOptions = RagProcessOptions()
    ): MultimodalRAG {
        logger.info { "Creating custom multimodal RAG with vector store" }

        // 创建向量存储
        val store = vectorStore ?: VectorStoreFactory.createInMemoryVectorStore()

        // 创建文档向量存储适配器
        val documentStore = VectorStoreFactory.adaptToDocumentVectorStore(store)

        // 创建多模态 RAG 实例
        return MultimodalRAG(
            documentStore = documentStore,
            embeddingService = embeddingService,
            reranker = reranker,
            defaultOptions = defaultOptions
        )
    }
}
