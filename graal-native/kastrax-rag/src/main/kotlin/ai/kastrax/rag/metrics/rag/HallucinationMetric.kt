package ai.kastrax.rag.metrics.rag

import ai.kastrax.rag.llm.LlmClient
import ai.kastrax.rag.metrics.MetricResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 幻觉检测指标，评估生成的回答中是否包含不在上下文中的信息（幻觉）。
 *
 * @property llmClient LLM 客户端，用于 LLM 辅助评估
 * @property systemPrompt 系统提示，用于 LLM 评估
 */
class HallucinationMetric(
    llmClient: LlmClient? = null,
    private val systemPrompt: String = DEFAULT_SYSTEM_PROMPT
) : RagMetric(llmClient) {

    companion object {
        private const val DEFAULT_SYSTEM_PROMPT = """
            你是一个专业的幻觉检测专家。你的任务是评估生成的回答中是否包含不在提供的上下文中的信息（幻觉）。
            
            评估标准：
            1. 事实一致性：回答中的事实是否与上下文中的事实一致
            2. 信息来源：回答中的信息是否来自上下文，而不是模型的先验知识
            3. 推断合理性：回答中的推断是否基于上下文中的信息，且合理
            4. 幻觉程度：回答中包含幻觉的程度
            
            请根据以上标准，给出一个 0-1 之间的分数，其中：
            - 0 表示严重幻觉，回答中大部分信息都不在上下文中
            - 0.25 表示明显幻觉，回答中包含多处不在上下文中的信息
            - 0.5 表示中等幻觉，回答中有一些不在上下文中的信息
            - 0.75 表示轻微幻觉，回答中可能有少量不在上下文中的信息
            - 1 表示无幻觉，回答中的所有信息都来自上下文
            
            请确保你的评估是客观的，并提供详细的理由。同时，请列出回答中的幻觉内容（如果有）。
        """
    }

    /**
     * 计算幻觉检测指标。
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
        logger.debug { "Calculating hallucination for query: ${input.query}" }

        // 如果没有 LLM 客户端，使用基于规则的评估
        if (llmClient == null) {
            return calculateRuleBasedHallucination(input, output)
        }

        try {
            // 构建评估提示
            val prompt = buildPrompt(input, output)

            // 使用 LLM 生成评估结果
            val llmResponse = llmClient.generate(systemPrompt, prompt)
            
            // 从 LLM 的输出中提取分数
            val score = extractScore(llmResponse)
            
            // 从 LLM 的输出中提取幻觉内容
            val hallucinations = extractHallucinations(llmResponse)

            return MetricResult(
                score = score,
                details = mapOf(
                    "query" to input.query,
                    "context" to input.context,
                    "answer" to output,
                    "llmResponse" to llmResponse,
                    "hallucinations" to hallucinations
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error evaluating hallucination with LLM" }
            
            // 出错时回退到基于规则的评估
            return calculateRuleBasedHallucination(input, output)
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
            append("请评估这个回答中是否包含不在上下文中的信息（幻觉），并给出一个 0-1 之间的分数。")
            append("请详细解释你的评分理由，并列出回答中的幻觉内容（如果有）。")
            append("格式如下：\n")
            append("分数：[0-1之间的数字]\n")
            append("理由：[详细解释]\n")
            append("幻觉内容：[列出回答中的幻觉内容，如果没有则写"无"]\n")
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
                    llmResponse.contains("无幻觉") -> 1.0
                    llmResponse.contains("轻微幻觉") -> 0.75
                    llmResponse.contains("中等幻觉") -> 0.5
                    llmResponse.contains("明显幻觉") -> 0.25
                    llmResponse.contains("严重幻觉") -> 0.0
                    else -> 0.5 // 默认中等分数
                }
            }
    }

    /**
     * 从 LLM 的输出中提取幻觉内容。
     *
     * @param llmResponse LLM 的输出
     * @return 提取的幻觉内容列表
     */
    private fun extractHallucinations(llmResponse: String): List<String> {
        // 尝试从响应中提取幻觉内容
        val hallucinationRegex = "幻觉内容：(.+)".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matchResult = hallucinationRegex.find(llmResponse)
        
        val hallucinationText = matchResult?.groupValues?.get(1)?.trim() ?: ""
        
        // 如果幻觉内容为"无"，返回空列表
        if (hallucinationText.equals("无", ignoreCase = true)) {
            return emptyList()
        }
        
        // 将幻觉内容拆分为列表
        return hallucinationText.split("\n", "；", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.equals("无", ignoreCase = true) }
    }

    /**
     * 基于规则计算幻觉检测。
     *
     * @param input RAG 评估输入
     * @param output 生成的回答
     * @return 评估结果
     */
    private fun calculateRuleBasedHallucination(input: RagEvaluationInput, output: String): MetricResult {
        // 如果没有回答或上下文，返回中等分数
        if (output.isBlank() || input.context.isBlank()) {
            return MetricResult(
                score = 0.5,
                details = mapOf(
                    "reason" to "回答或上下文为空，无法评估幻觉"
                )
            )
        }

        // 将上下文和回答分词
        val contextWords = input.context.split(" ", "，", "。", "、", "：", "；", "？", "！")
            .filter { it.length > 1 }
            .map { it.lowercase() }
            .distinct()
        
        val answerWords = output.split(" ", "，", "。", "、", "：", "；", "？", "！")
            .filter { it.length > 1 }
            .map { it.lowercase() }
            .distinct()
        
        // 计算回答中有多少词不在上下文中
        val hallucinatedWords = answerWords.filter { word ->
            !contextWords.any { it.contains(word) || word.contains(it) }
        }
        
        val hallucinationRatio = if (answerWords.isNotEmpty()) {
            hallucinatedWords.size.toDouble() / answerWords.size
        } else {
            0.0
        }
        
        // 根据幻觉比例计算分数（幻觉越多，分数越低）
        val score = (1.0 - hallucinationRatio).coerceIn(0.0, 1.0)
        
        return MetricResult(
            score = score,
            details = mapOf(
                "hallucinationRatio" to hallucinationRatio,
                "hallucinatedWords" to hallucinatedWords,
                "totalAnswerWords" to answerWords.size,
                "reason" to "基于回答中不在上下文中的词的比例计算"
            )
        )
    }
}
