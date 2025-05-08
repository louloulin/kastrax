package ai.kastrax.examples.workflow

import ai.kastrax.core.agent.agent
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.WorkflowExecuteOptions
import ai.kastrax.core.workflow.builder.step
import ai.kastrax.core.workflow.workflow
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking

/**
 * 简单工作流示例
 */
fun main() = runBlocking {
    println("简单工作流示例")
    println("=============")

    // 创建两个代理
    val researchAgent = agent {
        name = "研究助手"
        instructions = "你是一个研究助手，负责收集和整理信息。"

        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
            temperature(0.7)
            maxTokens(2000)
        }
    }

    val writingAgent = agent {
        name = "写作助手"
        instructions = "你是一个写作助手，负责根据提供的信息撰写内容。"

        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
            temperature(0.7)
            maxTokens(2000)
        }
    }

    // 创建工作流
    val myWorkflow = workflow {
        name = "简单内容创建工作流"
        description = "一个简单的工作流，用于研究主题并创建内容"

        // 研究步骤
        step(researchAgent) {
            id = "research"
            name = "研究"
            description = "研究指定主题"
            variables = mutableMapOf(
                "topic" to VariableReference("$.input.topic")
            )
        }

        // 写作步骤
        step(writingAgent) {
            id = "writing"
            name = "写作"
            description = "根据研究结果撰写内容"
            after("research")
            variables = mutableMapOf(
                "research" to VariableReference("$.steps.research.output.text")
            )
        }
    }

    // 执行工作流
    println("\n执行工作流...")
    val result = myWorkflow.execute(
        input = mapOf(
            "topic" to "人工智能在医疗领域的应用"
        ),
        options = WorkflowExecuteOptions()
    )

    // 打印结果
    println("\n工作流执行结果:")
    result.steps.forEach { (stepId, stepResult) ->
        println("\n步骤: $stepId")
        println("状态: ${stepResult.success}")
        println("输出: ${stepResult.output["text"] ?: "无输出"}")
    }

    println("\n简单工作流示例完成")
}
