package ai.kastrax.examples.other

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
 * 4. 每个字符单独发送，确保真正的字符级流式响应
 * 5. 优化HTTP客户端配置，提高网络传输效率
 */
fun runDirectStreamingExample() = runBlocking {
    // API 密钥
    val apiKey = "sk-85e83081df28490b9ae63188f0cb4f79"
    val baseUrl = "https://api.deepseek.com/v1"

    println("创建流式客户端...")
    try {
        // 创建流式客户端，增加超时设置
        val client = DeepSeekStreamingClient(
            baseUrl = baseUrl,
            apiKey = apiKey,
            timeout = 180000 // 增加超时时间到180秒
        )

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
                stream = true,
                temperature = 0.3,  // 降低温度以获得更可靠的结果
                maxTokens = 1000   // 设置较大的最大令牌数
            )

            print("\n回答: ")

            try {
                // 直接使用改进的 DeepSeekStreamingClient 的流式方法
                client.createChatCompletionStream(request).collect { chunk ->
                    when (chunk) {
                        is DeepSeekStreamChunk.Content -> {
                            // 打印内容并立即刷新，确保实时显示
                            // 使用 UTF-8 编码确保中文正确显示
                            val text = chunk.text
                            // 将文本转换为 UTF-8 字节数组，然后重新解析
                            val utf8Text = String(text.toByteArray(Charsets.UTF_8), Charsets.UTF_8)
                            print(utf8Text)
                            System.out.flush() // 关键：立即刷新输出缓冲区
                            // 移除了人为延迟，让网络传输决定速度
                        }
                        is DeepSeekStreamChunk.ToolCall -> {
                            // 处理工具调用
                            println("\n工具调用: ${chunk.name}(${chunk.arguments})")
                            System.out.flush() // 立即刷新输出缓冲区
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
    } catch (e: Exception) {
        println("创建流式客户端时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 主函数，调用示例。
 */
fun main() {
    // 设置系统属性，确保 UTF-8 编码
    System.setProperty("file.encoding", "UTF-8")
    System.setProperty("sun.jnu.encoding", "UTF-8")

    // 设置控制台输出编码
    System.setOut(java.io.PrintStream(System.out, true, "UTF-8"))

    // 设置区域设置为 UTF-8
    java.util.Locale.setDefault(java.util.Locale.US)

    runDirectStreamingExample()
}
