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
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 反思型Agent架构
 *
 * 反思型Agent对自己的思考过程、决策和行为进行反思和评估。
 * 通过元认知能力不断改进推理和问题解决能力。
 */
class ReflectiveAgent(
    private val baseAgent: Agent,
    private val config: ReflectiveAgentConfig = ReflectiveAgentConfig()
) : KastraXBase(component = "REFLECTIVE_AGENT", name = baseAgent.name), Agent {

    private val agentLogger = KotlinLogging.logger {}
    
    override val versionManager: AgentVersionManager? = baseAgent.versionManager

    // 会话反思存储
    private val sessionReflections = ConcurrentHashMap<String, MutableList<Reflection>>()
    
    // 全局反思存储
    private val globalReflections = mutableListOf<Reflection>()

    /**
     * 生成响应
     */
    override suspend fun generate(prompt: String, options: AgentGenerateOptions): AgentResponse = coroutineScope {
        agentLogger.debug { "Generating response with reflection for prompt: $prompt" }

        // 记录开始时间
        val startTime = Clock.System.now()

        // 获取会话ID
        val sessionId = options.metadata?.get("sessionId")

        // 获取会话的历史反思
        val sessionReflectionList = if (sessionId != null) {
            sessionReflections.getOrPut(sessionId) { mutableListOf() }
        } else {
            mutableListOf()
        }

        // 如果启用了预反思，先进行预反思
        val preReflectionResult = if (config.enablePreReflection) {
            val preReflectionDeferred = async {
                performPreReflection(prompt, sessionReflectionList)
            }
            preReflectionDeferred.await()
        } else null

        // 增强提示
        val enhancedPrompt = if (preReflectionResult != null) {
            """
                ${config.preReflectionPromptPrefix ?: ""}
                
                我的反思：
                $preReflectionResult
                
                用户问题：
                $prompt
            """.trimIndent()
        } else {
            prompt
        }

        // 使用基础Agent生成响应
        val baseResponse = baseAgent.generate(enhancedPrompt, options)

        // 如果启用了响应反思，对响应进行反思
        val responseReflectionResult = if (config.enableResponseReflection) {
            val responseReflectionDeferred = async {
                performResponseReflection(prompt, baseResponse.text, sessionReflectionList)
            }
            responseReflectionDeferred.await()
        } else null

        // 如果启用了后反思，进行后反思
        if (config.enablePostReflection) {
            val postReflectionDeferred = async {
                performPostReflection(prompt, baseResponse.text, sessionReflectionList)
            }
            
            // 不等待后反思完成，让它在后台运行
        }

        // 如果有响应反思结果，增强响应
        val finalResponse = if (responseReflectionResult != null && config.includeReflectionInResponse) {
            val enhancedText = """
                ${baseResponse.text}
                
                ${config.responseReflectionPrefix ?: ""}
                
                $responseReflectionResult
            """.trimIndent()
            
            baseResponse.copy(text = enhancedText)
        } else {
            baseResponse
        }

        // 记录结束时间
        val endTime = Clock.System.now()

        // 如果启用了学习，从反思中学习
        if (config.enableLearningFromReflection && sessionReflectionList.isNotEmpty()) {
            val learningDeferred = async {
                learnFromReflections(sessionReflectionList)
            }
            
            // 不等待学习完成，让它在后台运行
        }

        return@coroutineScope finalResponse
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
        
        // 如果没有用户消息，直接委派给基础Agent
        return baseAgent.generate(messages, options)
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
        // 重置基础Agent
        baseAgent.reset()
        
        // 清除所有会话反思
        sessionReflections.clear()
    }

    /**
     * 获取Agent状态
     */
    override suspend fun getState(): AgentState? {
        // 返回基础Agent的状态
        return baseAgent.getState()
    }

    /**
     * 更新Agent状态
     */
    override suspend fun updateState(status: AgentStatus): AgentState? {
        // 更新基础Agent的状态
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
        // 使用基础Agent创建会话
        return baseAgent.createSession(title, resourceId, metadata)
    }

    /**
     * 获取会话信息
     */
    override suspend fun getSession(sessionId: String): SessionInfo? {
        // 获取基础Agent的会话信息
        return baseAgent.getSession(sessionId)
    }

    /**
     * 获取会话消息
     */
    override suspend fun getSessionMessages(sessionId: String, limit: Int): List<SessionMessage>? {
        // 获取基础Agent的会话消息
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
        // 使用基础Agent的版本管理器创建新版本
        return baseAgent.createVersion(instructions, name, description, metadata, activateImmediately)
    }

    /**
     * 获取所有版本
     */
    override suspend fun getVersions(limit: Int, offset: Int): List<AgentVersion>? {
        // 获取基础Agent的所有版本
        return baseAgent.getVersions(limit, offset)
    }

    /**
     * 获取当前激活版本
     */
    override suspend fun getActiveVersion(): AgentVersion? {
        // 获取基础Agent的当前激活版本
        return baseAgent.getActiveVersion()
    }

    /**
     * 激活版本
     */
    override suspend fun activateVersion(versionId: String): AgentVersion? {
        // 激活基础Agent的指定版本
        return baseAgent.activateVersion(versionId)
    }

    /**
     * 回滚到指定版本
     */
    override suspend fun rollbackToVersion(versionId: String): AgentVersion? {
        // 回滚基础Agent到指定版本
        return baseAgent.rollbackToVersion(versionId)
    }

    /**
     * 获取会话的反思
     *
     * @param sessionId 会话ID
     * @param limit 限制数量
     * @return 反思列表
     */
    fun getSessionReflections(sessionId: String, limit: Int = 10): List<Reflection> {
        return sessionReflections[sessionId]?.takeLast(limit) ?: emptyList()
    }

    /**
     * 获取全局反思
     *
     * @param limit 限制数量
     * @return 反思列表
     */
    fun getGlobalReflections(limit: Int = 10): List<Reflection> {
        return globalReflections.takeLast(limit)
    }

    /**
     * 添加反思
     *
     * @param reflection 反思
     * @param sessionId 会话ID，如果为null则添加到全局反思
     */
    fun addReflection(reflection: Reflection, sessionId: String? = null) {
        if (sessionId != null) {
            val sessionReflectionList = sessionReflections.getOrPut(sessionId) { mutableListOf() }
            sessionReflectionList.add(reflection)
            
            // 如果会话反思超过最大数量，移除最旧的
            while (sessionReflectionList.size > config.maxReflectionsPerSession) {
                sessionReflectionList.removeAt(0)
            }
        } else {
            globalReflections.add(reflection)
            
            // 如果全局反思超过最大数量，移除最旧的
            while (globalReflections.size > config.maxGlobalReflections) {
                globalReflections.removeAt(0)
            }
        }
    }

    /**
     * 执行预反思
     */
    private suspend fun performPreReflection(
        prompt: String,
        sessionReflections: List<Reflection>
    ): String {
        agentLogger.debug { "Performing pre-reflection for prompt: $prompt" }
        
        // 构建预反思提示
        val preReflectionPrompt = buildString {
            appendLine("请在回答用户问题之前，先进行反思。考虑以下几点：")
            appendLine("1. 这个问题的核心是什么？")
            appendLine("2. 我需要什么信息来回答这个问题？")
            appendLine("3. 可能存在哪些误解或假设？")
            appendLine("4. 我应该如何组织我的回答？")
            
            if (sessionReflections.isNotEmpty()) {
                appendLine("\n过去的反思：")
                sessionReflections.takeLast(3).forEach { reflection ->
                    appendLine("- ${reflection.content}")
                }
            }
            
            appendLine("\n用户问题：")
            appendLine(prompt)
            
            appendLine("\n请提供简洁的反思，不超过5句话。")
        }
        
        // 使用基础Agent生成预反思
        val response = baseAgent.generate(preReflectionPrompt)
        
        // 创建反思记录
        val reflection = Reflection(
            id = UUID.randomUUID().toString(),
            type = ReflectionType.PRE_REFLECTION,
            content = response.text,
            timestamp = Clock.System.now(),
            prompt = prompt,
            response = null
        )
        
        // 添加到全局反思
        addReflection(reflection)
        
        return response.text
    }

    /**
     * 执行响应反思
     */
    private suspend fun performResponseReflection(
        prompt: String,
        response: String,
        sessionReflections: List<Reflection>
    ): String {
        agentLogger.debug { "Performing response reflection for prompt: $prompt" }
        
        // 构建响应反思提示
        val responseReflectionPrompt = buildString {
            appendLine("请对我的回答进行反思和评估。考虑以下几点：")
            appendLine("1. 我的回答是否完全解决了用户的问题？")
            appendLine("2. 我的解释是否清晰、准确？")
            appendLine("3. 我是否考虑了不同的观点或可能性？")
            appendLine("4. 我的回答有哪些可以改进的地方？")
            
            appendLine("\n用户问题：")
            appendLine(prompt)
            
            appendLine("\n我的回答：")
            appendLine(response)
            
            appendLine("\n请提供简洁的反思，不超过5句话。")
        }
        
        // 使用基础Agent生成响应反思
        val reflectionResponse = baseAgent.generate(responseReflectionPrompt)
        
        // 创建反思记录
        val reflection = Reflection(
            id = UUID.randomUUID().toString(),
            type = ReflectionType.RESPONSE_REFLECTION,
            content = reflectionResponse.text,
            timestamp = Clock.System.now(),
            prompt = prompt,
            response = response
        )
        
        // 添加到全局反思
        addReflection(reflection)
        
        return reflectionResponse.text
    }

    /**
     * 执行后反思
     */
    private suspend fun performPostReflection(
        prompt: String,
        response: String,
        sessionReflections: List<Reflection>
    ): String {
        agentLogger.debug { "Performing post-reflection for prompt: $prompt" }
        
        // 构建后反思提示
        val postReflectionPrompt = buildString {
            appendLine("请在回答用户问题后，进行深入反思。考虑以下几点：")
            appendLine("1. 我的回答过程中使用了什么推理策略？")
            appendLine("2. 我是否遇到了认知偏见或思维盲点？")
            appendLine("3. 我如何改进我的思考过程？")
            appendLine("4. 从这次交互中，我学到了什么？")
            
            appendLine("\n用户问题：")
            appendLine(prompt)
            
            appendLine("\n我的回答：")
            appendLine(response)
            
            appendLine("\n请提供深入的反思，重点关注思考过程而非内容。")
        }
        
        // 使用基础Agent生成后反思
        val reflectionResponse = baseAgent.generate(postReflectionPrompt)
        
        // 创建反思记录
        val reflection = Reflection(
            id = UUID.randomUUID().toString(),
            type = ReflectionType.POST_REFLECTION,
            content = reflectionResponse.text,
            timestamp = Clock.System.now(),
            prompt = prompt,
            response = response
        )
        
        // 添加到全局反思
        addReflection(reflection)
        
        return reflectionResponse.text
    }

    /**
     * 从反思中学习
     */
    private suspend fun learnFromReflections(reflections: List<Reflection>) {
        agentLogger.debug { "Learning from ${reflections.size} reflections" }
        
        // 如果反思数量不足，不进行学习
        if (reflections.size < config.minReflectionsForLearning) {
            return
        }
        
        // 构建学习提示
        val learningPrompt = buildString {
            appendLine("请分析以下反思记录，总结可以改进的关键模式和策略：")
            
            reflections.takeLast(config.maxReflectionsForLearning).forEach { reflection ->
                appendLine("\n反思类型：${reflection.type}")
                appendLine("内容：${reflection.content}")
                if (reflection.prompt != null) {
                    appendLine("问题：${reflection.prompt}")
                }
                if (reflection.response != null) {
                    appendLine("回答：${reflection.response}")
                }
                appendLine("---")
            }
            
            appendLine("\n请提供3-5条具体的改进建议，每条建议应包含：")
            appendLine("1. 观察到的模式")
            appendLine("2. 问题或机会")
            appendLine("3. 具体的改进策略")
        }
        
        // 使用基础Agent生成学习结果
        val learningResponse = baseAgent.generate(learningPrompt)
        
        // 创建学习记录
        val learning = Learning(
            id = UUID.randomUUID().toString(),
            content = learningResponse.text,
            timestamp = Clock.System.now(),
            reflectionIds = reflections.map { it.id }
        )
        
        // 这里可以实现更多的学习逻辑
        // 例如，更新Agent的指令或创建新版本
    }
}

