package ai.kastrax.memory.api

/**
 * 记忆优先级枚举，用于标记消息的重要性。
 */
enum class MemoryPriority(val value: Int) {
    /**
     * 低优先级，用于不太重要的消息，可能会被较早遗忘。
     */
    LOW(0),

    /**
     * 中等优先级，默认优先级。
     */
    MEDIUM(1),

    /**
     * 高优先级，用于重要消息，不容易被遗忘。
     */
    HIGH(2),

    /**
     * 关键优先级，用于必须保留的关键信息，几乎不会被遗忘。
     */
    CRITICAL(3);

    companion object {
        /**
         * 从整数值获取优先级。
         */
        fun fromValue(value: Int): MemoryPriority {
            return values().find { it.value == value } ?: MEDIUM
        }
    }
}

/**
 * 记忆优先级配置，用于配置优先级相关的参数。
 *
 * @property enablePriority 是否启用优先级机制
 * @property defaultPriority 默认优先级
 * @property priorityDecayRate 优先级衰减率（每天）
 * @property retentionPeriod 不同优先级的保留期（天数）
 */
data class MemoryPriorityConfig(
    val enablePriority: Boolean = true,
    val defaultPriority: MemoryPriority = MemoryPriority.MEDIUM,
    val priorityDecayRate: Double = 0.1,
    val retentionPeriod: Map<MemoryPriority, Int> = mapOf(
        MemoryPriority.LOW to 7,      // 低优先级消息保留7天
        MemoryPriority.MEDIUM to 30,  // 中等优先级消息保留30天
        MemoryPriority.HIGH to 90,    // 高优先级消息保留90天
        MemoryPriority.CRITICAL to 365 // 关键优先级消息保留365天
    )
)



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
