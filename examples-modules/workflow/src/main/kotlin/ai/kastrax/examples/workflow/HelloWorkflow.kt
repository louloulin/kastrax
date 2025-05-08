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
 * 简单的工作流示例
 */
fun main() = runBlocking {
    println("Hello Workflow 示例")
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

    // 创建一个简单的工作流
    val myWorkflow = workflow {
        name = "简单工作流"
        description = "一个简单的工作流示例"

        // 定义工作流步骤
        step(myAgent) {
            id = "greeting"
            name = "问候"
            description = "向用户问好"

            // 定义步骤输入
            variables = mapOf(
                "message" to VariableReference("$.input.message")
            )
        }

        step(myAgent) {
            id = "question"
            name = "提问"
            description = "向用户提问"

            // 定义步骤输入
            variables = mapOf(
                "message" to VariableReference("$.input.message")
            )
        }
    }

    // 执行工作流
    println("\n执行工作流:")
    val result = myWorkflow.execute(emptyMap(), WorkflowExecuteOptions())

    // 打印工作流结果
    println("\n工作流执行结果:")
    result.steps.forEach { (stepId, stepResult) ->
        println("\n步骤: $stepId")
        println("状态: ${stepResult.success}")
        println("输出: ${stepResult.output["text"] ?: "无输出"}")
    }

    println("\nHello Workflow 示例完成")
}