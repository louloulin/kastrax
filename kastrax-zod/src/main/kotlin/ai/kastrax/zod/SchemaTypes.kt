package ai.kastrax.zod

import java.util.regex.Pattern

/**
 * 字符串模式，用于验证字符串类型的数据。
 *
 * @property minLength 最小长度
 * @property maxLength 最大长度
 * @property pattern 正则表达式模式
 * @property email 是否验证为电子邮件
 * @property url 是否验证为URL
 * @property uuid 是否验证为UUID
 * @property cuid 是否验证为CUID
 * @property cuid2 是否验证为CUID2
 * @property ulid 是否验证为ULID
 * @property datetime 是否验证为ISO日期时间
 * @property date 是否验证为ISO日期
 * @property time 是否验证为ISO时间
 * @property ip 是否验证为IP地址
 */
class StringSchema(
    var minLength: Int? = null,
    var maxLength: Int? = null,
    var pattern: Pattern? = null,
    var email: Boolean = false,
    var url: Boolean = false,
    var uuid: Boolean = false,
    var cuid: Boolean = false,
    var cuid2: Boolean = false,
    var ulid: Boolean = false,
    var datetime: Boolean = false,
    var date: Boolean = false,
    var time: Boolean = false,
    var ip: Boolean = false
) : BaseSchema<String, String>() {

    override fun _parse(data: String): SchemaResult<String> {
        if (data !is String) {
            return SchemaResult.Failure(
                SchemaError(
                    listOf(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_TYPE,
                            message = "期望字符串，收到 ${data::class.simpleName}",
                            path = emptyList()
                        )
                    )
                )
            )
        }

        val issues = mutableListOf<SchemaIssue>()

        // 验证最小长度
        minLength?.let {
            if (data.length < it) {
                issues.add(
                    SchemaIssue(
                        code = SchemaIssueCode.TOO_SMALL,
                        message = "字符串长度至少为 $it 个字符",
                        path = emptyList(),
                        params = mapOf("minimum" to it, "type" to "string", "inclusive" to true)
                    )
                )
            }
        }

        // 验证最大长度
        maxLength?.let {
            if (data.length > it) {
                issues.add(
                    SchemaIssue(
                        code = SchemaIssueCode.TOO_BIG,
                        message = "字符串长度最多为 $it 个字符",
                        path = emptyList(),
                        params = mapOf("maximum" to it, "type" to "string", "inclusive" to true)
                    )
                )
            }
        }

        // 验证正则表达式
        pattern?.let {
            if (!it.matcher(data).matches()) {
                issues.add(
                    SchemaIssue(
                        code = SchemaIssueCode.INVALID_STRING,
                        message = "字符串不匹配模式",
                        path = emptyList(),
                        params = mapOf("pattern" to it.pattern())
                    )
                )
            }
        }

        // 验证电子邮件
        if (email && !isValidEmail(data)) {
            issues.add(
                SchemaIssue(
                    code = SchemaIssueCode.INVALID_STRING,
                    message = "无效的电子邮件",
                    path = emptyList(),
                    params = mapOf("validation" to "email")
                )
            )
        }

        // 验证URL
        if (url && !isValidUrl(data)) {
            issues.add(
                SchemaIssue(
                    code = SchemaIssueCode.INVALID_STRING,
                    message = "无效的URL",
                    path = emptyList(),
                    params = mapOf("validation" to "url")
                )
            )
        }

        // 验证UUID
        if (uuid && !isValidUuid(data)) {
            issues.add(
                SchemaIssue(
                    code = SchemaIssueCode.INVALID_STRING,
                    message = "无效的UUID",
                    path = emptyList(),
                    params = mapOf("validation" to "uuid")
                )
            )
        }

        // 验证CUID
        if (cuid && !isValidCuid(data)) {
            issues.add(
                SchemaIssue(
                    code = SchemaIssueCode.INVALID_STRING,
                    message = "无效的CUID",
                    path = emptyList(),
                    params = mapOf("validation" to "cuid")
                )
            )
        }

        // 验证CUID2
        if (cuid2 && !isValidCuid2(data)) {
            issues.add(
                SchemaIssue(
                    code = SchemaIssueCode.INVALID_STRING,
                    message = "无效的CUID2",
                    path = emptyList(),
                    params = mapOf("validation" to "cuid2")
                )
            )
        }

        // 验证ULID
        if (ulid && !isValidUlid(data)) {
            issues.add(
                SchemaIssue(
                    code = SchemaIssueCode.INVALID_STRING,
                    message = "无效的ULID",
                    path = emptyList(),
                    params = mapOf("validation" to "ulid")
                )
            )
        }

        // 验证ISO日期时间
        if (datetime && !isValidDatetime(data)) {
            issues.add(
                SchemaIssue(
                    code = SchemaIssueCode.INVALID_STRING,
                    message = "无效的ISO日期时间",
                    path = emptyList(),
                    params = mapOf("validation" to "datetime")
                )
            )
        }

        // 验证ISO日期
        if (date && !isValidDate(data)) {
            issues.add(
                SchemaIssue(
                    code = SchemaIssueCode.INVALID_STRING,
                    message = "无效的ISO日期",
                    path = emptyList(),
                    params = mapOf("validation" to "date")
                )
            )
        }

        // 验证ISO时间
        if (time && !isValidTime(data)) {
            issues.add(
                SchemaIssue(
                    code = SchemaIssueCode.INVALID_STRING,
                    message = "无效的ISO时间",
                    path = emptyList(),
                    params = mapOf("validation" to "time")
                )
            )
        }

        // 验证IP地址
        if (ip && !isValidIp(data)) {
            issues.add(
                SchemaIssue(
                    code = SchemaIssueCode.INVALID_STRING,
                    message = "无效的IP地址",
                    path = emptyList(),
                    params = mapOf("validation" to "ip")
                )
            )
        }

        return if (issues.isEmpty()) {
            SchemaResult.Success(data)
        } else {
            SchemaResult.Failure(SchemaError(issues))
        }
    }

    // 设置最小长度
    fun min(length: Int): StringSchema {
        minLength = length
        return this
    }

    // 设置最大长度
    fun max(length: Int): StringSchema {
        maxLength = length
        return this
    }

    // 设置长度范围
    fun length(min: Int, max: Int): StringSchema {
        minLength = min
        maxLength = max
        return this
    }

    // 设置正则表达式
    fun regex(pattern: Pattern): StringSchema {
        this.pattern = pattern
        return this
    }

    // 设置正则表达式（字符串形式）
    fun regex(pattern: String): StringSchema {
        this.pattern = Pattern.compile(pattern)
        return this
    }

    // 设置为电子邮件验证
    fun email(): StringSchema {
        email = true
        return this
    }

    // 设置为URL验证
    fun url(): StringSchema {
        url = true
        return this
    }

    // 设置为UUID验证
    fun uuid(): StringSchema {
        uuid = true
        return this
    }

    // 设置为CUID验证
    fun cuid(): StringSchema {
        cuid = true
        return this
    }

    // 设置为CUID2验证
    fun cuid2(): StringSchema {
        cuid2 = true
        return this
    }

    // 设置为ULID验证
    fun ulid(): StringSchema {
        ulid = true
        return this
    }

    // 设置为ISO日期时间验证
    fun datetime(): StringSchema {
        datetime = true
        return this
    }

    // 设置为ISO日期验证
    fun date(): StringSchema {
        date = true
        return this
    }

    // 设置为ISO时间验证
    fun time(): StringSchema {
        time = true
        return this
    }

    // 设置为IP地址验证
    fun ip(): StringSchema {
        ip = true
        return this
    }

    // 验证电子邮件的辅助方法
    private fun isValidEmail(value: String): Boolean {
        val emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        return Pattern.matches(emailRegex, value)
    }

    // 验证URL的辅助方法
    private fun isValidUrl(value: String): Boolean {
        return try {
            java.net.URL(value).toURI()
            true
        } catch (e: Exception) {
            false
        }
    }

    // 验证UUID的辅助方法
    private fun isValidUuid(value: String): Boolean {
        val uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        return Pattern.matches(uuidRegex, value)
    }

    // 验证CUID的辅助方法
    private fun isValidCuid(value: String): Boolean {
        val cuidRegex = "^c[a-z0-9]{24}$"
        return Pattern.matches(cuidRegex, value)
    }

    // 验证CUID2的辅助方法
    private fun isValidCuid2(value: String): Boolean {
        val cuid2Regex = "^[a-z][a-z0-9]{23}$"
        return Pattern.matches(cuid2Regex, value)
    }

    // 验证ULID的辅助方法
    private fun isValidUlid(value: String): Boolean {
        val ulidRegex = "^[0-9A-HJKMNP-TV-Z]{26}$"
        return Pattern.matches(ulidRegex, value)
    }

    // 验证ISO日期时间的辅助方法
    private fun isValidDatetime(value: String): Boolean {
        return try {
            java.time.OffsetDateTime.parse(value)
            true
        } catch (e: Exception) {
            false
        }
    }

    // 验证ISO日期的辅助方法
    private fun isValidDate(value: String): Boolean {
        return try {
            java.time.LocalDate.parse(value)
            true
        } catch (e: Exception) {
            false
        }
    }

    // 验证ISO时间的辅助方法
    private fun isValidTime(value: String): Boolean {
        return try {
            java.time.LocalTime.parse(value)
            true
        } catch (e: Exception) {
            false
        }
    }

    // 验证IP地址的辅助方法
    private fun isValidIp(value: String): Boolean {
        return isValidIpv4(value) || isValidIpv6(value)
    }

    // 验证IPv4地址的辅助方法
    private fun isValidIpv4(value: String): Boolean {
        val ipv4Regex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        return Pattern.matches(ipv4Regex, value)
    }

    // 验证IPv6地址的辅助方法
    private fun isValidIpv6(value: String): Boolean {
        val ipv6Regex = "^(([0-9a-fA-F]{1,4}:){7}([0-9a-fA-F]{1,4}|:))|(([0-9a-fA-F]{1,4}:){6}(:[0-9a-fA-F]{1,4}|((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3})|:))|(([0-9a-fA-F]{1,4}:){5}(((:[0-9a-fA-F]{1,4}){1,2})|:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3})|:))|(([0-9a-fA-F]{1,4}:){4}(((:[0-9a-fA-F]{1,4}){1,3})|((:[0-9a-fA-F]{1,4})?:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(([0-9a-fA-F]{1,4}:){3}(((:[0-9a-fA-F]{1,4}){1,4})|((:[0-9a-fA-F]{1,4}){0,2}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(([0-9a-fA-F]{1,4}:){2}(((:[0-9a-fA-F]{1,4}){1,5})|((:[0-9a-fA-F]{1,4}){0,3}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(([0-9a-fA-F]{1,4}:){1}(((:[0-9a-fA-F]{1,4}){1,6})|((:[0-9a-fA-F]{1,4}){0,4}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(:(((:[0-9a-fA-F]{1,4}){1,7})|((:[0-9a-fA-F]{1,4}){0,5}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))$"
        return Pattern.matches(ipv6Regex, value)
    }
}

