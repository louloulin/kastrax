package ai.kastrax.examples.agent

import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.integrations.deepseek.DeepSeekModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect

/**
 * 流式处理 Agent 示例
 *
 * 这个示例展示了如何创建一个支持流式响应的代理，
 * 可以实时接收和显示生成的文本内容。
 */
fun main() {
    streamingAgentExample()
}

fun streamingAgentExample() = runBlocking {
    println("=== 流式处理 Agent 示例 ===")
    println("展示实时文本生成功能\n")
    
    // 创建 Deepseek LLM 提供商
    val llm = deepSeek {
        // 设置 API 密钥
        apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
        
        // 设置模型
        model(DeepSeekModel.DEEPSEEK_CHAT)
        
        // 设置生成参数
        temperature(0.8)
        maxTokens(1500)
        topP(0.95)
        
        // 设置超时时间
        timeout(60)
    }
    
    // 创建支持流式处理的 Agent
    val streamingAgent = agent {
        name = "流式助手"
        instructions = """
            你是一个智能助手，擅长提供详细、有条理的回答。
            你的回答应该：
            1. 结构清晰，逻辑性强
            2. 内容丰富，信息准确
            3. 语言流畅，易于理解
            4. 适当使用例子和类比
            5. 保持专业和友好的语调
        """.trimIndent()
        
        model = llm
        
        // 配置流式处理选项
        defaultStreamOptions {
            temperature(0.8)
            maxTokens(1500)
        }
    }
    
    // 准备测试问题
    val questions = listOf(
        "请详细解释什么是人工智能，包括其发展历史和主要应用领域。",
        "Kotlin 协程的工作原理是什么？请举例说明如何使用协程进行异步编程。",
        "请介绍区块链技术的核心概念，以及它在现实世界中的应用场景。"
    )
    
    // 逐个处理问题，展示流式响应
    questions.forEachIndexed { index, question ->
        println("\n" + "=".repeat(60))
        println("问题 ${index + 1}: $question")
        println("=".repeat(60))
        println("\n正在生成回答...\n")
        
        // 创建流式处理选项
        val streamOptions = AgentStreamOptions(
            temperature = 0.8,
            maxTokens = 1500
        )
        
        try {
            // 调用流式生成方法
            val response = streamingAgent.stream(question, streamOptions)
            
            // 处理流式文本响应
            response.textStream?.let { textStream ->
                print("回答: ")
                textStream.collect { chunk ->
                    print(chunk)
                    // 添加小延迟以模拟真实的流式效果
                    kotlinx.coroutines.delay(10)
                }
                println() // 换行
            } ?: run {
                // 如果没有流式响应，显示完整文本
                println("回答: ${response.text}")
            }
            
            // 处理工具调用（如果有）
            if (response.toolCalls.isNotEmpty()) {
                println("\n检测到工具调用:")
                response.toolCalls.forEach { toolCall ->
                    println("- 工具: ${toolCall.name}")
                    println("  参数: ${toolCall.arguments}")
                }
            }
            
        } catch (e: Exception) {
            println("生成回答时发生错误: ${e.message}")
        }
        
        println("\n" + "-".repeat(60))
        println("问题 ${index + 1} 处理完成")
        
        // 在问题之间添加间隔
        if (index < questions.size - 1) {
            println("\n等待 2 秒后处理下一个问题...")
            kotlinx.coroutines.delay(2000)
        }
    }
    
    println("\n=== 流式处理示例完成 ===")
    println("所有问题已处理完毕")
}

/**
 * 交互式流式聊天示例
 */
fun interactiveStreamingExample() = runBlocking {
    println("=== 交互式流式聊天示例 ===")
    println("输入 'exit' 或 'quit' 退出聊天\n")
    
    // 创建流式聊天 Agent
    val chatAgent = agent {
        name = "聊天助手"
        instructions = """
            你是一个友好的聊天助手，善于进行自然对话。
            请保持对话的连贯性和趣味性。
        """.trimIndent()
        
        model = deepSeek {
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
            model(DeepSeekModel.DEEPSEEK_CHAT)
            temperature(0.9)
            maxTokens(800)
        }
    }
    
    while (true) {
        print("\n你: ")
        val userInput = readLine() ?: break
        
        if (userInput.lowercase() in listOf("exit", "quit", "退出")) {
            println("再见！")
            break
        }
        
        if (userInput.isBlank()) {
            continue
        }
        
        try {
            print("助手: ")
            val response = chatAgent.stream(userInput)
            
            response.textStream?.collect { chunk ->
                print(chunk)
                kotlinx.coroutines.delay(20) // 稍慢的打字效果
            } ?: print(response.text)
            
            println() // 换行
            
        } catch (e: Exception) {
            println("发生错误: ${e.message}")
        }
    }
}