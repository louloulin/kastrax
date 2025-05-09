package ai.kastrax.rag.metrics.rag

import ai.kastrax.rag.llm.LlmClient
import ai.kastrax.rag.metrics.MetricResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 幻觉检测指标，评估生成的回答中是否包含与上下文不符的信息。
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
            你是一个专业的幻觉检测专家。你的任务是评估生成的回答中是否包含与上下文不符的信息（幻觉）。
            
            评估标准：
            1. 事实一致性：回答中的事实是否与上下文中的信息一致
            2. 信息来源：回答中的信息是否来自上下文，而不是模型的先验知识
            3. 推断合理性：回答中的推断是否基于上下文中的信息，且合理
            4. 不确定性表达：对于上下文中没有的信息，回答是否适当表达了不确定性
            
            请根据以上标准，给出一个 0-1 之间的分数，其中：
            - 0 表示没有幻觉，回答完全基于上下文
            - 0.25 表示轻微幻觉，有少量不确定信息但基本符合上下文
            - 0.5 表示中等幻觉，有明显不符合上下文的信息
            - 0.75 表示严重幻觉，大部分信息不符合上下文
            - 1 表示完全幻觉，回答与上下文完全无关
            
            请确保你的评估是客观的，并提供详细的理由。
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
            
            // 如果有参考答案，也包含在提示中
            if (input.groundTruth != null) {
                append("参考答案:\n${input.groundTruth}\n\n")
            }
            
            append("请评估这个回答中是否包含与上下文不符的信息（幻觉），并给出一个 0-1 之间的分数。")
            append("请详细解释你的评分理由，并指出回答中的幻觉内容（如果有）。")
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
                    llmResponse.contains("完全幻觉") || llmResponse.contains("与上下文完全无关") -> 1.0
                    llmResponse.contains("严重幻觉") -> 0.75
                    llmResponse.contains("中等幻觉") || llmResponse.contains("明显不符合") -> 0.5
                    llmResponse.contains("轻微幻觉") -> 0.25
                    llmResponse.contains("没有幻觉") || llmResponse.contains("完全基于上下文") -> 0.0
                    else -> 0.5 // 默认中等分数
                }
            }
    }

    /**
     * 基于规则计算幻觉检测。
     *
     * @param input RAG 评估输入
     * @param output 生成的回答
     * @return 评估结果
     */
    private fun calculateRuleBasedHallucination(input: RagEvaluationInput, output: String): MetricResult {
        // 如果没有回答，返回 0 分（没有幻觉）
        if (output.isBlank()) {
            return MetricResult(
                score = 0.0,
                details = mapOf(
                    "reason" to "回答为空，无法评估幻觉"
                )
            )
        }

        // 从上下文中提取关键词和短语
        val contextKeywords = extractKeywords(input.context)
        
        // 从回答中提取关键词和短语
        val outputKeywords = extractKeywords(output)
        
        // 计算回答中不在上下文中的关键词比例
        val nonContextKeywords = outputKeywords.filter { !contextKeywords.contains(it) }
        val hallucinationRatio = if (outputKeywords.isNotEmpty()) {
            nonContextKeywords.size.toDouble() / outputKeywords.size
        } else {
            0.0
        }
        
        // 根据比例计算幻觉分数
        val score = when {
            hallucinationRatio > 0.8 -> 1.0 // 完全幻觉
            hallucinationRatio > 0.6 -> 0.75 // 严重幻觉
            hallucinationRatio > 0.4 -> 0.5 // 中等幻觉
            hallucinationRatio > 0.2 -> 0.25 // 轻微幻觉
            else -> 0.0 // 没有幻觉
        }
        
        return MetricResult(
            score = score,
            details = mapOf(
                "hallucinationRatio" to hallucinationRatio,
                "nonContextKeywords" to nonContextKeywords,
                "totalOutputKeywords" to outputKeywords.size,
                "reason" to "基于回答中不在上下文中的关键词比例计算"
            )
        )
    }
    
    /**
     * 从文本中提取关键词和短语。
     *
     * @param text 文本
     * @return 关键词和短语列表
     */
    private fun extractKeywords(text: String): List<String> {
        // 分词并过滤停用词和短词
        return text.split(" ", "，", "。", "、", "：", "；", "？", "！", "\n")
            .filter { it.length > 1 }
            .map { it.lowercase() }
            .distinct()
    }
}
