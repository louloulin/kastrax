package ai.kastrax.examples.streaming

import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.integrations.openai.openAi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 交互式流式聊天示例
 * 模拟真实的聊天体验
 */
fun main() = runBlocking {
    println("KastraX 交互式流式聊天示例")
    println("============================")
    println("这是一个模拟的交互式聊天，展示流式响应效果")
    println("输入 'quit' 或 'exit' 退出程序\n")

    // 创建聊天代理
    val chatAgent = agent {
        name = "ChatBot"
        instructions = """
            你是一个友好、有帮助的AI聊天助手。
            请用自然、对话式的语调回答用户的问题。
            保持回答简洁但有用，适合聊天场景。
            用中文与用户交流。
        """.trimIndent()
        
        // 注意：实际使用时需要配置正确的模型
        // model = openAi("gpt-4")
    }

    // 创建聊天会话
    val chatSession = chatAgent.createSession()
    println("聊天会话已创建 (ID: ${chatSession?.id})")
    println("开始聊天...\n")

    // 模拟用户输入的问题列表
    val simulatedInputs = listOf(
        "你好！",
        "你能做什么？",
        "我想学习编程，有什么建议吗？",
        "Python和Java哪个更适合初学者？",
        "能推荐一些学习资源吗？",
        "学习编程大概需要多长时间？",
        "我应该从哪个项目开始练手？",
        "谢谢你的建议！",
        "quit"
    )

    var messageCount = 0
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    for (userInput in simulatedInputs) {
        // 检查退出条件
        if (userInput.lowercase() in listOf("quit", "exit", "退出", "再见")) {
            println("👋 再见！感谢使用KastraX聊天助手！")
            break
        }

        messageCount++
        val currentTime = LocalDateTime.now().format(timeFormatter)
        
        // 显示用户消息
        println("[$currentTime] 👤 用户: $userInput")
        
        // 模拟用户输入延迟
        delay(500)
        
        try {
            // 显示"正在输入"指示器
            print("[$currentTime] 🤖 助手正在输入")
            repeat(3) {
                delay(300)
                print(".")
            }
            println()
            
            // 开始流式响应
            val response = chatAgent.stream(
                userInput,
                AgentStreamOptions(threadId = chatSession?.id)
            )
            
            val responseTime = LocalDateTime.now().format(timeFormatter)
            print("[$responseTime] 🤖 助手: ")
            
            var responseText = ""
            var charCount = 0
            
            response.textStream?.collect { chunk ->
                print(chunk)
                responseText += chunk
                charCount += chunk.length
                
                // 模拟真实的打字速度
                val typingDelay = when {
                    chunk.contains("\n") -> 100L // 换行后稍微停顿
                    chunk.matches(Regex("[。！？]")) -> 200L // 句号后停顿
                    chunk.matches(Regex("[，；：]")) -> 150L // 逗号后短暂停顿
                    charCount % 50 == 0 -> 80L // 每50个字符稍微停顿
                    else -> (20..60).random().toLong() // 随机打字速度
                }
                
                delay(typingDelay)
            }
            
            println("\n")
            
            // 显示响应统计
            val wordCount = responseText.split(Regex("\\s+")).size
            println("   📊 响应统计: ${responseText.length}字符, ${wordCount}词")
            
            // 模拟用户阅读时间
            val readingTime = (responseText.length * 50).coerceAtMost(3000)
            delay(readingTime.toLong())
            
        } catch (e: Exception) {
            println("❌ 抱歉，发生了错误: ${e.message}")
            println("   请稍后再试...")
            delay(1000)
        }
        
        // 消息间隔
        if (messageCount < simulatedInputs.size - 1) {
            println("" + "-".repeat(50))
            delay(800)
        }
    }

    // 显示聊天总结
    println("\n" + "=".repeat(50))
    println("聊天会话总结")
    println("=".repeat(50))
    
    val sessionMessages = chatAgent.getSessionMessages(chatSession?.id ?: "")
    sessionMessages?.let { messages ->
        val userMsgCount = messages.count { it.message.role == LlmMessageRole.USER }
        val assistantMsgCount = messages.count { it.message.role == LlmMessageRole.ASSISTANT }
        val totalChars = messages.filter { it.message.role == LlmMessageRole.ASSISTANT }
            .sumOf { it.message.content.length }
        
        println("📈 会话统计:")
        println("   • 总消息数: ${messages.size}")
        println("   • 用户消息: $userMsgCount")
        println("   • 助手消息: $assistantMsgCount")
        println("   • 助手总字符数: $totalChars")
        
        if (assistantMsgCount > 0) {
            println("   • 平均回复长度: ${totalChars / assistantMsgCount}字符")
        }
        
        println("\n📝 最近的对话:")
        messages.takeLast(6).forEach { message ->
            val role = if (message.message.role == LlmMessageRole.USER) "👤 用户" else "🤖 助手"
            val content = if (message.message.content.length > 100) {
                message.message.content.take(100) + "..."
            } else {
                message.message.content
            }
            println("   $role: $content")
        }
    }
    
    // 获取最终代理状态
    val finalState = chatAgent.getState()
    println("\n🔧 代理状态: ${finalState?.status ?: "未知"}")
    
    println("\n感谢体验KastraX交互式流式聊天！")
}

/**
 * 扩展函数：为字符串添加颜色（在支持ANSI的终端中）
 */
fun String.withColor(colorCode: String): String {
    return "\u001B[${colorCode}m$this\u001B[0m"
}

/**
 * 颜色常量
 */
object Colors {
    const val RED = "31"
    const val GREEN = "32"
    const val YELLOW = "33"
    const val BLUE = "34"
    const val PURPLE = "35"
    const val CYAN = "36"
    const val WHITE = "37"
}