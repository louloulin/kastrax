package ai.kastrax.memory

import ai.kastrax.core.memory.MemoryMessage
import ai.kastrax.core.memory.MemoryThread

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
