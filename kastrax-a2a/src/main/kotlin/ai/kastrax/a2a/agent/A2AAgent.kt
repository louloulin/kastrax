package ai.kastrax.a2a.agent

import ai.kastrax.a2a.model.*
import ai.kastrax.core.agent.Agent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonElement
import java.util.UUID

/**
 * A2A 代理接口，定义 A2A 代理的基本功能
 */
interface A2AAgent {
    /**
     * 获取代理卡片
     */
    fun getAgentCard(): AgentCard

    /**
     * 获取代理能力
     */
    fun getCapabilities(): List<Capability>

    /**
     * 调用代理能力
     */
    suspend fun invoke(request: InvokeRequest): InvokeResponse

    /**
     * 查询代理状态
     */
    suspend fun query(request: QueryRequest): QueryResponse

    /**
     * 处理 A2A 消息
     */
    suspend fun processMessage(message: A2AMessage): A2AMessage

    /**
     * 启动代理
     */
    fun start()

    /**
     * 停止代理
     */
    fun stop()
}

/**
 * A2A 代理基本实现，基于 Kotlin 协程和 Channel 实现异步消息处理
 */
class A2AAgentImpl(
    private val agentCard: AgentCard,
    private val baseAgent: Agent,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : A2AAgent {
    private val messageChannel = Channel<Pair<A2AMessage, Channel<A2AMessage>>>(Channel.BUFFERED)
    private var actorJob: Job? = null

    init {
        // 初始化时不自动启动，需要显式调用 start() 方法
    }

    override fun getAgentCard(): AgentCard = agentCard

    override fun getCapabilities(): List<Capability> = agentCard.capabilities

    override suspend fun invoke(request: InvokeRequest): InvokeResponse {
        val responseChannel = Channel<A2AMessage>()

        try {
            // 添加 10 秒超时
            return withTimeout(10000) {
                messageChannel.send(request to responseChannel)
                val response = responseChannel.receive()

                when (response) {
                    is InvokeResponse -> response
                    is ErrorMessage -> throw A2AException(response.code, response.message)
                    else -> throw A2AException("unexpected_response", "Unexpected response type: ${response.type}")
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw A2AException("timeout", "Request timed out after 10 seconds")
        } catch (e: Exception) {
            throw A2AException("invocation_error", e.message ?: "Unknown error during invocation")
        } finally {
            // 确保响应通道关闭
            if (!responseChannel.isClosedForReceive) {
                responseChannel.close()
            }
        }
    }

    override suspend fun query(request: QueryRequest): QueryResponse {
        val responseChannel = Channel<A2AMessage>()

        try {
            // 添加 10 秒超时
            return withTimeout(10000) {
                messageChannel.send(request to responseChannel)
                val response = responseChannel.receive()

                when (response) {
                    is QueryResponse -> response
                    is ErrorMessage -> throw A2AException(response.code, response.message)
                    else -> throw A2AException("unexpected_response", "Unexpected response type: ${response.type}")
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw A2AException("timeout", "Request timed out after 10 seconds")
        } catch (e: Exception) {
            throw A2AException("query_error", e.message ?: "Unknown error during query")
        } finally {
            // 确保响应通道关闭
            if (!responseChannel.isClosedForReceive) {
                responseChannel.close()
            }
        }
    }

    override suspend fun processMessage(message: A2AMessage): A2AMessage {
        return when (message) {
            is CapabilityRequest -> handleCapabilityRequest(message)
            is InvokeRequest -> handleInvokeRequest(message)
            is QueryRequest -> handleQueryRequest(message)
            else -> ErrorMessage(
                id = message.id,
                code = "unsupported_message_type",
                message = "Unsupported message type: ${message.type}"
            )
        }
    }

    override fun start() {
        if (actorJob != null) return

        actorJob = scope.launch {
            processMessages()
        }
    }

    override fun stop() {
        // 使用 runBlocking 确保协程完全取消和资源释放
        runBlocking {
            actorJob?.cancelAndJoin()
            actorJob = null
            messageChannel.close()
        }
    }

    private suspend fun processMessages() {
        try {
            for ((message, responseChannel) in messageChannel) {
                try {
                    val response = processMessage(message)
                    responseChannel.send(response)
                } catch (e: Exception) {
                    responseChannel.send(
                        ErrorMessage(
                            id = message.id,
                            code = "processing_error",
                            message = e.message ?: "Unknown error"
                        )
                    )
                } finally {
                    responseChannel.close()
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            // 通道已关闭，正常退出
            println("Message channel closed, exiting message processing loop")
        } catch (e: Exception) {
            // 其他异常，记录错误
            println("Error in message processing: ${e.message}")
        }
    }

    private fun handleCapabilityRequest(request: CapabilityRequest): CapabilityResponse {
        val capabilities = if (request.capabilityId != null) {
            agentCard.capabilities.filter { it.id == request.capabilityId }
        } else {
            agentCard.capabilities
        }

        return CapabilityResponse(
            id = request.id,
            capabilities = capabilities
        )
    }

    private suspend fun handleInvokeRequest(request: InvokeRequest): A2AMessage {
        // 查找请求的能力
        val capability = agentCard.capabilities.find { it.id == request.capabilityId }
            ?: return ErrorMessage(
                id = request.id,
                code = "capability_not_found",
                message = "Capability not found: ${request.capabilityId}"
            )

        // 验证参数
        val missingParams = capability.parameters
            .filter { it.required }
            .map { it.name }
            .filter { !request.parameters.containsKey(it) }

        if (missingParams.isNotEmpty()) {
            return ErrorMessage(
                id = request.id,
                code = "missing_parameters",
                message = "Missing required parameters: ${missingParams.joinToString(", ")}"
            )
        }

        try {
            // 将 A2A 请求转换为 kastrax Agent 请求
            val prompt = request.parameters["prompt"]?.toString() ?: ""

            // 调用 kastrax Agent
            val agentResponse = baseAgent.generate(prompt)

            // 将 kastrax Agent 响应转换为 A2A 响应
            return InvokeResponse(
                id = request.id,
                result = agentResponse.toJsonElement(),
                metadata = mapOf(
                    "agent_id" to baseAgent.name,
                    "capability_id" to capability.id
                )
            )
        } catch (e: Exception) {
            return ErrorMessage(
                id = request.id,
                code = "invocation_error",
                message = e.message ?: "Unknown error during invocation"
            )
        }
    }

    private suspend fun handleQueryRequest(request: QueryRequest): A2AMessage {
        return when (request.queryType) {
            "status" -> {
                QueryResponse(
                    id = request.id,
                    result = mapOf(
                        "status" to "active",
                        "agent_id" to agentCard.id,
                        "version" to agentCard.version
                    ).toJsonElement()
                )
            }
            "health" -> {
                QueryResponse(
                    id = request.id,
                    result = mapOf(
                        "status" to "healthy",
                        "uptime" to System.currentTimeMillis().toString()
                    ).toJsonElement()
                )
            }
            else -> {
                ErrorMessage(
                    id = request.id,
                    code = "unsupported_query_type",
                    message = "Unsupported query type: ${request.queryType}"
                )
            }
        }
    }
}

/**
 * A2A 异常
 */
class A2AException(val code: String, override val message: String) : Exception(message)

/**
 * 将对象转换为 JsonElement
 */
private fun Any.toJsonElement(): JsonElement {
    // 实际实现中，这里应该使用 kotlinx.serialization 将对象转换为 JsonElement
    // 为简化示例，这里返回一个空的 JsonElement
    return kotlinx.serialization.json.buildJsonObject { }
}
