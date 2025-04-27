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
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 目标导向Agent架构
 *
 * 目标导向Agent能够设定目标、分解任务、制定计划并执行以实现目标。
 * 通过持续跟踪进度、调整计划和评估结果确保目标达成。
 */
class GoalOrientedAgent(
    private val baseAgent: Agent,
    private val config: GoalOrientedAgentConfig = GoalOrientedAgentConfig()
) : KastraXBase(component = "GOAL_ORIENTED_AGENT", name = baseAgent.name), Agent {

    private val agentLogger = KotlinLogging.logger {}
    
    override val versionManager: AgentVersionManager? = baseAgent.versionManager

    // 目标存储
    private val goals = ConcurrentHashMap<String, Goal>()
    
    // 任务存储
    private val tasks = ConcurrentHashMap<String, Task>()
    
    // 会话-目标映射
    private val sessionGoalMap = ConcurrentHashMap<String, String>()

    /**
     * 生成响应
     */
    override suspend fun generate(prompt: String, options: AgentGenerateOptions): AgentResponse = coroutineScope {
        // 获取会话ID
        val sessionId = options.metadata?.get("sessionId")
        
        // 如果提示长度超过最小目标提取长度，尝试提取目标
        if (prompt.length >= config.minPromptLengthForGoal && config.enableAutoGoalExtraction) {
            // 检查会话是否已有关联目标
            val existingGoalId = sessionId?.let { sessionGoalMap[it] }
            val existingGoal = existingGoalId?.let { goals[it] }
            
            if (existingGoal == null) {
                // 提取目标
                val goal = extractGoalFromPrompt(prompt)
                
                // 如果成功提取目标，关联到会话
                if (sessionId != null) {
                    sessionGoalMap[sessionId] = goal.id
                }
                
                // 如果启用了自动任务创建，分解目标为任务
                if (config.enableAutoTaskCreation) {
                    decomposeGoalIntoTasks(goal)
                }
            } else if (existingGoal.status == GoalStatus.ACTIVE) {
                // 检查任务是否已完成
                checkTasksCompletion(existingGoal, prompt)
            }
        }
        
        // 使用基础Agent生成响应
        return@coroutineScope baseAgent.generate(prompt, options)
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
        
        // 清除所有目标和任务
        goals.clear()
        tasks.clear()
        sessionGoalMap.clear()
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
     * 创建目标
     *
     * @param description 目标描述
     * @param priority 目标优先级
     * @param deadline 截止日期
     * @return 创建的目标
     */
    fun createGoal(
        description: String,
        priority: GoalPriority = GoalPriority.MEDIUM,
        deadline: Instant? = null
    ): Goal {
        val goalId = UUID.randomUUID().toString()
        val goal = Goal(
            id = goalId,
            description = description,
            priority = priority,
            deadline = deadline,
            createdAt = Clock.System.now(),
            status = GoalStatus.ACTIVE
        )
        
        goals[goalId] = goal
        agentLogger.debug { "Created goal: $goalId - $description" }
        
        return goal
    }

    /**
     * 获取目标
     *
     * @param goalId 目标ID
     * @return 目标，如果不存在则返回null
     */
    fun getGoal(goalId: String): Goal? {
        return goals[goalId]
    }

    /**
     * 获取所有目标
     *
     * @param includeCompleted 是否包含已完成的目标
     * @return 目标列表
     */
    fun getAllGoals(includeCompleted: Boolean = true): List<Goal> {
        return goals.values.filter { includeCompleted || it.status != GoalStatus.COMPLETED }
    }

    /**
     * 更新目标状态
     *
     * @param goalId 目标ID
     * @param status 新状态
     * @return 更新后的目标，如果目标不存在则返回null
     */
    fun updateGoalStatus(goalId: String, status: GoalStatus): Goal? {
        val goal = goals[goalId] ?: return null
        
        val updatedGoal = goal.copy(
            status = status,
            completedAt = if (status == GoalStatus.COMPLETED) Clock.System.now() else goal.completedAt
        )
        
        goals[goalId] = updatedGoal
        agentLogger.debug { "Updated goal status: $goalId - $status" }
        
        return updatedGoal
    }

    /**
     * 创建任务
     *
     * @param goalId 关联的目标ID
     * @param description 任务描述
     * @param priority 任务优先级
     * @param deadline 截止日期
     * @return 创建的任务，如果目标不存在则返回null
     */
    fun createTask(
        goalId: String,
        description: String,
        priority: TaskPriority = TaskPriority.MEDIUM,
        deadline: Instant? = null
    ): Task? {
        // 检查目标是否存在
        if (!goals.containsKey(goalId)) {
            agentLogger.warn { "Cannot create task: goal $goalId does not exist" }
            return null
        }
        
        val taskId = UUID.randomUUID().toString()
        val task = Task(
            id = taskId,
            goalId = goalId,
            description = description,
            priority = priority,
            deadline = deadline,
            createdAt = Clock.System.now(),
            status = TaskStatus.PENDING
        )
        
        tasks[taskId] = task
        agentLogger.debug { "Created task: $taskId - $description for goal: $goalId" }
        
        return task
    }

    /**
     * 获取任务
     *
     * @param taskId 任务ID
     * @return 任务，如果不存在则返回null
     */
    fun getTask(taskId: String): Task? {
        return tasks[taskId]
    }

    /**
     * 获取目标的所有任务
     *
     * @param goalId 目标ID
     * @param includeCompleted 是否包含已完成的任务
     * @return 任务列表
     */
    fun getTasksForGoal(goalId: String, includeCompleted: Boolean = true): List<Task> {
        return tasks.values
            .filter { it.goalId == goalId }
            .filter { includeCompleted || it.status != TaskStatus.COMPLETED }
    }

    /**
     * 更新任务状态
     *
     * @param taskId 任务ID
     * @param status 新状态
     * @return 更新后的任务，如果任务不存在则返回null
     */
    fun updateTaskStatus(taskId: String, status: TaskStatus): Task? {
        val task = tasks[taskId] ?: return null
        
        val updatedTask = task.copy(
            status = status,
            completedAt = if (status == TaskStatus.COMPLETED) Clock.System.now() else task.completedAt
        )
        
        tasks[taskId] = updatedTask
        agentLogger.debug { "Updated task status: $taskId - $status" }
        
        // 如果任务已完成，检查目标是否已完成
        if (status == TaskStatus.COMPLETED) {
            checkGoalCompletion(updatedTask.goalId)
        }
        
        return updatedTask
    }

    /**
     * 从提示中提取目标
     */
    private suspend fun extractGoalFromPrompt(prompt: String): Goal {
        // 构建目标提取提示
        val extractionPrompt = """
            请从以下用户输入中提取主要目标：

            用户输入：
            $prompt

            请只回答一个简洁的目标描述，不要添加任何其他内容。
        """.trimIndent()
        
        // 使用基础Agent提取目标
        val response = baseAgent.generate(extractionPrompt)
        val goalDescription = response.text.trim()
        
        // 创建目标
        return createGoal(goalDescription)
    }

    /**
     * 将目标分解为任务
     */
    private suspend fun decomposeGoalIntoTasks(goal: Goal) {
        // 构建任务分解提示
        val decompositionPrompt = """
            请将以下目标分解为3-5个具体的任务：

            目标：${goal.description}

            请列出完成这个目标所需的具体任务，每行一个任务，不要添加任何其他内容。
        """.trimIndent()
        
        // 使用基础Agent分解任务
        val response = baseAgent.generate(decompositionPrompt)
        
        // 解析任务
        val taskDescriptions = response.text
            .trim()
            .lines()
            .filter { it.isNotBlank() }
            .map { it.trim().removePrefix("- ").removePrefix("* ").removePrefix("1. ").removePrefix("2. ").removePrefix("3. ").removePrefix("4. ").removePrefix("5. ") }
        
        // 创建任务
        taskDescriptions.forEach { description ->
            createTask(goal.id, description)
        }
    }

    /**
     * 检查任务完成情况
     */
    private suspend fun checkTasksCompletion(goal: Goal, prompt: String) {
        // 获取目标的未完成任务
        val pendingTasks = getTasksForGoal(goal.id, includeCompleted = false)
        
        // 如果没有未完成任务，目标已完成
        if (pendingTasks.isEmpty()) {
            updateGoalStatus(goal.id, GoalStatus.COMPLETED)
            return
        }
        
        // 检查每个任务是否已完成
        for (task in pendingTasks) {
            val completionCheckPrompt = """
                根据以下用户输入，判断任务是否已经完成：

                任务：${task.description}
                用户输入：$prompt

                请只回答"是"或"否"，不要添加任何其他内容。
            """.trimIndent()
            
            val response = baseAgent.generate(completionCheckPrompt)
            val isCompleted = response.text.trim().equals("是", ignoreCase = true)
            
            if (isCompleted) {
                updateTaskStatus(task.id, TaskStatus.COMPLETED)
            }
        }
    }

    /**
     * 检查目标完成情况
     */
    private fun checkGoalCompletion(goalId: String) {
        // 获取目标的所有任务
        val allTasks = getTasksForGoal(goalId)
        
        // 如果没有任务，不做任何操作
        if (allTasks.isEmpty()) {
            return
        }
        
        // 检查是否所有任务都已完成
        val allCompleted = allTasks.all { it.status == TaskStatus.COMPLETED }
        
        // 如果所有任务都已完成，将目标标记为已完成
        if (allCompleted) {
            updateGoalStatus(goalId, GoalStatus.COMPLETED)
        }
    }
}

/**
 * 目标导向Agent配置
 */
@Serializable
data class GoalOrientedAgentConfig(
    val enableAutoGoalExtraction: Boolean = true,
    val enableAutoTaskCreation: Boolean = true,
    val minPromptLengthForGoal: Int = 20,
    val maxGoalsPerSession: Int = 5,
    val maxTasksPerGoal: Int = 10
)

/**
 * 目标
 */
@Serializable
data class Goal(
    val id: String,
    val description: String,
    val priority: GoalPriority = GoalPriority.MEDIUM,
    val deadline: Instant? = null,
    val createdAt: Instant,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val completedAt: Instant? = null
)

/**
 * 目标优先级
 */
enum class GoalPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * 目标状态
 */
enum class GoalStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELLED
}

