package ai.kastrax.examples.agent

import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect

/**
 * 一个简单的HelloAgent示例
 */
fun main() = runBlocking {
    println("Hello Agent 示例")
    println("-------------------")

    // 创建一个使用 Deepseek 的代理
    val myAgent = agent {
        name = "助手"
        instructions = "你是一个有用的助手，可以回答用户的问题。"

        // 使用 Deepseek 模型
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
            temperature(0.7)
            maxTokens(2000)
            timeout(60000) // 60秒超时
        }
    }

    // 使用代理流式生成回答
    println("\n生成回答:")
    
    try {
        val response = myAgent.stream("Hello, 你好！请介绍一下自己。", AgentStreamOptions())

        // 打印流式回答
        println("\n代理回答:")
        response.textStream?.collect { chunk ->
            print(chunk)
            // 添加小延迟模拟打字效果
            kotlinx.coroutines.delay(50)
        } ?: run {
            // 如果没有流式响应，显示完整文本
            print(response.text)
        }
        
        println() // 换行
        
    } catch (e: Exception) {
        println("生成回答时发生错误: ${e.message}")
    }

    println("\nHello Agent 示例完成")
}
