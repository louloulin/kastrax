package ai.kastrax.examples.agent

import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.integrations.deepseek.DeepSeekModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect

/**
 * Deepseek Agent 示例
 *
 * 展示如何使用 Deepseek 作为 LLM 提供商创建 Agent
 */

/**
 * 主入口函数
 */
fun main() {
    deepseekAgentExample()
}

fun deepseekAgentExample() = runBlocking {
    // 使用 DSL 创建 Deepseek LLM 提供商
    val llm = deepSeek {
        // 设置 API 密钥
        apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")

        // 设置模型
        model(DeepSeekModel.DEEPSEEK_CHAT)

        // 设置生成参数
        temperature(0.7)
        maxTokens(2000)
        topP(0.95)

        // 设置超时时间（秒）
        timeout(60)
    }

    // 使用 DSL 创建 Agent
    val agent = agent {
        // 设置 Agent 名称
        name = "DeepseekAgent"

        // 设置 Agent 指令
        instructions = """
            你是一个由 Deepseek 驱动的智能助手，专注于提供准确、有用的信息。
            你应该：
            1. 回答用户的问题，提供详细且准确的信息
            2. 承认你不知道的事情，而不是编造信息
            3. 使用清晰、简洁的语言
            4. 保持礼貌和专业
        """.trimIndent()

        // 设置 LLM 模型
        model = llm

        // 配置默认流式选项
        defaultStreamOptions {
            temperature(0.7)
            maxTokens(2000)
        }
    }

    println("=== Deepseek Agent 示例 ===")

    // 准备提问
    val questions = listOf(
        "什么是大语言模型？",
        "Kotlin 协程与 Java 线程有什么区别？",
        "请解释一下量子计算的基本原理"
    )

    // 逐个提问并获取流式回答
    questions.forEachIndexed { index, question ->
        println("\n问题 ${index + 1}: $question")

        // 创建流式选项
        val options = AgentStreamOptions(
            temperature = 0.7,
            maxTokens = 2000
        )

        try {
            // 生成流式回答
            val response = agent.stream(question, options)

            println("回答:")
            
            // 处理流式文本响应
            response.textStream?.collect { chunk ->
                print(chunk)
                // 添加小延迟模拟打字效果
                kotlinx.coroutines.delay(25)
            } ?: run {
                // 如果没有流式响应，显示完整文本
                print(response.text)
            }
            
            println() // 换行
            
            // 处理工具调用（如果有）
            if (response.toolCalls.isNotEmpty()) {
                println("\n检测到工具调用:")
                response.toolCalls.forEach { toolCall ->
                    println("- 工具: ${toolCall.name}")
                    println("  参数: ${toolCall.arguments}")
                }
            }
            
        } catch (e: Exception) {
            println("生成回答时发生错误: ${e.message}")
        }
    }
}
