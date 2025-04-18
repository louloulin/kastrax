package ai.kastrax.memory

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.memory.Memory
import ai.kastrax.core.memory.MemoryMessage
import ai.kastrax.core.memory.MemoryThread
import ai.kastrax.core.memory.MemoryBuilder
import kotlinx.datetime.Clock
import java.util.UUID

/**
 * 内存系统实现。
 */
class MemoryImpl(
    private val storage: MemoryStorage,
    private val lastMessages: Int = 10,
    private val semanticRecall: Boolean = false
) : Memory, KastraXBase(component = "MEMORY", name = "memory") {
    
    override suspend fun saveMessage(message: LlmMessage, threadId: String): String {
        // 检查线程是否存在
        val thread = storage.getThread(threadId) ?: throw IllegalArgumentException("Thread not found: $threadId")
        
        // 创建内存消息
        val memoryMessage = MemoryMessage(
            id = UUID.randomUUID().toString(),
            threadId = threadId,
            message = message,
            createdAt = Clock.System.now()
        )
        
        // 保存消息
        val messageId = storage.saveMessage(memoryMessage)
        
        // 更新线程
        storage.updateThread(threadId, mapOf(
            "updatedAt" to Clock.System.now(),
            "messageCount" to (thread.messageCount + 1)
        ))
        
        return messageId
    }
    
    override suspend fun getMessages(threadId: String, limit: Int): List<MemoryMessage> {
        return storage.getMessages(threadId, limit)
    }
    
    override suspend fun searchMessages(query: String, threadId: String, limit: Int): List<MemoryMessage> {
        return if (semanticRecall) {
            // 如果启用了语义召回，使用搜索功能
            storage.searchMessages(query, threadId, limit)
        } else {
            // 否则，只返回最近的消息
            storage.getMessages(threadId, limit)
        }
    }
    
    override suspend fun createThread(title: String?): String {
        val thread = MemoryThread(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        return storage.createThread(thread)
    }
    
    override suspend fun deleteThread(threadId: String): Boolean {
        return storage.deleteThread(threadId)
    }
    
    override suspend fun getThread(threadId: String): MemoryThread? {
        return storage.getThread(threadId)
    }
    
    override suspend fun listThreads(limit: Int, offset: Int): List<MemoryThread> {
        return storage.listThreads(limit, offset)
    }
}

/**
 * 内存构建器实现。
 */
class MemoryBuilderImpl : MemoryBuilder {
    private var storage: MemoryStorage? = null
    private var lastMessages: Int = 10
    private var semanticRecall: Boolean = false
    
    override fun storage(storage: Any): MemoryBuilder {
        if (storage is MemoryStorage) {
            this.storage = storage
        }
        return this
    }
    
    override fun lastMessages(count: Int): MemoryBuilder {
        this.lastMessages = count
        return this
    }
    
    override fun semanticRecall(enabled: Boolean): MemoryBuilder {
        this.semanticRecall = enabled
        return this
    }
    
    override fun build(): Memory {
        val finalStorage = storage ?: InMemoryStorage()
        return MemoryImpl(
            storage = finalStorage,
            lastMessages = lastMessages,
            semanticRecall = semanticRecall
        )
    }
}

/**
 * 创建内存实例的DSL函数。
 */
fun memory(init: MemoryBuilderImpl.() -> Unit): Memory {
    val builder = MemoryBuilderImpl()
    builder.init()
    return builder.build()
}
