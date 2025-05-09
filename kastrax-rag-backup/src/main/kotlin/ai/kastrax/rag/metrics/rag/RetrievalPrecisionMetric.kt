package ai.kastrax.rag.metrics.rag

import ai.kastrax.rag.llm.LlmClient
import ai.kastrax.rag.metrics.MetricResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val logger = KotlinLogging.logger {}

/**
 * 检索精确度指标，评估检索结果与查询的相关性。
 *
 * @property llmClient LLM 客户端，用于 LLM 辅助评估
 * @property systemPrompt 系统提示，用于 LLM 评估
 */
class RetrievalPrecisionMetric(
    llmClient: LlmClient? = null,
    private val systemPrompt: String = DEFAULT_SYSTEM_PROMPT
) : RagMetric(llmClient) {

    companion object {
        private const val DEFAULT_SYSTEM_PROMPT = """
            你是一个专业的检索系统评估专家。你的任务是评估检索结果与用户查询的相关性。

            评估标准：
            1. 相关性：检索结果是否与查询相关
            2. 完整性：检索结果是否包含回答查询所需的信息
            3. 精确性：检索结果是否精确匹配查询的需求
            4. 冗余度：检索结果中是否存在冗余信息

            请根据以上标准，给出一个 0-1 之间的分数，其中：
            - 0 表示完全不相关或无用
            - 0.25 表示略微相关但不足以回答查询
            - 0.5 表示部分相关，包含一些有用信息
            - 0.75 表示相关且有用，但可能不完整
            - 1 表示高度相关，完全满足查询需求

            请确保你的评估是客观的，并提供详细的理由。
        """
    }

    /**
     * 计算检索精确度指标。
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
    ): MetricResult = coroutineScope {
        logger.debug { "Calculating retrieval precision for query: ${input.query}" }

        // 如果没有 LLM 客户端，使用基于规则的评估
        if (llmClient == null) {
            return@coroutineScope calculateRuleBasedPrecision(input)
        }

        try {
            // 构建评估提示
            val prompt = buildPrompt(input)

            // 使用 LLM 生成评估结果
            val llmResponse = llmClient.generate(systemPrompt, prompt)

            // 从 LLM 的输出中提取分数
            val score = extractScore(llmResponse)

            MetricResult(
                score = score,
                details = mapOf(
                    "query" to input.query,
                    "retrievalResults" to input.retrievalResults,
                    "llmResponse" to llmResponse
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error evaluating retrieval precision with LLM" }

            // 出错时回退到基于规则的评估
            calculateRuleBasedPrecision(input)
        }
    }

    /**
     * 构建评估提示。
     *
     * @param input RAG 评估输入
     * @return 评估提示
     */
    private fun buildPrompt(input: RagEvaluationInput): String {
        return buildString {
            append("用户查询: ${input.query}\n\n")
            append("检索结果:\n")

            input.retrievalResults.forEachIndexed { index, result ->
                append("结果 ${index + 1}:\n")
                append("内容: ${result.content}\n")
                append("分数: ${result.score}\n\n")
            }

            append("请评估这些检索结果与用户查询的相关性，并给出一个 0-1 之间的分数。")
            append("请详细解释你的评分理由，并指出检索结果的优点和不足。")
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
                    llmResponse.contains("高度相关") || llmResponse.contains("完全满足") -> 1.0
                    llmResponse.contains("相关且有用") -> 0.75
                    llmResponse.contains("部分相关") -> 0.5
                    llmResponse.contains("略微相关") -> 0.25
                    llmResponse.contains("完全不相关") || llmResponse.contains("无用") -> 0.0
                    else -> 0.5 // 默认中等分数
                }
            }
    }

    /**
     * 基于规则计算检索精确度。
     *
     * @param input RAG 评估输入
     * @return 评估结果
     */
    private fun calculateRuleBasedPrecision(input: RagEvaluationInput): MetricResult {
        // 如果没有检索结果，返回 0 分
        if (input.retrievalResults.isEmpty()) {
            return MetricResult(
                score = 0.0,
                details = mapOf(
                    "reason" to "没有检索结果"
                )
            )
        }

        // 计算平均分数
        val avgScore = input.retrievalResults.map { it.score }.average()

        // 根据平均分数调整最终分数
        val finalScore = when {
            avgScore > 0.8 -> 0.9 // 高相似度
            avgScore > 0.6 -> 0.7 // 中高相似度
            avgScore > 0.4 -> 0.5 // 中等相似度
            avgScore > 0.2 -> 0.3 // 低相似度
            else -> 0.1 // 极低相似度
        }

        return MetricResult(
            score = finalScore,
            details = mapOf(
                "avgScore" to avgScore,
                "numResults" to input.retrievalResults.size,
                "reason" to "基于检索结果的平均相似度分数计算"
            )
        )
    }
}
