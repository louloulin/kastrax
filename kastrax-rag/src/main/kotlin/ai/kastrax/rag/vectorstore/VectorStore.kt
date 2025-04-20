package ai.kastrax.rag.vectorstore

import ai.kastrax.rag.document.Document
import ai.kastrax.rag.embedding.Embedding
import ai.kastrax.rag.embedding.EmbeddedDocument
import ai.kastrax.rag.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 表示向量存储中的搜索结果。
 *
 * @property document 文档
 * @property score 相似度分数
 */
data class SearchResult(
    val document: Document,
    val score: Double
)

/**
 * 向量存储接口，用于存储和检索嵌入文档。
 */
interface VectorStore {
    /**
     * 添加嵌入文档到向量存储。
     *
     * @param documents 要添加的嵌入文档列表
     * @return 添加的文档数量
     */
    suspend fun addEmbeddedDocuments(documents: List<EmbeddedDocument>): Int
    
    /**
     * 添加文档到向量存储，自动生成嵌入向量。
     *
     * @param documents 要添加的文档列表
     * @param embeddingService 用于生成嵌入向量的服务
     * @return 添加的文档数量
     */
    suspend fun addDocuments(
        documents: List<Document>,
        embeddingService: EmbeddingService
    ): Int {
        val embeddedDocuments = embeddingService.embedDocuments(documents)
        return addEmbeddedDocuments(embeddedDocuments)
    }
    
    /**
     * 使用嵌入向量搜索相似文档。
     *
     * @param embedding 查询嵌入向量
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 搜索结果列表，按相似度降序排序
     */
    suspend fun similaritySearch(
        embedding: Embedding,
        limit: Int = 5,
        minScore: Double = 0.0
    ): List<SearchResult>
    
    /**
     * 使用文本搜索相似文档。
     *
     * @param text 查询文本
     * @param embeddingService 用于生成嵌入向量的服务
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 搜索结果列表，按相似度降序排序
     */
    suspend fun similaritySearch(
        text: String,
        embeddingService: EmbeddingService,
        limit: Int = 5,
        minScore: Double = 0.0
    ): List<SearchResult> {
        val embedding = embeddingService.embed(text)
        return similaritySearch(embedding, limit, minScore)
    }
    
    /**
     * 获取向量存储中的文档数量。
     *
     * @return 文档数量
     */
    suspend fun count(): Int
    
    /**
     * 清空向量存储。
     */
    suspend fun clear()
}

/**
 * 内存向量存储，将嵌入文档存储在内存中。
 */
class InMemoryVectorStore : VectorStore {
    private val documents = ConcurrentHashMap<String, EmbeddedDocument>()
    private var nextId = 0
    
    override suspend fun addEmbeddedDocuments(documents: List<EmbeddedDocument>): Int {
        var addedCount = 0
        
        for (document in documents) {
            val id = nextId++.toString()
            val metadata = document.document.metadata.toMutableMap().apply {
                put("id", id)
            }
            
            val docWithId = document.copy(
                document = document.document.copy(metadata = metadata)
            )
            
            this.documents[id] = docWithId
            addedCount++
        }
        
        return addedCount
    }
    
    override suspend fun similaritySearch(
        embedding: Embedding,
        limit: Int,
        minScore: Double
    ): List<SearchResult> {
        if (documents.isEmpty()) {
            return emptyList()
        }
        
        return coroutineScope {
            documents.values.map { embeddedDoc ->
                async {
                    val score = embedding.cosineSimilarity(embeddedDoc.embedding)
                    SearchResult(embeddedDoc.document, score)
                }
            }.awaitAll()
                .filter { it.score >= minScore }
                .sortedByDescending { it.score }
                .take(limit)
        }
    }
    
    override suspend fun count(): Int {
        return documents.size
    }
    
    override suspend fun clear() {
        documents.clear()
        nextId = 0
    }
}
