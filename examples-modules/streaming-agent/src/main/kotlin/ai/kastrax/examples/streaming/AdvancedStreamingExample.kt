package ai.kastrax.examples.streaming

import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.integrations.openai.openAi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

/**
 * 高级流式处理示例
 * 展示会话管理、并发处理和状态跟踪
 */
fun main() = runBlocking {
    println("KastraX 高级流式处理示例")
    println("==========================")

    // 创建代理
    val agent = agent {
        name = "Advanced Streaming Assistant"
        instructions = """
            你是一个高级AI助手，能够处理复杂的对话和任务。
            请保持对话的连贯性，并根据上下文提供相关的回答。
            用中文回答问题，保持专业和友好的语调。
        """.trimIndent()
        
        // 注意：实际使用时需要配置正确的模型
        // model = openAi("gpt-4")
    }

    // 示例1：会话管理
    println("\n=== 示例1：会话管理 ===")
    
    // 创建多个会话
    val session1 = agent.createSession()
    val session2 = agent.createSession()
    
    println("会话1 ID: ${session1?.id}")
    println("会话2 ID: ${session2?.id}")
    
    // 会话1：讨论编程
    println("\n--- 会话1：编程话题 ---")
    println("用户: 什么是面向对象编程？")
    
    val response1 = agent.stream(
        "什么是面向对象编程？",
        AgentStreamOptions(threadId = session1?.id)
    )
    
    print("助手: ")
    response1.textStream?.collect { chunk ->
        print(chunk)
        delay(40)
    }
    println("\n")
    
    // 会话2：讨论科学
    println("--- 会话2：科学话题 ---")
    println("用户: 解释一下相对论的基本概念")
    
    val response2 = agent.stream(
        "解释一下相对论的基本概念",
        AgentStreamOptions(threadId = session2?.id)
    )
    
    print("助手: ")
    response2.textStream?.collect { chunk ->
        print(chunk)
        delay(40)
    }
    println("\n")
    
    // 继续会话1
    println("--- 继续会话1 ---")
    println("用户: 能举个具体的例子吗？")
    
    val response3 = agent.stream(
        "能举个具体的例子吗？",
        AgentStreamOptions(threadId = session1?.id)
    )
    
    print("助手: ")
    response3.textStream?.collect { chunk ->
        print(chunk)
        delay(40)
    }
    println("\n")

    // 示例2：并发流式处理
    println("=== 示例2：并发流式处理 ===")
    
    val questions = listOf(
        "什么是机器学习？",
        "什么是深度学习？",
        "什么是神经网络？"
    )
    
    println("同时处理多个问题...")
    
    val concurrentResponses = questions.mapIndexed { index, question ->
        async {
            println("\n线程${index + 1}开始处理: $question")
            
            val response = agent.stream(question)
            val result = StringBuilder()
            
            response.textStream?.collect { chunk ->
                result.append(chunk)
                print("[${index + 1}] $chunk")
                delay(30)
            }
            
            println("\n线程${index + 1}完成")
            result.toString()
        }
    }
    
    val results = concurrentResponses.awaitAll()
    println("\n所有并发任务完成！")
    
    results.forEachIndexed { index, result ->
        println("结果${index + 1}长度: ${result.length}字符")
    }

    // 示例3：流式处理状态监控
    println("\n=== 示例3：流式处理状态监控 ===")
    println("用户: 请详细介绍人工智能的发展历程")
    
    val response4 = agent.stream("请详细介绍人工智能的发展历程")
    
    print("助手: ")
    var totalTokens = 0
    var sentences = 0
    var currentSentence = ""
    
    response4.textStream?.collect { chunk ->
        print(chunk)
        totalTokens++
        currentSentence += chunk
        
        // 检测句子结束
        if (chunk.contains("。") || chunk.contains("！") || chunk.contains("？")) {
            sentences++
            currentSentence = ""
            print(" [第${sentences}句] ")
        }
        
        // 每100个token显示状态
        if (totalTokens % 100 == 0) {
            print(" [${totalTokens}tokens] ")
        }
        
        delay(35)
    }
    
    println("\n")
    println("生成统计:")
    println("- 总tokens: $totalTokens")
    println("- 句子数: $sentences")
    
    // 获取代理状态
    val agentState = agent.getState()
    println("- 代理状态: ${agentState?.status}")

    // 示例4：交互式流式对话
    println("\n=== 示例4：交互式流式对话 ===")
    
    val conversationSession = agent.createSession()
    println("开始交互式对话 (会话ID: ${conversationSession?.id})")
    
    val conversationTopics = listOf(
        "你好，我想了解一下区块链技术",
        "区块链有什么实际应用？",
        "它的安全性如何保证？",
        "普通人如何学习区块链？"
    )
    
    conversationTopics.forEachIndexed { index, topic ->
        println("\n--- 对话轮次 ${index + 1} ---")
        println("用户: $topic")
        
        val response = agent.stream(
            topic,
            AgentStreamOptions(threadId = conversationSession?.id)
        )
        
        print("助手: ")
        var responseLength = 0
        
        response.textStream?.collect { chunk ->
            print(chunk)
            responseLength += chunk.length
            
            // 动态调整延迟
            val dynamicDelay = when {
                responseLength < 100 -> 50L
                responseLength < 300 -> 35L
                else -> 25L
            }
            
            delay(dynamicDelay)
        }
        
        println("\n回答长度: ${responseLength}字符")
        
        // 短暂停顿模拟思考时间
        if (index < conversationTopics.size - 1) {
            println("思考中...")
            delay(1500)
        }
    }

    // 示例5：流式处理错误恢复
    println("\n=== 示例5：流式处理错误恢复 ===")
    
    val problematicQuestions = listOf(
        "正常问题：什么是云计算？",
        "复杂问题：请详细解释量子纠缠现象的物理原理和数学表达",
        "简单问题：今天天气怎么样？"
    )
    
    problematicQuestions.forEach { question ->
        println("\n处理问题: $question")
        
        try {
            val response = agent.stream(question)
            
            print("助手: ")
            var successfulChunks = 0
            
            response.textStream?.collect { chunk ->
                print(chunk)
                successfulChunks++
                
                // 模拟可能的网络延迟
                delay(if (successfulChunks % 20 == 0) 100L else 30L)
            }
            
            println("\n✓ 成功处理，共${successfulChunks}个块")
            
        } catch (e: Exception) {
            println("✗ 流式处理失败: ${e.message}")
            println("尝试备用方案...")
            
            try {
                val fallbackResponse = agent.generate("请简单回答：${question.substringAfter("：")}")
                println("备用回答: ${fallbackResponse.text}")
            } catch (fallbackError: Exception) {
                println("备用方案也失败: ${fallbackError.message}")
            }
        }
    }

    // 示例6：会话历史分析
    println("\n=== 示例6：会话历史分析 ===")
    
    // 分析所有会话
    val allSessions = listOf(session1, session2, conversationSession)
    
    allSessions.forEachIndexed { index, session ->
        if (session != null) {
            println("\n--- 会话${index + 1}分析 ---")
            val messages = agent.getSessionMessages(session.id)
            
            println("消息总数: ${messages?.size ?: 0}")
            
            messages?.let { msgList ->
                val userMessages = msgList.filter { it.message.role == LlmMessageRole.USER }
                val assistantMessages = msgList.filter { it.message.role == LlmMessageRole.ASSISTANT }
                
                println("用户消息: ${userMessages.size}")
                println("助手消息: ${assistantMessages.size}")
                
                val totalChars = assistantMessages.sumOf { it.message.content.length }
                println("助手回复总字符数: $totalChars")
                
                if (assistantMessages.isNotEmpty()) {
                    val avgLength = totalChars / assistantMessages.size
                    println("平均回复长度: $avgLength 字符")
                }
            }
        }
    }

    // 最终状态
    println("\n=== 最终状态 ===")
    val finalState = agent.getState()
    println("代理最终状态: ${finalState?.status ?: "未知"}")
    
    println("\n高级流式处理示例完成！")
}