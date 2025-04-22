package ai.kastrax.evals.benchmark

import ai.kastrax.evals.metrics.AccuracyMethod
import ai.kastrax.evals.metrics.Metric
import ai.kastrax.evals.metrics.MetricCategory
import ai.kastrax.evals.metrics.RelevanceMethod
import ai.kastrax.evals.metrics.accuracyMetric
import ai.kastrax.evals.metrics.compositeMetric
import ai.kastrax.evals.metrics.llm.LlmClient
import ai.kastrax.evals.metrics.llm.correctnessMetric
import ai.kastrax.evals.metrics.llm.usefulnessMetric
import ai.kastrax.evals.metrics.relevanceMetric
import ai.kastrax.evals.metrics.responseTimeMetric
import ai.kastrax.evals.testsuite.TestCase
import ai.kastrax.evals.testsuite.TestSuite
import ai.kastrax.evals.testsuite.testCase
import ai.kastrax.evals.testsuite.testSuite
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 创建问答基准测试。
 *
 * @param name 基准测试的名称
 * @param description 基准测试的描述
 * @param testCases 测试用例列表
 * @param llmClient LLM 客户端，用于 LLM 辅助评估
 * @return 问答基准测试
 */
fun createQABenchmark(
    name: String = "问答基准测试",
    description: String = "评估 AI 系统回答问题的能力",
    testCases: List<TestCase<String, String>>,
    llmClient: LlmClient? = null
): Benchmark<String, String> {
    logger.info { "Creating QA benchmark: $name" }

    // 创建测试套件
    val testSuite = testSuite<String, String> {
        this.name = "问答测试套件"
        this.description = "评估 AI 系统回答问题的能力"
        tag("qa")

        // 添加测试用例
        testCases.forEach { testCase ->
            testCase(testCase)
        }
    }

    // 创建基准测试
    return benchmark {
        this.name = name
        this.description = description
        testSuite(testSuite)
    }
}

/**
 * 创建摘要基准测试。
 *
 * @param name 基准测试的名称
 * @param description 基准测试的描述
 * @param testCases 测试用例列表
 * @param llmClient LLM 客户端，用于 LLM 辅助评估
 * @return 摘要基准测试
 */
fun createSummarizationBenchmark(
    name: String = "摘要基准测试",
    description: String = "评估 AI 系统生成摘要的能力",
    testCases: List<TestCase<String, String>>,
    llmClient: LlmClient? = null
): Benchmark<String, String> {
    logger.info { "Creating summarization benchmark: $name" }

    // 创建测试套件
    val testSuite = testSuite<String, String> {
        this.name = "摘要测试套件"
        this.description = "评估 AI 系统生成摘要的能力"
        tag("summarization")

        // 添加测试用例
        testCases.forEach { testCase ->
            testCase(testCase)
        }
    }

    // 创建基准测试
    return benchmark {
        this.name = name
        this.description = description
        testSuite(testSuite)
    }
}

/**
 * 创建翻译基准测试。
 *
 * @param name 基准测试的名称
 * @param description 基准测试的描述
 * @param testCases 测试用例列表
 * @param llmClient LLM 客户端，用于 LLM 辅助评估
 * @return 翻译基准测试
 */
fun createTranslationBenchmark(
    name: String = "翻译基准测试",
    description: String = "评估 AI 系统翻译文本的能力",
    testCases: List<TestCase<String, String>>,
    llmClient: LlmClient? = null
): Benchmark<String, String> {
    logger.info { "Creating translation benchmark: $name" }

    // 创建测试套件
    val testSuite = testSuite<String, String> {
        this.name = "翻译测试套件"
        this.description = "评估 AI 系统翻译文本的能力"
        tag("translation")

        // 添加测试用例
        testCases.forEach { testCase ->
            testCase(testCase)
        }
    }

    // 创建基准测试
    return benchmark {
        this.name = name
        this.description = description
        testSuite(testSuite)
    }
}

/**
 * 创建性能基准测试。
 *
 * @param name 基准测试的名称
 * @param description 基准测试的描述
 * @param inputGenerator 输入生成器
 * @param inputCount 输入数量
 * @param targetTimeMs 目标响应时间（毫秒）
 * @param maxTimeMs 最大响应时间（毫秒）
 * @return 性能基准测试
 */
fun createPerformanceBenchmark(
    name: String = "性能基准测试",
    description: String = "评估 AI 系统的性能",
    inputGenerator: () -> String,
    inputCount: Int = 100,
    targetTimeMs: Long = 1000,
    maxTimeMs: Long = 5000
): Benchmark<String, String> {
    logger.info { "Creating performance benchmark: $name" }

    // 创建评估指标
    val rtMetric = responseTimeMetric<String, String>(
        targetTimeMs = targetTimeMs,
        maxTimeMs = maxTimeMs
    )

    // 创建测试用例
    val perfCases = List(inputCount) { index ->
        val input = inputGenerator()

        testCase<String, String> {
            this.name = "性能测试 ${index + 1}"
            this.description = "评估 AI 系统的响应时间"
            this.input = input
            this.metric(rtMetric)
            tag("performance")
        }
    }

    // 创建测试套件
    val testSuite = testSuite<String, String> {
        this.name = "性能测试套件"
        this.description = "评估 AI 系统的性能"
        tag("performance")

        // 添加测试用例
        perfCases.forEach { testCase ->
            testCase(testCase)
        }
    }

    // 创建基准测试
    return benchmark {
        this.name = name
        this.description = description
        testSuite(testSuite)
    }
}
