package ai.kastrax.core.agent.autonomy

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentStreamOptions
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