/**
 * 任务
 */
@Serializable
data class Task(
    val id: String,
    val goalId: String,
    val description: String,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val deadline: Instant? = null,
    val createdAt: Instant,
    val status: TaskStatus = TaskStatus.PENDING,
    val completedAt: Instant? = null
)

/**
 * 任务优先级
 */
enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * 任务状态
 */
enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

/**
 * 创建目标导向Agent的DSL函数
 */
fun goalOrientedAgent(init: GoalOrientedAgentBuilder.() -> Unit): GoalOrientedAgent {
    val builder = GoalOrientedAgentBuilder()
    builder.init()
    return builder.build()
}

/**
 * 目标导向Agent构建器
 */
class GoalOrientedAgentBuilder {
    private var baseAgent: Agent? = null
    private var config = GoalOrientedAgentConfig()

    /**
     * 设置基础Agent
     */
    fun baseAgent(agent: Agent) {
        this.baseAgent = agent
    }

    /**
     * 配置目标导向Agent
     */
    fun config(init: GoalOrientedAgentConfigBuilder.() -> Unit) {
        val builder = GoalOrientedAgentConfigBuilder(config)
        builder.init()
        this.config = builder.build()
    }

    /**
     * 构建目标导向Agent
     */
    fun build(): GoalOrientedAgent {
        requireNotNull(baseAgent) { "Base agent must be set" }

        return GoalOrientedAgent(
            baseAgent = baseAgent!!,
            config = config
        )
    }
}

