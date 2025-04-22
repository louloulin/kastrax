package ai.kastrax.observability.metrics

import java.util.concurrent.ConcurrentHashMap

/**
 * 指标注册表接口。
 */
interface MetricRegistry {
    /**
     * 注册指标。
     *
     * @param metric 指标
     */
    fun register(metric: Metric)

    /**
     * 注销指标。
     *
     * @param name 指标名称
     * @param tags 指标标签
     */
    fun unregister(name: String, tags: Map<String, String> = emptyMap())

    /**
     * 获取指标。
     *
     * @param name 指标名称
     * @param tags 指标标签
     * @return 指标，如果不存在则返回 null
     */
    fun getMetric(name: String, tags: Map<String, String> = emptyMap()): Metric?

    /**
     * 获取所有指标。
     *
     * @return 所有指标
     */
    fun getMetrics(): Collection<Metric>

    /**
     * 创建计数器。
     *
     * @param name 指标名称
     * @param description 指标描述
     * @param tags 指标标签
     * @return 计数器
     */
    fun counter(name: String, description: String = "", tags: Map<String, String> = emptyMap()): Counter

    /**
     * 创建仪表。
     *
     * @param name 指标名称
     * @param description 指标描述
     * @param tags 指标标签
     * @return 仪表
     */
    fun gauge(name: String, description: String = "", tags: Map<String, String> = emptyMap()): Gauge

    /**
     * 创建直方图。
     *
     * @param name 指标名称
     * @param description 指标描述
     * @param tags 指标标签
     * @return 直方图
     */
    fun histogram(name: String, description: String = "", tags: Map<String, String> = emptyMap()): Histogram

    /**
     * 创建计时器。
     *
     * @param name 指标名称
     * @param description 指标描述
     * @param tags 指标标签
     * @return 计时器
     */
    fun timer(name: String, description: String = "", tags: Map<String, String> = emptyMap()): Timer
}

/**
 * 默认指标注册表实现。
 */
class DefaultMetricRegistry : MetricRegistry {
    private val metrics = ConcurrentHashMap<MetricId, Metric>()

    override fun register(metric: Metric) {
        val id = MetricId(metric.getName(), metric.getTags())
        metrics[id] = metric
    }

    override fun unregister(name: String, tags: Map<String, String>) {
        val id = MetricId(name, tags)
        metrics.remove(id)
    }

    override fun getMetric(name: String, tags: Map<String, String>): Metric? {
        val id = MetricId(name, tags)
        return metrics[id]
    }

    override fun getMetrics(): Collection<Metric> {
        return metrics.values
    }

    override fun counter(name: String, description: String, tags: Map<String, String>): Counter {
        val id = MetricId(name, tags)
        return metrics.getOrPut(id) {
            DefaultCounter(name, description, tags)
        } as Counter
    }

    override fun gauge(name: String, description: String, tags: Map<String, String>): Gauge {
        val id = MetricId(name, tags)
        return metrics.getOrPut(id) {
            DefaultGauge(name, description, tags)
        } as Gauge
    }

    override fun histogram(name: String, description: String, tags: Map<String, String>): Histogram {
        val id = MetricId(name, tags)
        return metrics.getOrPut(id) {
            DefaultHistogram(name, description, tags)
        } as Histogram
    }

    override fun timer(name: String, description: String, tags: Map<String, String>): Timer {
        val id = MetricId(name, tags)
        return metrics.getOrPut(id) {
            DefaultTimer(name, description, tags)
        } as Timer
    }

    /**
     * 指标 ID。
     *
     * @property name 指标名称
     * @property tags 指标标签
     */
    private data class MetricId(
        val name: String,
        val tags: Map<String, String>
    )
}

/**
 * 指标系统。
 */
object MetricsSystem {
    private var registry: MetricRegistry = DefaultMetricRegistry()

    /**
     * 设置指标注册表。
     *
     * @param metricRegistry 指标注册表
     */
    fun setMetricRegistry(metricRegistry: MetricRegistry) {
        registry = metricRegistry
    }

    /**
     * 获取指标注册表。
     *
     * @return 指标注册表
     */
    fun getMetricRegistry(): MetricRegistry {
        return registry
    }

    /**
     * 创建计数器。
     *
     * @param name 指标名称
     * @param description 指标描述
     * @param tags 指标标签
     * @return 计数器
     */
    fun counter(name: String, description: String = "", tags: Map<String, String> = emptyMap()): Counter {
        return registry.counter(name, description, tags)
    }

    /**
     * 创建仪表。
     *
     * @param name 指标名称
     * @param description 指标描述
     * @param tags 指标标签
     * @return 仪表
     */
    fun gauge(name: String, description: String = "", tags: Map<String, String> = emptyMap()): Gauge {
        return registry.gauge(name, description, tags)
    }

    /**
     * 创建直方图。
     *
     * @param name 指标名称
     * @param description 指标描述
     * @param tags 指标标签
     * @return 直方图
     */
    fun histogram(name: String, description: String = "", tags: Map<String, String> = emptyMap()): Histogram {
        return registry.histogram(name, description, tags)
    }

    /**
     * 创建计时器。
     *
     * @param name 指标名称
     * @param description 指标描述
     * @param tags 指标标签
     * @return 计时器
     */
    fun timer(name: String, description: String = "", tags: Map<String, String> = emptyMap()): Timer {
        return registry.timer(name, description, tags)
    }
}
