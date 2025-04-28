package ai.kastrax.rag.metrics.rag

import ai.kastrax.rag.llm.LlmClient
import ai.kastrax.rag.metrics.MetricResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 回答质量指标，评估生成的回答的质量。
 *
 * @property llmClient LLM 客户端，用于 LLM 辅助评估
 * @property systemPrompt 系统提示，用于 LLM 评估
 */
class AnswerQualityMetric(
    llmClient: LlmClient? = null,
    private val systemPrompt: String = DEFAULT_SYSTEM_PROMPT
) : RagMetric(llmClient) {

    companion object {
        private const val DEFAULT_SYSTEM_PROMPT = """
            你是一个专业的回答质量评估专家。你的任务是评估生成的回答的质量。

            评估标准：
            1. 准确性：回答是否基于提供的上下文，是否包含事实错误
            2. 完整性：回答是否完整回答了用户的问题
            3. 相关性：回答是否与用户查询相关
            4. 清晰度：回答是否清晰、易于理解
            5. 简洁性：回答是否简洁，不包含冗余信息

            请根据以上标准，给出一个 0-1 之间的分数，其中：
            - 0 表示完全不准确或无关
            - 0.25 表示准确性低，但有一些相关内容
            - 0.5 表示部分准确，回答了部分问题
            - 0.75 表示大体准确且相关，但可能不完整或不够清晰
            - 1 表示高度准确、完整、相关且清晰

            请确保你的评估是客观的，并提供详细的理由。
        """
    }

    /**
     * 计算回答质量指标。
     *
     * @param input RAG 评估输入，包含查询、检索结果和上下文
     * @param output 生成的回答
     * @param options 评估选项
     * @return 评估结果
     */
    override suspend fun calculateRagMetric(
        input: RagEvaluationInput,
        output: String,
        options: Map<String, Any?>
    ): MetricResult {
        logger.debug { "Calculating answer quality for query: ${input.query}" }

        // 如果没有 LLM 客户端，使用基于规则的评估
        if (llmClient == null) {
            return calculateRuleBasedQuality(input, output)
        }

        try {
            // 构建评估提示
            val prompt = buildPrompt(input, output)

            // 使用 LLM 生成评估结果
            val llmResponse = llmClient.generate(systemPrompt, prompt)

            // 从 LLM 的输出中提取分数
            val score = extractScore(llmResponse)

            return MetricResult(
                score = score,
                details = mapOf(
                    "query" to input.query,
                    "context" to input.context,
                    "answer" to output,
                    "llmResponse" to llmResponse
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error evaluating answer quality with LLM" }

            // 出错时回退到基于规则的评估
            return calculateRuleBasedQuality(input, output)
        }
    }

    /**
     * 构建评估提示。
     *
     * @param input RAG 评估输入
     * @param output 生成的回答
     * @return 评估提示
     */
    private fun buildPrompt(input: RagEvaluationInput, output: String): String {
        return buildString {
            append("用户查询: ${input.query}\n\n")
            append("提供的上下文:\n${input.context}\n\n")
            append("生成的回答:\n$output\n\n")

            // 如果有参考答案，也包含在提示中
            if (input.groundTruth != null) {
                append("参考答案:\n${input.groundTruth}\n\n")
            }

            append("请评估这个回答的质量，并给出一个 0-1 之间的分数。")
            append("请详细解释你的评分理由，并指出回答的优点和不足。")
        }
    }

    /**
     * 从 LLM 的输出中提取分数。
     *
     * @param llmResponse LLM 的输出
     * @return 提取的分数
     */
    private fun extractScore(llmResponse: String): Double {
        // 尝试从响应中提取分数
        val scoreRegex = "分数：?(\\d+(\\.\\d+)?)".toRegex()
        val matchResult = scoreRegex.find(llmResponse)

        return matchResult?.groupValues?.get(1)?.toDoubleOrNull()
            ?: run {
                // 如果没有找到明确的分数，尝试从文本中推断
                when {
                    llmResponse.contains("高度准确") || llmResponse.contains("完整") -> 1.0
                    llmResponse.contains("大体准确") -> 0.75
                    llmResponse.contains("部分准确") -> 0.5
                    llmResponse.contains("准确性低") -> 0.25
                    llmResponse.contains("完全不准确") || llmResponse.contains("无关") -> 0.0
                    else -> 0.5 // 默认中等分数
                }
            }
    }

    /**
     * 基于规则计算回答质量。
     *
     * @param input RAG 评估输入
     * @param output 生成的回答
     * @return 评估结果
     */
    private fun calculateRuleBasedQuality(input: RagEvaluationInput, output: String): MetricResult {
        // 如果没有回答，返回 0 分
        if (output.isBlank()) {
            return MetricResult(
                score = 0.0,
                details = mapOf(
                    "reason" to "回答为空"
                )
            )
        }

        // 计算回答长度
        val answerLength = output.length

        // 检查回答是否包含上下文中的关键信息
        val contextWords = input.context.split(" ", "，", "。", "、", "：", "；", "？", "！")
            .filter { it.length > 1 }
            .map { it.lowercase() }
            .distinct()
            .take(20) // 限制关键词数量

        val matchedWords = contextWords.count { word ->
            output.lowercase().contains(word)
        }

        val matchRatio = if (contextWords.isNotEmpty()) {
            matchedWords.toDouble() / contextWords.size
        } else {
            0.5 // 如果没有有效的上下文词，给一个中等分数
        }

        // 检查回答是否包含查询中的关键词
        val queryWords = input.query.split(" ", "，", "。", "、", "：", "；", "？", "！")
            .filter { it.length > 1 }
            .map { it.lowercase() }

        val queryMatchedWords = queryWords.count { word ->
            output.lowercase().contains(word)
        }

        val queryMatchRatio = if (queryWords.isNotEmpty()) {
            queryMatchedWords.toDouble() / queryWords.size
        } else {
            0.5 // 如果没有有效的查询词，给一个中等分数
        }

        // 根据长度评估简洁性
        val conciseness = when {
            answerLength > 1000 -> 0.7 // 回答过长
            answerLength > 500 -> 0.9 // 适中长度
            answerLength > 100 -> 1.0 // 理想长度
            answerLength > 50 -> 0.8 // 较短
            else -> 0.5 // 太短，可能信息不足
        }

        // 计算最终分数，综合考虑上下文匹配率、查询匹配率和简洁性
        val finalScore = (matchRatio * 0.5 + queryMatchRatio * 0.3 + conciseness * 0.2)
            .coerceIn(0.0, 1.0)

        return MetricResult(
            score = finalScore,
            details = mapOf(
                "contextMatchRatio" to matchRatio,
                "queryMatchRatio" to queryMatchRatio,
                "conciseness" to conciseness,
                "answerLength" to answerLength,
                "matchedContextWords" to matchedWords,
                "totalContextWords" to contextWords.size,
                "matchedQueryWords" to queryMatchedWords,
                "totalQueryWords" to queryWords.size,
                "reason" to "基于上下文匹配率、查询匹配率和简洁性计算"
            )
        )
    }
}
