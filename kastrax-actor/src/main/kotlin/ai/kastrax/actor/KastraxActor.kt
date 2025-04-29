package ai.kastrax.actor

import actor.proto.Actor
import actor.proto.Context
import actor.proto.PoisonPill
import ai.kastrax.agent.Agent
import ai.kastrax.agent.AgentGenerateOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking

/**
 * KastraxActor 类，将 kastrax Agent 包装为 kactor Actor
 *
 * @property agent kastrax Agent 实例
 */
class KastraxActor(private val agent: Agent) : Actor {
    override fun receive(context: Context) {
        when (val msg = context.message) {
            is AgentRequest -> {
                // 使用 kastrax Agent 处理请求
                val response = runBlocking {
                    agent.generate(msg.prompt, msg.options)
                }
                // 发送响应
                context.respond(AgentResponse(response.text, response.toolCalls))
            }
            is AgentStreamRequest -> {
                // 处理流式请求
                val job = context.actorSystem.dispatcher.dispatch {
                    agent.stream(msg.prompt, msg.options) { chunk ->
                        context.send(msg.sender, AgentStreamChunk(chunk))
                    }
                    // 流结束后发送完成消息
                    context.send(msg.sender, AgentStreamComplete())
                }
                // 存储作业以便可以取消
                context.stash(job)
            }
            is CancelRequest -> {
                // 取消正在进行的流式请求
                val job = context.unstash() as? Job
                job?.cancel()
                context.respond(CancelResponse(success = true))
            }
            is ToolCallRequest -> {
                // 处理工具调用
                val result = runBlocking {
                    agent.executeTool(msg.toolName, msg.input)
                }
                context.respond(ToolCallResponse(result))
            }
            is CollaborationRequest -> {
                // 处理与其他 Agent 的协作请求
                val collaborationResult = runBlocking {
                    agent.generate(
                        "处理来自 ${msg.sender} 的请求: ${msg.task}",
                        AgentGenerateOptions(metadata = msg.metadata)
                    )
                }
                context.respond(CollaborationResponse(collaborationResult.text))
            }
            is PoisonPill -> {
                // 优雅地关闭 Agent
                context.stop(context.self)
            }
        }
    }
}
