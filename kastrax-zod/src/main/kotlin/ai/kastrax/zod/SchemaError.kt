package ai.kastrax.zod

/**
 * 扩展 SchemaError 类的功能。
 */

/**
 * 将错误格式化为平面列表，便于日志记录。
 */
fun SchemaError.flatten(): List<FlattenedError> {
    return issues.map { issue ->
        FlattenedError(
            message = issue.message,
            path = issue.path.joinToString("."),
            code = issue.code
        )
    }
}

/**
 * 获取第一个错误消息。
 */
fun SchemaError.getMessage(): String {
    return issues.firstOrNull()?.message ?: "验证失败"
}

/**
 * 将错误转换为异常。
 */
fun SchemaError.toException(): SchemaValidationException {
    return SchemaValidationException(this)
}

/**
 * 表示扁平化的错误，用于日志记录。
 *
 * @property message 错误消息
 * @property path 错误路径
 * @property code 错误代码
 */
data class FlattenedError(
    val message: String,
    val path: String,
    val code: SchemaIssueCode
)

/**
 * 错误映射函数类型，用于自定义错误消息。
 */
typealias ErrorMapFn = (issue: SchemaIssue) -> String?

/**
 * 错误映射，用于自定义错误消息。
 */
object ErrorMap {
    /**
     * 默认错误映射。
     */
    val defaultErrorMap: ErrorMapFn = { issue ->
        when (issue.code) {
            SchemaIssueCode.INVALID_TYPE -> {
                val expected = issue.params["expected"]
                val received = issue.params["received"]
                "期望 $expected，收到 $received"
            }
            SchemaIssueCode.REQUIRED -> "必需字段"
            SchemaIssueCode.INVALID_LITERAL -> {
                val expected = issue.params["expected"]
                "无效的字面值，期望: $expected"
            }
            SchemaIssueCode.CUSTOM -> issue.message
            SchemaIssueCode.INVALID_UNION -> "无效的联合类型"
            SchemaIssueCode.INVALID_ENUM_VALUE -> {
                val options = issue.params["options"]
                "无效的枚举值，期望值之一: $options"
            }
            SchemaIssueCode.TOO_SMALL -> {
                val minimum = issue.params["minimum"]
                val type = issue.params["type"]
                val inclusive = issue.params["inclusive"] as? Boolean ?: true
                val suffix = if (type == "string") "个字符" else "个元素"
                if (inclusive) {
                    "$type 长度至少为 $minimum $suffix"
                } else {
                    "$type 长度必须大于 $minimum $suffix"
                }
            }
            SchemaIssueCode.TOO_BIG -> {
                val maximum = issue.params["maximum"]
                val type = issue.params["type"]
                val inclusive = issue.params["inclusive"] as? Boolean ?: true
                val suffix = if (type == "string") "个字符" else "个元素"
                if (inclusive) {
                    "$type 长度最多为 $maximum $suffix"
                } else {
                    "$type 长度必须小于 $maximum $suffix"
                }
            }
            SchemaIssueCode.INVALID_STRING -> {
                val validation = issue.params["validation"]
                "无效的字符串，验证失败: $validation"
            }
            SchemaIssueCode.INVALID_ARGUMENTS -> "无效的函数参数"
            SchemaIssueCode.INVALID_RETURN_TYPE -> "无效的返回类型"
            SchemaIssueCode.INVALID_DATE -> "无效的日期"
            SchemaIssueCode.INVALID_INTERSECTION_TYPES -> "无效的交叉类型"
            SchemaIssueCode.NOT_MULTIPLE_OF -> {
                val multipleOf = issue.params["multipleOf"]
                "数字必须是 $multipleOf 的倍数"
            }
            SchemaIssueCode.NOT_FINITE -> "数字必须是有限的"
            SchemaIssueCode.MISSING_REQUIRED_FIELD -> "缺少必填字段"
        }
    }

    /**
     * 当前错误映射。
     * 可以通过设置该属性来自定义错误消息。
     *
     * 例如: ErrorMap.errorMap = { issue -> "Custom message for ${issue.code}" }
     */
    var errorMap: ErrorMapFn = defaultErrorMap

    /**
     * 获取错误消息。
     *
     * @param issue 错误问题
     * @return 错误消息
     */
    fun getErrorMessage(issue: SchemaIssue): String {
        return errorMap(issue) ?: defaultErrorMap(issue) ?: "验证失败"
    }
}
