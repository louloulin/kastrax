package ai.kastrax.rag

import ai.kastrax.rag.document.Document
import ai.kastrax.rag.document.DocumentLoader
import ai.kastrax.rag.document.DocumentSplitter
import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.vectorstore.SearchResult
import ai.kastrax.rag.vectorstore.VectorStore
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * RAG（检索增强生成）系统，用于从向量存储中检索相关文档并生成增强的上下文。
 *
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 */
class RAG(
    private val vectorStore: VectorStore,
    private val embeddingService: EmbeddingService
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
        
        val addedCount = vectorStore.addDocuments(processedDocuments, embeddingService)
        logger.info { "Added $addedCount documents to vector store" }
        
        return addedCount
    }
    
    /**
     * 使用查询文本搜索相关文档。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 搜索结果列表，按相似度降序排序
     */
    suspend fun search(
        query: String,
        limit: Int = 5,
        minScore: Double = 0.0
    ): List<SearchResult> {
        return vectorStore.similaritySearch(query, embeddingService, limit, minScore)
    }
    
    /**
     * 使用查询文本生成增强的上下文。
     *
     * @param query 查询文本
     * @param limit 使用的文档数量
     * @param minScore 最小相似度分数
     * @param separator 文档之间的分隔符
     * @return 增强的上下文
     */
    suspend fun generateContext(
        query: String,
        limit: Int = 5,
        minScore: Double = 0.0,
        separator: String = "\n\n"
    ): String {
        val results = search(query, limit, minScore)
        
        if (results.isEmpty()) {
            return ""
        }
        
        return results.joinToString(separator) { it.document.content }
    }
    
    /**
     * 使用查询文本生成增强的上下文，包括元数据。
     *
     * @param query 查询文本
     * @param limit 使用的文档数量
     * @param minScore 最小相似度分数
     * @param includeMetadata 是否包含元数据
     * @param metadataKeys 要包含的元数据键，如果为 null，则包含所有元数据
     * @return 增强的上下文，包括元数据
     */
    suspend fun generateContextWithMetadata(
        query: String,
        limit: Int = 5,
        minScore: Double = 0.0,
        includeMetadata: Boolean = true,
        metadataKeys: List<String>? = null
    ): String {
        val results = search(query, limit, minScore)
        
        if (results.isEmpty()) {
            return ""
        }
        
        return results.joinToString("\n\n") { result ->
            val content = result.document.content
            
            if (includeMetadata) {
                val metadata = if (metadataKeys != null) {
                    result.document.metadata.filterKeys { it in metadataKeys }
                } else {
                    result.document.metadata
                }
                
                val metadataStr = metadata.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                "[Source: $metadataStr]\n$content"
            } else {
                content
            }
        }
    }
    
    /**
     * 获取向量存储中的文档数量。
     *
     * @return 文档数量
     */
    suspend fun count(): Int {
        return vectorStore.count()
    }
    
    /**
     * 清空向量存储。
     */
    suspend fun clear() {
        vectorStore.clear()
    }
}
