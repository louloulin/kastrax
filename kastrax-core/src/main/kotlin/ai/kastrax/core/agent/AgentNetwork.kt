package ai.kastrax.core.agent

import ai.kastrax.core.agent.routing.ContextAwareRoutingStrategy
import ai.kastrax.core.agent.routing.DefaultRoutingStrategy
import ai.kastrax.core.agent.routing.RoutingStrategy
import ai.kastrax.core.agent.visualization.AgentNetworkVisualizer
import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.tool
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*
import java.time.Instant
import java.util.UUID

/**
 * 代理网络配置
 *
 * @property name 网络名称
 * @property instructions 网络指令
 * @property model LLM提供者
 * @property agents 专业代理列表
 * @property routingStrategy 路由策略
 * @property visualizeInteractions 是否可视化交互
 */
data class AgentNetworkConfig(
    val name: String,
    val instructions: String,
    val model: LlmProvider,
    val agents: List<Agent>,
    val routingStrategy: RoutingStrategy = DefaultRoutingStrategy(),
    val visualizeInteractions: Boolean = false
)

/**
 * 代理网络，用于协调多个专业代理
 */
class AgentNetwork(config: AgentNetworkConfig) : KastraXBase(component = "NETWORK", name = config.name), Agent {
    override val versionManager: ai.kastrax.core.agent.version.AgentVersionManager? = null
    private val instructions = config.instructions
    private val agents = config.agents
    private val model = config.model
    private val routingStrategy = config.routingStrategy
    private val visualizeInteractions = config.visualizeInteractions
    private val routingAgent: LLMAgent
    private val visualizer = if (visualizeInteractions) AgentNetworkVisualizer() else null

    // 代理历史记录
    private val agentHistory: MutableMap<String, MutableList<AgentInteraction>> = mutableMapOf()

    init {
        // 创建路由代理
        routingAgent = LLMAgent(
            name = config.name,
            instructions = routingStrategy.getInstructions(agents, instructions),
            model = config.model,
            tools = getTools()
        )
    }

    /**
     * 格式化代理ID
     */
    private fun formatAgentId(name: String): String {
        return routingStrategy.formatAgentId(name)
    }

