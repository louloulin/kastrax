package ai.kastrax.zod

import kotlinx.serialization.json.*
import java.util.regex.Pattern

/**
 * 将 Schema 转换为 JSON Schema。
 */
fun <I, O> Schema<I, O>.toJsonSchema(): JsonElement {
    return when (this) {
        is StringSchema -> JsonObject(
            buildMap {
                put("type", JsonPrimitive("string"))
                if (minLength != null) put("minLength", JsonPrimitive(minLength!!))
                if (maxLength != null) put("maxLength", JsonPrimitive(maxLength!!))
                if (pattern != null) put("pattern", JsonPrimitive(pattern!!.pattern()))
                if (email) put("format", JsonPrimitive("email"))
                if (url) put("format", JsonPrimitive("uri"))
                if (uuid) put("format", JsonPrimitive("uuid"))
                // Description is added by a wrapper class, not directly in StringSchema
                if (this is DescribedSchema<*, *>) put("description", JsonPrimitive(description))
            }
        )
        is NumberSchema -> JsonObject(
            buildMap {
                put("type", JsonPrimitive("number"))
                if (min != null) put("minimum", JsonPrimitive(min!!))
                if (max != null) put("maximum", JsonPrimitive(max!!))
                if (multipleOf != null) put("multipleOf", JsonPrimitive(multipleOf!!))
                // Description is added by a wrapper class, not directly in NumberSchema
                if (this is DescribedSchema<*, *>) put("description", JsonPrimitive(description))
            }
        )
        is BooleanSchema -> JsonObject(
            buildMap {
                put("type", JsonPrimitive("boolean"))
                // Description is added by a wrapper class, not directly in BooleanSchema
                if (this is DescribedSchema<*, *>) put("description", JsonPrimitive(description))
            }
        )
        is ObjectSchema<*, *> -> {
            val properties = JsonObject(
                fields.mapValues { (_, field) ->
                    @Suppress("UNCHECKED_CAST")
                    (field.schema as Schema<Any?, Any?>).toJsonSchema()
                }
            )

            val required = JsonArray(
                fields.filter { (_, field) -> field.required }
                    .map { (key, _) -> JsonPrimitive(key) }
            )

            JsonObject(
                buildMap {
                    put("type", JsonPrimitive("object"))
                    put("properties", properties)
                    if (required.isNotEmpty()) put("required", required)
                    if (catchall != null) {
                        @Suppress("UNCHECKED_CAST")
                        put("additionalProperties", (catchall as Schema<Any?, Any?>).toJsonSchema())
                    } else if (strict) {
                        put("additionalProperties", JsonPrimitive(false))
                    }
                    // Description is added by a wrapper class, not directly in ObjectSchema
                    if (this is DescribedSchema<*, *>) put("description", JsonPrimitive(description))
                }
            )
        }
        is ArraySchema<*, *> -> JsonObject(
            buildMap {
                put("type", JsonPrimitive("array"))
                @Suppress("UNCHECKED_CAST")
                put("items", (elementSchema as Schema<Any?, Any?>).toJsonSchema())
                if (minLength != null) put("minItems", JsonPrimitive(minLength!!))
                if (maxLength != null) put("maxItems", JsonPrimitive(maxLength!!))
                if (nonempty) put("minItems", JsonPrimitive(1))
                // Description is added by a wrapper class, not directly in ArraySchema
                if (this is DescribedSchema<*, *>) put("description", JsonPrimitive(description))
            }
        )
        is TupleSchema -> {
            val items = JsonArray(
                schemas.map { schema ->
                    @Suppress("UNCHECKED_CAST")
                    (schema as Schema<Any?, Any?>).toJsonSchema()
                }
            )

            JsonObject(
                buildMap {
                    put("type", JsonPrimitive("array"))
                    put("items", items)
                    put("minItems", JsonPrimitive(schemas.size))
                    if (rest == null) {
                        put("maxItems", JsonPrimitive(schemas.size))
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        put("additionalItems", (rest as Schema<Any?, Any?>).toJsonSchema())
                    }
                    // Description is added by a wrapper class, not directly in TupleSchema
                    if (this is DescribedSchema<*, *>) put("description", JsonPrimitive(description))
                }
            )
        }
        is UnionSchema -> JsonObject(
            buildMap {
                val anyOf = JsonArray(
                    schemas.map { schema ->
                        @Suppress("UNCHECKED_CAST")
                        (schema as Schema<Any?, Any?>).toJsonSchema()
                    }
                )
                put("anyOf", anyOf)
                // Description is added by a wrapper class, not directly in UnionSchema
                if (this is DescribedSchema<*, *>) put("description", JsonPrimitive(description))
            }
        )
        is IntersectionSchema<*, *> -> JsonObject(
            buildMap {
                val allOf = JsonArray(
                    listOf(
                        @Suppress("UNCHECKED_CAST")
                        (this as IntersectionSchema<*, *>).getLeftSchema().toJsonSchema(),
                        @Suppress("UNCHECKED_CAST")
                        (this as IntersectionSchema<*, *>).getRightSchema().toJsonSchema()
                    )
                )
                put("allOf", allOf)
                // Description is added by a wrapper class, not directly in IntersectionSchema
                if (this is DescribedSchema<*, *>) put("description", JsonPrimitive(description))
            }
        )
        is EnumSchema<*> -> JsonObject(
            buildMap {
                put("type", JsonPrimitive("string"))
                val enumValues = JsonArray(
                    (this as EnumSchema<*>).getValues().map { JsonPrimitive(it.toString()) }
                )
                put("enum", enumValues)
                // Description is added by a wrapper class, not directly in EnumSchema
                if (this is DescribedSchema<*, *>) put("description", JsonPrimitive(description))
            }
        )
        is LiteralSchema<*> -> JsonObject(
            buildMap {
                when (value) {
                    is String -> {
                        put("type", JsonPrimitive("string"))
                        put("enum", JsonArray(listOf(JsonPrimitive(value))))
                    }
                    is Number -> {
                        put("type", JsonPrimitive("number"))
                        put("enum", JsonArray(listOf(JsonPrimitive(value.toDouble()))))
                    }
                    is Boolean -> {
                        put("type", JsonPrimitive("boolean"))
                        put("enum", JsonArray(listOf(JsonPrimitive(value))))
                    }
                    else -> {
                        put("type", JsonPrimitive("string"))
                        put("enum", JsonArray(listOf(JsonPrimitive(value.toString()))))
                    }
                }
                // Description is added by a wrapper class, not directly in LiteralSchema
                if (this is DescribedSchema<*, *>) put("description", JsonPrimitive(description))
            }
        )
        is RefinedSchema<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            (this as RefinedSchema<*, *>).getBaseSchema().toJsonSchema()
        }
        is TransformedSchema<*, *, *> -> {
            @Suppress("UNCHECKED_CAST")
            (this as TransformedSchema<*, *, *>).getBaseSchema().toJsonSchema()
        }
        is DefaultSchema<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val baseSchema = (this as DefaultSchema<*, *>).getBaseSchema().toJsonSchema().jsonObject.toMutableMap()
            baseSchema["default"] = Json.encodeToJsonElement((this as DefaultSchema<*, *>).getDefaultValue())
            JsonObject(baseSchema)
        }
        else -> JsonObject(
            mapOf(
                "type" to JsonPrimitive("object"),
                "description" to JsonPrimitive(this::class.simpleName ?: "Unknown schema")
            )
        )
    }
}

