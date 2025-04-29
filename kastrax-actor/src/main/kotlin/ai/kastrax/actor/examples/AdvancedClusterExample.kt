package ai.kastrax.actor.examples

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.cluster.Kind
import actor.proto.fromProducer
import actor.proto.requestAwait
import ai.kastrax.actor.AgentRequest
import ai.kastrax.actor.AgentResponse
import ai.kastrax.actor.KastraxActor
import ai.kastrax.actor.cluster.ClusterConfig
import ai.kastrax.actor.cluster.configureCluster
import ai.kastrax.actor.cluster.getCluster
import ai.kastrax.actor.cluster.getClusterMembers
import ai.kastrax.actor.cluster.joinCluster
import ai.kastrax.actor.cluster.leaveCluster
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 高级集群示例
 * 
 * 演示更复杂的集群功能，包括：
 * - 多种类型的 Agent
 * - 负载均衡
 * - 故障检测和恢复
 * - 集群状态监控
 */
object AdvancedClusterExample {
    
    // 请求计数器
    private val requestCounter = AtomicInteger(0)
    
    // 节点状态
    private val nodeStatus = ConcurrentHashMap<String, String>()
    
    /**
     * 启动集群管理节点
     */
    fun startManagerNode() {
        // 配置集群
        val config = ClusterConfig(
            hostname = "localhost",
            port = 8090,
            clusterName = "advanced-cluster",
            seeds = listOf("localhost:8090")
        )
        
        // 创建系统
        val system = configureCluster("manager-node", config)
        
        // 获取集群实例
        val cluster = system.getCluster(config)
        
        // 创建并注册管理 Agent
        val managerAgent = ManagerAgent("manager-agent")
        val managerProps = fromProducer { KastraxActor(managerAgent) }
        val managerKind = Kind("manager", managerProps)
        cluster.registerKind(managerKind)
        
        // 创建并注册监控 Agent
        val monitorAgent = MonitorAgent("monitor-agent")
        val monitorProps = fromProducer { KastraxActor(monitorAgent) }
        val monitorKind = Kind("monitor", monitorProps)
        cluster.registerKind(monitorKind)
        
        // 加入集群
        system.joinCluster()
        
        println("管理节点已启动，监听端口: ${config.port}")
        println("集群名称: ${config.clusterName}")
        
        // 保持系统运行
        runBlocking {
            while (true) {
                // 获取集群成员
                val members = system.getClusterMembers()
                println("当前集群成员: $members")
                
                // 更新节点状态
                members.forEach { member ->
                    nodeStatus[member] = "ALIVE"
                }
                
                // 打印节点状态
                println("节点状态:")
                nodeStatus.forEach { (node, status) ->
                    println("  - $node: $status")
                }
                
                delay(5000)
            }
        }
    }
    