    /**
     * 获取工具
     */
    private fun getTools(): Map<String, Tool> {
        val transmitTool = tool {
            id = "transmit"
            name = "Transmit"
            description = "调用一个或多个专业代理来处理特定任务"
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("actions") {
                        put("type", "array")
                        putJsonObject("items") {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("agent") {
                                    put("type", "string")
                                    put("description", "要调用的代理名称")
                                }
                                putJsonObject("input") {
                                    put("type", "string")
                                    put("description", "传递给代理的输入")
                                }
                                putJsonObject("includeHistory") {
                                    put("type", "boolean")
                                    put("description", "是否在上下文中包含之前的代理输出")
                                }
                            }
                            putJsonArray("required") {
                                add("agent")
                                add("input")
                            }
                        }
                    }
                }
                putJsonArray("required") {
                    add("actions")
                }
            }
            execute = { input ->
                val result = try {
                    val actions = input.jsonObject["actions"]?.jsonArray
                    if (actions == null) {
                        buildJsonObject {
                            put("error", "未提供actions")
                        }
                    } else {

                    logger.debug("执行 ${actions.size} 个专业代理")

                    // 并行执行每个代理并收集结果
                    val results = coroutineScope {
                        actions.mapIndexed { index, action ->
                            val agentId = action.jsonObject["agent"]?.jsonPrimitive?.content
                                ?: return@mapIndexed async { "错误：未提供代理名称" }
                            val agentInput = action.jsonObject["input"]?.jsonPrimitive?.content
                                ?: return@mapIndexed async { "错误：未提供输入" }
                            val includeHistory = action.jsonObject["includeHistory"]
                                ?.jsonPrimitive?.booleanOrNull ?: false

                            async {
                                executeAgent(agentId, agentInput, includeHistory)
                            }
                        }.map { it.await() }
                    }

                    logger.debug("结果: $results")

                    // 将结果存储在代理历史记录中以供将来参考
                    actions.forEachIndexed { index, action ->
                        val agentId = action.jsonObject["agent"]?.jsonPrimitive?.content ?: return@forEachIndexed
                        val agentInput = action.jsonObject["input"]?.jsonPrimitive?.content ?: return@forEachIndexed

                        if (index < results.size) {
                            addToAgentHistory(agentId, AgentInteraction(
                                input = agentInput,
                                output = results[index],
                                timestamp = Instant.now().toString()
                            ))
                        }
                    }

                    // 格式化结果，清晰地显示代理名称
                    val formattedResults = actions.mapIndexedNotNull { index, action ->
                        val agentId = action.jsonObject["agent"]?.jsonPrimitive?.content
                            ?: return@mapIndexedNotNull null
                        if (index < results.size) {
                            "[$agentId]: ${results[index]}"
                        } else {
                            null
                        }
                    }.joinToString("\n\n")

                    buildJsonObject {
                        put("result", formattedResults)
                    }
                    }
                } catch (e: Exception) {
                    logger.error("transmit工具中的错误: ${e.message}")
                    buildJsonObject {
                        put("error", "执行代理时出错: ${e.message}")
                    }
                }
                result
            }
        }

        return mapOf("transmit" to transmitTool)
    }

    /**
     * 执行代理
     */
    private suspend fun executeAgent(agentId: String, input: String, includeHistory: Boolean = false): String {
        try {
            // 通过格式化ID查找代理
            val agent = agents.find { formatAgentId(it.name) == agentId }
                ?: throw IllegalArgumentException(
                    "找不到代理\"$agentId\"。可用代理: ${agents.map { formatAgentId(it.name) }.joinToString(", ")}"
                )

            // 准备消息，使用路由策略
            val messages = routingStrategy.prepareAgentContext(agentId, input, includeHistory, agentHistory.toMap())

            // 记录代理执行开始
            logger.debug("开始执行代理 '$agentId'")
            if (visualizeInteractions) {
                visualizer?.recordAgentStart(agentId, input)
            }

            // 从代理生成响应
            val result = agent.generate(messages)

            // 记录代理执行结束
            if (visualizeInteractions) {
                visualizer?.recordAgentEnd(agentId, result.text)
            }

            return result.text
        } catch (e: Exception) {
            logger.error("执行代理\"$agentId\"时出错: ${e.message}")
            if (visualizeInteractions) {
                visualizer?.recordAgentError(agentId, e.message ?: "未知错误")
            }
            return "无法执行代理\"$agentId\": ${e.message}"
        }
    }

    /**
     * 添加到代理历史记录
     */
    private fun addToAgentHistory(agentId: String, interaction: AgentInteraction) {
        if (!agentHistory.containsKey(agentId)) {
            agentHistory[agentId] = mutableListOf()
        }
        agentHistory[agentId]?.add(interaction)
    }

    /**
     * 获取代理历史记录
     */
    fun getAgentHistory(agentId: String): List<AgentInteraction> {
        return agentHistory[agentId] ?: emptyList()
    }

    /**
     * 在运行前清除网络历史记录
     */
    private fun clearNetworkHistoryBeforeRun() {
        agentHistory.clear()
    }

    /**
     * 获取所有代理交互历史
     */
    fun getAgentInteractionHistory(): Map<String, List<AgentInteraction>> {
        return agentHistory.toMap()
    }

    /**
     * 获取代理交互摘要
     */
    fun getAgentInteractionSummary(): String {
        val history = agentHistory
        val agentIds = history.keys

        if (agentIds.isEmpty()) {
            return "尚未发生代理交互。"
        }

        // 收集所有交互及其代理ID
        val allInteractions = mutableListOf<AgentInteractionWithMetadata>()

        // 跟踪交互的全局序列
        var globalSequence = 0

        // 收集所有交互及其源代理
        agentIds.forEach { agentId ->
            val interactions = history[agentId] ?: emptyList()
            interactions.forEachIndexed { index, interaction ->
                allInteractions.add(AgentInteractionWithMetadata(
                    agentId = agentId,
                    interaction = interaction,
                    index = index,
                    sequence = globalSequence++
                ))
            }
        }

        // 按时间戳排序
        allInteractions.sortBy { it.interaction.timestamp }

        // 格式化为可读摘要
        return allInteractions.joinToString("\n\n") { meta ->
            "时间: ${meta.interaction.timestamp}\n" +
            "代理: ${meta.agentId}\n" +
            "输入: ${meta.interaction.input}\n" +
            "输出: ${meta.interaction.output}"
        }
    }

    /**
     * 获取代理交互可视化
     *
     * @return 可视化HTML或null（如果未启用可视化）
     */
    fun getAgentInteractionVisualization(): String? {
        return if (visualizeInteractions && visualizer != null) {
            visualizer.generateVisualization(agentHistory.toMap())
        } else {
            null
        }
    }

    /**
     * 获取路由代理
     */
    fun getRoutingAgent(): Agent {
        return routingAgent
    }

    /**
     * 获取代理列表
     */
    fun getAgents(): List<Agent> {
        return agents
    }

    override suspend fun generate(prompt: String, options: AgentGenerateOptions): AgentResponse {
        clearNetworkHistoryBeforeRun()
        logger.debug("AgentNetwork: 开始生成，有 ${agents.size} 个可用代理")

        val ops = options.copy(
            maxSteps = options.maxSteps.takeIf { it > 1 } ?: (agents.size * 10) // 默认每个代理10个步骤
        )

        // 记录路由过程的开始
        logger.debug("AgentNetwork: 路由，最大步骤数: ${ops.maxSteps}")

        // 使用路由代理生成响应
        val result = routingAgent.generate(prompt, ops)

        // 记录完成
        logger.debug("AgentNetwork: 生成完成")

        return result
    }

    override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): AgentResponse {
        clearNetworkHistoryBeforeRun()
        logger.debug("AgentNetwork: 开始生成，有 ${agents.size} 个可用代理")

        val ops = options.copy(
            maxSteps = options.maxSteps.takeIf { it > 1 } ?: (agents.size * 10) // 默认每个代理10个步骤
        )

        // 记录路由过程的开始
        logger.debug("AgentNetwork: 路由，最大步骤数: ${ops.maxSteps}")

        // 使用路由代理生成响应
        val result = routingAgent.generate(messages, ops)

        // 记录完成
        logger.debug("AgentNetwork: 生成完成")

        return result
    }

    override suspend fun stream(prompt: String, options: AgentStreamOptions): AgentResponse {
        clearNetworkHistoryBeforeRun()
        logger.debug("AgentNetwork: 开始流式生成，有 ${agents.size} 个可用代理")

        val ops = options.copy(
            maxSteps = options.maxSteps.takeIf { it > 1 } ?: (agents.size * 10) // 默认每个代理10个步骤
        )

        // 记录路由过程的开始
        logger.debug("AgentNetwork: 流式路由，最大步骤数: ${ops.maxSteps}")

        // 使用路由代理生成响应
        val result = routingAgent.stream(prompt, ops)

        return result
    }

    /**
     * Stream a response from the agent using a list of messages.
     *
     * @param messages List of messages for the conversation
     * @param options Options for streaming
     * @return Agent response with streaming
     */
    suspend fun stream(messages: List<LlmMessage>, options: AgentStreamOptions): AgentResponse {
        clearNetworkHistoryBeforeRun()
        logger.debug("AgentNetwork: 开始流式生成，有 ${agents.size} 个可用代理")

        val ops = options.copy(
            maxSteps = options.maxSteps.takeIf { it > 1 } ?: (agents.size * 10) // 默认每个代理10个步骤
        )

        // 记录路由过程的开始
        logger.debug("AgentNetwork: 流式路由，最大步骤数: ${ops.maxSteps}")

        // 使用路由代理生成响应
        val result = routingAgent.stream(prompt = messages.last().content, ops)

        return result
    }

    /**
     * Reset the agent's state.
     */
    override suspend fun reset() {
        clearNetworkHistoryBeforeRun()
        routingAgent.reset()
    }

    /**
     * Get the agent's current state.
     */
    override suspend fun getState(): AgentState? {
        return routingAgent.getState()
    }

    /**
     * Update the agent's state.
     */
    override suspend fun updateState(status: AgentStatus): AgentState? {
        return routingAgent.updateState(status)
    }

    /**
     * Create a new session.
     */
    override suspend fun createSession(
        title: String?,
        resourceId: String?,
        metadata: Map<String, String>
    ): SessionInfo? {
        return routingAgent.createSession(title, resourceId, metadata)
    }

    /**
     * Get session information.
     */
    override suspend fun getSession(sessionId: String): SessionInfo? {
        return routingAgent.getSession(sessionId)
    }

    /**
     * Get session messages.
     */
    override suspend fun getSessionMessages(sessionId: String, limit: Int): List<SessionMessage>? {
        return routingAgent.getSessionMessages(sessionId, limit)
    }

    /**
     * 创建新版本
     */
    override suspend fun createVersion(
        instructions: String,
        name: String?,
        description: String?,
        metadata: Map<String, String>,
        activateImmediately: Boolean
    ): ai.kastrax.core.agent.version.AgentVersion? {
        logger.warn { "Agent网络不支持版本控制" }
        return null
    }

    /**
     * 获取所有版本
     */
    override suspend fun getVersions(limit: Int, offset: Int): List<ai.kastrax.core.agent.version.AgentVersion>? {
        logger.warn { "Agent网络不支持版本控制" }
        return null
    }

    /**
     * 获取当前激活版本
     */
    override suspend fun getActiveVersion(): ai.kastrax.core.agent.version.AgentVersion? {
        logger.warn { "Agent网络不支持版本控制" }
        return null
    }

    /**
     * 激活版本
     */
    override suspend fun activateVersion(versionId: String): ai.kastrax.core.agent.version.AgentVersion? {
        logger.warn { "Agent网络不支持版本控制" }
        return null
    }

    /**
     * 回滚到指定版本
     */
    override suspend fun rollbackToVersion(versionId: String): ai.kastrax.core.agent.version.AgentVersion? {
        logger.warn { "Agent网络不支持版本控制" }
        return null
    }
}

