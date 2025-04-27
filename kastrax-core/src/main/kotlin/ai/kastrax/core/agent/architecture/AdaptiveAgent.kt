package ai.kastrax.core.agent.architecture

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.SessionInfo
import ai.kastrax.core.agent.SessionMessage
import ai.kastrax.core.agent.AgentStatus
import ai.kastrax.core.agent.AgentState
import ai.kastrax.core.agent.version.AgentVersion
import ai.kastrax.core.agent.version.AgentVersionManager
import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 自适应Agent架构
 *
 * 自适应Agent能够根据用户交互和反馈动态调整其行为和响应方式。
 * 它通过学习用户偏好、分析交互历史和接收显式反馈来不断改进其性能。
 */
class AdaptiveAgent(
    private val baseAgent: Agent,
    private val config: AdaptiveAgentConfig = AdaptiveAgentConfig()
) : KastraXBase(component = "ADAPTIVE_AGENT", name = baseAgent.name), Agent {

    private val agentLogger = KotlinLogging.logger {}

    override val versionManager: AgentVersionManager? = baseAgent.versionManager

    // 用户偏好存储
    private val userPreferences = ConcurrentHashMap<String, UserPreference>()

    // 交互历史
    private val interactionHistory = mutableListOf<Interaction>()

    // 适应策略
    private val adaptationStrategies = mutableListOf<AdaptationStrategy>().apply {
        add(UserFeedbackStrategy())
        add(InteractionPatternStrategy())
        add(PerformanceMetricStrategy())
    }

    /**
     * 生成响应
     */
    override suspend fun generate(prompt: String, options: AgentGenerateOptions): AgentResponse {
        // 记录交互开始
        val interactionId = UUID.randomUUID().toString()
        val startTime = Clock.System.now()

        // 应用适应策略增强提示
        val enhancedPrompt = applyAdaptationStrategies(prompt, options)

        // 使用基础Agent生成响应
        val response = baseAgent.generate(enhancedPrompt, options)

        // 记录交互
        val endTime = Clock.System.now()
        val interaction = Interaction(
            id = interactionId,
            prompt = prompt,
            response = response.text,
            startTime = startTime,
            endTime = endTime,
            userId = options.metadata?.get("userId"),
            sessionId = options.metadata?.get("sessionId")
        )
        recordInteraction(interaction)

        // 如果配置了自动学习，异步分析交互
        if (config.enableAutoLearning) {
            coroutineScope {
                async {
                    learnFromInteraction(interaction)
                }
            }
        }

        return response
    }

    /**
     * 生成响应（多消息版本）
     */
    override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): AgentResponse {
        // 提取最后一条用户消息
        val lastUserMessage = messages.lastOrNull { it.role == LlmMessageRole.USER }?.content

        if (lastUserMessage != null) {
            // 记录交互开始
            val interactionId = UUID.randomUUID().toString()
            val startTime = Clock.System.now()

            // 应用适应策略增强消息
            val enhancedMessages = applyAdaptationStrategies(messages, options)

            // 使用基础Agent生成响应
            val response = baseAgent.generate(enhancedMessages, options)

            // 记录交互
            val endTime = Clock.System.now()
            val interaction = Interaction(
                id = interactionId,
                prompt = lastUserMessage,
                response = response.text,
                startTime = startTime,
                endTime = endTime,
                userId = options.metadata?.get("userId"),
                sessionId = options.metadata?.get("sessionId")
            )
            recordInteraction(interaction)

            // 如果配置了自动学习，异步分析交互
            if (config.enableAutoLearning) {
                coroutineScope {
                    async {
                        learnFromInteraction(interaction)
                    }
                }
            }

            return response
        }

        // 如果没有用户消息，直接使用基础Agent处理
        return baseAgent.generate(messages, options)
    }

    /**
     * 流式生成响应
     */
    override suspend fun stream(prompt: String, options: AgentStreamOptions): AgentResponse {
        // 应用适应策略增强提示
        val enhancedPrompt = applyAdaptationStrategies(prompt, options as AgentGenerateOptions)

        // 使用基础Agent流式生成响应
        return baseAgent.stream(enhancedPrompt, options)
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
    ): AgentVersion? {
        return baseAgent.createVersion(instructions, name, description, metadata, activateImmediately)
    }

    /**
     * 获取所有版本
     */
    override suspend fun getVersions(limit: Int, offset: Int): List<AgentVersion>? {
        return baseAgent.getVersions(limit, offset)
    }

    /**
     * 获取当前激活版本
     */
    override suspend fun getActiveVersion(): AgentVersion? {
        return baseAgent.getActiveVersion()
    }

    /**
     * 激活版本
     */
    override suspend fun activateVersion(versionId: String): AgentVersion? {
        return baseAgent.activateVersion(versionId)
    }

    /**
     * 回滚到指定版本
     */
    override suspend fun rollbackToVersion(versionId: String): AgentVersion? {
        return baseAgent.rollbackToVersion(versionId)
    }

    /**
     * 提供用户反馈
     *
     * @param interactionId 交互ID
     * @param rating 评分（1-5）
     * @param feedback 反馈内容
     * @param userId 用户ID
     */
    suspend fun provideFeedback(
        interactionId: String,
        rating: Int,
        feedback: String? = null,
        userId: String? = null
    ) {
        // 查找交互
        val interaction = interactionHistory.find { it.id == interactionId }

        if (interaction != null) {
            // 更新交互的反馈
            interaction.feedback = Feedback(
                rating = rating,
                comment = feedback,
                timestamp = Clock.System.now()
            )

            // 如果提供了用户ID，更新用户偏好
            if (userId != null) {
                updateUserPreference(userId, interaction)
            }

            // 如果评分较低，触发学习过程
            if (rating <= 3 && config.enableAutoLearning) {
                learnFromFeedback(interaction)
            }

            agentLogger.debug { "Received feedback for interaction $interactionId: rating=$rating" }
        } else {
            agentLogger.warn { "Interaction $interactionId not found" }
        }
    }

    /**
     * 设置用户偏好
     *
     * @param userId 用户ID
     * @param preference 偏好设置
     */
    fun setUserPreference(userId: String, preference: UserPreference) {
        userPreferences[userId] = preference
        agentLogger.debug { "Set user preference for $userId: $preference" }
    }

    /**
     * 获取用户偏好
     *
     * @param userId 用户ID
     * @return 用户偏好，如果不存在则返回null
     */
    fun getUserPreference(userId: String): UserPreference? {
        return userPreferences[userId]
    }

    /**
     * 获取交互历史
     *
     * @param limit 限制数量
     * @param userId 用户ID（可选）
     * @param includeCompleted 是否包含已完成的交互
     * @return 交互历史列表
     */
    fun getInteractionHistory(limit: Int = 100, userId: String? = null, includeCompleted: Boolean = true): List<Interaction> {
        return interactionHistory
            .filter { userId == null || it.userId == userId }
            .takeLast(limit)
    }

    /**
     * 添加适应策略
     *
     * @param strategy 适应策略
     */
    fun addAdaptationStrategy(strategy: AdaptationStrategy) {
        adaptationStrategies.add(strategy)
        agentLogger.debug { "Added adaptation strategy: ${strategy.javaClass.simpleName}" }
    }

    /**
     * 应用适应策略增强提示
     */
    private fun applyAdaptationStrategies(prompt: String, options: AgentGenerateOptions): String {
        var enhancedPrompt = prompt

        // 获取用户ID
        val userId = options.metadata?.get("userId")

        // 如果有用户ID，应用用户偏好
        if (userId != null) {
            val preference = getUserPreference(userId)
            if (preference != null) {
                enhancedPrompt = applyUserPreference(enhancedPrompt, preference)
            }
        }

        // 应用所有适应策略
        for (strategy in adaptationStrategies) {
            enhancedPrompt = strategy.enhancePrompt(enhancedPrompt, this, options)
        }

        return enhancedPrompt
    }

    /**
     * 应用适应策略增强消息
     */
    private fun applyAdaptationStrategies(
        messages: List<LlmMessage>,
        options: AgentGenerateOptions
    ): List<LlmMessage> {
        // 获取用户ID
        val userId = options.metadata?.get("userId")

        // 如果没有用户ID或适应策略，直接返回原始消息
        if (userId == null && adaptationStrategies.isEmpty()) {
            return messages
        }

        // 创建消息的可变副本
        val enhancedMessages = messages.toMutableList()

        // 找到最后一条用户消息
        val lastUserMessageIndex = enhancedMessages.indexOfLast { it.role == LlmMessageRole.USER }

        if (lastUserMessageIndex >= 0) {
            val lastUserMessage = enhancedMessages[lastUserMessageIndex]

            // 增强用户消息
            var enhancedContent = lastUserMessage.content

            // 如果有用户ID，应用用户偏好
            if (userId != null) {
                val preference = getUserPreference(userId)
                if (preference != null) {
                    enhancedContent = applyUserPreference(enhancedContent, preference)
                }
            }

            // 应用所有适应策略
            for (strategy in adaptationStrategies) {
                enhancedContent = strategy.enhancePrompt(enhancedContent, this, options)
            }

            // 替换原始消息
            if (enhancedContent != lastUserMessage.content) {
                enhancedMessages[lastUserMessageIndex] = lastUserMessage.copy(content = enhancedContent)
            }
        }

        return enhancedMessages
    }

    /**
     * 应用用户偏好
     */
    private fun applyUserPreference(prompt: String, preference: UserPreference): String {
        // 根据用户偏好增强提示
        val preferencesText = buildString {
            appendLine("用户偏好：")
            if (preference.communicationStyle != null) {
                appendLine("- 沟通风格：${preference.communicationStyle}")
            }
            if (preference.detailLevel != null) {
                appendLine("- 详细程度：${preference.detailLevel}")
            }
            if (preference.topics.isNotEmpty()) {
                appendLine("- 感兴趣的主题：${preference.topics.joinToString(", ")}")
            }
            if (preference.avoidTopics.isNotEmpty()) {
                appendLine("- 避免的主题：${preference.avoidTopics.joinToString(", ")}")
            }
            preference.customPreferences.forEach { (key, value) ->
                appendLine("- $key：$value")
            }
        }

        return """
            $preferencesText

            请根据上述用户偏好回答以下问题：

            $prompt
        """.trimIndent()
    }

    /**
     * 记录交互
     */
    private fun recordInteraction(interaction: Interaction) {
        interactionHistory.add(interaction)

        // 如果历史记录超过配置的最大数量，移除最旧的记录
        while (interactionHistory.size > config.maxInteractionHistory) {
            interactionHistory.removeAt(0)
        }
    }

    /**
     * 更新用户偏好
     */
    private fun updateUserPreference(userId: String, interaction: Interaction) {
        // 获取现有偏好或创建新偏好
        val preference = userPreferences.getOrPut(userId) { UserPreference() }

        // 根据交互和反馈更新偏好
        interaction.feedback?.let { feedback ->
            // 如果评分高，可能表明用户喜欢这种响应方式
            if (feedback.rating >= 4) {
                // 这里可以实现更复杂的偏好学习逻辑
                // 例如，分析响应的特征并更新偏好
            }
        }
    }

    /**
     * 从交互中学习
     */
    private suspend fun learnFromInteraction(interaction: Interaction) {
        // 分析交互，学习用户偏好和模式
        // 这里可以实现更复杂的学习逻辑
        agentLogger.debug { "Learning from interaction ${interaction.id}" }
    }

    /**
     * 从反馈中学习
     */
    private suspend fun learnFromFeedback(interaction: Interaction) {
        // 分析反馈，改进响应策略
        // 这里可以实现更复杂的学习逻辑
        agentLogger.debug { "Learning from feedback for interaction ${interaction.id}" }

        // 如果评分低，可以生成改进版本的响应
        interaction.feedback?.let { feedback ->
            if (feedback.rating <= 3) {
                val improvementPrompt = """
                    我之前对以下问题的回答没有达到用户期望。请帮我分析原因并提供改进建议：

                    用户问题：${interaction.prompt}

                    我的回答：${interaction.response}

                    用户评分：${feedback.rating}/5
                    用户反馈：${feedback.comment ?: "无具体反馈"}

                    请分析我的回答存在的问题，并提供具体的改进建议。
                """.trimIndent()

                val improvementResponse = baseAgent.generate(improvementPrompt)
                agentLogger.debug { "Improvement suggestion: ${improvementResponse.text}" }

                // 这里可以实现更多的学习和适应逻辑
                // 例如，更新Agent的指令或创建新版本
            }
        }
    }
}

