package ai.kastrax.core.agent.architecture

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GoalOrientedAgentTest {

    private lateinit var mockBaseAgent: Agent
    private lateinit var goalOrientedAgent: GoalOrientedAgent

    @BeforeEach
    fun setup() {
        mockBaseAgent = mockk<Agent>()

        // 设置基础Agent的行为
        coEvery { mockBaseAgent.name } returns "TestAgent"
        coEvery { mockBaseAgent.versionManager } returns null

        // 模拟目标提取响应
        coEvery {
            mockBaseAgent.generate(match { it.contains("提取主要目标") }, any())
        } returns AgentResponse(text = "学习Kotlin编程")

        // 模拟任务分解响应
        coEvery {
            mockBaseAgent.generate(match { it.contains("分解为3-5个具体的任务") }, any())
        } returns AgentResponse(
            text = """
                1. 了解Kotlin基础语法
                2. 学习Kotlin面向对象编程
                3. 掌握Kotlin协程
                4. 实践Kotlin项目开发
            """.trimIndent()
        )

        // 模拟任务完成检查响应
        coEvery {
            mockBaseAgent.generate(match { it.contains("任务是否已经完成") }, any())
        } returns AgentResponse(text = "是")

        // 模拟常规响应
        coEvery {
            mockBaseAgent.generate(any<String>(), any())
        } returns AgentResponse(text = "Mock response")

        // 使用DSL创建GoalOrientedAgent
        goalOrientedAgent = goalOrientedAgent {
            baseAgent(mockBaseAgent)
            config {
                minPromptLengthForGoal(10)
                enableAutoTaskCreation(true)
            }
        }
    }

    @Test
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
        assertEquals("Mock response", response.text)

        // 验证目标创建
        val goals = goalOrientedAgent.getAllGoals()
        assertTrue(goals.isNotEmpty())

        // 验证任务创建
        val tasks = goalOrientedAgent.getTasksForGoal(goals.first().id)
        assertTrue(tasks.isNotEmpty())
    }
}
