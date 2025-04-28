package ai.kastrax.rag.tools

import ai.kastrax.rag.llm.LlmClient
import ai.kastrax.rag.metrics.rag.RagMetric
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val logger = KotlinLogging.logger {}

/**
 * RAG 基准测试工具，用于创建和运行 RAG 评估基准测试。
 *
 * @property rag RAG 系统
 * @property llmClient LLM 客户端，用于 LLM 辅助评估
 * @property evaluationTool RAG 评估工具
 */
class RagBenchmarkTool(
    private val rag: RAG,
    private val llmClient: LlmClient? = null,
    private val evaluationTool: RagEvaluationTool = RagEvaluationTool(rag, llmClient)
) {
    /**
     * 创建 RAG 基准测试。
     *
     * @param name 基准测试名称
     * @param description 基准测试描述
     * @param testCases 测试用例列表
     * @param metrics 评估指标列表
     * @return RAG 基准测试
     */
    fun createBenchmark(
        name: String,
        description: String,
        testCases: List<RagTestCase>,
        metrics: List<RagMetric> = evaluationTool.metrics
    ): RagBenchmark {
        logger.info { "Creating RAG benchmark: $name" }

        return RagBenchmark(
            name = name,
            description = description,
            testCases = testCases,
            metrics = metrics,
            rag = rag,
            llmClient = llmClient,
            evaluationTool = evaluationTool
        )
    }

    /**
     * 从文件加载测试用例。
     *
     * @param filePath 文件路径
     * @return 测试用例列表
     */
    fun loadTestCasesFromFile(filePath: String): List<RagTestCase> {
        logger.info { "Loading test cases from file: $filePath" }

        // 读取文件内容
        val content = java.io.File(filePath).readText()

        // 解析测试用例
        return parseTestCases(content)
    }

    /**
     * 解析测试用例。
     *
     * @param content 文件内容
     * @return 测试用例列表
     */
    private fun parseTestCases(content: String): List<RagTestCase> {
        // 按行分割
        val lines = content.lines()

        // 解析测试用例
        val testCases = mutableListOf<RagTestCase>()
        var currentTestCase: MutableMap<String, String>? = null

        for (line in lines) {
            val trimmedLine = line.trim()

            // 跳过空行和注释
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue
            }

            // 检查是否是新测试用例的开始
            if (trimmedLine.startsWith("---")) {
                // 保存当前测试用例（如果有）
                currentTestCase?.let {
                    val name = it["name"] ?: "Test Case ${testCases.size + 1}"
                    val query = it["query"] ?: throw IllegalArgumentException("Test case must have a query")
                    val groundTruth = it["groundTruth"]
                    val tags = it["tags"]?.split(",")?.map { tag -> tag.trim() } ?: emptyList()

                    testCases.add(RagTestCase(name, query, groundTruth, tags))
                }

                // 创建新测试用例
                currentTestCase = mutableMapOf()
                continue
            }

            // 解析键值对
            val parts = trimmedLine.split(":", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim()

                currentTestCase?.put(key, value)
            }
        }

        // 保存最后一个测试用例（如果有）
        currentTestCase?.let {
            val name = it["name"] ?: "Test Case ${testCases.size + 1}"
            val query = it["query"] ?: throw IllegalArgumentException("Test case must have a query")
            val groundTruth = it["groundTruth"]
            val tags = it["tags"]?.split(",")?.map { tag -> tag.trim() } ?: emptyList()

            testCases.add(RagTestCase(name, query, groundTruth, tags))
        }

        return testCases
    }
}

/**
 * RAG 测试用例。
 *
 * @property name 测试用例名称
 * @property query 查询
 * @property groundTruth 参考答案（可选）
 * @property tags 标签列表
 */
data class RagTestCase(
    val name: String,
    val query: String,
    val groundTruth: String? = null,
    val tags: List<String> = emptyList()
)

/**
 * RAG 基准测试。
 *
 * @property name 基准测试名称
 * @property description 基准测试描述
 * @property testCases 测试用例列表
 * @property metrics 评估指标列表
 * @property rag RAG 系统
 * @property llmClient LLM 客户端，用于 LLM 辅助评估
 * @property evaluationTool RAG 评估工具
 */