/**
 * 自适应Agent配置
 */
@Serializable
data class AdaptiveAgentConfig(
    val enableAutoLearning: Boolean = true,
    val maxInteractionHistory: Int = 1000,
    val learningRate: Double = 0.1
)

/**
 * 用户偏好
 */
@Serializable
data class UserPreference(
    val communicationStyle: String? = null, // 例如：正式、友好、简洁
    val detailLevel: String? = null, // 例如：简要、详细、技术性
    val topics: List<String> = emptyList(), // 感兴趣的主题
    val avoidTopics: List<String> = emptyList(), // 避免的主题
    val customPreferences: Map<String, String> = emptyMap() // 自定义偏好
)

/**
 * 交互记录
 */
@Serializable
data class Interaction(
    val id: String,
    val prompt: String,
    val response: String,
    val startTime: Instant,
    val endTime: Instant,
    val userId: String? = null,
    val sessionId: String? = null,
    var feedback: Feedback? = null
)

/**
 * 反馈
 */
@Serializable
data class Feedback(
    val rating: Int, // 1-5
    val comment: String? = null,
    val timestamp: Instant
)

/**
 * 适应策略接口
 */
interface AdaptationStrategy {
    /**
     * 增强提示
     */
    fun enhancePrompt(prompt: String, agent: AdaptiveAgent, options: AgentGenerateOptions): String
}

