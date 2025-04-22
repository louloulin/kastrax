package ai.kastrax.evals.testsuite

import ai.kastrax.evals.metrics.Metric
import ai.kastrax.evals.metrics.MetricResult
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * 测试用例接口，用于定义测试用例。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
interface TestCase<I, O> {
    /**
     * 测试用例的唯一标识符。
     */
    val id: String

    /**
     * 测试用例的名称。
     */
    val name: String

    /**
     * 测试用例的描述。
     */
    val description: String

    /**
     * 测试用例的标签。
     */
    val tags: Set<String>

    /**
     * 测试用例的输入数据。
     */
    val input: I

    /**
     * 测试用例的期望输出。
     */
    val expectedOutput: O?

    /**
     * 测试用例的评估指标。
     */
    val metrics: List<Metric<I, O>>

    /**
     * 测试用例的评估选项。
     */
    val options: Map<String, Any?>

    /**
     * 执行测试用例。
     *
     * @param actualOutput AI 系统的实际输出
     * @return 测试用例结果
     */
    suspend fun execute(actualOutput: O): TestCaseResult<I, O>
}

/**
 * 测试用例结果，表示测试用例执行的结果。
 *
 * @param I 输入类型
 * @param O 输出类型
 * @property testCaseId 测试用例的唯一标识符
 * @property testCaseName 测试用例的名称
 * @property input 测试用例的输入数据
 * @property expectedOutput 测试用例的期望输出
 * @property actualOutput AI 系统的实际输出
 * @property metricResults 评估指标结果
 * @property passed 测试用例是否通过
 * @property startTime 开始时间
 * @property endTime 结束时间
 * @property durationMs 执行时间（毫秒）
 */
data class TestCaseResult<I, O>(
    val testCaseId: String,
    val testCaseName: String,
    val input: I,
    val expectedOutput: O?,
    val actualOutput: O,
    val metricResults: List<MetricResult>,
    val passed: Boolean,
    val startTime: Instant,
    val endTime: Instant,
    val durationMs: Long
)

/**
 * 基本测试用例实现。
 *
 * @param I 输入类型
 * @param O 输出类型
 * @property id 测试用例的唯一标识符
 * @property name 测试用例的名称
 * @property description 测试用例的描述
 * @property tags 测试用例的标签
 * @property input 测试用例的输入数据
 * @property expectedOutput 测试用例的期望输出
 * @property metrics 测试用例的评估指标
 * @property options 测试用例的评估选项
 * @property threshold 测试用例通过的阈值
 */
class BasicTestCase<I, O>(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String,
    override val description: String = "",
    override val tags: Set<String> = emptySet(),
    override val input: I,
    override val expectedOutput: O? = null,
    override val metrics: List<Metric<I, O>>,
    override val options: Map<String, Any?> = emptyMap(),
    val threshold: Double = 0.7
) : TestCase<I, O> {
    
    override suspend fun execute(actualOutput: O): TestCaseResult<I, O> {
        logger.info { "Executing test case: $name" }
        
        val startTime = Instant.now()
        
        // 计算所有指标的结果
        val metricResults = metrics.map { metric ->
            try {
                // 合并测试用例选项和指标特定选项
                val mergedOptions = options.toMutableMap()
                
                // 如果有期望输出，添加到选项中
                if (expectedOutput != null) {
                    mergedOptions["expected"] = expectedOutput
                }
                
                metric.calculate(input, actualOutput, mergedOptions)
            } catch (e: Exception) {
                logger.error(e) { "Error calculating metric: ${metric.name}" }
                ai.kastrax.evals.metrics.MetricResult(0.0, mapOf("error" to e.message))
            }
        }
        
        // 计算平均分数
        val averageScore = if (metricResults.isNotEmpty()) {
            metricResults.sumOf { it.score } / metricResults.size
        } else {
            0.0
        }
        
        // 判断测试用例是否通过
        val passed = averageScore >= threshold
        
        val endTime = Instant.now()
        val durationMs = endTime.toEpochMilli() - startTime.toEpochMilli()
        
        return TestCaseResult(
            testCaseId = id,
            testCaseName = name,
            input = input,
            expectedOutput = expectedOutput,
            actualOutput = actualOutput,
            metricResults = metricResults,
            passed = passed,
            startTime = startTime,
            endTime = endTime,
            durationMs = durationMs
        )
    }
}

/**
 * 测试用例构建器，用于创建测试用例。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
class TestCaseBuilder<I, O> {
    var id: String = UUID.randomUUID().toString()
    var name: String = ""
    var description: String = ""
    var tags: MutableSet<String> = mutableSetOf()
    var input: I? = null
    var expectedOutput: O? = null
    var metrics: MutableList<Metric<I, O>> = mutableListOf()
    var options: MutableMap<String, Any?> = mutableMapOf()
    var threshold: Double = 0.7
    
    /**
     * 添加标签。
     *
     * @param tag 标签
     */
    fun tag(tag: String) {
        tags.add(tag)
    }
    
    /**
     * 添加评估指标。
     *
     * @param metric 评估指标
     */
    fun metric(metric: Metric<I, O>) {
        metrics.add(metric)
    }
    
    /**
     * 添加评估选项。
     *
     * @param key 选项键
     * @param value 选项值
     */
    fun option(key: String, value: Any?) {
        options[key] = value
    }
    
    /**
     * 构建测试用例。
     *
     * @return 测试用例
     */
    fun build(): TestCase<I, O> {
        require(name.isNotBlank()) { "Test case name cannot be blank" }
        requireNotNull(input) { "Test case input cannot be null" }
        require(metrics.isNotEmpty()) { "Test case must have at least one metric" }
        
        return BasicTestCase(
            id = id,
            name = name,
            description = description,
            tags = tags,
            input = input!!,
            expectedOutput = expectedOutput,
            metrics = metrics,
            options = options,
            threshold = threshold
        )
    }
}

/**
 * 创建测试用例。
 *
 * @param I 输入类型
 * @param O 输出类型
 * @param block 构建器配置块
 * @return 测试用例
 */
fun <I, O> testCase(block: TestCaseBuilder<I, O>.() -> Unit): TestCase<I, O> {
    val builder = TestCaseBuilder<I, O>()
    builder.block()
    return builder.build()
}
