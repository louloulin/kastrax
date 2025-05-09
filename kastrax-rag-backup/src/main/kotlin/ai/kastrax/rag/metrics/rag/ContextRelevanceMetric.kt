package ai.kastrax.rag.metrics.rag

import ai.kastrax.rag.llm.LlmClient
import ai.kastrax.rag.metrics.MetricResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 上下文相关性指标，评估生成的上下文与查询的相关性。
 *
 * @property llmClient LLM 客户端，用于 LLM 辅助评估
 * @property systemPrompt 系统提示，用于 LLM 评估
 */
class ContextRelevanceMetric(
    llmClient: LlmClient? = null,
    private val systemPrompt: String = DEFAULT_SYSTEM_PROMPT
) : RagMetric(llmClient) {

    companion object {
        private const val DEFAULT_SYSTEM_PROMPT = """
            你是一个专业的上下文评估专家。你的任务是评估生成的上下文与用户查询的相关性。
            
            评估标准：
            1. 相关性：上下文是否与查询相关
            2. 完整性：上下文是否包含回答查询所需的所有信息
            3. 简洁性：上下文是否简洁，不包含无关信息
            4. 连贯性：上下文是否连贯，易于理解
            
            请根据以上标准，给出一个 0-1 之间的分数，其中：
            - 0 表示完全不相关或无用
            - 0.25 表示略微相关但信息不足
            - 0.5 表示部分相关，包含一些有用信息
            - 0.75 表示相关且信息充分，但可能包含一些无关内容
            - 1 表示高度相关，信息完整且简洁
            
            请确保你的评估是客观的，并提供详细的理由。
        """
    }

    /**
     * 计算上下文相关性指标。
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
        logger.debug { "Calculating context relevance for query: ${input.query}" }

        // 如果没有 LLM 客户端，使用基于规则的评估
        if (llmClient == null) {
            return calculateRuleBasedRelevance(input)
        }

        try {
            // 构建评估提示
            val prompt = buildPrompt(input)
            
            // 使用 LLM 生成评估结果
            val llmResponse = llmClient.generate(systemPrompt, prompt)
            
            // 从 LLM 的输出中提取分数
            val score = extractScore(llmResponse)
            
            return MetricResult(
                score = score,
                details = mapOf(
                    "query" to input.query,
                    "context" to input.context,
                    "llmResponse" to llmResponse
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error evaluating context relevance with LLM" }
            
            // 出错时回退到基于规则的评估
            return calculateRuleBasedRelevance(input)
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
            append("生成的上下文:\n${input.context}\n\n")
            append("请评估这个上下文与用户查询的相关性，并给出一个 0-1 之间的分数。")
            append("请详细解释你的评分理由，并指出上下文的优点和不足。")
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
                    llmResponse.contains("高度相关") || llmResponse.contains("信息完整") -> 1.0
                    llmResponse.contains("相关且信息充分") -> 0.75
                    llmResponse.contains("部分相关") -> 0.5
                    llmResponse.contains("略微相关") -> 0.25
                    llmResponse.contains("完全不相关") || llmResponse.contains("无用") -> 0.0
                    else -> 0.5 // 默认中等分数
                }
            }
    }

    /**
     * 基于规则计算上下文相关性。
     *
     * @param input RAG 评估输入
     * @return 评估结果
     */
    private fun calculateRuleBasedRelevance(input: RagEvaluationInput): MetricResult {
        // 如果没有上下文，返回 0 分
        if (input.context.isBlank()) {
            return MetricResult(
                score = 0.0,
                details = mapOf(
                    "reason" to "上下文为空"
                )
            )
        }

        // 计算上下文长度
        val contextLength = input.context.length
        
        // 检查上下文是否包含查询中的关键词
        val queryWords = input.query.split(" ", "，", "。", "、", "：", "；", "？", "！")
            .filter { it.length > 1 }
            .map { it.lowercase() }
        
        val matchedWords = queryWords.count { word ->
            input.context.lowercase().contains(word)
        }
        
        val matchRatio = if (queryWords.isNotEmpty()) {
            matchedWords.toDouble() / queryWords.size
        } else {
            0.5 // 如果没有有效的查询词，给一个中等分数
        }
        
        // 根据匹配率和上下文长度计算最终分数
        val lengthFactor = when {
            contextLength > 5000 -> 0.7 // 上下文过长，可能包含无关信息
            contextLength > 2000 -> 0.9 // 适中长度
            contextLength > 500 -> 1.0 // 理想长度
            contextLength > 100 -> 0.8 // 较短，可能信息不完整
            else -> 0.5 // 太短，信息可能不足
        }
        
        val finalScore = matchRatio * lengthFactor
        
        return MetricResult(
            score = finalScore,
            details = mapOf(
                "matchRatio" to matchRatio,
                "lengthFactor" to lengthFactor,
                "contextLength" to contextLength,
                "matchedWords" to matchedWords,
                "queryWords" to queryWords.size,
                "reason" to "基于查询词匹配率和上下文长度计算"
            )
        )
    }
}