/**
 * 用户反馈策略
 */
class UserFeedbackStrategy : AdaptationStrategy {
    override fun enhancePrompt(prompt: String, agent: AdaptiveAgent, options: AgentGenerateOptions): String {
        val userId = options.metadata?.get("userId") ?: return prompt

        // 获取用户的最近交互
        val recentInteractions = agent.getInteractionHistory(5, userId)

        // 如果没有足够的交互历史，直接返回原始提示
        if (recentInteractions.isEmpty()) {
            return prompt
        }

        // 分析用户反馈
        val feedbackAnalysis = analyzeUserFeedback(recentInteractions)

        // 如果没有有用的反馈分析，直接返回原始提示
        if (feedbackAnalysis.isBlank()) {
            return prompt
        }

        // 增强提示
        return """
            根据用户的反馈历史，请注意以下几点：
            $feedbackAnalysis

            用户问题：
            $prompt
        """.trimIndent()
    }

    /**
     * 分析用户反馈
     */
    private fun analyzeUserFeedback(interactions: List<Interaction>): String {
        val feedbackPoints = mutableListOf<String>()

        // 分析高评分交互
        val highRatedInteractions = interactions.filter { it.feedback?.rating ?: 0 >= 4 }
        if (highRatedInteractions.isNotEmpty()) {
            feedbackPoints.add("用户喜欢的响应风格：用户对类似以下内容的响应评价较高")
        }

        // 分析低评分交互
        val lowRatedInteractions = interactions.filter { it.feedback?.rating ?: 5 <= 2 }
        if (lowRatedInteractions.isNotEmpty()) {
            feedbackPoints.add("用户不喜欢的响应风格：用户对类似以下内容的响应评价较低")
        }

        return feedbackPoints.joinToString("\n\n")
    }
}

