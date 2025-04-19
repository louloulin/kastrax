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
    val schemas: Map<K, Schema<Any?, Any?>>
) : BaseSchema<Map<String, Any?>, Any?>() {

    override fun _parse(data: Map<String, Any?>): SchemaResult<Any?> {
        // 数据已经由方法签名保证是 Map<String, Any?> 类型

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

        val key = discriminatorValue as? K
        val schema = if (key != null) schemas[key] else null
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

        return schema.safeParse(data)
    }
}
