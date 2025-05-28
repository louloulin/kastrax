package ai.kastrax.examples.streaming

import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.integrations.openai.openAi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

/**
 * 代理流式返回的高级示例
 * 展示多种流式处理场景
 */
fun main() = runBlocking {
    println("KastraX 代理流式返回高级示例")
    println("=============================")

    // 创建代理
    val agent = agent {
        name = "Streaming Assistant"
        instructions = """
            你是一个智能助手，擅长提供详细和有用的回答。
            请用中文回答问题，并尽可能提供丰富的内容。
        """.trimIndent()
        
        // 注意：实际使用时需要配置正确的模型
        // model = openAi("gpt-4")
    }

    // 示例1：基础流式生成
    println("\n=== 示例1：基础流式生成 ===")
    println("用户: 请介绍一下人工智能的发展历史")
    
    val response1 = agent.stream("请介绍一下人工智能的发展历史")
    
    print("助手: ")
    var charCount = 0
    response1.textStream?.collect { chunk ->
        print(chunk)
        charCount += chunk.length
        // 模拟打字机效果
        if (charCount % 10 == 0) {
            delay(50)
        }
    }
    println("\n")

    // 示例2：带会话上下文的流式生成
    println("=== 示例2：带会话上下文的流式生成 ===")
    
    // 创建会话
    val session = agent.createSession()
    println("会话ID: ${session?.id}")
    
    println("\n用户: 什么是机器学习？")
    val response2 = agent.stream(
        "什么是机器学习？",
        AgentStreamOptions(threadId = session?.id)
    )
    
    print("助手: ")
    response2.textStream?.collect { chunk ->
        print(chunk)
        delay(30)
    }
    println("\n")
    
    // 继续对话
    println("用户: 它有哪些主要应用？")
    val response3 = agent.stream(
        "它有哪些主要应用？",
        AgentStreamOptions(threadId = session?.id)
    )
    
    print("助手: ")
    response3.textStream?.collect { chunk ->
        print(chunk)
        delay(30)
    }
    println("\n")

    // 示例3：流式生成与状态监控
    println("=== 示例3：流式生成与状态监控 ===")
    println("用户: 请解释深度学习的工作原理")
    
    val response4 = agent.stream("请解释深度学习的工作原理")
    
    print("助手: ")
    var tokenCount = 0
    response4.textStream?.collect { chunk ->
        print(chunk)
        tokenCount++
        
        // 每50个token显示一次状态
        if (tokenCount % 50 == 0) {
            print(" [已生成${tokenCount}个token] ")
        }
        
        delay(40)
    }
    println("\n")
    
    // 获取最终状态
    val finalState = agent.getState()
    println("最终状态: ${finalState?.status}")

    // 示例4：错误处理与重试
    println("\n=== 示例4：错误处理与重试 ===")
    
    try {
        println("用户: 请用代码示例说明神经网络")
        val response5 = agent.stream("请用代码示例说明神经网络")
        
        print("助手: ")
        response5.textStream?.collect { chunk ->
            print(chunk)
            delay(25)
        }
        println("\n")
        
    } catch (e: Exception) {
        println("流式生成出错: ${e.message}")
        println("尝试重新生成...")
        
        // 重试逻辑
        val retryResponse = agent.generate("请简单介绍神经网络")
        println("重试结果: ${retryResponse.text}")
    }

    // 示例5：多轮对话流式生成
    println("\n=== 示例5：多轮对话流式生成 ===")
    
    val questions = listOf(
        "什么是自然语言处理？",
        "它与机器学习有什么关系？",
        "请举一个实际应用的例子"
    )
    
    questions.forEachIndexed { index, question ->
        println("\n第${index + 1}轮对话:")
        println("用户: $question")
        
        val response = agent.stream(
            question,
            AgentStreamOptions(threadId = session?.id)
        )
        
        print("助手: ")
        response.textStream?.collect { chunk ->
            print(chunk)
            delay(35)
        }
        println("\n")
        
        // 短暂停顿
        delay(1000)
    }

    // 获取会话历史
    println("\n=== 会话历史 ===")
    val messages = agent.getSessionMessages(session?.id ?: "")
    println("总消息数: ${messages?.size ?: 0}")
    
    messages?.takeLast(2)?.forEach { message ->
        println("角色: ${message.message.role}")
        println("内容: ${message.message.content.take(100)}...")
        println("时间: ${message.createdAt}")
        println()
    }

    println("流式生成示例完成！")
}