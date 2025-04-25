package ai.kastrax.mcp.integration

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.tool.ToolCall
import ai.kastrax.core.tool.ToolResult
import ai.kastrax.mcp.client.MCPClient
import ai.kastrax.mcp.protocol.Resource
import ai.kastrax.mcp.protocol.ResourceType
import ai.kastrax.mcp.protocol.Tool
import ai.kastrax.mcp.protocol.ToolParameters
import ai.kastrax.mcp.protocol.ToolParameterProperty
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AgentIntegrationTest {
    @Test
    fun `test MCPToolWrapper`() = runBlocking {
        // 创建模拟 MCP 客户端
        val mcpClient = mock(MCPClient::class.java)

        // 创建 MCP 工具
        val mcpTool = Tool(
            id = "test-tool",
            name = "Test Tool",
            description = "A test tool",
            parameters = ToolParameters(
                type = "object",
                required = listOf("param1"),
                properties = mapOf(
                    "param1" to ToolParameterProperty(
                        type = "string",
                        description = "Parameter 1"
                    )
                )
            )
        )

        // 设置模拟行为
        `when`(mcpClient.callTool("test-tool", mapOf("param1" to "value")))
            .thenReturn("Tool result: value")

        // 创建 MCP 工具包装器
        val toolWrapper = MCPToolWrapper(mcpClient, mcpTool)

        // 测试工具调用
        val toolCall = ToolCall(
            id = "123",
            name = "test-tool",
            arguments = mapOf("param1" to "value")
        )

        val result = toolWrapper.call(toolCall)

        // 验证结果
        assert(result is ToolResult.Success)
        assertEquals("Tool result: value", (result as ToolResult.Success).result)
    }

    @Test
    fun `test MCPToolset`() = runBlocking {
        // 创建模拟 MCP 客户端
        val mcpClient = mock(MCPClient::class.java)

        // 创建 MCP 工具
        val mcpTool = Tool(
            id = "test-tool",
            name = "Test Tool",
            description = "A test tool",
            parameters = ToolParameters(
                type = "object",
                required = listOf("param1"),
                properties = mapOf(
                    "param1" to ToolParameterProperty(
                        type = "string",
                        description = "Parameter 1"
                    )
                )
            )
        )

        // 设置模拟行为
        `when`(mcpClient.tools())
            .thenReturn(listOf(mcpTool))

        // 创建 MCP 工具集
        val toolset = MCPToolset(mcpClient)
        toolset.initialize()

        // 测试获取工具
        val tools = toolset.getTools()
        assertEquals(1, tools.size)
        assertEquals("test-tool", tools[0].id)
        assertEquals("Test Tool", tools[0].name)
        assertEquals("A test tool", tools[0].description)

        // 测试获取特定工具
        val tool = toolset.getTool("test-tool")
        assertNotNull(tool)
        assertEquals("test-tool", tool.id)
        assertEquals("Test Tool", tool.name)
        assertEquals("A test tool", tool.description)
    }

    @Test
    fun `test agent extension functions`() = runBlocking {
        // 创建模拟 MCP 客户端
        val mcpClient = mock(MCPClient::class.java)

        // 创建 MCP 工具
        val mcpTool = Tool(
            id = "test-tool",
            name = "Test Tool",
            description = "A test tool",
            parameters = ToolParameters(
                type = "object",
                required = listOf("param1"),
                properties = mapOf(
                    "param1" to ToolParameterProperty(
                        type = "string",
                        description = "Parameter 1"
                    )
                )
            )
        )

        // 设置模拟行为
        `when`(mcpClient.isConnected())
            .thenReturn(true)
        `when`(mcpClient.tools())
            .thenReturn(listOf(mcpTool))

        // 创建模拟代理
        val agent = mock(Agent::class.java)
        val agentBuilder = mock(Agent.Builder::class.java)

        // 设置模拟行为
        `when`(agentBuilder.tool(org.mockito.ArgumentMatchers.any()))
            .thenReturn(agentBuilder)

        // 测试 mcpTools 扩展函数
        agentBuilder.mcpTools(mcpClient)

        // 创建 MCP 工具集
        val toolset = MCPToolset(mcpClient)
        toolset.initialize()

        // 设置模拟行为
        `when`(agent.generate(
            "test prompt",
            mapOf("test-tool" to toolset.getTool("test-tool")!!),
            AgentGenerateOptions()
        )).thenReturn(
            Agent.Response(
                text = "Test response",
                toolCalls = emptyList()
            )
        )

        // 测试 generate 扩展函数
        val response = agent.generate(
            "test prompt",
            mapOf("test" to toolset),
            AgentGenerateOptions()
        )

        // 验证结果
        assertEquals("Test response", response.text)
        assertEquals(0, response.toolCalls.size)
    }
}
