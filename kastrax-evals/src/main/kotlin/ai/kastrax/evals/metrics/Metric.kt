package ai.kastrax.evals.metrics

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * 评估指标接口，用于定义评估指标。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
interface Metric<I, O> {
    /**
     * 指标的名称。
     */
    val name: String

    /**
     * 指标的描述。
     */
    val description: String

    /**
     * 指标的类别。
     */
    val category: MetricCategory

    /**
     * 计算指标值。
     *
     * @param input 输入数据
     * @param output AI 系统的输出
     * @param options 计算选项
     * @return 指标结果
     */
    suspend fun calculate(
        input: I,
        output: O,
        options: Map<String, Any?> = emptyMap()
    ): MetricResult
}

/**
 * 指标类别，用于对指标进行分类。
 */
enum class MetricCategory {
    /**
     * 准确性指标，用于评估输出的准确性。
     */
    ACCURACY,

    /**
     * 相关性指标，用于评估输出与输入的相关程度。
     */
    RELEVANCE,

    /**
     * 性能指标，用于评估系统的性能。
     */
    PERFORMANCE,

    /**
     * 质量指标，用于评估输出的质量。
     */
    QUALITY,

    /**
     * 安全性指标，用于评估输出的安全性。
     */
    SAFETY,

    /**
     * 自定义指标，用于自定义评估。
     */
    CUSTOM
}

/**
 * 指标结果，表示指标计算的结果。
 *
 * @property score 指标分数，范围为 0.0 到 1.0
 * @property details 指标详情，包含指标计算的详细信息
 */
data class MetricResult(
    val score: Double,
    val details: Map<String, Any?> = emptyMap()
)

/**
 * 指标运行结果，表示指标运行的结果。
 *
 * @param I 输入类型
 * @param O 输出类型
 * @property metricName 指标名称
 * @property metricCategory 指标类别
 * @property input 输入数据
 * @property output AI 系统的输出
 * @property result 指标结果
 * @property startTime 开始时间
 * @property endTime 结束时间
 * @property durationMs 运行时间（毫秒）
 */
data class MetricRunResult<I, O>(
    val metricName: String,
    val metricCategory: MetricCategory,
    val input: I,
    val output: O,
    val result: MetricResult,
    val startTime: Instant,
    val endTime: Instant,
    val durationMs: Long
)

/**
 * 指标构建器，用于创建指标。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
class MetricBuilder<I, O> {
    var name: String = ""
    var description: String = ""
    var category: MetricCategory = MetricCategory.CUSTOM
    private var calculateFunction: (suspend (I, O, Map<String, Any?>) -> MetricResult)? = null

    /**
     * 设置计算函数。
     *
     * @param block 计算函数
     */
    fun calculate(block: suspend (I, O, Map<String, Any?>) -> MetricResult) {
        calculateFunction = block
    }

    /**
     * 构建指标。
     *
     * @return 指标
     */
    fun build(): Metric<I, O> {
        require(name.isNotBlank()) { "Metric name cannot be blank" }
        require(calculateFunction != null) { "Calculate function must be provided" }

        return object : Metric<I, O> {
            override val name: String = this@MetricBuilder.name
            override val description: String = this@MetricBuilder.description
            override val category: MetricCategory = this@MetricBuilder.category

            override suspend fun calculate(
                input: I,
                output: O,
                options: Map<String, Any?>
            ): MetricResult {
                return calculateFunction!!.invoke(input, output, options)
            }
        }
    }
}

/**
 * 创建指标。
 *
 * @param I 输入类型
 * @param O 输出类型
 * @param block 构建器配置块
 * @return 指标
 */
fun <I, O> metric(block: MetricBuilder<I, O>.() -> Unit): Metric<I, O> {
    val builder = MetricBuilder<I, O>()
    builder.block()
    return builder.build()
}

/**
 * 指标运行器，用于运行指标。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
class MetricRunner<I, O> {
    /**
     * 运行指标。
     *
     * @param metric 指标
     * @param input 输入数据
     * @param output AI 系统的输出
     * @param options 计算选项
     * @return 指标运行结果
     */
    suspend fun run(
        metric: Metric<I, O>,
        input: I,
        output: O,
        options: Map<String, Any?> = emptyMap()
    ): MetricRunResult<I, O> {
        logger.info { "Running metric: ${metric.name}" }
        
        val startTime = Instant.now()
        
        val result = try {
            metric.calculate(input, output, options)
        } catch (e: Exception) {
            logger.error(e) { "Error running metric: ${metric.name}" }
            MetricResult(0.0, mapOf("error" to e.message))
        }
        
        val endTime = Instant.now()
        val durationMs = endTime.toEpochMilli() - startTime.toEpochMilli()
        
        return MetricRunResult(
            metricName = metric.name,
            metricCategory = metric.category,
            input = input,
            output = output,
            result = result,
            startTime = startTime,
            endTime = endTime,
            durationMs = durationMs
        )
    }
    
    /**
     * 运行多个指标。
     *
     * @param metrics 指标列表
     * @param input 输入数据
     * @param output AI 系统的输出
     * @param options 计算选项
     * @return 指标运行结果列表
     */
    suspend fun runAll(
        metrics: List<Metric<I, O>>,
        input: I,
        output: O,
        options: Map<String, Any?> = emptyMap()
    ): List<MetricRunResult<I, O>> {
        return metrics.map { metric ->
            run(metric, input, output, options)
        }
    }
}
