package ai.kastrax.actor

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PoisonPill
import ai.kastrax.actor.multimodal.MultimodalMessage
import ai.kastrax.actor.multimodal.MultimodalType
import ai.kastrax.actor.multimodal.MultimodalContent
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse as CoreAgentResponse
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.concurrent.ConcurrentHashMap

/**
 * KastraxActor 类，将 kastrax Agent 包装为 kactor Actor
 *
 * @property agent kastrax Agent 实例
 */
class KastraxActor(private val agent: Agent) : Actor {
    companion object {
        /**
         * 最大递归深度，防止无限递归
         */
        private const val MAX_RECURSION_DEPTH = 5

        /**
         * 日志前缀，用于标识日志来源
         */
        private const val LOG_PREFIX = "[KastraxActor]"
    }
    private val jobs = ConcurrentHashMap<String, Job>()
    override suspend fun Context.receive(msg: Any) {
        println("$LOG_PREFIX Actor ${self.id} received message: ${msg::class.simpleName} from ${sender?.id ?: "null"}")

        when (msg) {
            is actor.proto.Started -> {
                println("$LOG_PREFIX Actor ${self.id} started")
            }
            is AgentRequest -> {
                // 使用 kastrax Agent 处理请求
                // 添加详细日志以便调试
                println("$LOG_PREFIX Actor ${self.id} processing AgentRequest from ${sender?.id ?: "null"}, prompt: ${msg.prompt.take(50)}${if (msg.prompt.length > 50) "..." else ""}")

                // 添加安全检查，防止无限递归
                val currentDepth = msg.options.metadata?.get("depth")?.toIntOrNull() ?: 0

                // 如果递归深度超过最大值，返回错误消息
                if (currentDepth > MAX_RECURSION_DEPTH) {
                    val errorResponse = AgentResponse("[错误] 达到最大递归深度 $MAX_RECURSION_DEPTH", emptyList())
                    println("$LOG_PREFIX Actor ${self.id} reached max recursion depth $MAX_RECURSION_DEPTH, responding with error")
                    respond(errorResponse)
                    return
                }

                val options = if (msg.options.metadata?.get("sender") == agent.name) {
                    // 如果发送者是自己，创建一个新的选项对象，避免递归
                    println("$LOG_PREFIX Actor ${self.id} detected self-reference, creating new options to avoid recursion")
                    AgentGenerateOptions(metadata = mapOf(
                        "internal" to "true",
                        "depth" to (currentDepth + 1).toString()
                    ))
                } else {
                    // 更新递归深度
                    val updatedMetadata = msg.options.metadata?.toMutableMap() ?: mutableMapOf()
                    updatedMetadata["depth"] = (currentDepth + 1).toString()
                    println("$LOG_PREFIX Actor ${self.id} updated metadata: depth=${updatedMetadata["depth"]}")
                    msg.options.copy(metadata = updatedMetadata)
                }

                try {
                    println("$LOG_PREFIX Actor ${self.id} generating response using agent: ${agent.name}")
                    val response = runBlocking {
                        agent.generate(msg.prompt, options)
                    }
                    // 发送响应
                    val agentResponse = AgentResponse(response.text, response.toolCalls)
                    println("$LOG_PREFIX Actor ${self.id} responding to ${sender?.id ?: "null"} with text: ${agentResponse.text.take(50)}${if (agentResponse.text.length > 50) "..." else ""}")
                    respond(agentResponse)
                    println("$LOG_PREFIX Actor ${self.id} response sent successfully")
                } catch (e: Exception) {
                    println("$LOG_PREFIX Actor ${self.id} error generating response: ${e.message}")
                    e.printStackTrace()
                    println("$LOG_PREFIX Actor ${self.id} stack trace: ${e.stackTraceToString().lines().take(5).joinToString("\n")}")
                    val errorResponse = AgentResponse("[错误] 生成响应时发生异常: ${e.message}", emptyList())
                    respond(errorResponse)
                }
            }
            is AgentStreamRequest -> {
                // 处理流式请求
                val job = GlobalScope.launch {
                    val response = runBlocking {
                        agent.stream(msg.prompt, msg.options)
                    }
                    // 收集流并发送块
                    response.textStream?.let { flow ->
                        runBlocking {
                            flow.collect { chunk ->
                                send(msg.sender, AgentStreamChunk(chunk))
                            }
                        }
                    }
                    // 流结束后发送完成消息
                    send(msg.sender, AgentStreamComplete)
                }
                // 存储作业以便可以取消
                jobs["streamJob"] = job
            }
            is MultimodalRequest -> {
                // 处理多模态请求

                // 检查递归深度
                val currentDepth = msg.options.metadata?.get("depth")?.toIntOrNull() ?: 0

                if (currentDepth > MAX_RECURSION_DEPTH) {
                    val errorMessage = MultimodalMessage(
                        content = "[错误] 达到最大递归深度 $MAX_RECURSION_DEPTH",
                        type = MultimodalType.TEXT
                    )
                    respond(MultimodalResponse(errorMessage))
                    return
                }

                when (msg.message.type) {
                    MultimodalType.TEXT -> {
                        // 处理文本类型的多模态消息
                        val content = msg.message.content as? String ?: ""

                        // 添加安全检查，防止无限递归
                        val options = if (msg.options.metadata?.get("sender") == agent.name) {
                            // 如果发送者是自己，创建一个新的选项对象，避免递归
                            AgentGenerateOptions(metadata = mapOf(
                                "internal" to "true",
                                "depth" to (currentDepth + 1).toString()
                            ))
                        } else {
                            // 更新递归深度
                            val updatedMetadata = msg.options.metadata?.toMutableMap() ?: mutableMapOf()
                            updatedMetadata["depth"] = (currentDepth + 1).toString()
                            updatedMetadata["sender"] = self.id
                            msg.options.copy(metadata = updatedMetadata)
                        }

                        val response = runBlocking {
                            try {
                                agent.generate(content, options)
                            } catch (e: Exception) {
                                CoreAgentResponse("[错误] 生成响应时发生异常: ${e.message}", emptyList())
                            }
                        }

                        // 创建多模态响应
                        val responseMessage = MultimodalMessage(
                            content = response.text,
                            type = MultimodalType.TEXT
                        )
                        respond(MultimodalResponse(responseMessage, response.toolCalls))
                    }
                    MultimodalType.IMAGE -> {
                        // 处理图像类型的多模态消息
                        // 这里需要根据 Agent 的能力来处理图像
                        // 如果 Agent 不支持图像处理，可以返回错误消息
                        val errorMessage = MultimodalMessage(
                            content = "该 Agent 不支持图像处理",
                            type = MultimodalType.TEXT
                        )
                        respond(MultimodalResponse(errorMessage))
                    }
                    MultimodalType.AUDIO -> {
                        // 处理音频类型的多模态消息
                        // 这里需要根据 Agent 的能力来处理音频
                        // 如果 Agent 不支持音频处理，可以返回错误消息
                        val errorMessage = MultimodalMessage(
                            content = "该 Agent 不支持音频处理",
                            type = MultimodalType.TEXT
                        )
                        respond(MultimodalResponse(errorMessage))
                    }
                    MultimodalType.VIDEO -> {
                        // 处理视频类型的多模态消息
                        // 这里需要根据 Agent 的能力来处理视频
                        // 如果 Agent 不支持视频处理，可以返回错误消息
                        val errorMessage = MultimodalMessage(
                            content = "该 Agent 不支持视频处理",
                            type = MultimodalType.TEXT
                        )
                        respond(MultimodalResponse(errorMessage))
                    }
                    MultimodalType.FILE -> {
                        // 处理文件类型的多模态消息
                        // 这里需要根据 Agent 的能力来处理文件
                        // 如果 Agent 不支持文件处理，可以返回错误消息
                        val errorMessage = MultimodalMessage(
                            content = "该 Agent 不支持文件处理",
                            type = MultimodalType.TEXT
                        )
                        respond(MultimodalResponse(errorMessage))
                    }
                    MultimodalType.MIXED -> {
                        // 处理混合类型的多模态消息
                        // 这里需要根据 Agent 的能力来处理混合类型的消息
                        // 如果 Agent 不支持混合类型处理，可以返回错误消息
                        val errorMessage = MultimodalMessage(
                            content = "该 Agent 不支持混合类型处理",
                            type = MultimodalType.TEXT
                        )
                        respond(MultimodalResponse(errorMessage))
                    }
                }
            }
            is CancelRequest -> {
                // 取消正在进行的流式请求
                val job = jobs["streamJob"]
                job?.cancel()
                respond(CancelResponse(success = true))
            }
            is ToolCallRequest -> {
                // 处理工具调用
                val result = runBlocking {
                    // 在这里我们需要使用 agent 的工具执行功能
                    // 由于 Agent 接口没有直接的 executeTool 方法，我们需要模拟工具执行
                    val tools = (agent as? ai.kastrax.core.agent.LLMAgent)?.tools
                    val tool = tools?.get(msg.toolName)
                    if (tool != null) {
                        val result = tool.execute(msg.input)
                        if (result is JsonObject) {
                            result
                        } else {
                            JsonObject(mapOf("result" to result))
                        }
                    } else {
                        JsonObject(mapOf("error" to JsonPrimitive("Tool not found: ${msg.toolName}")))
                    }
                }
                respond(ToolCallResponse(result))
            }
            is CollaborationRequest -> {
                // 处理与其他 Agent 的协作请求
                // 添加安全检查，防止无限递归
                if (msg.sender == agent.name) {
                    // 如果发送者是自己，返回简单响应以避免递归
                    respond(CollaborationResponse("[内部处理] ${msg.task}"))
                    return
                }

                // 创建安全的元数据
                val safeMetadata = msg.metadata.toMutableMap()
                val currentDepth = safeMetadata["depth"]?.toIntOrNull() ?: 0
                safeMetadata["depth"] = (currentDepth + 1).toString()

                // 检查最大递归深度
                if (currentDepth >= MAX_RECURSION_DEPTH) {
                    respond(CollaborationResponse("[达到最大递归深度 $MAX_RECURSION_DEPTH] 无法继续处理请求"))
                    return
                }

                val collaborationResult = runBlocking {
                    agent.generate(
                        "处理来自 ${msg.sender} 的请求: ${msg.task}",
                        AgentGenerateOptions(metadata = safeMetadata)
                    )
                }
                respond(CollaborationResponse(collaborationResult.text))
            }
            is PoisonPill -> {
                // 优雅地关闭 Agent
                ActorSystem.default().stop(self)
            }
            else -> {
                println("$LOG_PREFIX Actor ${self.id} received unknown message type: ${msg::class.simpleName}")
                // 对于未知消息类型，返回错误响应
                if (sender != null) {
                    val errorResponse = AgentResponse("[错误] 不支持的消息类型: ${msg::class.simpleName}", emptyList())
                    respond(errorResponse)
                }
            }
        }
    }
}
