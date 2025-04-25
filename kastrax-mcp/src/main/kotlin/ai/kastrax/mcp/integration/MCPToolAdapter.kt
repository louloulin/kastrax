package ai.kastrax.mcp.integration

import ai.kastrax.core.tools.Tool as KastraXTool
import ai.kastrax.mcp.client.MCPClient
import ai.kastrax.mcp.exception.MCPException
import ai.kastrax.mcp.protocol.Tool as MCPTool
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.*

private val logger = KotlinLogging.logger {}

/**
 * MCP工具适配器，将MCP工具包装为KastraX工具
 */
class MCPToolAdapter(private val mcpClient: MCPClient, private val mcpTool: MCPTool) : KastraXTool {
    override val id: String = mcpTool.id
    override val name: String = mcpTool.name
    override val description: String = mcpTool.description
    override val inputSchema: JsonElement = ToolConverter.convertMCPSchemaToJsonSchema(mcpTool.parameters.toJsonElement())
    override val outputSchema: JsonElement? = null // MCP工具没有输出模式

    override suspend fun execute(input: JsonElement): JsonElement {
        try {
            // 将JsonElement转换为Map<String, Any>
            val params = ToolConverter.jsonElementToMap(input)

            // 调用MCP工具
            val result = mcpClient.callTool(mcpTool.id, params.filterValues { it != null }.mapValues { it.value as Any })

            // 尝试将结果解析为JSON
            return try {
                Json.parseToJsonElement(result)
            } catch (e: Exception) {
                JsonPrimitive(result)
            }
        } catch (e: MCPException) {
            logger.error(e) { "MCP error when executing tool ${mcpTool.id}: ${e.message}" }
            return buildJsonObject {
                put("error", JsonPrimitive(e.message))
                put("code", JsonPrimitive(e.code))
                if (e.data != null) {
                    put("data", JsonPrimitive(e.data))
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to execute tool: ${mcpTool.id}" }
            return buildJsonObject {
                put("error", JsonPrimitive(e.message ?: "Unknown error"))
            }
        }
    }

    override suspend fun executeWithContext(
        input: JsonElement,
        threadId: String?,
        resourceId: String?
    ): JsonElement {
        // MCP工具不支持上下文，直接调用execute
        return execute(input)
    }
}
