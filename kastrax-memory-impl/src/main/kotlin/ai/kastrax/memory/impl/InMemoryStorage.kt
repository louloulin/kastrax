package ai.kastrax.memory.impl

import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MemoryThread
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * 内存存储的内存实现，用于开发和测试。
 * 注意：此实现不持久化数据，应用重启后数据会丢失。
 */
class InMemoryStorage : MemoryStorage {
    private val messagesMutex = Mutex()
    private val threadsMutex = Mutex()
    
    private val messages = mutableMapOf<String, MutableList<MemoryMessage>>()
    private val threads = mutableMapOf<String, MemoryThread>()
    
    override suspend fun saveMessage(message: MemoryMessage): String {
        messagesMutex.withLock {
            val threadMessages = messages.getOrPut(message.threadId) { mutableListOf() }
            threadMessages.add(message)
            // 按时间排序
            threadMessages.sortByDescending { it.createdAt }
        }
        return message.id
    }
    
    override suspend fun getMessages(threadId: String, limit: Int): List<MemoryMessage> {
        return messagesMutex.withLock {
            messages[threadId]?.take(limit) ?: emptyList()
        }
    }
    
    override suspend fun searchMessages(query: String, threadId: String, limit: Int): List<MemoryMessage> {
        // 简单实现：在消息内容中搜索查询字符串
        // 在实际应用中，这里应该使用向量搜索或其他语义搜索方法
        return messagesMutex.withLock {
            messages[threadId]
                ?.filter { it.message.content.contains(query, ignoreCase = true) }
                ?.take(limit)
                ?: emptyList()
        }
    }
    
    override suspend fun createThread(thread: MemoryThread): String {
        threadsMutex.withLock {
            threads[thread.id] = thread
        }
        return thread.id
    }
    
    override suspend fun deleteThread(threadId: String): Boolean {
        val threadRemoved = threadsMutex.withLock {
            threads.remove(threadId) != null
        }
        
        if (threadRemoved) {
            messagesMutex.withLock {
                messages.remove(threadId)
            }
        }
        
        return threadRemoved
    }
    
    override suspend fun getThread(threadId: String): MemoryThread? {
        return threadsMutex.withLock {
            threads[threadId]
        }
    }
    
    override suspend fun listThreads(limit: Int, offset: Int): List<MemoryThread> {
        return threadsMutex.withLock {
            threads.values
                .sortedByDescending { it.updatedAt }
                .drop(offset)
                .take(limit)
        }
    }
    
    override suspend fun updateThread(threadId: String, updates: Map<String, Any>): Boolean {
        return threadsMutex.withLock {
            val thread = threads[threadId] ?: return@withLock false
            
            val updatedThread = thread.copy(
                title = updates["title"] as? String ?: thread.title,
                updatedAt = updates["updatedAt"] as? kotlinx.datetime.Instant ?: Clock.System.now(),
                messageCount = updates["messageCount"] as? Int ?: thread.messageCount
            )
            
            threads[threadId] = updatedThread
            true
        }
    }
}
