package ai.kastrax.actor.examples

import actor.proto.ActorSystem
import actor.proto.fromProducer
import ai.kastrax.actor.AgentRequest
import ai.kastrax.actor.KastraxActor
import ai.kastrax.actor.monitoring.*
import ai.kastrax.actor.network.AgentNetwork
import ai.kastrax.actor.network.AgentRelation
import ai.kastrax.actor.network.NetworkTopology
import ai.kastrax.actor.network.protocols.*
import ai.kastrax.actor.network.topology.*
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.random.Random

/**
 * 监控和可视化示例
 *
 * 本示例展示了如何使用 kastrax-actor 模块提供的监控和可视化工具
 */
object MonitoringVisualizationExample {

    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 创建 Actor 系统
        val system = ActorSystem("monitoring-visualization-example")

        try {
            // 根据命令行参数选择要运行的示例
            when (args.firstOrNull()?.lowercase()) {
                "network" -> networkVisualizationExample(system)
                "performance" -> performanceDashboardExample(system)
                "collaboration" -> collaborationFlowExample(system)
                "metrics" -> metricsCollectorExample(system)
                else -> {
                    // 默认运行所有示例
                    networkVisualizationExample(system)
                    performanceDashboardExample(system)
                    collaborationFlowExample(system)
                    metricsCollectorExample(system)
                }
            }
        } finally {
            // 关闭系统
            system.shutdown()
        }
    }

    /**
     * 网络可视化示例
     */
    private suspend fun networkVisualizationExample(system: ActorSystem) {
        println("\n=== 网络可视化示例 ===\n")

        // 创建不同类型的拓扑
        val topologies = listOf(
            createHierarchicalTopology(),
            createSmallWorldTopology(),
            createRoleBasedTopology()
        )

        // 创建可视化工具
        val visualizer = ActorNetworkVisualizer()

        // 为每种拓扑生成可视化
        topologies.forEachIndexed { index, topology ->
            val topologyName = when (index) {
                0 -> "Hierarchical"
                1 -> "SmallWorld"
                2 -> "RoleBased"
                else -> "Unknown"
            }

            // 生成 HTML 可视化
            val htmlOutputPath = "${topologyName.lowercase()}_network.html"
            visualizer.saveVisualization(
                topology = topology as NetworkTopology,
                outputPath = htmlOutputPath,
                format = "html",
                title = "$topologyName Network Topology"
            )
            println("已生成 $topologyName 网络拓扑的 HTML 可视化: $htmlOutputPath")

            // 生成 DOT 图表
            val dotOutputPath = "${topologyName.lowercase()}_network.dot"
            visualizer.saveVisualization(
                topology = topology as NetworkTopology,
                outputPath = dotOutputPath,
                format = "dot",
                title = "$topologyName Network Topology"
            )
            println("已生成 $topologyName 网络拓扑的 DOT 图表: $dotOutputPath")
        }
    }

    /**
     * 性能仪表盘示例
     */
    private suspend fun performanceDashboardExample(system: ActorSystem) {
        println("\n=== 性能仪表盘示例 ===\n")

        // 创建性能仪表盘
        val dashboard = PerformanceDashboard(system)

        // 注册指标
        dashboard.registerMetric(Metric("messages.processed", MetricType.COUNTER, "Number of messages processed"))
        dashboard.registerMetric(Metric("messages.processing_time", MetricType.TIMER, "Message processing time (ms)"))
        dashboard.registerMetric(Metric("agents.active", MetricType.GAUGE, "Number of active agents"))
        dashboard.registerMetric(Metric("memory.usage", MetricType.GAUGE, "Memory usage (MB)"))
        dashboard.registerMetric(Metric("response.length", MetricType.HISTOGRAM, "Response length distribution"))

        // 模拟收集指标数据
        println("模拟收集指标数据...")
        repeat(50) {
            // 更新计数器
            dashboard.incrementCounter("messages.processed", Random.nextLong(1, 5))

            // 更新计时器
            dashboard.recordTimer("messages.processing_time", Random.nextDouble(10.0, 500.0))

            // 更新仪表
            dashboard.setGauge("agents.active", Random.nextLong(5, 15))
            dashboard.setGauge("memory.usage", Random.nextLong(100, 500))

            // 更新直方图
            dashboard.recordHistogram("response.length", Random.nextDouble(50.0, 2000.0))

            delay(100) // 模拟时间流逝
        }

        // 生成仪表盘
        val outputPath = "performance_dashboard.html"
        dashboard.saveDashboard(outputPath, "Kastrax Actor Performance Dashboard")
        println("已生成性能仪表盘: $outputPath")

        // 打印一些统计信息
        println("\n指标统计信息:")
        println("处理的消息数: ${dashboard.getCounterValue("messages.processed")}")
        println("消息处理时间统计: ${dashboard.getTimerStats("messages.processing_time")}")
        println("响应长度统计: ${dashboard.getHistogramStats("response.length")}")
    }

    /**
     * 协作流程示例
     */
    private suspend fun collaborationFlowExample(system: ActorSystem) {
        println("\n=== 协作流程示例 ===\n")

        // 创建 Agent 网络
        val network = AgentNetwork(system, NetworkTopology())

        // 创建 Agent
        val agents = listOf(
            SimpleAgent("coordinator", "Coordinator"),
            SimpleAgent("researcher", "Researcher"),
            SimpleAgent("writer", "Writer"),
            SimpleAgent("reviewer", "Reviewer")
        )

        // 添加 Agent 到网络
        agents.forEach { network.addAgent(it) }

        // 建立 Agent 之间的关系
        network.connectAgents("coordinator", "researcher", AgentRelation.MASTER)
        network.connectAgents("coordinator", "writer", AgentRelation.MASTER)
        network.connectAgents("coordinator", "reviewer", AgentRelation.MASTER)
        network.connectAgents("researcher", "writer", AgentRelation.PEER)
        network.connectAgents("writer", "reviewer", AgentRelation.PEER)

        // 创建不同类型的协作协议
        val protocols = listOf(
            SequentialProtocol(listOf("researcher", "writer", "reviewer")),
            ParallelProtocol(listOf("researcher", "writer", "reviewer")),
            DebateProtocol(
                debaterIds = listOf("researcher", "writer", "reviewer"),
                moderatorId = "coordinator",
                rounds = 2
            )
        )

        // 创建可视化工具
        val visualizer = CollaborationFlowVisualizer()

        // 为每种协议生成可视化
        protocols.forEachIndexed { index, protocol ->
            val protocolName = when (index) {
                0 -> "Sequential"
                1 -> "Parallel"
                2 -> "Debate"
                else -> "Unknown"
            }

            println("执行 $protocolName 协作协议...")

            // 执行协作任务
            val task = "分析人工智能在教育领域的应用前景"
            val result = network.collaborate(protocol, "coordinator", task)

            // 生成 HTML 可视化
            val htmlOutputPath = "${protocolName.lowercase()}_collaboration.html"
            visualizer.saveVisualization(
                result = result,
                outputPath = htmlOutputPath,
                format = "html",
                title = "$protocolName Collaboration Flow"
            )
            println("已生成 $protocolName 协作流程的 HTML 可视化: $htmlOutputPath")

            // 生成 Mermaid 图表
            val mermaidOutputPath = "${protocolName.lowercase()}_collaboration.mmd"
            visualizer.saveVisualization(
                result = result,
                outputPath = mermaidOutputPath,
                format = "mermaid",
                title = "$protocolName Collaboration Flow"
            )
            println("已生成 $protocolName 协作流程的 Mermaid 图表: $mermaidOutputPath")
        }

        // 关闭网络
        network.shutdown()
    }

    /**
     * 指标收集器示例
     */
    private suspend fun metricsCollectorExample(system: ActorSystem) {
        println("\n=== 指标收集器示例 ===\n")

        // 创建指标收集器
        val collector = MetricsCollector(system)

        // 创建 Agent 网络
        val network = AgentNetwork(system, NetworkTopology())

        // 创建 Agent
        val agents = listOf(
            SimpleAgent("agent1", "Agent 1"),
            SimpleAgent("agent2", "Agent 2"),
            SimpleAgent("agent3", "Agent 3")
        )

        // 添加 Agent 到网络
        agents.forEach { network.addAgent(it) }

        // 建立 Agent 之间的关系
        network.connectAgents("agent1", "agent2", AgentRelation.PEER)
        network.connectAgents("agent2", "agent3", AgentRelation.PEER)
        network.connectAgents("agent3", "agent1", AgentRelation.PEER)

        // 监控 Agent 网络
        collector.monitorAgentNetwork(network)

        // 注册自定义指标聚合器
        collector.registerAggregator("custom.random_value") {
            Random.nextDouble(0.0, 100.0)
        }

        // 开始收集指标
        collector.startCollection(1000) // 每秒收集一次

        // 模拟系统运行
        println("模拟系统运行，收集指标...")
        repeat(10) {
            // 模拟一些活动
            collector.recordMetric("agent.messages.sent", Random.nextDouble(1.0, 10.0))
            collector.recordMetric("agent.messages.received", Random.nextDouble(1.0, 10.0))
            collector.recordMetric("agent.processing.time", Random.nextDouble(10.0, 200.0))

            delay(1000) // 等待一秒
        }

        // 停止收集指标
        collector.stopCollection()

        // 导出指标到 CSV
        val csvOutputPath = "metrics_data.csv"
        collector.exportToCsv(csvOutputPath)
        println("已导出指标数据到 CSV: $csvOutputPath")

        // 生成指标报告
        val reportOutputPath = "metrics_report.html"
        collector.saveReport(reportOutputPath, "Kastrax Actor Metrics Report")
        println("已生成指标报告: $reportOutputPath")

        // 打印一些统计信息
        println("\n指标统计信息:")
        collector.getMetricNames().forEach { name ->
            val stats = collector.getMetricStats(name)
            if (stats.isNotEmpty()) {
                println("$name: min=${stats["min"]?.toInt() ?: 0}, avg=${stats["avg"]?.toInt() ?: 0}, max=${stats["max"]?.toInt() ?: 0}, count=${stats["count"]?.toInt() ?: 0}")
            }
        }

        // 关闭网络
        network.shutdown()
    }

    /**
     * 创建层次拓扑
     */
    private fun createHierarchicalTopology(): HierarchicalTopology {
        val topology = HierarchicalTopology()

        // 构建层次结构
        topology.addRootNode("ceo")
        topology.addChildNode("ceo", "cto")
        topology.addChildNode("ceo", "cfo")
        topology.addChildNode("cto", "dev1")
        topology.addChildNode("cto", "dev2")
        topology.addChildNode("cto", "dev3")
        topology.addChildNode("cfo", "finance1")
        topology.addChildNode("cfo", "finance2")

        return topology
    }

    /**
     * 创建小世界拓扑
     */
    private fun createSmallWorldTopology(): SmallWorldTopology {
        val topology = SmallWorldTopology()

        // 创建小世界网络
        val nodeIds = (1..20).map { "node$it" }
        topology.createSmallWorldNetwork(nodeIds, k = 4, beta = 0.2)

        return topology
    }

    /**
     * 创建基于角色的拓扑
     */
    private fun createRoleBasedTopology(): RoleBasedTopology {
        val topology = RoleBasedTopology()

        // 定义角色
        val managerRole = RoleBasedTopology.Role(
            name = "manager",
            description = "项目管理者",
            capabilities = setOf("planning", "coordination")
        )

        val developerRole = RoleBasedTopology.Role(
            name = "developer",
            description = "开发人员",
            capabilities = setOf("coding", "testing")
        )

        val designerRole = RoleBasedTopology.Role(
            name = "designer",
            description = "设计师",
            capabilities = setOf("ui_design", "ux_design")
        )

        val testerRole = RoleBasedTopology.Role(
            name = "tester",
            description = "测试人员",
            capabilities = setOf("testing", "quality_assurance")
        )

        // 添加角色
        topology.addRole(managerRole)
        topology.addRole(developerRole)
        topology.addRole(designerRole)
        topology.addRole(testerRole)

        // 添加节点
        val nodeIds = listOf(
            "alice", "bob", "charlie", "dave",
            "eve", "frank", "grace", "heidi"
        )

        // 分配角色
        topology.assignRole("alice", "manager")
        topology.assignRole("bob", "developer")
        topology.assignRole("charlie", "developer")
        topology.assignRole("dave", "developer")
        topology.assignRole("eve", "designer")
        topology.assignRole("frank", "designer")
        topology.assignRole("grace", "tester")
        topology.assignRole("heidi", "tester")

        // 创建团队结构
        topology.createTeamStructure(
            leaderRole = "manager",
            memberRoles = listOf("developer", "designer", "tester")
        )

        return topology
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
            // 模拟处理延迟
            delay(Random.nextLong(50, 200))

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
                status = AgentStatus.IDLE,
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
