package ai.kastrax.evals.testsuite

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private val logger = KotlinLogging.logger {}

/**
 * 测试套件接口，用于组织和管理测试用例。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
interface TestSuite<I, O> {
    /**
     * 测试套件的唯一标识符。
     */
    val id: String

    /**
     * 测试套件的名称。
     */
    val name: String

    /**
     * 测试套件的描述。
     */
    val description: String

    /**
     * 测试套件的标签。
     */
    val tags: Set<String>

    /**
     * 测试套件中的测试用例。
     */
    val testCases: List<TestCase<I, O>>

    /**
     * 添加测试用例到测试套件。
     *
     * @param testCase 测试用例
     */
    fun addTestCase(testCase: TestCase<I, O>)

    /**
     * 添加多个测试用例到测试套件。
     *
     * @param testCases 测试用例列表
     */
    fun addTestCases(testCases: List<TestCase<I, O>>)

    /**
     * 获取指定标签的测试用例。
     *
     * @param tag 标签
     * @return 测试用例列表
     */
    fun getTestCasesByTag(tag: String): List<TestCase<I, O>>

    /**
     * 获取指定标签的测试用例。
     *
     * @param tags 标签集合
     * @return 测试用例列表
     */
    fun getTestCasesByTags(tags: Set<String>): List<TestCase<I, O>>

    /**
     * 执行测试套件。
     *
     * @param outputProvider 输出提供者，用于获取 AI 系统的输出
     * @param parallel 是否并行执行测试用例
     * @return 测试套件结果
     */
    suspend fun execute(
        outputProvider: OutputProvider<I, O>,
        parallel: Boolean = false
    ): TestSuiteResult<I, O>
}

/**
 * 输出提供者接口，用于获取 AI 系统的输出。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
interface OutputProvider<I, O> {
    /**
     * 获取 AI 系统的输出。
     *
     * @param input 输入数据
     * @return AI 系统的输出
     */
    suspend fun getOutput(input: I): O
}

/**
 * 测试套件结果，表示测试套件执行的结果。
 *
 * @param I 输入类型
 * @param O 输出类型
 * @property testSuiteId 测试套件的唯一标识符
 * @property testSuiteName 测试套件的名称
 * @property testCaseResults 测试用例结果列表
 * @property passedCount 通过的测试用例数量
 * @property failedCount 失败的测试用例数量
 * @property passRate 通过率
 * @property startTime 开始时间
 * @property endTime 结束时间
 * @property durationMs 执行时间（毫秒）
 */
data class TestSuiteResult<I, O>(
    val testSuiteId: String,
    val testSuiteName: String,
    val testCaseResults: List<TestCaseResult<I, O>>,
    val passedCount: Int,
    val failedCount: Int,
    val passRate: Double,
    val startTime: Instant,
    val endTime: Instant,
    val durationMs: Long
)

/**
 * 基本测试套件实现。
 *
 * @param I 输入类型
 * @param O 输出类型
 * @property id 测试套件的唯一标识符
 * @property name 测试套件的名称
 * @property description 测试套件的描述
 * @property tags 测试套件的标签
 */
