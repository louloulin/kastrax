package ai.kastrax.core.tools

import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.math.CalculatorTool
import ai.kastrax.zod.objectInput
import ai.kastrax.zod.objectOutput
import ai.kastrax.zod.stringField
import ai.kastrax.zod.numberField
import ai.kastrax.zod.booleanField
import org.slf4j.LoggerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * 简单工具工厂，提供基本的工具实现。
 */
object SimpleToolFactory {
    private val logger = LoggerFactory.getLogger(SimpleToolFactory::class.java)

    /**
     * 创建简单的计算器工具。
     */
    fun createCalculatorTool(): Tool {
        // 使用新的 CalculatorTool 实现
        val calculatorTool = CalculatorTool.create()

        // 包装为兼容旧测试的工具
        return object : Tool {
            override val id: String = calculatorTool.id
            override val name: String = calculatorTool.name
            override val description: String = calculatorTool.description
            override val inputSchema: JsonElement = calculatorTool.inputSchema
            override val outputSchema: JsonElement? = calculatorTool.outputSchema

            override suspend fun execute(input: JsonElement): JsonElement = withContext(Dispatchers.Default) {
                try {
                    val result = calculatorTool.execute(input)
                    if (result is JsonObject) {
                        // 转换为旧格式的输出
                        buildJsonObject {
                            put("result", result["result"] ?: JsonPrimitive(0.0))
                            put("success", true)
                            put("message", "计算成功")
                        }
                    } else {
                        // 如果不是 JsonObject，直接返回
                        result
                    }
                } catch (e: Exception) {
                    logger.error("计算失败", e)
                    buildJsonObject {
                        put("result", 0.0)
                        put("success", false)
                        put("message", "计算失败: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * 创建简单的文本处理工具。
     */
    fun createTextProcessingTool(): Tool {
        // 创建简单的文本处理工具
        return object : Tool {
            override val id: String = "text_processing"
            override val name: String = "文本处理"
            override val description: String = "执行基本的文本处理操作"

            override val inputSchema: JsonElement = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("operation") {
                        put("type", "string")
                        put("description", "要执行的操作")
                    }
                    putJsonObject("text") {
                        put("type", "string")
                        put("description", "要处理的文本")
                    }
                }
            }

            override val outputSchema: JsonElement? = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("result") {
                        put("type", "string")
                        put("description", "处理结果")
                    }
                    putJsonObject("success") {
                        put("type", "boolean")
                        put("description", "处理是否成功")
                    }
                    putJsonObject("message") {
                        put("type", "string")
                        put("description", "处理消息或错误信息")
                    }
                }
            }

            override suspend fun execute(input: JsonElement): JsonElement = withContext(Dispatchers.Default) {
                try {
                    // 处理不同格式的输入
                    val operation: String
                    val text: String

                    if (input is JsonObject) {
                        // 直接使用 JsonObject
                        operation = input["operation"]?.let {
                            when (it) {
                                is JsonPrimitive -> it.content
                                else -> it.toString()
                            }
                        } ?: throw IllegalArgumentException("Operation is required")

                        text = input["text"]?.let {
                            when (it) {
                                is JsonPrimitive -> it.content
                                else -> it.toString()
                            }
                        } ?: throw IllegalArgumentException("Text is required")
                    } else {
                        // 尝试解析为 Map
                        try {
                            val mapSerializer = MapSerializer(String.serializer(), String.serializer())
                            val map = Json.decodeFromJsonElement(mapSerializer, input)
                            operation = map["operation"]?.toString() ?: throw IllegalArgumentException("Operation is required")
                            text = map["text"]?.toString() ?: throw IllegalArgumentException("Text is required")
                        } catch (e: Exception) {
                            throw IllegalArgumentException("Invalid input format: ${e.message}")
                        }
                    }

                    val result = when (operation) {
                        "uppercase" -> text.uppercase()
                        "lowercase" -> text.lowercase()
                        "reverse" -> text.reversed()
                        "length" -> text.length.toString()
                        else -> throw IllegalArgumentException("未知操作: $operation")
                    }

                    buildJsonObject {
                        put("result", result)
                        put("success", true)
                        put("message", "处理成功")
                    }
                } catch (e: Exception) {
                    logger.error("文本处理失败", e)
                    buildJsonObject {
                        put("result", "")
                        put("success", false)
                        put("message", "处理失败: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * 创建简单的日期时间工具。
     */
    fun createDateTimeTool(): Tool {
        // 创建简单的日期时间工具
        return object : Tool {
            override val id: String = "date_time"
            override val name: String = "日期时间"
            override val description: String = "执行基本的日期时间操作"

            override val inputSchema: JsonElement = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("operation") {
                        put("type", "string")
                        put("description", "要执行的操作")
                    }
                }
            }

            override val outputSchema: JsonElement? = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("result") {
                        put("type", "string")
                        put("description", "操作结果")
                    }
                    putJsonObject("success") {
                        put("type", "boolean")
                        put("description", "操作是否成功")
                    }
                    putJsonObject("message") {
                        put("type", "string")
                        put("description", "操作消息或错误信息")
                    }
                }
            }

            override suspend fun execute(input: JsonElement): JsonElement = withContext(Dispatchers.Default) {
                try {
                    // 处理不同格式的输入
                    val operation: String

                    if (input is JsonObject) {
                        // 直接使用 JsonObject
                        operation = input["operation"]?.let {
                            when (it) {
                                is JsonPrimitive -> it.content
                                else -> it.toString()
                            }
                        } ?: throw IllegalArgumentException("Operation is required")
                    } else {
                        // 尝试解析为 Map
                        try {
                            val mapSerializer = MapSerializer(String.serializer(), String.serializer())
                            val map = Json.decodeFromJsonElement(mapSerializer, input)
                            operation = map["operation"]?.toString() ?: throw IllegalArgumentException("Operation is required")
                        } catch (e: Exception) {
                            throw IllegalArgumentException("Invalid input format: ${e.message}")
                        }
                    }

                    val result = when (operation) {
                        "current_time" -> java.time.LocalDateTime.now().toString()
                        "current_date" -> java.time.LocalDate.now().toString()
                        "current_timestamp" -> System.currentTimeMillis().toString()
                        else -> throw IllegalArgumentException("未知操作: $operation")
                    }

                    buildJsonObject {
                        put("result", result)
                        put("success", true)
                        put("message", "操作成功")
                    }
                } catch (e: Exception) {
                    logger.error("日期时间操作失败", e)
                    buildJsonObject {
                        put("result", "")
                        put("success", false)
                        put("message", "操作失败: ${e.message}")
                    }
                }
            }
        }
    }
}
