package ai.kastrax.rag

import ai.kastrax.rag.context.ContextBuilder
import ai.kastrax.rag.context.ContextBuilderConfig
import ai.kastrax.rag.document.DocumentLoader
import ai.kastrax.rag.document.DocumentSplitter
import ai.kastrax.rag.model.RetrieveContextResult
import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.rag.reranker.Reranker
import ai.kastrax.rag.retriever.Retriever
import ai.kastrax.rag.retriever.RetrieverFactory
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.document.DocumentVectorStore
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
    val hybridOptions: HybridOptions = HybridOptions(),
    val semanticOptions: SemanticOptions = SemanticOptions(),
    val rerankingOptions: RerankingOptions = RerankingOptions(),
    val queryEnhancementOptions: QueryEnhancementOptions = QueryEnhancementOptions(),
    val contextOptions: ContextBuilderConfig = ContextBuilderConfig()
)

/**
 * 混合搜索选项。
 *
 * @property vectorWeight 向量权重
 * @property keywordWeight 关键词权重
 */
data class HybridOptions(
    val vectorWeight: Double = 0.7,
    val keywordWeight: Double = 0.3
)

/**
 * 语义检索选项。
 *
 * @property useChunking 是否使用分块
 * @property chunkSize 分块大小
 * @property chunkOverlap 分块重叠大小
 */
data class SemanticOptions(
    val useChunking: Boolean = true,
    val chunkSize: Int = 1000,
    val chunkOverlap: Int = 200
)

/**
 * 重排序选项。
 *
 * @property useDiversity 是否使用多样性重排序
 * @property diversityWeight 多样性权重
 * @property useMetadata 是否使用元数据重排序
 * @property metadataFields 元数据字段
 * @property metadataWeights 元数据权重
 */
data class RerankingOptions(
    val useDiversity: Boolean = false,
    val diversityWeight: Double = 0.3,
    val useMetadata: Boolean = false,
    val metadataFields: List<String> = emptyList(),
    val metadataWeights: Map<String, Double> = emptyMap()
)

/**
 * 查询增强选项。
 *
 * @property useSynonyms 是否使用同义词
 * @property useDecomposition 是否使用分解
 * @property useNormalization 是否使用归一化
 */
data class QueryEnhancementOptions(
    val useSynonyms: Boolean = true,
    val useDecomposition: Boolean = false,
    val useNormalization: Boolean = true
)

/**
 * RAG（检索增强生成）系统，用于从向量存储中检索相关文档并生成增强的上下文。
 *
 * @property documentStore 文档向量存储
 * @property embeddingService 嵌入服务
 * @property reranker 重排序器，默认为 IdentityReranker
 * @property defaultOptions 默认的 RAG 处理选项
 */
