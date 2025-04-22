package ai.kastrax.evals.metrics.llm

import ai.kastrax.evals.metrics.Metric
import ai.kastrax.evals.metrics.MetricCategory
import ai.kastrax.evals.metrics.MetricResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 回答有用性评估指标，使用 LLM 评估回答的有用性。
 *
 * @param llmClient LLM 客户端，用于生成评估结果
 * @param systemPrompt 系统提示，用于指导 LLM 进行评估
 * @param scoreExtractor 分数提取器，用于从 LLM 的输出中提取分数
 */
class UsefulnessMetric(
    private val llmClient: LlmClient,
    private val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    private val scoreExtractor: ScoreExtractor = DefaultScoreExtractor()
) : Metric<String, String> {
    
    override val name: String = "Usefulness"
    override val description: String = "Evaluates the usefulness of the answer using LLM"
    override val category: MetricCategory = MetricCategory.QUALITY
    
    override suspend fun calculate(
        input: String,
        output: String,
        options: Map<String, Any?>
    ): MetricResult {
        // 构建评估提示
        val prompt = buildPrompt(input, output)
        
        try {
            // 使用 LLM 生成评估结果
            val llmResponse = llmClient.generate(systemPrompt, prompt)
            
            // 从 LLM 的输出中提取分数
            val score = scoreExtractor.extractScore(llmResponse)
            
            return MetricResult(
                score = score,
                details = mapOf(
                    "input" to input,
                    "output" to output,
                    "llmResponse" to llmResponse
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error evaluating usefulness with LLM" }
            
            return MetricResult(
                score = 0.0,
                details = mapOf(
                    "error" to e.message,
                    "input" to input,
                    "output" to output
                )
            )
        }
    }
    
    /**
     * 构建评估提示。
     *
     * @param input 输入文本
     * @param output 输出文本
     * @return 评估提示
     */
    private fun buildPrompt(
        input: String,
        output: String
    ): String {
        return """
        请评估以下回答对用户的有用性。
        
        问题：$input
        
        回答：$output
        
        请根据回答的有用性给出 0 到 10 的评分，其中 0 表示完全没有用，10 表示非常有用。
        评估有用性时，请考虑以下因素：
        1. 回答是否直接解决了用户的问题
        2. 回答是否提供了足够的信息
        3. 回答是否清晰易懂
        4. 回答是否提供了实用的建议或指导
        5. 回答是否帮助用户更好地理解问题
        
        请首先分析回答的有用性，然后给出评分。
        
        评分：
        """.trimIndent()
    }
    
    companion object {
        /**
         * 默认系统提示。
         */
        const val DEFAULT_SYSTEM_PROMPT = """
            你是一个专业的回答评估专家。你的任务是评估回答对用户的有用性。
            请根据问题和回答，给出客观、公正的评估。
            评估有用性时，请考虑回答是否解决了用户的问题，是否提供了足够的信息，是否清晰易懂，是否提供了实用的建议或指导，以及是否帮助用户更好地理解问题。
            请注意，有用性与正确性不同，一个回答可能是正确的，但如果它没有解决用户的实际需求，那么它的有用性就不高。
        """
    }
}

/**
 * 创建回答有用性评估指标。
 *
 * @param llmClient LLM 客户端，用于生成评估结果
 * @param systemPrompt 系统提示，用于指导 LLM 进行评估
 * @param scoreExtractor 分数提取器，用于从 LLM 的输出中提取分数
 * @return 回答有用性评估指标
 */
fun usefulnessMetric(
    llmClient: LlmClient,
    systemPrompt: String = UsefulnessMetric.DEFAULT_SYSTEM_PROMPT,
    scoreExtractor: ScoreExtractor = DefaultScoreExtractor()
): UsefulnessMetric {
    return UsefulnessMetric(llmClient, systemPrompt, scoreExtractor)
}
