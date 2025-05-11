package ai.kastrax.codebase.util

import kotlin.time.Duration

/**
 * 时间扩展函数
 */
object TimeExtensions {
    /**
     * 将秒数转换为 Duration
     *
     * @return Duration
     */
    fun Int.seconds(): Duration = Duration.parse("${this}s")
    
    /**
     * 将毫秒数转换为 Duration
     *
     * @return Duration
     */
    fun Int.milliseconds(): Duration = Duration.parse("${this}ms")
    
    /**
     * 将分钟数转换为 Duration
     *
     * @return Duration
     */
    fun Int.minutes(): Duration = Duration.parse("${this}m")
    
    /**
     * 将小时数转换为 Duration
     *
     * @return Duration
     */
    fun Int.hours(): Duration = Duration.parse("${this}h")
    
    /**
     * 将天数转换为 Duration
     *
     * @return Duration
     */
    fun Int.days(): Duration = Duration.parse("${this}d")
}
