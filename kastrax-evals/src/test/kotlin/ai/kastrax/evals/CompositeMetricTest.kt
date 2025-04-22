package ai.kastrax.evals

import ai.kastrax.evals.metrics.MetricCategory
import ai.kastrax.evals.metrics.MetricResult
import ai.kastrax.evals.metrics.compositeMetric
import ai.kastrax.evals.metrics.metric
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CompositeMetricTest {

    @Test
    fun testCompositeMetric() = runBlocking {
        // 创建测试指标
        val metric1 = metric<String, String> {
            name = "TestMetric1"
            description = "Test metric 1"
            category = MetricCategory.ACCURACY
            calculate { input, output, options ->
                MetricResult(0.8, mapOf("detail1" to "value1"))
            }
        }

        val metric2 = metric<String, String> {
            name = "TestMetric2"
            description = "Test metric 2"
            category = MetricCategory.RELEVANCE
            calculate { input, output, options ->
                MetricResult(0.6, mapOf("detail2" to "value2"))
            }
        }

        val metric3 = metric<String, String> {
            name = "TestMetric3"
            description = "Test metric 3"
            category = MetricCategory.QUALITY
            calculate { input, output, options ->
                MetricResult(0.4, mapOf("detail3" to "value3"))
            }
        }

        // 创建组合指标，使用均匀权重
        val compositeMetric1 = compositeMetric(
            name = "CompositeMetric1",
            description = "Composite metric with uniform weights",
            category = MetricCategory.CUSTOM,
            metrics = listOf(metric1, metric2, metric3)
        )

        val result1 = compositeMetric1.calculate(
            input = "input",
            output = "output",
            options = emptyMap()
        )

        // 验证均匀权重的结果
        assertEquals((0.8 + 0.6 + 0.4) / 3, result1.score)

        // 创建组合指标，使用自定义权重
        val compositeMetric2 = compositeMetric(
            name = "CompositeMetric2",
            description = "Composite metric with custom weights",
            category = MetricCategory.CUSTOM,
            metrics = listOf(metric1, metric2, metric3),
            weights = listOf(0.5, 0.3, 0.2)
        )

        val result2 = compositeMetric2.calculate(
            input = "input",
            output = "output",
            options = emptyMap()
        )

        // 验证自定义权重的结果
        assertEquals(0.8 * 0.5 + 0.6 * 0.3 + 0.4 * 0.2, result2.score)

        // 创建组合指标，使用 Pair 构造
        val compositeMetric3 = compositeMetric(
            name = "CompositeMetric3",
            description = "Composite metric with pairs",
            category = MetricCategory.CUSTOM,
            metric1 to 0.5,
            metric2 to 0.3,
            metric3 to 0.2
        )

        val result3 = compositeMetric3.calculate(
            input = "input",
            output = "output",
            options = emptyMap()
        )

        // 验证 Pair 构造的结果
        assertEquals(0.8 * 0.5 + 0.6 * 0.3 + 0.4 * 0.2, result3.score)
    }

    @Test
    fun testCompositeMetricWithErrors() = runBlocking {
        // 创建测试指标
        val metric1 = metric<String, String> {
            name = "TestMetric1"
            description = "Test metric 1"
            category = MetricCategory.ACCURACY
            calculate { input, output, options ->
                MetricResult(0.8, mapOf("detail1" to "value1"))
            }
        }

        val metric2 = metric<String, String> {
            name = "TestMetric2"
            description = "Test metric 2"
            category = MetricCategory.RELEVANCE
            calculate { input, output, options ->
                throw RuntimeException("Test error")
            }
        }

        val metric3 = metric<String, String> {
            name = "TestMetric3"
            description = "Test metric 3"
            category = MetricCategory.QUALITY
            calculate { input, output, options ->
                MetricResult(0.4, mapOf("detail3" to "value3"))
            }
        }

        // 创建组合指标
        val compositeMetric = compositeMetric(
            name = "CompositeMetric",
            description = "Composite metric with errors",
            category = MetricCategory.CUSTOM,
            metrics = listOf(metric1, metric2, metric3),
            weights = listOf(0.5, 0.3, 0.2)
        )

        val result = compositeMetric.calculate(
            input = "input",
            output = "output",
            options = emptyMap()
        )

        // 验证结果，忽略出错的指标
        assertEquals((0.8 * 0.5 + 0.0 * 0.3 + 0.4 * 0.2) / (0.5 + 0.3 + 0.2), result.score)
    }
}
