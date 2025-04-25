package ai.kastrax.mcp.examples

import ai.kastrax.mcp.client.mcpClient
import ai.kastrax.mcp.protocol.MCPMessage
import ai.kastrax.mcp.protocol.MCPRequest
import ai.kastrax.mcp.protocol.MCPResponse
import ai.kastrax.mcp.protocol.Tool
import ai.kastrax.mcp.protocol.ToolParameters
import ai.kastrax.mcp.protocol.ToolParameterProperty
import ai.kastrax.mcp.transport.Transport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Remote MCP Example
 * This example demonstrates how to use the Kastrax MCP client to connect to a remote MCP server.
 * Since we don't have a public MCP server available, we'll simulate one using a custom transport.
 */
fun main() = runBlocking {
    println("Starting Remote MCP Example...")

    // Create a simulated remote MCP server transport
    val remoteTransport = SimulatedRemoteTransport()

    // Create MCP client directly with the transport
    val client = ai.kastrax.mcp.client.MCPClientImpl("RemoteMCPClient", "1.0.0", remoteTransport)

    try {
        // Connect to server
        println("Connecting to remote MCP server...")
        client.connect()
        println("Connected to remote MCP server")

        // Get server information
        println("\nServer information:")
        println("Name: ${client.name}")
        println("Version: ${client.version}")
        println("Capabilities:")
        println("- Resources: ${client.supportsCapability("resources")}")
        println("- Tools: ${client.supportsCapability("tools")}")
        println("- Prompts: ${client.supportsCapability("prompts")}")

        // Get available tools
        val tools = client.tools()
        println("\nAvailable tools:")
        tools.forEach { tool ->
            println("- ${tool.name}: ${tool.description}")
            println("  Parameters:")
            tool.parameters.properties.forEach { (name, prop) ->
                println("    - $name: ${prop.description} (${prop.type}${if (tool.parameters.required.contains(name)) ", required" else ""})")
            }
        }

        // Call tools
        println("\nCalling echo tool...")
        try {
            val echoResult = client.callTool("echo", mapOf("message" to "Hello, Remote MCP Server!"))
            println("Echo result: $echoResult")
        } catch (e: Exception) {
            println("Error calling echo tool: ${e.message}")
        }

        println("\nCalling random_number tool...")
        try {
            val randomResult = client.callTool("random_number", mapOf("min" to "1", "max" to "100"))
            println("Random number result: $randomResult")
        } catch (e: Exception) {
            println("Error calling random_number tool: ${e.message}")
        }

        println("\nCalling current_time tool...")
        try {
            val timeResult = client.callTool("current_time", emptyMap())
            println("Current time result: $timeResult")
        } catch (e: Exception) {
            println("Error calling current_time tool: ${e.message}")
        }

        // Test error handling
        println("\nTesting error handling...")
        try {
            val errorResult = client.callTool("nonexistent_tool", mapOf("param" to "value"))
            println("Result: $errorResult")
        } catch (e: Exception) {
            println("Expected error: ${e.message}")
        }

    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        // Disconnect
        client.disconnect()
        println("Disconnected from remote MCP server")

        // Add a delay to ensure all resources are released
        kotlinx.coroutines.delay(1000)

        // Force exit program
        kotlin.system.exitProcess(0)
    }
}

/**
 * Simulated Remote Transport
 * This transport simulates a remote MCP server by implementing the Transport interface.
 */
class SimulatedRemoteTransport : Transport {
    private val json = Json { ignoreUnknownKeys = true }
    private val isConnected = AtomicBoolean(false)
    private val messageFlow = MutableSharedFlow<MCPMessage>(replay = 10)
    private val requestId = AtomicInteger(0)

    override suspend fun connect() {
        println("Simulated Remote Transport: Connecting...")
        isConnected.set(true)
        println("Simulated Remote Transport: Connected")
    }

    override suspend fun disconnect() {
        println("Simulated Remote Transport: Disconnecting...")
        isConnected.set(false)
        println("Simulated Remote Transport: Disconnected")
    }

    override suspend fun send(message: MCPMessage) {
        if (!isConnected.get()) {
            throw IllegalStateException("Not connected")
        }

        println("Simulated Remote Transport: Sending message: ${message.javaClass.simpleName}")

        if (message is MCPRequest) {
            // Simulate server response based on the request method
            when (message.method) {
                "initialize" -> handleInitialize(message)
                "listTools" -> handleListTools(message)
                "callTool" -> handleCallTool(message)
                else -> handleUnknownMethod(message)
            }
        }
    }

    override fun receive(): Flow<MCPMessage> {
        if (!isConnected.get()) {
            throw IllegalStateException("Not connected")
        }
        return messageFlow.asSharedFlow()
    }

    override fun isConnected(): Boolean {
        return isConnected.get()
    }

