package ai.kastrax.rag.tools

import ai.kastrax.rag.llm.LlmClient
import ai.kastrax.rag.metrics.MetricResult
import ai.kastrax.rag.metrics.rag.*
import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.vectorstore.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val logger = KotlinLogging.logger {}

/**
 * RAG 评估工具，用于评估 RAG 系统的质量。
 *
 * @property rag RAG 系统
 * @property llmClient LLM 客户端，用于 LLM 辅助评估
 * @property metrics 评估指标列表
 */
class RagEvaluationTool(
    private val rag: RAG,
    private val llmClient: LlmClient? = null,
    val metrics: List<RagMetric> = defaultMetrics(llmClient)
) {
    companion object {
        /**
         * 创建默认的评估指标列表。
         *
         * @param llmClient LLM 客户端，用于 LLM 辅助评估
         * @return 评估指标列表
         */
        fun defaultMetrics(llmClient: LlmClient? = null): List<RagMetric> {
            val metrics = mutableListOf<RagMetric>()

            // 添加检索精确度指标
            metrics.add(RetrievalPrecisionMetric(llmClient))

            // 添加上下文相关性指标
            metrics.add(ContextRelevanceMetric(llmClient))

            // 添加回答质量指标
            metrics.add(AnswerQualityMetric(llmClient))

            // 添加幻觉检测指标
            try {
                val hallucinationMetricClass = Class.forName("ai.kastrax.rag.metrics.rag.HallucinationMetric")
                val constructor = hallucinationMetricClass.getConstructor(LlmClient::class.java)
                val hallucinationMetric = constructor.newInstance(llmClient) as RagMetric
                metrics.add(hallucinationMetric)
            } catch (e: Exception) {
                // 如果找不到 HallucinationMetric 类，则忽略
            }

            return metrics
        }
    }

    /**
     * 评估 RAG 系统的质量。
     *
     * @param query 用户查询
     * @param answer 生成的回答
     * @param options RAG 处理选项
     * @param groundTruth 参考答案（可选）
     * @return 评估结果
     */
    suspend fun evaluate(
        query: String,
        answer: String,
        options: RagProcessOptions? = null,
        groundTruth: String? = null
    ): RagEvaluationResult = coroutineScope {
        logger.info { "Evaluating RAG for query: $query" }

        // 获取检索结果和上下文
        val searchResults = rag.search(query, options = options)
        val context = rag.generateContext(query, options = options)

        // 转换检索结果
        val retrievalResults = searchResults.map { result ->
            RetrievalResult(
                content = result.document.content,
                score = result.score,
                metadata = result.document.metadata
            )
        }

        // 创建评估输入
        val evaluationInput = RagEvaluationInput(
            query = query,
            retrievalResults = retrievalResults,
            context = context,
            groundTruth = groundTruth
        )

        // 并行计算所有指标
        val metricResults = metrics.map { metric ->
            async {
                try {
                    val result = metric.calculate(evaluationInput, answer, emptyMap())
                    metric.javaClass.simpleName to result
                } catch (e: Exception) {
                    logger.error(e) { "Error calculating metric: ${metric.javaClass.simpleName}" }
                    metric.javaClass.simpleName to MetricResult(
                        score = 0.0,
                        details = mapOf("error" to e.message)
                    )
                }
            }
        }.map { it.await() }.toMap()

        // 计算总体分数
        val overallScore = if (metricResults.isNotEmpty()) {
            metricResults.values.map { it.score }.average()
        } else {
            0.0
        }

        RagEvaluationResult(
            query = query,
            answer = answer,
            context = context,
            retrievalResults = retrievalResults,
            metricResults = metricResults,
            overallScore = overallScore
        )
    }

    /**
     * 评估多个查询的 RAG 系统质量。
     *
     * @param queries 查询列表
     * @param generateAnswer 生成回答的函数
     * @param options RAG 处理选项
     * @param groundTruths 参考答案列表（可选）
     * @return 评估结果列表
     */
    suspend fun evaluateBatch(
        queries: List<String>,
        generateAnswer: suspend (String, String) -> String,
        options: RagProcessOptions? = null,
        groundTruths: List<String>? = null
    ): List<RagEvaluationResult> = coroutineScope {
        logger.info { "Batch evaluating RAG for ${queries.size} queries" }

        queries.mapIndexed { index, query ->
            async {
                try {
                    // 获取上下文
                    val context = rag.generateContext(query, options = options)

                    // 生成回答
                    val answer = generateAnswer(query, context)

                    // 获取参考答案（如果有）
                    val groundTruth = groundTruths?.getOrNull(index)

                    // 评估
                    evaluate(query, answer, options, groundTruth)
                } catch (e: Exception) {
                    logger.error(e) { "Error evaluating query: $query" }
                    RagEvaluationResult(
                        query = query,
                        answer = "",
                        context = "",
                        retrievalResults = emptyList(),
                        metricResults = emptyMap(),
                        overallScore = 0.0,
                        error = e.message
                    )
                }
            }
        }.map { it.await() }
    }

    /**
     * 生成评估报告。
     *
     * @param result 评估结果
     * @param detailed 是否生成详细报告
     * @return 评估报告
     */
    fun generateReport(result: RagEvaluationResult, detailed: Boolean = false): String {
        return buildString {
            append("# RAG 评估报告\n\n")

            append("## 总体评分\n\n")
            append("总体分数: ${result.overallScore.format(2)}\n\n")

            append("## 查询和回答\n\n")
            append("查询: ${result.query}\n\n")
            append("回答: ${result.answer}\n\n")

            if (detailed) {
                append("## 上下文\n\n")
                append("```\n${result.context}\n```\n\n")

                append("## 检索结果\n\n")
                result.retrievalResults.forEachIndexed { index, retrievalResult ->
                    append("### 结果 ${index + 1} (分数: ${retrievalResult.score.format(3)})\n\n")
                    append("```\n${retrievalResult.content}\n```\n\n")
                }
            }

            append("## 指标评分\n\n")
            result.metricResults.forEach { (metricName, metricResult) ->
                append("### $metricName\n\n")
                append("分数: ${metricResult.score.format(2)}\n\n")

                if (detailed) {
                    append("详情:\n\n")
                    metricResult.details.forEach { (key, value) ->
                        if (key != "query" && key != "context" && key != "answer" && key != "llmResponse") {
                            append("- $key: $value\n")
                        }
                    }
                    append("\n")
                }
            }

            if (result.error != null) {
                append("## 错误\n\n")
                append("${result.error}\n\n")
            }
        }
    }

    /**
     * 生成批量评估报告。
     *
     * @param results 评估结果列表
     * @param detailed 是否生成详细报告
     * @return 批量评估报告
     */
    fun generateBatchReport(results: List<RagEvaluationResult>, detailed: Boolean = false): String {
        return buildString {
            append("# RAG 批量评估报告\n\n")

            val averageScore = results.map { it.overallScore }.average()
            append("## 总体评分\n\n")
            append("平均分数: ${averageScore.format(2)}\n\n")

            append("## 各查询评分\n\n")
            append("| 查询 | 总体分数 | 检索精确度 | 上下文相关性 | 回答质量 | 幻觉检测 |\n")
            append("|------|----------|------------|--------------|----------|----------|\n")

            results.forEach { result ->
                val retrievalPrecision = result.metricResults["RetrievalPrecisionMetric"]?.score?.format(2) ?: "N/A"
                val contextRelevance = result.metricResults["ContextRelevanceMetric"]?.score?.format(2) ?: "N/A"
                val answerQuality = result.metricResults["AnswerQualityMetric"]?.score?.format(2) ?: "N/A"
                val hallucination = result.metricResults["HallucinationMetric"]?.score?.format(2) ?: "N/A"

                append("| ${result.query.take(30)}${if (result.query.length > 30) "..." else ""} ")
                append("| ${result.overallScore.format(2)} ")
                append("| $retrievalPrecision ")
                append("| $contextRelevance ")
                append("| $answerQuality ")
                append("| $hallucination |\n")
            }

            if (detailed) {
                append("\n## 详细评估\n\n")
                results.forEachIndexed { index, result ->
                    append("### 查询 ${index + 1}: ${result.query}\n\n")
                    append(generateReport(result, detailed = true))
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
 * RAG 评估结果。
 *
 * @property query 用户查询
 * @property answer 生成的回答
 * @property context 生成的上下文
 * @property retrievalResults 检索结果
 * @property metricResults 各指标的评估结果
 * @property overallScore 总体分数
 * @property error 错误信息（如果有）
 */
data class RagEvaluationResult(
    val query: String,
    val answer: String,
    val context: String,
    val retrievalResults: List<RetrievalResult>,
    val metricResults: Map<String, MetricResult>,
    val overallScore: Double,
    val error: String? = null
)
