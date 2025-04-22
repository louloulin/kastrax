package ai.kastrax.observability.metrics

/**
 * 指标类型枚举。
 */
enum class MetricType {
    COUNTER,
    GAUGE,
    HISTOGRAM,
    TIMER
}

/**
 * 指标接口。
 */
interface Metric {
    /**
     * 获取指标名称。
     *
     * @return 指标名称
     */
    fun getName(): String

    /**
     * 获取指标类型。
     *
     * @return 指标类型
     */
    fun getType(): MetricType

    /**
     * 获取指标标签。
     *
     * @return 指标标签
     */
    fun getTags(): Map<String, String>

    /**
     * 获取指标描述。
     *
     * @return 指标描述
     */
    fun getDescription(): String
}

/**
 * 计数器指标接口。
 */
interface Counter : Metric {
    /**
     * 增加计数器值。
     *
     * @param amount 增加的数量
     */
    fun increment(amount: Long = 1)

    /**
     * 获取计数器当前值。
     *
     * @return 计数器当前值
     */
    fun getCount(): Long
}

/**
 * 仪表指标接口。
 */
interface Gauge : Metric {
    /**
     * 设置仪表值。
     *
     * @param value 仪表值
     */
    fun setValue(value: Double)

    /**
     * 获取仪表当前值。
     *
     * @return 仪表当前值
     */
    fun getValue(): Double
}

/**
 * 直方图指标接口。
 */
interface Histogram : Metric {
    /**
     * 记录值。
     *
     * @param value 值
     */
    fun record(value: Long)

    /**
     * 获取直方图摘要。
     *
     * @return 直方图摘要
     */
    fun getSummary(): HistogramSummary
}

/**
 * 直方图摘要。
 *
 * @property count 记录的值的数量
 * @property sum 记录的值的总和
 * @property min 记录的最小值
 * @property max 记录的最大值
 * @property mean 记录的值的平均值
 * @property percentiles 百分位数映射
 */
data class HistogramSummary(
    val count: Long,
    val sum: Long,
    val min: Long,
    val max: Long,
    val mean: Double,
    val percentiles: Map<Double, Long>
)

/**
 * 计时器指标接口。
 */
interface Timer : Metric {
    /**
     * 记录持续时间。
     *
     * @param durationMs 持续时间（毫秒）
     */
    fun record(durationMs: Long)

    /**
     * 开始计时。
     *
     * @return 计时上下文
     */
    fun start(): TimerContext

    /**
     * 获取计时器摘要。
     *
     * @return 计时器摘要
     */
    fun getSummary(): TimerSummary
}

/**
 * 计时上下文接口。
 */
interface TimerContext : AutoCloseable {
    /**
     * 停止计时并记录持续时间。
     *
     * @return 持续时间（毫秒）
     */
    fun stop(): Long
}

/**
 * 计时器摘要。
 *
 * @property count 记录的持续时间的数量
 * @property totalTime 记录的持续时间的总和（毫秒）
 * @property min 记录的最小持续时间（毫秒）
 * @property max 记录的最大持续时间（毫秒）
 * @property mean 记录的持续时间的平均值（毫秒）
 * @property percentiles 百分位数映射（毫秒）
 */
data class TimerSummary(
    val count: Long,
    val totalTime: Long,
    val min: Long,
    val max: Long,
    val mean: Double,
    val percentiles: Map<Double, Long>
)
