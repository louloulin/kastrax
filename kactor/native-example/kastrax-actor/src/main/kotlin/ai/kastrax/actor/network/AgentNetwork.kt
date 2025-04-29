package ai.kastrax.actor.network

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.fromProducer
import ai.kastrax.actor.AgentRequest
import ai.kastrax.actor.AgentResponse
import ai.kastrax.actor.KastraxActor
import ai.kastrax.actor.network.protocols.CollaborationProtocol
import ai.kastrax.actor.network.protocols.CollaborationResult
import ai.kastrax.actor.network.protocols.CollaborationStep
import ai.kastrax.core.agent.Agent
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Agent 之间的关系类型
 *
 * 定义了 Agent 之间可能存在的各种关系，用于构建自主性的分布式 AI Agent 网络
 */
enum class AgentRelation {
    /**
     * 对等关系 - Agents 之间平等协作
     */
    PEER,

    /**
     * 主从关系（源节点是主节点）- 主 Agent 可以指导和分配任务给从 Agent
     */
    MASTER,

    /**
     * 主从关系（源节点是从节点）- 从 Agent 接受主 Agent 的指导和任务
     */
    SLAVE,

    /**
     * 协作关系 - Agents 之间紧密协作，共同解决问题
     */
    COLLABORATOR,

    /**
     * 监督关系（源节点是监督者）- 监督 Agent 监控和评估被监督 Agent 的表现
     */
    SUPERVISOR,

    /**
     * 监督关系（源节点是被监督者）- 被监督 Agent 接受监督 Agent 的监控和评估
     */
    SUPERVISED
}



/**
 * Agent 网络，管理多个 Agent 之间的协作关系
 *
 * Agent 网络是一个自主性的分布式 AI Agent 系统，支持 Agent 之间的关系管理、消息传递和协作
 *
 * @property system Actor 系统
 * @property topology 网络拓扑结构
 */
class AgentNetwork(
    private val system: ActorSystem,
    private val topology: NetworkTopology = NetworkTopology()
) {
    /**
     * Agent ID 到 PID 的映射
     */
    private val agentPids = ConcurrentHashMap<String, PID>()

    /**
     * 添加 Agent 到网络
     *
     * @param agent Agent 实例
     * @param id Agent ID
     * @return Agent 的 PID
     */
    fun addAgent(agent: Agent, id: String = agent.name): PID {
        // 创建 Actor
        val props = fromProducer { KastraxActor(agent) }
        val pid = system.root.spawnNamed(props, id)

        // 添加到映射
        agentPids[id] = pid

        // 添加到拓扑结构
        topology.addNode(id)

        return pid
    }

    /**
     * 连接两个 Agent
     *
     * @param fromId 源 Agent ID
     * @param toId 目标 Agent ID
     * @param relation 关系类型
     */
    fun connectAgents(fromId: String, toId: String, relation: AgentRelation = AgentRelation.PEER) {
        topology.addEdge(fromId, toId, relation)
    }

    /**
     * 断开两个 Agent 的连接
     *
     * @param fromId 源 Agent ID
     * @param toId 目标 Agent ID
     */
    fun disconnectAgents(fromId: String, toId: String) {
        topology.removeEdge(fromId, toId)
    }

    /**
     * 移除 Agent
     *
     * @param id Agent ID
     */
    fun removeAgent(id: String) {
        // 从拓扑结构中移除
        topology.removeNode(id)

        // 停止 Actor
        val pid = agentPids[id]
        if (pid != null) {
            system.root.stop(pid)
            agentPids.remove(id)
        }
    }

    /**
     * 获取 Agent 的 PID
     *
     * @param id Agent ID
     * @return Agent 的 PID，如果不存在则返回 null
     */
    fun getAgentPid(id: String): PID? {
        return agentPids[id]
    }

    /**
     * 获取与指定 Agent 相连的所有 Agent
     *
     * @param id Agent ID
     * @return 相连的 Agent ID 列表
     */
    fun getConnectedAgents(id: String): List<String> {
        return topology.getConnectedNodes(id)
    }

    /**
     * 获取与指定 Agent 具有特定关系的所有 Agent
     *
     * @param id Agent ID
     * @param relation 关系类型
     * @return 具有特定关系的 Agent ID 列表
     */
    fun getAgentsByRelation(id: String, relation: AgentRelation): List<String> {
        return topology.getNodesByRelation(id, relation)
    }

    /**
     * 发送消息给指定 Agent
     *
     * @param fromId 源 Agent ID
     * @param toId 目标 Agent ID
     * @param message 消息内容
     */
    fun sendMessage(fromId: String, toId: String, message: String) {
        val toPid = agentPids[toId] ?: return

        // 创建请求
        val request = AgentRequest(
            prompt = "来自 $fromId 的消息: $message",
            options = ai.kastrax.core.agent.AgentGenerateOptions(
                metadata = mapOf("sender" to fromId)
            )
        )

        // 发送消息
        system.root.send(toPid, request)
    }

    /**
     * 向指定 Agent 发送请求并等待响应
     *
     * @param fromId 源 Agent ID
     * @param toId 目标 Agent ID
     * @param message 消息内容
     * @param timeout 超时时间
     * @return Agent 的响应
     */
    suspend fun askAgent(
        fromId: String,
        toId: String,
        message: String,
        timeout: Duration = Duration.ofSeconds(30)
    ): AgentResponse {
        val toPid = agentPids[toId] ?: throw IllegalArgumentException("Agent not found: $toId")

        // 创建请求
        val request = AgentRequest(
            prompt = "来自 $fromId 的请求: $message",
            options = ai.kastrax.core.agent.AgentGenerateOptions(
                metadata = mapOf("sender" to fromId)
            )
        )

        // 发送请求并等待响应
        return system.root.requestAwait(toPid, request, timeout)
    }

    /**
     * 广播消息给所有连接的 Agent
     *
     * @param fromId 源 Agent ID
     * @param message 消息内容
     */
    fun broadcastMessage(fromId: String, message: String) {
        // 获取所有连接的 Agent
        val connectedAgents = getConnectedAgents(fromId)

        // 向每个 Agent 发送消息
        connectedAgents.forEach { toId ->
            sendMessage(fromId, toId, message)
        }
    }

    /**
     * 执行协作任务
     *
     * @param protocol 协作协议
     * @param initiatorId 发起者 Agent ID
     * @param task 任务描述
     * @return 协作结果
     */
    suspend fun collaborate(
        protocol: CollaborationProtocol,
        initiatorId: String,
        task: String
    ): CollaborationResult {
        return protocol.execute(this, initiatorId, task)
    }

    /**
     * 关闭网络
     */
    fun shutdown() {
        // 停止所有 Actor
        agentPids.forEach { (_, pid) ->
            system.root.stop(pid)
        }

        // 清空映射
        agentPids.clear()

        // 清空拓扑结构
        topology.clear()
    }
}


