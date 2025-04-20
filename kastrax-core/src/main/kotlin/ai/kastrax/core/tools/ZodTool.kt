package ai.kastrax.core.tools

import ai.kastrax.zod.Schema
import ai.kastrax.zod.parseJson
import ai.kastrax.zod.toJson
import ai.kastrax.zod.toJsonSchema
import kotlinx.serialization.json.*

/**
 * 将任意对象转换为 JsonElement。
 */
private fun anyToJsonElement(value: Any?): JsonElement {
    return when (value) {
        null -> JsonNull
        is JsonElement -> value
        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Map<*, *> -> {
            val map = value as Map<String, Any?>
            JsonObject(map.mapValues { (_, v) -> anyToJsonElement(v) })
        }
        is List<*> -> JsonArray(value.map { anyToJsonElement(it) })
        is Array<*> -> JsonArray(value.map { anyToJsonElement(it) })
        else -> JsonPrimitive(value.toString())
    }
}

/**
 * 使用 kastrax-zod 的工具接口。
 */
interface ZodTool<I, O> {
    /**
     * 工具的唯一标识符。
     */
    val id: String

    /**
     * 工具的名称。
     */
    val name: String

    /**
     * 工具的描述。
     */
    val description: String

    /**
     * 输入模式。
     */
    val inputSchema: Schema<I, I>

    /**
     * 输出模式（可选）。
     */
    val outputSchema: Schema<O, O>?

    /**
     * 执行工具。
     */
    suspend fun execute(input: I): O

    /**
     * 带上下文执行工具。
     */
    suspend fun executeWithContext(
        input: I,
        threadId: String? = null,
        resourceId: String? = null
    ): O {
        // 默认实现调用常规的 execute 方法
        return execute(input)
    }

    /**
     * 将此 ZodTool 转换为传统的 Tool。
     */
    fun toTool(): Tool {
        return object : Tool {
            override val id: String = this@ZodTool.id
            override val name: String = this@ZodTool.name
            override val description: String = this@ZodTool.description
            override val inputSchema: JsonElement = this@ZodTool.inputSchema.toJsonSchema()
            override val outputSchema: JsonElement? = this@ZodTool.outputSchema?.toJsonSchema()

            override suspend fun execute(input: JsonElement): JsonElement {
                val parsedInput = this@ZodTool.inputSchema.parseJson(input)
                val result = this@ZodTool.execute(parsedInput)
                return if (this@ZodTool.outputSchema != null) {
                    @Suppress("UNCHECKED_CAST")
                    (this@ZodTool.outputSchema as Schema<Any?, O>).toJson(result)
                } else {
                    anyToJsonElement(result)
                }
            }

            override suspend fun executeWithContext(
                input: JsonElement,
                threadId: String?,
                resourceId: String?
            ): JsonElement {
                val parsedInput = this@ZodTool.inputSchema.parseJson(input)
                val result = this@ZodTool.executeWithContext(parsedInput, threadId, resourceId)
                return if (this@ZodTool.outputSchema != null) {
                    @Suppress("UNCHECKED_CAST")
                    (this@ZodTool.outputSchema as Schema<Any?, O>).toJson(result)
                } else {
                    anyToJsonElement(result)
                }
            }
        }
    }
}

/**
 * ZodTool 构建器。
 */
class ZodToolBuilder<I, O> {
    var id: String = ""
    var name: String = ""
    var description: String = ""
    lateinit var inputSchema: Schema<I, I>
    var outputSchema: Schema<O, O>? = null
    lateinit var execute: suspend (I) -> O
    var executeWithContext: (suspend (I, String?, String?) -> O)? = null

    /**
     * 从构建器配置构建 ZodTool 实例。
     */
    fun build(): ZodTool<I, O> {
        require(id.isNotEmpty()) { "Tool ID must not be empty" }
        require(name.isNotEmpty()) { "Tool name must not be empty" }
        require(description.isNotEmpty()) { "Tool description must not be empty" }

        return object : ZodTool<I, O> {
            override val id: String = this@ZodToolBuilder.id
            override val name: String = this@ZodToolBuilder.name
            override val description: String = this@ZodToolBuilder.description
            override val inputSchema: Schema<I, I> = this@ZodToolBuilder.inputSchema
            override val outputSchema: Schema<O, O>? = this@ZodToolBuilder.outputSchema

            override suspend fun execute(input: I): O {
                return this@ZodToolBuilder.execute(input)
            }

            override suspend fun executeWithContext(
                input: I,
                threadId: String?,
                resourceId: String?
            ): O {
                return if (this@ZodToolBuilder.executeWithContext != null) {
                    this@ZodToolBuilder.executeWithContext!!(input, threadId, resourceId)
                } else {
                    execute(input)
                }
            }
        }
    }
}

/**
 * 创建 ZodTool 的 DSL 函数。
 */
fun <I, O> zodTool(init: ZodToolBuilder<I, O>.() -> Unit): ZodTool<I, O> {
    val builder = ZodToolBuilder<I, O>()
    builder.init()
    return builder.build()
}

/**
 * 创建传统 Tool 的 DSL 函数（使用 ZodTool）。
 */
fun <I, O> zodToolAsLegacy(init: ZodToolBuilder<I, O>.() -> Unit): Tool {
    return zodTool(init).toTool()
}
