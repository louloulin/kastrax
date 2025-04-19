package ai.kastrax.zod

/**
 * 为 UnionSchema 添加额外的功能。
 */

/**
 * 设置用于区分联合类型的字段名。
 *
 * @param discriminator 判别器字段名
 * @return 更新后的联合模式
 */
fun UnionSchema.discriminatedBy(discriminator: String): UnionSchema {
    return UnionSchema(schemas, discriminator)
}

/**
 * 判别联合模式，使用判别器字段区分联合类型。
 *
 * @property discriminator 判别器字段名
 * @property schemas 模式映射，键为判别器值，值为对应的模式
 */
class DiscriminatedUnionSchema<K : Any>(
    val discriminator: String,
    val schemas: Map<K, Schema<*, *>>
) : BaseSchema<Any?, Any?>() {

    override fun _parse(data: Any?): SchemaResult<Any?> {
        if (data !is Map<*, *>) {
            return SchemaResult.Failure(
                SchemaError(
                    listOf(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_TYPE,
                            message = "期望对象，收到 ${data?.javaClass?.simpleName}",
                            path = emptyList()
                        )
                    )
                )
            )
        }

        val discriminatorValue = data[discriminator]
        if (discriminatorValue == null) {
            return SchemaResult.Failure(
                SchemaError(
                    listOf(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_UNION,
                            message = "缺少判别器字段: $discriminator",
                            path = listOf(discriminator)
                        )
                    )
                )
            )
        }

        @Suppress("UNCHECKED_CAST")
        val schema = schemas[discriminatorValue as? K]
        if (schema == null) {
            return SchemaResult.Failure(
                SchemaError(
                    listOf(
                        SchemaIssue(
                            code = SchemaIssueCode.INVALID_UNION,
                            message = "无效的判别器值: $discriminatorValue",
                            path = listOf(discriminator),
                            params = mapOf("options" to schemas.keys)
                        )
                    )
                )
            )
        }

        @Suppress("UNCHECKED_CAST")
        return (schema as Schema<Any?, Any?>).safeParse(data)
    }
}
