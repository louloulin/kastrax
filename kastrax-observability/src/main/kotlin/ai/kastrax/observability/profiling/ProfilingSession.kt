package ai.kastrax.observability.profiling

import java.time.Instant

/**
 * 性能分析会话接口。
 * 表示一次性能分析的会话。
 */
interface ProfilingSession {
    /**
     * 获取会话ID。
     *
     * @return 会话ID
     */
    fun getId(): String

    /**
     * 获取会话名称。
     *
     * @return 会话名称
     */
    fun getName(): String

    /**
     * 获取会话开始时间。
     *
     * @return 会话开始时间
     */
    fun getStartTime(): Instant

    /**
     * 获取会话结束时间。
     *
     * @return 会话结束时间，如果会话尚未结束则返回null
     */
    fun getEndTime(): Instant?

    /**
     * 获取会话状态。
     *
     * @return 会话状态
     */
    fun getStatus(): SessionStatus

    /**
     * 获取会话标签。
     *
     * @return 会话标签
     */
    fun getTags(): Map<String, String>

    /**
     * 添加会话标签。
     *
     * @param key 标签键
     * @param value 标签值
     */
    fun addTag(key: String, value: String)

    /**
     * 开始一个子会话。
     *
     * @param name 子会话名称
     * @return 子会话
     */
    fun startSubSession(name: String): ProfilingSession

    /**
     * 开始一个子会话，并执行指定的代码块。
     *
     * @param name 子会话名称
     * @param block 要执行的代码块
     * @return 代码块的返回值
     */
    fun <T> withSubSession(name: String, block: (ProfilingSession) -> T): T

    /**
     * 获取所有子会话。
     *
     * @return 子会话列表
     */
    fun getSubSessions(): List<ProfilingSession>

    /**
     * 获取会话指标。
     *
     * @return 会话指标
     */
    fun getMetrics(): Map<String, Any>

    /**
     * 添加会话指标。
     *
     * @param key 指标键
     * @param value 指标值
     */
    fun addMetric(key: String, value: Any)

    /**
     * 记录一个事件。
     *
     * @param name 事件名称
     * @param attributes 事件属性
     */
    fun recordEvent(name: String, attributes: Map<String, String> = emptyMap())

    /**
     * 获取所有记录的事件。
     *
     * @return 事件列表
     */
    fun getEvents(): List<ProfilingEvent>

    /**
     * 结束会话。
     *
     * @return 会话结果
     */
    fun end(): ProfilingResult

    /**
     * 取消会话。
     *
     * @param reason 取消原因
     */
    fun cancel(reason: String)

    /**
     * 检查会话是否已结束。
     *
     * @return 如果会话已结束则返回true，否则返回false
     */
    fun isEnded(): Boolean
}

/**
 * 会话状态枚举。
 */
enum class SessionStatus {
    /**
     * 会话正在进行中。
     */
    ACTIVE,

    /**
     * 会话已成功完成。
     */
    COMPLETED,

    /**
     * 会话已取消。
     */
    CANCELLED,

    /**
     * 会话执行出错。
     */
    ERROR
}

/**
 * 性能分析事件类。
 *
 * @property name 事件名称
 * @property timestamp 事件时间戳
 * @property attributes 事件属性
 */
data class ProfilingEvent(
    val name: String,
    val timestamp: Instant,
    val attributes: Map<String, String> = emptyMap()
)

/**
 * 性能分析结果类。
 *
 * @property sessionId 会话ID
 * @property name 会话名称
 * @property startTime 开始时间
 * @property endTime 结束时间
 * @property duration 持续时间（毫秒）
 * @property status 会话状态
 * @property tags 会话标签
 * @property metrics 会话指标
 * @property events 会话事件
 * @property subSessions 子会话结果
 */
data class ProfilingResult(
    val sessionId: String,
    val name: String,
    val startTime: Instant,
    val endTime: Instant,
    val duration: Long,
    val status: SessionStatus,
    val tags: Map<String, String>,
    val metrics: Map<String, Any>,
    val events: List<ProfilingEvent>,
    val subSessions: List<ProfilingResult> = emptyList()
)
