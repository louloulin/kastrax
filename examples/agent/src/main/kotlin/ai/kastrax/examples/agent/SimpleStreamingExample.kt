package ai.kastrax.examples.agent

import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.integrations.deepseek.DeepSeekModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect

/**
 * 简单流式处理示例
 *
 * 演示如何使用 Agent 的流式响应功能，实现实时文本生成效果。
 */
fun main() = runBlocking {
    println("=== 简单流式处理示例 ===")
    println("演示实时文本生成\n")
    
    // 创建支持流式处理的 Agent
    val agent = agent {
        name = "流式助手"
        instructions = """
            你是一个智能助手，请提供清晰、有条理的回答。
            回答要详细但不冗长，语言要流畅自然。
        """.trimIndent()
        
        model = deepSeek {
            apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
            model(DeepSeekModel.DEEPSEEK_CHAT)
            temperature(0.7)
            maxTokens(1000)
        }
    }
    
    // 测试问题
    val question = "请解释什么是机器学习，并举一个简单的例子。"
    
    println("问题: $question\n")
    println("回答: ")
    
    try {
        // 使用流式生成
        val response = agent.stream(question, AgentStreamOptions())
        
        // 收集并显示流式文本
        response.textStream?.collect { chunk ->
            print(chunk)
            // 添加小延迟模拟打字效果
            kotlinx.coroutines.delay(50)
        } ?: run {
            // 如果没有流式响应，显示完整文本
            print(response.text)
        }
        
        println("\n\n=== 流式处理完成 ===")
        
    } catch (e: Exception) {
        println("\n错误: ${e.message}")
    }
}