class BasicTestSuite<I, O>(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String,
    override val description: String = "",
    override val tags: Set<String> = emptySet()
) : TestSuite<I, O> {
    
    private val _testCases = mutableListOf<TestCase<I, O>>()
    
    override val testCases: List<TestCase<I, O>>
        get() = _testCases.toList()
    
    override fun addTestCase(testCase: TestCase<I, O>) {
        _testCases.add(testCase)
    }
    
    override fun addTestCases(testCases: List<TestCase<I, O>>) {
        _testCases.addAll(testCases)
    }
    
    override fun getTestCasesByTag(tag: String): List<TestCase<I, O>> {
        return _testCases.filter { it.tags.contains(tag) }
    }
    
    override fun getTestCasesByTags(tags: Set<String>): List<TestCase<I, O>> {
        return _testCases.filter { it.tags.intersect(tags).isNotEmpty() }
    }
    
    override suspend fun execute(
        outputProvider: OutputProvider<I, O>,
        parallel: Boolean
    ): TestSuiteResult<I, O> = coroutineScope {
        logger.info { "Executing test suite: $name" }
        
        val startTime = Instant.now()
        
        val testCaseResults = if (parallel) {
            // 并行执行测试用例
            _testCases.map { testCase ->
                async {
                    try {
                        val output = outputProvider.getOutput(testCase.input)
                        testCase.execute(output)
                    } catch (e: Exception) {
                        logger.error(e) { "Error executing test case: ${testCase.name}" }
                        TestCaseResult(
                            testCaseId = testCase.id,
                            testCaseName = testCase.name,
                            input = testCase.input,
                            expectedOutput = testCase.expectedOutput,
                            actualOutput = null as O, // 这里会有类型转换警告，但在实际运行时不会出现
                            metricResults = emptyList(),
                            passed = false,
                            startTime = Instant.now(),
                            endTime = Instant.now(),
                            durationMs = 0
                        )
                    }
                }
            }.awaitAll()
        } else {
            // 串行执行测试用例
            _testCases.map { testCase ->
                try {
                    val output = outputProvider.getOutput(testCase.input)
                    testCase.execute(output)
                } catch (e: Exception) {
                    logger.error(e) { "Error executing test case: ${testCase.name}" }
                    TestCaseResult(
                        testCaseId = testCase.id,
                        testCaseName = testCase.name,
                        input = testCase.input,
                        expectedOutput = testCase.expectedOutput,
                        actualOutput = null as O, // 这里会有类型转换警告，但在实际运行时不会出现
                        metricResults = emptyList(),
                        passed = false,
                        startTime = Instant.now(),
                        endTime = Instant.now(),
                        durationMs = 0
                    )
                }
            }
        }
        
        val passedCount = testCaseResults.count { it.passed }
        val failedCount = testCaseResults.size - passedCount
        val passRate = if (testCaseResults.isNotEmpty()) {
            passedCount.toDouble() / testCaseResults.size
        } else {
            0.0
        }
        
        val endTime = Instant.now()
        val durationMs = endTime.toEpochMilli() - startTime.toEpochMilli()
        
        TestSuiteResult(
            testSuiteId = id,
            testSuiteName = name,
            testCaseResults = testCaseResults,
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
 * 测试套件构建器，用于创建测试套件。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
class TestSuiteBuilder<I, O> {
    var id: String = UUID.randomUUID().toString()
    var name: String = ""
    var description: String = ""
    var tags: MutableSet<String> = mutableSetOf()
    private val testCases = mutableListOf<TestCase<I, O>>()
    
    /**
     * 添加标签。
     *
     * @param tag 标签
     */
    fun tag(tag: String) {
        tags.add(tag)
    }
    
    /**
     * 添加测试用例。
     *
     * @param testCase 测试用例
     */
    fun testCase(testCase: TestCase<I, O>) {
        testCases.add(testCase)
    }
    
    /**
     * 创建并添加测试用例。
     *
     * @param block 测试用例构建器配置块
     */
    fun testCase(block: TestCaseBuilder<I, O>.() -> Unit) {
        val testCase = ai.kastrax.evals.testsuite.testCase(block)
        testCases.add(testCase)
    }
    
    /**
     * 构建测试套件。
     *
     * @return 测试套件
     */
    fun build(): TestSuite<I, O> {
        require(name.isNotBlank()) { "Test suite name cannot be blank" }
        
        return BasicTestSuite<I, O>(
            id = id,
            name = name,
            description = description,
            tags = tags
        ).apply {
            addTestCases(testCases)
        }
    }
}

/**
 * 创建测试套件。
 *
 * @param I 输入类型
 * @param O 输出类型
 * @param block 构建器配置块
 * @return 测试套件
 */
fun <I, O> testSuite(block: TestSuiteBuilder<I, O>.() -> Unit): TestSuite<I, O> {
    val builder = TestSuiteBuilder<I, O>()
    builder.block()
    return builder.build()
}
