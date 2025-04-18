package ai.kastrax.memory

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.LlmMessage
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID

/**
 * 内存系统接口，用于存储和检索对话历史。
 */
interface Memory {
    /**
     * 保存消息到指定的线程。
     *
     * @param message 要保存的消息
     * @param threadId 线程ID
     * @return 保存的消息ID
     */
    suspend fun saveMessage(message: LlmMessage, threadId: String): String
    
    /**
     * 获取指定线程的消息。
     *
     * @param threadId 线程ID
     * @param limit 返回的最大消息数量
     * @return 消息列表
     */
    suspend fun getMessages(threadId: String, limit: Int = 10): List<MemoryMessage>
    
    /**
     * 搜索指定线程中与查询相关的消息。
     *
     * @param query 搜索查询
     * @param threadId 线程ID
     * @param limit 返回的最大消息数量
     * @return 相关消息列表
     */
    suspend fun searchMessages(query: String, threadId: String, limit: Int = 5): List<MemoryMessage>
    
    /**
     * 创建新的线程。
     *
     * @param title 线程标题（可选）
     * @return 新线程的ID
     */
    suspend fun createThread(title: String? = null): String
    
    /**
     * 删除指定的线程。
     *
     * @param threadId 要删除的线程ID
     * @return 是否成功删除
     */
    suspend fun deleteThread(threadId: String): Boolean
    
    /**
     * 获取线程信息。
     *
     * @param threadId 线程ID
     * @return 线程信息
     */
    suspend fun getThread(threadId: String): MemoryThread?
    
    /**
     * 列出所有线程。
     *
     * @param limit 返回的最大线程数量
     * @param offset 分页偏移量
     * @return 线程列表
     */
    suspend fun listThreads(limit: Int = 20, offset: Int = 0): List<MemoryThread>
}

/**
 * 内存中存储的消息。
 *
 * @property id 消息ID
 * @property threadId 线程ID
 * @property message LLM消息
 * @property createdAt 创建时间
 */
data class MemoryMessage(
    val id: String,
    val threadId: String,
    val message: LlmMessage,
    val createdAt: Instant
)

/**
 * 内存中的线程信息。
 *
 * @property id 线程ID
 * @property title 线程标题
 * @property createdAt 创建时间
 * @property updatedAt 最后更新时间
 * @property messageCount 消息数量
 */
data class MemoryThread(
    val id: String,
    val title: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val messageCount: Int = 0
)

/**
 * 内存构建器，用于创建内存实例。
 */
class MemoryBuilder {
    var storage: MemoryStorage? = null
    var lastMessages: Int = 10
    var semanticRecall: Boolean = false
    
    /**
     * 构建内存实例。
     */
    fun build(): Memory {
        val finalStorage = storage ?: InMemoryStorage()
        return MemoryImpl(
            storage = finalStorage,
            lastMessages = lastMessages,
            semanticRecall = semanticRecall
        )
    }
}

/**
 * 内存存储接口，定义了存储和检索消息的方法。
 */
interface MemoryStorage {
    /**
     * 保存消息。
     */
    suspend fun saveMessage(message: MemoryMessage): String
    
    /**
     * 获取消息。
     */
    suspend fun getMessages(threadId: String, limit: Int): List<MemoryMessage>
    
    /**
     * 搜索消息。
     */
    suspend fun searchMessages(query: String, threadId: String, limit: Int): List<MemoryMessage>
    
    /**
     * 创建线程。
     */
    suspend fun createThread(thread: MemoryThread): String
    
    /**
     * 删除线程。
     */
    suspend fun deleteThread(threadId: String): Boolean
    
    /**
     * 获取线程。
     */
    suspend fun getThread(threadId: String): MemoryThread?
    
    /**
     * 列出线程。
     */
    suspend fun listThreads(limit: Int, offset: Int): List<MemoryThread>
    
    /**
     * 更新线程。
     */
    suspend fun updateThread(threadId: String, updates: Map<String, Any>): Boolean
}

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
 * 创建内存实例的DSL函数。
 */
fun memory(init: MemoryBuilder.() -> Unit): Memory {
    val builder = MemoryBuilder()
    builder.init()
    return builder.build()
}
