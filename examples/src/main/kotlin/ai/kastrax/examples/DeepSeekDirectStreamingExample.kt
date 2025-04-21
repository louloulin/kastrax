package ai.kastrax.examples

import ai.kastrax.integrations.deepseek.*
import kotlinx.coroutines.runBlocking

/**
 * DeepSeek 直接流式响应示例。
 *
 * 这个示例直接使用改进的 DeepSeekStreamingClient，绕过 Agent 层，
 * 实现真正的字符级实时流式响应。主要改进包括：
 * 1. 使用 channelFlow 而非普通 flow，更好地控制背压
 * 2. 添加更多 SSE 相关头部，确保实时响应
 * 3. 去除人为延迟，让网络传输决定速度
 * 4. 每发送一个字符就让出协程，确保实时处理
 */
fun runDirectStreamingExample() = runBlocking {
    // API 密钥
    val apiKey = "sk-85e83081df28490b9ae63188f0cb4f79"
    val baseUrl = "https://api.deepseek.com/v1"

    // 创建流式客户端
    val client = DeepSeekStreamingClient(baseUrl, apiKey)

    println("DeepSeek 直接流式响应示例")
    println("-------------------")

    // 预定义的问题列表
    val questions = listOf(
        "2+2等于多少？",
        "什么是人工智能？",
        "计算平方根 16",
        "用中文解释量子力学的基本原理"
    )

    // 自动执行每个问题
    for (question in questions) {
        println("\n问题: $question")
        println("DeepSeek 正在思考...")

        // 创建请求
        val request = DeepSeekChatCompletionRequest(
            model = "deepseek-chat",
            messages = listOf(
                DeepSeekMessage(
                    role = "system",
                    content = "你是一个有帮助的助手，可以回答问题和执行计算。"
                ),
                DeepSeekMessage(
                    role = "user",
                    content = question
                )
            ),
            stream = true
        )

        print("\n回答: ")

        try {
            // 直接使用改进的 DeepSeekStreamingClient 的流式方法
            client.createChatCompletionStream(request).collect { chunk ->
                when (chunk) {
                    is DeepSeekStreamChunk.Content -> {
                        // 打印内容并立即刷新，确保实时显示
                        print(chunk.text)
                        System.out.flush() // 关键：立即刷新输出缓冲区
                    }
                    is DeepSeekStreamChunk.Finished -> {
                        // 完成时打印换行
                        println("\n(完成原因: ${chunk.reason})")
                    }
                    is DeepSeekStreamChunk.Done -> {
                        // 流结束
                        println("\n-------------------")
                    }
                }
            }
        } catch (e: Exception) {
            println("\n错误: ${e.message}")
            e.printStackTrace()
        }

        // 添加延时，避免请求过快
        Thread.sleep(1000)
    }

    // 关闭客户端
    // 注意: 在示例结束时，客户端会自动关闭

    println("\n示例结束。")
}

/**
 * 主函数，调用示例。
 */
fun main() {
    runDirectStreamingExample()
}
