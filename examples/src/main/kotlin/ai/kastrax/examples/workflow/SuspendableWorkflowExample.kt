package ai.kastrax.examples.workflow

import ai.kastrax.core.agent.agent
import ai.kastrax.core.workflow.StepConfig
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.SimpleWorkflow
import ai.kastrax.core.workflow.engine.WorkflowEngine
import ai.kastrax.core.workflow.state.InMemoryWorkflowStateStorage
import ai.kastrax.core.workflow.state.StepStateStatus
import ai.kastrax.core.workflow.state.WorkflowStateStatus
import ai.kastrax.core.workflow.suspend.AbstractSuspendableStep
import ai.kastrax.core.workflow.suspend.SuspendController
import ai.kastrax.integrations.openai.openAi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 可暂停工作流示例。
 */
fun main() = runBlocking {
    println("开始可暂停工作流示例...")

    // 创建OpenAI客户端
    val openAi = openAi {
        apiKey = System.getenv("OPENAI_API_KEY") ?: ""
    }

    // 创建Agent
    val agent = agent(openAi) {
        model = "gpt-3.5-turbo"
    }

    // 创建初始步骤
    val initStep = object : WorkflowStep {
        override val id: String = "init"
        override val name: String = "Initial Step"
        override val description: String = "Initial step"
        override val after: List<String> = emptyList()
        override val variables: Map<String, VariableReference> = emptyMap()
        override val config: StepConfig? = null

        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行初始步骤...")

            return WorkflowStepResult(
                stepId = id,
                success = true,
                output = mapOf("message" to "初始步骤完成"),
                executionTime = 100
            )
        }
    }

    // 创建可暂停步骤
    val userInputStep = object : AbstractSuspendableStep(
        id = "userInput",
        name = "User Input Step",
        description = "Step that waits for user input",
        after = listOf("init")
    ) {
        override suspend fun execute(context: WorkflowContext, suspendController: SuspendController): WorkflowStepResult {
            println("执行用户输入步骤...")
            println("等待用户输入...")

            // 暂停工作流，等待用户输入
            suspendController.suspend(buildJsonObject {
                put("message", "请提供您的姓名")
            })

            // 这里的代码不会执行，因为工作流已经暂停
            return WorkflowStepResult(
                stepId = id,
                success = true,
                output = emptyMap(),
                executionTime = 0
            )
        }
    }

    // 创建最终步骤
    val finalStep = object : WorkflowStep {
        override val id: String = "final"
        override val name: String = "Final Step"
        override val description: String = "Final step"
        override val after: List<String> = listOf("userInput")
        override val variables: Map<String, VariableReference> = emptyMap()
        override val config: StepConfig? = null

        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行最终步骤...")

            // 获取用户输入
            val userName = context.variables["userName"] ?: "未知用户"

            val message = "你好，$userName！感谢您的参与。"
            println(message)

            return WorkflowStepResult(
                stepId = id,
                success = true,
                output = mapOf("message" to message),
                executionTime = 100
            )
        }
    }

    // 创建工作流
    val steps = mapOf(
        initStep.id to initStep,
        userInputStep.id to userInputStep,
        finalStep.id to finalStep
    )

    val suspendableWorkflow = SimpleWorkflow(
        workflowName = "SuspendableWorkflow",
        description = "Workflow with suspendable steps",
        steps = steps
    )

    // 创建工作流引擎
    val stateStorage = InMemoryWorkflowStateStorage()
    val workflowEngine = WorkflowEngine(
        workflows = mapOf("SuspendableWorkflow" to suspendableWorkflow),
        stateStorage = stateStorage
    )

    // 执行工作流
    println("\n开始执行工作流...")
    val result = workflowEngine.executeWorkflow(
        workflowId = "SuspendableWorkflow",
        input = emptyMap()
    )

    // 检查工作流是否暂停
    if (result.steps.any { (_, stepResult) -> stepResult.suspendPayload != null }) {
        println("\n工作流已暂停，等待用户输入...")

        // 获取暂停的步骤ID和暂停信息
        val suspendedStepEntry = result.steps.entries.find { (_, stepResult) ->
            stepResult.suspendPayload != null
        }

        val suspendedStepId = suspendedStepEntry?.key
        val suspendPayload = suspendedStepEntry?.value?.suspendPayload

        if (suspendedStepId != null) {
            println("暂停的步骤: $suspendedStepId")
            println("暂停信息: $suspendPayload")

            // 获取工作流状态
            val state = stateStorage.getWorkflowState("SuspendableWorkflow", result.runId!!)

            if (state != null) {
                println("\n工作流状态:")
                println("状态: ${state.status}")
                println("暂停的步骤: ${state.suspendedSteps}")

                // 模拟用户输入
                println("\n模拟用户输入: 张三")

                // 恢复工作流
                println("\n恢复工作流...")
                val resumeResult = workflowEngine.resumeWorkflow(
                    workflowId = "SuspendableWorkflow",
                    runId = state.runId,
                    stepId = suspendedStepId,
                    input = mapOf("userName" to "张三")
                )

                // 检查恢复后的结果
                println("\n工作流恢复执行结果:")
                println("成功: ${resumeResult.success}")
                println("输出: ${resumeResult.output}")
                println("步骤: ${resumeResult.steps.keys}")

                // 获取更新后的工作流状态
                val updatedState = stateStorage.getWorkflowState("SuspendableWorkflow", state.runId)

                if (updatedState != null) {
                    println("\n更新后的工作流状态:")
                    println("状态: ${updatedState.status}")
                    println("步骤状态:")
                    updatedState.steps.forEach { (stepId, stepState) ->
                        println("  $stepId: ${stepState.status}")
                    }
                }
            }
        }
    } else {
        println("\n工作流执行完成，没有暂停步骤")
        println("结果: ${result.output}")
    }
}
