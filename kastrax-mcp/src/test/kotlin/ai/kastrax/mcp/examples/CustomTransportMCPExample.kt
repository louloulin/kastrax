package ai.kastrax.mcp.examples

import ai.kastrax.mcp.client.MCPClient
import ai.kastrax.mcp.client.MCPClientImpl
import ai.kastrax.mcp.protocol.*
import ai.kastrax.mcp.server.MCPServer
import ai.kastrax.mcp.server.MCPServerImpl
import ai.kastrax.mcp.transport.Transport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 自定义传输层MCP示例
 * 演示如何使用自定义传输层实现MCP
 */
fun main() = runBlocking {
    println("启动自定义传输层MCP示例...")

    // 创建 JSON 序列化器
    val json = Json { ignoreUnknownKeys = true }

    // 创建自定义传输层
    val transport = DirectTransport(json)

    // 创建MCP服务器
    val server = MCPServerImpl("CustomTransportServer", "1.0.0")

    // 添加工具和处理器
    val echoTool = Tool(
        id = "echo",
        name = "echo",
        description = "回显输入的消息",
        parameters = ToolParameters(
            type = "object",
            required = listOf("message"),
            properties = mapOf(
                "message" to ToolParameterProperty(
                    type = "string",
                    description = "要回显的消息"
                )
            )
        )
    )

    // 创建工具处理器
    val echoHandler = object : ToolHandler {
        override suspend fun handleToolCall(toolId: String, parameters: Map<String, Any>): String {
            val message = parameters["message"] as? String ?: "No message provided"
            println("执行echo工具，消息: $message")
            return message
        }
    }

    // 注册工具和处理器
    transport.addServerHandler(MCPMethods.LIST_TOOLS) { request ->
        MCPResponse(
            id = request.id,
            result = json.encodeToJsonElement(
                ListToolsResult.serializer(),
                ListToolsResult(
                    tools = listOf(echoTool)
                )
            )
        )
    }

    transport.addServerHandler(MCPMethods.CALL_TOOL) { request ->
        val params = request.params?.let {
            json.decodeFromJsonElement(CallToolParams.serializer(), it)
        }

        if (params?.id == "echo") {
            val parameters = mapOf("message" to "Hello, Custom Transport MCP!")
            val result = echoHandler.handleToolCall("echo", parameters)

            MCPResponse(
                id = request.id,
                result = json.encodeToJsonElement(
                    CallToolResult.serializer(),
                    CallToolResult(
                        result = result
                    )
                )
            )
        } else {
            MCPResponse(
                id = request.id,
                error = MCPError(
                    code = MCPErrorCodes.TOOL_NOT_FOUND,
                    message = "Tool not found: ${params?.id}"
                )
            )
        }
    }

    // 启动服务器
    server.start()
    println("MCP服务器已启动")

    // 创建MCP客户端
    val client = MCPClientImpl("CustomTransportClient", "1.0.0", transport)

    try {
        // 连接到服务器
        println("连接到MCP服务器...")
        client.connect()
        println("已连接到MCP服务器")

        // 获取服务器信息
        println("\n服务器信息:")
        println("名称: ${client.name}")
        println("版本: ${client.version}")
        println("功能:")
        println("- 资源: ${client.supportsCapability("resources")}")
        println("- 工具: ${client.supportsCapability("tools")}")
        println("- 提示: ${client.supportsCapability("prompts")}")

        // 获取可用工具
        val tools = client.tools()
        println("\n可用工具:")
        tools.forEach { tool ->
            println("- ${tool.name}: ${tool.description}")
        }

        // 调用echo工具
        println("\n调用echo工具...")
        val result = client.callTool("echo", mapOf("message" to "Hello, Custom Transport MCP!"))
        println("echo结果: $result")

    } catch (e: Exception) {
        println("发生错误: ${e.message}")
        e.printStackTrace()
    } finally {
        // 断开连接
        client.disconnect()
        server.stop()
        println("已停止MCP服务器和客户端")
    }
}

