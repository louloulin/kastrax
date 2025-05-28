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
 * 并行工作流示例
 */
class ParallelWorkflowExample {
    fun main() = runBlocking {
    println("并行工作流示例")
    println("=============")

    // 创建代理
    val dataAgent = agent {
        name = "数据分析助手"
        instructions = "你是一个数据分析助手，负责分析和解释数据。"

        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
            temperature(0.3)
            maxTokens(2000)
        }
    }

    val marketingAgent = agent {
        name = "市场营销助手"
        instructions = "你是一个市场营销助手，负责提供市场营销建议和策略。"

        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
            temperature(0.7)
            maxTokens(2000)
        }
    }

    val productAgent = agent {
        name = "产品开发助手"
        instructions = "你是一个产品开发助手，负责提供产品开发和改进建议。"

        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
            temperature(0.7)
            maxTokens(2000)
        }
    }

    val summaryAgent = agent {
        name = "总结助手"
        instructions = "你是一个总结助手，负责整合和总结多个来源的信息。"

        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
            temperature(0.5)
            maxTokens(3000)
        }
    }

    // 创建工作流
    val myWorkflow = workflow {
        name = "并行业务分析工作流"
        description = "并行执行多个分析任务，然后汇总结果"

        // 数据分析步骤
        step(dataAgent) {
            id = "data_analysis"
            name = "数据分析"
            description = "分析产品数据"
            variables = mutableMapOf(
                "product" to VariableReference("$.input.product"),
                "data" to VariableReference("$.input.data")
            )
        }

        // 市场分析步骤 (与数据分析并行)
        step(marketingAgent) {
            id = "market_analysis"
            name = "市场分析"
            description = "分析市场情况"
            variables = mutableMapOf(
                "product" to VariableReference("$.input.product"),
                "market" to VariableReference("$.input.market")
            )
        }

        // 产品分析步骤 (与数据分析和市场分析并行)
        step(productAgent) {
            id = "product_analysis"
            name = "产品分析"
            description = "分析产品特性"
            variables = mutableMapOf(
                "product" to VariableReference("$.input.product"),
                "features" to VariableReference("$.input.features")
            )
        }

        // 总结步骤 (依赖所有前面的步骤)
        step(summaryAgent) {
            id = "summary"
            name = "总结"
            description = "总结所有分析结果"
            after("data_analysis", "market_analysis", "product_analysis")
            variables = mutableMapOf(
                "product" to VariableReference("$.input.product"),
                "data_analysis" to VariableReference("$.steps.data_analysis.output.text"),
                "market_analysis" to VariableReference("$.steps.market_analysis.output.text"),
                "product_analysis" to VariableReference("$.steps.product_analysis.output.text")
            )
        }
    }

    // 执行工作流
    println("\n执行工作流...")
    val result = myWorkflow.execute(
        input = mapOf(
            "product" to "智能手机",
            "data" to "过去一年销售增长了20%，用户满意度为4.2/5，主要用户群体为25-40岁",
            "market" to "市场竞争激烈，主要竞争对手有3家，市场份额分别为30%、25%、15%",
            "features" to "5.5英寸屏幕，8GB内存，128GB存储，4000mAh电池，三摄像头"
        ),
        options = WorkflowExecuteOptions()
    )

    // 打印结果
    println("\n工作流执行结果:")
    result.steps.forEach { (stepId, stepResult) ->
        println("\n步骤: $stepId")
        println("状态: ${stepResult.success}")
        println("输出摘要: ${stepResult.output["text"]?.toString()?.take(100)}...")
    }

    // 打印最终总结
    println("\n最终总结:")
    val summary = result.steps["summary"]?.output?.get("text") as? String
    println(summary)

    println("\n并行工作流示例完成")
    }
}
