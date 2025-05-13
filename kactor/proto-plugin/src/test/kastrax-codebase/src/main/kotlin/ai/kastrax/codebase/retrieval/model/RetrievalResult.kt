package ai.kastrax.codebase.retrieval.model

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.memory.SemanticMemory

/**
 * 检索特征
 *
 * @property name 特征名称
 * @property weight 特征权重
 * @property value 特征值
 */
data class RetrievalFeature(
    val name: String,
    val weight: Double,
    val value: Double
)

/**
 * 检索结果
 *
 * @property element 代码元素
 * @property score 分数
 * @property features 特征列表
 * @property explanation 解释
 */
data class RetrievalResult(
    val element: CodeElement,
    val score: Double,
    val features: List<RetrievalFeature> = emptyList(),
    val explanation: String? = null
)

/**
 * 记忆检索结果
 *
 * @property memory 语义记忆
 * @property score 分数
 * @property features 特征列表
 * @property explanation 解释
 */
data class MemoryRetrievalResult(
    val memory: SemanticMemory,
    val score: Double,
    val features: List<RetrievalFeature> = emptyList(),
    val explanation: String? = null
)

/**
 * 混合检索结果
 *
 * @property element 代码元素
 * @property vectorScore 向量分数
 * @property keywordScore 关键词分数
 * @property combinedScore 组合分数
 * @property source 来源
 */
data class HybridRetrievalResult(
    val element: CodeElement,
    val vectorScore: Float,
    val keywordScore: Float,
    val combinedScore: Float,
    val source: RetrievalSource
) {
    /**
     * 转换为通用的 RetrievalResult
     *
     * @return RetrievalResult 实例
     */
    fun toRetrievalResult(): RetrievalResult {
        return RetrievalResult(
            element = element,
            score = combinedScore.toDouble(),
            explanation = "Vector score: $vectorScore, Keyword score: $keywordScore"
        )
    }
}

/**
 * 检索来源
 */
enum class RetrievalSource {
    /**
     * 向量检索
     */
    VECTOR,

    /**
     * 关键词检索
     */
    KEYWORD,

    /**
     * 混合检索
     */
    HYBRID
}
