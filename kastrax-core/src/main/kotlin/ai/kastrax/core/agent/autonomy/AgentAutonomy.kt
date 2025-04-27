package ai.kastrax.core.agent.autonomy

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.util.UUID

/**
 * Agent自主性模式
 */
enum class AutonomyMode {
    REACTIVE,    // 反应式：只响应用户输入
    PROACTIVE,   // 主动式：可以主动提出建议和想法
    EXPLORATIVE, // 探索式：可以自主探索和学习
    CREATIVE,    // 创造式：可以生成创意内容
    ADAPTIVE     // 适应式：可以学习和适应环境
}

/**
 * Agent自主性级别
 */
enum class AutonomyLevel {
    NONE,       // 无自主性
    LOW,        // 低自主性
    MEDIUM,     // 中等自主性
    HIGH,       // 高自主性
    FULL        // 完全自主性
}

/**
 * Agent创意类型
 */
enum class CreativityType {
    COMBINATIONAL, // 组合式创意：组合现有想法
    EXPLORATORY,   // 探索式创意：探索现有概念空间
    TRANSFORMATIONAL // 转换式创意：改变概念空间
}

/**
 * Agent自主行为
 */
@Serializable
data class AutonomousBehavior(
    val id: String = UUID.randomUUID().toString(),
    val agentId: String,
    val type: String,
    val description: String,
    val timestamp: Instant = Clock.System.now(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Agent创意内容
 */
@Serializable
data class CreativeContent(
    val id: String = UUID.randomUUID().toString(),
    val agentId: String,
    val type: CreativityType,
    val content: String,
    val inspiration: String? = null,
    val timestamp: Instant = Clock.System.now(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Agent自主目标
 */
@Serializable
data class AutonomousGoal(
    val id: String = UUID.randomUUID().toString(),
    val agentId: String,
    val description: String,
    val priority: Int = 1,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val createdAt: Instant = Clock.System.now(),
    val completedAt: Instant? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 目标状态
 */
enum class GoalStatus {
    ACTIVE,     // 活跃状态
    COMPLETED,  // 已完成
    ABANDONED,  // 已放弃
    PAUSED      // 已暂停
}

/**
 * Agent自主性配置
 */
@Serializable
data class AutonomyConfig(
    val mode: AutonomyMode = AutonomyMode.REACTIVE,
    val level: AutonomyLevel = AutonomyLevel.MEDIUM,
    val creativityLevel: Double = 0.5,
    val explorationRate: Double = 0.3,
    val learningRate: Double = 0.2,
    val maxGoals: Int = 3,
    val enabledCapabilities: Set<AutonomyCapability> = setOf(
        AutonomyCapability.SELF_REFLECTION,
        AutonomyCapability.IDEA_GENERATION
    ),
    val constraints: List<String> = listOf(
        "不能执行有害操作",
        "必须遵循用户的明确指示",
        "不能违反伦理准则"
    )
)

/**
 * Agent自主能力
 */
enum class AutonomyCapability {
    SELF_REFLECTION,    // 自我反思
    IDEA_GENERATION,    // 想法生成
    GOAL_SETTING,       // 目标设定
    LEARNING,           // 学习能力
    EXPLORATION,        // 探索能力
    ADAPTATION,         // 适应能力
    CREATIVITY,         // 创造力
    PLANNING,           // 规划能力
    SELF_EVALUATION,    // 自我评估
    CURIOSITY           // 好奇心
}

/**
 * Agent自主性管理器
 */
class AgentAutonomy(
    private val agent: Agent,
    private val config: AutonomyConfig = AutonomyConfig()
) : KastraXBase(component = "AUTONOMY", name = agent.name) {
    
    // 自主行为历史
    private val behaviorHistory = mutableListOf<AutonomousBehavior>()
    
    // 创意内容历史
    private val creativeHistory = mutableListOf<CreativeContent>()
    
    // 自主目标
    private val goals = mutableListOf<AutonomousGoal>()
    
    // 学习的知识
    private val learnedKnowledge = mutableMapOf<String, String>()
    
    /**
     * 生成自主响应
     */
    suspend fun generateAutonomousResponse(
        input: String,
        options: AgentGenerateOptions = AgentGenerateOptions()
    ): AgentResponse {
        // 记录自主行为
        recordBehavior("response_generation", "生成对用户输入的自主响应")
        
        // 根据自主性模式和级别增强提示
        val enhancedPrompt = enhancePromptWithAutonomy(input)
        
        // 生成响应
        val response = agent.generate(enhancedPrompt, options)
        
        // 如果启用了自我反思，添加自我反思
        if (AutonomyCapability.SELF_REFLECTION in config.enabledCapabilities) {
            // 异步执行自我反思，不阻塞响应
            coroutineScope {
                async {
                    performSelfReflection(input, response.text)
                }
            }
        }
        
        return response
    }
    
    /**
     * 根据自主性模式和级别增强提示
     */
    private fun enhancePromptWithAutonomy(input: String): String {
        val autonomyInstructions = when (config.mode) {
            AutonomyMode.REACTIVE -> {
                // 反应式模式不添加额外指令
                input
            }
            AutonomyMode.PROACTIVE -> {
                // 主动式模式添加主动提供建议的指令
                """
                在回答以下问题时，除了直接回答，还可以主动提供相关的建议和想法：
                
                $input
                """.trimIndent()
            }
            AutonomyMode.EXPLORATIVE -> {
                // 探索式模式添加探索和学习的指令
                """
                在回答以下问题时，可以探索相关的概念和想法，并提出新的问题或方向：
                
                $input
                
                如果你认为有值得探索的相关主题，请主动提出。
                """.trimIndent()
            }
            AutonomyMode.CREATIVE -> {
                // 创造式模式添加创意生成的指令
                """
                在回答以下问题时，发挥你的创造力，提出新颖、独特的想法和解决方案：
                
                $input
                
                不要局限于常规思维，尝试提出创新的观点。
                """.trimIndent()
            }
            AutonomyMode.ADAPTIVE -> {
                // 适应式模式添加学习和适应的指令
                """
                在回答以下问题时，考虑我们之前的交流，并根据我的偏好和需求调整你的回答：
                
                $input
                
                随着我们的交流，不断学习和适应我的需求。
                """.trimIndent()
            }
        }
        
        // 根据自主性级别调整指令的强度
        return when (config.level) {
            AutonomyLevel.NONE -> input // 无自主性，直接返回原始输入
            AutonomyLevel.LOW -> {
                // 低自主性，轻微增强
                if (config.mode == AutonomyMode.REACTIVE) input else autonomyInstructions
            }
            AutonomyLevel.MEDIUM -> {
                // 中等自主性，正常增强
                autonomyInstructions
            }
            AutonomyLevel.HIGH, AutonomyLevel.FULL -> {
                // 高/完全自主性，强烈增强
                """
                $autonomyInstructions
                
                在回答时，充分发挥你的自主性和创造力，不必严格局限于问题的直接回答。
                你可以探索相关主题，提出新的想法，甚至挑战问题的假设。
                """.trimIndent()
            }
        }
    }
    
    /**
     * 执行自我反思
     */
    private suspend fun performSelfReflection(input: String, response: String) {
        val reflectionPrompt = """
            请对以下交互进行反思：
            
            用户输入：$input
            
            你的回答：$response
            
            反思以下几点：
            1. 我的回答是否充分解决了用户的问题？
            2. 我是否展示了适当的自主性和创造力？
            3. 我的回答有哪些可以改进的地方？
            4. 我是否遵循了所有约束条件？
            
            请提供具体的反思和改进建议。
        """.trimIndent()
        
        val reflectionResponse = agent.generate(reflectionPrompt)
        
        // 记录自我反思
        recordBehavior("self_reflection", reflectionResponse.text)
        
        // 从反思中学习
        learnFromReflection(reflectionResponse.text)
    }
    
    /**
     * 从反思中学习
     */
    private fun learnFromReflection(reflection: String) {
        // 简单实现：提取关键学习点
        val learningPoints = reflection.split("\n")
            .filter { it.contains("改进") || it.contains("学习") || it.contains("应该") }
        
        if (learningPoints.isNotEmpty()) {
            val key = "reflection_${Clock.System.now()}"
            learnedKnowledge[key] = learningPoints.joinToString("\n")
            logger.debug { "从反思中学习: $key -> ${learnedKnowledge[key]}" }
        }
    }
    
    /**
     * 生成创意内容
     */
    suspend fun generateCreativeContent(
        prompt: String,
        type: CreativityType = CreativityType.EXPLORATORY,
        options: AgentGenerateOptions = AgentGenerateOptions()
    ): CreativeContent {
        // 检查是否启用了创造力
        if (AutonomyCapability.CREATIVITY !in config.enabledCapabilities) {
            throw IllegalStateException("创造力能力未启用")
        }
        
        // 记录自主行为
        recordBehavior("creative_generation", "生成创意内容: $type")
        
        // 根据创意类型增强提示
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
            temperature = options.temperature.coerceAtLeast(0.7) + config.creativityLevel * 0.3
        )
        
        // 生成创意内容
        val response = agent.generate(enhancedPrompt, creativeOptions)
        
        // 创建创意内容对象
        val creativeContent = CreativeContent(
            agentId = agent.name,
            type = type,
            content = response.text,
            inspiration = prompt
        )
        
        // 添加到历史
        creativeHistory.add(creativeContent)
        
        return creativeContent
    }
    
    /**
     * 设置自主目标
     */
    suspend fun setAutonomousGoal(description: String, priority: Int = 1): AutonomousGoal {
        // 检查是否启用了目标设定
        if (AutonomyCapability.GOAL_SETTING !in config.enabledCapabilities) {
            throw IllegalStateException("目标设定能力未启用")
        }
        
        // 检查是否超过最大目标数
        if (goals.count { it.status == GoalStatus.ACTIVE } >= config.maxGoals) {
            throw IllegalStateException("已达到最大目标数: ${config.maxGoals}")
        }
        
        // 记录自主行为
        recordBehavior("goal_setting", "设置自主目标: $description")
        
        // 创建目标
        val goal = AutonomousGoal(
            agentId = agent.name,
            description = description,
            priority = priority
        )
        
        // 添加到目标列表
        goals.add(goal)
        
        return goal
    }
    
    /**
     * 更新目标状态
     */
    fun updateGoalStatus(goalId: String, status: GoalStatus): AutonomousGoal? {
        val goal = goals.find { it.id == goalId } ?: return null
        
        // 更新状态
        val updatedGoal = if (status == GoalStatus.COMPLETED) {
            goal.copy(status = status, completedAt = Clock.System.now())
        } else {
            goal.copy(status = status)
        }
        
        // 更新目标列表
        goals.removeIf { it.id == goalId }
        goals.add(updatedGoal)
        
        // 记录自主行为
        recordBehavior("goal_update", "更新目标状态: ${goal.description} -> $status")
        
        return updatedGoal
    }
    
    /**
     * 自主探索
     */
    suspend fun exploreAutonomously(
        topic: String,
        depth: Int = 1,
        options: AgentGenerateOptions = AgentGenerateOptions()
    ): List<String> {
        // 检查是否启用了探索能力
        if (AutonomyCapability.EXPLORATION !in config.enabledCapabilities) {
            throw IllegalStateException("探索能力未启用")
        }
        
        // 记录自主行为
        recordBehavior("autonomous_exploration", "自主探索主题: $topic")
        
        val explorationResults = mutableListOf<String>()
        
        // 初始探索
        val initialPrompt = """
            请探索以下主题，提出3-5个值得进一步研究的子主题或问题：
            
            $topic
            
            对于每个子主题，简要解释为什么它值得探索。
        """.trimIndent()
        
        val initialResponse = agent.generate(initialPrompt, options)
        explorationResults.add(initialResponse.text)
        
        // 如果深度大于1，继续探索
        if (depth > 1) {
            // 提取子主题
            val subTopics = extractSubTopics(initialResponse.text)
            
            // 对每个子主题进行探索
            for (subTopic in subTopics.take(2)) { // 限制为前2个子主题，避免过度探索
                val subPrompt = """
                    请深入探索以下子主题，提供新的见解和想法：
                    
                    $subTopic
                """.trimIndent()
                
                val subResponse = agent.generate(subPrompt, options)
                explorationResults.add(subResponse.text)
            }
        }
        
        return explorationResults
    }
    
    /**
     * 提取子主题
     */
    private fun extractSubTopics(text: String): List<String> {
        // 简单实现：按行分割，查找可能的子主题标记
        return text.split("\n")
            .filter { line ->
                line.trim().matches(Regex("^\\d+\\..*|^-.*|^•.*")) ||
                line.contains("主题") || line.contains("问题")
            }
            .map { it.trim() }
    }
    
    /**
     * 记录自主行为
     */
    private fun recordBehavior(type: String, description: String) {
        val behavior = AutonomousBehavior(
            agentId = agent.name,
            type = type,
            description = description
        )
        
        behaviorHistory.add(behavior)
        logger.debug { "记录自主行为: $type - $description" }
    }
    
    /**
     * 获取自主行为历史
     */
    fun getBehaviorHistory(): List<AutonomousBehavior> {
        return behaviorHistory.toList()
    }
    
    /**
     * 获取创意内容历史
     */
    fun getCreativeHistory(): List<CreativeContent> {
        return creativeHistory.toList()
    }
    
    /**
     * 获取自主目标
     */
    fun getGoals(includeCompleted: Boolean = false): List<AutonomousGoal> {
        return if (includeCompleted) {
            goals.toList()
        } else {
            goals.filter { it.status != GoalStatus.COMPLETED && it.status != GoalStatus.ABANDONED }
        }
    }
    
    /**
     * 获取学习的知识
     */
    fun getLearnedKnowledge(): Map<String, String> {
        return learnedKnowledge.toMap()
    }
    
    /**
     * 获取当前配置
     */
    fun getConfig(): AutonomyConfig {
        return config
    }
}
