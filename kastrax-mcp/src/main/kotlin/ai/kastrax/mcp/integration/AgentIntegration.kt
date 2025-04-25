package ai.kastrax.mcp.integration

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.tool.Tool
import ai.kastrax.core.tool.ToolCall
import ai.kastrax.core.tool.ToolResult
import ai.kastrax.mcp.client.MCPClient
import ai.kastrax.mcp.protocol.MCPErrorCodes
import ai.kastrax.mcp.protocol.MCPException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * MCP 工具集
 *
 * 包含从 MCP 客户端获取的工具
 */
class MCPToolset(
    private val mcpClient: MCPClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val tools = ConcurrentHashMap<String, ai.kastrax.mcp.protocol.Tool>()

    /**
     * 初始化工具集
     */
    suspend fun initialize() {
        // 获取 MCP 工具
        val mcpTools = mcpClient.tools()

        // 保存工具
        mcpTools.forEach { tool ->
            tools[tool.id] = tool
        }

        logger.info { "Initialized MCP toolset with ${tools.size} tools" }
    }

    /**
     * 获取工具列表
     */
    fun getTools(): List<Tool> {
        return tools.values.map { mcpTool ->
            MCPToolWrapper(mcpClient, mcpTool)
        }
    }

    /**
     * 获取工具
     */
    fun getTool(toolId: String): Tool? {
        val mcpTool = tools[toolId] ?: return null
        return MCPToolWrapper(mcpClient, mcpTool)
    }
}

/**
 * MCP 工具包装器
 *
 * 将 MCP 工具包装为 KastraX 工具
 */
class MCPToolWrapper(
    private val mcpClient: MCPClient,
    private val mcpTool: ai.kastrax.mcp.protocol.Tool
) : Tool {
    override val id: String = mcpTool.id
    override val name: String = mcpTool.name
    override val description: String = mcpTool.description

    override suspend fun call(toolCall: ToolCall): ToolResult {
        return try {
            // 将参数转换为 Map<String, Any>
            val parameters = toolCall.arguments.mapValues { (_, value) ->
                when (value) {
                    is String -> value
                    is Number -> value
                    is Boolean -> value
                    else -> value.toString()
                }
            }

            // 调用 MCP 工具
            val result = mcpClient.callTool(mcpTool.id, parameters)

            ToolResult.Success(result)
        } catch (e: MCPException) {
            when (e.code) {
                MCPErrorCodes.TOOL_NOT_FOUND -> ToolResult.Error("Tool not found: ${mcpTool.id}")
                MCPErrorCodes.INVALID_PARAMS -> ToolResult.Error("Invalid parameters: ${e.message}")
                else -> ToolResult.Error("MCP error: ${e.message}")
            }
        } catch (e: Exception) {
            ToolResult.Error("Failed to call tool: ${e.message}")
        }
    }
}

/**
 * 扩展函数：将 MCP 工具添加到代理
 */
suspend fun Agent.Builder.mcpTools(mcpClient: MCPClient) {
    // 初始化 MCP 客户端
    if (!mcpClient.isConnected()) {
        mcpClient.connect()
    }

    // 创建 MCP 工具集
    val mcpToolset = MCPToolset(mcpClient)
    mcpToolset.initialize()

    // 添加 MCP 工具
    mcpToolset.getTools().forEach { tool ->
        tool(tool)
    }
}

/**
 * 扩展函数：使用 MCP 工具集生成回答
 */
suspend fun Agent.generate(
    prompt: String,
    toolsets: Map<String, MCPToolset>,
    options: AgentGenerateOptions = AgentGenerateOptions()
): Agent.Response {
    // 创建工具映射
    val tools = mutableMapOf<String, Tool>()

    // 添加 MCP 工具
    toolsets.forEach { (prefix, toolset) ->
        toolset.getTools().forEach { tool ->
            tools["${prefix}.${tool.id}"] = tool
        }
    }

    // 生成回答
    return generate(prompt, tools, options)
}
