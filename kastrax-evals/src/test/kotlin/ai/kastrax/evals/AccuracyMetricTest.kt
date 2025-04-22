package ai.kastrax.evals

import ai.kastrax.evals.metrics.AccuracyMethod
import ai.kastrax.evals.metrics.accuracyMetric
import ai.kastrax.evals.metrics.MetricResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AccuracyMetricTest {

    @Test
    fun testExactMatch() = runBlocking {
        val metric = accuracyMetric(AccuracyMethod.EXACT_MATCH)

        // 完全匹配
        val result1 = metric.calculate(
            input = "What is the capital of France?",
            output = "Paris",
            options = mapOf("expected" to "Paris")
        )
        assertEquals(1.0, result1.score)

        // 不匹配
        val result2 = metric.calculate(
            input = "What is the capital of France?",
            output = "London",
            options = mapOf("expected" to "Paris")
        )
        assertEquals(0.0, result2.score)

        // 大小写不同但忽略大小写
        val result3 = metric.calculate(
            input = "What is the capital of France?",
            output = "paris",
            options = mapOf("expected" to "Paris")
        )
        assertEquals(1.0, result3.score)

        // 空格不同但忽略空格
        val result4 = metric.calculate(
            input = "What is the capital of France?",
            output = "  Paris  ",
            options = mapOf("expected" to "Paris")
        )
        assertEquals(1.0, result4.score)
    }

    @Test
    fun testPartialMatch() = runBlocking {
        val metric = accuracyMetric(AccuracyMethod.PARTIAL_MATCH)

        // 完全匹配
        val result1 = metric.calculate(
            input = "What is the capital of France?",
            output = "Paris",
            options = mapOf("expected" to "Paris")
        )
        assertEquals(1.0, result1.score)

        // 部分匹配
        val result2 = metric.calculate(
            input = "What is the capital of France?",
            output = "The capital of France is Paris.",
            options = mapOf("expected" to "Paris")
        )
        assertEquals(1.0, result2.score)

        // 部分匹配（期望输出是输出的一部分）
        val result3 = metric.calculate(
            input = "What is the capital of France?",
            output = "Par",
            options = mapOf("expected" to "Paris")
        )
        assertEquals(0.6, result3.score, 0.1)
    }

    @Test
    fun testSimilarity() = runBlocking {
        val metric = accuracyMetric(AccuracyMethod.SIMILARITY, threshold = 0.5)

        // 完全匹配
        val result1 = metric.calculate(
            input = "What is the capital of France?",
            output = "Paris is the capital of France",
            options = mapOf("expected" to "Paris is the capital of France")
        )
        assertEquals(1.0, result1.score)

        // 高相似度
        val result2 = metric.calculate(
            input = "What is the capital of France?",
            output = "Paris is the capital city of France",
            options = mapOf("expected" to "Paris is the capital of France")
        )
        assertEquals(1.0, result2.score)

        // 低相似度
        val result3 = metric.calculate(
            input = "What is the capital of France?",
            output = "France is a country in Europe",
            options = mapOf("expected" to "Paris is the capital of France")
        )
        assert(result3.score < 0.5)
    }
}
