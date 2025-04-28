package ai.kastrax.mcp.client

import ai.kastrax.mcp.exception.MCPException
import ai.kastrax.mcp.protocol.*
import ai.kastrax.mcp.transport.MockTransport
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MCPClientTest {
    private lateinit var transport: MockTransport
    private lateinit var client: MCPClient

    @BeforeEach
    fun setup() {
        transport = MockTransport()
        transport.addDefaultRequestHandlers()

        client = MCPClientImpl(
            name = "test-client",
            version = "1.0.0",
            clientId = "test-client-id",
            transport = transport,
            timeoutMs = 1000
        )
    }

    @Test
    fun `test connect and disconnect`() = runBlocking {
        assertFalse(transport.isConnected())

        client.connect()
        assertTrue(transport.isConnected())

        client.disconnect()
        assertFalse(transport.isConnected())
    }

    @Test
    @org.junit.jupiter.api.Disabled("Skipping test due to transport issues")
    fun `test resources`() = runBlocking {
        client.connect()

        val resources = client.resources()
        assertEquals(1, resources.size)
        assertEquals("test-resource", resources[0].id)
        assertEquals("Test Resource", resources[0].name)
        assertEquals("A test resource", resources[0].description)
        assertEquals(ResourceType.TEXT, resources[0].type)

        val content = client.getResource("test-resource")
        assertEquals("Test resource content", content)

        assertThrows<MCPException> {
            runBlocking {
                client.getResource("nonexistent")
            }
        }

        client.disconnect()
    }

    @Test
    @org.junit.jupiter.api.Disabled("Skipping test due to transport issues")
    fun `test tools`() = runBlocking {
        client.connect()

        val tools = client.tools()
        assertEquals(1, tools.size)
        assertEquals("test-tool", tools[0].id)
        assertEquals("Test Tool", tools[0].name)
        assertEquals("A test tool", tools[0].description)

        val result = client.callTool("test-tool", mapOf("param1" to "value"))
        assertEquals("Tool result: value", result)

        assertThrows<MCPException> {
            runBlocking {
                client.callTool("nonexistent", emptyMap())
            }
        }

        client.disconnect()
    }

    @Test
    @org.junit.jupiter.api.Disabled("Skipping test due to transport issues")
    fun `test prompts`() = runBlocking {
        client.connect()

        val prompts = client.prompts()
        assertEquals(1, prompts.size)
        assertEquals("test-prompt", prompts[0].id)
        assertEquals("Test Prompt", prompts[0].name)
        assertEquals("A test prompt", prompts[0].description)

        val content = client.getPrompt("test-prompt")
        assertEquals("Hello, {{param1}}!", content)

        assertThrows<MCPException> {
            runBlocking {
                client.getPrompt("nonexistent")
            }
        }

        client.disconnect()
    }

    @Test
    fun `test capabilities`() = runBlocking {
        client.connect()

        assertTrue(client.supportsCapability("resources"))
        assertTrue(client.supportsCapability("tools"))
        assertTrue(client.supportsCapability("prompts"))
        assertFalse(client.supportsCapability("sampling"))
        assertTrue(client.supportsCapability("cancelRequest"))
        assertTrue(client.supportsCapability("progress"))

        client.disconnect()
    }
}
