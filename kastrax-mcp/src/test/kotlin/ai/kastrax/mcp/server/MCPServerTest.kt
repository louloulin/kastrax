package ai.kastrax.mcp.server

import ai.kastrax.mcp.protocol.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MCPServerTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `test server builder`() {
        val server = mcpServer {
            name("test-server")
            version("1.0.0")

            resource {
                name = "test-resource"
                description = "A test resource"
                content = "Test resource content"
            }

            tool {
                name = "test-tool"
                description = "A test tool"
                parameters {
                    parameter {
                        name = "param1"
                        type = "string"
                        description = "Parameter 1"
                        required = true
                    }
                }
                handler { params ->
                    "Tool result: ${params["param1"]}"
                }
            }

            prompt {
                name = "test-prompt"
                description = "A test prompt"
                content = "Hello, {{param1}}!"
                parameters {
                    parameter {
                        name = "param1"
                        type = "string"
                        description = "Parameter 1"
                        required = true
                    }
                }
            }
        }

        assertEquals("test-server", server.name)
        assertEquals("1.0.0", server.version)

        val resources = server.getResources()
        assertEquals(1, resources.size)
        assertEquals("test-resource", resources[0].id)

        val tools = server.getTools()
        assertEquals(1, tools.size)
        assertEquals("test-tool", tools[0].id)

        val prompts = server.getPrompts()
        assertEquals(1, prompts.size)
        assertEquals("test-prompt", prompts[0].id)
    }

    @Test
    fun `test handle initialize request`() = runBlocking {
        val server = MCPServerImpl("test-server", "1.0.0")

        val request = MCPRequest(
            id = "123",
            method = MCPMethods.INITIALIZE,
            params = json.encodeToJsonElement(
                InitializeParams(
                    name = "test-client",
                    version = "1.0.0",
                    capabilities = ClientCapabilities(
                        resources = true,
                        tools = true,
                        prompts = true
                    )
                )
            )
        )

        val response = handleRequest(server, request)

        assertEquals("123", response.id)
        assertNotNull(response.result)
        assertNull(response.error)

        val result = json.decodeFromJsonElement(InitializeResult.serializer(), response.result!!)
        assertEquals("test-server", result.name)
        assertEquals("1.0.0", result.version)
        assertFalse(result.capabilities.resources)
        assertFalse(result.capabilities.tools)
        assertFalse(result.capabilities.prompts)
    }

    @Test
    fun `test handle list resources request`() = runBlocking {
        val server = MCPServerImpl("test-server", "1.0.0")

        val resource = Resource(
            id = "test-resource",
            name = "Test Resource",
            description = "A test resource",
            type = ResourceType.TEXT
        )

        server.registerResource(resource)

        val request = MCPRequest(
            id = "123",
            method = MCPMethods.LIST_RESOURCES,
            params = null
        )

        val response = handleRequest(server, request)

        assertEquals("123", response.id)
        assertNotNull(response.result)
        assertNull(response.error)

        val result = json.decodeFromJsonElement(ListResourcesResult.serializer(), response.result!!)
        assertEquals(1, result.resources.size)
        assertEquals("test-resource", result.resources[0].id)
    }

    @Test
    fun `test handle get resource request`() = runBlocking {
        val server = MCPServerImpl("test-server", "1.0.0")

        val resource = Resource(
            id = "test-resource",
            name = "Test Resource",
            description = "A test resource",
            type = ResourceType.TEXT
        )

        server.registerResource(resource)
        server.registerResourceContentProvider(
            "test-resource",
            StaticResourceContentProvider(
                mapOf("test-resource" to "Test resource content")
            )
        )

        val request = MCPRequest(
            id = "123",
            method = MCPMethods.GET_RESOURCE,
            params = json.encodeToJsonElement(
                GetResourceParams(
                    id = "test-resource"
                )
            )
        )

        val response = handleRequest(server, request)

        assertEquals("123", response.id)
        assertNotNull(response.result)
        assertNull(response.error)

        val result = json.decodeFromJsonElement(GetResourceResult.serializer(), response.result!!)
        assertEquals("Test resource content", result.content)
    }

    @Test
    fun `test handle list tools request`() = runBlocking {
        val server = MCPServerImpl("test-server", "1.0.0")

        val tool = Tool(
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

        server.registerTool(tool)

        val request = MCPRequest(
            id = "123",
            method = MCPMethods.LIST_TOOLS,
            params = null
        )

        val response = handleRequest(server, request)

        assertEquals("123", response.id)
        assertNotNull(response.result)
        assertNull(response.error)

        val result = json.decodeFromJsonElement(ListToolsResult.serializer(), response.result!!)
        assertEquals(1, result.tools.size)
        assertEquals("test-tool", result.tools[0].id)
    }

    @Test
    fun `test handle call tool request`() = runBlocking {
        val server = MCPServerImpl("test-server", "1.0.0")

        val tool = Tool(
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

        server.registerTool(tool)
        server.registerToolHandler(
            "test-tool",
            object : ToolHandler {
                override suspend fun handleToolCall(toolId: String, parameters: Map<String, Any>): String {
                    return "Tool result: ${parameters["param1"]}"
                }
            }
        )

        val request = MCPRequest(
            id = "123",
            method = MCPMethods.CALL_TOOL,
            params = json.encodeToJsonElement(
                CallToolParams(
                    id = "test-tool",
                    parameters = JsonPrimitive("value")
                )
            )
        )

        val response = handleRequest(server, request)

        assertEquals("123", response.id)
        assertNotNull(response.result)
        assertNull(response.error)

        val result = json.decodeFromJsonElement(CallToolResult.serializer(), response.result!!)
        assertEquals("Tool result: value", result.result)
    }

    /**
     * 处理请求
     */
    private suspend fun handleRequest(server: MCPServerImpl, request: MCPRequest): MCPResponse {
        // 使用反射调用私有方法
        val method = MCPServerImpl::class.java.getDeclaredMethod("handleRequest", MCPRequest::class.java)
        method.isAccessible = true
        return method.invoke(server, request) as MCPResponse
    }
}
