package ai.kastrax.zod

import kotlinx.serialization.json.JsonElement
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * 表示验证结果的密封类。
 * 可以是成功（包含验证后的数据）或失败（包含错误信息）。
 */
sealed class SchemaResult<out T> {
    /**
     * 表示成功的验证结果。
     *
     * @property data 验证后的数据
     */
    data class Success<T>(val data: T) : SchemaResult<T>() {
        val success: Boolean = true
    }

    /**
     * 表示失败的验证结果。
     *
     * @property error 验证错误
     */
    data class Failure(val error: SchemaError) : SchemaResult<Nothing>() {
        val success: Boolean = false
    }
}

/**
 * 表示验证过程中发生的错误。
 *
 * @property issues 错误问题列表
 */
data class SchemaError(
    val issues: List<SchemaIssue>
) {
    /**
     * 将错误格式化为嵌套对象，便于展示。
     */
    fun format(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val pathGroups = issues.groupBy { it.path.joinToString(".") }

        pathGroups.forEach { (path, issues) ->
            if (path.isEmpty()) {
                result["_errors"] = issues.map { it.message }
            } else {
                val parts = path.split(".")
                var current = result

                // 构建嵌套路径
                for (i in parts.indices) {
                    val part = parts[i]
                    if (i == parts.lastIndex) {
                        current[part] = mapOf("_errors" to issues.map { it.message })
                    } else {
                        if (part !in current) {
                            current[part] = mutableMapOf<String, Any>()
                        }
                        @Suppress("UNCHECKED_CAST")
                        current = current[part] as MutableMap<String, Any>
                    }
                }
            }
        }

        return result
    }
}

/**
 * 表示验证过程中的单个问题。
 *
 * @property code 错误代码
 * @property message 错误消息
 * @property path 错误路径
 */
data class SchemaIssue(
    val code: SchemaIssueCode,
    val message: String,
    val path: List<String> = emptyList(),
    val params: Map<String, Any> = emptyMap()
)

/**
 * 错误代码枚举。
 */
enum class SchemaIssueCode {
    INVALID_TYPE,
    REQUIRED,
    INVALID_LITERAL,
    CUSTOM,
    INVALID_UNION,
    INVALID_ENUM_VALUE,
    TOO_SMALL,
    TOO_BIG,
    INVALID_STRING,
    INVALID_ARGUMENTS,
    INVALID_RETURN_TYPE,
    INVALID_DATE,
    INVALID_INTERSECTION_TYPES,
    NOT_MULTIPLE_OF,
    NOT_FINITE,
    MISSING_REQUIRED_FIELD
}