/**
 * 交互模式策略
 */
class InteractionPatternStrategy : AdaptationStrategy {
    override fun enhancePrompt(prompt: String, agent: AdaptiveAgent, options: AgentGenerateOptions): String {
        val userId = options.metadata?.get("userId") ?: return prompt

        // 获取用户的交互历史
        val userInteractions = agent.getInteractionHistory(20, userId)

        // 如果没有足够的交互历史，直接返回原始提示
        if (userInteractions.size < 5) {
            return prompt
        }

        // 分析交互模式
        val patternAnalysis = analyzeInteractionPatterns(userInteractions)

        // 如果没有有用的模式分析，直接返回原始提示
        if (patternAnalysis.isBlank()) {
            return prompt
        }

        // 增强提示
        return """
            根据用户的交互模式，请注意以下几点：
            $patternAnalysis

            用户问题：
            $prompt
        """.trimIndent()
    }

    /**
     * 分析交互模式
     */
    private fun analyzeInteractionPatterns(interactions: List<Interaction>): String {
        // 这里可以实现更复杂的模式分析逻辑
        // 例如，分析用户提问的类型、长度、复杂度等

        return "用户倾向于提出简短、直接的问题，希望得到具体的答案。"
    }
}

/**
 * 性能指标策略
 */
class PerformanceMetricStrategy : AdaptationStrategy {
    override fun enhancePrompt(prompt: String, agent: AdaptiveAgent, options: AgentGenerateOptions): String {
        // 这里可以实现基于性能指标的适应策略
        // 例如，根据响应时间、成功率等调整提示

        return prompt
    }
}

