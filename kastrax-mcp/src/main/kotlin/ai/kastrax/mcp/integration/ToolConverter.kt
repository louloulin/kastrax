package ai.kastrax.mcp.integration

import ai.kastrax.core.tools.Tool as KastraXTool
import ai.kastrax.mcp.client.MCPClient
import ai.kastrax.mcp.exception.MCPException
import ai.kastrax.mcp.protocol.MCPErrorCodes
import ai.kastrax.mcp.protocol.Tool as MCPTool
import kotlinx.serialization.json.*

/**
 * 工具转换器，用于在KastraX工具和MCP工具之间进行转换
 */
object ToolConverter {
    /**
     * 将任意值转换为JsonElement
     */
    fun convertToJsonElement(value: Any?): JsonElement {
        return when (value) {
            is JsonElement -> value
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            is Map<*, *> -> {
                buildJsonObject {
                    @Suppress("UNCHECKED_CAST")
                    (value as Map<String, Any?>).forEach { (k, v) ->
                        put(k, convertToJsonElement(v))
                    }
                }
            }
            is List<*> -> {
                buildJsonArray {
                    value.forEach { item ->
                        add(convertToJsonElement(item ?: JsonNull))
                    }
                }
            }
            is Array<*> -> {
                buildJsonArray {
                    value.forEach { item ->
                        add(convertToJsonElement(item ?: JsonNull))
                    }
                }
            }
            null -> JsonNull
            else -> {
                // 尝试将对象转换为字符串
                try {
                    JsonPrimitive(value.toString())
                } catch (e: Exception) {
                    throw MCPException(MCPErrorCodes.INTERNAL_ERROR, "不支持的类型: ${value::class.java.name}")
                }
            }
        }
    }

    /**
     * 将JsonElement转换为Map<String, Any?>
     */
    fun jsonElementToMap(element: JsonElement): Map<String, Any?> {
        return when (element) {
            is JsonObject -> element.mapValues { (_, value) -> jsonElementToAny(value) }
            else -> throw MCPException(MCPErrorCodes.INVALID_PARAMS, "需要JsonObject类型，实际是${element::class.java.name}")
        }
    }

    /**
     * 将JsonElement转换为原生类型
     */
    fun jsonElementToAny(element: JsonElement): Any? {
        return when (element) {
            is JsonObject -> element.mapValues { (_, value) -> jsonElementToAny(value) }
            is JsonArray -> element.map { jsonElementToAny(it) }
            is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.booleanOrNull != null -> element.boolean
                    element.intOrNull != null -> element.int
                    element.longOrNull != null -> element.long
                    element.doubleOrNull != null -> element.double
                    else -> element.content
                }
            }
            JsonNull -> null
        }
    }

    /**
     * 将MCP模式转换为JSON Schema
     */
    fun convertMCPSchemaToJsonSchema(schema: JsonElement): JsonElement {
        // 实现从 MCP 参数定义到 JSON Schema 的转换
        return buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                // 处理参数
                if (schema is JsonObject) {
                    schema.forEach { (paramName, paramDef) ->
                        if (paramDef is JsonObject) {
                            putJsonObject(paramName) {
                                // 复制类型信息
                                paramDef["type"]?.let { put("type", it) }
                                paramDef["description"]?.let { put("description", it) }
                                // 处理嵌套属性
                                paramDef["properties"]?.let { put("properties", it) }
                                paramDef["items"]?.let { put("items", it) }
                            }
                        }
                    }
                }
            }
            // 添加必需字段
            putJsonArray("required") {
                if (schema is JsonObject) {
                    schema.forEach { (paramName, paramDef) ->
                        if (paramDef is JsonObject && paramDef["required"]?.jsonPrimitive?.boolean == true) {
                            add(paramName)
                        }
                    }
                }
            }
        }
    }
}
