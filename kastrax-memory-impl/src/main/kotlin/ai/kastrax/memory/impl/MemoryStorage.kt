package ai.kastrax.memory.impl

import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MemoryPriority
import ai.kastrax.memory.api.MemoryThread
import kotlinx.datetime.Instant

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
     * 删除消息。
     */
    suspend fun deleteMessage(messageId: String): Boolean

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

    /**
     * 更新消息的优先级。
     */
    suspend fun updateMessagePriority(messageId: String, priority: MemoryPriority): Boolean

    /**
     * 获取消息的优先级。
     */
    suspend fun getMessagePriority(messageId: String): MemoryPriority?

    /**
     * 更新消息的访问信息。
     */
    suspend fun updateMessageAccess(messageId: String, lastAccessedAt: Instant, accessCount: Int): Boolean

    /**
     * 获取所有消息的优先级和访问信息。
     * 返回三元组列表：(messageId, priority, lastAccessedAt, createdAt)
     */
    suspend fun getAllMessagesWithPriority(): List<MessagePriorityInfo>
}
