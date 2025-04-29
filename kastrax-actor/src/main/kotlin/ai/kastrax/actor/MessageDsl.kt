package ai.kastrax.actor

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.fromProducer
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentStreamOptions

/**
 * 发送消息给 Agent
 *
 * @param target 目标 Agent 的 PID
 * @param prompt 提示文本
 * @param options 生成选项
 */
fun ActorSystem.sendMessage(target: PID, prompt: String, options: AgentGenerateOptions = AgentGenerateOptions()) {
    root.send(target, AgentRequest(prompt, options))
}

/**
 * 请求-响应模式，向 Agent 发送请求并等待响应
 *
 * @param target 目标 Agent 的 PID
 * @param prompt 提示文本
 * @param options 生成选项
 * @return Agent 生成的文本
 */
suspend fun ActorSystem.askMessage(target: PID, prompt: String, options: AgentGenerateOptions = AgentGenerateOptions()): String {
    val response = root.requestAwait<AgentResponse>(target, AgentRequest(prompt, options))
    return response.text
}

/**
 * 流式请求，向 Agent 发送流式生成请求
 *
 * @param target 目标 Agent 的 PID
 * @param prompt 提示文本
 * @param options 流式生成选项
 * @param onChunk 处理文本块的回调函数
 */
fun ActorSystem.streamMessage(target: PID, prompt: String, options: AgentStreamOptions = AgentStreamOptions(), onChunk: (String) -> Unit) {
    val streamHandler = spawnStreamHandler(onChunk)
    root.send(target, AgentStreamRequest(prompt, options, streamHandler))
}

/**
 * 创建流处理 Actor
 *
 * @param onChunk 处理文本块的回调函数
 * @return 流处理 Actor 的 PID
 */
private fun ActorSystem.spawnStreamHandler(onChunk: (String) -> Unit): PID {
    val props = fromProducer {
        object : Actor {
            override suspend fun Context.receive(msg: Any) {
                when (msg) {
                    is AgentStreamChunk -> onChunk(msg.chunk)
                    is AgentStreamComplete -> ActorSystem.default().stop(self)
                }
            }
        }
    }
    return root.spawn(props)
}
