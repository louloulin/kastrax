package ai.kastrax.evals.testsuite

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private val logger = KotlinLogging.logger {}

/**
 * 批量评估器接口，用于批量评估 AI 系统。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
interface BatchEvaluator<I, O> {
    /**
     * 批量评估 AI 系统。
     *
     * @param testSuites 测试套件列表
     * @param outputProvider 输出提供者
     * @param parallel 是否并行执行测试套件
     * @return 批量评估结果
     */
    suspend fun evaluate(
        testSuites: List<TestSuite<I, O>>,
        outputProvider: OutputProvider<I, O>,
        parallel: Boolean = false
    ): BatchEvaluationResult<I, O>
}

/**
 * 批量评估结果，表示批量评估的结果。
 *
 * @param I 输入类型
 * @param O 输出类型
 * @property id 批量评估的唯一标识符
 * @property testSuiteResults 测试套件结果列表
 * @property passedCount 通过的测试用例数量
 * @property failedCount 失败的测试用例数量
 * @property passRate 通过率
 * @property startTime 开始时间
 * @property endTime 结束时间
 * @property durationMs 执行时间（毫秒）
 */
data class BatchEvaluationResult<I, O>(
    val id: String,
    val testSuiteResults: List<TestSuiteResult<I, O>>,
    val passedCount: Int,
    val failedCount: Int,
    val passRate: Double,
    val startTime: Instant,
    val endTime: Instant,
    val durationMs: Long
)

/**
 * 基本批量评估器实现。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
class BasicBatchEvaluator<I, O> : BatchEvaluator<I, O> {
    
    override suspend fun evaluate(
        testSuites: List<TestSuite<I, O>>,
        outputProvider: OutputProvider<I, O>,
        parallel: Boolean
    ): BatchEvaluationResult<I, O> = coroutineScope {
        logger.info { "Evaluating ${testSuites.size} test suites" }
        
        val startTime = Instant.now()
        
        val testSuiteResults = if (parallel) {
            // 并行执行测试套件
            testSuites.map { testSuite ->
                async {
                    testSuite.execute(outputProvider, parallel)
                }
            }.awaitAll()
        } else {
            // 串行执行测试套件
            testSuites.map { testSuite ->
                testSuite.execute(outputProvider, parallel)
            }
        }
        
        val passedCount = testSuiteResults.sumOf { it.passedCount }
        val failedCount = testSuiteResults.sumOf { it.failedCount }
        val totalCount = passedCount + failedCount
        val passRate = if (totalCount > 0) {
            passedCount.toDouble() / totalCount
        } else {
            0.0
        }
        
        val endTime = Instant.now()
        val durationMs = endTime.toEpochMilli() - startTime.toEpochMilli()
        
        BatchEvaluationResult(
            id = UUID.randomUUID().toString(),
            testSuiteResults = testSuiteResults,
            passedCount = passedCount,
            failedCount = failedCount,
            passRate = passRate,
            startTime = startTime,
            endTime = endTime,
            durationMs = durationMs
        )
    }
}

/**
 * 结果聚合器接口，用于聚合评估结果。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
interface ResultAggregator<I, O> {
    /**
     * 聚合测试用例结果。
     *
     * @param results 测试用例结果列表
     * @return 聚合结果
     */
    fun aggregateTestCaseResults(results: List<TestCaseResult<I, O>>): Map<String, Any?>
    
    /**
     * 聚合测试套件结果。
     *
     * @param results 测试套件结果列表
     * @return 聚合结果
     */
    fun aggregateTestSuiteResults(results: List<TestSuiteResult<I, O>>): Map<String, Any?>
    
    /**
     * 聚合批量评估结果。
     *
     * @param result 批量评估结果
     * @return 聚合结果
     */
    fun aggregateBatchResult(result: BatchEvaluationResult<I, O>): Map<String, Any?>
}

/**
 * 基本结果聚合器实现。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
class BasicResultAggregator<I, O> : ResultAggregator<I, O> {
    
    override fun aggregateTestCaseResults(results: List<TestCaseResult<I, O>>): Map<String, Any?> {
        val passedCount = results.count { it.passed }
        val failedCount = results.size - passedCount
        val passRate = if (results.isNotEmpty()) {
            passedCount.toDouble() / results.size
        } else {
            0.0
        }
        
        val averageScore = if (results.isNotEmpty()) {
            results.flatMap { it.metricResults }.map { it.score }.average()
        } else {
            0.0
        }
        
        val averageDuration = if (results.isNotEmpty()) {
            results.map { it.durationMs }.average()
        } else {
            0.0
        }
        
        return mapOf(
            "totalCount" to results.size,
            "passedCount" to passedCount,
            "failedCount" to failedCount,
            "passRate" to passRate,
            "averageScore" to averageScore,
            "averageDuration" to averageDuration
        )
    }
    
    override fun aggregateTestSuiteResults(results: List<TestSuiteResult<I, O>>): Map<String, Any?> {
        val passedCount = results.sumOf { it.passedCount }
        val failedCount = results.sumOf { it.failedCount }
        val totalCount = passedCount + failedCount
        val passRate = if (totalCount > 0) {
            passedCount.toDouble() / totalCount
        } else {
            0.0
        }
        
        val averagePassRate = if (results.isNotEmpty()) {
            results.map { it.passRate }.average()
        } else {
            0.0
        }
        
        val averageDuration = if (results.isNotEmpty()) {
            results.map { it.durationMs }.average()
        } else {
            0.0
        }
        
        return mapOf(
            "totalSuites" to results.size,
            "totalTestCases" to totalCount,
            "passedCount" to passedCount,
            "failedCount" to failedCount,
            "passRate" to passRate,
            "averagePassRate" to averagePassRate,
            "averageDuration" to averageDuration
        )
    }
    
    override fun aggregateBatchResult(result: BatchEvaluationResult<I, O>): Map<String, Any?> {
        return mapOf(
            "id" to result.id,
            "totalSuites" to result.testSuiteResults.size,
            "totalTestCases" to (result.passedCount + result.failedCount),
            "passedCount" to result.passedCount,
            "failedCount" to result.failedCount,
            "passRate" to result.passRate,
            "duration" to result.durationMs,
            "startTime" to result.startTime,
            "endTime" to result.endTime,
            "suiteResults" to aggregateTestSuiteResults(result.testSuiteResults)
        )
    }
}
