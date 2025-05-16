package ai.kastrax.code.model

/**
 * 测试生成请求
 *
 * @property code 代码
 * @property language 编程语言
 * @property framework 测试框架
 * @property coverage 覆盖率要求
 */
data class TestGenerationRequest(
    val code: String,
    val language: String,
    val framework: String,
    val coverage: Double = 0.8
)

/**
 * 测试生成结果
 *
 * @property id 结果ID
 * @property testCode 测试代码
 * @property explanation 解释
 * @property framework 测试框架
 * @property coverage 覆盖率要求
 */
data class TestGenerationResult(
    val id: String,
    val testCode: String,
    val explanation: String,
    val framework: String,
    val coverage: Double
)
