package ai.kastrax.examples

import ai.kastrax.memory.api.MessageRole
import ai.kastrax.memory.impl.MemoryFactory
import kotlinx.coroutines.runBlocking

/**
 * 内存系统示例，展示如何使用 KastraX 的内存系统。
 */
fun main() = runBlocking {
    // 创建内存系统
    val memory = MemoryFactory.createMemory {
        storage(MemoryFactory.createInMemoryStorage())
        lastMessages(10)
        semanticRecall(true)
    }
    
    println("KastraX 内存系统示例")
    println("-------------------")
    
    // 创建一个新的对话线程
    val threadId = memory.createThread("示例对话")
    println("创建了新的对话线程: $threadId")
    
    // 创建一个简单的消息适配器
    class SimpleMessage(
        override val role: MessageRole,
        override val content: String,
        override val name: String? = null,
        override val toolCalls: List<ai.kastrax.memory.api.ToolCall> = emptyList(),
        override val toolCallId: String? = null
    ) : ai.kastrax.memory.api.Message
    
    // 保存系统消息
    val systemMessage = SimpleMessage(
        role = MessageRole.SYSTEM,
        content = "你是一个有帮助的助手。"
    )
    memory.saveMessage(systemMessage, threadId)
    println("保存了系统消息")
    
    // 保存用户消息
    val userMessage1 = SimpleMessage(
        role = MessageRole.USER,
        content = "你好，请介绍一下自己。"
    )
    memory.saveMessage(userMessage1, threadId)
    println("保存了用户消息 1")
    
    // 保存助手消息
    val assistantMessage1 = SimpleMessage(
        role = MessageRole.ASSISTANT,
        content = "你好！我是一个AI助手，我可以回答问题、提供信息和帮助你完成各种任务。有什么我可以帮助你的吗？"
    )
    memory.saveMessage(assistantMessage1, threadId)
    println("保存了助手消息 1")
    
    // 保存更多消息
    val userMessage2 = SimpleMessage(
        role = MessageRole.USER,
        content = "我想了解一下人工智能。"
    )
    memory.saveMessage(userMessage2, threadId)
    println("保存了用户消息 2")
    
    val assistantMessage2 = SimpleMessage(
        role = MessageRole.ASSISTANT,
        content = "人工智能（AI）是计算机科学的一个分支，致力于创建能够模拟人类智能的系统。它包括机器学习、自然语言处理、计算机视觉等多个领域。现代AI系统可以执行各种任务，如语言翻译、图像识别、游戏和决策支持等。"
    )
    memory.saveMessage(assistantMessage2, threadId)
    println("保存了助手消息 2")
    
    // 获取对话历史
    println("\n获取对话历史:")
    val messages = memory.getMessages(threadId)
    messages.forEachIndexed { index, message ->
        println("${index + 1}. [${message.message.role}] ${message.message.content.take(50)}${if (message.message.content.length > 50) "..." else ""}")
    }
    
    // 搜索消息
    println("\n搜索包含'人工智能'的消息:")
    val searchResults = memory.searchMessages("人工智能", threadId)
    searchResults.forEach { message ->
        println("[${message.message.role}] ${message.message.content.take(50)}${if (message.message.content.length > 50) "..." else ""}")
    }
    
    // 获取线程信息
    println("\n线程信息:")
    val thread = memory.getThread(threadId)
    if (thread != null) {
        println("ID: ${thread.id}")
        println("标题: ${thread.title}")
        println("创建时间: ${thread.createdAt}")
        println("更新时间: ${thread.updatedAt}")
        println("消息数量: ${thread.messageCount}")
    }
    
    // 列出所有线程
    println("\n所有线程:")
    val threads = memory.listThreads()
    threads.forEach { t ->
        println("${t.id}: ${t.title} (${t.messageCount}条消息)")
    }
    
    // 删除线程
    println("\n删除线程 $threadId")
    val deleted = memory.deleteThread(threadId)
    println("删除${if (deleted) "成功" else "失败"}")
    
    // 验证线程已删除
    val remainingThreads = memory.listThreads()
    println("剩余线程数量: ${remainingThreads.size}")
}