/**
 * 数字模式，用于验证数字类型的数据。
 *
 * @property min 最小值
 * @property max 最大值
 * @property isInt 是否必须为整数
 * @property multipleOf 必须是该值的倍数
 * @property finite 是否必须为有限数
 */
class NumberSchema(
    var min: Double? = null,
    var max: Double? = null,
    var isInt: Boolean = false,
    var multipleOf: Double? = null,
    var finite: Boolean = true
) : BaseSchema<Number, Double>() {

    override fun _parse(data: Number): SchemaResult<Double> {
        if (data !is Number) {
            return SchemaResult.Failure(
                SchemaError(
                    listOf(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_TYPE,
                            message = "期望数字，收到 ${data::class.simpleName}",
                            path = emptyList()
                        )
                    )
                )
            )
        }

        val value = data.toDouble()
        val issues = mutableListOf<SchemaIssue>()

        // 验证是否为有限数
        if (finite && (value.isInfinite() || value.isNaN())) {
            issues.add(
                SchemaIssue(
                    code = SchemaIssueCode.NOT_FINITE,
                    message = "数字必须是有限的",
                    path = emptyList()
                )
            )
        }

        // 验证最小值
        min?.let {
            if (value < it) {
                issues.add(
                    SchemaIssue(
                        code = SchemaIssueCode.TOO_SMALL,
                        message = "数字必须大于或等于 $it",
                        path = emptyList(),
                        params = mapOf("minimum" to it, "type" to "number", "inclusive" to true)
                    )
                )
            }
        }

        // 验证最大值
        max?.let {
            if (value > it) {
                issues.add(
                    SchemaIssue(
                        code = SchemaIssueCode.TOO_BIG,
                        message = "数字必须小于或等于 $it",
                        path = emptyList(),
                        params = mapOf("maximum" to it, "type" to "number", "inclusive" to true)
                    )
                )
            }
        }

        // 验证是否为整数
        if (isInt && value % 1 != 0.0) {
            issues.add(
                SchemaIssue(
                    code = SchemaIssueCode.INVALID_TYPE,
                    message = "数字必须是整数",
                    path = emptyList(),
                    params = mapOf("expected" to "integer", "received" to "float")
                )
            )
        }

        // 验证是否为倍数
        multipleOf?.let {
            if (value % it != 0.0) {
                issues.add(
                    SchemaIssue(
                        code = SchemaIssueCode.NOT_MULTIPLE_OF,
                        message = "数字必须是 $it 的倍数",
                        path = emptyList(),
                        params = mapOf("multipleOf" to it)
                    )
                )
            }
        }

        return if (issues.isEmpty()) {
            SchemaResult.Success(value)
        } else {
            SchemaResult.Failure(SchemaError(issues))
        }
    }

    // 设置最小值
    fun min(value: Number): NumberSchema {
        min = value.toDouble()
        return this
    }

    // 设置最大值
    fun max(value: Number): NumberSchema {
        max = value.toDouble()
        return this
    }

    // 设置为整数
    fun int(): NumberSchema {
        isInt = true
        return this
    }

    // 设置为倍数
    fun multipleOf(value: Number): NumberSchema {
        multipleOf = value.toDouble()
        return this
    }

    // 设置为有限数
    fun finite(value: Boolean = true): NumberSchema {
        finite = value
        return this
    }

    // 设置为正数
    fun positive(): NumberSchema {
        min = 0.0
        return this
    }

    // 设置为负数
    fun negative(): NumberSchema {
        max = 0.0
        return this
    }

    // 设置为非负数
    fun nonnegative(): NumberSchema {
        min = 0.0
        return this
    }

    // 设置为非正数
    fun nonpositive(): NumberSchema {
        max = 0.0
        return this
    }
}

