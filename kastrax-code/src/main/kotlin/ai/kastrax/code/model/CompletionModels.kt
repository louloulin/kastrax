package ai.kastrax.code.model

/**
 * 补全请求
 *
 * @property code 当前代码
 * @property prefix 前缀
 * @property language 编程语言
 * @property maxCompletions 最大补全数量
 */
data class CompletionRequest(
    val code: String,
    val prefix: String,
    val language: String,
    val maxCompletions: Int = 5
)

/**
 * 补全结果
 *
 * @property id 结果ID
 * @property text 补全文本
 * @property displayText 显示文本
 * @property confidence 置信度
 */
data class CompletionResult(
    val id: String,
    val text: String,
    val displayText: String,
    val confidence: Double
)
