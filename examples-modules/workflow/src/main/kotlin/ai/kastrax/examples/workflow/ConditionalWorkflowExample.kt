package ai.kastrax.examples.workflow

import ai.kastrax.core.agent.agent
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowExecuteOptions
import ai.kastrax.core.workflow.builder.step
import ai.kastrax.core.workflow.workflow
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking

/**
 * 条件工作流示例
 */
class ConditionalWorkflowExample {
    fun main() = runBlocking {
    println("条件工作流示例")
    println("=============")

    // 创建代理
    val classifierAgent = agent {
        name = "分类助手"
        instructions = "你是一个分类助手，负责对输入的文本进行分类。"

        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
            temperature(0.3)
            maxTokens(1000)
        }
    }

    val technicalAgent = agent {
        name = "技术内容助手"
        instructions = "你是一个技术内容助手，负责解释技术概念和提供技术解决方案。"

        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
            temperature(0.7)
            maxTokens(2000)
        }
    }

    val generalAgent = agent {
        name = "通用内容助手"
        instructions = "你是一个通用内容助手，负责回答各种非技术性问题。"

        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
            temperature(0.7)
            maxTokens(2000)
        }
    }

    // 创建工作流
    val myWorkflow = workflow {
        name = "条件内容创建工作流"
        description = "根据查询类型选择不同的处理路径"

        // 分类步骤
        step(classifierAgent) {
            id = "classify"
            name = "分类"
            description = "对查询进行分类"
            variables = mutableMapOf(
                "query" to VariableReference("$.input.query")
            )
        }

        // 技术处理步骤
        step(technicalAgent) {
            id = "technical_processing"
            name = "技术处理"
            description = "处理技术类查询"
            after("classify")
            variables = mutableMapOf(
                "query" to VariableReference("$.input.query"),
                "classification" to VariableReference("$.steps.classify.output.text")
            )

            // 条件：只有当分类结果包含"技术"或"编程"时才执行
            condition = { context: WorkflowContext ->
                val classification = context.steps["classify"]?.output?.get("text") as? String ?: ""
                classification.contains("技术", ignoreCase = true) ||
                classification.contains("编程", ignoreCase = true) ||
                classification.contains("代码", ignoreCase = true)
            }
        }

        // 通用处理步骤
        step(generalAgent) {
            id = "general_processing"
            name = "通用处理"
            description = "处理非技术类查询"
            after("classify")
            variables = mutableMapOf(
                "query" to VariableReference("$.input.query"),
                "classification" to VariableReference("$.steps.classify.output.text")
            )

            // 条件：只有当分类结果不包含"技术"和"编程"时才执行
            condition = { context: WorkflowContext ->
                val classification = context.steps["classify"]?.output?.get("text") as? String ?: ""
                !classification.contains("技术", ignoreCase = true) &&
                !classification.contains("编程", ignoreCase = true) &&
                !classification.contains("代码", ignoreCase = true)
            }
        }
    }

    // 执行工作流 - 技术查询
    println("\n执行工作流 (技术查询)...")
    val techResult = myWorkflow.execute(
        input = mapOf(
            "query" to "如何使用Python实现快速排序算法？"
        ),
        options = WorkflowExecuteOptions()
    )

    // 打印结果
    println("\n技术查询工作流执行结果:")
    techResult.steps.forEach { (stepId, stepResult) ->
        if (stepResult.success) {
            println("\n步骤: $stepId")
            println("状态: ${stepResult.success}")
            println("输出: ${stepResult.output["text"] ?: "无输出"}")
        }
    }

    // 执行工作流 - 非技术查询
    println("\n执行工作流 (非技术查询)...")
    val generalResult = myWorkflow.execute(
        input = mapOf(
            "query" to "推荐几本世界名著？"
        ),
        options = WorkflowExecuteOptions()
    )

    // 打印结果
    println("\n非技术查询工作流执行结果:")
    generalResult.steps.forEach { (stepId, stepResult) ->
        if (stepResult.success) {
            println("\n步骤: $stepId")
            println("状态: ${stepResult.success}")
            println("输出: ${stepResult.output["text"] ?: "无输出"}")
        }
    }

    println("\n条件工作流示例完成")
    }
}
