package ai.kastrax.evals.metrics.rag

import ai.kastrax.core.llm.LlmClient
import ai.kastrax.evals.metrics.Metric
import ai.kastrax.evals.metrics.MetricResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * RAG 评估指标的基类，用于评估 RAG 系统的质量。
 *
 * @property llmClient LLM 客户端，用于 LLM 辅助评估
 */
abstract class RagMetric(
    protected val llmClient: LlmClient? = null
) : Metric<RagEvaluationInput, String> {

    /**
     * 计算 RAG 评估指标。
     *
     * @param input RAG 评估输入，包含查询、检索结果和上下文
     * @param output 生成的回答
     * @param options 评估选项
     * @return 评估结果
     */
    override suspend fun calculate(
        input: RagEvaluationInput,
        output: String,
        options: Map<String, Any?>
    ): MetricResult {
        logger.debug { "Calculating RAG metric for query: ${input.query}" }
        return calculateRagMetric(input, output, options)
    }

    /**
     * 计算 RAG 评估指标的具体实现。
     *
     * @param input RAG 评估输入，包含查询、检索结果和上下文
     * @param output 生成的回答
     * @param options 评估选项
     * @return 评估结果
     */
    protected abstract suspend fun calculateRagMetric(
        input: RagEvaluationInput,
        output: String,
        options: Map<String, Any?>
    ): MetricResult
}

/**
 * RAG 评估输入，包含查询、检索结果和上下文。
 *
 * @property query 用户查询
 * @property retrievalResults 检索结果
 * @property context 生成的上下文
 * @property groundTruth 参考答案（可选）
 */
data class RagEvaluationInput(
    val query: String,
    val retrievalResults: List<RetrievalResult>,
    val context: String,
    val groundTruth: String? = null
)

/**
 * 检索结果，包含文档内容、分数和元数据。
 *
 * @property content 文档内容
 * @property score 相似度分数
 * @property metadata 元数据
 */
data class RetrievalResult(
    val content: String,
    val score: Double,
    val metadata: Map<String, Any?> = emptyMap()
)
