package ai.kastrax.core.agent.architecture

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GoalOrientedAgentTest {

    private lateinit var baseAgent: Agent
    private lateinit var goalOrientedAgent: GoalOrientedAgent

    @BeforeEach
    fun setup() {
        // 创建基础Agent，使用Deepseek LLM Provider
        baseAgent = agent {
            name = "TestAgent"
            instructions = "你是一个有帮助的助手，可以回答问题和执行计算。"

            // 使用Deepseek模型
            model = deepSeek {
                model(DeepSeekModel.DEEPSEEK_CHAT)
                apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "test-api-key")
                temperature(0.7)
                maxTokens(2000)
                timeout(60000) // 60秒超时
            }
        }

        // 使用DSL创建GoalOrientedAgent
        goalOrientedAgent = goalOrientedAgent {
            baseAgent(baseAgent)
            config {
                minPromptLengthForGoal(10)
                enableAutoTaskCreation(true)
            }
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".*")
    fun `test create goal`() = runBlocking {
        // 创建目标
        val goal = goalOrientedAgent.createGoal(
            description = "学习Kotlin编程",
            priority = GoalPriority.HIGH
        )

        // 验证结果
        assertNotNull(goal)
        assertEquals("学习Kotlin编程", goal.description)
        assertEquals(GoalPriority.HIGH, goal.priority)
        assertEquals(GoalStatus.ACTIVE, goal.status)
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".*")
    fun `test create and update task`() = runBlocking {
        // 创建目标
        val goal = goalOrientedAgent.createGoal(
            description = "学习Kotlin编程"
        )

        // 创建任务
        val task = goalOrientedAgent.createTask(
            goalId = goal.id,
            description = "了解Kotlin基础语法",
            priority = TaskPriority.HIGH
        )

        // 验证任务创建
        assertNotNull(task)
        assertEquals("了解Kotlin基础语法", task.description)
        assertEquals(TaskPriority.HIGH, task.priority)
        assertEquals(TaskStatus.PENDING, task.status)

        // 更新任务状态
        val updatedTask = goalOrientedAgent.updateTaskStatus(
            taskId = task!!.id,
            status = TaskStatus.COMPLETED
        )

        // 验证任务更新
        assertNotNull(updatedTask)
        assertEquals(TaskStatus.COMPLETED, updatedTask!!.status)
        assertNotNull(updatedTask.completedAt)
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".*")
    fun `test goal completion when all tasks completed`() = runBlocking {
        // 创建目标
        val goal = goalOrientedAgent.createGoal(
            description = "学习Kotlin编程"
        )

        // 创建任务
        val task1 = goalOrientedAgent.createTask(
            goalId = goal.id,
            description = "了解Kotlin基础语法"
        )

        val task2 = goalOrientedAgent.createTask(
            goalId = goal.id,
            description = "学习Kotlin面向对象编程"
        )

        // 完成所有任务
        goalOrientedAgent.updateTaskStatus(task1!!.id, TaskStatus.COMPLETED)
        goalOrientedAgent.updateTaskStatus(task2!!.id, TaskStatus.COMPLETED)

        // 获取更新后的目标
        val updatedGoal = goalOrientedAgent.getGoal(goal.id)

        // 验证目标状态
        assertNotNull(updatedGoal)
        assertEquals(GoalStatus.COMPLETED, updatedGoal!!.status)
        assertNotNull(updatedGoal.completedAt)
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".*")
    fun `test generate with goal-oriented prompt`() = runBlocking {
        // 准备测试数据
        val prompt = "我想学习Kotlin编程，请帮我制定一个学习计划"
        val options = AgentGenerateOptions()
        val metadata = mapOf("sessionId" to "test-session")
        val optionsWithMetadata = options.copy(metadata = metadata)

        // 执行测试
        val response = goalOrientedAgent.generate(prompt, optionsWithMetadata)

        // 验证结果
        assertNotNull(response)
        assertNotNull(response.text)
        assertTrue(response.text.isNotEmpty())

        // 验证目标创建
        val goals = goalOrientedAgent.getAllGoals()
        assertTrue(goals.isNotEmpty())

        // 验证任务创建
        val tasks = goalOrientedAgent.getTasksForGoal(goals.first().id)
        assertTrue(tasks.isNotEmpty())
    }
}
