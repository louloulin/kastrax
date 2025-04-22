package ai.kastrax.evals.testsuite

import ai.kastrax.evals.metrics.AccuracyMethod
import ai.kastrax.evals.metrics.MetricCategory
import ai.kastrax.evals.metrics.accuracyMetric
import ai.kastrax.evals.metrics.metric
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestSuiteTest {
    
    @Test
    fun testBasicTestSuite() = runBlocking {
        // 创建测试指标
        val accuracyMetric = accuracyMetric(AccuracyMethod.EXACT_MATCH)
        
        // 创建测试用例
        val testCase1 = testCase<String, String> {
            name = "Test Case 1"
            input = "What is the capital of France?"
            expectedOutput = "Paris"
            metric(accuracyMetric)
            tag("geography")
        }
        
        val testCase2 = testCase<String, String> {
            name = "Test Case 2"
            input = "What is the capital of Germany?"
            expectedOutput = "Berlin"
            metric(accuracyMetric)
            tag("geography")
        }
        
        val testCase3 = testCase<String, String> {
            name = "Test Case 3"
            input = "What is the capital of Italy?"
            expectedOutput = "Rome"
            metric(accuracyMetric)
            tag("geography")
        }
        
        // 创建测试套件
        val testSuite = BasicTestSuite<String, String>(
            name = "Geography Test Suite",
            description = "A test suite for geography questions",
            tags = setOf("geography", "capitals")
        )
        
        // 添加测试用例
        testSuite.addTestCase(testCase1)
        testSuite.addTestCases(listOf(testCase2, testCase3))
        
        // 验证测试套件属性
        assertEquals("Geography Test Suite", testSuite.name)
        assertEquals("A test suite for geography questions", testSuite.description)
        assertEquals(setOf("geography", "capitals"), testSuite.tags)
        assertEquals(3, testSuite.testCases.size)
        
        // 验证按标签获取测试用例
        val geographyTestCases = testSuite.getTestCasesByTag("geography")
        assertEquals(3, geographyTestCases.size)
        
        // 创建输出提供者
        val outputProvider = object : OutputProvider<String, String> {
            override suspend fun getOutput(input: String): String {
                return when (input) {
                    "What is the capital of France?" -> "Paris"
                    "What is the capital of Germany?" -> "Berlin"
                    "What is the capital of Italy?" -> "Milan" // 错误答案
                    else -> "Unknown"
                }
            }
        }
        
        // 执行测试套件
        val result = testSuite.execute(outputProvider)
        
        // 验证结果
        assertEquals(testSuite.id, result.testSuiteId)
        assertEquals(testSuite.name, result.testSuiteName)
        assertEquals(3, result.testCaseResults.size)
        assertEquals(2, result.passedCount)
        assertEquals(1, result.failedCount)
        assertEquals(2.0 / 3.0, result.passRate)
    }
    
    @Test
    fun testTestSuiteBuilder() = runBlocking {
        // 创建测试指标
        val metric1 = metric<String, String> {
            name = "Metric1"
            description = "Metric 1"
            category = MetricCategory.ACCURACY
            calculate { input, output, options ->
                ai.kastrax.evals.metrics.MetricResult(
                    if (input.contains("France") && output == "Paris") 1.0 else 0.0
                )
            }
        }
        
        // 使用构建器创建测试套件
        val testSuite = testSuite<String, String> {
            name = "Geography Test Suite"
            description = "A test suite for geography questions"
            tag("geography")
            tag("capitals")
            
            // 添加测试用例
            testCase {
                name = "Test Case 1"
                input = "What is the capital of France?"
                expectedOutput = "Paris"
                metric(metric1)
                tag("europe")
            }
            
            testCase {
                name = "Test Case 2"
                input = "What is the capital of Germany?"
                expectedOutput = "Berlin"
                metric(metric1)
                tag("europe")
            }
        }
        
        // 验证测试套件属性
        assertEquals("Geography Test Suite", testSuite.name)
        assertEquals("A test suite for geography questions", testSuite.description)
        assertEquals(setOf("geography", "capitals"), testSuite.tags)
        assertEquals(2, testSuite.testCases.size)
        
        // 验证按标签获取测试用例
        val europeTestCases = testSuite.getTestCasesByTag("europe")
        assertEquals(2, europeTestCases.size)
        
        // 创建输出提供者
        val outputProvider = object : OutputProvider<String, String> {
            override suspend fun getOutput(input: String): String {
                return when (input) {
                    "What is the capital of France?" -> "Paris"
                    "What is the capital of Germany?" -> "Munich" // 错误答案
                    else -> "Unknown"
                }
            }
        }
        
        // 执行测试套件
        val result = testSuite.execute(outputProvider)
        
        // 验证结果
        assertEquals(testSuite.id, result.testSuiteId)
        assertEquals(testSuite.name, result.testSuiteName)
        assertEquals(2, result.testCaseResults.size)
        assertEquals(1, result.passedCount)
        assertEquals(1, result.failedCount)
        assertEquals(0.5, result.passRate)
    }
    
    @Test
    fun testParallelExecution() = runBlocking {
        // 创建测试指标
        val accuracyMetric = accuracyMetric(AccuracyMethod.EXACT_MATCH)
        
        // 创建测试用例
        val testCases = List(10) { index ->
            testCase<String, String> {
                name = "Test Case ${index + 1}"
                input = "Question $index"
                expectedOutput = "Answer $index"
                metric(accuracyMetric)
            }
        }
        
        // 创建测试套件
        val testSuite = testSuite<String, String> {
            name = "Parallel Test Suite"
            description = "A test suite for testing parallel execution"
            
            // 添加测试用例
            testCases.forEach { testCase(it) }
        }
        
        // 创建输出提供者
        val outputProvider = object : OutputProvider<String, String> {
            override suspend fun getOutput(input: String): String {
                // 模拟耗时操作
                Thread.sleep(100)
                
                val index = input.substringAfter("Question ").toInt()
                return if (index % 2 == 0) {
                    "Answer $index" // 正确答案
                } else {
                    "Wrong Answer" // 错误答案
                }
            }
        }
        
        // 串行执行测试套件
        val startTimeSerial = System.currentTimeMillis()
        val resultSerial = testSuite.execute(outputProvider, parallel = false)
        val endTimeSerial = System.currentTimeMillis()
        val durationSerial = endTimeSerial - startTimeSerial
        
        // 并行执行测试套件
        val startTimeParallel = System.currentTimeMillis()
        val resultParallel = testSuite.execute(outputProvider, parallel = true)
        val endTimeParallel = System.currentTimeMillis()
        val durationParallel = endTimeParallel - startTimeParallel
        
        // 验证结果
        assertEquals(10, resultSerial.testCaseResults.size)
        assertEquals(10, resultParallel.testCaseResults.size)
        assertEquals(5, resultSerial.passedCount)
        assertEquals(5, resultParallel.passedCount)
        assertEquals(5, resultSerial.failedCount)
        assertEquals(5, resultParallel.failedCount)
        assertEquals(0.5, resultSerial.passRate)
        assertEquals(0.5, resultParallel.passRate)
        
        // 验证并行执行比串行执行快
        assertTrue(durationParallel < durationSerial, "Parallel execution should be faster than serial execution")
    }
}