/**
 * 从 JSON 解析输入。
 */
fun <I, O> Schema<I, O>.parseJson(json: JsonElement): I {
    return when (this) {
        is StringSchema -> {
            if (json !is JsonPrimitive || !json.isString) {
                throw IllegalArgumentException("Expected string, got $json")
            }
            @Suppress("UNCHECKED_CAST")
            json.content as I
        }
        is NumberSchema -> {
            if (json !is JsonPrimitive || (!json.isString && json.content.toDoubleOrNull() == null)) {
                throw IllegalArgumentException("Expected number, got $json")
            }
            @Suppress("UNCHECKED_CAST")
            json.content.toDouble() as I
        }
        is BooleanSchema -> {
            if (json !is JsonPrimitive || (!json.isString && json.content.toBooleanStrictOrNull() == null)) {
                throw IllegalArgumentException("Expected boolean, got $json")
            }
            @Suppress("UNCHECKED_CAST")
            json.content.toBoolean() as I
        }
        is ObjectSchema<*, *> -> {
            if (json !is JsonObject) {
                throw IllegalArgumentException("Expected object, got $json")
            }

            val result = mutableMapOf<String, Any?>()

            // Process defined fields
            for ((key, field) in fields) {
                if (key in json) {
                    @Suppress("UNCHECKED_CAST")
                    val fieldSchema = field.schema as Schema<Any?, Any?>
                    result[key] = fieldSchema.parseJson(json[key]!!)
                } else if (field.required) {
                    throw IllegalArgumentException("Missing required field: $key")
                }
            }

            // Process additional fields
            if (!strict && catchall != null) {
                for ((key, value) in json) {
                    if (key !in fields) {
                        @Suppress("UNCHECKED_CAST")
                        val catchallSchema = catchall as Schema<Any?, Any?>
                        result[key] = catchallSchema.parseJson(value)
                    }
                }
            } else if (!strict) {
                for ((key, value) in json) {
                    if (key !in fields) {
                        result[key] = when (value) {
                            is JsonPrimitive -> {
                                when {
                                    value.isString -> value.content
                                    value.content.toDoubleOrNull() != null -> value.content.toDouble()
                                    value.content.toBooleanStrictOrNull() != null -> value.content.toBoolean()
                                    else -> value.content
                                }
                            }
                            is JsonObject -> parseJson(value)
                            is JsonArray -> value.map { parseJson(it) }
                            else -> value.toString()
                        }
                    }
                }
            }

            @Suppress("UNCHECKED_CAST")
            result as I
        }
        is ArraySchema<*, *> -> {
            if (json !is JsonArray) {
                throw IllegalArgumentException("Expected array, got $json")
            }

            @Suppress("UNCHECKED_CAST")
            val elementSchema = elementSchema as Schema<Any?, Any?>

            @Suppress("UNCHECKED_CAST")
            json.map { elementSchema.parseJson(it) } as I
        }
        is TupleSchema -> {
            if (json !is JsonArray) {
                throw IllegalArgumentException("Expected array, got $json")
            }

            if (json.size < schemas.size) {
                throw IllegalArgumentException("Expected at least ${schemas.size} elements, got ${json.size}")
            }

            if (rest == null && json.size > schemas.size) {
                throw IllegalArgumentException("Expected exactly ${schemas.size} elements, got ${json.size}")
            }

            val result = mutableListOf<Any?>()

            // Process defined elements
            for (i in schemas.indices) {
                @Suppress("UNCHECKED_CAST")
                val schema = schemas[i] as Schema<Any?, Any?>
                result.add(schema.parseJson(json[i]))
            }

            // Process rest elements
            if (rest != null) {
                for (i in schemas.size until json.size) {
                    @Suppress("UNCHECKED_CAST")
                    val restSchema = rest as Schema<Any?, Any?>
                    result.add(restSchema.parseJson(json[i]))
                }
            }

            @Suppress("UNCHECKED_CAST")
            result as I
        }
        is UnionSchema -> {
            for (schema in schemas) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    return (schema as Schema<Any?, Any?>).parseJson(json) as I
                } catch (e: Exception) {
                    // Try next schema
                }
            }
            throw IllegalArgumentException("No matching schema found for $json")
        }
        is IntersectionSchema<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val leftSchema = (this as IntersectionSchema<*, *>).getLeftSchema()
            @Suppress("UNCHECKED_CAST")
            val rightSchema = (this as IntersectionSchema<*, *>).getRightSchema()

            val leftResult = leftSchema.parseJson(json)
            val rightResult = rightSchema.parseJson(json)

            // For objects, merge the results
            if (leftResult is Map<*, *> && rightResult is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val result = (leftResult as Map<String, Any?>).toMutableMap()
                @Suppress("UNCHECKED_CAST")
                result.putAll(rightResult as Map<String, Any?>)
                @Suppress("UNCHECKED_CAST")
                return result as I
            }

            // For other types, return the right result
            @Suppress("UNCHECKED_CAST")
            return rightResult as I
        }
        is EnumSchema<*> -> {
            if (json !is JsonPrimitive || !json.isString) {
                throw IllegalArgumentException("Expected string, got $json")
            }

            val value = json.content
            val enumValue = (this as EnumSchema<*>).getValues().find { it.toString() == value }
                ?: throw IllegalArgumentException("Invalid enum value: $value")

            @Suppress("UNCHECKED_CAST")
            enumValue as I
        }
        is LiteralSchema<*> -> {
            if (json !is JsonPrimitive) {
                throw IllegalArgumentException("Expected primitive, got $json")
            }

            val jsonValue = when {
                json.isString -> json.content
                json.content.toDoubleOrNull() != null -> json.content.toDouble()
                json.content.toBooleanStrictOrNull() != null -> json.content.toBoolean()
                else -> json.content
            }

            if (jsonValue != value) {
                throw IllegalArgumentException("Expected $value, got $jsonValue")
            }

            @Suppress("UNCHECKED_CAST")
            value as I
        }
        is RefinedSchema<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val baseSchema = (this as RefinedSchema<*, *>).getBaseSchema()
            @Suppress("UNCHECKED_CAST")
            baseSchema.parseJson(json) as I
        }
        is TransformedSchema<*, *, *> -> {
            @Suppress("UNCHECKED_CAST")
            val baseSchema = (this as TransformedSchema<*, *, *>).getBaseSchema()
            @Suppress("UNCHECKED_CAST")
            baseSchema.parseJson(json) as I
        }
        is DefaultSchema<*, *> -> {
            if (json == JsonNull) {
                @Suppress("UNCHECKED_CAST")
                return (this as DefaultSchema<*, *>).getDefaultValue() as I
            }

            @Suppress("UNCHECKED_CAST")
            val baseSchema = (this as DefaultSchema<*, *>).getBaseSchema()
            @Suppress("UNCHECKED_CAST")
            baseSchema.parseJson(json) as I
        }
        else -> {
            // Try to parse as a generic value
            @Suppress("UNCHECKED_CAST")
            when (json) {
                is JsonPrimitive -> {
                    when {
                        json.isString -> json.content as I
                        json.content.toDoubleOrNull() != null -> json.content.toDouble() as I
                        json.content.toBooleanStrictOrNull() != null -> json.content.toBoolean() as I
                        else -> json.content as I
                    }
                }
                is JsonObject -> {
                    val result = mutableMapOf<String, Any?>()
                    for ((key, value) in json) {
                        result[key] = parseJson(value)
                    }
                    result as I
                }
                is JsonArray -> {
                    json.map { parseJson(it) } as I
                }
                else -> {
                    throw IllegalArgumentException("Cannot parse $json")
                }
            }
        }
    }
}

