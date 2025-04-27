package ai.kastrax.examples.workflow

import ai.kastrax.core.agent.agent
import ai.kastrax.core.workflow.*
import ai.kastrax.core.workflow.builder.workflow
import ai.kastrax.core.workflow.engine.WorkflowEngine
import ai.kastrax.core.workflow.state.InMemoryWorkflowStateStorage
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 增强工作流示例，展示条件分支、并行执行和循环功能。
 */
fun main() = runBlocking {
    println("开始增强工作流示例...")

    // 创建Deepseek客户端
    val deepseekLlm = deepSeek {
        model(DeepSeekModel.DEEPSEEK_CHAT)
        apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "test-api-key")
        temperature(0.7)
        maxTokens(2000)
        timeout(60000) // 60秒超时
    }

    // 创建Agent
    val agent = agent {
        name = "DeepseekAgent"
        model = deepseekLlm
    }

    // 创建初始步骤
    val initStep = object : WorkflowStep {
        override val id: String = "init"
        override val name: String = "Initial Step"
        override val description: String = "Initial step"
        override val after: List<String> = emptyList()
        override val variables: Map<String, VariableReference> = emptyMap()

        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行初始步骤...")

            // 获取输入值，如果没有则使用默认值15
            val inputValue = context.input["value"]?.toString()?.toIntOrNull() ?: 15
            println("输入值: $inputValue")

            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf("value" to inputValue)
            )
        }
    }

    // 创建高值处理步骤
    val highValueStep = object : WorkflowStep {
        override val id: String = "highValue"
        override val name: String = "High Value"
        override val description: String = "High value branch"
        override val after: List<String> = emptyList()
        override val variables: Map<String, VariableReference> = emptyMap()

        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            val value = context.getStepOutput("init")?.get("value")?.toString()?.toIntOrNull() ?: 0
            println("执行高值处理步骤，值为: $value")

            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf(
                    "message" to "高值处理: $value",
                    "category" to "high"
                )
            )
        }
    }

    // 创建低值处理步骤
    val lowValueStep = object : WorkflowStep {
        override val id: String = "lowValue"
        override val name: String = "Low Value"
        override val description: String = "Low value branch"
        override val after: List<String> = emptyList()
        override val variables: Map<String, VariableReference> = emptyMap()

        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            val value = context.getStepOutput("init")?.get("value")?.toString()?.toIntOrNull() ?: 0
            println("执行低值处理步骤，值为: $value")

            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf(
                    "message" to "低值处理: $value",
                    "category" to "low"
                )
            )
        }
    }

    // 创建分析步骤1
    val analyzeStep1 = object : WorkflowStep {
        override val id: String = "analyze1"
        override val name: String = "Analyze 1"
        override val description: String = "First analysis step"
        override val after: List<String> = emptyList()
        override val variables: Map<String, VariableReference> = emptyMap()

        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行分析步骤1...")

            // 获取条件步骤的结果
            val conditionalStepId = context.steps.keys.find { it.startsWith("if-") }
            val category = context.getStepOutput(conditionalStepId ?: "")?.get("category")?.toString() ?: "unknown"

            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf("analysis1" to "分析1结果: 类别=$category")
            )
        }
    }

    // 创建分析步骤2
    val analyzeStep2 = object : WorkflowStep {
        override val id: String = "analyze2"
        override val name: String = "Analyze 2"
        override val description: String = "Second analysis step"
        override val after: List<String> = emptyList()
        override val variables: Map<String, VariableReference> = emptyMap()

        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行分析步骤2...")

            // 获取初始步骤的值
            val value = context.getStepOutput("init")?.get("value")?.toString()?.toIntOrNull() ?: 0

            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf("analysis2" to "分析2结果: 值=$value")
            )
        }
    }

    // 创建循环体步骤
    val loopBodyStep = object : WorkflowStep {
        override val id: String = "loopBody"
        override val name: String = "Loop Body"
        override val description: String = "Loop body step"
        override val after: List<String> = emptyList()
        override val variables: Map<String, VariableReference> = emptyMap()

        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            val iteration = context.variables["iteration"] as? Int ?: 0
            println("执行循环步骤，迭代次数: $iteration")

            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf(
                    "iteration" to iteration,
                    "message" to "迭代 $iteration 的结果"
                )
            )
        }
    }

    // 创建最终步骤
    val finalStep = object : WorkflowStep {
        override val id: String = "final"
        override val name: String = "Final Step"
        override val description: String = "Final step"
        override val after: List<String> = emptyList()
        override val variables: Map<String, VariableReference> = emptyMap()

        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行最终步骤...")

            // 获取并行步骤的结果
            val parallelStepId = context.steps.keys.find { it.startsWith("parallel-") }
            val analysis1 = context.getStepOutput(parallelStepId ?: "")?.get("analysis1")?.toString() ?: ""
            val analysis2 = context.getStepOutput(parallelStepId ?: "")?.get("analysis2")?.toString() ?: ""

            // 获取循环步骤的结果
            val loopStepId = context.steps.keys.find { it.startsWith("loop-") }
            val iterations = context.getStepOutput(loopStepId ?: "")?.get("iterations")?.toString()?.toIntOrNull() ?: 0

            // 创建最终输出
            val summary = """
                |处理完成!
                |
                |分析结果:
                |$analysis1
                |$analysis2
                |
                |循环执行了 $iterations 次
            """.trimMargin()

            println("\n最终结果:")
            println(summary)

            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf("summary" to summary)
            )
        }
    }

    // 创建工作流
    val enhancedWorkflow = workflow("EnhancedWorkflow", "增强工作流示例") {
        // 添加初始步骤
        step(initStep)

        // 添加条件分支
        ifThen(
            condition = { context ->
                val value = context.getStepOutput("init")?.get("value")?.toString()?.toIntOrNull() ?: 0
                value > 10
            },
            thenStep = highValueStep,
            elseStep = lowValueStep
        )

        // 添加并行步骤
        parallel(analyzeStep1, analyzeStep2)

        // 添加循环步骤
        loop(
            condition = { _, iteration -> iteration < 3 }, // 执行3次循环
            body = loopBodyStep
        )

        // 添加最终步骤
        step(finalStep)
    }

    // 创建工作流引擎
    val workflowEngine = WorkflowEngine(
        workflows = mapOf("EnhancedWorkflow" to enhancedWorkflow),
        stateStorage = InMemoryWorkflowStateStorage()
    )

    // 执行工作流
    println("\n开始执行工作流...")
    val result = workflowEngine.executeWorkflow(
        workflowId = "EnhancedWorkflow",
        input = mapOf("value" to 15) // 使用高值
    )

    // 检查工作流执行结果
    if (result.success) {
        println("\n工作流执行成功!")
        println("执行时间: ${result.executionTime}ms")
        println("步骤数: ${result.steps.size}")
    } else {
        println("\n工作流执行失败!")
        println("错误: ${result.error}")
    }

    // 使用低值再次执行工作流
    println("\n使用低值再次执行工作流...")
    val result2 = workflowEngine.executeWorkflow(
        workflowId = "EnhancedWorkflow",
        input = mapOf("value" to 5) // 使用低值
    )

    // 检查工作流执行结果
    if (result2.success) {
        println("\n工作流执行成功!")
        println("执行时间: ${result2.executionTime}ms")
        println("步骤数: ${result2.steps.size}")
    } else {
        println("\n工作流执行失败!")
        println("错误: ${result2.error}")
    }
}
