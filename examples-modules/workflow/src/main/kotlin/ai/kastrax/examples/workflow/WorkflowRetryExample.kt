package ai.kastrax.examples

import ai.kastrax.core.agent.agent
import ai.kastrax.core.workflow.RetryConfig
import ai.kastrax.core.workflow.StepConfig
import ai.kastrax.core.workflow.workflow
import ai.kastrax.integrations.openai.openai
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * 工作流重试机制示例
 * 这个示例展示了如何在工作流中使用重试机制。
 */
fun main() = runBlocking {
    println("开始执行WorkflowRetryExample...")
    
    // 创建一个计数器，用于模拟失败和重试
    val attemptCounter = AtomicInteger(0)
    
    // 创建一个会在前两次调用时失败的代理
    val unreliableAgent = agent {
        name = "不稳定代理"
        instructions = "这是一个不稳定的代理，前两次调用会失败。"
        
        model = openai {
            model("gpt-3.5-turbo")
        }
        
        // 重写generate方法，模拟失败和重试
        override suspend fun generate(input: String): ai.kastrax.core.agent.AgentResponse {
            val attempt = attemptCounter.incrementAndGet()
            println("代理调用尝试 #$attempt")
            
            if (attempt <= 2) {
                println("代理调用失败，将重试...")
                throw RuntimeException("模拟的代理失败 #$attempt")
            }
            
            println("代理调用成功！")
            return ai.kastrax.core.agent.AgentResponse(
                content = "这是成功的响应，在第 $attempt 次尝试后。",
                usage = null,
                finishReason = "success"
            )
        }
    }
    
    // 创建工作流
    val retryWorkflow = workflow {
        name = "重试示例工作流"
        description = "演示工作流中的重试机制"
        
        // 添加一个带有重试配置的代理步骤
        agentStep(unreliableAgent) {
            id = "unreliable_step"
            name = "不稳定步骤"
            description = "这个步骤会失败几次，然后成功"
            
            // 配置重试
            config = StepConfig(
                retryConfig = RetryConfig(
                    maxRetries = 3,
                    initialDelay = Duration.ofSeconds(1),
                    backoffFactor = 2.0,
                    jitter = 0.2
                )
            )
        }
        
        // 添加一个依赖于第一个步骤的步骤
        agentStep(agent {
            name = "总结代理"
            instructions = "总结前一个步骤的结果。"
            model = openai { model("gpt-3.5-turbo") }
        }) {
            id = "summary_step"
            name = "总结步骤"
            description = "总结前一个步骤的结果"
            after = listOf("unreliable_step")
            
            // 使用DSL方式配置重试
            retry(maxRetries = 2, initialDelay = Duration.ofMillis(500))
        }
    }
    
    println("执行带有重试机制的工作流...")
    
    // 执行工作流
    val result = retryWorkflow.execute()
    
    // 输出结果
    println("\n工作流执行结果:")
    println("成功: ${result.success}")
    
    if (result.success) {
        println("\n步骤结果:")
        result.steps.forEach { (stepId, stepResult) ->
            println("- $stepId: ${stepResult.success}")
            if (stepResult.success) {
                println("  输出: ${stepResult.output}")
            } else {
                println("  错误: ${stepResult.error}")
            }
        }
    } else {
        println("工作流执行失败: ${result.error}")
    }
    
    println("\n示例结束")
}
