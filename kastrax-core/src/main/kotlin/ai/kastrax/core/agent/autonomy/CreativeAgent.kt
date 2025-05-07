package ai.kastrax.core.agent.autonomy

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.AgentStreamResponse
import ai.kastrax.core.agent.SessionInfo
import ai.kastrax.core.agent.SessionMessage
import ai.kastrax.core.agent.AgentStatus
import ai.kastrax.core.agent.AgentState
import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.ToolCallResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonElement

/**
 * 创造性Agent，扩展Agent接口，添加创造性和自主性功能
 */
class CreativeAgent(
    private val baseAgent: Agent,
    autonomyConfig: AutonomyConfig = AutonomyConfig()
) : KastraXBase(component = "CREATIVE_AGENT", name = baseAgent.name), Agent {

    // 自主性管理器
    private val autonomy = AgentAutonomy(baseAgent, autonomyConfig)

    // 版本管理器
    override val versionManager = baseAgent.versionManager

    /**
     * 生成响应
     */
    override suspend fun generate(prompt: String, options: AgentGenerateOptions): AgentResponse {
        // 使用自主性管理器生成响应
        return autonomy.generateAutonomousResponse(prompt, options)
    }

    /**
     * 生成响应（多消息）
     */
    override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): AgentResponse {
        // 将消息转换为单个提示
        val lastMessage = messages.lastOrNull { it.role == LlmMessageRole.USER }?.content
            ?: return baseAgent.generate(messages, options)

        // 使用自主性管理器生成响应
        val autonomousResponse = autonomy.generateAutonomousResponse(lastMessage, options)

        // 创建新的消息列表，替换最后一个用户消息的响应
        val newMessages = messages.toMutableList()
        val assistantMessage = LlmMessage(
            role = LlmMessageRole.ASSISTANT,
            content = autonomousResponse.text,
            toolCalls = autonomousResponse.toolCalls
        )

        // 如果最后一条消息是用户消息，添加助手消息；否则替换最后一条助手消息
        if (newMessages.lastOrNull()?.role == LlmMessageRole.USER) {
            newMessages.add(assistantMessage)
        } else {
            val lastAssistantIndex = newMessages.indexOfLast { it.role == LlmMessageRole.ASSISTANT }
            if (lastAssistantIndex >= 0) {
                newMessages[lastAssistantIndex] = assistantMessage
            } else {
                newMessages.add(assistantMessage)
            }
        }

        // 使用基础Agent生成最终响应
        return baseAgent.generate(newMessages, options)
    }

    /**
     * 流式生成响应
     */
    override suspend fun stream(prompt: String, options: AgentStreamOptions): AgentResponse {
        // 使用自主性管理器生成响应
        val autonomousResponse = autonomy.generateAutonomousResponse(prompt, AgentGenerateOptions())

        // 使用基础Agent流式生成响应
        return baseAgent.stream(autonomousResponse.text, options)
    }

    /**
     * 流式生成响应，返回流
     */
    override suspend fun generateStream(prompt: String, options: AgentStreamOptions): Flow<AgentStreamResponse> {
        // 使用自主性管理器生成响应
        val enhancedPrompt = autonomy.enhancePromptWithAutonomy(prompt)

        // 使用基础Agent流式生成响应
        return baseAgent.generateStream(enhancedPrompt, options)
    }



    /**
     * 重置Agent状态
     */
    override suspend fun reset() {
        baseAgent.reset()
    }

    /**
     * 获取Agent状态
     */
    override suspend fun getState(): AgentState? {
        return baseAgent.getState()
    }

    /**
     * 更新Agent状态
     */
    override suspend fun updateState(status: AgentStatus): AgentState? {
        return baseAgent.updateState(status)
    }

    /**
     * 创建会话
     */
    override suspend fun createSession(
        title: String?,
        resourceId: String?,
        metadata: Map<String, String>
    ): SessionInfo? {
        return baseAgent.createSession(title, resourceId, metadata)
    }

    /**
     * 获取会话信息
     */
    override suspend fun getSession(sessionId: String): SessionInfo? {
        return baseAgent.getSession(sessionId)
    }

    /**
     * 获取会话消息
     */
    override suspend fun getSessionMessages(sessionId: String, limit: Int): List<SessionMessage>? {
        return baseAgent.getSessionMessages(sessionId, limit)
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
        return baseAgent.createVersion(instructions, name, description, metadata, activateImmediately)
    }

    /**
     * 获取所有版本
     */
    override suspend fun getVersions(
        limit: Int,
        offset: Int
    ): List<ai.kastrax.core.agent.version.AgentVersion>? {
        return baseAgent.getVersions(limit, offset)
    }

    /**
     * 获取当前激活版本
     */
    override suspend fun getActiveVersion(): ai.kastrax.core.agent.version.AgentVersion? {
        return baseAgent.getActiveVersion()
    }

    /**
     * 激活版本
     */
    override suspend fun activateVersion(versionId: String): ai.kastrax.core.agent.version.AgentVersion? {
        return baseAgent.activateVersion(versionId)
    }

    /**
     * 回滚到指定版本
     */
    override suspend fun rollbackToVersion(versionId: String): ai.kastrax.core.agent.version.AgentVersion? {
        return baseAgent.rollbackToVersion(versionId)
    }

    /**
     * 获取自主性管理器
     */
    fun getAutonomy(): AgentAutonomy {
        return autonomy
    }

    /**
     * 生成创意内容
     */
    suspend fun generateCreativeContent(
        prompt: String,
        type: CreativityType = CreativityType.EXPLORATORY,
        options: AgentGenerateOptions = AgentGenerateOptions()
    ): CreativeContent {
        return autonomy.generateCreativeContent(prompt, type, options)
    }

    /**
     * 流式生成创意内容
     */
    suspend fun generateCreativeContentStream(
        prompt: String,
        type: CreativityType = CreativityType.EXPLORATORY,
        options: AgentStreamOptions = AgentStreamOptions()
    ): Flow<AgentStreamResponse> {
        // 使用自主性管理器生成响应
        val enhancedPrompt = when (type) {
            CreativityType.COMBINATIONAL -> {
                """
                请基于以下提示，创造一个结合多种现有概念的新想法：

                $prompt

                尝试将不同领域的概念结合起来，创造出新颖且有价值的内容。
                """.trimIndent()
            }
            CreativityType.EXPLORATORY -> {
                """
                请基于以下提示，探索现有概念空间的边界，创造出新颖的内容：

                $prompt

                在现有规则和约束内，尽可能推动创意的边界。
                """.trimIndent()
            }
            CreativityType.TRANSFORMATIONAL -> {
                """
                请基于以下提示，创造一个彻底改变现有概念空间的革命性想法：

                $prompt

                挑战基本假设，改变思考问题的方式，创造出真正突破性的内容。
                """.trimIndent()
            }
        }

        // 设置创造力温度
        val creativeOptions = options.copy(
            temperature = options.temperature.coerceAtLeast(0.7) + autonomy.getConfig().creativityLevel * 0.3
        )

        // 使用基础Agent流式生成响应
        return baseAgent.generateStream(enhancedPrompt, creativeOptions)
    }

    /**
     * 自主探索
     */
    suspend fun exploreAutonomously(
        topic: String,
        depth: Int = 1,
        options: AgentGenerateOptions = AgentGenerateOptions()
    ): List<String> {
        return autonomy.exploreAutonomously(topic, depth, options)
    }

    /**
     * 流式自主探索
     */
    suspend fun exploreAutonomouslyStream(
        topic: String,
        depth: Int = 1,
        options: AgentStreamOptions = AgentStreamOptions()
    ): Flow<AgentStreamResponse> {
        // 初始探索提示
        val initialPrompt = """
            请探索以下主题，提出3-5个值得进一步研究的子主题或问题：

            $topic

            对于每个子主题，简要解释为什么它值得探索。
        """.trimIndent()

        // 设置探索温度
        val explorationOptions = options.copy(
            temperature = options.temperature.coerceAtLeast(0.6) + autonomy.getConfig().explorationRate * 0.3
        )

        // 流式生成初始探索
        return baseAgent.generateStream(initialPrompt, explorationOptions)
    }

    /**
     * 设置自主目标
     */
    suspend fun setGoal(description: String, priority: Int = 1): AutonomousGoal {
        return autonomy.setAutonomousGoal(description, priority)
    }

    /**
     * 更新目标状态
     */
    fun updateGoalStatus(goalId: String, status: GoalStatus): AutonomousGoal? {
        return autonomy.updateGoalStatus(goalId, status)
    }

    /**
     * 获取自主目标
     */
    fun getGoals(includeCompleted: Boolean = false): List<AutonomousGoal> {
        return autonomy.getGoals(includeCompleted)
    }

    /**
     * 获取自主行为历史
     */
    fun getBehaviorHistory(): List<AutonomousBehavior> {
        return autonomy.getBehaviorHistory()
    }

    /**
     * 获取创意内容历史
     */
    fun getCreativeHistory(): List<CreativeContent> {
        return autonomy.getCreativeHistory()
    }

    /**
     * 获取学习的知识
     */
    fun getLearnedKnowledge(): Map<String, String> {
        return autonomy.getLearnedKnowledge()
    }
}