/**
 * 代理交互
 */
data class AgentInteraction(
    val input: String,
    val output: String,
    val timestamp: String = Instant.now().toString()
)

/**
 * 带元数据的代理交互
 */
private data class AgentInteractionWithMetadata(
    val agentId: String,
    val interaction: AgentInteraction,
    val index: Int,
    val sequence: Int
)

/**
 * 创建代理网络的DSL函数
 */
fun agentNetwork(init: AgentNetworkBuilder.() -> Unit): AgentNetwork {
    val builder = AgentNetworkBuilder()
    builder.init()
    return builder.build()
}

/**
 * 代理网络构建器
 */
class AgentNetworkBuilder {
    var name: String = ""
    var instructions: String = ""
    lateinit var model: LlmProvider
    val agents: MutableList<Agent> = mutableListOf()
    var routingStrategy: RoutingStrategy = DefaultRoutingStrategy()
    var visualizeInteractions: Boolean = false

    /**
     * 添加代理
     */
    fun agent(agent: Agent) {
        agents.add(agent)
    }

    /**
     * 设置路由策略
     */
    fun routingStrategy(strategy: RoutingStrategy) {
        routingStrategy = strategy
    }

    /**
     * 使用上下文感知路由策略
     */
    fun useContextAwareRouting() {
        routingStrategy = ContextAwareRoutingStrategy()
    }

    /**
     * 启用交互可视化
     */
    fun enableVisualization() {
        visualizeInteractions = true
    }

    /**
     * 构建代理网络
     */
    fun build(): AgentNetwork {
        require(name.isNotEmpty()) { "代理网络名称不能为空" }
        require(instructions.isNotEmpty()) { "代理网络指令不能为空" }
        require(::model.isInitialized) { "代理网络模型必须定义" }
        require(agents.isNotEmpty()) { "代理网络必须至少有一个代理" }

        return AgentNetwork(
            AgentNetworkConfig(
                name = name,
                instructions = instructions,
                model = model,
                agents = agents,
                routingStrategy = routingStrategy,
                visualizeInteractions = visualizeInteractions
            )
        )
    }
}
