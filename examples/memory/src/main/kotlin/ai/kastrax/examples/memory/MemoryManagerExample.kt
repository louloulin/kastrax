package ai.kastrax.examples.memory

import ai.kastrax.memory.api.*
import ai.kastrax.memory.impl.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import java.io.File

/**
 * 记忆管理器示例，展示如何使用记忆查询和管理 API
 */
fun main() = runBlocking {
    // 创建记忆管理器
    val storage = InMemoryStorage()
    val memory = MemoryImpl(storage)
    val manager = MemoryManagerFactory.createManager(storage)

    println("KastraX 记忆管理器示例")
    println("-------------------------")

    // 创建一个新的对话线程
    val threadId = memory.createThread("示例对话")
    println("创建了新的对话线程: $threadId")

    // 创建一个简单的消息适配器
    class SimpleMessage(
        override val role: MessageRole,
        override val content: String,
        override val name: String? = null,
        override val toolCalls: List<ToolCall> = emptyList(),
        override val toolCallId: String? = null
    ) : Message

    // 保存一些测试消息
    val systemMessage = SimpleMessage(
        role = MessageRole.SYSTEM,
        content = "你是一个有帮助的助手。"
    )
    memory.saveMessage(systemMessage, threadId)

    val userMessage1 = SimpleMessage(
        role = MessageRole.USER,
        content = "你好，请介绍一下自己。"
    )
    memory.saveMessage(userMessage1, threadId)

    val assistantMessage1 = SimpleMessage(
        role = MessageRole.ASSISTANT,
        content = "你好！我是一个AI助手，我可以回答问题、提供信息和帮助你完成各种任务。有什么我可以帮你的吗？"
    )
    memory.saveMessage(assistantMessage1, threadId)

    val userMessage2 = SimpleMessage(
        role = MessageRole.USER,
        content = "我想了解一下人工智能的发展历史。"
    )
    memory.saveMessage(userMessage2, threadId, MemoryPriority.HIGH)

    val assistantMessage2 = SimpleMessage(
        role = MessageRole.ASSISTANT,
        content = "人工智能的发展历史可以追溯到20世纪50年代。1956年的达特茅斯会议被认为是人工智能研究的正式开始。之后经历了几次起伏，包括两次AI冬天。近年来，由于深度学习的突破，AI领域取得了显著进展。"
    )
    memory.saveMessage(assistantMessage2, threadId, MemoryPriority.HIGH)

    // 使用记忆管理器
    println("\n使用记忆管理器:")

    // 1. 高级查询消息
    println("\n1. 高级查询消息 (USER角色):")
    val userMessages = manager.queryMessages(threadId, MemoryQueryOptions(
        filters = listOf(RoleFilter(roles = listOf(MessageRole.USER))),
        limit = 10
    ))
    userMessages.forEach { message ->
        println("- ${message.message.content}")
    }

    // 2. 内容过滤查询
    println("\n2. 内容过滤查询 (包含'人工智能'):")
    val aiMessages = manager.queryMessages(threadId, MemoryQueryOptions(
        filters = listOf(ContentFilter(query = "人工智能", matchType = ContentMatchType.CONTAINS)),
        limit = 10
    ))
    aiMessages.forEach { message ->
        println("- ${message.message.content}")
    }

    // 3. 获取消息上下文
    println("\n3. 获取消息上下文:")
    // 首先获取一个消息ID
    val messages = memory.getMessages(threadId)
    val messageId = messages.find { it.message.role == MessageRole.USER }?.id

    if (messageId != null) {
        val contextMessages = manager.getMessageContext(ContextSelector(
            messageId = messageId,
            beforeCount = 1,
            afterCount = 1
        ))

        println("消息上下文:")
        contextMessages.forEach { message ->
            println("- ${message.message.role}: ${message.message.content}")
        }
    }

    // 4. 获取消息统计
    println("\n4. 获取消息统计:")
    val stats = manager.getMessageStats(threadId)
    println("总消息数: ${stats.totalMessages}")
    println("按角色统计:")
    stats.messagesByRole.forEach { (role, count) ->
        println("- $role: $count")
    }
    println("平均消息长度: ${stats.averageMessageLength}")
    println("最早消息时间: ${stats.oldestMessageTime}")
    println("最新消息时间: ${stats.newestMessageTime}")

    // 5. 导出线程
    println("\n5. 导出线程 (JSON):")
    val jsonExport = manager.exportThread(threadId, ExportFormat.JSON)
    val jsonFile = File("memory_export.json")
    jsonFile.writeText(jsonExport)
    println("线程已导出到: ${jsonFile.absolutePath}")

    println("\n6. 导出线程 (Markdown):")
    val markdownExport = manager.exportThread(threadId, ExportFormat.MARKDOWN)
    val markdownFile = File("memory_export.md")
    markdownFile.writeText(markdownExport)
    println("线程已导出到: ${markdownFile.absolutePath}")

    // 7. 批量更新消息优先级
    println("\n7. 批量更新消息优先级:")
    val updatedCount = manager.batchUpdateMessages(
        options = MessageBatchOptions(
            threadId = threadId,
            filters = listOf(RoleFilter(roles = listOf(MessageRole.USER)))
        ),
        updates = MessageUpdateOptions(
            priority = MemoryPriority.CRITICAL
        )
    )
    println("已更新 $updatedCount 条消息的优先级")

    // 8. 创建第二个线程用于合并示例
    val threadId2 = memory.createThread("第二个对话")

    val userMessage3 = SimpleMessage(
        role = MessageRole.USER,
        content = "深度学习是什么？"
    )
    memory.saveMessage(userMessage3, threadId2)

    val assistantMessage3 = SimpleMessage(
        role = MessageRole.ASSISTANT,
        content = "深度学习是机器学习的一个子领域，它使用多层神经网络来模拟人脑的学习过程。深度学习在图像识别、自然语言处理和语音识别等领域取得了突破性进展。"
    )
    memory.saveMessage(assistantMessage3, threadId2)

    // 9. 合并线程
    println("\n9. 合并线程:")
    val mergedThreadId = manager.mergeThreads(
        sourceThreadIds = listOf(threadId, threadId2),
        title = "合并的对话"
    )
    println("已合并线程，新线程ID: $mergedThreadId")

    // 获取合并后的消息
    val mergedMessages = memory.getMessages(mergedThreadId)
    println("合并后的消息数量: ${mergedMessages.size}")

    println("\n记忆管理器示例完成")
}
