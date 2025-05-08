package ai.kastrax.examples.tools

import kotlinx.serialization.Serializable

/**
 * 用户数据类
 */
@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String,
    val age: Int
)

/**
 * 高级用户数据类
 */
@Serializable
data class AdvancedUser(
    val id: String,
    val name: String,
    val email: String,
    val age: Int,
    val tags: List<String> = emptyList(),
    val createdAt: String = ""
)

/**
 * 用户搜索结果数据类
 */
@Serializable
data class UserSearchResult(
    val users: List<User>,
    val total: Int,
    val searchTime: Double
)

/**
 * 高级用户搜索结果数据类
 */
@Serializable
data class AdvancedUserSearchResult(
    val users: List<AdvancedUser>,
    val total: Int,
    val searchTime: Double
)

/**
 * 用户验证结果数据类
 */
@Serializable
data class UserValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
)

/**
 * 计算器输入数据类
 */
@Serializable
data class CalculatorInput(
    val expression: String
)

/**
 * 计算器输出数据类
 */
@Serializable
data class CalculatorOutput(
    val result: Double,
    val error: String? = null
)
