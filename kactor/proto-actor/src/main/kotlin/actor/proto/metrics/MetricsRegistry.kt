package actor.proto.metrics

import java.util.concurrent.ConcurrentHashMap

/**
 * 度量注册表，用于管理所有度量
 */
open class MetricsRegistry {
    private val counters = ConcurrentHashMap<String, Counter>()
    private val gauges = ConcurrentHashMap<String, Gauge>()
    private val histograms = ConcurrentHashMap<String, Histogram>()
    
    /**
     * 创建或获取一个计数器
     * @param name 计数器名称
     * @param tags 计数器标签
     * @return 计数器实例
     */
    open fun counter(name: String, tags: Map<String, String> = emptyMap()): Counter {
        val key = metricKey(name, tags)
        return counters.computeIfAbsent(key) { DefaultCounter(name, tags) }
    }
    
    /**
     * 创建或获取一个仪表
     * @param name 仪表名称
     * @param tags 仪表标签
     * @return 仪表实例
     */
    open fun gauge(name: String, tags: Map<String, String> = emptyMap()): Gauge {
        val key = metricKey(name, tags)
        return gauges.computeIfAbsent(key) { DefaultGauge(name, tags) }
    }
    
    /**
     * 创建或获取一个直方图
     * @param name 直方图名称
     * @param tags 直方图标签
     * @return 直方图实例
     */
    open fun histogram(name: String, tags: Map<String, String> = emptyMap()): Histogram {
        val key = metricKey(name, tags)
        return histograms.computeIfAbsent(key) { DefaultHistogram(name, tags) }
    }
    
    /**
     * 获取所有计数器
     */
    fun getCounters(): Map<String, Counter> = counters.toMap()
    
    /**
     * 获取所有仪表
     */
    fun getGauges(): Map<String, Gauge> = gauges.toMap()
    
    /**
     * 获取所有直方图
     */
    fun getHistograms(): Map<String, Histogram> = histograms.toMap()
    
    /**
     * 清除所有度量
     */
    fun clear() {
        counters.clear()
        gauges.clear()
        histograms.clear()
    }
    
    /**
     * 生成度量的唯一键
     */
    protected fun metricKey(name: String, tags: Map<String, String>): String {
        val sortedTags = tags.entries.sortedBy { it.key }
        return name + sortedTags.joinToString("") { "${it.key}=${it.value}" }
    }
}
