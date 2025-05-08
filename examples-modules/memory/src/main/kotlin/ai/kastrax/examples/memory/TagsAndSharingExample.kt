package ai.kastrax.examples.memory

import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.openai.openAi
import ai.kastrax.memory.api.MemoryTag
import ai.kastrax.memory.api.MessageRole
import ai.kastrax.memory.impl.EnhancedMemory
import ai.kastrax.memory.impl.SimpleMessage
import ai.kastrax.memory.impl.enhancedMemory
import kotlinx.coroutines.runBlocking

/**
 * 标签和线程共享示例，展示如何使用标签管理器和线程共享功能。
 */
fun main() = runBlocking {
    println("KastraX 标签和线程共享示例")
    println("-------------------")
    
    // 创建增强型内存，启用标签管理器和线程共享
    val memory = enhancedMemory {
        lastMessages(10)
        tagManager(true)
        threadSharing(true)
    }
    
    // 创建使用标签和线程共享的Agent
    val agent = agent {
        name = "TagsAndSharingAgent"
        instructions = """
            你是一个具有标签和线程共享能力的助手。你可以使用标签对消息进行分类，并在不同线程之间共享消息。
        """.trimIndent()
        model = openAi("gpt-4o")
        memory = memory
    }
    
    // 创建两个线程
    val threadId1 = memory.createThread("Python讨论")
    val threadId2 = memory.createThread("Java讨论")
    
    println("创建了两个线程:")
    println("线程1: $threadId1 (Python讨论)")
    println("线程2: $threadId2 (Java讨论)")
    
    // 在第一个线程中添加消息
    val pythonMessages = listOf(
        SimpleMessage(
            role = MessageRole.USER,
            content = "Python是一种简单易学的编程语言，适合初学者。"
        ),
        SimpleMessage(
            role = MessageRole.ASSISTANT,
            content = "是的，Python语法简洁，有丰富的库和框架，非常适合初学者和数据科学领域。"
        ),
        SimpleMessage(
            role = MessageRole.USER,
            content = "Python的主要优势是什么？"
        ),
        SimpleMessage(
            role = MessageRole.ASSISTANT,
            content = "Python的主要优势包括：简洁的语法，丰富的库和框架，强大的社区支持，以及在数据科学、机器学习和Web开发等领域的广泛应用。"
        )
    )
    
    // 在第二个线程中添加消息
    val javaMessages = listOf(
        SimpleMessage(
            role = MessageRole.USER,
            content = "Java是一种面向对象的编程语言，广泛用于企业级应用开发。"
        ),
        SimpleMessage(
            role = MessageRole.ASSISTANT,
            content = "是的，Java是一种强类型、面向对象的编程语言，具有'一次编写，到处运行'的特性，广泛用于企业级应用开发。"
        ),
        SimpleMessage(
            role = MessageRole.USER,
            content = "Java的主要优势是什么？"
        ),
        SimpleMessage(
            role = MessageRole.ASSISTANT,
            content = "Java的主要优势包括：平台独立性，强大的面向对象特性，丰富的API和框架，良好的安全性，以及在企业级应用和Android开发中的广泛应用。"
        )
    )
    
    // 保存消息并添加标签
    println("\n保存消息并添加标签:")
    
    // 保存Python线程消息
    for (message in pythonMessages) {
        val messageId = memory.saveMessage(message, threadId1)
        
        // 添加标签
        if (message.role == MessageRole.USER) {
            (memory as EnhancedMemory).addTagToMessage(
                messageId,
                MemoryTag(
                    name = "language",
                    value = "Python",
                    color = "#3572A5"
                )
            )
        }
        
        if (message.content.contains("优势")) {
            (memory as EnhancedMemory).addTagToMessage(
                messageId,
                MemoryTag(
                    name = "topic",
                    value = "advantages",
                    color = "#FF9800"
                )
            )
        }
    }
    
    // 保存Java线程消息
    for (message in javaMessages) {
        val messageId = memory.saveMessage(message, threadId2)
        
        // 添加标签
        if (message.role == MessageRole.USER) {
            (memory as EnhancedMemory).addTagToMessage(
                messageId,
                MemoryTag(
                    name = "language",
                    value = "Java",
                    color = "#B07219"
                )
            )
        }
        
        if (message.content.contains("优势")) {
            (memory as EnhancedMemory).addTagToMessage(
                messageId,
                MemoryTag(
                    name = "topic",
                    value = "advantages",
                    color = "#FF9800"
                )
            )
        }
    }
    
    // 根据标签搜索消息
    println("\n根据标签搜索消息:")
    
    // 搜索Python相关消息
    val pythonTaggedMessages = (memory as EnhancedMemory).searchMessagesByTag(
        threadId1,
        tagName = "language",
        tagValue = "Python"
    )
    
    println("Python相关消息 (${pythonTaggedMessages.size}):")
    pythonTaggedMessages.forEach { message ->
        println("- ${message.message.role}: ${message.message.content}")
    }
    
    // 搜索优势相关消息
    val advantagesTaggedMessages = (memory as EnhancedMemory).searchMessagesByTag(
        threadId1,
        tagName = "topic",
        tagValue = "advantages"
    ) + (memory as EnhancedMemory).searchMessagesByTag(
        threadId2,
        tagName = "topic",
        tagValue = "advantages"
    )
    
    println("\n优势相关消息 (${advantagesTaggedMessages.size}):")
    advantagesTaggedMessages.forEach { message ->
        println("- ${message.message.role}: ${message.message.content}")
    }
    
    // 创建共享线程
    println("\n创建共享线程:")
    val sharedThreadId = (memory as EnhancedMemory).createSharedThread(
        "编程语言比较",
        listOf(threadId1, threadId2)
    )
    println("共享线程ID: $sharedThreadId")
    
    // 获取共享线程中的消息
    val sharedMessages = memory.getMessages(sharedThreadId)
    println("\n共享线程中的消息 (${sharedMessages.size}):")
    sharedMessages.forEach { message ->
        println("- ${message.message.role}: ${message.message.content}")
    }
    
    // 在共享线程中添加新消息
    println("\n在共享线程中添加新消息:")
    val newMessage = SimpleMessage(
        role = MessageRole.USER,
        content = "Python和Java各有优势，选择哪种语言取决于具体的应用场景和需求。"
    )
    
    val newMessageId = memory.saveMessage(newMessage, sharedThreadId)
    
    // 添加标签
    (memory as EnhancedMemory).addTagToMessage(
        newMessageId,
        MemoryTag(
            name = "topic",
            value = "comparison",
            color = "#4CAF50"
        )
    )
    
    // 生成回复
    val response = agent.generate(newMessage.content, threadId = sharedThreadId)
    println("助手: ${response.content}")
    
    // 添加访问控制
    println("\n添加访问控制:")
    val userId1 = "user123"
    val userId2 = "user456"
    
    (memory as EnhancedMemory).addUserToThreadAccess(sharedThreadId, userId1)
    println("用户 $userId1 已添加到共享线程的访问控制列表")
    
    // 检查访问权限
    val hasAccess1 = (memory as EnhancedMemory).hasAccessToThread(sharedThreadId, userId1)
    val hasAccess2 = (memory as EnhancedMemory).hasAccessToThread(sharedThreadId, userId2)
    
    println("用户 $userId1 是否有权限访问共享线程: $hasAccess1")
    println("用户 $userId2 是否有权限访问共享线程: $hasAccess2")
}