/**
 * 基础模式接口，定义了所有模式类型共有的方法。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
interface Schema<I, O> {
    /**
     * 解析并验证输入数据。
     * 如果验证成功，返回验证后的数据；如果失败，抛出异常。
     *
     * @param data 要验证的输入数据
     * @return 验证后的数据
     * @throws SchemaValidationException 如果验证失败
     */
    fun parse(data: I): O

    /**
     * 安全地解析并验证输入数据。
     * 返回一个 [SchemaResult]，表示验证成功或失败。
     *
     * @param data 要验证的输入数据
     * @return 验证结果
     */
    fun safeParse(data: I): SchemaResult<O>

    /**
     * 异步解析并验证输入数据。
     * 如果验证成功，返回验证后的数据；如果失败，抛出异常。
     *
     * @param data 要验证的输入数据
     * @return 验证后的数据
     * @throws SchemaValidationException 如果验证失败
     */
    suspend fun parseAsync(data: I): O

    /**
     * 安全地异步解析并验证输入数据。
     * 返回一个 [SchemaResult]，表示验证成功或失败。
     *
     * @param data 要验证的输入数据
     * @return 验证结果
     */
    suspend fun safeParseAsync(data: I): SchemaResult<O>

    /**
     * 将此模式转换为可选模式。
     * 可选模式接受 null 或 undefined 值。
     *
     * @return 新的可选模式
     */
    fun optional(): Schema<I?, O?>

    /**
     * 将此模式转换为可空模式。
     * 可空模式接受 null 值。
     *
     * @return 新的可空模式
     */
    fun nullable(): Schema<I?, O?>

    /**
     * 将此模式转换为可空可选模式。
     * 可空可选模式接受 null 或 undefined 值。
     *
     * @return 新的可空可选模式
     */
    fun nullish(): Schema<I?, O?>

    /**
     * 为此模式添加默认值。
     * 当输入为 undefined 时，将使用默认值。
     *
     * @param defaultValue 默认值
     * @return 带有默认值的新模式
     */
    fun default(defaultValue: O): Schema<I?, O>

    /**
     * 为此模式添加描述。
     *
     * @param description 描述文本
     * @return 带有描述的新模式
     */
    fun describe(description: String): Schema<I, O>

    /**
     * 添加自定义验证逻辑。
     *
     * @param check 验证函数
     * @param message 错误消息
     * @return 带有自定义验证的新模式
     */
    fun refine(check: (data: O) -> Boolean, message: String? = null): Schema<I, O>

    /**
     * 添加转换逻辑。
     *
     * @param transform 转换函数
     * @return 带有转换的新模式
     */
    fun <U> transform(transform: (data: O) -> U): Schema<I, U>

    /**
     * 将此模式与另一个模式组合，创建一个联合类型。
     *
     * @param other 另一个模式
     * @return 联合模式
     */
    fun <I2, O2> or(other: Schema<I2, O2>): Schema<Any?, Any?>

    /**
     * 将此模式与另一个模式组合，创建一个交叉类型。
     *
     * @param other 另一个模式
     * @return 交叉模式
     */
    fun <I2, O2> and(other: Schema<I2, O2>): Schema<Any?, Any?>

    /**
     * 将此模式转换为数组模式。
     *
     * @return 数组模式
     */
    fun array(): Schema<List<I>?, List<O>>

    /**
     * 将此模式转换为 Promise 模式。
     *
     * @return Promise 模式
     */
    fun promise(): Schema<Deferred<I>?, Deferred<O>>

    /**
     * 将此模式与另一个模式连接，创建一个管道。
     *
     * @param other 另一个模式
     * @return 管道模式
     */
    fun <U> pipe(other: Schema<O, U>): Schema<I, U>
}

/**
 * 验证异常，当验证失败时抛出。
 *
 * @property error 验证错误
 */
class SchemaValidationException(val error: SchemaError) : Exception(
    "验证失败: ${error.issues.joinToString("; ") { it.message }}"
)

/**
 * 基础模式抽象类，提供了通用实现。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
abstract class BaseSchema<I, O> : Schema<I, O> {
    /**
     * 内部验证方法，由子类实现。
     *
     * @param data 要验证的输入数据
     * @return 验证结果
     */
    protected abstract fun _parse(data: I): SchemaResult<O>

    /**
     * 内部异步验证方法，由子类实现。
     *
     * @param data 要验证的输入数据
     * @return 验证结果
     */
    protected open suspend fun _parseAsync(data: I): SchemaResult<O> {
        return _parse(data)
    }

    override fun parse(data: I): O {
        val result = _parse(data)
        return when (result) {
            is SchemaResult.Success -> result.data
            is SchemaResult.Failure -> throw SchemaValidationException(result.error)
        }
    }

    override fun safeParse(data: I): SchemaResult<O> {
        return _parse(data)
    }

    override suspend fun parseAsync(data: I): O {
        val result = _parseAsync(data)
        return when (result) {
            is SchemaResult.Success -> result.data
            is SchemaResult.Failure -> throw SchemaValidationException(result.error)
        }
    }

    override suspend fun safeParseAsync(data: I): SchemaResult<O> {
        return _parseAsync(data)
    }

    override fun optional(): Schema<I?, O?> {
        return OptionalSchema(this)
    }

    override fun nullable(): Schema<I?, O?> {
        return NullableSchema(this)
    }

    override fun nullish(): Schema<I?, O?> {
        return NullishSchema(this)
    }

    override fun default(defaultValue: O): Schema<I?, O> {
        return DefaultSchema(this, defaultValue)
    }

    override fun describe(description: String): Schema<I, O> {
        return DescribedSchema(this, description)
    }

    override fun refine(check: (data: O) -> Boolean, message: String?): Schema<I, O> {
        return RefinedSchema(this, check, message)
    }

    override fun <U> transform(transform: (data: O) -> U): Schema<I, U> {
        return TransformedSchema(this, transform)
    }

    override fun <I2, O2> or(other: Schema<I2, O2>): Schema<Any?, Any?> {
        return UnionSchema(listOf(this, other))
    }

    override fun <I2, O2> and(other: Schema<I2, O2>): Schema<Any?, Any?> {
        return IntersectionSchema(this, other)
    }

    override fun array(): Schema<List<I>?, List<O>> {
        return ArraySchema(this)
    }

    override fun promise(): Schema<Deferred<I>?, Deferred<O>> {
        return PromiseSchema(this)
    }

    override fun <U> pipe(other: Schema<O, U>): Schema<I, U> {
        return PipeSchema(this, other)
    }
}

