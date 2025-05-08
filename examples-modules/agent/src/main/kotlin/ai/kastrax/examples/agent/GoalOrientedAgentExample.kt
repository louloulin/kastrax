package ai.kastrax.examples.agent

import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.architecture.goalOrientedAgent
import ai.kastrax.core.agent.architecture.GoalPriority
import ai.kastrax.core.agent.architecture.TaskPriority
import ai.kastrax.core.agent.architecture.TaskStatus
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.integrations.deepseek.DeepSeekModel
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

/**
 * 目标导向Agent示例
 */
fun main() = runBlocking {
    // 创建基础LLM
    val llm = deepSeek {
        // 直接设置 API 密钥
        apiKey("sk-85e83081df28490b9ae63188f0cb4f79")

        // 设置模型
        model(DeepSeekModel.DEEPSEEK_CHAT)

        // 设置生成参数
        temperature(0.7)
        maxTokens(2000)
        topP(0.95)
    }

    // 使用DSL创建基础Agent
    val baseAgent = agent {
        name = "基础Agent"
        model = llm
    }

    // 使用DSL创建目标导向Agent
    val goalOrientedAgent = goalOrientedAgent {
        baseAgent(baseAgent)
        config {
            minPromptLengthForGoal(20)
            enableAutoTaskCreation(true)
            enableAutoGoalExtraction(true)
        }
    }

    // 方法1：手动创建目标和任务
    println("=== 手动创建目标和任务 ===")

    // 创建目标
    val goal = goalOrientedAgent.createGoal(
        description = "学习Kotlin协程",
        priority = GoalPriority.HIGH,
        deadline = Clock.System.now().plus(7, DateTimeUnit.DAY)
    )

    println("创建目标: ${goal.description}, 优先级: ${goal.priority}")

    // 创建任务
    val task1 = goalOrientedAgent.createTask(
        goalId = goal.id,
        description = "了解协程的基本概念",
        priority = TaskPriority.HIGH
    )

    val task2 = goalOrientedAgent.createTask(
        goalId = goal.id,
        description = "学习协程的上下文和作用域",
        priority = TaskPriority.MEDIUM
    )

    val task3 = goalOrientedAgent.createTask(
        goalId = goal.id,
        description = "实践协程的异常处理",
        priority = TaskPriority.MEDIUM
    )

    println("为目标创建了 ${goalOrientedAgent.getTasksForGoal(goal.id).size} 个任务")

    // 更新任务状态
    goalOrientedAgent.updateTaskStatus(task1!!.id, TaskStatus.COMPLETED)
    println("完成任务: ${task1.description}")

    // 方法2：使用自动目标提取和任务分解
    println("\n=== 自动目标提取和任务分解 ===")

    val sessionId = "session-123"
    val prompt = "我想学习Kotlin的协程，请帮我制定一个学习计划，包括基础概念、实际应用和最佳实践。"

    val options = AgentGenerateOptions()
    val optionsWithMetadata = options.copy(metadata = mapOf("sessionId" to sessionId))
    val response = goalOrientedAgent.generate(
        prompt = prompt,
        options = optionsWithMetadata
    )

    println("目标导向Agent响应:")
    println(response.text)

    // 查看自动创建的目标和任务
    val goals = goalOrientedAgent.getAllGoals()
    val autoGoal = goals.lastOrNull { it.id != goal.id }

    if (autoGoal != null) {
        println("\n自动创建的目标: ${autoGoal.description}")

        val autoTasks = goalOrientedAgent.getTasksForGoal(autoGoal.id)
        println("自动创建的任务:")
        autoTasks.forEach { task ->
            println("- ${task.description} (${task.status})")
        }
    }
}
