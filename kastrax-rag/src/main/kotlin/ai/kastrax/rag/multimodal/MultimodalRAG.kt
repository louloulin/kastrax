package ai.kastrax.rag.multimodal

import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.context.ContextBuilder
import ai.kastrax.rag.context.ContextBuilderConfig
import ai.kastrax.rag.model.RetrieveContextResult
import ai.kastrax.rag.reranker.Reranker
import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

/**
 * 多模态 RAG 系统，支持文本、图像、音频和视频等多种模态的检索增强生成。
 *
 * @property documentStore 文档存储
 * @property embeddingService 多模态嵌入服务
 * @property reranker 重排序器
 * @property defaultOptions 默认选项
 */
class MultimodalRAG(
    private val documentStore: DocumentVectorStore,
    val embeddingService: MultimodalEmbeddingService,
    private val reranker: Reranker,
    private val defaultOptions: RagProcessOptions = RagProcessOptions()
) {

    /**
     * 加载多模态文档。
     *
     * @param documents 多模态文档列表
     * @return 是否成功
     */
    suspend fun loadMultimodalDocuments(documents: List<MultimodalDocument>): Boolean {
        logger.info { "Loading ${documents.size} multimodal documents" }

        // 转换为普通文档
        val standardDocuments = documents.map { it.toDocument() }

        // 生成嵌入向量
        val embeddings = embeddingService.embedMultimodalDocuments(documents)

        // 加载文档和嵌入向量
        // 创建一个实现 EmbeddingService 接口的对象，返回预计算的嵌入向量
        val precomputedEmbeddingService = object : ai.kastrax.store.embedding.EmbeddingService {
            private val dim: Int = documentStore.dimension
            override suspend fun embed(text: String): FloatArray = FloatArray(dim)
            override suspend fun embedBatch(texts: List<String>): List<FloatArray> = embeddings
            override fun dimension(): Int = dim
            override fun close() {}
        }
        return documentStore.addDocuments(standardDocuments, precomputedEmbeddingService)
    }

    /**
     * 使用多模态查询搜索文档。
     *
     * @param textQuery 文本查询
     * @param imageUrl 图像 URL
     * @param audioUrl 音频 URL
     * @param videoUrl 视频 URL
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param options RAG 处理选项
     * @return 搜索结果列表
     */
    suspend fun multimodalSearch(
        textQuery: String,
        imageUrl: String? = null,
        audioUrl: String? = null,
        videoUrl: String? = null,
        limit: Int = 5,
        minScore: Double = 0.0,
        options: RagProcessOptions? = null
    ): List<DocumentSearchResult> {
        logger.info { "Performing multimodal search with text: '$textQuery', image: $imageUrl, audio: $audioUrl, video: $videoUrl" }

        // 创建多模态查询文档
        val queryDocument = createQueryDocument(textQuery, imageUrl, audioUrl, videoUrl)

        // 生成查询嵌入向量
        val queryEmbedding = embeddingService.embedMultimodalDocument(queryDocument)

        // 使用嵌入向量搜索文档
        val searchResults = documentStore.similaritySearch(queryEmbedding, limit)

        // 应用重排序
        val finalOptions = options ?: defaultOptions
        return if (finalOptions.useReranking) {
            reranker.rerank(textQuery, searchResults)
        } else {
            searchResults
        }
    }

    /**
     * 使用多模态查询生成上下文。
     *
     * @param textQuery 文本查询
     * @param imageUrl 图像 URL
     * @param audioUrl 音频 URL
     * @param videoUrl 视频 URL
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param options RAG 处理选项
     * @return 生成的上下文
     */
    suspend fun generateMultimodalContext(
        textQuery: String,
        imageUrl: String? = null,
        audioUrl: String? = null,
        videoUrl: String? = null,
        limit: Int = 5,
        minScore: Double = 0.0,
        options: RagProcessOptions? = null
    ): String {
        logger.info { "Generating multimodal context with text: '$textQuery', image: $imageUrl, audio: $audioUrl, video: $videoUrl" }

        // 搜索文档
        val results = multimodalSearch(textQuery, imageUrl, audioUrl, videoUrl, limit, minScore, options)

        // 生成上下文
        val contextBuilder = ContextBuilder(options?.contextOptions ?: defaultOptions.contextOptions)
        return withContext(Dispatchers.IO) { contextBuilder.buildContext(textQuery, results) }
    }

    /**
     * 使用多模态查询检索上下文。
     *
     * @param textQuery 文本查询
     * @param imageUrl 图像 URL
     * @param audioUrl 音频 URL
     * @param videoUrl 视频 URL
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param options RAG 处理选项
     * @return 检索上下文结果
     */
    suspend fun retrieveMultimodalContext(
        textQuery: String,
        imageUrl: String? = null,
        audioUrl: String? = null,
        videoUrl: String? = null,
        limit: Int = 5,
        minScore: Double = 0.0,
        options: RagProcessOptions? = null
    ): RetrieveContextResult {
        logger.info { "Retrieving multimodal context with text: '$textQuery', image: $imageUrl, audio: $audioUrl, video: $videoUrl" }

        // 搜索文档
        val results = multimodalSearch(textQuery, imageUrl, audioUrl, videoUrl, limit, minScore, options)

        // 生成上下文
        val contextBuilder = ContextBuilder(options?.contextOptions ?: defaultOptions.contextOptions)
        val context = withContext(Dispatchers.IO) { contextBuilder.buildContext(textQuery, results) }

        return RetrieveContextResult(context, results.map { it.document })
    }

    /**
     * 生成上下文。
     *
     * @param results 搜索结果列表
     * @param query 查询文本
     * @param options RAG 处理选项
     * @return 生成的上下文
     */
    private suspend fun generateContext(
        results: List<DocumentSearchResult>,
        query: String,
        options: RagProcessOptions? = null
    ): String = withContext(Dispatchers.IO) {
        if (results.isEmpty()) {
            logger.warn { "No documents found for query: $query" }
            return@withContext ""
        }

        // 创建上下文构建器
        val contextBuilder = ContextBuilder(options?.contextOptions ?: defaultOptions.contextOptions)

        // 构建上下文
        return@withContext contextBuilder.buildContext(query, results)
    }

    /**
     * 创建查询文档。
     *
     * @param textQuery 文本查询
     * @param imageUrl 图像 URL
     * @param audioUrl 音频 URL
     * @param videoUrl 视频 URL
     * @return 多模态文档
     */
    private fun createQueryDocument(
        textQuery: String,
        imageUrl: String?,
        audioUrl: String?,
        videoUrl: String?
    ): MultimodalDocument {
        val mediaUrls = listOfNotNull(imageUrl, audioUrl, videoUrl)

        val mediaType = when {
            imageUrl != null && audioUrl == null && videoUrl == null -> MultimodalDocumentType.IMAGE
            imageUrl == null && audioUrl != null && videoUrl == null -> MultimodalDocumentType.AUDIO
            imageUrl == null && audioUrl == null && videoUrl != null -> MultimodalDocumentType.VIDEO
            mediaUrls.isEmpty() -> MultimodalDocumentType.TEXT
            else -> MultimodalDocumentType.MIXED
        }

        return MultimodalDocument(
            id = "query",
            content = textQuery,
            mediaUrls = mediaUrls,
            mediaType = mediaType
        )
    }
}
