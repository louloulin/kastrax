package ai.kastrax.evals.benchmark

import ai.kastrax.evals.testsuite.BatchEvaluationResult
import ai.kastrax.evals.testsuite.BatchEvaluator
import ai.kastrax.evals.testsuite.BasicBatchEvaluator
import ai.kastrax.evals.testsuite.OutputProvider
import ai.kastrax.evals.testsuite.TestSuite
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.coroutineScope

private val logger = KotlinLogging.logger {}

/**
 * 基准测试接口，用于比较不同 AI 系统的性能。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
interface Benchmark<I, O> {
    /**
     * 基准测试的唯一标识符。
     */
    val id: String

    /**
     * 基准测试的名称。
     */
    val name: String

    /**
     * 基准测试的描述。
     */
    val description: String

    /**
     * 基准测试的测试套件。
     */
    val testSuites: List<TestSuite<I, O>>

    /**
     * 运行基准测试。
     *
     * @param outputProviders 输出提供者映射，键为 AI 系统的名称，值为输出提供者
     * @param parallel 是否并行执行测试套件
     * @return 基准测试结果
     */
    suspend fun run(
        outputProviders: Map<String, OutputProvider<I, O>>,
        parallel: Boolean = false
    ): BenchmarkResult<I, O>
}

/**
 * 基准测试结果，表示基准测试的结果。
 *
 * @param I 输入类型
 * @param O 输出类型
 * @property id 基准测试结果的唯一标识符
 * @property benchmarkId 基准测试的唯一标识符
 * @property benchmarkName 基准测试的名称
 * @property systemResults 系统结果映射，键为 AI 系统的名称，值为批量评估结果
 * @property startTime 开始时间
 * @property endTime 结束时间
 * @property durationMs 执行时间（毫秒）
 */
data class BenchmarkResult<I, O>(
    val id: String,
    val benchmarkId: String,
    val benchmarkName: String,
    val systemResults: Map<String, BatchEvaluationResult<I, O>>,
    val startTime: Instant,
    val endTime: Instant,
    val durationMs: Long
)

/**
 * 基本基准测试实现。
 *
 * @param I 输入类型
 * @param O 输出类型
 * @property id 基准测试的唯一标识符
 * @property name 基准测试的名称
 * @property description 基准测试的描述
 * @property testSuites 基准测试的测试套件
 */
class BasicBenchmark<I, O>(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String,
    override val description: String = "",
    override val testSuites: List<TestSuite<I, O>>
) : Benchmark<I, O> {
    
    private val batchEvaluator: BatchEvaluator<I, O> = BasicBatchEvaluator()
    
    override suspend fun run(
        outputProviders: Map<String, OutputProvider<I, O>>,
        parallel: Boolean
    ): BenchmarkResult<I, O> = coroutineScope {
        logger.info { "Running benchmark: $name" }
        
        val startTime = Instant.now()
        
        val systemResults = mutableMapOf<String, BatchEvaluationResult<I, O>>()
        
        for ((systemName, outputProvider) in outputProviders) {
            logger.info { "Evaluating system: $systemName" }
            
            val result = batchEvaluator.evaluate(testSuites, outputProvider, parallel)
            systemResults[systemName] = result
        }
        
        val endTime = Instant.now()
        val durationMs = endTime.toEpochMilli() - startTime.toEpochMilli()
        
        BenchmarkResult(
            id = UUID.randomUUID().toString(),
            benchmarkId = id,
            benchmarkName = name,
            systemResults = systemResults,
            startTime = startTime,
            endTime = endTime,
            durationMs = durationMs
        )
    }
}

