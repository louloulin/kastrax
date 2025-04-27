package ai.kastrax.rag

import ai.kastrax.rag.context.ContextBuilder
import ai.kastrax.rag.context.ContextBuilderConfig
import ai.kastrax.rag.document.*
import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.query.CompositeQueryTransformer
import ai.kastrax.rag.query.DecompositionQueryTransformer
import ai.kastrax.rag.query.NormalizationQueryTransformer
import ai.kastrax.rag.query.QueryTransformer
import ai.kastrax.rag.query.SynonymQueryTransformer
import ai.kastrax.rag.reranker.ContextAwareReranker
import ai.kastrax.rag.reranker.ContextAwareRerankerConfig
import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.rag.reranker.Reranker
import ai.kastrax.rag.retrieval.HybridRetriever
import ai.kastrax.rag.retrieval.HybridRetrieverConfig
import ai.kastrax.rag.retrieval.QueryEnhancedRetriever
import ai.kastrax.rag.retrieval.QueryEnhancedRetrieverConfig
import ai.kastrax.rag.retrieval.Retriever
import ai.kastrax.rag.retrieval.SemanticRetriever
import ai.kastrax.rag.retrieval.SemanticRetrieverConfig
import ai.kastrax.rag.retrieval.TfIdfKeywordExtractor
import ai.kastrax.rag.retrieval.TopKRetriever
import ai.kastrax.rag.vectorstore.SearchResult
import ai.kastrax.rag.vectorstore.RagVectorStore
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * RAG 处理选项，用于配置检索和重排序过程。
 *
 * @property useHybridSearch 是否使用混合搜索，默认为 false
 * @property useSemanticRetrieval 是否使用语义检索，默认为 false
 * @property useReranking 是否使用重排序，默认为 true
 * @property useQueryEnhancement 是否使用查询增强，默认为 false
 * @property hybridOptions 混合搜索选项，仅当 useHybridSearch 为 true 时有效
 * @property semanticOptions 语义检索选项，仅当 useSemanticRetrieval 为 true 时有效
 * @property rerankingOptions 重排序选项，仅当 useReranking 为 true 时有效
 * @property queryEnhancementOptions 查询增强选项，仅当 useQueryEnhancement 为 true 时有效
 * @property contextOptions 上下文构建选项
 */
data class RagProcessOptions(
    val useHybridSearch: Boolean = false,
    val useSemanticRetrieval: Boolean = false,
    val useReranking: Boolean = true,
    val useContextAwareReranking: Boolean = false,
    val useQueryEnhancement: Boolean = false,
    val hybridOptions: HybridRetrieverConfig = HybridRetrieverConfig(),
    val semanticOptions: SemanticRetrieverConfig = SemanticRetrieverConfig(),
    val rerankingOptions: Any? = null,
    val contextAwareRerankingOptions: ContextAwareRerankerConfig = ContextAwareRerankerConfig(),
    val queryEnhancementOptions: QueryEnhancedRetrieverConfig = QueryEnhancedRetrieverConfig(),
    val contextOptions: ContextBuilderConfig = ContextBuilderConfig()
)

/**
 * RAG（检索增强生成）系统，用于从向量存储中检索相关文档并生成增强的上下文。
 *
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 * @property reranker 重排序器，默认为 IdentityReranker
 * @property defaultOptions 默认的 RAG 处理选项
 */
