package ai.kastrax.evals.benchmark

import ai.kastrax.evals.metrics.AccuracyMethod
import ai.kastrax.evals.metrics.accuracyMetric
import ai.kastrax.evals.testsuite.OutputProvider
import ai.kastrax.evals.testsuite.testCase
import ai.kastrax.evals.testsuite.testSuite
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BenchmarkTest {
    
    @Test
    fun testBasicBenchmark() = runBlocking {
        // 创建测试指标
        val accuracyMetric = accuracyMetric(AccuracyMethod.EXACT_MATCH)
        
        // 创建测试套件
        val testSuite1 = testSuite<String, String> {
            name = "Geography Test Suite"
            description = "A test suite for geography questions"
            tag("geography")
            
            // 添加测试用例
            testCase {
                name = "Test Case 1"
                input = "What is the capital of France?"
                expectedOutput = "Paris"
                metric(accuracyMetric)
            }
            
            testCase {
                name = "Test Case 2"
                input = "What is the capital of Germany?"
                expectedOutput = "Berlin"
                metric(accuracyMetric)
            }
        }
        
        val testSuite2 = testSuite<String, String> {
            name = "Math Test Suite"
            description = "A test suite for math questions"
            tag("math")
            
            // 添加测试用例
            testCase {
                name = "Test Case 1"
                input = "What is 2 + 2?"
                expectedOutput = "4"
                metric(accuracyMetric)
            }
            
            testCase {
                name = "Test Case 2"
                input = "What is 3 * 3?"
                expectedOutput = "9"
                metric(accuracyMetric)
            }
        }
        
        // 创建基准测试
        val benchmark = BasicBenchmark(
            name = "General Knowledge Benchmark",
            description = "A benchmark for general knowledge questions",
            testSuites = listOf(testSuite1, testSuite2)
        )
        
        // 验证基准测试属性
        assertEquals("General Knowledge Benchmark", benchmark.name)
        assertEquals("A benchmark for general knowledge questions", benchmark.description)
        assertEquals(2, benchmark.testSuites.size)
        
        // 创建输出提供者
        val system1 = object : OutputProvider<String, String> {
            override suspend fun getOutput(input: String): String {
                return when (input) {
                    "What is the capital of France?" -> "Paris"
                    "What is the capital of Germany?" -> "Berlin"
                    "What is 2 + 2?" -> "4"
                    "What is 3 * 3?" -> "9"
                    else -> "Unknown"
                }
            }
        }
        
        val system2 = object : OutputProvider<String, String> {
            override suspend fun getOutput(input: String): String {
                return when (input) {
                    "What is the capital of France?" -> "Paris"
                    "What is the capital of Germany?" -> "Munich" // 错误答案
                    "What is 2 + 2?" -> "4"
                    "What is 3 * 3?" -> "6" // 错误答案
                    else -> "Unknown"
                }
            }
        }
        
        // 运行基准测试
        val result = benchmark.run(
            mapOf(
                "System 1" to system1,
                "System 2" to system2
            )
        )
        
        // 验证结果
        assertEquals(benchmark.id, result.benchmarkId)
        assertEquals(benchmark.name, result.benchmarkName)
        assertEquals(2, result.systemResults.size)
        
        // 验证系统 1 的结果
        val system1Result = result.systemResults["System 1"]!!
        assertEquals(2, system1Result.testSuiteResults.size)
        assertEquals(4, system1Result.passedCount)
        assertEquals(0, system1Result.failedCount)
        assertEquals(1.0, system1Result.passRate)
        
        // 验证系统 2 的结果
        val system2Result = result.systemResults["System 2"]!!
        assertEquals(2, system2Result.testSuiteResults.size)
        assertEquals(2, system2Result.passedCount)
        assertEquals(2, system2Result.failedCount)
        assertEquals(0.5, system2Result.passRate)
    }
    
    @Test
    fun testBenchmarkBuilder() = runBlocking {
        // 创建测试指标
        val accuracyMetric = accuracyMetric(AccuracyMethod.EXACT_MATCH)
        
        // 创建测试套件
        val testSuite1 = testSuite<String, String> {
            name = "Geography Test Suite"
            description = "A test suite for geography questions"
            tag("geography")
            
            // 添加测试用例
            testCase {
                name = "Test Case 1"
                input = "What is the capital of France?"
                expectedOutput = "Paris"
                metric(accuracyMetric)
            }
        }
        
        val testSuite2 = testSuite<String, String> {
            name = "Math Test Suite"
            description = "A test suite for math questions"
            tag("math")
            
            // 添加测试用例
            testCase {
                name = "Test Case 1"
                input = "What is 2 + 2?"
                expectedOutput = "4"
                metric(accuracyMetric)
            }
        }
        
        // 使用构建器创建基准测试
        val benchmark = benchmark<String, String> {
            name = "General Knowledge Benchmark"
            description = "A benchmark for general knowledge questions"
            testSuite(testSuite1)
            testSuite(testSuite2)
        }
        
        // 验证基准测试属性
        assertEquals("General Knowledge Benchmark", benchmark.name)
        assertEquals("A benchmark for general knowledge questions", benchmark.description)
        assertEquals(2, benchmark.testSuites.size)
    }
    
    @Test
    fun testBenchmarkComparator() = runBlocking {
        // 创建测试指标
        val accuracyMetric = accuracyMetric(AccuracyMethod.EXACT_MATCH)
        
        // 创建测试套件
        val testSuite = testSuite<String, String> {
            name = "Test Suite"
            
            // 添加测试用例
            testCase {
                name = "Test Case 1"
                input = "Question 1"
                expectedOutput = "Answer 1"
                metric(accuracyMetric)
            }
            
            testCase {
                name = "Test Case 2"
                input = "Question 2"
                expectedOutput = "Answer 2"
                metric(accuracyMetric)
            }
        }
        
        // 创建基准测试
        val benchmark = benchmark<String, String> {
            name = "Benchmark"
            testSuite(testSuite)
        }
        
        // 创建输出提供者
        val system1 = object : OutputProvider<String, String> {
            override suspend fun getOutput(input: String): String {
                return when (input) {
                    "Question 1" -> "Answer 1"
                    "Question 2" -> "Answer 2"
                    else -> "Unknown"
                }
            }
        }
        
        val system2 = object : OutputProvider<String, String> {
            override suspend fun getOutput(input: String): String {
                return when (input) {
                    "Question 1" -> "Answer 1"
                    "Question 2" -> "Wrong Answer"
                    else -> "Unknown"
                }
            }
        }
        
        val system3 = object : OutputProvider<String, String> {
            override suspend fun getOutput(input: String): String {
                return "Wrong Answer"
            }
        }
        
        // 运行基准测试
        val result = benchmark.run(
            mapOf(
                "System 1" to system1,
                "System 2" to system2,
                "System 3" to system3
            )
        )
        
        // 创建比较器
        val comparator = BenchmarkComparator<String, String>()
        
        // 比较结果
        val comparisonResult = comparator.compare(result)
        
        // 验证比较结果
        assertEquals(benchmark.id, comparisonResult.benchmarkId)
        assertEquals(benchmark.name, comparisonResult.benchmarkName)
        assertEquals(3, comparisonResult.systemNames.size)
        assertEquals(3, comparisonResult.systemPassRates.size)
        assertEquals("System 1", comparisonResult.bestSystem)
        assertEquals("System 3", comparisonResult.worstSystem)
        assertEquals(3, comparisonResult.systemComparisons.size)
        
        // 验证系统通过率
        assertEquals(1.0, comparisonResult.systemPassRates["System 1"])
        assertEquals(0.5, comparisonResult.systemPassRates["System 2"])
        assertEquals(0.0, comparisonResult.systemPassRates["System 3"])
        
        // 验证系统比较
        val comparison1 = comparisonResult.systemComparisons.find { it.system1 == "System 1" && it.system2 == "System 2" }!!
        assertEquals(0.5, comparison1.passRateDifference)
        assertEquals(100.0, comparison1.passRatePercentageDifference)
        
        val comparison2 = comparisonResult.systemComparisons.find { it.system1 == "System 1" && it.system2 == "System 3" }!!
        assertEquals(1.0, comparison2.passRateDifference)
        assertTrue(comparison2.passRatePercentageDifference > 0)
        
        val comparison3 = comparisonResult.systemComparisons.find { it.system1 == "System 2" && it.system2 == "System 3" }!!
        assertEquals(0.5, comparison3.passRateDifference)
        assertTrue(comparison3.passRatePercentageDifference > 0)
    }
}
