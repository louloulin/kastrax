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
 * 并行工作流示例
 *
 * 这个示例展示了如何创建包含并行步骤的工作流。
 */
fun main() = runBlocking {
    println("并行工作流示例")
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

    // 创建一个并行工作流
    val parallelWorkflow = workflow {
        name = "并行工作流"
        description = "一个包含并行步骤的工作流示例"

        // 初始步骤 - 使用自定义步骤
        step(object : WorkflowStep {
            override val id: String = "input"
            override val name: String = "输入"
            override val description: String = "设置初始输入"
            override val after: List<String> = emptyList()
            override val variables: Map<String, VariableReference> = emptyMap()

            override suspend fun execute(context: ai.kastrax.core.workflow.WorkflowContext): ai.kastrax.core.workflow.WorkflowStepResult {
                val topic = "人工智能"
                println("设置主题: $topic")

                return ai.kastrax.core.workflow.WorkflowStepResult(
                    stepId = id,
                    success = true,
                    output = mapOf("topic" to topic),
                    executionTime = 0
                )
            }
        })

        // 并行步骤1：历史研究
        step(myAgent) {
            id = "history_research"
            name = "历史研究"
            description = "研究主题的历史"
            after = mutableListOf("input")

            variables = mapOf(
                "message" to VariableReference("$.steps.input.output.topic")
            )
        }

        // 并行步骤2：现状分析
        step(myAgent) {
            id = "current_state"
            name = "现状分析"
            description = "分析主题的当前状态"
            after = mutableListOf("input")

            variables = mapOf(
                "message" to VariableReference("$.steps.input.output.topic")
            )
        }

        // 并行步骤3：未来展望
        step(myAgent) {
            id = "future_trends"
            name = "未来展望"
            description = "预测主题的未来趋势"
            after = mutableListOf("input")

            variables = mapOf(
                "message" to VariableReference("$.steps.input.output.topic")
            )
        }

        // 最终步骤：综合报告
        step(myAgent) {
            id = "comprehensive_report"
            name = "综合报告"
            description = "生成综合报告"
            after = mutableListOf("history_research", "current_state", "future_trends")

            variables = mapOf(
                "topic" to VariableReference("$.steps.input.output.topic"),
                "history" to VariableReference("$.steps.history_research.output.text"),
                "currentState" to VariableReference("$.steps.current_state.output.text"),
                "futureTrends" to VariableReference("$.steps.future_trends.output.text"),
                "message" to VariableReference("$.steps.input.output.topic")
            )
        }
    }

    // 执行工作流
    println("\n执行并行工作流:")
    val result = parallelWorkflow.execute(emptyMap(), WorkflowExecuteOptions())

    // 打印工作流结果
    println("\n工作流执行结果:")

    // 打印并行步骤结果
    val parallelSteps = listOf("history_research", "current_state", "future_trends")
    parallelSteps.forEach { stepId ->
        val stepResult = result.steps[stepId]
        if (stepResult != null && stepResult.success) {
            println("\n$stepId 结果:")
            println("----------")
            println(stepResult.output["text"] ?: "无输出")
        }
    }

    // 打印最终报告
    val finalReport = result.steps["comprehensive_report"]
    if (finalReport != null && finalReport.success) {
        println("\n最终综合报告:")
        println("----------")
        println(finalReport.output["text"] ?: "无输出")
    }

    println("\n并行工作流示例完成")
}
