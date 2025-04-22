package ai.kastrax.evals

import ai.kastrax.evals.metrics.ResponseTimeMethod
import ai.kastrax.evals.metrics.responseTimeMetric
import ai.kastrax.evals.metrics.MetricResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResponseTimeMetricTest {

    @Test
    fun testThreshold() = runBlocking {
        val metric = responseTimeMetric<String, String>(
            ResponseTimeMethod.THRESHOLD,
            targetTimeMs = 1000,
            maxTimeMs = 5000
        )

        // 响应时间小于目标时间
        val result1 = metric.calculate(
            input = "input",
            output = "output",
            options = mapOf("responseTimeMs" to 500L)
        )
        assertEquals(1.0, result1.score)

        // 响应时间等于目标时间
        val result2 = metric.calculate(
            input = "input",
            output = "output",
            options = mapOf("responseTimeMs" to 1000L)
        )
        assertEquals(1.0, result2.score)

        // 响应时间大于目标时间
        val result3 = metric.calculate(
            input = "input",
            output = "output",
            options = mapOf("responseTimeMs" to 2000L)
        )
        assertEquals(0.0, result3.score)
    }

    @Test
    fun testLinear() = runBlocking {
        val metric = responseTimeMetric<String, String>(
            ResponseTimeMethod.LINEAR,
            targetTimeMs = 1000,
            maxTimeMs = 5000
        )

        // 响应时间小于目标时间
        val result1 = metric.calculate(
            input = "input",
            output = "output",
            options = mapOf("responseTimeMs" to 500L)
        )
        assertEquals(1.0, result1.score)

        // 响应时间等于目标时间
        val result2 = metric.calculate(
            input = "input",
            output = "output",
            options = mapOf("responseTimeMs" to 1000L)
        )
        assertEquals(1.0, result2.score)

        // 响应时间在目标时间和最大时间之间
        val result3 = metric.calculate(
            input = "input",
            output = "output",
            options = mapOf("responseTimeMs" to 3000L)
        )
        assertEquals(0.5, result3.score)

        // 响应时间等于最大时间
        val result4 = metric.calculate(
            input = "input",
            output = "output",
            options = mapOf("responseTimeMs" to 5000L)
        )
        assertEquals(0.0, result4.score)

        // 响应时间大于最大时间
        val result5 = metric.calculate(
            input = "input",
            output = "output",
            options = mapOf("responseTimeMs" to 6000L)
        )
        assertEquals(0.0, result5.score)
    }

    @Test
    fun testExponential() = runBlocking {
        val metric = responseTimeMetric<String, String>(
            ResponseTimeMethod.EXPONENTIAL,
            targetTimeMs = 1000,
            maxTimeMs = 5000
        )

        // 响应时间小于目标时间
        val result1 = metric.calculate(
            input = "input",
            output = "output",
            options = mapOf("responseTimeMs" to 500L)
        )
        assertEquals(1.0, result1.score)

        // 响应时间等于目标时间
        val result2 = metric.calculate(
            input = "input",
            output = "output",
            options = mapOf("responseTimeMs" to 1000L)
        )
        assertEquals(1.0, result2.score)

        // 响应时间在目标时间和最大时间之间
        val result3 = metric.calculate(
            input = "input",
            output = "output",
            options = mapOf("responseTimeMs" to 3000L)
        )
        assertTrue(result3.score > 0.0 && result3.score < 1.0)

        // 响应时间接近最大时间
        val result4 = metric.calculate(
            input = "input",
            output = "output",
            options = mapOf("responseTimeMs" to 4900L)
        )
        assertTrue(result4.score > 0.0 && result4.score < 0.1)
    }
}
