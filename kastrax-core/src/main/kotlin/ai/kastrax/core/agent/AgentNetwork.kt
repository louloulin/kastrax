package ai.kastrax.core.agent

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
 */
data class AgentNetworkConfig(
    val name: String,
    val instructions: String,
    val model: LlmProvider,
    val agents: List<Agent>
)

/**
 * 代理网络，用于协调多个专业代理
 */
class AgentNetwork(config: AgentNetworkConfig) : KastraXBase(component = "NETWORK", name = config.name), Agent {
    private val instructions = config.instructions
    private val agents = config.agents
    private val model = config.model
    private val routingAgent: LLMAgent

    // 代理历史记录
    private val agentHistory: MutableMap<String, MutableList<AgentInteraction>> = mutableMapOf()

    init {
        // 创建路由代理
        routingAgent = LLMAgent(
            name = config.name,
            instructions = getInstructions(),
            model = config.model,
            tools = getTools()
        )
    }

    /**
     * 格式化代理ID
     */
    private fun formatAgentId(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }

    /**
     * 获取路由指令
     */
    private fun getInstructions(): String {
        // 创建可用代理列表
        val agentList = agents.joinToString("\n") { agent ->
            val id = formatAgentId(agent.name)
            " - **$id**: ${agent.name}"
        }

        return """
            你是一个专业代理网络中的路由器。
            你的工作是决定哪个代理应该处理任务的每个步骤。

            ## 系统指令
            $instructions

            ## 可用的专业代理
            你可以使用"transmit"工具调用这些代理：
            $agentList

            ## 如何使用"transmit"工具

            "transmit"工具允许你调用一个或多个专业代理。

            ### 单个代理调用
            要调用单个代理，使用以下格式：
            ```json
            {
              "actions": [
                {
                  "agent": "agent_name",
                  "input": "给代理的详细指令",
                  "includeHistory": true
                }
              ]
            }
            ```

            ### 多个代理调用
            要调用多个代理，使用以下格式：
            ```json
            {
              "actions": [
                {
                  "agent": "agent_name_1",
                  "input": "给代理1的详细指令",
                  "includeHistory": true
                },
                {
                  "agent": "agent_name_2",
                  "input": "给代理2的详细指令",
                  "includeHistory": false
                }
              ]
            }
            ```

            ## 最佳实践
            1. 将复杂任务分解为更小的步骤
            2. 为每个步骤选择最合适的代理
            3. 为每个代理提供清晰、详细的指令
            4. 在需要时综合多个代理的结果
            5. 为用户提供最终摘要或答案

            ## 工作流程
            1. 分析用户的请求
            2. 确定哪些专业代理可以提供帮助
            3. 使用transmit工具调用适当的代理
            4. 审查代理的响应
            5. 调用更多代理或提供最终答案
        """.trimIndent()
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

            // 准备消息
            val userMessage = LlmMessage(role = LlmMessageRole.USER, content = input)
            val messages = mutableListOf<LlmMessage>()

            // 如果请求，包含相关历史
            if (includeHistory) {
                val history = getAgentHistory(agentId)
                if (history.isNotEmpty()) {
                    messages.add(LlmMessage(
                        role = LlmMessageRole.SYSTEM,
                        content = "以下是之前与你的交互历史，可能对当前任务有帮助:\n" +
                                history.joinToString("\n\n") { "用户: ${it.input}\n你: ${it.output}" }
                    ))
                }
            }

            messages.add(userMessage)

            // 从代理生成响应
            val result = agent.generate(messages)

            return result.text
        } catch (e: Exception) {
            logger.error("执行代理\"$agentId\"时出错: ${e.message}")
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

    /**
     * 添加代理
     */
    fun agent(agent: Agent) {
        agents.add(agent)
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
                agents = agents
            )
        )
    }
}
