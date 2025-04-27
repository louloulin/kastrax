package ai.kastrax.core.agent.architecture

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.AgentStatus
import ai.kastrax.core.agent.AgentState
import ai.kastrax.core.agent.SessionInfo
import ai.kastrax.core.agent.SessionMessage
import ai.kastrax.core.agent.version.AgentVersion
import ai.kastrax.core.agent.version.AgentVersionManager
import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.LlmMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging

/**
 * 分层Agent架构
 *
 * 分层Agent允许创建具有层次结构的Agent系统，高层Agent可以委派任务给低层Agent并整合结果。
 * 适用于复杂任务的分解和专业化处理。
 *
 * @param coordinator 协调器Agent，负责决策和任务分配
 * @param subAgents 子Agent映射，键为子Agent名称，值为子Agent实例
 */
class HierarchicalAgent(
    private val coordinator: Agent,
    private val subAgents: Map<String, Agent>
) : KastraXBase(component = "HIERARCHICAL_AGENT", name = coordinator.name), Agent {

    private val agentLogger = KotlinLogging.logger {}
    
    override val versionManager: AgentVersionManager? = coordinator.versionManager

    /**
     * 生成响应
     *
     * 协调器Agent决定如何分解任务并委派给适当的子Agent
     */
    override suspend fun generate(prompt: String, options: AgentGenerateOptions): AgentResponse = coroutineScope {
        agentLogger.debug { "Generating response for prompt: $prompt" }

        // 首先，让协调器决定如何处理这个提示
        val coordinationPrompt = """
            作为一个协调器，你需要决定如何处理以下用户请求。你可以选择自己处理，
            或者将任务委派给以下专业子Agent之一：

            可用的子Agent:
            ${subAgents.entries.map { (name, _) -> 
                "- $name" 
            }.joinToString("\n")}

            用户请求: $prompt

            请以JSON格式回答，包含以下字段：
            {
                "handler": "coordinator" 或子Agent的名称,
                "reason": "选择此处理者的原因",
                "subTasks": [可选] 如果需要多个子Agent协作，列出子任务及其处理者
            }
        """.trimIndent()

        val coordinationResponse = coordinator.generate(coordinationPrompt, options)

        try {
            // 解析协调器的决策
            val decision = parseCoordinationDecision(coordinationResponse.text)
            
            // 根据决策执行相应操作
            when {
                // 如果协调器自己处理
                decision.handler == "coordinator" -> {
                    agentLogger.debug { "Coordinator handling the request directly" }
                    coordinator.generate(prompt, options)
                }
                
                // 如果有子任务，并行执行它们
                decision.subTasks.isNotEmpty() -> {
                    agentLogger.debug { "Executing ${decision.subTasks.size} sub-tasks in parallel" }
                    executeSubTasks(prompt, decision.subTasks, options)
                }
                
                // 如果委派给单个子Agent
                subAgents.containsKey(decision.handler) -> {
                    agentLogger.debug { "Delegating to sub-agent: ${decision.handler}" }
                    val subAgent = subAgents[decision.handler]!!
                    subAgent.generate(prompt, options)
                }
                
                // 如果指定的处理者不存在，回退到协调器
                else -> {
                    agentLogger.warn { "Specified handler '${decision.handler}' not found, falling back to coordinator" }
                    coordinator.generate(prompt, options)
                }
            }
        } catch (e: Exception) {
            // 如果解析或处理过程中出错，回退到协调器
            agentLogger.error(e) { "Error processing coordination decision, falling back to coordinator" }
            coordinator.generate(prompt, options)
        }
    }

    /**
     * 生成响应（多消息版本）
     */
    override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): AgentResponse {
        // 提取最后一条用户消息
        val lastUserMessage = messages.lastOrNull { it.role.name.equals("USER", ignoreCase = true) }?.content
        
        if (lastUserMessage != null) {
            // 如果有用户消息，使用单消息版本的generate方法
            return generate(lastUserMessage, options)
        }
        
        // 如果没有用户消息，直接委派给协调器
        agentLogger.debug { "No user message found, delegating directly to coordinator" }
        return coordinator.generate(messages, options)
    }

    /**
     * 流式生成响应
     */
    override suspend fun stream(prompt: String, options: AgentStreamOptions): AgentResponse {
        // 简单实现：直接使用generate方法，然后返回结果
        // 在实际应用中，应该实现真正的流式处理
        return generate(prompt, options as AgentGenerateOptions)
    }

    /**
     * 重置Agent状态
     */
    override suspend fun reset() {
        // 重置协调器和所有子Agent
        coordinator.reset()
        subAgents.values.forEach { it.reset() }
    }

    /**
     * 获取Agent状态
     */
    override suspend fun getState(): AgentState? {
        // 返回协调器的状态
        return coordinator.getState()
    }

    /**
     * 更新Agent状态
     */
    override suspend fun updateState(status: AgentStatus): AgentState? {
        // 更新协调器的状态
        return coordinator.updateState(status)
    }

    /**
     * 创建会话
     */
    override suspend fun createSession(
        title: String?,
        resourceId: String?,
        metadata: Map<String, String>
    ): SessionInfo? {
        // 使用协调器创建会话
        return coordinator.createSession(title, resourceId, metadata)
    }

    /**
     * 获取会话信息
     */
    override suspend fun getSession(sessionId: String): SessionInfo? {
        // 获取协调器的会话信息
        return coordinator.getSession(sessionId)
    }

    /**
     * 获取会话消息
     */
    override suspend fun getSessionMessages(sessionId: String, limit: Int): List<SessionMessage>? {
        // 获取协调器的会话消息
        return coordinator.getSessionMessages(sessionId, limit)
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
    ): AgentVersion? {
        // 使用协调器的版本管理器创建新版本
        return coordinator.createVersion(instructions, name, description, metadata, activateImmediately)
    }

    /**
     * 获取所有版本
     */
    override suspend fun getVersions(limit: Int, offset: Int): List<AgentVersion>? {
        // 获取协调器的所有版本
        return coordinator.getVersions(limit, offset)
    }

    /**
     * 获取当前激活版本
     */
    override suspend fun getActiveVersion(): AgentVersion? {
        // 获取协调器的当前激活版本
        return coordinator.getActiveVersion()
    }

    /**
     * 激活版本
     */
    override suspend fun activateVersion(versionId: String): AgentVersion? {
        // 激活协调器的指定版本
        return coordinator.activateVersion(versionId)
    }

    /**
     * 回滚到指定版本
     */
    override suspend fun rollbackToVersion(versionId: String): AgentVersion? {
        // 回滚协调器到指定版本
        return coordinator.rollbackToVersion(versionId)
    }

    /**
     * 执行子任务
     */
    private suspend fun executeSubTasks(
        originalPrompt: String,
        subTasks: List<SubTask>,
        options: AgentGenerateOptions
    ): AgentResponse = coroutineScope {
        // 并行执行所有子任务
        val results = subTasks.map { subTask ->
            async {
                val handler = subTask.handler
                val prompt = subTask.prompt ?: originalPrompt
                
                val subAgent = if (handler == "coordinator") {
                    coordinator
                } else {
                    subAgents[handler] ?: coordinator
                }
                
                val response = subAgent.generate(prompt, options)
                SubTaskResult(handler, response.text)
            }
        }.awaitAll()
        
        // 让协调器整合结果
        val integrationPrompt = """
            你需要整合以下子任务的结果，为用户提供一个连贯、全面的回答：
            
            原始问题: $originalPrompt
            
            子任务结果:
            ${results.joinToString("\n\n") { result ->
                "子Agent: ${result.handler}\n结果: ${result.result}"
            }}
            
            请提供一个整合了所有信息的完整回答。
        """.trimIndent()
        
        coordinator.generate(integrationPrompt, options)
    }

    /**
     * 解析协调决策
     */
    private fun parseCoordinationDecision(decisionText: String): CoordinationDecision {
        // 尝试直接解析JSON
        try {
            // 查找JSON部分
            val jsonPattern = """\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}""".toRegex()
            val jsonMatch = jsonPattern.find(decisionText)?.value
            
            if (jsonMatch != null) {
                return Json.decodeFromString<CoordinationDecision>(jsonMatch)
            }
        } catch (e: Exception) {
            agentLogger.warn(e) { "Failed to parse coordination decision JSON: $decisionText" }
        }
        
        // 如果无法解析JSON，使用启发式方法
        val handlerPattern = """"handler"\\s*:\\s*"([^"]+)"""".toRegex()
        val handlerMatch = handlerPattern.find(decisionText)?.groupValues?.get(1)
        
        return CoordinationDecision(
            handler = handlerMatch ?: "coordinator",
            reason = "Fallback due to parsing failure",
            subTasks = emptyList()
        )
    }
}

