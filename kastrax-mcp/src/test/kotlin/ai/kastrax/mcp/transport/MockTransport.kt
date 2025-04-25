package ai.kastrax.mcp.transport

import ai.kastrax.mcp.protocol.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 模拟传输层，用于测试
 */
class MockTransport : Transport {
    private val json = Json { ignoreUnknownKeys = true }
    private val isConnected = AtomicBoolean(false)
    private val messageFlow = MutableSharedFlow<MCPMessage>(replay = 10)
    private val sentMessages = mutableListOf<MCPMessage>()
    private val requestHandlers = mutableMapOf<String, suspend (MCPRequest) -> MCPResponse>()
    
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
        
        sentMessages.add(message)
        
        if (message is MCPRequest) {
            val handler = requestHandlers[message.method]
            if (handler != null) {
                val response = handler(message)
                messageFlow.emit(response)
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
     * 添加请求处理器
     */
    fun addRequestHandler(method: String, handler: suspend (MCPRequest) -> MCPResponse) {
        requestHandlers[method] = handler
    }
    
    /**
     * 添加默认请求处理器
     */
    fun addDefaultRequestHandlers() {
        // 初始化处理器
        addRequestHandler(MCPMethods.INITIALIZE) { request ->
            MCPResponse(
                id = request.id,
                result = json.encodeToJsonElement(
                    InitializeResult(
                        name = "mock-server",
                        version = "1.0.0",
                        capabilities = ServerCapabilities(
                            resources = true,
                            tools = true,
                            prompts = true,
                            sampling = false,
                            cancelRequest = true,
                            progress = true
                        )
                    )
                )
            )
        }
        
        // 列出资源处理器
        addRequestHandler(MCPMethods.LIST_RESOURCES) { request ->
            MCPResponse(
                id = request.id,
                result = json.encodeToJsonElement(
                    ListResourcesResult(
                        resources = listOf(
                            Resource(
                                id = "test-resource",
                                name = "Test Resource",
                                description = "A test resource",
                                type = ResourceType.TEXT
                            )
                        )
                    )
                )
            )
        }
        
        // 获取资源处理器
        addRequestHandler(MCPMethods.GET_RESOURCE) { request ->
            val params = request.params?.let {
                json.decodeFromJsonElement(GetResourceParams.serializer(), it)
            }
            
            if (params?.id == "test-resource") {
                MCPResponse(
                    id = request.id,
                    result = json.encodeToJsonElement(
                        GetResourceResult(
                            content = "Test resource content"
                        )
                    )
                )
            } else {
                MCPResponse(
                    id = request.id,
                    error = MCPError(
                        code = MCPErrorCodes.RESOURCE_NOT_FOUND,
                        message = "Resource not found: ${params?.id}"
                    )
                )
            }
        }
        
        // 列出工具处理器
        addRequestHandler(MCPMethods.LIST_TOOLS) { request ->
            MCPResponse(
                id = request.id,
                result = json.encodeToJsonElement(
                    ListToolsResult(
                        tools = listOf(
                            Tool(
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
                        )
                    )
                )
            )
        }
        
        // 调用工具处理器
        addRequestHandler(MCPMethods.CALL_TOOL) { request ->
            val params = request.params?.let {
                json.decodeFromJsonElement(CallToolParams.serializer(), it)
            }
            
            if (params?.id == "test-tool") {
                val parameters = params.parameters as? JsonElement
                val param1 = (parameters as? Map<*, *>)?.get("param1") as? JsonPrimitive
                
                MCPResponse(
                    id = request.id,
                    result = json.encodeToJsonElement(
                        CallToolResult(
                            result = "Tool result: ${param1?.content ?: "no param1"}"
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
        
        // 列出提示处理器
        addRequestHandler(MCPMethods.LIST_PROMPTS) { request ->
            MCPResponse(
                id = request.id,
                result = json.encodeToJsonElement(
                    ListPromptsResult(
                        prompts = listOf(
                            Prompt(
                                id = "test-prompt",
                                name = "Test Prompt",
                                description = "A test prompt",
                                parameters = PromptParameters(
                                    type = "object",
                                    required = listOf("param1"),
                                    properties = mapOf(
                                        "param1" to PromptParameterProperty(
                                            type = "string",
                                            description = "Parameter 1"
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        }
        
        // 获取提示处理器
        addRequestHandler(MCPMethods.GET_PROMPT) { request ->
            val params = request.params?.let {
                json.decodeFromJsonElement(GetPromptParams.serializer(), it)
            }
            
            if (params?.id == "test-prompt") {
                MCPResponse(
                    id = request.id,
                    result = json.encodeToJsonElement(
                        GetPromptResult(
                            content = "Hello, {{param1}}!"
                        )
                    )
                )
            } else {
                MCPResponse(
                    id = request.id,
                    error = MCPError(
                        code = MCPErrorCodes.PROMPT_NOT_FOUND,
                        message = "Prompt not found: ${params?.id}"
                    )
                )
            }
        }
    }
    
    /**
     * 发送消息
     */
    suspend fun emitMessage(message: MCPMessage) {
        messageFlow.emit(message)
    }
    
    /**
     * 获取已发送的消息
     */
    fun getSentMessages(): List<MCPMessage> {
        return sentMessages.toList()
    }
    
    /**
     * 清除已发送的消息
     */
    fun clearSentMessages() {
        sentMessages.clear()
    }
}
