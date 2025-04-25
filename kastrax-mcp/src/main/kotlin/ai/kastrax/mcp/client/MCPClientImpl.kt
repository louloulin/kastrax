package ai.kastrax.mcp.client

import ai.kastrax.mcp.exception.MCPException
import ai.kastrax.mcp.protocol.*
import ai.kastrax.mcp.transport.Transport
import ai.kastrax.mcp.transport.StdioTransport
import ai.kastrax.mcp.transport.SSETransport
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val logger = KotlinLogging.logger {}

/**
 * MCP 客户端实现
 */
class MCPClientImpl(
    override val name: String,
    override val version: String,
    private val transport: Transport,
    private val timeoutMs: Long = 60000
) : MCPClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val isInitialized = AtomicBoolean(false)
    private val pendingRequests = ConcurrentHashMap<String, suspend (MCPResponse) -> Unit>()
    private val serverCapabilities = ConcurrentHashMap<String, Boolean>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var receiveJob: Job? = null

    override suspend fun connect() {
        if (transport.isConnected()) {
            return
        }

        transport.connect()

        // 启动接收消息的协程
        receiveJob = transport.receive()
            .onEach { message ->
                when (message) {
                    is MCPResponse -> {
                        val id = message.id
                        if (id != null) {
                            val callback = pendingRequests.remove(id)
                            callback?.invoke(message)
                        }
                    }
                    is MCPRequest -> {
                        // 客户端不处理请求
                        logger.warn { "Received unexpected request: $message" }
                    }
                    is MCPNotification -> {
                        // 处理通知
                        handleNotification(message)
                    }
                }
            }
            .launchIn(scope)

        // 初始化客户端
        initialize()
    }

    override suspend fun disconnect() {
        if (!transport.isConnected()) {
            return
        }

        // 取消接收消息的协程
        receiveJob?.cancel()
        receiveJob = null

        // 断开连接
        transport.disconnect()

        // 重置状态
        isInitialized.set(false)
        pendingRequests.clear()
        serverCapabilities.clear()
    }

    override fun isConnected(): Boolean {
        return transport.isConnected()
    }

    override suspend fun resources(): List<Resource> {
        if (!isInitialized.get()) {
            throw IllegalStateException("Client not initialized")
        }

        if (!supportsCapability("resources")) {
            return emptyList()
        }

        val response = sendRequest<ListResourcesResult>(
            MCPMethods.LIST_RESOURCES,
            null
        )

        return response.resources
    }

    override suspend fun getResource(resourceId: String): String {
        if (!isInitialized.get()) {
            throw IllegalStateException("Client not initialized")
        }

        if (!supportsCapability("resources")) {
            throw UnsupportedOperationException("Server does not support resources")
        }

        val response = sendRequest<GetResourceResult>(
            MCPMethods.GET_RESOURCE,
            GetResourceParams(resourceId)
        )

        return response.content
    }

    override suspend fun tools(): List<Tool> {
        if (!isInitialized.get()) {
            throw IllegalStateException("Client not initialized")
        }

        if (!supportsCapability("tools")) {
            return emptyList()
        }

        val response = sendRequest<ListToolsResult>(
            MCPMethods.LIST_TOOLS,
            null
        )

        return response.tools
    }

    override suspend fun callTool(toolId: String, parameters: Map<String, Any>): String {
        if (!isInitialized.get()) {
            throw IllegalStateException("Client not initialized")
        }

        if (!supportsCapability("tools")) {
            throw UnsupportedOperationException("Server does not support tools")
        }

        // 将参数转换为 JsonElement
        val jsonParameters = mapToJsonElement(parameters)

        val response = sendRequest<CallToolResult>(
            MCPMethods.CALL_TOOL,
            CallToolParams(toolId, jsonParameters)
        )

        return response.result
    }

    override suspend fun prompts(): List<Prompt> {
        if (!isInitialized.get()) {
            throw IllegalStateException("Client not initialized")
        }

        if (!supportsCapability("prompts")) {
            return emptyList()
        }

        val response = sendRequest<ListPromptsResult>(
            MCPMethods.LIST_PROMPTS,
            null
        )

        return response.prompts
    }

    override suspend fun getPrompt(promptId: String): String {
        if (!isInitialized.get()) {
            throw IllegalStateException("Client not initialized")
        }

        if (!supportsCapability("prompts")) {
            throw UnsupportedOperationException("Server does not support prompts")
        }

        val response = sendRequest<GetPromptResult>(
            MCPMethods.GET_PROMPT,
            GetPromptParams(promptId)
        )

        return response.content
    }

    override fun supportsCapability(capability: String): Boolean {
        return serverCapabilities[capability] ?: false
    }

    /**
     * 初始化客户端
     */
    private suspend fun initialize() {
        if (isInitialized.get()) {
            return
        }

        val clientCapabilities = ClientCapabilities(
            resources = true,
            tools = true,
            prompts = true,
            sampling = false,
            cancelRequest = true,
            progress = true
        )

        val response = sendRequest<InitializeResult>(
            MCPMethods.INITIALIZE,
            InitializeParams(name, version, clientCapabilities)
        )

        // 保存服务器能力
        serverCapabilities["resources"] = response.capabilities.resources
        serverCapabilities["tools"] = response.capabilities.tools
        serverCapabilities["prompts"] = response.capabilities.prompts
        serverCapabilities["sampling"] = response.capabilities.sampling
        serverCapabilities["cancelRequest"] = response.capabilities.cancelRequest
        serverCapabilities["progress"] = response.capabilities.progress

        isInitialized.set(true)

        logger.info { "Initialized MCP client: $name" }
        logger.debug { "Server capabilities: $serverCapabilities" }
    }

    /**
     * 处理通知
     */
    private fun handleNotification(notification: MCPNotification) {
        when (notification.method) {
            MCPMethods.PROGRESS -> {
                val params = json.decodeFromJsonElement(ProgressParams.serializer(), notification.params!!)
                logger.debug { "Progress: ${params.value}% - ${params.message}" }
            }
            MCPMethods.LOG -> {
                val params = json.decodeFromJsonElement(LogParams.serializer(), notification.params!!)
                when (params.level) {
                    LogLevel.DEBUG -> logger.debug { params.message }
                    LogLevel.INFO -> logger.info { params.message }
                    LogLevel.WARN -> logger.warn { params.message }
                    LogLevel.ERROR -> logger.error { params.message }
                }
            }
            else -> {
                logger.warn { "Received unknown notification: ${notification.method}" }
            }
        }
    }

    /**
     * 发送请求并等待响应
     */
    private suspend inline fun <reified T> sendRequest(method: String, params: Any?): T {
        val id = UUID.randomUUID().toString()

        // 将参数转换为 JsonElement
        val jsonParams = params?.let {
            // 使用安全的方式处理序列化
            when (it) {
                is JsonElement -> it
                is String -> JsonPrimitive(it)
                is Number -> JsonPrimitive(it)
                is Boolean -> JsonPrimitive(it)
                is Map<*, *> -> {
                    try {
                        val jsonObject = buildJsonObject {
                            it.forEach { (key, value) ->
                                if (key is String) {
                                    when (value) {
                                        is String -> put(key, value)
                                        is Number -> put(key, value)
                                        is Boolean -> put(key, value)
                                        else -> put(key, value.toString())
                                    }
                                }
                            }
                        }
                        jsonObject
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to convert Map to JsonObject: $it" }
                        JsonPrimitive(it.toString())
                    }
                }
                is Collection<*> -> {
                    try {
                        val jsonArray = buildJsonArray {
                            it.forEach { value ->
                                when (value) {
                                    is String -> add(value)
                                    is Number -> add(value)
                                    is Boolean -> add(value)
                                    else -> add(value.toString())
                                }
                            }
                        }
                        jsonArray
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to convert Collection to JsonArray: $it" }
                        JsonPrimitive(it.toString())
                    }
                }
                else -> JsonPrimitive(it.toString())
            }
        }

        val request = MCPRequest(id, method, jsonParams)

        return withTimeout(timeoutMs) {
            suspendCoroutine { continuation ->
                // 注册回调
                pendingRequests[id] = { response ->
                    if (response.error != null) {
                        val error = response.error
                        continuation.resumeWithException(
                            MCPException(
                                error.code,
                                error.message,
                                error.data?.toString()
                            )
                        )
                    } else {
                        try {
                            val result = response.result?.let {
                                try {
                                    // 使用安全的方式处理反序列化
                                    when (it) {
                                        is JsonObject -> {
                                            try {
                                                json.decodeFromJsonElement<T>(it)
                                            } catch (e: Exception) {
                                                logger.warn(e) { "Failed to decode JsonObject directly, trying as string" }
                                                // 如果直接解析失败，尝试先转换为字符串
                                                val jsonString = it.toString()
                                                json.decodeFromString<T>(jsonString)
                                            }
                                        }
                                        is JsonPrimitive -> {
                                            // 如果是基本类型，尝试直接转换
                                            @Suppress("UNCHECKED_CAST")
                                            when {
                                                it.isString -> it.content as? T ?: it.toString() as T
                                                it.booleanOrNull != null -> it.boolean as T
                                                it.doubleOrNull != null -> it.double as T
                                                it.longOrNull != null -> it.long as T
                                                it.intOrNull != null -> it.int as T
                                                else -> it.toString() as T
                                            }
                                        }
                                        is JsonArray -> {
                                            try {
                                                json.decodeFromJsonElement<T>(it)
                                            } catch (e: Exception) {
                                                logger.warn(e) { "Failed to decode JsonArray directly, trying as string" }
                                                val jsonString = it.toString()
                                                json.decodeFromString<T>(jsonString)
                                            }
                                        }
                                        else -> {
                                            logger.warn { "Unknown JSON element type: ${it::class.java.name}" }
                                            @Suppress("UNCHECKED_CAST")
                                            it.toString() as T
                                        }
                                    }
                                } catch (e: Exception) {
                                    logger.error(e) { "Failed to decode response result: $it" }
                                    null
                                }
                            }

                            if (result != null) {
                                continuation.resume(result)
                            } else {
                                continuation.resumeWithException(
                                    MCPException(
                                        MCPErrorCodes.INTERNAL_ERROR,
                                        "Null result"
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            continuation.resumeWithException(
                                MCPException(
                                    MCPErrorCodes.INTERNAL_ERROR,
                                    "Failed to decode response: ${e.message}",
                                    e.toString()
                                )
                            )
                        }
                    }
                }

                // 发送请求
                scope.launch {
                    try {
                        transport.send(request)
                    } catch (e: Exception) {
                        pendingRequests.remove(id)
                        continuation.resumeWithException(
                            MCPException(
                                MCPErrorCodes.INTERNAL_ERROR,
                                "Failed to send request: ${e.message}",
                                e.toString()
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * 将 Map<String, Any> 转换为 JsonElement
     */
    private fun mapToJsonElement(map: Map<String, Any>): JsonElement {
        val jsonMap = map.mapValues { (_, value) ->
            when (value) {
                is String -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    mapToJsonElement(value as Map<String, Any>)
                }
                is List<*> -> {
                    val list = value.map { item ->
                        when (item) {
                            is String -> JsonPrimitive(item)
                            is Number -> JsonPrimitive(item)
                            is Boolean -> JsonPrimitive(item)
                            is Map<*, *> -> {
                                @Suppress("UNCHECKED_CAST")
                                mapToJsonElement(item as Map<String, Any>)
                            }
                            else -> JsonPrimitive(item.toString())
                        }
                    }
                    kotlinx.serialization.json.JsonArray(list)
                }
                else -> JsonPrimitive(value.toString())
            }
        }

        return JsonObject(jsonMap)
    }
}

/**
 * MCP 客户端构建器实现
 */
class MCPClientBuilderImpl : MCPClientBuilder {
    private var name: String = "kastrax-client"
    private var version: String = "1.0.0"
    private var timeoutMs: Long = 60000
    private var transport: Transport? = null

    override fun name(name: String): MCPClientBuilder {
        this.name = name
        return this
    }

    override fun version(version: String): MCPClientBuilder {
        this.version = version
        return this
    }

    override fun server(configure: MCPServerConfig.() -> Unit): MCPClientBuilder {
        val config = MCPServerConfigImpl()
        config.configure()

        transport = config.transport

        return this
    }

    override fun timeout(timeoutMs: Long): MCPClientBuilder {
        this.timeoutMs = timeoutMs
        return this
    }

    override fun build(): MCPClient {
        val transport = this.transport ?: throw IllegalStateException("Server not configured")

        return MCPClientImpl(name, version, transport, timeoutMs)
    }
}

/**
 * MCP 服务器配置实现
 */
class MCPServerConfigImpl : MCPServerConfig {
    var transport: Transport? = null

    override fun stdio(configure: StdioServerConfig.() -> Unit) {
        val config = StdioServerConfigImpl()
        config.configure()

        transport = StdioTransport(config.command, config.args, config.env)
    }

    override fun sse(configure: SSEServerConfig.() -> Unit) {
        val config = SSEServerConfigImpl()
        config.configure()

        transport = SSETransport(config.url, config.headers)
    }
}

/**
 * 标准输入/输出服务器配置实现
 */
class StdioServerConfigImpl : StdioServerConfig {
    override var command: String = ""
    override var args: List<String> = emptyList()
    override var env: Map<String, String> = emptyMap()
}

/**
 * SSE 服务器配置实现
 */
class SSEServerConfigImpl : SSEServerConfig {
    override var url: String = ""
    override var headers: Map<String, String> = emptyMap()
}

/**
 * 创建 MCP 客户端
 */
fun mcpClient(configure: MCPClientBuilder.() -> Unit): MCPClient {
    val builder = MCPClientBuilderImpl()
    builder.configure()
    return builder.build()
}
