package ai.kastrax.examples.agent

import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking

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
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
            temperature(0.7)
            maxTokens(2000)
            timeout(60000) // 60秒超时
        }
    }

    // 使用代理生成回答
    println("\n生成回答:")
    val response = myAgent.generate("Hello, 你好！请介绍一下自己。")

    // 打印回答
    println("\n代理回答:")
    println(response.text)

    println("\nHello Agent 示例完成")
}