    /**
     * 启动工作节点
     */
    fun startWorkerNode(port: Int, name: String, agentType: String) {
        // 配置集群
        val config = ClusterConfig(
            hostname = "localhost",
            port = port,
            clusterName = "advanced-cluster",
            seeds = listOf("localhost:8090")
        )
        
        // 创建系统
        val system = configureCluster(name, config)
        
        // 获取集群实例
        val cluster = system.getCluster(config)
        
        // 创建并注册 Agent
        val agent = when (agentType) {
            "processor" -> ProcessorAgent("$name-processor")
            "analyzer" -> AnalyzerAgent("$name-analyzer")
            else -> WorkerAgent("$name-worker")
        }
        
        val props = fromProducer { KastraxActor(agent) }
        val kind = Kind(agentType, props)
        cluster.registerKind(kind)
        
        // 加入集群
        system.joinCluster()
        
        println("工作节点已启动，类型: $agentType，监听端口: ${config.port}")
        println("集群名称: ${config.clusterName}")
        
        // 保持系统运行
        runBlocking {
            while (true) {
                // 获取集群成员
                val members = system.getClusterMembers()
                println("当前集群成员: $members")
                
                // 模拟工作负载
                val workload = requestCounter.get()
                println("当前工作负载: $workload 请求")
                
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
            clusterName = "advanced-cluster",
            seeds = listOf("localhost:8090")
        )
        
        // 创建系统
        val system = configureCluster("client-node", config)
        
        // 获取集群实例
        val cluster = system.getCluster(config)
        
        // 加入集群
        system.joinCluster()
        
        println("客户端已连接到集群")
        
        // 获取集群成员
        val members = system.getClusterMembers()
        println("当前集群成员: $members")
        
        // 模拟客户端请求
        while (true) {
            try {
                // 增加请求计数
                val requestId = requestCounter.incrementAndGet()
                
                // 根据请求 ID 选择不同的 Agent 类型
                val agentType = when (requestId % 3) {
                    0 -> "processor"
                    1 -> "analyzer"
                    else -> "worker"
                }
                
                // 获取虚拟 Actor
                val actorId = "client-request-$requestId"
                val pid = cluster.get(actorId, agentType)
                
                println("发送请求 #$requestId 到 $agentType")
                
                // 发送请求
                val response = system.root.requestAwait<AgentResponse>(
                    pid,
                    AgentRequest("处理请求 #$requestId"),
                    Duration.ofSeconds(5)
                )
                
                println("收到响应: ${response.text}")
                
                // 等待一段时间再发送下一个请求
                delay(1000)
            } catch (e: Exception) {
                println("请求失败: ${e.message}")
                delay(5000)
            }
        }
    }
    
    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty() || args[0] == "manager") {
            startManagerNode()
        } else if (args[0] == "worker") {
            val port = if (args.size > 1) args[1].toInt() else 8091
            val name = if (args.size > 2) args[2] else "worker-node"
            val agentType = if (args.size > 3) args[3] else "worker"
            startWorkerNode(port, name, agentType)
        } else if (args[0] == "client") {
            startClient()
        } else {
            println("用法: AdvancedClusterExample [manager|worker|client]")
            println("  manager: 启动管理节点")
            println("  worker [port] [name] [type]: 启动工作节点")
            println("  client: 启动客户端")
        }
    }
    
    /**
     * 管理 Agent 实现
     */
    private class ManagerAgent(override val name: String) : Agent {
        override val versionManager: AgentVersionManager? = null
        
        override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): CoreAgentResponse {
            val lastMessage = messages.lastOrNull { it.role.name.equals("USER", ignoreCase = true) }?.content ?: ""
            return generate(lastMessage, options)
        }
        
        override suspend fun generate(prompt: String, options: AgentGenerateOptions): CoreAgentResponse {
            return CoreAgentResponse(
                text = "[$name] 管理响应: $prompt",
                toolCalls = emptyList()
            )
        }
        
        override suspend fun stream(prompt: String, options: AgentStreamOptions): CoreAgentResponse {
            return CoreAgentResponse(
                text = "[$name] 管理流式响应: $prompt",
                toolCalls = emptyList(),
                textStream = flowOf("[$name]", " 管理", "流式", "响应", ": ", prompt)
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
     * 监控 Agent 实现
     */
    private class MonitorAgent(override val name: String) : Agent {
        override val versionManager: AgentVersionManager? = null
        
        override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): CoreAgentResponse {
            val lastMessage = messages.lastOrNull { it.role.name.equals("USER", ignoreCase = true) }?.content ?: ""
            return generate(lastMessage, options)
        }
        
        override suspend fun generate(prompt: String, options: AgentGenerateOptions): CoreAgentResponse {
            return CoreAgentResponse(
                text = "[$name] 监控响应: $prompt",
                toolCalls = emptyList()
            )
        }
        
        override suspend fun stream(prompt: String, options: AgentStreamOptions): CoreAgentResponse {
            return CoreAgentResponse(
                text = "[$name] 监控流式响应: $prompt",
                toolCalls = emptyList(),
                textStream = flowOf("[$name]", " 监控", "流式", "响应", ": ", prompt)
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
     * 工作 Agent 实现
     */
    private class WorkerAgent(override val name: String) : Agent {
        override val versionManager: AgentVersionManager? = null
        
        override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): CoreAgentResponse {
            val lastMessage = messages.lastOrNull { it.role.name.equals("USER", ignoreCase = true) }?.content ?: ""
            return generate(lastMessage, options)
        }
        
        override suspend fun generate(prompt: String, options: AgentGenerateOptions): CoreAgentResponse {
            return CoreAgentResponse(
                text = "[$name] 工作响应: $prompt",
                toolCalls = emptyList()
            )
        }
        
        override suspend fun stream(prompt: String, options: AgentStreamOptions): CoreAgentResponse {
            return CoreAgentResponse(
                text = "[$name] 工作流式响应: $prompt",
                toolCalls = emptyList(),
                textStream = flowOf("[$name]", " 工作", "流式", "响应", ": ", prompt)
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
     * 处理器 Agent 实现
     */
    private class ProcessorAgent(override val name: String) : Agent {
        override val versionManager: AgentVersionManager? = null
        
        override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): CoreAgentResponse {
            val lastMessage = messages.lastOrNull { it.role.name.equals("USER", ignoreCase = true) }?.content ?: ""
            return generate(lastMessage, options)
        }
        
        override suspend fun generate(prompt: String, options: AgentGenerateOptions): CoreAgentResponse {
            return CoreAgentResponse(
                text = "[$name] 处理响应: $prompt",
                toolCalls = emptyList()
            )
        }
        
        override suspend fun stream(prompt: String, options: AgentStreamOptions): CoreAgentResponse {
            return CoreAgentResponse(
                text = "[$name] 处理流式响应: $prompt",
                toolCalls = emptyList(),
                textStream = flowOf("[$name]", " 处理", "流式", "响应", ": ", prompt)
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
     * 分析器 Agent 实现
     */
    private class AnalyzerAgent(override val name: String) : Agent {
        override val versionManager: AgentVersionManager? = null
        
        override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): CoreAgentResponse {
            val lastMessage = messages.lastOrNull { it.role.name.equals("USER", ignoreCase = true) }?.content ?: ""
            return generate(lastMessage, options)
        }
        
        override suspend fun generate(prompt: String, options: AgentGenerateOptions): CoreAgentResponse {
            return CoreAgentResponse(
                text = "[$name] 分析响应: $prompt",
                toolCalls = emptyList()
            )
        }
        
        override suspend fun stream(prompt: String, options: AgentStreamOptions): CoreAgentResponse {
            return CoreAgentResponse(
                text = "[$name] 分析流式响应: $prompt",
                toolCalls = emptyList(),
                textStream = flowOf("[$name]", " 分析", "流式", "响应", ": ", prompt)
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
}
