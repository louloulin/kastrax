package ai.kastrax.examples.agent

import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.qwen.QwenModel
import ai.kastrax.integrations.qwen.qwen
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect

/**
 * 简单 Qwen 代理示例
 *
 * 这个示例展示了如何创建一个使用 Qwen 模型的简单代理并使用它来生成回答。
 */
fun main() = runBlocking {
    println("简单 Qwen 代理示例")
    println("-------------------")
    
    // 创建一个使用 Qwen 的代理
    val myAgent = agent {
        name = "Qwen 专家助手"
        instructions = """
            你是一个由 Qwen 驱动的专业AI助手，擅长回答各种领域的问题。
            你的回答应该：
            1. 准确、全面
            2. 条理清晰，易于理解
            3. 在适当的情况下提供例子
            4. 避免冗长，直接切入主题
        """.trimIndent()
        
        // 使用 Qwen 模型
        model = qwen {
            model(QwenModel.QWEN2_5_72B_INSTRUCT)
            apiKey(System.getenv("QWEN_API_KEY") ?: "your-api-key-here")
            temperature(0.7)
            maxTokens(2000)
            timeout(60000) // 60秒超时
        }
    }
    
    // 准备一些问题
    val questions = listOf(
        "什么是人工智能？",
        "如何学习编程？",
        "请解释量子计算的基本原理。"
    )
    
    // 使用代理流式回答问题
    questions.forEachIndexed { index, question ->
        println("\n问题 ${index + 1}: $question")
        println("回答: ")
        
        try {
            // 使用流式选项
            val streamOptions = AgentStreamOptions(
                temperature = 0.7,
                maxTokens = 1500
            )
            
            val response = myAgent.stream(question, streamOptions)
            
            // 收集并打印流式响应
            response.textStream?.collect { chunk ->
                print(chunk)
                kotlinx.coroutines.delay(20) // 模拟打字效果
            } ?: run {
                // 如果没有流式响应，打印完整文本
                print(response.text)
            }
            
            println("\n" + "-".repeat(50))
            
        } catch (e: Exception) {
            println("错误: ${e.message}")
            println("-".repeat(50))
        }
    }
    
    println("\n示例完成！")
}