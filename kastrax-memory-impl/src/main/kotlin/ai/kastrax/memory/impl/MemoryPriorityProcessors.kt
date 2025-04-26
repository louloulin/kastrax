package ai.kastrax.memory.impl

import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MemoryPriority
import ai.kastrax.memory.api.MemoryProcessor
import ai.kastrax.memory.api.MemoryProcessorOptions

/**
 * 优先级过滤器，用于根据优先级过滤消息。
 *
 * @property minPriority 最小优先级
 */
class PriorityFilter(
    private val minPriority: MemoryPriority = MemoryPriority.LOW
) : MemoryProcessor {
    override suspend fun process(
        messages: List<MemoryMessage>,
        options: MemoryProcessorOptions
    ): List<MemoryMessage> {
        // 过滤掉低于指定优先级的消息
        return messages.filter { 
            val priority = it.priority ?: MemoryPriority.MEDIUM
            priority.value >= minPriority.value
        }
    }
}

/**
 * 优先级排序器，用于根据优先级对消息进行排序。
 *
 * @property prioritizeRecent 是否优先考虑最近的消息
 * @property recentWeight 最近消息的权重
 */
class PrioritySorter(
    private val prioritizeRecent: Boolean = true,
    private val recentWeight: Double = 0.3
) : MemoryProcessor {
    override suspend fun process(
        messages: List<MemoryMessage>,
        options: MemoryProcessorOptions
    ): List<MemoryMessage> {
        if (messages.isEmpty()) {
            return messages
        }
        
        // 获取最新和最旧的时间戳
        val latestTime = messages.maxOf { it.createdAt.toEpochMilliseconds() }
        val oldestTime = messages.minOf { it.createdAt.toEpochMilliseconds() }
        val timeRange = if (latestTime > oldestTime) (latestTime - oldestTime).toDouble() else 1.0
        
        // 根据优先级和时间排序
        return messages.sortedByDescending { message ->
            val priority = message.priority ?: MemoryPriority.MEDIUM
            val priorityScore = priority.value.toDouble()
            
            if (prioritizeRecent && timeRange > 0) {
                // 计算时间归一化分数 (0-1)
                val timeScore = (message.createdAt.toEpochMilliseconds() - oldestTime) / timeRange
                // 组合优先级和时间分数
                priorityScore + (timeScore * recentWeight)
            } else {
                priorityScore
            }
        }
    }
}