/**
 * 布尔模式，用于验证布尔类型的数据。
 */
class BooleanSchema : BaseSchema<Boolean, Boolean>() {

    override fun _parse(data: Boolean): SchemaResult<Boolean> {
        if (data !is Boolean) {
            return SchemaResult.Failure(
                SchemaError(
                    listOf(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_TYPE,
                            message = "期望布尔值，收到 ${data::class.simpleName}",
                            path = emptyList()
                        )
                    )
                )
            )
        }

        return SchemaResult.Success(data)
    }
}

/**
 * 枚举模式，用于验证枚举类型的数据。
 *
 * @property values 有效的枚举值列表
 */
class EnumSchema<T : Any>(
    private val values: List<T>
) : BaseSchema<T, T>() {

    /**
     * 获取枚举值列表。
     */
    fun getValues(): List<T> = values

    override fun _parse(data: T): SchemaResult<T> {
        if (!values.contains(data)) {
            return SchemaResult.Failure(
                SchemaError(
                    listOf(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_ENUM_VALUE,
                            message = "无效的枚举值，期望值之一: ${values.joinToString(", ")}",
                            path = emptyList(),
                            params = mapOf("options" to values)
                        )
                    )
                )
            )
        }

        return SchemaResult.Success(data)
    }
}

/**
 * 字面值模式，用于验证特定字面值的数据。
 *
 * @property value 期望的字面值
 */
