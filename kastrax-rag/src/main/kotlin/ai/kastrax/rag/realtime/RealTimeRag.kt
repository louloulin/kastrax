package ai.kastrax.rag.realtime

import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.model.RetrieveContextResult
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 实时 RAG 配置。
 *
 * @property maxDocuments 最大文档数量
 * @property maxDocumentAge 最大文档年龄（毫秒）
 * @property useTimeDecay 是否使用时间衰减
 * @property timeDecayFactor 时间衰减因子
 */
data class RealTimeRagConfig(
    val maxDocuments: Int = 1000,
    val maxDocumentAge: Long = 24 * 60 * 60 * 1000, // 24 小时
    val useTimeDecay: Boolean = true,
    val timeDecayFactor: Double = 0.5
)

/**
 * 实时 RAG，支持实时添加和检索文档。
 *
 * @property rag RAG 实例
 * @property config 配置
 */
class RealTimeRag(
    private val rag: RAG,
    private val config: RealTimeRagConfig = RealTimeRagConfig()
) {
    private val documentTimestamps = ConcurrentHashMap<String, Long>()
    private val mutex = Mutex()
    
    /**
     * 添加文档。
     *
     * @param document 文档
     * @return 文档 ID
     */
    suspend fun addDocument(document: Document): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                // 生成 ID（如果没有）
                val id = if (document.id.isBlank()) {
                    UUID.randomUUID().toString()
                } else {
                    document.id
                }
                
                // 创建新文档
                val newDocument = Document(
                    id = id,
                    content = document.content,
                    metadata = document.metadata + mapOf("timestamp" to System.currentTimeMillis())
                )
                
                // 添加文档
                val documents = listOf(newDocument)
                val success = rag.loadDocuments(object : ai.kastrax.rag.document.DocumentLoader {
                    override suspend fun load(): List<Document> = documents
                })
                
                if (success > 0) {
                    // 记录时间戳
                    documentTimestamps[id] = System.currentTimeMillis()
                    
                    // 清理旧文档
                    cleanupOldDocuments()
                    
                    return@withLock id
                } else {
                    throw RuntimeException("Failed to add document")
                }
            } catch (e: Exception) {
                logger.error(e) { "Error adding document" }
                throw e
            }
        }
    }
    
    /**
     * 搜索文档。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @return 搜索结果列表
     */
    suspend fun search(
        query: String,
        limit: Int = 5
    ): List<DocumentSearchResult> = withContext(Dispatchers.IO) {
        try {
            // 搜索文档
            val results = rag.search(query, limit)
            
            // 应用时间衰减
            return@withContext if (config.useTimeDecay) {
                applyTimeDecay(results)
            } else {
                results
            }
        } catch (e: Exception) {
            logger.error(e) { "Error searching documents" }
            return@withContext emptyList()
        }
    }
    
    /**
     * 生成上下文。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @return 生成的上下文
     */
    suspend fun generateContext(
        query: String,
        limit: Int = 5
    ): String = withContext(Dispatchers.IO) {
        try {
            // 生成上下文
            return@withContext rag.generateContext(query, limit)
        } catch (e: Exception) {
            logger.error(e) { "Error generating context" }
            return@withContext ""
        }
    }
    
    /**
     * 检索上下文。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @return 检索上下文结果
     */
    suspend fun retrieveContext(
        query: String,
        limit: Int = 5
    ): RetrieveContextResult = withContext(Dispatchers.IO) {
        try {
            // 检索上下文
            return@withContext rag.retrieveContext(query, limit)
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving context" }
            return@withContext RetrieveContextResult("", emptyList())
        }
    }
    
    /**
     * 应用时间衰减。
     *
     * @param results 搜索结果列表
     * @return 应用时间衰减后的结果列表
     */
    private fun applyTimeDecay(results: List<DocumentSearchResult>): List<DocumentSearchResult> {
        val now = System.currentTimeMillis()
        
        return results.map { result ->
            val timestamp = documentTimestamps[result.document.id] ?: now
            val age = now - timestamp
            val decayFactor = Math.exp(-config.timeDecayFactor * age / config.maxDocumentAge)
            
            DocumentSearchResult(
                document = result.document,
                score = result.score * decayFactor
            )
        }.sortedByDescending { it.score }
    }
    
    /**
     * 清理旧文档。
     */
    private suspend fun cleanupOldDocuments() {
        val now = System.currentTimeMillis()
        val oldDocumentIds = mutableListOf<String>()
        
        // 找出过期的文档
        documentTimestamps.forEach { (id, timestamp) ->
            val age = now - timestamp
            if (age > config.maxDocumentAge) {
                oldDocumentIds.add(id)
            }
        }
        
        // 如果文档数量超过最大值，删除最旧的文档
        if (documentTimestamps.size > config.maxDocuments) {
            val excessCount = documentTimestamps.size - config.maxDocuments
            val oldestDocuments = documentTimestamps.entries
                .sortedBy { it.value }
                .take(excessCount)
                .map { it.key }
            
            oldDocumentIds.addAll(oldestDocuments)
        }
        
        // 删除文档
        if (oldDocumentIds.isNotEmpty()) {
            // TODO: 实现删除文档的功能
            // rag.deleteDocuments(oldDocumentIds)
            
            // 移除时间戳
            oldDocumentIds.forEach { documentTimestamps.remove(it) }
        }
    }
}
