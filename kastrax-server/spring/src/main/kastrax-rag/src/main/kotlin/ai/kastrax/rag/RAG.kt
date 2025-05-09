package ai.kastrax.rag

import ai.kastrax.rag.context.ContextBuilder
import ai.kastrax.rag.document.DocumentLoader
import ai.kastrax.rag.document.DocumentSplitter
import ai.kastrax.rag.model.RetrieveContextResult
import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.rag.reranker.Reranker
import ai.kastrax.rag.store.DocumentStore
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.embedding.EmbeddingService
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
    val useQueryEnhancement: Boolean = false,
    val hybridOptions: Any? = null,
    val semanticOptions: Any? = null,
    val rerankingOptions: Any? = null,
    val queryEnhancementOptions: Any? = null,
    val contextOptions: ContextBuilderConfig = ContextBuilderConfig()
)

/**
 * 上下文构建器配置。
 *
 * @property maxTokens 最大令牌数
 * @property separator 分隔符
 * @property includeMetadata 是否包含元数据
 * @property metadataTemplate 元数据模板
 */
data class ContextBuilderConfig(
    val maxTokens: Int = 4000,
    val separator: String = "\n\n",
    val includeMetadata: Boolean = false,
    val metadataTemplate: String = "Source: {source}"
)

/**
 * RAG（检索增强生成）系统，用于从文档存储中检索相关文档并生成增强的上下文。
 *
 * @property documentStore 文档存储
 * @property embeddingService 嵌入服务
 * @property reranker 重排序器，默认为 IdentityReranker
 * @property defaultOptions 默认的 RAG 处理选项
 */
open class RAG(
    protected val documentStore: DocumentStore,
    protected val embeddingService: EmbeddingService,
    protected val reranker: Reranker = IdentityReranker(),
    protected val defaultOptions: RagProcessOptions = RagProcessOptions()
) {
    /**
     * 加载文档。
     *
     * @param loader 文档加载器
     * @param splitter 文档分割器，如果为 null，则不进行分割
     * @return 加载的文档数量
     */
    suspend fun loadDocuments(
        loader: DocumentLoader,
        splitter: DocumentSplitter? = null
    ): Int {
        logger.info { "Loading documents with ${loader.javaClass.simpleName}" }
        
        // 加载文档
        val documents = loader.load()
        
        // 分割文档（如果需要）
        val finalDocuments = if (splitter != null) {
            logger.debug { "Splitting documents with ${splitter.javaClass.simpleName}" }
            documents.flatMap { splitter.split(it) }
        } else {
            documents
        }
        
        logger.info { "Loaded ${finalDocuments.size} documents" }
        
        // 添加文档到文档存储
        val success = documentStore.addDocuments(finalDocuments, embeddingService)
        
        return if (success) finalDocuments.size else 0
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
    ): List<DocumentSearchResult> {
        logger.info { "Searching for: '$query'" }
        
        val opts = options ?: defaultOptions
        
        // 使用文档存储搜索
        val results = documentStore.search(query, embeddingService, limit, minScore)
        
        // 应用重排序
        return if (opts.useReranking) {
            reranker.rerank(query, results)
        } else {
            results
        }
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
        logger.info { "Generating context for: '$query'" }
        
        // 搜索文档
        val results = search(query, limit, minScore, options)
        
        if (results.isEmpty()) {
            return ""
        }
        
        // 使用上下文构建器构建上下文
        val opts = options ?: defaultOptions
        val contextBuilder = ContextBuilder(opts.contextOptions)
        return contextBuilder.buildContext(results, query)
    }
    
    /**
     * 检索上下文，返回检索结果和生成的上下文。
     *
     * @param query 查询文本
     * @param limit 使用的文档数量
     * @param minScore 最小相似度分数
     * @param options RAG 处理选项，如果为 null，则使用默认选项
     * @return 检索结果和生成的上下文
     */
    suspend fun retrieveContext(
        query: String,
        limit: Int = 5,
        minScore: Double = 0.0,
        options: RagProcessOptions? = null
    ): RetrieveContextResult {
        logger.info { "Retrieving context for: '$query'" }
        
        // 搜索文档
        val results = search(query, limit, minScore, options)
        
        // 生成上下文
        val context = if (results.isNotEmpty()) {
            val opts = options ?: defaultOptions
            val contextBuilder = ContextBuilder(opts.contextOptions)
            contextBuilder.buildContext(results, query)
        } else {
            ""
        }
        
        return RetrieveContextResult(results, context)
    }
    
    /**
     * 清空文档存储。
     */
    suspend fun clear() {
        logger.info { "Clearing document store" }
        documentStore.clear()
    }
}