class RAG(
    private val documentStore: DocumentVectorStore,
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
        try {
            // 加载文档
            val documents = loader.load()
            logger.debug { "Loaded ${documents.size} documents" }

            // 分割文档（如果需要）
            val processedDocuments = if (splitter != null) {
                documents.flatMap { document ->
                    splitter.split(document)
                }
            } else {
                documents
            }
            logger.debug { "Processed ${processedDocuments.size} documents after splitting" }

            // 添加文档到向量存储
            val success = documentStore.addDocuments(processedDocuments, embeddingService)

            if (success) {
                logger.info { "Added ${processedDocuments.size} documents to vector store" }
                return processedDocuments.size
            } else {
                logger.error { "Failed to add documents to vector store" }
                return 0
            }
        } catch (e: Exception) {
            logger.error(e) { "Error loading documents" }
            throw e
        }
    }

    /**
     * 加载文档列表并添加到向量存储。
     *
     * @param documents 文档列表
     * @param splitter 文档分割器，如果为 null，则不分割文档
     * @return 是否成功添加
     */
    suspend fun loadDocuments(
        documents: List<Document>,
        splitter: DocumentSplitter? = null
    ): Boolean {
        try {
            logger.debug { "Loading ${documents.size} documents" }

            // 分割文档（如果需要）
            val processedDocuments = if (splitter != null) {
                documents.flatMap { document ->
                    splitter.split(document)
                }
            } else {
                documents
            }
            logger.debug { "Processed ${processedDocuments.size} documents after splitting" }

            // 添加文档到向量存储
            val success = documentStore.addDocuments(processedDocuments, embeddingService)

            if (success) {
                logger.info { "Added ${processedDocuments.size} documents to vector store" }
            } else {
                logger.error { "Failed to add documents to vector store" }
            }

            return success
        } catch (e: Exception) {
            logger.error(e) { "Error loading documents" }
            return false
        }
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
        try {
            val opts = options ?: defaultOptions

            // 创建检索器
            val retriever = createRetriever(opts)

            // 检索文档
            val results = retriever.retrieve(query, limit, minScore)

            // 重排序（如果需要）
            return if (opts.useReranking) {
                reranker.rerank(query, results)
            } else {
                results
            }
        } catch (e: Exception) {
            logger.error(e) { "Error searching documents" }
            return emptyList()
        }
    }

    /**
     * 生成上下文。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param options RAG 处理选项，如果为 null，则使用默认选项
     * @return 生成的上下文
     */
    suspend fun generateContext(
        query: String,
        limit: Int = 5,
        minScore: Double = 0.0,
        options: RagProcessOptions? = null
    ): String {
        try {
            val opts = options ?: defaultOptions

            // 搜索文档
            val results = search(query, limit, minScore, opts)

            if (results.isEmpty()) {
                logger.warn { "No documents found for query: $query" }
                return ""
            }

            // 创建上下文构建器
            val contextBuilder = ContextBuilder(opts.contextOptions)

            // 构建上下文
            return contextBuilder.buildContext(query, results)
        } catch (e: Exception) {
            logger.error(e) { "Error generating context" }
            return ""
        }
    }

    /**
     * 检索上下文。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param options RAG 处理选项，如果为 null，则使用默认选项
     * @return 检索上下文结果
     */
    suspend fun retrieveContext(
        query: String,
        limit: Int = 5,
        minScore: Double = 0.0,
        options: RagProcessOptions? = null
    ): RetrieveContextResult {
        try {
            val opts = options ?: defaultOptions

            // 搜索文档
            val results = search(query, limit, minScore, opts)

            if (results.isEmpty()) {
                logger.warn { "No documents found for query: $query" }
                return RetrieveContextResult("", emptyList<Document>())
            }

            // 创建上下文构建器
            val contextBuilder = ContextBuilder(opts.contextOptions)

            // 构建上下文
            val context = contextBuilder.buildContext(query, results)

            // 提取文档
            val documents = results.map { it.document }

            return RetrieveContextResult(context, documents)
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving context" }
            return RetrieveContextResult("", emptyList<Document>())
        }
    }

    /**
     * 生成带元数据的上下文。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param options RAG 处理选项，如果为 null，则使用默认选项
     * @return 生成的上下文和元数据
     */
    suspend fun generateContextWithMetadata(
        query: String,
        limit: Int = 5,
        minScore: Double = 0.0,
        options: RagProcessOptions? = null
    ): Pair<String, Map<String, Any>> {
        try {
            val opts = options ?: defaultOptions

            // 搜索文档
            val results = search(query, limit, minScore, opts)

            if (results.isEmpty()) {
                logger.warn { "No documents found for query: $query" }
                return Pair("", emptyMap())
            }

            // 创建上下文构建器
            val contextBuilder = ContextBuilder(opts.contextOptions)

            // 构建上下文
            val context = contextBuilder.buildContext(query, results)

            // 收集元数据
            val metadata = mutableMapOf<String, Any>(
                "query" to query,
                "result_count" to results.size,
                "min_score" to minScore,
                "max_score" to (results.maxOfOrNull { it.score } ?: 0.0),
                "avg_score" to (results.map { it.score }.average()),
                "document_ids" to results.map { it.document.id },
                "timestamp" to System.currentTimeMillis()
            )

            return Pair(context, metadata)
        } catch (e: Exception) {
            logger.error(e) { "Error generating context with metadata" }
            return Pair("", emptyMap())
        }
    }

    /**
     * 获取相似度分数。
     *
     * @param query 查询文本
     * @param documentIds 文档 ID 列表
     * @return 相似度分数映射
     */
    suspend fun getSimilarityScores(
        query: String,
        documentIds: List<String>
    ): Map<String, Double> {
        try {
            if (documentIds.isEmpty()) {
                return emptyMap()
            }

            // 搜索文档
            val results = search(query, limit = documentIds.size * 2)

            // 创建 ID 到分数的映射
            val scoreMap = results.associate { it.document.id to it.score }

            // 返回指定文档的分数
            return documentIds.associateWith { id -> scoreMap[id] ?: 0.0 }
        } catch (e: Exception) {
            logger.error(e) { "Error getting similarity scores" }
            return emptyMap()
        }
    }

    /**
     * 获取嵌入服务。
     *
     * @return 嵌入服务
     */
    fun getEmbeddingService(): EmbeddingService {
        return embeddingService
    }

    /**
     * 获取文档向量存储。
     *
     * @return 文档向量存储
     */
    fun getDocumentStore(): DocumentVectorStore {
        return documentStore
    }

    /**
     * 获取文档。
     *
     * @param id 文档 ID
     * @return 文档，如果不存在则返回 null
     */
    suspend fun getDocument(id: String): Document? {
        try {
            // 搜索文档
            val results = search("id:$id", 1)
            return results.firstOrNull()?.document
        } catch (e: Exception) {
            logger.error(e) { "Error getting document with ID $id" }
            return null
        }
    }

    /**
     * 删除文档。
     *
     * @param id 文档 ID
     * @return 是否成功删除
     */
    suspend fun deleteDocument(id: String): Boolean {
        try {
            return documentStore.deleteDocuments(listOf(id))
        } catch (e: Exception) {
            logger.error(e) { "Error deleting document with ID $id" }
            return false
        }
    }

    /**
     * 创建检索器。
     *
     * @param options RAG 处理选项
     * @return 检索器
     */
    private fun createRetriever(options: RagProcessOptions): Retriever {
        // 创建向量存储检索器
        val vectorStoreRetriever = RetrieverFactory.createVectorStoreRetriever(
            documentStore = documentStore,
            embeddingService = embeddingService
        )

        // 根据选项创建检索器
        return when {
            options.useHybridSearch -> {
                // 创建关键词检索器
                val keywordRetriever = RetrieverFactory.createKeywordRetriever(
                    documentStore = documentStore
                )

                // 创建混合检索器
                RetrieverFactory.createHybridRetriever(
                    documentStore = documentStore,
                    embeddingService = embeddingService,
                    vectorWeight = options.hybridOptions.vectorWeight,
                    keywordWeight = options.hybridOptions.keywordWeight
                )
            }
            options.useSemanticRetrieval -> {
                // 这里可以实现语义检索器
                // 目前暂时使用向量存储检索器
                vectorStoreRetriever
            }
            else -> {
                // 默认使用向量存储检索器
                vectorStoreRetriever
            }
        }
    }
}
