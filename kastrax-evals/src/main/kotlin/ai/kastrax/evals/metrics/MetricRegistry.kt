package ai.kastrax.evals.metrics

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 指标注册表，用于管理和访问指标。
 */
object MetricRegistry {
    /**
     * 注册的指标映射。
     */
    private val metrics = mutableMapOf<String, Metric<*, *>>()
    
    /**
     * 注册指标。
     *
     * @param metric 指标
     * @return 是否成功注册
     */
    fun register(metric: Metric<*, *>): Boolean {
        val name = metric.name
        
        if (metrics.containsKey(name)) {
            logger.warn { "Metric with name '$name' already exists" }
            return false
        }
        
        metrics[name] = metric
        logger.info { "Registered metric: $name" }
        return true
    }
    
    /**
     * 注销指标。
     *
     * @param name 指标名称
     * @return 是否成功注销
     */
    fun unregister(name: String): Boolean {
        if (!metrics.containsKey(name)) {
            logger.warn { "Metric with name '$name' does not exist" }
            return false
        }
        
        metrics.remove(name)
        logger.info { "Unregistered metric: $name" }
        return true
    }
    
    /**
     * 获取指标。
     *
     * @param name 指标名称
     * @return 指标，如果不存在则返回 null
     */
    @Suppress("UNCHECKED_CAST")
    fun <I, O> get(name: String): Metric<I, O>? {
        return metrics[name] as? Metric<I, O>
    }
    
    /**
     * 获取所有指标。
     *
     * @return 所有指标的列表
     */
    fun getAll(): List<Metric<*, *>> {
        return metrics.values.toList()
    }
    
    /**
     * 获取指定类别的指标。
     *
     * @param category 指标类别
     * @return 指定类别的指标列表
     */
    fun getByCategory(category: MetricCategory): List<Metric<*, *>> {
        return metrics.values.filter { it.category == category }
    }
    
    /**
     * 清除所有指标。
     */
    fun clear() {
        metrics.clear()
        logger.info { "Cleared all metrics" }
    }
}

/**
 * 指标注册表扩展函数，用于注册指标。
 *
 * @param metric 指标
 * @return 是否成功注册
 */
fun <T : Metric<*, *>> T.register(): T {
    MetricRegistry.register(this)
    return this
}

/**
 * 指标注册表扩展函数，用于注销指标。
 *
 * @return 是否成功注销
 */
fun <T : Metric<*, *>> T.unregister(): Boolean {
    return MetricRegistry.unregister(this.name)
}
