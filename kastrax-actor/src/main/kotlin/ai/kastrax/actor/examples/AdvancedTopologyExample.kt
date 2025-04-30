package ai.kastrax.actor.examples

import actor.proto.ActorSystem
import actor.proto.fromProducer
import ai.kastrax.actor.AgentRequest
import ai.kastrax.actor.KastraxActor
import ai.kastrax.actor.network.AgentNetwork
import ai.kastrax.actor.network.AgentRelation
import ai.kastrax.actor.network.topology.*
import ai.kastrax.actor.network.NetworkTopology
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ai.kastrax.core.agent.AgentStatus as CoreAgentStatus

/**
 * 高级拓扑示例
 *
 * 本示例展示了如何使用 kastrax-actor 模块提供的高级网络拓扑
 */
object AdvancedTopologyExample {

    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 创建 Actor 系统
        val system = ActorSystem("advanced-topology-example")

        try {
            // 根据命令行参数选择要运行的示例
            when (args.firstOrNull()?.lowercase()) {
                "dynamic" -> dynamicTopologyExample(system)
                "hierarchical" -> hierarchicalTopologyExample(system)
                "smallworld" -> smallWorldTopologyExample(system)
                "rolebased" -> roleBasedTopologyExample(system)
                else -> {
                    // 默认运行所有示例
                    dynamicTopologyExample(system)
                    hierarchicalTopologyExample(system)
                    smallWorldTopologyExample(system)
                    roleBasedTopologyExample(system)
                }
            }
        } finally {
            // 关闭系统
            system.shutdown()
        }
    }

    /**
     * 动态拓扑示例
     */
    private suspend fun dynamicTopologyExample(system: ActorSystem) {
        println("\n=== 动态拓扑示例 ===\n")

        // 创建动态拓扑
        val topology = DynamicTopology()

        // 创建 Agent 网络
        val network = AgentNetwork(system, NetworkTopology())

        // 创建 Agent
        val agents = listOf(
            SimpleAgent("agent1", "Agent 1"),
            SimpleAgent("agent2", "Agent 2"),
            SimpleAgent("agent3", "Agent 3"),
            SimpleAgent("agent4", "Agent 4"),
            SimpleAgent("agent5", "Agent 5")
        )

        // 添加 Agent 到网络
        agents.forEach { network.addAgent(it) }

        // 创建研究拓扑配置
        val researchConfig = TopologyConfiguration(
            nodes = listOf("agent1", "agent2", "agent3"),
            edges = listOf(
                Triple("agent1", "agent2", AgentRelation.PEER),
                Triple("agent1", "agent3", AgentRelation.PEER),
                Triple("agent2", "agent3", AgentRelation.PEER)
            ),
            taskTypes = listOf("research", "analysis")
        )

        // 创建开发拓扑配置
        val developmentConfig = TopologyConfiguration(
            nodes = listOf("agent1", "agent3", "agent4", "agent5"),
            edges = listOf(
                Triple("agent1", "agent3", AgentRelation.MASTER),
                Triple("agent1", "agent4", AgentRelation.MASTER),
                Triple("agent1", "agent5", AgentRelation.MASTER),
                Triple("agent3", "agent4", AgentRelation.PEER),
                Triple("agent3", "agent5", AgentRelation.PEER),
                Triple("agent4", "agent5", AgentRelation.PEER)
            ),
            taskTypes = listOf("development", "implementation")
        )

        // 添加拓扑配置
        topology.addTopologyConfiguration("research", researchConfig)
        topology.addTopologyConfiguration("development", developmentConfig)

        // 激活研究拓扑
        println("激活研究拓扑配置")
        topology.activateTopologyConfiguration("research")

        // 打印当前拓扑结构
        printTopologyStructure(topology)

        // 模拟任务类型变化
        println("\n任务类型变更为 'development'")
        topology.selectTopologyForTask("development")

        // 打印新的拓扑结构
        printTopologyStructure(topology)

        // 创建拓扑快照
        val snapshot = topology.createSnapshot()

        // 清除拓扑结构
        topology.clear()
        println("\n清除拓扑结构后的节点数: ${topology.getAllNodes().size}")

        // 从快照恢复
        println("从快照恢复拓扑结构")
        topology.restoreFromSnapshot(snapshot)

        // 打印恢复后的拓扑结构
        printTopologyStructure(topology)

        // 关闭网络
        network.shutdown()
    }

    /**
     * 层次拓扑示例
     */
    private suspend fun hierarchicalTopologyExample(system: ActorSystem) {
        println("\n=== 层次拓扑示例 ===\n")

        // 创建层次拓扑
        val topology = HierarchicalTopology()

        // 创建 Agent 网络
        val network = AgentNetwork(system, NetworkTopology())

        // 创建 Agent
        val agents = listOf(
            SimpleAgent("ceo", "CEO"),
            SimpleAgent("cto", "CTO"),
            SimpleAgent("cfo", "CFO"),
            SimpleAgent("dev1", "Developer 1"),
            SimpleAgent("dev2", "Developer 2"),
            SimpleAgent("dev3", "Developer 3"),
            SimpleAgent("accountant1", "Accountant 1"),
            SimpleAgent("accountant2", "Accountant 2")
        )

        // 添加 Agent 到网络
        agents.forEach { network.addAgent(it) }

        // 构建层次结构
        topology.addRootNode("ceo")
        topology.addChildNode("ceo", "cto")
        topology.addChildNode("ceo", "cfo")
        topology.addChildNode("cto", "dev1")
        topology.addChildNode("cto", "dev2")
        topology.addChildNode("cto", "dev3")
        topology.addChildNode("cfo", "accountant1")
        topology.addChildNode("cfo", "accountant2")

        // 打印层次结构
        println("层次结构:")
        printHierarchy(topology, "ceo", 0)

        // 获取各层级的节点
        println("\n各层级节点:")
        for (level in 0..topology.getMaxDepth()) {
            val nodesAtLevel = topology.getNodesAtLevel(level)
            println("层级 $level: ${nodesAtLevel.joinToString()}")
        }

        // 获取叶子节点
        val leafNodes = topology.getLeafNodes()
        println("\n叶子节点: ${leafNodes.joinToString()}")

        // 移动节点
        println("\n将 dev3 从 CTO 下移动到 CFO 下")
        topology.moveNode("dev3", "cfo")

        // 打印更新后的层次结构
        println("\n更新后的层次结构:")
        printHierarchy(topology, "ceo", 0)

        // 获取两个节点的最近公共祖先
        val lca = topology.getLowestCommonAncestor("dev1", "accountant1")
        println("\ndev1 和 accountant1 的最近公共祖先: $lca")

        // 关闭网络
        network.shutdown()
    }

    /**
     * 小世界拓扑示例
     */
    private suspend fun smallWorldTopologyExample(system: ActorSystem) {
        println("\n=== 小世界拓扑示例 ===\n")

        // 创建小世界拓扑
        val topology = SmallWorldTopology()

        // 创建 Agent 网络
        val network = AgentNetwork(system, NetworkTopology())

        // 创建 Agent
        val agentIds = (1..20).map { "agent$it" }
        val agents = agentIds.map { SimpleAgent(it, "Agent ${it.substring(5)}") }

        // 添加 Agent 到网络
        agents.forEach { network.addAgent(it) }

        // 创建小世界网络
        topology.createSmallWorldNetwork(agentIds, k = 4, beta = 0.2)

        // 打印网络统计信息
        val clusteringCoefficient = topology.calculateClusteringCoefficient()
        val averagePathLength = topology.calculateAveragePathLength()
        val smallWorldness = topology.calculateSmallWorldness()

        println("网络统计信息:")
        println("节点数: ${agentIds.size}")
        println("聚类系数: $clusteringCoefficient")
        println("平均路径长度: $averagePathLength")
        println("小世界系数: $smallWorldness")

        // 检测社区结构
        val communities = topology.detectCommunities()

        println("\n检测到的社区结构:")
        communities.forEach { (communityId, members) ->
            println("社区 $communityId: ${members.joinToString()}")
        }

        // 添加桥接节点
        val bridgeNodeId = "bridge"
        network.addAgent(SimpleAgent(bridgeNodeId, "Bridge Node"))

        // 选择每个社区的一个代表节点
        val representativeNodes = communities.values.map { it.first() }

        // 添加桥接
        topology.addBridgeNode(bridgeNodeId, representativeNodes)

        // 重新计算网络统计信息
        val newAveragePathLength = topology.calculateAveragePathLength()
        println("\n添加桥接节点后的平均路径长度: $newAveragePathLength")

        // 关闭网络
        network.shutdown()
    }

    /**
     * 基于角色的拓扑示例
     */
    private suspend fun roleBasedTopologyExample(system: ActorSystem) {
        println("\n=== 基于角色的拓扑示例 ===\n")

        // 创建基于角色的拓扑
        val topology = RoleBasedTopology()

        // 创建 Agent 网络
        val network = AgentNetwork(system, NetworkTopology())

        // 定义角色
        val managerRole = RoleBasedTopology.Role(
            name = "manager",
            description = "项目管理者，负责协调团队工作",
            capabilities = setOf("planning", "coordination", "evaluation")
        )

        val developerRole = RoleBasedTopology.Role(
            name = "developer",
            description = "开发人员，负责实现功能",
            capabilities = setOf("coding", "testing", "debugging")
        )

        val designerRole = RoleBasedTopology.Role(
            name = "designer",
            description = "设计师，负责用户界面和用户体验设计",
            capabilities = setOf("ui_design", "ux_design", "prototyping")
        )

        val testerRole = RoleBasedTopology.Role(
            name = "tester",
            description = "测试人员，负责质量保证",
            capabilities = setOf("testing", "quality_assurance", "bug_reporting")
        )

        // 添加角色
        topology.addRole(managerRole)
        topology.addRole(developerRole)
        topology.addRole(designerRole)
        topology.addRole(testerRole)

        // 创建 Agent
        val agents = listOf(
            SimpleAgent("alice", "Alice"),
            SimpleAgent("bob", "Bob"),
            SimpleAgent("charlie", "Charlie"),
            SimpleAgent("dave", "Dave"),
            SimpleAgent("eve", "Eve"),
            SimpleAgent("frank", "Frank"),
            SimpleAgent("grace", "Grace")
        )

        // 添加 Agent 到网络
        agents.forEach { network.addAgent(it) }

        // 分配角色
        topology.assignRole("alice", "manager")
        topology.assignRole("bob", "developer")
        topology.assignRole("charlie", "developer")
        topology.assignRole("dave", "developer")
        topology.assignRole("eve", "designer")
        topology.assignRole("frank", "tester")
        topology.assignRole("grace", "tester")

        // 创建团队结构
        topology.createTeamStructure(
            leaderRole = "manager",
            memberRoles = listOf("developer", "designer", "tester")
        )

        // 打印角色分配
        println("角色分配:")
        agents.forEach { agent ->
            val role = topology.getNodeRole(agent.name) ?: "未分配"
            println("${agent.name}: $role")
        }

        // 打印每个角色的 Agent
        println("\n每个角色的 Agent:")
        topology.getAllRoles().keys.forEach { role ->
            val nodesWithRole = topology.getNodesByRole(role)
            println("$role: ${nodesWithRole.joinToString()}")
        }

        // 打印具有特定能力的 Agent
        println("\n具有 'testing' 能力的 Agent:")
        val testersWithCapability = topology.getNodesByCapability("testing")
        println(testersWithCapability.joinToString())

        // 打印网络结构
        println("\n网络结构:")
        val allNodes = topology.getAllNodes()
        allNodes.forEach { node ->
            val connectedNodes = topology.getConnectedNodes(node)
            println("$node 连接到: ${connectedNodes.joinToString()}")
        }

        // 关闭网络
        network.shutdown()
    }

    /**
     * 打印拓扑结构
     */
    private fun printTopologyStructure(topology: NetworkTopology) {
        println("拓扑结构:")
        val nodes = topology.getAllNodes()
        nodes.forEach { node ->
            val connectedNodes = topology.getConnectedNodes(node)
            println("$node 连接到: ${connectedNodes.joinToString()}")
        }
    }

    /**
     * 打印拓扑结构 (动态拓扑版本)
     */
    private fun printTopologyStructure(topology: DynamicTopology) {
        println("拓扑结构:")
        val nodes = topology.getAllNodes()
        nodes.forEach { node ->
            val connectedNodes = topology.getConnectedNodes(node)
            println("$node 连接到: ${connectedNodes.joinToString()}")
        }
    }

    /**
     * 递归打印层次结构
     */
    private fun printHierarchy(topology: HierarchicalTopology, nodeId: String, level: Int) {
        val indent = "  ".repeat(level)
        println("$indent$nodeId")

        val children = topology.getChildNodes(nodeId)
        children.forEach { child ->
            printHierarchy(topology, child, level + 1)
        }
    }

    /**
     * 简单 Agent 实现
     */
    private class SimpleAgent(
        override val name: String,
        private val displayName: String
    ) : Agent {
        override val versionManager: AgentVersionManager? = null

        override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): CoreAgentResponse {
            val lastMessage = messages.lastOrNull { it.role.name.equals("USER", ignoreCase = true) }?.content ?: ""
            return generate(lastMessage, options)
        }

        override suspend fun generate(prompt: String, options: AgentGenerateOptions): CoreAgentResponse {
            return CoreAgentResponse(
                text = "[$displayName] 响应: $prompt",
                toolCalls = emptyList()
            )
        }

        override suspend fun stream(prompt: String, options: AgentStreamOptions): CoreAgentResponse {
            val response = generate(prompt, AgentGenerateOptions())
            return CoreAgentResponse(
                text = response.text,
                toolCalls = emptyList(),
                textStream = flowOf(response.text)
            )
        }

        override suspend fun getState(): AgentState {
            return AgentState(
                status = CoreAgentStatus.IDLE,
                lastUpdated = Clock.System.now()
            )
        }

        override suspend fun updateState(status: AgentStatus): AgentState? {
            // 简单实现，返回更新后的状态
            return AgentState(
                status = status,
                lastUpdated = Clock.System.now()
            )
        }

        override suspend fun createSession(
            title: String?,
            resourceId: String?,
            metadata: Map<String, String>
        ): SessionInfo? {
            return null
        }

        override suspend fun getSession(sessionId: String): SessionInfo? {
            return null
        }

        override suspend fun getSessionMessages(sessionId: String, limit: Int): List<SessionMessage>? {
            return emptyList()
        }

        override suspend fun reset() {
            // 不做任何操作
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
}
