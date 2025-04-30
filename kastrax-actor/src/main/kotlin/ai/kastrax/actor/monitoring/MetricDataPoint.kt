package ai.kastrax.actor.monitoring

import kotlinx.datetime.Instant

/**
 * 指标数据点，表示某个时间点的指标值
 *
 * @property timestamp 时间戳
 * @property value 指标值
 * @property tags 标签
 */
data class MetricDataPoint(
    val timestamp: Instant,
    val value: Double,
    val tags: Map<String, String> = emptyMap()
)
