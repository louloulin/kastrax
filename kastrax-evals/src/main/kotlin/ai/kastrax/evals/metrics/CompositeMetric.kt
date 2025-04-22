package ai.kastrax.evals.metrics

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val logger = KotlinLogging.logger {}

/**
 * 组合指标，用于组合多个指标。
 *
 * @param I 输入类型
 * @param O 输出类型
 * @property name 指标名称
 * @property description 指标描述
 * @property category 指标类别
 * @property metrics 组合的指标列表
 * @property weights 指标权重列表，与 metrics 一一对应
 */
class CompositeMetric<I, O>(
    override val name: String,
    override val description: String,
    override val category: MetricCategory,
    private val metrics: List<Metric<I, O>>,
    private val weights: List<Double>
) : Metric<I, O> {
    
    init {
        require(metrics.isNotEmpty()) { "Metrics list cannot be empty" }
        require(metrics.size == weights.size) { "Metrics and weights must have the same size" }
        require(weights.all { it >= 0 }) { "Weights must be non-negative" }
        require(weights.sum() > 0) { "Sum of weights must be positive" }
    }
    
    override suspend fun calculate(
        input: I,
        output: O,
        options: Map<String, Any?>
    ): MetricResult = coroutineScope {
        // 并行计算所有指标
        val results = metrics.mapIndexed { index, metric ->
            async {
                try {
                    val result = metric.calculate(input, output, options)
                    index to result
                } catch (e: Exception) {
                    logger.error(e) { "Error calculating metric: ${metric.name}" }
                    index to MetricResult(0.0, mapOf("error" to e.message))
                }
            }
        }.map { it.await() }.toMap()
        
        // 计算加权平均分数
        var weightedSum = 0.0
        var weightSum = 0.0
        
        for (i in metrics.indices) {
            val weight = weights[i]
            val result = results[i] ?: continue
            
            weightedSum += result.score * weight
            weightSum += weight
        }
        
        // 计算最终分数
        val finalScore = if (weightSum > 0) weightedSum / weightSum else 0.0
        
        // 构建详细信息
        val details = mutableMapOf<String, Any?>()
        
        for (i in metrics.indices) {
            val metric = metrics[i]
            val result = results[i]
            
            if (result != null) {
                details["${metric.name}_score"] = result.score
                details["${metric.name}_weight"] = weights[i]
                details["${metric.name}_details"] = result.details
            }
        }
        
        MetricResult(finalScore, details)
    }
}

/**
 * 创建组合指标。
 *
 * @param I 输入类型
 * @param O 输出类型
 * @param name 指标名称
 * @param description 指标描述
 * @param category 指标类别
 * @param metrics 组合的指标列表
 * @param weights 指标权重列表，与 metrics 一一对应，如果为 null，则使用均匀权重
 * @return 组合指标
 */
fun <I, O> compositeMetric(
    name: String,
    description: String,
    category: MetricCategory,
    metrics: List<Metric<I, O>>,
    weights: List<Double>? = null
): CompositeMetric<I, O> {
    val finalWeights = weights ?: List(metrics.size) { 1.0 }
    return CompositeMetric(name, description, category, metrics, finalWeights)
}

/**
 * 创建组合指标。
 *
 * @param I 输入类型
 * @param O 输出类型
 * @param name 指标名称
 * @param description 指标描述
 * @param category 指标类别
 * @param vararg metricsWithWeights 指标和权重的对列表
 * @return 组合指标
 */
fun <I, O> compositeMetric(
    name: String,
    description: String,
    category: MetricCategory,
    vararg metricsWithWeights: Pair<Metric<I, O>, Double>
): CompositeMetric<I, O> {
    val metrics = metricsWithWeights.map { it.first }
    val weights = metricsWithWeights.map { it.second }
    return CompositeMetric(name, description, category, metrics, weights)
}
