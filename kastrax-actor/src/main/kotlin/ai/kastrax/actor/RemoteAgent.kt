package ai.kastrax.actor

import actor.proto.ActorSystem
import actor.proto.PID
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

/**
 * RemoteAgent 类，用于连接和操作远程 Agent
 *
 * @property system ActorSystem 实例
 * @property address 远程地址
 */
class RemoteAgent(
    private val system: ActorSystem,
    private val address: String
) {
    /**
     * 连接到远程 Agent
     *
     * @param agentId Agent ID
     * @return 远程 Agent 的 PID
     */
    fun connect(agentId: String): PID {
        return PID(address, agentId)
    }

    /**
     * 发送消息给远程 Agent
     *
     * @param agentId Agent ID
     * @param message 要发送的消息
     */
    fun send(agentId: String, message: AgentMessage) {
        val pid = connect(agentId)
        system.root.send(pid, message)
    }

    /**
     * 请求-响应模式，向远程 Agent 发送请求并等待响应
     *
     * @param agentId Agent ID
     * @param message 要发送的消息
     * @param timeout 超时时间，默认为 30 秒
     * @return 远程 Agent 的响应
     */
    suspend fun ask(agentId: String, message: AgentMessage, timeout: Duration = Duration.parse("30s")): AgentMessage {
        val pid = connect(agentId)
        return withTimeout(timeout) {
            system.root.requestAwait<AgentMessage>(pid, message)
        }
    }
}
