package ai.kastrax.evals.metrics.llm

import ai.kastrax.evals.metrics.Metric
import ai.kastrax.evals.metrics.MetricCategory
import ai.kastrax.evals.metrics.MetricResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 回答正确性评估指标，使用 LLM 评估回答的正确性。
 *
 * @param llmClient LLM 客户端，用于生成评估结果
 * @param systemPrompt 系统提示，用于指导 LLM 进行评估
 * @param scoreExtractor 分数提取器，用于从 LLM 的输出中提取分数
 */
class CorrectnessMetric(
    private val llmClient: LlmClient,
    private val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    private val scoreExtractor: ScoreExtractor = DefaultScoreExtractor()
) : Metric<String, String> {
    
    override val name: String = "Correctness"
    override val description: String = "Evaluates the correctness of the answer using LLM"
    override val category: MetricCategory = MetricCategory.ACCURACY
    
    override suspend fun calculate(
        input: String,
        output: String,
        options: Map<String, Any?>
    ): MetricResult {
        // 从选项中获取参考答案
        val reference = options["reference"] as? String
        
        // 构建评估提示
        val prompt = buildPrompt(input, output, reference)
        
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
                    "reference" to reference,
                    "llmResponse" to llmResponse
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error evaluating correctness with LLM" }
            
            return MetricResult(
                score = 0.0,
                details = mapOf(
                    "error" to e.message,
                    "input" to input,
                    "output" to output,
                    "reference" to reference
                )
            )
        }
    }
    
    /**
     * 构建评估提示。
     *
     * @param input 输入文本
     * @param output 输出文本
     * @param reference 参考答案
     * @return 评估提示
     */
    private fun buildPrompt(
        input: String,
        output: String,
        reference: String?
    ): String {
        return if (reference != null) {
            """
            请评估以下回答的正确性。
            
            问题：$input
            
            回答：$output
            
            参考答案：$reference
            
            请根据回答的正确性给出 0 到 10 的评分，其中 0 表示完全错误，10 表示完全正确。
            请首先分析回答的正确性，然后给出评分。
            
            评分：
            """.trimIndent()
        } else {
            """
            请评估以下回答的正确性。
            
            问题：$input
            
            回答：$output
            
            请根据回答的正确性给出 0 到 10 的评分，其中 0 表示完全错误，10 表示完全正确。
            请首先分析回答的正确性，然后给出评分。
            
            评分：
            """.trimIndent()
        }
    }
    
    companion object {
        /**
         * 默认系统提示。
         */
        const val DEFAULT_SYSTEM_PROMPT = """
            你是一个专业的回答评估专家。你的任务是评估回答的正确性。
            请根据问题和回答，给出客观、公正的评估。
            如果提供了参考答案，请将回答与参考答案进行比较。
            如果没有提供参考答案，请根据你的知识和理解评估回答的正确性。
            请注意，回答可能部分正确，也可能包含错误信息。
        """
    }
}

/**
 * LLM 客户端接口，用于生成评估结果。
 */
interface LlmClient {
    /**
     * 生成评估结果。
     *
     * @param systemPrompt 系统提示
     * @param userPrompt 用户提示
     * @return LLM 的输出
     */
    suspend fun generate(systemPrompt: String, userPrompt: String): String
}

/**
 * 分数提取器接口，用于从 LLM 的输出中提取分数。
 */
interface ScoreExtractor {
    /**
     * 从 LLM 的输出中提取分数。
     *
     * @param llmResponse LLM 的输出
     * @return 提取的分数，范围为 0.0 到 1.0
     */
    fun extractScore(llmResponse: String): Double
}

/**
 * 默认分数提取器，使用正则表达式从 LLM 的输出中提取分数。
 */
class DefaultScoreExtractor : ScoreExtractor {
    override fun extractScore(llmResponse: String): Double {
        // 使用正则表达式提取分数
        val scoreRegex = Regex("评分：?\\s*(\\d+(?:\\.\\d+)?)|得分：?\\s*(\\d+(?:\\.\\d+)?)|分数：?\\s*(\\d+(?:\\.\\d+)?)|\\b(\\d+(?:\\.\\d+)?)\\s*分\\b|\\b(\\d+)\\s*[/／]\\s*10\\b")
        val matchResult = scoreRegex.find(llmResponse)
        
        if (matchResult != null) {
            // 提取匹配的分数
            val scoreStr = matchResult.groupValues.drop(1).firstOrNull { it.isNotEmpty() }
            
            if (scoreStr != null) {
                try {
                    // 将分数转换为 Double
                    val score = scoreStr.toDouble()
                    
                    // 将分数归一化到 0.0 到 1.0 的范围
                    return score / 10.0
                } catch (e: NumberFormatException) {
                    logger.error { "Error parsing score: $scoreStr" }
                }
            }
        }
        
        // 如果无法提取分数，则返回 0.5（中等分数）
        return 0.5
    }
}

/**
 * 创建回答正确性评估指标。
 *
 * @param llmClient LLM 客户端，用于生成评估结果
 * @param systemPrompt 系统提示，用于指导 LLM 进行评估
 * @param scoreExtractor 分数提取器，用于从 LLM 的输出中提取分数
 * @return 回答正确性评估指标
 */
fun correctnessMetric(
    llmClient: LlmClient,
    systemPrompt: String = CorrectnessMetric.DEFAULT_SYSTEM_PROMPT,
    scoreExtractor: ScoreExtractor = DefaultScoreExtractor()
): CorrectnessMetric {
    return CorrectnessMetric(llmClient, systemPrompt, scoreExtractor)
}