/**
 * 创建自适应Agent的DSL函数
 */
fun adaptiveAgent(init: AdaptiveAgentBuilder.() -> Unit): AdaptiveAgent {
    val builder = AdaptiveAgentBuilder()
    builder.init()
    return builder.build()
}

/**
 * 自适应Agent构建器
 */
class AdaptiveAgentBuilder {
    private var baseAgent: Agent? = null
    private var config = AdaptiveAgentConfig()
    private val adaptationStrategies = mutableListOf<AdaptationStrategy>()

    /**
     * 设置基础Agent
     */
    fun baseAgent(agent: Agent) {
        this.baseAgent = agent
    }

    /**
     * 配置自适应Agent
     */
    fun config(init: AdaptiveAgentConfigBuilder.() -> Unit) {
        val builder = AdaptiveAgentConfigBuilder(config)
        builder.init()
        this.config = builder.build()
    }

    /**
     * 添加适应策略
     */
    fun addStrategy(strategy: AdaptationStrategy) {
        adaptationStrategies.add(strategy)
    }

    /**
     * 构建自适应Agent
     */
    fun build(): AdaptiveAgent {
        requireNotNull(baseAgent) { "Base agent must be set" }

        val agent = AdaptiveAgent(
            baseAgent = baseAgent!!,
            config = config
        )

        // 添加自定义适应策略
        adaptationStrategies.forEach { agent.addAdaptationStrategy(it) }

        return agent
    }
}

/**
 * 自适应Agent配置构建器
 */
class AdaptiveAgentConfigBuilder(private val config: AdaptiveAgentConfig) {
    private var enableAutoLearning: Boolean = config.enableAutoLearning
    private var maxInteractionHistory: Int = config.maxInteractionHistory
    private var learningRate: Double = config.learningRate
    
    /**
     * 启用/禁用自动学习
     */
    fun enableAutoLearning(enable: Boolean) {
        this.enableAutoLearning = enable
    }

    /**
     * 设置最大交互历史数量
     */
    fun maxInteractionHistory(max: Int) {
        this.maxInteractionHistory = max
    }

    /**
     * 设置学习率
     */
    fun learningRate(rate: Double) {
        this.learningRate = rate
    }

    /**
     * 构建配置
     */
    fun build(): AdaptiveAgentConfig {
        return AdaptiveAgentConfig(
            enableAutoLearning = enableAutoLearning,
            maxInteractionHistory = maxInteractionHistory,
            learningRate = learningRate
        )
    }
}