/**
 * 目标导向Agent配置构建器
 */
class GoalOrientedAgentConfigBuilder(private val config: GoalOrientedAgentConfig) {
    private var enableAutoGoalExtraction: Boolean = config.enableAutoGoalExtraction
    private var enableAutoTaskCreation: Boolean = config.enableAutoTaskCreation
    private var minPromptLengthForGoal: Int = config.minPromptLengthForGoal
    private var maxGoalsPerSession: Int = config.maxGoalsPerSession
    private var maxTasksPerGoal: Int = config.maxTasksPerGoal
    
    /**
     * 启用/禁用自动目标提取
     */
    fun enableAutoGoalExtraction(enable: Boolean) {
        this.enableAutoGoalExtraction = enable
    }
    
    /**
     * 启用/禁用自动任务创建
     */
    fun enableAutoTaskCreation(enable: Boolean) {
        this.enableAutoTaskCreation = enable
    }
    
    /**
     * 设置最小提示长度（用于目标提取）
     */
    fun minPromptLengthForGoal(length: Int) {
        this.minPromptLengthForGoal = length
    }
    
    /**
     * 设置每个会话的最大目标数
     */
    fun maxGoalsPerSession(max: Int) {
        this.maxGoalsPerSession = max
    }
    
    /**
     * 设置每个目标的最大任务数
     */
    fun maxTasksPerGoal(max: Int) {
        this.maxTasksPerGoal = max
    }
    
    /**
     * 构建配置
     */
    fun build(): GoalOrientedAgentConfig {
        return GoalOrientedAgentConfig(
            enableAutoGoalExtraction = enableAutoGoalExtraction,
            enableAutoTaskCreation = enableAutoTaskCreation,
            minPromptLengthForGoal = minPromptLengthForGoal,
            maxGoalsPerSession = maxGoalsPerSession,
            maxTasksPerGoal = maxTasksPerGoal
        )
    }
}