class RagBenchmark(
    val name: String,
    val description: String,
    val testCases: List<RagTestCase>,
    val metrics: List<RagMetric>,
    private val rag: RAG,
    private val llmClient: LlmClient? = null,
    private val evaluationTool: RagEvaluationTool
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 运行基准测试。
     *
     * @param generateAnswer 生成回答的函数
     * @param options RAG 处理选项
     * @return 基准测试结果
     */
    suspend fun run(
        generateAnswer: suspend (String, String) -> String,
        options: RagProcessOptions? = null
    ): RagBenchmarkResult = coroutineScope {
        logger.info { "Running RAG benchmark: $name" }

        // 并行评估每个测试用例
        val results = testCases.map { testCase ->
            async {
                try {
                    // 获取上下文
                    val context = rag.generateContext(testCase.query, options = options)

                    // 生成回答
                    val answer = generateAnswer(testCase.query, context)

                    // 评估
                    val result = evaluationTool.evaluate(
                        query = testCase.query,
                        answer = answer,
                        options = options,
                        groundTruth = testCase.groundTruth
                    )

                    RagTestResult(testCase, result)
                } catch (e: Exception) {
                    logger.error(e) { "Error evaluating test case: ${testCase.name}" }
                    RagTestResult(
                        testCase = testCase,
                        result = null,
                        error = e.message
                    )
                }
            }
        }.map { it.await() }

        // 计算总体分数
        val overallScore = results
            .mapNotNull { it.result?.overallScore }
            .takeIf { it.isNotEmpty() }
            ?.average() ?: 0.0

        RagBenchmarkResult(
            benchmarkName = name,
            testResults = results,
            overallScore = overallScore
        )
    }

    /**
     * 生成基准测试报告。
     *
     * @param result 基准测试结果
     * @param detailed 是否生成详细报告
     * @return 基准测试报告
     */
    fun generateReport(result: RagBenchmarkResult, detailed: Boolean = false): String {
        return buildString {
            append("# RAG 基准测试报告: ${result.benchmarkName}\n\n")

            append("## 总体评分\n\n")
            append("总体分数: ${result.overallScore.format(2)}\n\n")

            append("## 测试用例结果\n\n")
            append("| 测试用例 | 总体分数 | 检索精确度 | 上下文相关性 | 回答质量 | 幻觉检测 |\n")
            append("|----------|----------|------------|--------------|----------|----------|\n")

            result.testResults.forEach { testResult ->
                val result = testResult.result
                if (result != null) {
                    val retrievalPrecision = result.metricResults["RetrievalPrecisionMetric"]?.score?.format(2) ?: "N/A"
                    val contextRelevance = result.metricResults["ContextRelevanceMetric"]?.score?.format(2) ?: "N/A"
                    val answerQuality = result.metricResults["AnswerQualityMetric"]?.score?.format(2) ?: "N/A"
                    val hallucination = result.metricResults["HallucinationMetric"]?.score?.format(2) ?: "N/A"

                    append("| ${testResult.testCase.name} ")
                    append("| ${result.overallScore.format(2)} ")
                    append("| $retrievalPrecision ")
                    append("| $contextRelevance ")
                    append("| $answerQuality ")
                    append("| $hallucination |\n")
                } else {
                    append("| ${testResult.testCase.name} | 错误 | - | - | - | - |\n")
                }
            }

            if (detailed) {
                append("\n## 详细测试结果\n\n")
                result.testResults.forEachIndexed { index, testResult ->
                    append("### 测试用例 ${index + 1}: ${testResult.testCase.name}\n\n")
                    append("查询: ${testResult.testCase.query}\n\n")

                    if (testResult.testCase.groundTruth != null) {
                        append("参考答案: ${testResult.testCase.groundTruth}\n\n")
                    }

                    if (testResult.error != null) {
                        append("错误: ${testResult.error}\n\n")
                    } else if (testResult.result != null) {
                        append("回答: ${testResult.result.answer}\n\n")

                        append("#### 评估结果\n\n")
                        append("总体分数: ${testResult.result.overallScore.format(2)}\n\n")

                        testResult.result.metricResults.forEach { (metricName, metricResult) ->
                            append("$metricName: ${metricResult.score.format(2)}\n\n")

                            // 显示详细信息
                            append("详情:\n\n")
                            metricResult.details.forEach { (key, value) ->
                                if (key != "query" && key != "context" && key != "answer" && key != "llmResponse") {
                                    append("- $key: $value\n")
                                }
                            }
                            append("\n")
                        }
                    }

                    append("\n---\n\n")
                }
            }
        }
    }

    /**
     * 格式化 Double 值，保留指定位数的小数。
     *
     * @param digits 小数位数
     * @return 格式化后的字符串
     */
    private fun Double.format(digits: Int): String {
        return "%.${digits}f".format(this)
    }
}

/**
 * RAG 测试结果。
 *
 * @property testCase 测试用例
 * @property result 评估结果
 * @property error 错误信息（如果有）
 */
data class RagTestResult(
    val testCase: RagTestCase,
    val result: RagEvaluationResult? = null,
    val error: String? = null
)

/**
 * RAG 基准测试结果。
 *
 * @property benchmarkName 基准测试名称
 * @property testResults 测试结果列表
 * @property overallScore 总体分数
 */
data class RagBenchmarkResult(
    val benchmarkName: String,
    val testResults: List<RagTestResult>,
    val overallScore: Double
)
