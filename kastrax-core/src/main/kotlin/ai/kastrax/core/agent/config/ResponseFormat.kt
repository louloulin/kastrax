package ai.kastrax.core.agent.config

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 扩展函数，将 ResponseFormat 转换为 JsonElement
 */
fun ResponseFormat.toJsonElement(): JsonElement = when (this) {
    is ResponseFormat.Json -> this.toJsonElement()
    is ResponseFormat.Text -> this.toJsonElement()
    is ResponseFormat.Markdown -> this.toJsonElement()
}

/**
 * 响应格式配置，控制模型生成的输出格式。
 */
sealed class ResponseFormat {
    /**
     * 将响应格式转换为 JsonElement。
     */
    abstract fun toJsonElement(): JsonElement

    /**
     * JSON 响应格式，要求模型以 JSON 格式输出。
     *
     * @property schema 可选的 JSON Schema，用于指定输出的结构
     */
    data class Json(val schema: JsonElement? = null) : ResponseFormat() {
        override fun toJsonElement(): JsonElement {
            return if (schema != null) {
                JsonObject(mapOf(
                    "type" to JsonPrimitive("json"),
                    "schema" to schema
                ))
            } else {
                JsonObject(mapOf(
                    "type" to JsonPrimitive("json")
                ))
            }
        }
    }

    /**
     * 文本响应格式，模型以纯文本格式输出。
     */
    object Text : ResponseFormat() {
        override fun toJsonElement(): JsonElement {
            return JsonObject(mapOf(
                "type" to JsonPrimitive("text")
            ))
        }
    }

    /**
     * Markdown 响应格式，模型以 Markdown 格式输出。
     */
    object Markdown : ResponseFormat() {
        override fun toJsonElement(): JsonElement {
            return JsonObject(mapOf(
                "type" to JsonPrimitive("markdown")
            ))
        }
    }

    companion object {
        /**
         * 创建 JSON 响应格式。
         */
        fun json(schema: JsonElement? = null): Json = Json(schema)

        /**
         * 创建文本响应格式。
         */
        fun text(): Text = Text

        /**
         * 创建 Markdown 响应格式。
         */
        fun markdown(): Markdown = Markdown
    }
}
