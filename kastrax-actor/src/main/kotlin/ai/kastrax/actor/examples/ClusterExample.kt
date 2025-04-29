package ai.kastrax.actor.examples

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.cluster.Cluster
import actor.proto.cluster.Kind
import actor.proto.fromProducer
import actor.proto.requestAwait
import ai.kastrax.actor.AgentRequest
import ai.kastrax.actor.AgentResponse
import ai.kastrax.actor.KastraxActor
import ai.kastrax.actor.cluster.ClusterConfig
import ai.kastrax.actor.cluster.configureCluster
import ai.kastrax.actor.cluster.joinCluster
import ai.kastrax.actor.cluster.leaveCluster
import ai.kastrax.actor.cluster.getClusterMembers
import ai.kastrax.actor.cluster.getCluster
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse as CoreAgentResponse
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.SessionInfo
import ai.kastrax.core.agent.SessionMessage
import ai.kastrax.core.agent.AgentState
import ai.kastrax.core.agent.AgentStatus
import ai.kastrax.core.agent.version.AgentVersion
import ai.kastrax.core.agent.version.AgentVersionManager
import ai.kastrax.core.llm.LlmMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.time.Duration

/**
 * 集群示例 Agent 实现
 */
class ClusterMockAgent(override val name: String) : Agent {
    override val versionManager: AgentVersionManager? = null

    override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): CoreAgentResponse {
        val lastMessage = messages.lastOrNull { it.role.name.equals("USER", ignoreCase = true) }?.content ?: ""
        return generate(lastMessage, options)
    }

    override suspend fun generate(prompt: String, options: AgentGenerateOptions): CoreAgentResponse {
        return CoreAgentResponse(
            text = "[$name] 回答: $prompt",
            toolCalls = emptyList()
        )
    }

    override suspend fun stream(prompt: String, options: AgentStreamOptions): CoreAgentResponse {
        return CoreAgentResponse(
            text = "[$name] 流式回答: $prompt",
            toolCalls = emptyList(),
            textStream = flowOf("[$name]", " 流式", "回答", ": ", prompt)
        )
    }

    override suspend fun reset() {
        // 不做任何操作
    }

    override suspend fun getState(): AgentState? {
        return null
    }

    override suspend fun updateState(status: AgentStatus): AgentState? {
        return null
    }

    override suspend fun createSession(title: String?, resourceId: String?, metadata: Map<String, String>): SessionInfo? {
        return null
    }

    override suspend fun getSession(sessionId: String): SessionInfo? {
        return null
    }

    override suspend fun getSessionMessages(sessionId: String, limit: Int): List<SessionMessage>? {
        return emptyList()
    }

    override suspend fun createVersion(
        instructions: String,
        name: String?,
        description: String?,
        metadata: Map<String, String>,
        activateImmediately: Boolean
    ): AgentVersion? {
        return null
    }

    override suspend fun getVersions(limit: Int, offset: Int): List<AgentVersion>? {
        return emptyList()
    }

    override suspend fun getActiveVersion(): AgentVersion? {
        return null
    }

    override suspend fun activateVersion(versionId: String): AgentVersion? {
        return null
    }

    override suspend fun rollbackToVersion(versionId: String): AgentVersion? {
        return null
    }
}

/**
 * 集群示例
 */
object ClusterExample {
    /**
     * 启动种子节点
     */
    fun startSeedNode() {
        // 配置集群
        val config = ClusterConfig(
            hostname = "localhost",
            port = 8090,
            clusterName = "kastrax-demo-cluster",
            seeds = listOf("localhost:8090")
        )
        val system = configureCluster("seed-node", config)

        // 创建 Agent
        val seedAgent = ClusterMockAgent("种子节点代理")

        // 创建 Actor Props
        val props = fromProducer { KastraxActor(seedAgent) }

        // 获取集群实例
        val cluster = system.getCluster(config)

        // 注册 Kind
        val kind = Kind("assistant", props)
        cluster.registerKind(kind)

        // 加入集群
        system.joinCluster()

        println("种子节点已启动，监听端口: ${config.port}")
        println("集群名称: ${config.clusterName}")
        println("按 Ctrl+C 停止服务器")

        // 保持系统运行
        runBlocking {
            while (true) {
                val members = system.getClusterMembers()
                println("当前集群成员: $members")
                delay(5000)
            }
        }
    }

    /**
     * 启动工作节点
     */
    fun startWorkerNode(port: Int, name: String) {
        // 配置集群
        val config = ClusterConfig(
            hostname = "localhost",
            port = port,
            clusterName = "kastrax-demo-cluster",
            seeds = listOf("localhost:8090")
        )
        val system = configureCluster(name, config)

        // 创建 Agent
        val workerAgent = ClusterMockAgent("$name-代理")

        // 创建 Actor Props
        val props = fromProducer { KastraxActor(workerAgent) }

        // 获取集群实例
        val cluster = system.getCluster(config)

        // 注册 Kind
        val kind = Kind("assistant", props)
        cluster.registerKind(kind)

        // 加入集群
        system.joinCluster()

        println("工作节点已启动，监听端口: ${config.port}")
        println("集群名称: ${config.clusterName}")
        println("按 Ctrl+C 停止服务器")

        // 保持系统运行
        runBlocking {
            while (true) {
                val members = system.getClusterMembers()
                println("当前集群成员: $members")
                delay(5000)
            }
        }
    }

    /**
     * 启动客户端
     */
    fun startClient() = runBlocking {
        // 配置集群
        val config = ClusterConfig(
            hostname = "localhost",
            port = 0, // 使用随机端口
            clusterName = "kastrax-demo-cluster",
            seeds = listOf("localhost:8090")
        )
        val system = configureCluster("client-node", config)

        // 加入集群
        system.joinCluster()

        println("客户端已连接到集群")

        // 获取集群成员
        val members = system.getClusterMembers()
        println("当前集群成员: $members")

        // 获取集群实例
        val cluster = system.getCluster(config)

        // 获取虚拟 Actor
        val assistantPid = cluster.get("client-assistant", "assistant")

        // 发送消息
        val response = system.root.requestAwait<AgentResponse>(
            assistantPid,
            AgentRequest("你好，集群助手！"),
            Duration.ofSeconds(5)
        )
        println("集群助手回答: ${response.text}")

        // 广播消息
        println("广播消息给所有助手...")
        cluster.pubSub.publish("assistant", AgentRequest("这是一条广播消息"))

        // 离开集群
        system.leaveCluster()
        println("已离开集群")
    }

    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty() || args[0] == "seed") {
            startSeedNode()
        } else if (args[0] == "worker") {
            val port = if (args.size > 1) args[1].toInt() else 8091
            val name = if (args.size > 2) args[2] else "worker-node"
            startWorkerNode(port, name)
        } else if (args[0] == "client") {
            startClient()
        } else {
            println("用法: ClusterExample [seed|worker|client]")
            println("  seed: 启动种子节点")
            println("  worker [port] [name]: 启动工作节点")
            println("  client: 启动客户端")
        }
    }
}
