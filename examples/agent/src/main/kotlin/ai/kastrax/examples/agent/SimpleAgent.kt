package ai.kastrax.examples.agent

import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking

/**
 * 简单代理示例
 *
 * 这个示例展示了如何创建一个简单的代理并使用它来生成回答。
 */
fun main() = runBlocking {
    println("简单代理示例")
    println("-------------------")
    
    // 创建一个使用 Deepseek 的代理
    val myAgent = agent {
        name = "专家助手"
        instructions = """
            你是一个专业的AI助手，擅长回答各种领域的问题。
            你的回答应该：
            1. 准确、全面
            2. 条理清晰，易于理解
            3. 在适当的情况下提供例子
            4. 避免冗长，直接切入主题
        """.trimIndent()
        
        // 使用 Deepseek 模型
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
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
    
    // 使用代理回答问题
    questions.forEachIndexed { index, question ->
        println("\n问题 ${index + 1}: $question")
        println("生成回答中...")
        
        val response = myAgent.generate(question)
        
        println("\n回答 ${index + 1}:")
        println("----------")
        println(response.text)
    }
    
    println("\n简单代理示例完成")
}
