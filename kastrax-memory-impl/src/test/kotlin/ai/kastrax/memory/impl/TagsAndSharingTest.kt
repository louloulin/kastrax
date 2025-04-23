package ai.kastrax.memory.impl

import ai.kastrax.memory.api.MemoryTag
import ai.kastrax.memory.api.MessageRole
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class TagsAndSharingTest {

    @Test
    fun `test tag manager functionality`() = runTest {
        // 创建增强型内存，启用标签管理器
        val memory = enhancedMemory {
            storage(MemoryFactory.createInMemoryStorage())
            lastMessages(10)
            tagManager(true)
        }

        // 创建线程
        val threadId = memory.createThread("Test Thread")

        // 保存消息
        val message1 = SimpleMessage(
            role = MessageRole.USER,
            content = "This is a test message about Python."
        )
        val message2 = SimpleMessage(
            role = MessageRole.ASSISTANT,
            content = "Python is a popular programming language."
        )
        val message3 = SimpleMessage(
            role = MessageRole.USER,
            content = "Java is another programming language."
        )

        val messageId1 = memory.saveMessage(message1, threadId)
        val messageId2 = memory.saveMessage(message2, threadId)
        val messageId3 = memory.saveMessage(message3, threadId)

        // 添加标签
        (memory as EnhancedMemory).addTagToMessage(
            messageId1,
            MemoryTag(
                name = "language",
                value = "Python",
                color = "#3572A5"
            )
        )

        (memory as EnhancedMemory).addTagToMessage(
            messageId2,
            MemoryTag(
                name = "language",
                value = "Python",
                color = "#3572A5"
            )
        )

        (memory as EnhancedMemory).addTagToMessage(
            messageId3,
            MemoryTag(
                name = "language",
                value = "Java",
                color = "#B07219"
            )
        )

        // 获取消息标签
        val tags1 = memory.getMessageTags(messageId1)
        assertEquals(1, tags1.size)
        assertEquals("language", tags1[0].name)
        assertEquals("Python", tags1[0].value)

        // 根据标签搜索消息
        val pythonMessages = memory.searchMessagesByTag(
            threadId,
            tagName = "language",
            tagValue = "Python"
        )

        assertEquals(2, pythonMessages.size)
        assertTrue(pythonMessages.any { it.id == messageId1 })
        assertTrue(pythonMessages.any { it.id == messageId2 })

        val javaMessages = memory.searchMessagesByTag(
            threadId,
            tagName = "language",
            tagValue = "Java"
        )

        assertEquals(1, javaMessages.size)
        assertTrue(javaMessages.any { it.id == messageId3 })
    }

    @Test
    fun `test thread sharing functionality`() = runTest {
        // 创建增强型内存，启用线程共享
        val memory = enhancedMemory {
            storage(MemoryFactory.createInMemoryStorage())
            lastMessages(10)
            threadSharing(true)
        }

        // 创建两个线程
        val threadId1 = memory.createThread("Thread 1")
        val threadId2 = memory.createThread("Thread 2")

        // 在第一个线程中添加消息
        val message1 = SimpleMessage(
            role = MessageRole.USER,
            content = "Message in thread 1"
        )
        memory.saveMessage(message1, threadId1)

        // 在第二个线程中添加消息
        val message2 = SimpleMessage(
            role = MessageRole.USER,
            content = "Message in thread 2"
        )
        memory.saveMessage(message2, threadId2)

        // 创建共享线程
        val sharedThreadId = (memory as EnhancedMemory).createSharedThread(
            "Shared Thread",
            listOf(threadId1, threadId2)
        )

        // 获取共享线程中的消息
        val sharedMessages = memory.getMessages(sharedThreadId)
        assertEquals(2, sharedMessages.size)
        assertTrue(sharedMessages.any { it.message.content == "Message in thread 1" })
        assertTrue(sharedMessages.any { it.message.content == "Message in thread 2" })

        // 在共享线程中添加新消息
        val message3 = SimpleMessage(
            role = MessageRole.USER,
            content = "Message in shared thread"
        )
        memory.saveMessage(message3, sharedThreadId)

        // 获取更新后的共享线程消息
        val updatedSharedMessages = memory.getMessages(sharedThreadId)
        assertEquals(3, updatedSharedMessages.size)
        assertTrue(updatedSharedMessages.any { it.message.content == "Message in shared thread" })
    }

    @Test
    fun `test thread access control`() = runTest {
        // 创建增强型内存，启用线程共享
        val memory = enhancedMemory {
            storage(MemoryFactory.createInMemoryStorage())
            lastMessages(10)
            threadSharing(true)
        }

        // 创建线程
        val threadId = memory.createThread("Test Thread")

        // 添加用户到访问控制列表
        val userId1 = "user123"
        val userId2 = "user456"

        (memory as EnhancedMemory).addUserToThreadAccess(threadId, userId1)

        // 检查访问权限
        val hasAccess1 = memory.hasAccessToThread(threadId, userId1)
        val hasAccess2 = memory.hasAccessToThread(threadId, userId2)

        assertTrue(hasAccess1)
        assertFalse(hasAccess2)

        // 移除用户
        memory.removeUserFromThreadAccess(threadId, userId1)

        // 再次检查访问权限
        val hasAccess1After = memory.hasAccessToThread(threadId, userId1)
        assertTrue(hasAccess1After) // 现在应该返回true，因为访问控制列表为空
    }
}
