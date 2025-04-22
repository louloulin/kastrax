package ai.kastrax.evals.metrics

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.math.exp

private val logger = KotlinLogging.logger {}

/**
 * 响应时间指标，用于评估系统的响应时间。
 *
 * 支持多种响应时间评估方法：
 * - 阈值评估：根据响应时间是否超过阈值进行评估
 * - 线性评估：根据响应时间与目标时间的线性关系进行评估
 * - 指数评估：根据响应时间与目标时间的指数关系进行评估
 *
 * @param method 响应时间评估方法
 * @param targetTimeMs 目标响应时间（毫秒）
 * @param maxTimeMs 最大可接受的响应时间（毫秒）
 */
class ResponseTimeMetric<I, O>(
    private val method: ResponseTimeMethod = ResponseTimeMethod.THRESHOLD,
    private val targetTimeMs: Long = 1000,
    private val maxTimeMs: Long = 5000
) : Metric<I, O> {

    override val name: String = "ResponseTime"
    override val description: String = "Evaluates the response time of the system"
    override val category: MetricCategory = MetricCategory.PERFORMANCE

    override suspend fun calculate(
        input: I,
        output: O,
        options: Map<String, Any?>
    ): MetricResult {
        // 从选项中获取响应时间
        val responseTimeMs = options["responseTimeMs"] as? Long
            ?: throw IllegalArgumentException("Response time must be provided for response time evaluation")

        // 根据方法计算响应时间分数
        val score = when (method) {
            ResponseTimeMethod.THRESHOLD -> calculateThresholdScore(responseTimeMs)
            ResponseTimeMethod.LINEAR -> calculateLinearScore(responseTimeMs)
            ResponseTimeMethod.EXPONENTIAL -> calculateExponentialScore(responseTimeMs)
        }

        return MetricResult(
            score = score,
            details = mapOf(
                "method" to method.name,
                "targetTimeMs" to targetTimeMs,
                "maxTimeMs" to maxTimeMs,
                "responseTimeMs" to responseTimeMs
            )
        )
    }

    /**
     * 计算阈值评估的响应时间分数。
     *
     * @param responseTimeMs 响应时间（毫秒）
     * @return 响应时间分数，范围为 0.0 到 1.0
     */
    private fun calculateThresholdScore(responseTimeMs: Long): Double {
        return if (responseTimeMs <= targetTimeMs) 1.0 else 0.0
    }

    /**
     * 计算线性评估的响应时间分数。
     *
     * @param responseTimeMs 响应时间（毫秒）
     * @return 响应时间分数，范围为 0.0 到 1.0
     */
    private fun calculateLinearScore(responseTimeMs: Long): Double {
        // 如果响应时间小于等于目标时间，则返回 1.0
        if (responseTimeMs <= targetTimeMs) {
            return 1.0
        }

        // 如果响应时间大于最大时间，则返回 0.0
        if (responseTimeMs >= maxTimeMs) {
            return 0.0
        }

        // 计算线性分数
        return 1.0 - (responseTimeMs - targetTimeMs).toDouble() / (maxTimeMs - targetTimeMs)
    }

    /**
     * 计算指数评估的响应时间分数。
     *
     * @param responseTimeMs 响应时间（毫秒）
     * @return 响应时间分数，范围为 0.0 到 1.0
     */
    private fun calculateExponentialScore(responseTimeMs: Long): Double {
        // 如果响应时间小于等于目标时间，则返回 1.0
        if (responseTimeMs <= targetTimeMs) {
            return 1.0
        }

        // 计算指数分数
        val lambda = -Math.log(0.01) / (maxTimeMs - targetTimeMs).toDouble()
        return exp(-lambda * (responseTimeMs - targetTimeMs))
    }
}

/**
 * 响应时间评估方法。
 */
enum class ResponseTimeMethod {
    /**
     * 阈值评估，根据响应时间是否超过阈值进行评估。
     */
    THRESHOLD,

    /**
     * 线性评估，根据响应时间与目标时间的线性关系进行评估。
     */
    LINEAR,

    /**
     * 指数评估，根据响应时间与目标时间的指数关系进行评估。
     */
    EXPONENTIAL
}

/**
 * 创建响应时间指标。
 *
 * @param method 响应时间评估方法
 * @param targetTimeMs 目标响应时间（毫秒）
 * @param maxTimeMs 最大可接受的响应时间（毫秒）
 * @return 响应时间指标
 */
fun <I, O> responseTimeMetric(
    method: ResponseTimeMethod = ResponseTimeMethod.THRESHOLD,
    targetTimeMs: Long = 1000,
    maxTimeMs: Long = 5000
): ResponseTimeMetric<I, O> {
    return ResponseTimeMetric(method, targetTimeMs, maxTimeMs)
}