class LiteralSchema<T : Any>(
    val value: T
) : BaseSchema<T, T>() {

    override fun _parse(data: T): SchemaResult<T> {
        if (data != value) {
            return SchemaResult.Failure(
                SchemaError(
                    listOf(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_LITERAL,
                            message = "无效的字面值，期望: $value",
                            path = emptyList(),
                            params = mapOf("expected" to value)
                        )
                    )
                )
            )
        }

        return SchemaResult.Success(data)
    }
}

/**
 * Null模式，用于验证null值。
 */
class NullSchema : BaseSchema<Any?, Nothing?>() {

    override fun _parse(data: Any?): SchemaResult<Nothing?> {
        if (data != null) {
            return SchemaResult.Failure(
                SchemaError(
                    listOf(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_TYPE,
                            message = "期望null，收到 ${data::class.simpleName}",
                            path = emptyList()
                        )
                    )
                )
            )
        }

        return SchemaResult.Success(null)
    }
}

/**
 * Undefined模式，用于验证undefined值（在Kotlin中通常表示为null）。
 */
class UndefinedSchema : BaseSchema<Any?, Nothing?>() {

    override fun _parse(data: Any?): SchemaResult<Nothing?> {
        if (data != null) {
            return SchemaResult.Failure(
                SchemaError(
                    listOf(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_TYPE,
                            message = "期望undefined，收到 ${data::class.simpleName}",
                            path = emptyList()
                        )
                    )
                )
            )
        }

        return SchemaResult.Success(null)
    }
}

/**
 * Any模式，接受任何类型的数据。
 */
class AnySchema : BaseSchema<Any?, Any?>() {

    override fun _parse(data: Any?): SchemaResult<Any?> {
        return SchemaResult.Success(data)
    }
}
