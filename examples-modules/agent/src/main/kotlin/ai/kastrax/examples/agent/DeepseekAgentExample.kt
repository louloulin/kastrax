package ai.kastrax.examples.agent

import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.integrations.deepseek.DeepSeekModel
import kotlinx.coroutines.runBlocking

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
        // 直接设置 API 密钥
        apiKey("sk-85e83081df28490b9ae63188f0cb4f79")

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

        // 配置默认生成选项
        defaultGenerateOptions {
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

    // 逐个提问并获取回答
    questions.forEachIndexed { index, question ->
        println("\n问题 ${index + 1}: $question")

        // 创建生成选项
        val options = AgentGenerateOptions()

        // 生成回答
        val response = agent.generate(question, options)

        println("回答:")
        println(response.text)
    }
}