/**
 * 协调决策
 */
@Serializable
data class CoordinationDecision(
    val handler: String,
    val reason: String,
    val subTasks: List<SubTask> = emptyList()
)

/**
 * 子任务
 */
@Serializable
data class SubTask(
    val handler: String,
    val prompt: String? = null,
    val description: String? = null
)

/**
 * 子任务结果
 */
data class SubTaskResult(
    val handler: String,
    val result: String
)

/**
 * 创建分层Agent的DSL函数
 */
fun hierarchicalAgent(init: HierarchicalAgentBuilder.() -> Unit): HierarchicalAgent {
    val builder = HierarchicalAgentBuilder()
    builder.init()
    return builder.build()
}

/**
 * 分层Agent构建器
 */
class HierarchicalAgentBuilder {
    private var coordinator: Agent? = null
    private val subAgents = mutableMapOf<String, Agent>()
    
    /**
     * 设置协调器Agent
     */
    fun coordinator(agent: Agent) {
        this.coordinator = agent
    }
    
    /**
     * 添加子Agent
     */
    fun addSubAgent(name: String, agent: Agent) {
        subAgents[name] = agent
    }
    
    /**
     * 构建分层Agent
     */
    fun build(): HierarchicalAgent {
        requireNotNull(coordinator) { "Coordinator agent must be set" }
        require(subAgents.isNotEmpty()) { "At least one sub-agent must be added" }
        
        return HierarchicalAgent(
            coordinator = coordinator!!,
            subAgents = subAgents.toMap()
        )
    }
}
