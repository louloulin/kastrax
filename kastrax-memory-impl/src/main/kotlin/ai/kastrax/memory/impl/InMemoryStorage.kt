package ai.kastrax.memory.impl

import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MemoryPriority
import ai.kastrax.memory.api.MemoryThread
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * 内存存储的内存实现，用于开发和测试。
 * 注意：此实现不持久化数据，应用重启后数据会丢失。
 */
class InMemoryStorage : MemoryStorage {
    private val messagesMutex = Mutex()
    private val threadsMutex = Mutex()
    private val priorityMutex = Mutex()

    private val messages = mutableMapOf<String, MutableList<MemoryMessage>>()
    private val threads = mutableMapOf<String, MemoryThread>()
    private val messagePriorities = mutableMapOf<String, MemoryPriority>()
    private val messageLastAccessed = mutableMapOf<String, Instant>()
    private val messageAccessCounts = mutableMapOf<String, Int>()

    // 消息ID到线程ID的映射，用于快速查找消息所属的线程
    private val messageToThread = mutableMapOf<String, String>()

    override suspend fun saveMessage(message: MemoryMessage): String {
        messagesMutex.withLock {
            val threadMessages = messages.getOrPut(message.threadId) { mutableListOf() }
            threadMessages.add(message)
            // 按时间排序
            threadMessages.sortByDescending { it.createdAt }

            // 记录消息ID到线程ID的映射
            messageToThread[message.id] = message.threadId

            // 保存优先级信息（如果有）
            message.priority?.let { priority ->
                priorityMutex.withLock {
                    messagePriorities[message.id] = priority
                }
            }

            // 初始化访问信息
            messageLastAccessed[message.id] = message.lastAccessedAt ?: message.createdAt
            messageAccessCounts[message.id] = message.accessCount
        }
        return message.id
    }

    override suspend fun getMessages(threadId: String, limit: Int): List<MemoryMessage> {
        val result = messagesMutex.withLock {
            val threadMessages = messages[threadId]?.take(limit) ?: emptyList()

            // 更新访问信息
            val now = Clock.System.now()
            for (message in threadMessages) {
                messageLastAccessed[message.id] = now
                messageAccessCounts[message.id] = (messageAccessCounts[message.id] ?: 0) + 1
            }

            // 返回消息，可能需要附加优先级信息
            threadMessages.map { message ->
                val priority = priorityMutex.withLock { messagePriorities[message.id] }
                val lastAccessed = messageLastAccessed[message.id]
                val accessCount = messageAccessCounts[message.id] ?: 0

                if (priority != null || lastAccessed != null || accessCount > 0) {
                    message.copy(
                        priority = priority,
                        lastAccessedAt = lastAccessed,
                        accessCount = accessCount
                    )
                } else {
                    message
                }
            }
        }

        return result
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
                // 获取要删除的消息ID列表
                val messageIds = messages[threadId]?.map { it.id } ?: emptyList()

                // 删除消息
                messages.remove(threadId)

                // 删除消息相关的优先级和访问信息
                priorityMutex.withLock {
                    for (messageId in messageIds) {
                        messagePriorities.remove(messageId)
                        messageLastAccessed.remove(messageId)
                        messageAccessCounts.remove(messageId)
                        messageToThread.remove(messageId)
                    }
                }
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

    override suspend fun deleteMessage(messageId: String): Boolean {
        return messagesMutex.withLock {
            // 获取消息所属的线程ID
            val threadId = messageToThread[messageId] ?: return@withLock false

            // 从线程的消息列表中删除消息
            val threadMessages = messages[threadId] ?: return@withLock false
            val removed = threadMessages.removeIf { it.id == messageId }

            if (removed) {
                // 删除消息相关的优先级和访问信息
                priorityMutex.withLock {
                    messagePriorities.remove(messageId)
                    messageLastAccessed.remove(messageId)
                    messageAccessCounts.remove(messageId)
                    messageToThread.remove(messageId)
                }
            }

            removed
        }
    }

    override suspend fun updateMessagePriority(messageId: String, priority: MemoryPriority): Boolean {
        return priorityMutex.withLock {
            // 检查消息是否存在
            if (!messageToThread.containsKey(messageId)) {
                return@withLock false
            }

            // 更新优先级
            messagePriorities[messageId] = priority
            true
        }
    }

    override suspend fun getMessagePriority(messageId: String): MemoryPriority? {
        return priorityMutex.withLock {
            messagePriorities[messageId]
        }
    }

    override suspend fun updateMessageAccess(messageId: String, lastAccessedAt: Instant, accessCount: Int): Boolean {
        return messagesMutex.withLock {
            // 检查消息是否存在
            if (!messageToThread.containsKey(messageId)) {
                return@withLock false
            }

            // 更新访问信息
            messageLastAccessed[messageId] = lastAccessedAt
            messageAccessCounts[messageId] = accessCount
            true
        }
    }

    override suspend fun getAllMessagesWithPriority(): List<MessagePriorityInfo> {
        return messagesMutex.withLock {
            val result = mutableListOf<MessagePriorityInfo>()

            // 遍历所有线程的所有消息
            for ((threadId, threadMessages) in messages) {
                for (message in threadMessages) {
                    val priority = priorityMutex.withLock { messagePriorities[message.id] }
                    val lastAccessed = messageLastAccessed[message.id]

                    result.add(MessagePriorityInfo(
                        messageId = message.id,
                        priority = priority,
                        lastAccessedAt = lastAccessed,
                        createdAt = message.createdAt
                    ))
                }
            }

            result
        }
    }
}