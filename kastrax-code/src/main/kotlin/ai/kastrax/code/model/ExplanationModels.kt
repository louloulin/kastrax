package ai.kastrax.code.model

/**
 * 解释请求
 *
 * @property code 代码
 * @property language 编程语言
 * @property detailLevel 详细程度
 */
data class ExplanationRequest(
    val code: String,
    val language: String,
    val detailLevel: DetailLevel = DetailLevel.NORMAL
)

/**
 * 解释结果
 *
 * @property id 结果ID
 * @property explanation 解释
 * @property detailLevel 详细程度
 */
data class ExplanationResult(
    val id: String,
    val explanation: String,
    val detailLevel: DetailLevel
)


