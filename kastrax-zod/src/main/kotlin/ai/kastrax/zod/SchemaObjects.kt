package ai.kastrax.zod

/**
 * 表示对象模式中的字段。
 *
 * @property key 字段名
 * @property schema 字段的模式
 * @property required 字段是否必需
 */
data class ObjectField<K : Any, V>(
    val key: K,
    val schema: Schema<*, V>,
    val required: Boolean = true
)

/**
 * 对象模式，用于验证对象结构。
 *
 * @property fields 对象的字段列表
 * @property strict 是否严格模式（不允许未定义的字段）
 * @property catchall 用于验证所有未明确定义字段的模式
 * @property unknownKeys 处理未知键的策略
 */
class ObjectSchema<I : Map<*, *>, O : Map<*, *>>(
    val fields: Map<String, ObjectField<*, *>>,
    val strict: Boolean = false,
    val catchall: Schema<*, *>? = null,
    val unknownKeys: UnknownKeysStrategy = UnknownKeysStrategy.STRIP
) : BaseSchema<I, O>() {

    /**
     * 未知键处理策略。
     */
    enum class UnknownKeysStrategy {
        /**
         * 保留未知键。
         */
        PASSTHROUGH,

        /**
         * 移除未知键。
         */
        STRIP,

        /**
         * 严格模式，不允许未知键。
         */
        STRICT
    }

    @Suppress("UNCHECKED_CAST")
    override fun _parse(data: I): SchemaResult<O> {
        if (data !is Map<*, *>) {
            return SchemaResult.Failure(
                SchemaError(
                    listOf(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_TYPE,
                            message = "期望对象，收到 ${data::class.simpleName}",
                            path = emptyList()
                        )
                    )
                )
            )
        }

        val result = mutableMapOf<String, Any?>()
        val issues = mutableListOf<SchemaIssue>()

        // 验证必需字段是否存在
        for ((key, field) in fields) {
            if (field.required && !data.containsKey(key)) {
                issues.add(
                    SchemaIssue(
                        code = SchemaIssueCode.REQUIRED,
                        message = "必需字段缺失",
                        path = listOf(key)
                    )
                )
            }
        }

        // 验证每个字段
        for ((key, value) in data) {
            if (key !is String) {
                issues.add(
                    SchemaIssue(
                        code = SchemaIssueCode.INVALID_TYPE,
                        message = "对象键必须是字符串",
                        path = emptyList()
                    )
                )
                continue
            }

            val field = fields[key]
            if (field != null) {
                // 验证已定义的字段
                val fieldSchema = field.schema as Schema<Any?, Any?>
                val fieldResult = fieldSchema.safeParse(value)

                when (fieldResult) {
                    is SchemaResult.Success -> {
                        result[key] = fieldResult.data
                    }
                    is SchemaResult.Failure -> {
                        // 添加路径前缀
                        val fieldIssues = fieldResult.error.issues.map { issue ->
                            issue.copy(path = listOf(key) + issue.path)
                        }
                        issues.addAll(fieldIssues)
                    }
                }
            } else if (catchall != null) {
                // 使用catchall模式验证未定义的字段
                val catchallSchema = catchall as Schema<Any?, Any?>
                val catchallResult = catchallSchema.safeParse(value)

                when (catchallResult) {
                    is SchemaResult.Success -> {
                        result[key] = catchallResult.data
                    }
                    is SchemaResult.Failure -> {
                        // 添加路径前缀
                        val catchallIssues = catchallResult.error.issues.map { issue ->
                            issue.copy(path = listOf(key) + issue.path)
                        }
                        issues.addAll(catchallIssues)
                    }
                }
            } else if (unknownKeys == UnknownKeysStrategy.STRICT) {
                // 严格模式下，不允许未定义的字段
                issues.add(
                    SchemaIssue(
                        code = SchemaIssueCode.CUSTOM,
                        message = "未知字段: $key",
                        path = listOf(key)
                    )
                )
            } else if (unknownKeys == UnknownKeysStrategy.PASSTHROUGH) {
                // 保留未知字段
                result[key] = value
            }
            // 如果是STRIP策略，则忽略未定义的字段
        }

        return if (issues.isEmpty()) {
            SchemaResult.Success(result as O)
        } else {
            SchemaResult.Failure(SchemaError(issues))
        }
    }

    /**
     * 扩展对象模式，添加新字段。
     *
     * @param extension 要添加的字段
     * @return 新的对象模式
     */
    fun extend(extension: Map<String, ObjectField<*, *>>): ObjectSchema<I, Map<String, Any?>> {
        val newFields = fields.toMutableMap()
        newFields.putAll(extension)
        return ObjectSchema(newFields, strict, catchall, unknownKeys)
    }

    /**
     * 合并两个对象模式。
     *
     * @param other 要合并的对象模式
     * @return 合并后的对象模式
     */
    fun merge(other: ObjectSchema<*, *>): ObjectSchema<I, Map<String, Any?>> {
        val newFields = fields.toMutableMap()
        newFields.putAll(other.fields)
        return ObjectSchema(newFields, strict, catchall, unknownKeys)
    }

    /**
     * 选择对象模式中的特定字段。
     *
     * @param keys 要选择的字段名
     * @return 只包含选定字段的新对象模式
     */
    fun pick(keys: List<String>): ObjectSchema<I, Map<String, Any?>> {
        val newFields = fields.filterKeys { it in keys }
        return ObjectSchema(newFields, strict, catchall, unknownKeys)
    }

    /**
     * 忽略对象模式中的特定字段。
     *
     * @param keys 要忽略的字段名
     * @return 不包含忽略字段的新对象模式
     */
    fun omit(keys: List<String>): ObjectSchema<I, Map<String, Any?>> {
        val newFields = fields.filterKeys { it !in keys }
        return ObjectSchema(newFields, strict, catchall, unknownKeys)
    }

    /**
     * 将所有必需字段转换为可选字段。
     *
     * @return 所有字段都是可选的新对象模式
     */
    fun partial(): ObjectSchema<I, Map<String, Any?>> {
        val newFields = fields.mapValues { (_, field) ->
            field.copy(required = false)
        }
        return ObjectSchema(newFields, strict, catchall, unknownKeys)
    }

    /**
     * 深度将所有必需字段转换为可选字段，包括嵌套对象。
     *
     * @return 所有字段都是可选的新对象模式
     */
    fun deepPartial(): ObjectSchema<I, Map<String, Any?>> {
        // 创建新的字段集合
        val newFields = mutableMapOf<String, ObjectField<String, Any?>>()

        // 遍历原始字段
        for ((key, field) in fields) {
            val schema = field.schema

            // 如果是对象模式，递归处理
            if (schema is ObjectSchema<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val partialSchema = schema.deepPartial() as Schema<*, Any?>
                newFields[key] = ObjectField(key, partialSchema, false)
            } else {
                // 其他类型直接复制并设置为可选
                @Suppress("UNCHECKED_CAST")
                newFields[key] = ObjectField(key, schema as Schema<*, Any?>, false)
            }
        }

        return ObjectSchema(newFields, strict, catchall, unknownKeys)
    }

    /**
     * 将所有可选字段转换为必需字段。
     *
     * @return 所有字段都是必需的新对象模式
     */
    fun required(): ObjectSchema<I, Map<String, Any?>> {
        val newFields = fields.mapValues { (_, field) ->
            field.copy(required = true)
        }
        return ObjectSchema(newFields, strict, catchall, unknownKeys)
    }

    /**
     * 设置为严格模式，不允许未定义的字段。
     *
     * @return 严格模式的新对象模式
     */
    fun strict(): ObjectSchema<I, Map<String, Any?>> {
        return ObjectSchema(fields, true, catchall, UnknownKeysStrategy.STRICT)
    }

    /**
     * 设置为通过模式，保留未定义的字段。
     *
     * @return 通过模式的新对象模式
     */
    fun passthrough(): ObjectSchema<I, Map<String, Any?>> {
        return ObjectSchema(fields, false, catchall, UnknownKeysStrategy.PASSTHROUGH)
    }

    /**
     * 设置为剥离模式，移除未定义的字段。
     *
     * @return 剥离模式的新对象模式
     */
    fun strip(): ObjectSchema<I, Map<String, Any?>> {
        return ObjectSchema(fields, false, catchall, UnknownKeysStrategy.STRIP)
    }

    /**
     * 设置catchall模式，用于验证所有未明确定义的字段。
     *
     * @param schema 用于验证未定义字段的模式
     * @return 带有catchall模式的新对象模式
     */
    fun catchall(schema: Schema<*, *>): ObjectSchema<I, Map<String, Any?>> {
        return ObjectSchema(fields, strict, schema, unknownKeys)
    }

    /**
     * 获取对象模式的键集合。
     *
     * @return 键集合
     */
    fun keyof(): Set<String> {
        return fields.keys
    }
}