/**
 * 反思型Agent配置
 */
@Serializable
data class ReflectiveAgentConfig(
    val enablePreReflection: Boolean = true,
    val enableResponseReflection: Boolean = true,
    val enablePostReflection: Boolean = true,
    val includeReflectionInResponse: Boolean = false,
    val enableLearningFromReflection: Boolean = true,
    val maxReflectionsPerSession: Int = 100,
    val maxGlobalReflections: Int = 1000,
    val minReflectionsForLearning: Int = 5,
    val maxReflectionsForLearning: Int = 20,
    val preReflectionPromptPrefix: String? = null,
    val responseReflectionPrefix: String? = "我的反思："
)

/**
 * 反思类型
 */
enum class ReflectionType {
    PRE_REFLECTION,
    RESPONSE_REFLECTION,
    POST_REFLECTION
}

/**
 * 反思
 */
@Serializable
data class Reflection(
    val id: String,
    val type: ReflectionType,
    val content: String,
    val timestamp: Instant,
    val prompt: String? = null,
    val response: String? = null
)

/**
 * 学习
 */
@Serializable
data class Learning(
    val id: String,
    val content: String,
    val timestamp: Instant,
    val reflectionIds: List<String>
)

/**
 * 创建反思型Agent的DSL函数
 */
fun reflectiveAgent(init: ReflectiveAgentBuilder.() -> Unit): ReflectiveAgent {
    val builder = ReflectiveAgentBuilder()
    builder.init()
    return builder.build()
}

