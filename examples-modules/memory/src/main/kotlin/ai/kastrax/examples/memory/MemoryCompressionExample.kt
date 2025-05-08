package ai.kastrax.examples.memory

import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.openai.openAi
import ai.kastrax.memory.api.MemoryCompressionConfig
import ai.kastrax.memory.api.MessageRole
import ai.kastrax.memory.impl.EnhancedMemory
import ai.kastrax.memory.impl.LlmMemoryCompressor
import ai.kastrax.memory.impl.SimpleMessage
import ai.kastrax.memory.impl.enhancedMemory
import kotlinx.coroutines.runBlocking

/**
 * 内存压缩示例，展示如何使用内存压缩功能。
 */
fun main() = runBlocking {
    println("KastraX 内存压缩示例")
    println("-------------------")
    
    // 创建OpenAI LLM实例
    val llm = openAi("gpt-4o")
    
    // 创建LLM内存压缩器
    val memoryCompressor = LlmMemoryCompressor(llm)
    
    // 创建增强型内存，启用内存压缩
    val enhancedMemory = enhancedMemory {
        lastMessages(10)
        memoryCompressor(memoryCompressor)
        compressionConfig(
            MemoryCompressionConfig(
                enabled = true,
                threshold = 15,  // 当消息数量达到15条时触发压缩
                targetSize = 5,   // 压缩后保留5条消息
                preserveSystemMessages = true,
                preserveRecentMessages = 3
            )
        )
    }
    
    // 创建使用内存压缩的Agent
    val agent = agent {
        name = "MemoryCompressionAgent"
        instructions = """
            你是一个具有内存压缩能力的助手。你可以记住对话历史，并在对话变长时自动压缩历史记录。
            
            当你看到对话摘要时，请参考摘要中的信息来保持对话的连贯性。
        """.trimIndent()
        model = llm
        memory = enhancedMemory
    }
    
    // 创建一个新的对话线程
    val threadId = enhancedMemory.createThread("内存压缩示例对话")
    println("创建了新的对话线程: $threadId")
    
    // 添加系统消息
    enhancedMemory.saveMessage(
        SimpleMessage(
            role = MessageRole.SYSTEM,
            content = "这是一个测试内存压缩功能的对话。"
        ),
        threadId
    )
    
    // 模拟一个长对话（超过阈值）
    val conversation = listOf(
        "你好，我想了解一下人工智能的基础知识。",
        "人工智能有哪些主要的分支？",
        "机器学习和深度学习有什么区别？",
        "神经网络是如何工作的？",
        "什么是卷积神经网络？",
        "自然语言处理是做什么的？",
        "强化学习的基本原理是什么？",
        "什么是监督学习和无监督学习？",
        "人工智能在医疗领域有哪些应用？",
        "人工智能在金融领域有哪些应用？",
        "人工智能在教育领域有哪些应用？",
        "人工智能的伦理问题有哪些？",
        "什么是图灵测试？",
        "人工智能的未来发展趋势是什么？",
        "如何开始学习人工智能？"
    )
    
    // 模拟对话
    for ((index, userMessage) in conversation.withIndex()) {
        println("\n用户: $userMessage")
        
        // 保存用户消息
        enhancedMemory.saveMessage(
            SimpleMessage(
                role = MessageRole.USER,
                content = userMessage
            ),
            threadId
        )
        
        // 生成回复
        val response = agent.generate(userMessage, threadId = threadId)
        println("助手: ${response.content}")
        
        // 获取当前消息数量
        val messages = enhancedMemory.getMessages(threadId, 100)
        println("当前消息数量: ${messages.size}")
        
        // 检查是否有摘要消息
        val summaryMessage = messages.find { 
            it.message.role == MessageRole.SYSTEM && 
            it.message.name == "conversation_summary" 
        }
        
        if (summaryMessage != null && index > 0) {
            println("\n检测到对话摘要:")
            println(summaryMessage.message.content)
        }
    }
    
    // 压缩后的对话继续
    val continuedConversation = listOf(
        "基于我们之前的讨论，你能推荐一些学习人工智能的资源吗？",
        "Python在人工智能中为什么这么重要？",
        "TensorFlow和PyTorch有什么区别？"
    )
    
    println("\n\n压缩后继续对话")
    println("-------------------")
    
    for (userMessage in continuedConversation) {
        println("\n用户: $userMessage")
        
        // 保存用户消息
        enhancedMemory.saveMessage(
            SimpleMessage(
                role = MessageRole.USER,
                content = userMessage
            ),
            threadId
        )
        
        // 生成回复
        val response = agent.generate(userMessage, threadId = threadId)
        println("助手: ${response.content}")
    }
}
