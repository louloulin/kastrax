package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.memory.api.MemoryPriority
import ai.kastrax.memory.api.MemoryPriorityConfig
import ai.kastrax.memory.api.MemoryPriorityProcessor
import ai.kastrax.memory.api.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * 基础的记忆优先级处理器实现。
 *
 * @property storage 内存存储
 */
class BasicMemoryPriorityProcessor(
    private val storage: MemoryStorage
) : MemoryPriorityProcessor, KastraXBase(component = "MEMORY_PRIORITY", name = "basic") {

    // 内存中的优先级缓存
    private val priorityCache = ConcurrentHashMap<String, MemoryPriority>()

    // 内存中的最后访问时间缓存
    private val lastAccessCache = ConcurrentHashMap<String, Instant>()

    // 内存中的访问计数缓存
    private val accessCountCache = ConcurrentHashMap<String, Int>()

    override suspend fun calculatePriority(message: Message, config: MemoryPriorityConfig): MemoryPriority {
        if (!config.enablePriority) {
            return config.defaultPriority
        }

        // 这里可以实现更复杂的优先级计算逻辑
        // 例如，基于消息内容、角色等因素

        // 简单实现：根据消息角色确定优先级
        return when (message.role) {
            ai.kastrax.memory.api.MessageRole.SYSTEM -> MemoryPriority.CRITICAL
            ai.kastrax.memory.api.MessageRole.USER -> MemoryPriority.HIGH
            ai.kastrax.memory.api.MessageRole.ASSISTANT -> MemoryPriority.MEDIUM
            ai.kastrax.memory.api.MessageRole.TOOL -> MemoryPriority.LOW
        }
    }

    override suspend fun updatePriority(messageId: String, priority: MemoryPriority): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 更新存储中的优先级
                val success = storage.updateMessagePriority(messageId, priority)

                // 如果更新成功，也更新缓存
                if (success) {
                    priorityCache[messageId] = priority
                }

                success
            } catch (e: Exception) {
                logger.error("更新消息优先级失败: ${e.message}")
                false
            }
        }
    }

    override suspend fun getPriority(messageId: String): MemoryPriority? {
        return withContext(Dispatchers.IO) {
            try {
                // 首先检查缓存
                priorityCache[messageId]?.let { return@withContext it }

                // 如果缓存中没有，从存储中获取
                val priority = storage.getMessagePriority(messageId)

                // 更新缓存
                if (priority != null) {
                    priorityCache[messageId] = priority
                }

                priority
            } catch (e: Exception) {
                logger.error("获取消息优先级失败: ${e.message}")
                null
            }
        }
    }

    override suspend fun applyPriorityDecay(config: MemoryPriorityConfig): Int {
        if (!config.enablePriority || config.priorityDecayRate <= 0) {
            return 0
        }

        return withContext(Dispatchers.IO) {
            try {
                // 获取所有消息的优先级和最后访问时间
                val messagesWithPriority = storage.getAllMessagesWithPriority()
                var updatedCount = 0

                val now = Clock.System.now()

                for ((messageId, priority, lastAccessed) in messagesWithPriority) {
                    if (priority == null || lastAccessed == null) continue

                    // 计算自上次访问以来的天数
                    val daysSinceLastAccess = (now.toEpochMilliseconds() - lastAccessed.toEpochMilliseconds()) / (24 * 60 * 60 * 1000)

                    // 如果天数为0，跳过
                    if (daysSinceLastAccess <= 0) continue

                    // 计算衰减后的优先级
                    val decayAmount = (daysSinceLastAccess * config.priorityDecayRate).toInt()

                    if (decayAmount > 0) {
                        // 计算新的优先级值
                        val newPriorityValue = (priority.value - decayAmount).coerceAtLeast(MemoryPriority.LOW.value)
                        val newPriority = MemoryPriority.fromValue(newPriorityValue)

                        // 如果优先级有变化，更新它
                        if (newPriority != priority) {
                            if (updatePriority(messageId, newPriority)) {
                                updatedCount++
                            }
                        }
                    }
                }

                updatedCount
            } catch (e: Exception) {
                logger.error("应用优先级衰减失败: ${e.message}")
                0
            }
        }
    }

    override suspend fun cleanupLowPriorityMessages(config: MemoryPriorityConfig): Int {
        if (!config.enablePriority) {
            return 0
        }

        return withContext(Dispatchers.IO) {
            try {
                // 获取所有消息的优先级和创建时间
                val messagesWithPriority = storage.getAllMessagesWithPriority()
                var deletedCount = 0

                val now = Clock.System.now()

                for ((messageId, priority, _, createdAt) in messagesWithPriority) {
                    if (priority == null || createdAt == null) continue

                    // 获取该优先级的保留期
                    val retentionDays = config.retentionPeriod[priority] ?: continue

                    // 计算消息的年龄（天）
                    val messageAgeDays = (now.toEpochMilliseconds() - createdAt.toEpochMilliseconds()) / (24 * 60 * 60 * 1000)

                    // 如果消息超过了保留期，删除它
                    if (messageAgeDays > retentionDays) {
                        if (storage.deleteMessage(messageId)) {
                            // 从缓存中移除
                            priorityCache.remove(messageId)
                            lastAccessCache.remove(messageId)
                            accessCountCache.remove(messageId)

                            deletedCount++
                        }
                    }
                }

                deletedCount
            } catch (e: Exception) {
                logger.error("清理低优先级消息失败: ${e.message}")
                0
            }
        }
    }

    /**
     * 记录消息访问。
     *
     * @param messageId 消息ID
     */
    suspend fun recordAccess(messageId: String) {
        withContext(Dispatchers.IO) {
            try {
                // 更新最后访问时间
                val now = Clock.System.now()
                lastAccessCache[messageId] = now

                // 更新访问计数
                val currentCount = accessCountCache.getOrDefault(messageId, 0)
                accessCountCache[messageId] = currentCount + 1

                // 更新存储
                storage.updateMessageAccess(messageId, now, currentCount + 1)
            } catch (e: Exception) {
                logger.error("记录消息访问失败: ${e.message}")
            }
        }
    }
}
