package ai.kastrax.examples

import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.anthropic.AnthropicModel
import ai.kastrax.integrations.anthropic.anthropic
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

/**
 * Anthropic 流式响应示例
 * 这个示例展示了如何使用 Anthropic Claude 模型进行流式响应。
 */
fun main() = runBlocking {
    println("开始执行AnthropicStreamingExample...")
    
    // 设置系统属性，确保UTF-8编码
    System.setProperty("file.encoding", "UTF-8")
    System.setProperty("sun.jnu.encoding", "UTF-8")
    
    // 设置区域设置为UTF-8
    java.util.Locale.setDefault(java.util.Locale.US)
    
    // 创建Anthropic代理
    val myAgent = agent {
        name = "Anthropic流式助手"
        instructions = "你是一个有帮助的助手，可以回答问题。"
        
        // 使用Anthropic Claude模型
        model = anthropic {
            model(AnthropicModel.CLAUDE_3_SONNET)
            apiKey(System.getenv("ANTHROPIC_API_KEY") ?: "your-api-key-here")
            useEnhancedStreaming(true)
        }
    }
    
    println("Anthropic流式响应示例")
    println("-------------------")
    
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
        println("Claude正在思考...")
        
        print("\n回答: ")
        
        try {
            // 使用流式响应
            val response = myAgent.stream(question)
            
            // 收集流式响应的内容
            response.textStream?.collect { chunk ->
                // 将文本转换为UTF-8字节数组，然后重新解析，确保中文正确显示
                val utf8Text = String(chunk.toByteArray(Charsets.UTF_8), Charsets.UTF_8)
                print(utf8Text)
                System.out.flush()  // 立即刷新输出缓冲区，确保实时显示
                
                // 添加小延迟，确保字符能够被看到
                Thread.sleep(1)  // 1毫秒延迟，可以根据需要调整
            }
        } catch (e: Exception) {
            println("\n流式响应出错: ${e.message}")
            e.printStackTrace()
        }
        
        // 添加延时，避免请求过快
        Thread.sleep(1000)
    }
    
    println("\n-------------------")
    println("示例结束")
}