/**
 * 直接传输层实现
 * 客户端和服务器直接通信，不通过网络
 */
class DirectTransport(private val json: Json) : Transport {
    private val isConnected = AtomicBoolean(false)
    private val messageFlow = MutableSharedFlow<MCPMessage>(replay = 10)
    private val serverHandlers = mutableMapOf<String, suspend (MCPRequest) -> MCPResponse>()

    init {
        // 初始化默认处理器
        serverHandlers[MCPMethods.INITIALIZE] = { request ->
            val params = request.params?.let {
                json.decodeFromJsonElement(InitializeParams.serializer(), it)
            }

            if (params != null) {
                MCPResponse(
                    id = request.id,
                    result = json.encodeToJsonElement(
                        InitializeResult.serializer(),
                        InitializeResult(
                            name = "CustomTransportServer",
                            version = "1.0.0",
                            capabilities = ServerCapabilities(
                                resources = false,
                                tools = true,
                                prompts = false,
                                sampling = false,
                                cancelRequest = true,
                                progress = true
                            )
                        )
                    )
                )
            } else {
                MCPResponse(
                    id = request.id,
                    error = MCPError(
                        code = MCPErrorCodes.INVALID_PARAMS,
                        message = "Invalid params"
                    )
                )
            }
        }

        serverHandlers[MCPMethods.LIST_TOOLS] = { request ->
            MCPResponse(
                id = request.id,
                result = json.encodeToJsonElement(
                    ListToolsResult.serializer(),
                    ListToolsResult(
                        tools = listOf(
                            Tool(
                                id = "echo",
                                name = "echo",
                                description = "回显输入的消息",
                                parameters = ToolParameters(
                                    type = "object",
                                    required = listOf("message"),
                                    properties = mapOf(
                                        "message" to ToolParameterProperty(
                                            type = "string",
                                            description = "要回显的消息"
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        }

        serverHandlers[MCPMethods.CALL_TOOL] = { request ->
            val params = request.params?.let {
                json.decodeFromJsonElement(CallToolParams.serializer(), it)
            }

            if (params?.id == "echo") {
                val parameters = params.parameters as? Map<*, *>
                val message = parameters?.get("message") as? String ?: "No message provided"

                MCPResponse(
                    id = request.id,
                    result = json.encodeToJsonElement(
                        CallToolResult.serializer(),
                        CallToolResult(
                            result = message
                        )
                    )
                )
            } else {
                MCPResponse(
                    id = request.id,
                    error = MCPError(
                        code = MCPErrorCodes.TOOL_NOT_FOUND,
                        message = "Tool not found: ${params?.id}"
                    )
                )
            }
        }
    }

    override suspend fun connect() {
        isConnected.set(true)
    }

    override suspend fun disconnect() {
        isConnected.set(false)
    }

    override suspend fun send(message: MCPMessage) {
        if (!isConnected.get()) {
            throw IllegalStateException("Not connected")
        }

        if (message is MCPRequest) {
            val handler = serverHandlers[message.method]
            if (handler != null) {
                try {
                    val response = handler(message)
                    messageFlow.emit(response)
                } catch (e: Exception) {
                    val errorResponse = MCPResponse(
                        id = message.id,
                        error = MCPError(
                            code = MCPErrorCodes.INTERNAL_ERROR,
                            message = e.message ?: "Unknown error"
                        )
                    )
                    messageFlow.emit(errorResponse)
                }
            } else {
                val errorResponse = MCPResponse(
                    id = message.id,
                    error = MCPError(
                        code = MCPErrorCodes.METHOD_NOT_FOUND,
                        message = "Method not found: ${message.method}"
                    )
                )
                messageFlow.emit(errorResponse)
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

    /**
     * 添加服务器请求处理器
     */
    fun addServerHandler(method: String, handler: suspend (MCPRequest) -> MCPResponse) {
        serverHandlers[method] = handler
    }
}
