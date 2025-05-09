package ai.kastrax.ragx

import ai.kastrax.ragx.document.DocumentLoader
import ai.kastrax.ragx.document.DocumentSplitter
import ai.kastrax.ragx.retrieval.Retriever
import ai.kastrax.ragx.retrieval.RetrieverFactory
import ai.kastrax.ragx.reranker.Reranker
import ai.kastrax.ragx.reranker.IdentityReranker
import ai.kastrax.ragx.context.ContextBuilder
import ai.kastrax.store.VectorStore
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

/**
 * RagX 是新一代的 RAG（检索增强生成）框架，基于 kastrax-store 向量存储架构。
 *
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 * @property retriever 检索器
 * @property reranker 重排序器
 * @property contextBuilder 上下文构建器
 * @property defaultOptions 默认选项
 */
class RagX(
    private val vectorStore: VectorStore,
    private val embeddingService: EmbeddingService,
    private val retriever: Retriever = RetrieverFactory.createTopKRetriever(vectorStore, embeddingService),
    private val reranker: Reranker = IdentityReranker(),
    private val contextBuilder: ContextBuilder = ContextBuilder(),
    private val defaultOptions: RagXOptions = RagXOptions()
) {
    /**
     * 加载文档。
     *
     * @param loader 文档加载器
     * @param splitter 文档分割器
     * @return 加载的文档数量
     */
    suspend fun loadDocuments(
        loader: DocumentLoader,
        splitter: DocumentSplitter? = null
    ): Int = withContext(Dispatchers.IO) {
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
            
            // 计算嵌入向量并存储文档
            val contents = processedDocuments.map { it.content }
            val metadata = processedDocuments.map { it.metadata }
            val embeddings = embeddingService.embedBatch(contents)
            
            // 存储文档
            val indexName = defaultOptions.indexName
            val ids = vectorStore.upsert(
                indexName = indexName,
                vectors = embeddings,
                metadata = metadata.mapIndexed { i, meta -> meta + ("content" to contents[i]) },
                ids = processedDocuments.map { it.id }
            )
            
            logger.info { "Stored ${ids.size} documents in index $indexName" }
            return@withContext ids.size
        } catch (e: Exception) {
            logger.error(e) { "Error loading documents" }
            throw e
        }
    }
    
    /**
     * 搜索文档。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param options 选项
     * @return 搜索结果列表
     */
    suspend fun search(
        query: String,
        limit: Int = 5,
        minScore: Double = 0.0,
        options: RagXOptions? = null
    ): List<DocumentSearchResult> = withContext(Dispatchers.IO) {
        try {
            val opts = options ?: defaultOptions
            
            // 检索文档
            val retrievedDocs = retriever.retrieve(query, limit, minScore)
            
            // 重排序
            val rerankedDocs = reranker.rerank(query, retrievedDocs)
            
            // 返回结果
            return@withContext rerankedDocs
        } catch (e: Exception) {
            logger.error(e) { "Error searching documents" }
            throw e
        }
    }
    
    /**
     * 生成上下文。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param options 选项
     * @return 生成的上下文
     */
    suspend fun generateContext(
        query: String,
        limit: Int = 5,
        minScore: Double = 0.0,
        options: RagXOptions? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val opts = options ?: defaultOptions
            
            // 检索文档
            val retrievedDocs = retriever.retrieve(query, limit, minScore)
            
            // 重排序
            val rerankedDocs = reranker.rerank(query, retrievedDocs)
            
            // 构建上下文
            val context = contextBuilder.buildContext(query, rerankedDocs, opts)
            
            return@withContext context
        } catch (e: Exception) {
            logger.error(e) { "Error generating context" }
            throw e
        }
    }
    
    /**
     * 检索上下文。
     *
     * @param query 查询文本
     * @param options 选项
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 检索上下文结果
     */
    suspend fun retrieveContext(
        query: String,
        options: RagXOptions? = null,
        limit: Int = 5,
        minScore: Double = 0.0
    ): RetrieveContextResult = withContext(Dispatchers.IO) {
        try {
            val opts = options ?: defaultOptions
            
            // 检索文档
            val retrievedDocs = retriever.retrieve(query, limit, minScore)
            
            // 重排序
            val rerankedDocs = reranker.rerank(query, retrievedDocs)
            
            // 构建上下文
            val context = contextBuilder.buildContext(query, rerankedDocs, opts)
            
            return@withContext RetrieveContextResult(
                context = context,
                documents = rerankedDocs.map { it.document }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving context" }
            throw e
        }
    }
}

/**
 * 检索上下文结果。
 *
 * @property context 上下文
 * @property documents 文档列表
 */
data class RetrieveContextResult(
    val context: String,
    val documents: List<Document>
)