/**
 * 可选模式，接受 null 或 undefined 值。
 */
class OptionalSchema<I, O>(private val schema: Schema<I, O>) : BaseSchema<I?, O?>() {
    override fun _parse(data: I?): SchemaResult<O?> {
        if (data == null) {
            return SchemaResult.Success(null)
        }

        return when (val result = schema.safeParse(data)) {
            is SchemaResult.Success -> SchemaResult.Success(result.data)
            is SchemaResult.Failure -> SchemaResult.Failure(result.error)
        }
    }
}

/**
 * 可空模式，接受 null 值。
 */
class NullableSchema<I, O>(private val schema: Schema<I, O>) : BaseSchema<I?, O?>() {
    override fun _parse(data: I?): SchemaResult<O?> {
        if (data == null) {
            return SchemaResult.Success(null)
        }

        return when (val result = schema.safeParse(data)) {
            is SchemaResult.Success -> SchemaResult.Success(result.data)
            is SchemaResult.Failure -> SchemaResult.Failure(result.error)
        }
    }
}

/**
 * 可空可选模式，接受 null 或 undefined 值。
 */
class NullishSchema<I, O>(private val schema: Schema<I, O>) : BaseSchema<I?, O?>() {
    override fun _parse(data: I?): SchemaResult<O?> {
        if (data == null) {
            return SchemaResult.Success(null)
        }

        return when (val result = schema.safeParse(data)) {
            is SchemaResult.Success -> SchemaResult.Success(result.data)
            is SchemaResult.Failure -> SchemaResult.Failure(result.error)
        }
    }
}

/**
 * 带有默认值的模式。
 */
class DefaultSchema<I, O>(
    private val schema: Schema<I, O>,
    private val defaultValue: O
) : BaseSchema<I?, O>() {
    override fun _parse(data: I?): SchemaResult<O> {
        if (data == null) {
            return SchemaResult.Success(defaultValue)
        }

        return when (val result = schema.safeParse(data)) {
            is SchemaResult.Success -> SchemaResult.Success(result.data)
            is SchemaResult.Failure -> SchemaResult.Failure(result.error)
        }
    }
}

/**
 * 带有描述的模式。
 */
class DescribedSchema<I, O>(
    private val schema: Schema<I, O>,
    val description: String
) : BaseSchema<I, O>() {
    override fun _parse(data: I): SchemaResult<O> {
        return schema.safeParse(data)
    }
}

/**
 * 带有自定义验证的模式。
 */
class RefinedSchema<I, O>(
    private val schema: Schema<I, O>,
    private val check: (data: O) -> Boolean,
    private val message: String?
) : BaseSchema<I, O>() {
    override fun _parse(data: I): SchemaResult<O> {
        val result = schema.safeParse(data)

        return when (result) {
            is SchemaResult.Success -> {
                if (check(result.data)) {
                    result
                } else {
                    SchemaResult.Failure(
                        SchemaError(
                            listOf(
                                SchemaIssue(
                                    code = SchemaIssueCode.CUSTOM,
                                    message = message ?: "自定义验证失败",
                                    path = emptyList()
                                )
                            )
                        )
                    )
                }
            }
            is SchemaResult.Failure -> result
        }
    }
}

/**
 * 带有转换的模式。
 */
