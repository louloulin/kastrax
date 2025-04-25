package ai.kastrax.app.examples

import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking

/**
 * 简单的 DeepSeek 测试示例
 */
fun main() = runBlocking {
    println("DeepSeek 测试")
    println("========================")

    try {
        // 设置 UTF-8 编码，确保中文正确显示
        System.setProperty("file.encoding", "UTF-8")
        System.setProperty("sun.jnu.encoding", "UTF-8")

        // 创建一个使用 DeepSeek 的代理
        val myAgent = agent {
            name = "DeepSeek 助手"
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
        val response = myAgent.generate("你好，请介绍一下自己。")

        // 打印响应
        println("\nDeepSeek 响应:")
        println(response.text)
    } catch (e: Exception) {
        println("发生错误: ${e.message}")
        e.printStackTrace()
    }
}
