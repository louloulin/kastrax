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
    private val jobs = ConcurrentHashMap<String, Job>()
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is AgentRequest -> {
                // 使用 kastrax Agent 处理请求
                // 添加安全检查，防止无限递归
                val options = if (msg.options.metadata?.get("sender") == agent.name) {
                    // 如果发送者是自己，创建一个新的选项对象，避免递归
                    AgentGenerateOptions(metadata = mapOf("internal" to "true"))
                } else {
                    msg.options
                }

                val response = runBlocking {
                    agent.generate(msg.prompt, options)
                }
                // 发送响应
                respond(AgentResponse(response.text, response.toolCalls))
            }
            is AgentStreamRequest -> {
                // 处理流式请求
                val job = kotlinx.coroutines.GlobalScope.launch {
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
                when (msg.message.type) {
                    MultimodalType.TEXT -> {
                        // 处理文本类型的多模态消息
                        val content = msg.message.content as? String ?: ""

                        // 添加安全检查，防止无限递归
                        val options = if (msg.options.metadata?.get("sender") == agent.name) {
                            // 如果发送者是自己，创建一个新的选项对象，避免递归
                            AgentGenerateOptions(metadata = mapOf("internal" to "true"))
                        } else {
                            msg.options
                        }

                        val response = runBlocking {
                            agent.generate(content, options)
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
                safeMetadata["max_depth"] = ((safeMetadata["max_depth"]?.toIntOrNull() ?: 0) + 1).toString()

                // 检查最大递归深度
                if ((safeMetadata["max_depth"]?.toIntOrNull() ?: 0) > 5) {
                    respond(CollaborationResponse("[达到最大递归深度] 无法继续处理请求"))
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
        }
    }
}
