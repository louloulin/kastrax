package ai.kastrax.app.examples

import ai.kastrax.app.kastraxInstance
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.integrations.deepseek.DeepSeekException
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.core.agent.agent
import kotlinx.coroutines.runBlocking

/**
 * 简单示例应用程序。
 * 展示如何使用 KastraX 实例。
 */
fun main() = runBlocking {
    println("KastraX 简单示例应用程序")
    println("========================")

    // 设置 UTF-8 编码，确保中文正确显示
    System.setProperty("file.encoding", "UTF-8")
    System.setProperty("sun.jnu.encoding", "UTF-8")

    try {
        // 创建一个使用 DeepSeek 的代理
        println("\n创建 DeepSeek 测试代理:")
        val deepSeekAgent = agent {
            name = "DeepSeek 测试代理"
            instructions = "你是一个有帮助的助手，可以回答问题。"

            // 使用 DeepSeek 模型
            model = deepSeek {
                model(DeepSeekModel.DEEPSEEK_CHAT)
                apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "sk-85e83081df28490b9ae63188f0cb4f79")
                timeout(120000) // 120秒超时
            }
        }

        // 发送请求
        println("正在发送请求到 DeepSeek API...")
        val response = deepSeekAgent.generate("你好，请介绍一下自己。")

        // 打印响应
        println("\nDeepSeek 响应:")
        println(response.text)

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
    } catch (e: DeepSeekException) {
        println("DeepSeek API 错误: ${e.message}")
        println("请检查 API 密钥是否正确，或者尝试增加超时时间")
    } catch (e: Exception) {
        println("发生错误: ${e.message}")
        e.printStackTrace()
    }
}
