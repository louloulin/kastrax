package ai.kastrax.examples.workflow

import ai.kastrax.core.agent.agent
import ai.kastrax.core.workflow.WorkflowExecuteOptions
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.builder.step
import ai.kastrax.core.workflow.workflow
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

/**
 * 条件工作流示例
 *
 * 这个示例展示了如何使用条件分支创建工作流。
 */
fun main() = runBlocking {
    println("条件工作流示例")
    println("-------------------")

    // 创建一个使用 Deepseek 的代理
    val myAgent = agent {
        name = "助手"
        instructions = "你是一个有用的助手，可以回答用户的问题。"

        // 使用 Deepseek 模型
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
            temperature(0.7)
            maxTokens(2000)
            timeout(60000) // 60秒超时
        }
    }

    // 创建一个条件工作流
    val conditionalWorkflow = workflow {
        name = "条件工作流"
        description = "一个包含条件分支的工作流示例"

        // 初始步骤 - 使用自定义步骤
        step(object : WorkflowStep {
            override val id: String = "input"
            override val name: String = "输入"
            override val description: String = "获取用户输入"
            override val after: List<String> = emptyList()
            override val variables: Map<String, VariableReference> = emptyMap()

            override suspend fun execute(context: ai.kastrax.core.workflow.WorkflowContext): ai.kastrax.core.workflow.WorkflowStepResult {
                // 返回一个随机数，用于后续条件分支
                val randomValue = (1..10).random()
                println("生成随机值: $randomValue")

                return ai.kastrax.core.workflow.WorkflowStepResult(
                    stepId = id,
                    success = true,
                    output = mapOf("value" to randomValue),
                    executionTime = 0
                )
            }
        })

        // 条件分支：高值路径
        step(myAgent) {
            id = "high_value"
            name = "高值处理"
            description = "处理高值输入"

            // 只有当输入值大于5时才执行此步骤
            condition = { context ->
                val value = context.steps["input"]?.output?.get("value") as? Int ?: 0
                value > 5
            }

            variables = mapOf(
                "message" to VariableReference("$.steps.input.output.value")
            )
        }

        // 条件分支：低值路径
        step(myAgent) {
            id = "low_value"
            name = "低值处理"
            description = "处理低值输入"

            // 只有当输入值小于或等于5时才执行此步骤
            condition = { context ->
                val value = context.steps["input"]?.output?.get("value") as? Int ?: 0
                value <= 5
            }

            variables = mapOf(
                "message" to VariableReference("$.steps.input.output.value")
            )
        }

        // 最终步骤：汇总结果
        step(myAgent) {
            id = "summary"
            name = "结果汇总"
            description = "汇总处理结果"

            // 在任一分支执行完成后执行
            after = mutableListOf("high_value", "low_value")

            variables = mapOf(
                "highValueResult" to VariableReference("$.steps.high_value.output.text"),
                "lowValueResult" to VariableReference("$.steps.low_value.output.text"),
                "value" to VariableReference("$.steps.input.output.value"),
                "message" to VariableReference("$.steps.input.output.value")
            )
        }
    }

    // 执行工作流
    println("\n执行条件工作流:")
    val result = conditionalWorkflow.execute(emptyMap(), WorkflowExecuteOptions())

    // 打印工作流结果
    println("\n工作流执行结果:")
    result.steps.forEach { (stepId, stepResult) ->
        if (stepResult.success) {
            println("\n步骤: $stepId")
            println("状态: ${stepResult.success}")
            println("输出: ${stepResult.output["text"] ?: stepResult.output["value"] ?: "无输出"}")
        }
    }

    println("\n条件工作流示例完成")
}