/**
 * 反思型Agent构建器
 */
class ReflectiveAgentBuilder {
    private var baseAgent: Agent? = null
    private var config = ReflectiveAgentConfig()

    /**
     * 设置基础Agent
     */
    fun baseAgent(agent: Agent) {
        this.baseAgent = agent
    }

    /**
     * 配置反思型Agent
     */
    fun config(init: ReflectiveAgentConfigBuilder.() -> Unit) {
        val builder = ReflectiveAgentConfigBuilder(config)
        builder.init()
        this.config = builder.build()
    }

    /**
     * 构建反思型Agent
     */
    fun build(): ReflectiveAgent {
        requireNotNull(baseAgent) { "Base agent must be set" }

        return ReflectiveAgent(
            baseAgent = baseAgent!!,
            config = config
        )
    }
}

/**
 * 反思型Agent配置构建器
 */
class ReflectiveAgentConfigBuilder(private val config: ReflectiveAgentConfig) {
    private var enablePreReflection: Boolean = config.enablePreReflection
    private var enableResponseReflection: Boolean = config.enableResponseReflection
    private var enablePostReflection: Boolean = config.enablePostReflection
    private var includeReflectionInResponse: Boolean = config.includeReflectionInResponse
    private var enableLearningFromReflection: Boolean = config.enableLearningFromReflection
    private var maxReflectionsPerSession: Int = config.maxReflectionsPerSession
    private var maxGlobalReflections: Int = config.maxGlobalReflections
    private var minReflectionsForLearning: Int = config.minReflectionsForLearning
    private var maxReflectionsForLearning: Int = config.maxReflectionsForLearning
    private var preReflectionPromptPrefix: String? = config.preReflectionPromptPrefix
    private var responseReflectionPrefix: String? = config.responseReflectionPrefix
    
