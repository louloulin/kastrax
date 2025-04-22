package ai.kastrax.evals.testsuite

import ai.kastrax.evals.metrics.AccuracyMethod
import ai.kastrax.evals.metrics.MetricCategory
import ai.kastrax.evals.metrics.accuracyMetric
import ai.kastrax.evals.metrics.metric
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestCaseTest {
    
    @Test
    fun testBasicTestCase() = runBlocking {
        // 创建测试指标
        val accuracyMetric = accuracyMetric(AccuracyMethod.EXACT_MATCH)
        
        // 创建测试用例
        val testCase = BasicTestCase(
            name = "Test Case 1",
            description = "A test case",
            tags = setOf("test", "example"),
            input = "What is the capital of France?",
            expectedOutput = "Paris",
            metrics = listOf(accuracyMetric),
            threshold = 0.7
        )
        
        // 验证测试用例属性
        assertEquals("Test Case 1", testCase.name)
        assertEquals("A test case", testCase.description)
        assertEquals(setOf("test", "example"), testCase.tags)
        assertEquals("What is the capital of France?", testCase.input)
        assertEquals("Paris", testCase.expectedOutput)
        assertEquals(1, testCase.metrics.size)
        assertEquals(0.7, testCase.threshold)
        
        // 执行测试用例 - 正确答案
        val result1 = testCase.execute("Paris")
        
        // 验证结果
        assertEquals(testCase.id, result1.testCaseId)
        assertEquals(testCase.name, result1.testCaseName)
        assertEquals(testCase.input, result1.input)
        assertEquals(testCase.expectedOutput, result1.expectedOutput)
        assertEquals("Paris", result1.actualOutput)
        assertEquals(1, result1.metricResults.size)
        assertEquals(1.0, result1.metricResults[0].score)
        assertTrue(result1.passed)
        
        // 执行测试用例 - 错误答案
        val result2 = testCase.execute("London")
        
        // 验证结果
        assertEquals(testCase.id, result2.testCaseId)
        assertEquals(testCase.name, result2.testCaseName)
        assertEquals(testCase.input, result2.input)
        assertEquals(testCase.expectedOutput, result2.expectedOutput)
        assertEquals("London", result2.actualOutput)
        assertEquals(1, result2.metricResults.size)
        assertEquals(0.0, result2.metricResults[0].score)
        assertFalse(result2.passed)
    }
    
    @Test
    fun testTestCaseBuilder() = runBlocking {
        // 创建测试指标
        val metric1 = metric<String, String> {
            name = "Metric1"
            description = "Metric 1"
            category = MetricCategory.ACCURACY
            calculate { input, output, options ->
                ai.kastrax.evals.metrics.MetricResult(0.8)
            }
        }
        
        val metric2 = metric<String, String> {
            name = "Metric2"
            description = "Metric 2"
            category = MetricCategory.RELEVANCE
            calculate { input, output, options ->
                ai.kastrax.evals.metrics.MetricResult(0.6)
            }
        }
        
        // 使用构建器创建测试用例
        val testCase = testCase<String, String> {
            name = "Test Case 1"
            description = "A test case"
            tag("test")
            tag("example")
            input = "What is the capital of France?"
            expectedOutput = "Paris"
            metric(metric1)
            metric(metric2)
            option("key1", "value1")
            option("key2", 123)
            threshold = 0.8
        }
        
        // 验证测试用例属性
        assertEquals("Test Case 1", testCase.name)
        assertEquals("A test case", testCase.description)
        assertEquals(setOf("test", "example"), testCase.tags)
        assertEquals("What is the capital of France?", testCase.input)
        assertEquals("Paris", testCase.expectedOutput)
        assertEquals(2, testCase.metrics.size)
        assertEquals(mapOf("key1" to "value1", "key2" to 123), testCase.options)
        
        // 执行测试用例
        val result = testCase.execute("Paris")
        
        // 验证结果
        assertEquals(testCase.id, result.testCaseId)
        assertEquals(testCase.name, result.testCaseName)
        assertEquals(testCase.input, result.input)
        assertEquals(testCase.expectedOutput, result.expectedOutput)
        assertEquals("Paris", result.actualOutput)
        assertEquals(2, result.metricResults.size)
        assertEquals(0.8, result.metricResults[0].score)
        assertEquals(0.6, result.metricResults[1].score)
        
        // 验证通过/失败
        // 平均分数为 (0.8 + 0.6) / 2 = 0.7，小于阈值 0.8，所以应该失败
        assertFalse(result.passed)
    }
}
