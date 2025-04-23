package ai.kastrax.memory.impl

import ai.kastrax.memory.api.MemoryMessage
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 内存统计类，用于跟踪内存使用情况。
 */
class MemoryStats {
    // 总消息数
    private val totalMessages = AtomicLong(0)
    
    // 每个线程的消息数
    private val threadMessageCounts = ConcurrentHashMap<String, AtomicLong>()
    
    // 每个角色的消息数
    private val roleMessageCounts = ConcurrentHashMap<String, AtomicLong>()
    
    // 每小时的消息数
    private val hourlyMessageCounts = ConcurrentHashMap<String, AtomicLong>()
    
    // 开始时间
    private val startTime: Instant = Clock.System.now()
    
    /**
     * 记录一条消息。
     */
    fun recordMessage(message: MemoryMessage) {
        // 增加总消息数
        totalMessages.incrementAndGet()
        
        // 增加线程消息数
        threadMessageCounts.computeIfAbsent(message.threadId) { AtomicLong(0) }.incrementAndGet()
        
        // 增加角色消息数
        val role = message.message.role.name
        roleMessageCounts.computeIfAbsent(role) { AtomicLong(0) }.incrementAndGet()
        
        // 增加小时消息数
        val hour = formatHour(message.createdAt)
        hourlyMessageCounts.computeIfAbsent(hour) { AtomicLong(0) }.incrementAndGet()
    }
    
    /**
     * 获取总消息数。
     */
    fun getTotalMessages(): Long {
        return totalMessages.get()
    }
    
    /**
     * 获取线程消息数。
     */
    fun getThreadMessageCount(threadId: String): Long {
        return threadMessageCounts[threadId]?.get() ?: 0
    }
    
    /**
     * 获取角色消息数。
     */
    fun getRoleMessageCount(role: String): Long {
        return roleMessageCounts[role]?.get() ?: 0
    }
    
    /**
     * 获取小时消息数。
     */
    fun getHourlyMessageCount(hour: String): Long {
        return hourlyMessageCounts[hour]?.get() ?: 0
    }
    
    /**
     * 获取所有线程的消息数。
     */
    fun getAllThreadMessageCounts(): Map<String, Long> {
        return threadMessageCounts.mapValues { it.value.get() }
    }
    
    /**
     * 获取所有角色的消息数。
     */
    fun getAllRoleMessageCounts(): Map<String, Long> {
        return roleMessageCounts.mapValues { it.value.get() }
    }
    
    /**
     * 获取所有小时的消息数。
     */
    fun getAllHourlyMessageCounts(): Map<String, Long> {
        return hourlyMessageCounts.mapValues { it.value.get() }
    }
    
    /**
     * 获取运行时间（毫秒）。
     */
    fun getUptime(): Long {
        return Clock.System.now().toEpochMilliseconds() - startTime.toEpochMilliseconds()
    }
    
    /**
     * 重置统计信息。
     */
    fun reset() {
        totalMessages.set(0)
        threadMessageCounts.clear()
        roleMessageCounts.clear()
        hourlyMessageCounts.clear()
    }
    
    /**
     * 格式化小时。
     */
    private fun formatHour(instant: Instant): String {
        val epochSeconds = instant.epochSeconds
        val epochHours = epochSeconds / 3600
        return epochHours.toString()
    }
}
