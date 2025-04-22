package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.memory.api.EmbeddingGenerator
import ai.kastrax.memory.api.Message
import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.SemanticMemory
import ai.kastrax.memory.api.SemanticRecallConfig
import ai.kastrax.memory.api.SemanticSearchResult
import ai.kastrax.memory.api.VectorStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import java.util.UUID

/**
 * 内存中的语义内存实现。
 */
class InMemorySemanticMemory(
    private val embeddingGenerator: EmbeddingGenerator,
    private val vectorStorage: VectorStorage
) : SemanticMemory, KastraXBase(component = "SEMANTIC_MEMORY", name = "in-memory") {
    private val mutex = Mutex()
    private val messages = mutableMapOf<String, MemoryMessage>()
    
    override suspend fun saveMessage(message: Message, threadId: String): String {
        val messageId = UUID.randomUUID().toString()
        
        // 创建内存消息
        val memoryMessage = MemoryMessage(
            id = messageId,
            threadId = threadId,
            message = message,
            createdAt = Clock.System.now()
        )
        
        // 保存消息
        mutex.withLock {
            messages[messageId] = memoryMessage
        }
        
        // 生成嵌入向量
        try {
            val embedding = embeddingGenerator.generateEmbedding(message.content)
            
            // 保存向量
            vectorStorage.saveVector(
                id = messageId,
                vector = embedding,
                metadata = mapOf(
                    "threadId" to threadId,
                    "role" to message.role,
                    "createdAt" to memoryMessage.createdAt.toString()
                )
            )
        } catch (e: Exception) {
            logger.error("生成嵌入向量失败: ${e.message}")
        }
        
        return messageId
    }
    
    override suspend fun semanticSearch(
        query: String,
        threadId: String,
        config: SemanticRecallConfig
    ): List<SemanticSearchResult> {
        try {
            // 生成查询向量
            val queryEmbedding = embeddingGenerator.generateEmbedding(query)
            
            // 搜索相似向量
            val results = vectorStorage.searchVectors(
                vector = queryEmbedding,
                limit = config.topK,
                minScore = config.minScore,
                filter = mapOf("threadId" to threadId)
            )
            
            // 获取消息
            return mutex.withLock {
                results.mapNotNull { (id, score, _) ->
                    val message = messages[id] ?: return@mapNotNull null
                    SemanticSearchResult(message, score)
                }
            }
        } catch (e: Exception) {
            logger.error("语义搜索失败: ${e.message}")
            return emptyList()
        }
    }
    
    override suspend fun getSemanticRecallMessages(
        query: String,
        threadId: String,
        config: SemanticRecallConfig
    ): List<MemoryMessage> {
        // 搜索相似消息
        val searchResults = semanticSearch(query, threadId, config)
        
        // 如果没有结果，返回空列表
        if (searchResults.isEmpty()) {
            return emptyList()
        }
        
        // 获取所有消息
        val allMessages = mutex.withLock {
            messages.values.filter { it.threadId == threadId }
                .sortedBy { it.createdAt }
        }
        
        // 为每个结果添加上下文消息
        val resultMessages = mutableSetOf<MemoryMessage>()
        
        for (result in searchResults) {
            val index = allMessages.indexOfFirst { it.id == result.message.id }
            if (index == -1) continue
            
            // 添加当前消息
            resultMessages.add(result.message)
            
            // 添加前后消息
            val start = maxOf(0, index - config.messageRange)
            val end = minOf(allMessages.size - 1, index + config.messageRange)
            
            for (i in start..end) {
                resultMessages.add(allMessages[i])
            }
        }
        
        // 按时间排序
        return resultMessages.sortedBy { it.createdAt }
    }
}
