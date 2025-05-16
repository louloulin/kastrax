package ai.kastrax.code.model

/**
 * 重构请求
 *
 * @property code 代码
 * @property instructions 指令
 * @property language 编程语言
 */
data class RefactoringRequest(
    val code: String,
    val instructions: String,
    val language: String
)

/**
 * 重构结果
 *
 * @property id 结果ID
 * @property originalCode 原始代码
 * @property refactoredCode 重构后的代码
 * @property explanation 解释
 */
data class RefactoringResult(
    val id: String,
    val originalCode: String,
    val refactoredCode: String,
    val explanation: String
)