    private suspend fun handleInitialize(request: MCPRequest) {
        val response = MCPResponse(
            id = request.id,
            result = buildJsonObject {
                put("name", "SimulatedRemoteMCPServer")
                put("version", "1.0.0")
                put("capabilities", buildJsonObject {
                    put("resources", false)
                    put("tools", true)
                    put("prompts", false)
                    put("sampling", false)
                    put("cancelRequest", true)
                    put("progress", false)
                })
            }
        )
        messageFlow.emit(response)
    }

    private suspend fun handleListTools(request: MCPRequest) {
        val tools = listOf(
            Tool(
                id = "echo",
                name = "echo",
                description = "Echo the input message",
                parameters = ToolParameters(
                    type = "object",
                    required = listOf("message"),
                    properties = mapOf(
                        "message" to ToolParameterProperty(
                            type = "string",
                            description = "The message to echo"
                        )
                    )
                )
            ),
            Tool(
                id = "random_number",
                name = "random_number",
                description = "Generate a random number between min and max",
                parameters = ToolParameters(
                    type = "object",
                    required = listOf("min", "max"),
                    properties = mapOf(
                        "min" to ToolParameterProperty(
                            type = "string",
                            description = "The minimum value"
                        ),
                        "max" to ToolParameterProperty(
                            type = "string",
                            description = "The maximum value"
                        )
                    )
                )
            ),
            Tool(
                id = "current_time",
                name = "current_time",
                description = "Get the current time",
                parameters = ToolParameters(
                    type = "object",
                    required = listOf(),
                    properties = mapOf()
                )
            )
        )

        val response = MCPResponse(
            id = request.id,
            result = json.encodeToJsonElement(ai.kastrax.mcp.protocol.ListToolsResult.serializer(), ai.kastrax.mcp.protocol.ListToolsResult(tools))
        )
        messageFlow.emit(response)
    }

    private suspend fun handleCallTool(request: MCPRequest) {
        val params = request.params?.let {
            json.decodeFromJsonElement(ai.kastrax.mcp.protocol.CallToolParams.serializer(), it)
        }

        if (params == null) {
            val response = MCPResponse(
                id = request.id,
                error = ai.kastrax.mcp.protocol.MCPError(
                    code = ai.kastrax.mcp.protocol.MCPErrorCodes.INVALID_PARAMS,
                    message = "Invalid params"
                )
            )
            messageFlow.emit(response)
            return
        }

        when (params.id) {
            "echo" -> {
                val message = try {
                    params.parameters.jsonObject["message"]?.toString() ?: "No message"
                } catch (e: Exception) {
                    "Error parsing message: ${e.message}"
                }
                val response = MCPResponse(
                    id = request.id,
                    result = json.encodeToJsonElement(
                        ai.kastrax.mcp.protocol.CallToolResult.serializer(),
                        ai.kastrax.mcp.protocol.CallToolResult("Echo: $message")
                    )
                )
                messageFlow.emit(response)
            }
            "random_number" -> {
                val min = try {
                    params.parameters.jsonObject["min"]?.toString()?.replace("\"", "")?.toIntOrNull() ?: 1
                } catch (e: Exception) {
                    1
                }
                val max = try {
                    params.parameters.jsonObject["max"]?.toString()?.replace("\"", "")?.toIntOrNull() ?: 100
                } catch (e: Exception) {
                    100
                }
                val random = (min..max).random()
                val response = MCPResponse(
                    id = request.id,
                    result = json.encodeToJsonElement(
                        ai.kastrax.mcp.protocol.CallToolResult.serializer(),
                        ai.kastrax.mcp.protocol.CallToolResult("Random number: $random")
                    )
                )
                messageFlow.emit(response)
            }
            "current_time" -> {
                val currentTime = java.time.LocalDateTime.now().toString()
                val response = MCPResponse(
                    id = request.id,
                    result = json.encodeToJsonElement(
                        ai.kastrax.mcp.protocol.CallToolResult.serializer(),
                        ai.kastrax.mcp.protocol.CallToolResult("Current time: $currentTime")
                    )
                )
                messageFlow.emit(response)
            }
            else -> {
                val response = MCPResponse(
                    id = request.id,
                    error = ai.kastrax.mcp.protocol.MCPError(
                        code = ai.kastrax.mcp.protocol.MCPErrorCodes.TOOL_NOT_FOUND,
                        message = "Tool not found: ${params.id}"
                    )
                )
                messageFlow.emit(response)
            }
        }
    }

    private suspend fun handleUnknownMethod(request: MCPRequest) {
        val response = MCPResponse(
            id = request.id,
            error = ai.kastrax.mcp.protocol.MCPError(
                code = ai.kastrax.mcp.protocol.MCPErrorCodes.METHOD_NOT_FOUND,
                message = "Method not found: ${request.method}"
            )
        )
        messageFlow.emit(response)
    }
}