/**
 * 将输出转换为 JSON。
 */
fun <I, O> Schema<I, O>.toJson(output: O): JsonElement {
    return when (this) {
        is StringSchema -> JsonPrimitive(output as String)
        is NumberSchema -> JsonPrimitive((output as Number).toDouble())
        is BooleanSchema -> JsonPrimitive(output as Boolean)
        is ObjectSchema<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val map = output as Map<String, Any?>
            JsonObject(
                map.mapValues { (key, value) ->
                    val field = fields[key]
                    if (field != null) {
                        @Suppress("UNCHECKED_CAST")
                        (field.schema as Schema<Any?, Any?>).toJson(value)
                    } else if (catchall != null) {
                        @Suppress("UNCHECKED_CAST")
                        (catchall as Schema<Any?, Any?>).toJson(value)
                    } else {
                        Json.encodeToJsonElement(value)
                    }
                }
            )
        }
        is ArraySchema<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val list = output as List<Any?>
            JsonArray(
                list.map { value ->
                    @Suppress("UNCHECKED_CAST")
                    (elementSchema as Schema<Any?, Any?>).toJson(value)
                }
            )
        }
        is TupleSchema -> {
            @Suppress("UNCHECKED_CAST")
            val list = output as List<Any?>
            JsonArray(
                list.mapIndexed { index, value ->
                    if (index < schemas.size) {
                        @Suppress("UNCHECKED_CAST")
                        (schemas[index] as Schema<Any?, Any?>).toJson(value)
                    } else if (rest != null) {
                        @Suppress("UNCHECKED_CAST")
                        (rest as Schema<Any?, Any?>).toJson(value)
                    } else {
                        Json.encodeToJsonElement(value)
                    }
                }
            )
        }
        is EnumSchema<*> -> JsonPrimitive(output.toString())
        is LiteralSchema<*> -> {
            when (output) {
                is String -> JsonPrimitive(output)
                is Number -> JsonPrimitive(output.toDouble())
                is Boolean -> JsonPrimitive(output)
                else -> JsonPrimitive(output.toString())
            }
        }
        is RefinedSchema<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            (this as RefinedSchema<*, *>).getBaseSchema().toJson(output)
        }
        is TransformedSchema<*, *, *> -> {
            @Suppress("UNCHECKED_CAST")
            (this as TransformedSchema<*, *, *>).getBaseSchema().toJson(output)
        }
        is DefaultSchema<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            (this as DefaultSchema<*, *>).getBaseSchema().toJson(output)
        }
        else -> {
            // Try to convert to JSON
            when (output) {
                is String -> JsonPrimitive(output)
                is Number -> JsonPrimitive(output.toDouble())
                is Boolean -> JsonPrimitive(output)
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val map = output as Map<String, Any?>
                    JsonObject(
                        map.mapValues { (_, value) ->
                            Json.encodeToJsonElement(value)
                        }
                    )
                }
                is List<*> -> {
                    JsonArray(
                        output.map { value ->
                            Json.encodeToJsonElement(value)
                        }
                    )
                }
                else -> {
                    JsonPrimitive(output.toString())
                }
            }
        }
    }
}

/**
 * 辅助函数：解析 JSON 元素
 */
private fun parseJson(json: JsonElement): Any? {
    return when (json) {
        is JsonPrimitive -> {
            when {
                json.isString -> json.content
                json.content.toDoubleOrNull() != null -> json.content.toDouble()
                json.content.toBooleanStrictOrNull() != null -> json.content.toBoolean()
                else -> json.content
            }
        }
        is JsonObject -> {
            val result = mutableMapOf<String, Any?>()
            for ((key, value) in json) {
                result[key] = parseJson(value)
            }
            result
        }
        is JsonArray -> {
            json.map { parseJson(it) }
        }
        else -> {
            null
        }
    }
}
