package actor.proto.metrics.prometheus

import actor.proto.metrics.Counter
import actor.proto.metrics.Gauge
import actor.proto.metrics.Histogram
import actor.proto.metrics.Metric
import actor.proto.metrics.MetricsRegistry
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Collector
import io.prometheus.client.hotspot.DefaultExports

/**
 * Prometheus 度量注册表
 */
class PrometheusMetricsRegistry : MetricsRegistry() {
    private val registry = CollectorRegistry.defaultRegistry
    
    init {
        // 注册 JVM 度量
        DefaultExports.initialize()
    }
    
    /**
     * 创建或获取一个 Prometheus 计数器
     */
    override fun counter(name: String, tags: Map<String, String>): Counter {
        val key = metricKey(name, tags)
        return getCounters()[key] as? Counter ?: PrometheusCounter(name, tags, registry).also {
            super.counter(name, tags) // 注册到父类
        }
    }
    
    /**
     * 创建或获取一个 Prometheus 仪表
     */
    override fun gauge(name: String, tags: Map<String, String>): Gauge {
        val key = metricKey(name, tags)
        return getGauges()[key] as? Gauge ?: PrometheusGauge(name, tags, registry).also {
            super.gauge(name, tags) // 注册到父类
        }
    }
    
    /**
     * 创建或获取一个 Prometheus 直方图
     */
    override fun histogram(name: String, tags: Map<String, String>): Histogram {
        val key = metricKey(name, tags)
        return getHistograms()[key] as? Histogram ?: PrometheusHistogram(name, tags, registry).also {
            super.histogram(name, tags) // 注册到父类
        }
    }
    
    /**
     * 获取 Prometheus 注册表
     */
    fun getRegistry(): CollectorRegistry = registry
}

/**
 * Prometheus 计数器实现
 */
class PrometheusCounter(
    override val name: String,
    override val tags: Map<String, String>,
    registry: CollectorRegistry
) : Counter {
    private val counter: io.prometheus.client.Counter
    
    init {
        val builder = io.prometheus.client.Counter.build()
            .name(name.replace('.', '_'))
            .help("Counter $name")
        
        if (tags.isNotEmpty()) {
            builder.labelNames(*tags.keys.toTypedArray())
        }
        
        counter = builder.register(registry)
    }
    
    override fun inc(value: Long) {
        if (tags.isEmpty()) {
            counter.inc(value.toDouble())
        } else {
            counter.labels(*tags.values.toTypedArray()).inc(value.toDouble())
        }
    }
}

/**
 * Prometheus 仪表实现
 */
class PrometheusGauge(
    override val name: String,
    override val tags: Map<String, String>,
    registry: CollectorRegistry
) : Gauge {
    private val gauge: io.prometheus.client.Gauge
    
    init {
        val builder = io.prometheus.client.Gauge.build()
            .name(name.replace('.', '_'))
            .help("Gauge $name")
        
        if (tags.isNotEmpty()) {
            builder.labelNames(*tags.keys.toTypedArray())
        }
        
        gauge = builder.register(registry)
    }
    
    override fun set(value: Double) {
        if (tags.isEmpty()) {
            gauge.set(value)
        } else {
            gauge.labels(*tags.values.toTypedArray()).set(value)
        }
    }
}

/**
 * Prometheus 直方图实现
 */
class PrometheusHistogram(
    override val name: String,
    override val tags: Map<String, String>,
    registry: CollectorRegistry
) : Histogram {
    private val histogram: io.prometheus.client.Histogram
    
    init {
        val builder = io.prometheus.client.Histogram.build()
            .name(name.replace('.', '_'))
            .help("Histogram $name")
        
        if (tags.isNotEmpty()) {
            builder.labelNames(*tags.keys.toTypedArray())
        }
        
        histogram = builder.register(registry)
    }
    
    override fun observe(value: Double) {
        if (tags.isEmpty()) {
            histogram.observe(value)
        } else {
            histogram.labels(*tags.values.toTypedArray()).observe(value)
        }
    }
}