class TransformedSchema<I, O, U>(
    private val schema: Schema<I, O>,
    private val transform: (data: O) -> U
) : BaseSchema<I, U>() {
    override fun _parse(data: I): SchemaResult<U> {
        val result = schema.safeParse(data)

        return when (result) {
            is SchemaResult.Success -> {
                try {
                    val transformed = transform(result.data)
                    SchemaResult.Success(transformed)
                } catch (e: Exception) {
                    SchemaResult.Failure(
                        SchemaError(
                            listOf(
                                SchemaIssue(
                                    code = SchemaIssueCode.CUSTOM,
                                    message = "转换失败: ${e.message}",
                                    path = emptyList()
                                )
                            )
                        )
                    )
                }
            }
            is SchemaResult.Failure -> SchemaResult.Failure(result.error)
        }
    }
}

/**
 * 联合模式，接受多个模式中的任意一个。
 *
 * @property schemas 模式列表
 * @property discriminator 判别器字段名，用于区分联合类型
 */
class UnionSchema(
    val schemas: List<Schema<*, *>>,
    val discriminator: String? = null
) : BaseSchema<Any?, Any?>() {
    override fun _parse(data: Any?): SchemaResult<Any?> {
        // 如果有判别器，先检查判别器
        if (discriminator != null && data is Map<*, *>) {
            val discriminatorValue = data[discriminator]
            if (discriminatorValue != null) {
                // 尝试找到匹配的模式
                for (schema in schemas) {
                    if (schema is ObjectSchema<*, *>) {
                        val typeField = schema.fields[discriminator]
                        if (typeField?.schema is LiteralSchema<*>) {
                            val literalSchema = typeField.schema as LiteralSchema<*>
                            if (discriminatorValue == literalSchema.value) {
                                @Suppress("UNCHECKED_CAST")
                                return (schema as Schema<Any?, Any?>).safeParse(data)
                            }
                        }
                    }
                }
            }
        }

        // 如果没有判别器或判别器不匹配，尝试所有模式
        val errors = mutableListOf<SchemaError>()

        for (schema in schemas) {
            @Suppress("UNCHECKED_CAST")
            val result = (schema as Schema<Any?, Any?>).safeParse(data)

            when (result) {
                is SchemaResult.Success -> return result
                is SchemaResult.Failure -> errors.add(result.error)
            }
        }

        return SchemaResult.Failure(
            SchemaError(
                listOf(
                    SchemaIssue(
                        code = SchemaIssueCode.INVALID_UNION,
                        message = "无效的联合类型",
                        path = emptyList(),
                        params = mapOf("unionErrors" to errors)
                    )
                )
            )
        )
    }
}

/**
 * 交叉模式，要求同时满足两个模式。
 */
class IntersectionSchema<I1, O1, I2, O2>(
    private val left: Schema<I1, O1>,
    private val right: Schema<I2, O2>
) : BaseSchema<Any?, Any?>() {
    override fun _parse(data: Any?): SchemaResult<Any?> {
        @Suppress("UNCHECKED_CAST")
        val leftResult = (left as Schema<Any?, Any?>).safeParse(data)

        when (leftResult) {
            is SchemaResult.Success -> {
                @Suppress("UNCHECKED_CAST")
                val rightResult = (right as Schema<Any?, Any?>).safeParse(data)

                return when (rightResult) {
                    is SchemaResult.Success -> {
                        // 合并结果
                        // 注意：这里的实现取决于具体的数据类型
                        // 对于对象，应该合并属性
                        SchemaResult.Success(rightResult.data)
                    }
                    is SchemaResult.Failure -> rightResult
                }
            }
            is SchemaResult.Failure -> return leftResult
        }
    }
}

/**
 * 数组模式，验证数组中的每个元素。
 *
 * @property elementSchema 元素模式
 * @property minLength 最小长度
 * @property maxLength 最大长度
 * @property nonempty 是否非空
 */
