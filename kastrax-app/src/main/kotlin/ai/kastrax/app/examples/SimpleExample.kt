package ai.kastrax.app.examples

import ai.kastrax.app.kastraxInstance
import ai.kastrax.core.agent.AgentGenerateOptions
import kotlinx.coroutines.runBlocking

/**
 * 简单示例应用程序。
 * 展示如何使用 KastraX 实例。
 */
fun main() = runBlocking {
    println("KastraX 简单示例应用程序")
    println("========================")

    // 使用助手代理
    println("\n使用助手代理:")
    val assistantAgent = kastraxInstance.getAgent("assistant")
    val assistantResponse = assistantAgent.generate(
        "计算 15 * 7 的结果是多少？",
        AgentGenerateOptions(temperature = 0.7, maxTokens = 1000)
    )
    println("助手回答: ${assistantResponse.text}")

    // 使用专家代理
    println("\n使用专家代理:")
    val expertAgent = kastraxInstance.getAgent("expert")
    val expertResponse = expertAgent.generate(
        "简单解释一下什么是神经网络？",
        AgentGenerateOptions(temperature = 0.3, maxTokens = 2000)
    )
    println("专家回答: ${expertResponse.text}")
}
