package ai.kastrax.mcp.server

import ai.kastrax.mcp.exception.MCPException
import ai.kastrax.mcp.protocol.*
import ai.kastrax.mcp.transport.Transport
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import ai.kastrax.mcp.transport.sse.SSESession
import ai.kastrax.mcp.transport.sse.respondSSE
import ai.kastrax.mcp.transport.sse.sendEvent

import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.*

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val logger = KotlinLogging.logger {}

/**
 * MCP 服务器实现
 */
class MCPServerImpl(
    override val name: String,
    override val version: String
) : MCPServer {
    private val json = Json { ignoreUnknownKeys = true }
    private val isRunning = AtomicBoolean(false)
    private val resources = ConcurrentHashMap<String, Resource>()
    private val tools = ConcurrentHashMap<String, Tool>()
    private val prompts = ConcurrentHashMap<String, Prompt>()
    private val resourceContentProviders = ConcurrentHashMap<String, ResourceContentProvider>()
    private val toolHandlers = ConcurrentHashMap<String, ToolHandler>()
    private val promptContentProviders = ConcurrentHashMap<String, PromptContentProvider>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private var stdioJob: Job? = null

    override suspend fun start() {
        if (isRunning.get()) {
            return
        }

        // 启动标准输入/输出模式
        stdioJob = scope.launch {
            val reader = BufferedReader(InputStreamReader(System.`in`))
            val writer = BufferedWriter(OutputStreamWriter(System.out))

            try {
                isRunning.set(true)

                while (isRunning.get()) {
                    val line = withContext(Dispatchers.IO) {
                        reader.readLine()
                    } ?: break

                    if (line.isBlank()) {
                        continue
                    }

                    try {
                        // 解析请求
                        val request = json.decodeFromString<MCPRequest>(line)

                        // 处理请求
                        val response = handleRequest(request)

                        // 发送响应
                        val responseJson = json.encodeToString(MCPResponse.serializer(), response)
                        withContext(Dispatchers.IO) {
                            writer.write(responseJson)
                            writer.newLine()
                            writer.flush()
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to handle request: $line" }

                        // 发送错误响应
                        val response = MCPResponse(
                            id = null,
                            error = MCPError(
                                code = MCPErrorCodes.INTERNAL_ERROR,
                                message = "Failed to handle request: ${e.message}"
                            )
                        )

                        val responseJson = json.encodeToString(MCPResponse.serializer(), response)
                        withContext(Dispatchers.IO) {
                            writer.write(responseJson)
                            writer.newLine()
                            writer.flush()
                        }
                    }
                }
            } finally {
                withContext(Dispatchers.IO) {
                    writer.close()
                    reader.close()
                }

                isRunning.set(false)
            }
        }

        logger.info { "Started MCP server: $name (stdio mode)" }
    }

    override suspend fun startSSE(host: String, port: Int) {
        if (isRunning.get()) {
            return
        }

        // 启动 SSE 服务器
        val embeddedServer = embeddedServer(Netty, host = host, port = port) {
            routing {
                route("/mcp") {
                    // SSE 端点
                    get("/sse") {
                        call.response.cacheControl(CacheControl.NoCache(null))

                        try {
                            val session = call.respondSSE()
                            // 发送初始化消息
                            session.sendEvent(
                                event = "initialize",
                                data = json.encodeToString(
                                    InitializeResult.serializer(),
                                    InitializeResult(
                                        name = name,
                                        version = version,
                                        capabilities = ServerCapabilities(
                                            resources = resources.isNotEmpty(),
                                            tools = tools.isNotEmpty(),
                                            prompts = prompts.isNotEmpty(),
                                            sampling = false,
                                            cancelRequest = true,
                                            progress = true
                                        )
                                    )
                                )
                            )

                            // 保持连接活跃
                            while (true) {
                                session.send(
                                    event = "ping",
                                    data = "ping"
                                )
                                delay(30000) // 30 秒
                            }
                        } catch (e: Exception) {
                            logger.error(e) { "SSE connection error" }
                        }
                    }

                    // 请求处理端点
                    post {
                        try {
                            val requestJson = call.receiveText()
                            val request = json.decodeFromString<MCPRequest>(requestJson)

                            // 处理请求
                            val response = handleRequest(request)

                            // 发送响应
                            call.respond(response)
                        } catch (e: Exception) {
                            logger.error(e) { "Failed to handle request" }

                            // 发送错误响应
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                MCPResponse(
                                    id = null,
                                    error = MCPError(
                                        code = MCPErrorCodes.INTERNAL_ERROR,
                                        message = "Failed to handle request: ${e.message}"
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }

        server = embeddedServer
        server?.start(wait = false)
        isRunning.set(true)

        logger.info { "Started MCP server: $name (SSE mode) at http://$host:$port/mcp/sse" }
    }

    override suspend fun stop() {
        if (!isRunning.get()) {
            return
        }

        // 停止标准输入/输出模式
        stdioJob?.cancel()
        stdioJob = null

        // 停止 SSE 服务器
        server?.stop(1000, 2000)
        server = null

        isRunning.set(false)

        logger.info { "Stopped MCP server: $name" }
    }

    override fun registerResource(resource: Resource) {
        resources[resource.id] = resource
    }

    override fun registerTool(tool: Tool) {
        tools[tool.id] = tool
    }

    override fun registerPrompt(prompt: Prompt) {
        prompts[prompt.id] = prompt
    }

    override fun getResources(): List<Resource> {
        return resources.values.toList()
    }

    override fun getTools(): List<Tool> {
        return tools.values.toList()
    }

    override fun getPrompts(): List<Prompt> {
        return prompts.values.toList()
    }

    /**
     * 注册资源内容提供者
     */
    fun registerResourceContentProvider(resourceId: String, provider: ResourceContentProvider) {
        resourceContentProviders[resourceId] = provider
    }

    /**
     * 注册工具处理器
     */
    fun registerToolHandler(toolId: String, handler: ToolHandler) {
        toolHandlers[toolId] = handler
    }

    /**
     * 注册提示内容提供者
     */
    fun registerPromptContentProvider(promptId: String, provider: PromptContentProvider) {
        promptContentProviders[promptId] = provider
    }

    /**
     * 处理请求
     */
    private suspend fun handleRequest(request: MCPRequest): MCPResponse {
        return try {
            when (request.method) {
                MCPMethods.INITIALIZE -> {
                    val params = request.params?.let {
                        json.decodeFromJsonElement(InitializeParams.serializer(), it)
                    } ?: InitializeParams("unknown", "1.0.0", ClientCapabilities())

                    logger.info { "Initialized by client: ${params.name} (${params.version})" }
                    logger.debug { "Client capabilities: ${params.capabilities}" }

                    MCPResponse(
                        id = request.id,
                        result = json.encodeToJsonElement(
                            InitializeResult(
                                name = name,
                                version = version,
                                capabilities = ServerCapabilities(
                                    resources = resources.isNotEmpty(),
                                    tools = tools.isNotEmpty(),
                                    prompts = prompts.isNotEmpty(),
                                    sampling = false,
                                    cancelRequest = true,
                                    progress = true
                                )
                            )
                        )
                    )
                }
                MCPMethods.SHUTDOWN -> {
                    logger.info { "Shutdown requested" }

                    MCPResponse(
                        id = request.id,
                        result = JsonPrimitive(true)
                    )
                }
                MCPMethods.EXIT -> {
                    logger.info { "Exit requested" }

                    // 停止服务器
                    scope.launch {
                        delay(100)
                        stop()
                    }

                    MCPResponse(
                        id = request.id,
                        result = JsonPrimitive(true)
                    )
                }
                MCPMethods.LIST_RESOURCES -> {
                    MCPResponse(
                        id = request.id,
                        result = json.encodeToJsonElement(
                            ListResourcesResult(
                                resources = getResources()
                            )
                        )
                    )
                }
                MCPMethods.GET_RESOURCE -> {
                    val params = request.params?.let {
                        json.decodeFromJsonElement(GetResourceParams.serializer(), it)
                    } ?: throw IllegalArgumentException("Missing parameters")

                    val resourceId = params.id
                    val resource = resources[resourceId] ?: throw ResourceNotFoundException(resourceId)

                    val content = resourceContentProviders[resourceId]?.getResourceContent(resourceId)
                        ?: throw IllegalStateException("No content provider for resource: $resourceId")

                    MCPResponse(
                        id = request.id,
                        result = json.encodeToJsonElement(
                            GetResourceResult(
                                content = content
                            )
                        )
                    )
                }
                MCPMethods.LIST_TOOLS -> {
                    MCPResponse(
                        id = request.id,
                        result = json.encodeToJsonElement(
                            ListToolsResult(
                                tools = getTools()
                            )
                        )
                    )
                }
                MCPMethods.CALL_TOOL -> {
                    val params = request.params?.let {
                        json.decodeFromJsonElement(CallToolParams.serializer(), it)
                    } ?: throw IllegalArgumentException("Missing parameters")

                    val toolId = params.id
                    val tool = tools[toolId] ?: throw ToolNotFoundException(toolId)

                    val handler = toolHandlers[toolId] ?: throw IllegalStateException("No handler for tool: $toolId")

                    // 将 JsonElement 转换为 Map<String, Any>
                    val parameters = jsonElementToMap(params.parameters)

                    // 调用工具
                    val result = handler.handleToolCall(toolId, parameters)

                    MCPResponse(
                        id = request.id,
                        result = json.encodeToJsonElement(
                            CallToolResult(
                                result = result
                            )
                        )
                    )
                }
                MCPMethods.LIST_PROMPTS -> {
                    MCPResponse(
                        id = request.id,
                        result = json.encodeToJsonElement(
                            ListPromptsResult(
                                prompts = getPrompts()
                            )
                        )
                    )
                }
                MCPMethods.GET_PROMPT -> {
                    val params = request.params?.let {
                        json.decodeFromJsonElement(GetPromptParams.serializer(), it)
                    } ?: throw IllegalArgumentException("Missing parameters")

                    val promptId = params.id
                    val prompt = prompts[promptId] ?: throw PromptNotFoundException(promptId)

                    val content = promptContentProviders[promptId]?.getPromptContent(promptId)
                        ?: throw IllegalStateException("No content provider for prompt: $promptId")

                    MCPResponse(
                        id = request.id,
                        result = json.encodeToJsonElement(
                            GetPromptResult(
                                content = content
                            )
                        )
                    )
                }
                MCPMethods.CANCEL_REQUEST -> {
                    val params = request.params?.let {
                        json.decodeFromJsonElement(CancelRequestParams.serializer(), it)
                    } ?: throw IllegalArgumentException("Missing parameters")

                    logger.info { "Cancel request: ${params.id}" }

                    MCPResponse(
                        id = request.id,
                        result = JsonPrimitive(true)
                    )
                }
                else -> {
                    logger.warn { "Unknown method: ${request.method}" }

                    MCPResponse(
                        id = request.id,
                        error = MCPError(
                            code = MCPErrorCodes.METHOD_NOT_FOUND,
                            message = "Unknown method: ${request.method}"
                        )
                    )
                }
            }
        } catch (e: ResourceNotFoundException) {
            logger.error(e) { "Resource not found: ${e.message}" }

            MCPResponse(
                id = request.id,
                error = MCPError(
                    code = MCPErrorCodes.RESOURCE_NOT_FOUND,
                    message = e.message ?: "Resource not found"
                )
            )
        } catch (e: ToolNotFoundException) {
            logger.error(e) { "Tool not found: ${e.message}" }

            MCPResponse(
                id = request.id,
                error = MCPError(
                    code = MCPErrorCodes.TOOL_NOT_FOUND,
                    message = e.message ?: "Tool not found"
                )
            )
        } catch (e: PromptNotFoundException) {
            logger.error(e) { "Prompt not found: ${e.message}" }

            MCPResponse(
                id = request.id,
                error = MCPError(
                    code = MCPErrorCodes.PROMPT_NOT_FOUND,
                    message = e.message ?: "Prompt not found"
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to handle request: ${request.method}" }

            MCPResponse(
                id = request.id,
                error = MCPError(
                    code = MCPErrorCodes.INTERNAL_ERROR,
                    message = "Failed to handle request: ${e.message}"
                )
            )
        }
    }

    /**
     * 将 JsonElement 转换为 Map<String, Any>
     */
    private fun jsonElementToMap(element: JsonElement): Map<String, Any> {
        return when (element) {
            is JsonObject -> {
                element.mapValues { (_, value) ->
                    when (value) {
                        is JsonPrimitive -> {
                            when {
                                value.isString -> value.content
                                value.content == "true" || value.content == "false" -> value.content.toBoolean()
                                value.content.toIntOrNull() != null -> value.content.toInt()
                                value.content.toLongOrNull() != null -> value.content.toLong()
                                value.content.toDoubleOrNull() != null -> value.content.toDouble()
                                else -> value.content
                            }
                        }
                        is JsonObject -> jsonElementToMap(value)
                        is JsonArray -> value.map { jsonElementToAny(it) }
                        else -> value.toString()
                    }
                }
            }
            else -> emptyMap()
        }
    }

    /**
     * 将 JsonElement 转换为 Any
     */
    private fun jsonElementToAny(element: JsonElement): Any {
        return when (element) {
            is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.content == "true" || element.content == "false" -> element.content.toBoolean()
                    element.content.toIntOrNull() != null -> element.content.toInt()
                    element.content.toLongOrNull() != null -> element.content.toLong()
                    element.content.toDoubleOrNull() != null -> element.content.toDouble()
                    else -> element.content
                }
            }
            is JsonObject -> jsonElementToMap(element)
            is JsonArray -> element.map { jsonElementToAny(it) }
            else -> element.toString()
        }
    }
}

/**
 * MCP 服务器构建器实现
 */
class MCPServerBuilderImpl : MCPServerBuilder {
    private var name: String = "kastrax-server"
    private var version: String = "1.0.0"
    private val resources = mutableListOf<Pair<Resource, ResourceContentProvider>>()
    private val tools = mutableListOf<Pair<Tool, ToolHandler>>()
    private val prompts = mutableListOf<Pair<Prompt, PromptContentProvider>>()

    override fun name(name: String): MCPServerBuilder {
        this.name = name
        return this
    }

    override fun version(version: String): MCPServerBuilder {
        this.version = version
        return this
    }

    override fun resource(configure: ResourceBuilder.() -> Unit): MCPServerBuilder {
        val builder = ResourceBuilderImpl()
        builder.configure()

        val resource = Resource(
            id = builder.name,
            name = builder.name,
            description = builder.description,
            type = ResourceType.TEXT,
            metadata = builder.metadata
        )

        val contentProvider = StaticResourceContentProvider(
            mapOf(resource.id to builder.content)
        )

        resources.add(resource to contentProvider)

        return this
    }

    override fun tool(configure: ToolBuilder.() -> Unit): MCPServerBuilder {
        val builder = ToolBuilderImpl()
        builder.configure()

        val parameters = builder.parametersBuilder?.build() ?: ToolParameters()

        val tool = Tool(
            id = builder.name,
            name = builder.name,
            description = builder.description,
            parameters = parameters
        )

        val handler = builder.handler ?: throw IllegalStateException("Tool handler not set")

        val toolHandler = object : ToolHandler {
            override suspend fun handleToolCall(toolId: String, parameters: Map<String, Any>): String {
                return handler(parameters)
            }
        }

        tools.add(tool to toolHandler)

        return this
    }

    override fun prompt(configure: PromptBuilder.() -> Unit): MCPServerBuilder {
        val builder = PromptBuilderImpl()
        builder.configure()

        val parameters = builder.parametersBuilder?.buildPromptParameters() ?: PromptParameters()

        val prompt = Prompt(
            id = builder.name,
            name = builder.name,
            description = builder.description,
            parameters = parameters
        )

        val contentProvider = StaticPromptContentProvider(
            mapOf(prompt.id to builder.content)
        )

        prompts.add(prompt to contentProvider)

        return this
    }

    override fun build(): MCPServer {
        val server = MCPServerImpl(name, version)

        // 注册资源
        resources.forEach { (resource, contentProvider) ->
            server.registerResource(resource)
            server.registerResourceContentProvider(resource.id, contentProvider)
        }

        // 注册工具
        tools.forEach { (tool, handler) ->
            server.registerTool(tool)
            server.registerToolHandler(tool.id, handler)
        }

        // 注册提示
        prompts.forEach { (prompt, contentProvider) ->
            server.registerPrompt(prompt)
            server.registerPromptContentProvider(prompt.id, contentProvider)
        }

        return server
    }
}

/**
 * 资源构建器实现
 */
class ResourceBuilderImpl : ResourceBuilder {
    override var name: String = ""
    override var description: String = ""
    override var content: String = ""
    override var metadata: Map<String, String> = emptyMap()
}

/**
 * 工具构建器实现
 */
class ToolBuilderImpl : ToolBuilder {
    override var name: String = ""
    override var description: String = ""
    var parametersBuilder: ParametersBuilderImpl? = null
    var handler: (suspend (Map<String, Any>) -> String)? = null

    override fun parameters(configure: ParametersBuilder.() -> Unit) {
        val builder = ParametersBuilderImpl()
        builder.configure()
        parametersBuilder = builder
    }

    override fun handler(handler: suspend (Map<String, Any>) -> String) {
        this.handler = handler
    }
}

/**
 * 参数构建器实现
 */
class ParametersBuilderImpl : ParametersBuilder {
    private val parameters = mutableListOf<ToolParameterProperty>()
    private val required = mutableListOf<String>()

    override fun parameter(configure: ParameterBuilder.() -> Unit) {
        val builder = ParameterBuilderImpl()
        builder.configure()

        parameters.add(
            ToolParameterProperty(
                type = builder.type,
                description = builder.description
            )
        )

        if (builder.required) {
            required.add(builder.name)
        }
    }

    fun build(): ToolParameters {
        return ToolParameters(
            type = "object",
            required = required,
            properties = parameters.associateBy { it.description }
                .mapValues { (_, value) -> value }
        )
    }

    fun buildPromptParameters(): PromptParameters {
        return PromptParameters()
    }
}

/**
 * 参数构建器实现
 */
class ParameterBuilderImpl : ParameterBuilder {
    override var name: String = ""
    override var type: String = "string"
    override var description: String = ""
    override var required: Boolean = false
}

/**
 * 提示构建器实现
 */
class PromptBuilderImpl : PromptBuilder {
    override var name: String = ""
    override var description: String = ""
    override var content: String = ""
    var parametersBuilder: ParametersBuilderImpl? = null

    override fun parameters(configure: ParametersBuilder.() -> Unit) {
        val builder = ParametersBuilderImpl()
        builder.configure()
        parametersBuilder = builder
    }
}

/**
 * 创建 MCP 服务器
 */
fun mcpServer(configure: MCPServerBuilder.() -> Unit): MCPServer {
    val builder = MCPServerBuilderImpl()
    builder.configure()
    return builder.build()
}