class ArraySchema<I, O>(
    val elementSchema: Schema<I, O>,
    var minLength: Int? = null,
    var maxLength: Int? = null,
    var nonempty: Boolean = false
) : BaseSchema<List<I>?, List<O>>() {
    override fun _parse(data: List<I>?): SchemaResult<List<O>> {
        if (data == null) {
            return SchemaResult.Failure(
                SchemaError(
                    listOf(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_TYPE,
                            message = "期望数组，收到 null",
                            path = emptyList()
                        )
                    )
                )
            )
        }

        val issues = mutableListOf<SchemaIssue>()

        // 验证最小长度
        minLength?.let {
            if (data.size < it) {
                issues.add(
                    SchemaIssue(
                        code = SchemaIssueCode.TOO_SMALL,
                        message = "数组长度至少为 $it 个元素",
                        path = emptyList(),
                        params = mapOf("minimum" to it, "type" to "array", "inclusive" to true)
                    )
                )
            }
        }

        // 验证最大长度
        maxLength?.let {
            if (data.size > it) {
                issues.add(
                    SchemaIssue(
                        code = SchemaIssueCode.TOO_BIG,
                        message = "数组长度最多为 $it 个元素",
                        path = emptyList(),
                        params = mapOf("maximum" to it, "type" to "array", "inclusive" to true)
                    )
                )
            }
        }

        // 验证非空
        if (nonempty && data.isEmpty()) {
            issues.add(
                SchemaIssue(
                    code = SchemaIssueCode.TOO_SMALL,
                    message = "数组不能为空",
                    path = emptyList(),
                    params = mapOf("minimum" to 1, "type" to "array", "inclusive" to true)
                )
            )
        }

        // 如果已经有错误，直接返回
        if (issues.isNotEmpty()) {
            return SchemaResult.Failure(SchemaError(issues))
        }

        val result = mutableListOf<O>()

        for ((index, item) in data.withIndex()) {
            val itemResult = elementSchema.safeParse(item)

            when (itemResult) {
                is SchemaResult.Success -> result.add(itemResult.data)
                is SchemaResult.Failure -> {
                    // 添加路径前缀
                    val itemIssues = itemResult.error.issues.map { issue ->
                        issue.copy(path = listOf(index.toString()) + issue.path)
                    }
                    issues.addAll(itemIssues)
                }
            }
        }

        return if (issues.isEmpty()) {
            SchemaResult.Success(result)
        } else {
            SchemaResult.Failure(SchemaError(issues))
        }
    }
}

/**
 * Promise 模式，验证 Promise 的结果。
 */
class PromiseSchema<I, O>(private val valueSchema: Schema<I, O>) : BaseSchema<Deferred<I>?, Deferred<O>>() {
    override fun _parse(data: Deferred<I>?): SchemaResult<Deferred<O>> {
        if (data == null) {
            return SchemaResult.Failure(
                SchemaError(
                    listOf(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_TYPE,
                            message = "期望 Promise，收到 null",
                            path = emptyList()
                        )
                    )
                )
            )
        }

        // 同步方法不能使用 coroutineScope，抛出异常提示使用异步方法
        throw UnsupportedOperationException("异步模式必须使用 parseAsync 方法")
    }

    override suspend fun _parseAsync(data: Deferred<I>?): SchemaResult<Deferred<O>> {
        if (data == null) {
            return SchemaResult.Failure(
                SchemaError(
                    listOf(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_TYPE,
                            message = "期望 Promise，收到 null",
                            path = emptyList()
                        )
                    )
                )
            )
        }

        val value = data.await()
        val result = valueSchema.safeParseAsync(value)

        return when (result) {
            is SchemaResult.Success -> {
                // 使用 kotlinx.coroutines.async 创建新的 Deferred
                val deferred = kotlinx.coroutines.GlobalScope.async {
                    result.data
                }
                SchemaResult.Success(deferred)
            }
            is SchemaResult.Failure -> SchemaResult.Failure(result.error)
        }
    }
}

/**
 * 管道模式，将一个模式的输出传递给另一个模式。
 */
class PipeSchema<I, O, U>(
    private val inputSchema: Schema<I, O>,
    private val outputSchema: Schema<O, U>
) : BaseSchema<I, U>() {
    override fun _parse(data: I): SchemaResult<U> {
        val inputResult = inputSchema.safeParse(data)

        return when (inputResult) {
            is SchemaResult.Success -> outputSchema.safeParse(inputResult.data)
            is SchemaResult.Failure -> SchemaResult.Failure(inputResult.error)
        }
    }

    override suspend fun _parseAsync(data: I): SchemaResult<U> {
        val inputResult = inputSchema.safeParseAsync(data)

        return when (inputResult) {
            is SchemaResult.Success -> outputSchema.safeParseAsync(inputResult.data)
            is SchemaResult.Failure -> SchemaResult.Failure(inputResult.error)
        }
    }
}
