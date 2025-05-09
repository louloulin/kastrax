package ai.kastrax.rag.metrics

/**
 * 评估指标接口，用于评估 AI 系统的输出质量。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
interface Metric<I, O> {
    /**
     * 计算评估指标。
     *
     * @param input 输入数据
     * @param output AI 系统的输出
     * @param options 评估选项
     * @return 评估结果
     */
    suspend fun calculate(
        input: I,
        output: O,
        options: Map<String, Any?> = emptyMap()
    ): MetricResult
}

/**
 * 评估结果，包含分数和详细信息。
 *
 * @property score 评估分数，范围为 0.0 到 1.0
 * @property details 详细信息
 */
data class MetricResult(
    val score: Double,
    val details: Map<String, Any?> = emptyMap()
)