    /**
     * 启用/禁用预反思
     */
    fun enablePreReflection(enable: Boolean) {
        this.enablePreReflection = enable
    }
    
    /**
     * 启用/禁用响应反思
     */
    fun enableResponseReflection(enable: Boolean) {
        this.enableResponseReflection = enable
    }
    
    /**
     * 启用/禁用后反思
     */
    fun enablePostReflection(enable: Boolean) {
        this.enablePostReflection = enable
    }
    
    /**
     * 设置是否在响应中包含反思
     */
    fun includeReflectionInResponse(include: Boolean) {
        this.includeReflectionInResponse = include
    }
    
    /**
     * 启用/禁用从反思中学习
     */
    fun enableLearningFromReflection(enable: Boolean) {
        this.enableLearningFromReflection = enable
    }
    
    /**
     * 设置每个会话的最大反思数
     */
    fun maxReflectionsPerSession(max: Int) {
        this.maxReflectionsPerSession = max
    }
    
    /**
     * 设置全局最大反思数
     */
    fun maxGlobalReflections(max: Int) {
        this.maxGlobalReflections = max
    }
    
    /**
     * 设置学习所需的最小反思数
     */
    fun minReflectionsForLearning(min: Int) {
        this.minReflectionsForLearning = min
    }
    
    /**
     * 设置学习使用的最大反思数
     */
    fun maxReflectionsForLearning(max: Int) {
        this.maxReflectionsForLearning = max
    }
    
    /**
     * 设置预反思提示前缀
     */
    fun preReflectionPromptPrefix(prefix: String?) {
        this.preReflectionPromptPrefix = prefix
    }
    
    /**
     * 设置响应反思前缀
     */
    fun responseReflectionPrefix(prefix: String?) {
        this.responseReflectionPrefix = prefix
    }
    
    /**
     * 构建配置
     */
    fun build(): ReflectiveAgentConfig {
        return ReflectiveAgentConfig(
            enablePreReflection = enablePreReflection,
            enableResponseReflection = enableResponseReflection,
            enablePostReflection = enablePostReflection,
            includeReflectionInResponse = includeReflectionInResponse,
            enableLearningFromReflection = enableLearningFromReflection,
            maxReflectionsPerSession = maxReflectionsPerSession,
            maxGlobalReflections = maxGlobalReflections,
            minReflectionsForLearning = minReflectionsForLearning,
            maxReflectionsForLearning = maxReflectionsForLearning,
            preReflectionPromptPrefix = preReflectionPromptPrefix,
            responseReflectionPrefix = responseReflectionPrefix
        )
    }
}
