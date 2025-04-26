package ai.kastrax.memory.impl

import ai.kastrax.memory.api.MemoryPriority
import kotlinx.datetime.Instant

/**
 * 消息优先级信息，用于存储消息的优先级和访问信息。
 *
 * @property messageId 消息ID
 * @property priority 优先级
 * @property lastAccessedAt 最后访问时间
 * @property createdAt 创建时间
 */
data class MessagePriorityInfo(
    val messageId: String,
    val priority: MemoryPriority?,
    val lastAccessedAt: Instant?,
    val createdAt: Instant?
)
