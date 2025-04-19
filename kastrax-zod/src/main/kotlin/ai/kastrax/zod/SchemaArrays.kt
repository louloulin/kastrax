package ai.kastrax.zod

/**
 * 为 ArraySchema 添加额外的功能。
 */

/**
 * 设置数组模式的最小长度。
 *
 * @param length 最小长度
 * @return 更新后的数组模式
 */
fun <I, O> ArraySchema<I, O>.min(length: Int): ArraySchema<I, O> {
    minLength = length
    return this
}

/**
 * 设置数组模式的最大长度。
 *
 * @param length 最大长度
 * @return 更新后的数组模式
 */
fun <I, O> ArraySchema<I, O>.max(length: Int): ArraySchema<I, O> {
    maxLength = length
    return this
}

/**
 * 设置数组模式的精确长度。
 *
 * @param length 精确长度
 * @return 更新后的数组模式
 */
fun <I, O> ArraySchema<I, O>.length(length: Int): ArraySchema<I, O> {
    minLength = length
    maxLength = length
    return this
}

/**
 * 设置数组模式为非空数组。
 *
 * @return 非空数组模式
 */
fun <I, O> ArraySchema<I, O>.nonempty(): ArraySchema<I, O> {
    nonempty = true
    minLength = minLength?.coerceAtLeast(1) ?: 1
    return this
}

/**
 * 获取数组模式的元素模式。
 *
 * @return 元素模式
 */
fun <I, O> ArraySchema<I, O>.element(): Schema<I, O> {
    return elementSchema
}

/**
 * 元组模式，用于验证固定长度且元素类型不同的数组。
 *
 * @property schemas 元素模式列表
 * @property rest 用于验证额外元素的模式
 */
class TupleSchema<I, O>(
    val schemas: List<Schema<*, *>>,
    val rest: Schema<*, *>? = null
) : BaseSchema<List<*>?, List<*>>() {

    @Suppress("UNCHECKED_CAST")
    override fun _parse(data: List<*>?): SchemaResult<List<*>> {
        if (data == null) {
            return SchemaResult.Failure(
                SchemaError(
                    listOf(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_TYPE,
                            message = "期望元组，收到 null",
                            path = emptyList()
                        )
                    )
                )
            )
        }

        if (data !is List<*>) {
            return SchemaResult.Failure(
                SchemaError(
                    listOf(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_TYPE,
                            message = "期望元组，收到 ${data::class.simpleName}",
                            path = emptyList()
                        )
                    )
                )
            )
        }

        val issues = mutableListOf<SchemaIssue>()

        // 验证元组长度
        if (rest == null && data.size > schemas.size) {
            issues.add(
                SchemaIssue(
                    code = SchemaIssueCode.TOO_BIG,
                    message = "元组长度最多为 ${schemas.size} 个元素",
                    path = emptyList(),
                    params = mapOf("maximum" to schemas.size, "type" to "array", "inclusive" to true)
                )
            )
        }

        if (data.size < schemas.size) {
            issues.add(
                SchemaIssue(
                    code = SchemaIssueCode.TOO_SMALL,
                    message = "元组长度至少为 ${schemas.size} 个元素",
                    path = emptyList(),
                    params = mapOf("minimum" to schemas.size, "type" to "array", "inclusive" to true)
                )
            )
        }

        // 如果已经有错误，直接返回
        if (issues.isNotEmpty()) {
            return SchemaResult.Failure(SchemaError(issues))
        }

        val result = mutableListOf<Any?>()

        // 验证每个元素
        for (i in data.indices) {
            val item = data[i]
            val schema = if (i < schemas.size) {
                schemas[i]
            } else if (rest != null) {
                rest
            } else {
                // 这种情况不应该发生，因为我们已经验证了长度
                continue
            }

            val itemSchema = schema as Schema<Any?, Any?>
            val itemResult = itemSchema.safeParse(item)

            when (itemResult) {
                is SchemaResult.Success -> result.add(itemResult.data)
                is SchemaResult.Failure -> {
                    // 添加路径前缀
                    val itemIssues = itemResult.error.issues.map { issue ->
                        issue.copy(path = listOf(i.toString()) + issue.path)
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

    /**
     * 设置用于验证额外元素的模式。
     *
     * @param schema 用于验证额外元素的模式
     * @return 更新后的元组模式
     */
    fun rest(schema: Schema<*, *>): TupleSchema<I, O> {
        return TupleSchema(schemas, schema)
    }
}
