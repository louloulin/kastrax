package ai.kastrax.mcp.integration

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentBuilder
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.tools.Tool
import ai.kastrax.mcp.client.MCPClient
import ai.kastrax.mcp.exception.MCPException
import ai.kastrax.mcp.protocol.MCPErrorCodes
import ai.kastrax.mcp.protocol.Tool as MCPTool
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * MCP 工具集
 *
 * 包含从 MCP 客户端获取的工具
 */
class MCPToolset(
    val mcpClient: MCPClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val tools = ConcurrentHashMap<String, MCPTool>()

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
    fun getTools(): List<MCPTool> {
        return tools.values.toList()
    }

    /**
     * 获取工具
     */
    fun getTool(toolId: String): MCPTool? {
        return tools[toolId]
    }
}



/**
 * 扩展函数：将 MCP 工具添加到代理
 */
suspend fun AgentBuilder.mcpTools(mcpClient: MCPClient) {
    // 初始化 MCP 客户端
    if (!mcpClient.isConnected()) {
        mcpClient.connect()
    }

    // 创建 MCP 工具集
    val mcpToolset = MCPToolset(mcpClient)
    mcpToolset.initialize()

    // 添加 MCP 工具
    mcpClient.tools().forEach { mcpTool ->
        // 创建工具包装器
        val wrapper = MCPToolAdapter(mcpClient, mcpTool)
        // 添加工具
        tools {
            tool(wrapper)
        }
    }
}

/**
 * 扩展函数：使用 MCP 工具集生成回答
 */
suspend fun Agent.generate(
    prompt: String,
    toolsets: Map<String, MCPToolset>,
    options: AgentGenerateOptions = AgentGenerateOptions()
): AgentResponse {
    // 创建工具映射
    val toolMap = mutableMapOf<String, ai.kastrax.core.tools.Tool>()

    // 添加 MCP 工具
    toolsets.forEach { (prefix, toolset) ->
        toolset.mcpClient.tools().forEach { mcpTool ->
            // 创建工具包装器
            val wrapper = MCPToolAdapter(toolset.mcpClient, mcpTool)
            // 添加工具
            toolMap["${prefix}.${mcpTool.id}"] = wrapper
        }
    }

    // 生成回答
    return generate(prompt, options.copy(toolsets = mapOf("mcp" to toolMap)))
}