/**
 * 基准测试构建器，用于创建基准测试。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
class BenchmarkBuilder<I, O> {
    var id: String = UUID.randomUUID().toString()
    var name: String = ""
    var description: String = ""
    private val testSuites = mutableListOf<TestSuite<I, O>>()
    
    /**
     * 添加测试套件。
     *
     * @param testSuite 测试套件
     */
    fun testSuite(testSuite: TestSuite<I, O>) {
        testSuites.add(testSuite)
    }
    
    /**
     * 构建基准测试。
     *
     * @return 基准测试
     */
    fun build(): Benchmark<I, O> {
        require(name.isNotBlank()) { "Benchmark name cannot be blank" }
        require(testSuites.isNotEmpty()) { "Benchmark must have at least one test suite" }
        
        return BasicBenchmark(
            id = id,
            name = name,
            description = description,
            testSuites = testSuites
        )
    }
}

/**
 * 创建基准测试。
 *
 * @param I 输入类型
 * @param O 输出类型
 * @param block 构建器配置块
 * @return 基准测试
 */
fun <I, O> benchmark(block: BenchmarkBuilder<I, O>.() -> Unit): Benchmark<I, O> {
    val builder = BenchmarkBuilder<I, O>()
    builder.block()
    return builder.build()
}

/**
 * 基准测试比较器，用于比较不同 AI 系统的性能。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
class BenchmarkComparator<I, O> {
    
    /**
     * 比较基准测试结果。
     *
     * @param result 基准测试结果
     * @return 比较结果
     */
    fun compare(result: BenchmarkResult<I, O>): BenchmarkComparisonResult {
        logger.info { "Comparing benchmark results: ${result.benchmarkName}" }
        
        val systemNames = result.systemResults.keys.toList()
        val systemPassRates = result.systemResults.mapValues { (_, batchResult) -> batchResult.passRate }
        
        val bestSystem = systemPassRates.maxByOrNull { it.value }?.key ?: ""
        val worstSystem = systemPassRates.minByOrNull { it.value }?.key ?: ""
        
        val systemComparisons = mutableListOf<SystemComparison>()
        
        for (i in systemNames.indices) {
            for (j in i + 1 until systemNames.size) {
                val system1 = systemNames[i]
                val system2 = systemNames[j]
                
                val passRate1 = systemPassRates[system1] ?: 0.0
                val passRate2 = systemPassRates[system2] ?: 0.0
                
                val difference = passRate1 - passRate2
                val percentageDifference = if (passRate2 != 0.0) {
                    difference / passRate2 * 100
                } else {
                    0.0
                }
                
                systemComparisons.add(
                    SystemComparison(
                        system1 = system1,
                        system2 = system2,
                        passRateDifference = difference,
                        passRatePercentageDifference = percentageDifference
                    )
                )
            }
        }
        
        return BenchmarkComparisonResult(
            benchmarkId = result.benchmarkId,
            benchmarkName = result.benchmarkName,
            systemNames = systemNames,
            systemPassRates = systemPassRates,
            bestSystem = bestSystem,
            worstSystem = worstSystem,
            systemComparisons = systemComparisons
        )
    }
}

/**
 * 基准测试比较结果，表示基准测试比较的结果。
 *
 * @property benchmarkId 基准测试的唯一标识符
 * @property benchmarkName 基准测试的名称
 * @property systemNames 系统名称列表
 * @property systemPassRates 系统通过率映射，键为 AI 系统的名称，值为通过率
 * @property bestSystem 最佳系统的名称
 * @property worstSystem 最差系统的名称
 * @property systemComparisons 系统比较列表
 */
data class BenchmarkComparisonResult(
    val benchmarkId: String,
    val benchmarkName: String,
    val systemNames: List<String>,
    val systemPassRates: Map<String, Double>,
    val bestSystem: String,
    val worstSystem: String,
    val systemComparisons: List<SystemComparison>
)

/**
 * 系统比较，表示两个系统之间的比较。
 *
 * @property system1 第一个系统的名称
 * @property system2 第二个系统的名称
 * @property passRateDifference 通过率差异
 * @property passRatePercentageDifference 通过率百分比差异
 */
data class SystemComparison(
    val system1: String,
    val system2: String,
    val passRateDifference: Double,
    val passRatePercentageDifference: Double
)
