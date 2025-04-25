package ai.kastrax.mcp.integration

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentBuilder
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.tools.Tool
import ai.kastrax.mcp.client.MCPClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * MCP 工具包装器
 */
class MCPToolWrapper(
    private val mcpClient: MCPClient,
    private val mcpTool: ai.kastrax.mcp.protocol.Tool
) : Tool {
    override val id: String = mcpTool.id
    override val name: String = mcpTool.name
    override val description: String = mcpTool.description
    override val inputSchema: JsonElement = mcpTool.parameters.toJsonElement()
    override val outputSchema: JsonElement? = null

    override suspend fun execute(input: JsonElement): JsonElement {
        return try {
            val params = if (input is JsonObject) {
                input.entries.associate { it.key to it.value }
            } else {
                emptyMap()
            }
            val result = mcpClient.callTool(id, params)
            JsonPrimitive(result)
        } catch (e: Exception) {
            buildJsonObject {
                put("error", JsonPrimitive(e.message ?: "Unknown error"))
            }
        }
    }
}

/**
 * MCP 工具集
 */
class MCPToolset(private val mcpClient: MCPClient) {
    private val tools = mutableMapOf<String, Tool>()

    /**
     * 初始化工具集
     */
    suspend fun initialize() {
        val mcpTools = mcpClient.tools()
        for (mcpTool in mcpTools) {
            val tool = MCPToolWrapper(mcpClient, mcpTool)
            tools[tool.id] = tool
        }
    }

    /**
     * 获取所有工具
     */
    fun getTools(): List<Tool> {
        return tools.values.toList()
    }

    /**
     * 获取特定工具
     */
    fun getTool(id: String): Tool? {
        return tools[id]
    }
}

/**
 * 代理扩展函数 - 添加 MCP 工具
 */
suspend fun AgentBuilder.mcpTools(mcpClient: MCPClient) {
    if (mcpClient.isConnected()) {
        val toolset = MCPToolset(mcpClient)
        toolset.initialize()
        for (tool in toolset.getTools()) {
            tools {
                tool(tool)
            }
        }
    }
}

/**
 * 代理扩展函数 - 使用 MCP 工具集生成
 */
suspend fun Agent.generateWithMCPTools(
    prompt: String,
    toolsets: Map<String, MCPToolset>,
    options: AgentGenerateOptions = AgentGenerateOptions()
): AgentResponse {
    val toolMap = mutableMapOf<String, Map<String, Tool>>()
    for ((name, toolset) in toolsets) {
        toolMap[name] = toolset.getTools().associateBy { it.id }
    }
    val newOptions = options.copy(toolsets = toolMap)
    return generate(prompt, newOptions)
}
