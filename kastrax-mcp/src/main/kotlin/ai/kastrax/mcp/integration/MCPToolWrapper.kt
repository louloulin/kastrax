package ai.kastrax.mcp.integration

import ai.kastrax.core.tools.Tool
import ai.kastrax.mcp.client.MCPClient
import ai.kastrax.mcp.exception.MCPException
import ai.kastrax.mcp.protocol.MCPErrorCodes
import ai.kastrax.mcp.protocol.Tool as MCPTool
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.*

private val logger = KotlinLogging.logger {}

/**
 * MCP 工具包装器
 *
 * 将 MCP 工具包装为 KastraX 工具
 */
class MCPToolWrapper(
    private val mcpClient: MCPClient,
    private val mcpTool: MCPTool
) : Tool {
    override val id: String = mcpTool.id
    override val name: String = mcpTool.name
    override val description: String = mcpTool.description

    // 创建输入模式的 JSON 表示
    override val inputSchema: JsonElement = mcpTool.parameters.toJsonElement()

    // 创建输出模式的 JSON 表示（如果有）
    override val outputSchema: JsonElement? = mcpTool.returns?.let {
        buildJsonObject {
            put("type", JsonPrimitive(it.type))
            put("description", JsonPrimitive(it.description))
        }
    }

    override suspend fun execute(input: JsonElement): JsonElement {
        return try {
            // 将 JSON 输入转换为 Map<String, Any?>
            val parameters = when (input) {
                is JsonObject -> input.mapValues { (_, value) ->
                    when (value) {
                        is JsonPrimitive -> {
                            when {
                                value.isString -> value.content
                                value.booleanOrNull != null -> value.boolean
                                value.intOrNull != null -> value.int
                                value.longOrNull != null -> value.long
                                value.doubleOrNull != null -> value.double
                                else -> value.toString()
                            }
                        }
                        is JsonObject -> value.toString()
                        is JsonArray -> value.toString()
                        else -> value.toString()
                    }
                }
                else -> throw IllegalArgumentException("Input must be a JSON object")
            }

            // 调用 MCP 工具
            val result = mcpClient.callTool(mcpTool.id, parameters)

            // 将结果转换为 JsonElement
            ToolConverter.convertToJsonElement(result)
        } catch (e: MCPException) {
            buildJsonObject {
                put("error", JsonPrimitive(when (e.code) {
                    MCPErrorCodes.TOOL_NOT_FOUND -> "Tool not found: ${mcpTool.id}"
                    MCPErrorCodes.INVALID_PARAMS -> "Invalid parameters: ${e.message}"
                    else -> "MCP error: ${e.message}"
                }))
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to execute tool: ${mcpTool.id}" }
            buildJsonObject {
                put("error", JsonPrimitive("Failed to call tool: ${e.message}"))
            }
        }
    }

    /**
     * 将 Map 转换为 JsonObject
     */
    private fun mapToJsonObject(map: Map<String, Any?>): JsonObject {
        return ToolConverter.convertToJsonElement(map).jsonObject
    }

    /**
     * 将 List 转换为 JsonArray
     */
    private fun listToJsonArray(list: List<*>): JsonArray {
        return ToolConverter.convertToJsonElement(list).jsonArray
    }
}
