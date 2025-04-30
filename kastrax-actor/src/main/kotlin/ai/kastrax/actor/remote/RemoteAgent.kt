package ai.kastrax.actor.remote

import actor.proto.ActorSystem
import actor.proto.PID
import ai.kastrax.actor.AgentMessage
import ai.kastrax.actor.AgentRequest
import ai.kastrax.actor.AgentResponse
import ai.kastrax.actor.AgentStreamRequest
import ai.kastrax.actor.ToolCallRequest
import ai.kastrax.actor.ToolCallResponse
import ai.kastrax.actor.CollaborationRequest
import ai.kastrax.actor.CollaborationResponse
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentStreamOptions
import kotlinx.serialization.json.JsonObject
import kotlin.time.Duration

/**
 * 远程 Agent 连接和通信
 *
 * @property system Actor 系统
 * @property address 远程地址，格式为 "hostname:port"
 */
class RemoteAgent(
    private val system: ActorSystem,
    private val address: String
) {
    /**
     * 连接到远程 Agent
     *
     * @param agentId Agent ID
     * @return PID 对象
     */
    fun connect(agentId: String): PID {
        // 确保地址格式正确
        val effectiveAddress = if (address.startsWith("127.0.0.1") || address.startsWith("localhost")) {
            address
        } else {
            "127.0.0.1:${address.substringAfter(':')}"
        }

        println("Connecting to remote agent at $effectiveAddress/$agentId")
        return PID(effectiveAddress, agentId)
    }

    /**
     * 发送消息给远程 Agent
     *
     * @param agentId Agent ID
     * @param message 消息
     */
    fun send(agentId: String, message: AgentMessage) {
        val pid = connect(agentId)
        system.root.send(pid, message)
    }

    /**
     * 请求-响应模式
     *
     * @param agentId Agent ID
     * @param message 消息
     * @param timeout 超时时间
     * @return 响应消息
     */
    suspend fun ask(agentId: String, message: AgentMessage, timeout: Duration = Duration.parse("30s")): AgentMessage {
        val pid = connect(agentId)
        try {
            // 添加日志以便调试
            println("Sending request to $pid with message: $message")

            // 记录远程 Agent 的地址
            val remoteAddress = "$address/$agentId"
            println("Remote agent address: $remoteAddress")

            // 将 kotlin.time.Duration 转换为 java.time.Duration
            val javaDuration = java.time.Duration.ofMillis(timeout.inWholeMilliseconds)
            println("Using timeout: $javaDuration")

            // 先发送一个测试消息，确保远程 Actor 可访问
            println("Sending test message to $pid")
            system.root.send(pid, message)

            // 等待一下，确保消息已经到达
            kotlinx.coroutines.delay(1000)

            // 发送请求并等待响应
            println("Sending request and waiting for response...")
            val result = system.root.requestAwait<AgentMessage>(pid, message, javaDuration)
            println("Received response from $pid: $result")
            return result
        } catch (e: Exception) {
            println("Error while asking $pid: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * 生成响应
     *
     * @param agentId Agent ID
     * @param prompt 提示
     * @param options 生成选项
     * @return Agent 响应
     */
    suspend fun generate(agentId: String, prompt: String, options: AgentGenerateOptions = AgentGenerateOptions()): AgentResponse {
        val pid = connect(agentId)
        return system.root.requestAwait(pid, AgentRequest(prompt, options))
    }

    /**
     * 流式生成
     *
     * @param agentId Agent ID
     * @param prompt 提示
     * @param options 流式选项
     * @param sender 发送者 PID
     */
    fun stream(agentId: String, prompt: String, options: AgentStreamOptions = AgentStreamOptions(), sender: PID) {
        val pid = connect(agentId)
        system.root.send(pid, AgentStreamRequest(prompt, options, sender))
    }

    /**
     * 执行工具调用
     *
     * @param agentId Agent ID
     * @param toolName 工具名称
     * @param input 输入参数
     * @return 工具调用结果
     */
    suspend fun executeTool(agentId: String, toolName: String, input: JsonObject): ToolCallResponse {
        val pid = connect(agentId)
        return system.root.requestAwait(pid, ToolCallRequest(toolName, input))
    }

    /**
     * 发送协作请求
     *
     * @param agentId Agent ID
     * @param task 任务描述
     * @param sender 发送者名称
     * @param metadata 元数据
     * @return 协作响应
     */
    suspend fun collaborate(agentId: String, task: String, sender: String, metadata: Map<String, String> = emptyMap()): CollaborationResponse {
        val pid = connect(agentId)
        return system.root.requestAwait(pid, CollaborationRequest(task, sender, metadata))
    }
}
