package ai.kastrax.examples

import ai.kastrax.integrations.gemini.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

/**
 * Gemini 直接流式响应示例
 * 这个示例展示了如何直接使用 GeminiStreamingClient 进行流式响应，而不是通过 Agent 层。
 */
fun main() = runBlocking {
    println("开始执行GeminiDirectStreamingExample...")
    
    // 设置系统属性，确保UTF-8编码
    System.setProperty("file.encoding", "UTF-8")
    System.setProperty("sun.jnu.encoding", "UTF-8")
    
    // 设置控制台输出编码
    System.setOut(java.io.PrintStream(System.out, true, "UTF-8"))
    
    // 设置区域设置为UTF-8
    java.util.Locale.setDefault(java.util.Locale.US)
    
    try {
        println("Gemini直接流式响应示例")
        println("-------------------")
        
        // API密钥
        val apiKey = System.getenv("GOOGLE_API_KEY") ?: "your-api-key-here"
        val baseUrl = "https://generativelanguage.googleapis.com/v1"
        
        println("创建流式客户端...")
        // 创建流式客户端
        val client = GeminiStreamingClient(baseUrl, apiKey)
        
        // 预定义的问题列表
        val questions = listOf(
            "2+2等于多少？",
            "什么是人工智能？",
            "计算平方根16",
            "用中文解释量子力学的基本原理"
        )
        
        // 自动执行每个问题
        for (question in questions) {
            println("\n问题: $question")
            println("Gemini正在思考...")
            
            // 创建请求
            val request = GeminiChatRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(
                            GeminiPart(
                                text = question
                            )
                        )
                    )
                ),
                system = "你是一个有帮助的助手，可以回答问题和执行计算。"
            )
            
            print("\n回答: ")
            
            try {
                // 直接使用GeminiStreamingClient的流式方法
                client.createChatCompletionStream(GeminiModel.GEMINI_1_5_PRO.id, request).collect { chunk ->
                    when (chunk) {
                        is GeminiStreamChunk.Content -> {
                            // 打印内容并立即刷新，确保实时显示
                            // 使用UTF-8编码确保中文正确显示
                            val text = chunk.text
                            // 将文本转换为UTF-8字节数组，然后重新解析
                            val utf8Text = String(text.toByteArray(Charsets.UTF_8), Charsets.UTF_8)
                            print(utf8Text)
                            System.out.flush() // 关键：立即刷新输出缓冲区
                            
                            // 添加小延迟，确保字符能够被看到
                            Thread.sleep(1) // 1毫秒延迟，可以根据需要调整
                        }
                        is GeminiStreamChunk.Finished -> {
                            // 完成时打印换行
                            println("\n(完成原因: ${chunk.reason})")
                        }
                        is GeminiStreamChunk.Done -> {
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
        
        println("\n示例结束。")
    } catch (e: Exception) {
        println("创建流式客户端时出错: ${e.message}")
        e.printStackTrace()
    }
}
