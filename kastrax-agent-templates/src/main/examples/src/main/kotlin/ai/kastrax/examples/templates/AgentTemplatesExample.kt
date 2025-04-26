package ai.kastrax.examples.templates

import ai.kastrax.agent.templates.AgentTemplates
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking

/**
 * 代理模板示例，展示如何使用预设的代理模板。
 */
fun main() = runBlocking {
    println("KastraX 代理模板示例")
    println("-------------------")

    // 创建 DeepSeek 提供者
    val deepseek = deepSeek {
        model(DeepSeekModel.DEEPSEEK_CHAT)
        // 显式设置 API 密钥
        apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
    }

    // 创建数学助手代理
    val mathAgent = AgentTemplates.createMathAssistantAgent(
        name = "数学助手",
        model = deepseek
    )

    // 创建日期时间助手代理
    val dateTimeAgent = AgentTemplates.createDateTimeAssistantAgent(
        name = "日期时间助手",
        model = deepseek
    )

    // 使用数学助手代理
    println("\n使用数学助手代理:")
    val mathQuestions = listOf(
        "计算 25 + 17 的结果",
        "计算 144 的平方根",
        "计算 sin(90) 的值"
    )

    for (question in mathQuestions) {
        println("\n问题: $question")
        try {
            val response = mathAgent.generate(question)
            println("数学助手: ${response.text}")
        } catch (e: Exception) {
            println("错误: ${e.message}")
        }
    }

    // 使用日期时间助手代理
    println("\n使用日期时间助手代理:")
    val dateTimeQuestions = listOf(
        "现在的日期和时间是什么？",
        "今天加上30天是什么日期？",
        "北京时间下午3点在伦敦是几点？"
    )

    for (question in dateTimeQuestions) {
        println("\n问题: $question")
        try {
            val response = dateTimeAgent.generate(question)
            println("日期时间助手: ${response.text}")
        } catch (e: Exception) {
            println("错误: ${e.message}")
        }
    }
}
