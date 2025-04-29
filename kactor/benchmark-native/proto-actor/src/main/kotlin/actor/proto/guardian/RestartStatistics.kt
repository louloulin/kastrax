package actor.proto.guardian

import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

/**
 * RestartStatistics 跟踪 actor 重启的次数和时间
 */
class RestartStatistics {
    private val failureTimes = CopyOnWriteArrayList<Instant>()
    
    /**
     * 获取失败次数
     * @return 失败次数
     */
    fun failureCount(): Int = failureTimes.size
    
    /**
     * 记录一次失败
     */
    fun fail() {
        failureTimes.add(Instant.now())
    }
    
    /**
     * 重置失败计数
     */
    fun reset() {
        failureTimes.clear()
    }
    
    /**
     * 获取指定时间段内的失败次数
     * @param withinDuration 时间段
     * @return 失败次数
     */
    fun numberOfFailures(withinDuration: Duration): Int {
        if (withinDuration == Duration.ZERO) {
            return failureTimes.size
        }
        
        val now = Instant.now()
        return failureTimes.count { now.minus(withinDuration).isBefore(it) }
    }
}