class RAG(
    private val vectorStore: RagVectorStore,
    private val embeddingService: EmbeddingService,
    private val reranker: Reranker = IdentityReranker(),
    private val defaultOptions: RagProcessOptions = RagProcessOptions()
) {
    /**
     * 从文档加载器加载文档并添加到向量存储。
     *
     * @param loader 文档加载器
     * @param splitter 文档分割器，如果为 null，则不分割文档
     * @return 添加的文档数量
     */
    suspend fun loadDocuments(
        loader: DocumentLoader,
        splitter: DocumentSplitter? = null
    ): Int {
        val documents = loader.load()
        logger.info { "Loaded ${documents.size} documents from ${loader.javaClass.simpleName}" }

        val processedDocuments = if (splitter != null) {
            val splitDocs = documents.flatMap { splitter.split(it) }
            logger.info { "Split ${documents.size} documents into ${splitDocs.size} chunks" }
            splitDocs
        } else {
            documents
        }

        // Convert Document objects to strings and metadata maps
        val docContents = processedDocuments.map { it.content }
        val docMetadata = processedDocuments.map { it.metadata.mapValues { entry -> entry.value.toString() } }

        val addedIds = vectorStore.addDocuments(docContents, embeddingService, docMetadata)
        logger.info { "Added ${addedIds.size} documents to vector store" }

        return addedIds.size
    }

    /**
     * 使用查询文本搜索相关文档。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param options RAG 处理选项，如果为 null，则使用默认选项
     * @return 搜索结果列表，按相似度降序排序
     */
    suspend fun search(
        query: String,
        limit: Int = 5,
        minScore: Double = 0.0,
        options: RagProcessOptions? = null
    ): List<SearchResult> {
        val opts = options ?: defaultOptions

        // 创建检索器
        val retriever = createRetriever(opts)

        // 使用检索器获取初始结果
        val initialResults = retriever.retrieve(query, limit, minScore)

        // 如果需要，应用重排序
        return if (initialResults.isNotEmpty()) {
            if (opts.useContextAwareReranking && reranker is ContextAwareReranker) {
                // 获取会话历史或其他上下文信息
                val context = getContextForReranking(query, opts)
                reranker.rerank(query, initialResults, context)
            } else if (opts.useReranking) {
                reranker.rerank(query, initialResults)
            } else {
                initialResults
            }
        } else {
            initialResults
        }
    }

    /**
     * 根据选项创建检索器。
     *
     * @param options RAG 处理选项
     * @return 检索器
     */
    private fun createRetriever(options: RagProcessOptions): Retriever {
        // 首先创建基础检索器
        val baseRetriever = when {
            options.useHybridSearch -> {
                logger.debug { "Using hybrid retriever with options: ${options.hybridOptions}" }
                HybridRetriever(
                    vectorStore = vectorStore,
                    embeddingService = embeddingService,
                    keywordExtractor = TfIdfKeywordExtractor(),
                    config = options.hybridOptions
                )
            }
            options.useSemanticRetrieval -> {
                logger.debug { "Using semantic retriever with options: ${options.semanticOptions}" }
                SemanticRetriever(
                    vectorStore = vectorStore,
                    embeddingService = embeddingService,
                    config = options.semanticOptions
                )
            }
            else -> {
                logger.debug { "Using top-k retriever" }
                TopKRetriever(vectorStore, embeddingService)
            }
        }

        // 如果启用了查询增强，则包装基础检索器
        return if (options.useQueryEnhancement) {
            logger.debug { "Using query enhanced retriever with options: ${options.queryEnhancementOptions}" }

            // 创建查询转换器
            val queryTransformer = createQueryTransformer()

            // 创建查询增强检索器
            QueryEnhancedRetriever(
                baseRetriever = baseRetriever,
                queryTransformer = queryTransformer,
                config = options.queryEnhancementOptions
            )
        } else {
            baseRetriever
        }
    }

    /**
     * 创建查询转换器。
     *
     * @return 查询转换器
     */
    private fun createQueryTransformer(): QueryTransformer {
        // 创建多个查询转换器
        val transformers = listOf(
            NormalizationQueryTransformer(),
            SynonymQueryTransformer(createSynonymMap()),
            DecompositionQueryTransformer()
        )

        // 返回组合查询转换器
        return CompositeQueryTransformer(transformers)
    }

    /**
     * 创建同义词映射。
     *
     * @return 同义词映射
     */
    private fun createSynonymMap(): Map<String, List<String>> {
        // 这里可以从配置文件或数据库加载同义词映射
        // 这里使用一个简单的示例
        return mapOf(
            "ai" to listOf("artificial intelligence", "machine learning", "deep learning"),
            "ml" to listOf("machine learning", "deep learning", "neural networks"),
            "nlp" to listOf("natural language processing", "text analysis", "language understanding"),
            "rag" to listOf("retrieval augmented generation", "retrieval-based", "document retrieval")
        )
    }

    /**
     * 获取用于重排序的上下文信息。
     * 在实际应用中，这可能来自会话历史、用户偏好或其他上下文信息。
     *
     * @param query 查询文本
     * @param options RAG 处理选项
     * @return 上下文信息
     */
    protected open fun getContextForReranking(query: String, options: RagProcessOptions): String {
        // 这里可以从会话历史、用户偏好或其他来源获取上下文信息
        // 在这个简单实现中，我们只返回一个空字符串
        // 在实际应用中，你可能需要从会话管理器或用户配置中获取上下文
        return ""
    }



    /**
     * 使用查询文本生成增强的上下文。
     *
     * @param query 查询文本
     * @param limit 使用的文档数量
     * @param minScore 最小相似度分数
     * @param options RAG 处理选项，如果为 null，则使用默认选项
     * @return 增强的上下文
     */
    suspend fun generateContext(
        query: String,
        limit: Int = 5,
        minScore: Double = 0.0,
        options: RagProcessOptions? = null
    ): String {
        val opts = options ?: defaultOptions
        val results = search(query, limit, minScore, opts)

        if (results.isEmpty()) {
            return ""
        }

        // 使用上下文构建器构建上下文
        val contextBuilder = ContextBuilder(opts.contextOptions)
        return contextBuilder.buildContext(results, query)
    }

    /**
     * 使用查询文本生成增强的上下文，包括元数据。
     *
     * @param query 查询文本
     * @param limit 使用的文档数量
     * @param minScore 最小相似度分数
     * @param includeMetadata 是否包含元数据
     * @param metadataKeys 要包含的元数据键，如果为 null，则包含所有元数据
     * @param options RAG 处理选项，如果为 null，则使用默认选项
     * @return 增强的上下文，包括元数据
     */
    suspend fun generateContextWithMetadata(
        query: String,
        limit: Int = 5,
        minScore: Double = 0.0,
        includeMetadata: Boolean = true,
        metadataKeys: List<String>? = null,
        options: RagProcessOptions? = null
    ): String {
        val opts = options ?: defaultOptions.copy(
            contextOptions = defaultOptions.contextOptions.copy(includeMetadata = includeMetadata)
        )

        val results = search(query, limit, minScore, opts)

        logger.info { "Search results for query '$query': ${results.size} results with min score $minScore" }
        results.forEachIndexed { index, result ->
            logger.info { "Result $index: score=${result.score}, content=${result.document.content.take(50)}..." }
        }

        if (results.isEmpty()) {
            logger.info { "No results found for query '$query' with min score $minScore" }
            return ""
        }

        // 创建自定义上下文构建器配置
        val contextConfig = opts.contextOptions.copy(
            includeMetadata = includeMetadata,
            headerTemplate = "以下是与查询相关的文档：\n\n",
            footerTemplate = "\n\n请根据以上文档回答查询：{query}"
        )

        // 使用上下文构建器构建上下文
        val contextBuilder = ContextBuilder(contextConfig)
        return contextBuilder.buildContext(results, query)
    }

    /**
     * 检索上下文，返回检索结果和生成的上下文。
     *
     * @param query 查询文本
     * @param options RAG 处理选项，如果为 null，则使用默认选项
     * @param limit 使用的文档数量
     * @param minScore 最小相似度分数
     * @return 检索结果和生成的上下文
     */
    suspend fun retrieveContext(
        query: String,
        options: RagProcessOptions? = null,
        limit: Int = 5,
        minScore: Double = 0.0
    ): RetrieveContextResult {
        val opts = options ?: defaultOptions
        val results = search(query, limit, minScore, opts)

        val context = if (results.isNotEmpty()) {
            val contextBuilder = ContextBuilder(opts.contextOptions)
            contextBuilder.buildContext(results, query)
        } else {
            ""
        }

        return RetrieveContextResult(results, context)
    }

    /**
     * 获取向量存储中的文档数量。
     *
     * @return 文档数量
     */
    suspend fun count(): Int {
        return vectorStore.size()
    }

    /**
     * 清空向量存储。
     */
    suspend fun clear() {
        vectorStore.clear()
    }

    /**
     * 获取查询文本与向量存储中文档的相似度分数。
     * 这个方法主要用于调试目的，帮助理解检索过程。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param options RAG 处理选项，如果为 null，则使用默认选项
     * @return 相似度分数映射，键为文档ID，值为相似度分数
     */
    suspend fun getSimilarityScores(
        query: String,
        limit: Int = 10,
        minScore: Double = 0.0,
        options: RagProcessOptions? = null
    ): Map<String, Double> {
        val results = search(query, limit, minScore, options)
        return results.associate {
            val docId = it.document.metadata["id"]?.toString() ?: it.document.content.take(20) + "..."
            docId to it.score
        }
    }
}

/**
 * 检索上下文结果，包含检索结果和生成的上下文。
 *
 * @property sourceDocuments 检索结果
 * @property context 生成的上下文
 */
data class RetrieveContextResult(
    val sourceDocuments: List<SearchResult>,
    val context: String
)
