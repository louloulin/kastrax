package ai.kastrax.memory.impl

import ai.kastrax.memory.api.MemoryPriority
import ai.kastrax.memory.api.MemoryPriorityConfig
import ai.kastrax.memory.api.MemoryProcessorOptions
import ai.kastrax.memory.api.MessageRole
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.days

class MemoryPriorityTest {

    private lateinit var memory: MemoryImpl
    private lateinit var storage: InMemoryStorage
    private lateinit var priorityProcessor: BasicMemoryPriorityProcessor
    private lateinit var priorityConfig: MemoryPriorityConfig

    @BeforeEach
    fun setup() {
        storage = InMemoryStorage()
        priorityConfig = MemoryPriorityConfig(
            enablePriority = true,
            defaultPriority = MemoryPriority.MEDIUM,
            priorityDecayRate = 0.5,
            retentionPeriod = mapOf(
                MemoryPriority.LOW to 1,
                MemoryPriority.MEDIUM to 7,
                MemoryPriority.HIGH to 30,
                MemoryPriority.CRITICAL to 90
            )
        )
        priorityProcessor = BasicMemoryPriorityProcessor(storage)
        memory = MemoryImpl(
            storage = storage,
            priorityConfig = priorityConfig
        )
    }

    @Test
    fun `test save message with priority`() = runBlocking {
        // 创建线程
        val threadId = memory.createThread("Test Thread")

        // 保存消息并指定优先级
        val messageId = memory.saveMessage(
            message = SimpleMessage(
                role = MessageRole.USER,
                content = "This is a high priority message"
            ),
            threadId = threadId,
            priority = MemoryPriority.HIGH
        )

        // 验证消息已保存并具有正确的优先级
        val priority = memory.getMessagePriority(messageId)
        assertEquals(MemoryPriority.HIGH, priority)

        // 获取消息并验证优先级
        val messages = memory.getMessages(threadId, 10)
        assertEquals(1, messages.size)
        assertEquals(MemoryPriority.HIGH, messages[0].priority)
    }

    @Test
    fun `test automatic priority calculation`() = runBlocking {
        // 创建线程
        val threadId = memory.createThread("Test Thread")

        // 保存系统消息（应该是关键优先级）
        val systemMessageId = memory.saveMessage(
            message = SimpleMessage(
                role = MessageRole.SYSTEM,
                content = "System instruction"
            ),
            threadId = threadId
        )

        // 保存用户消息（应该是高优先级）
        val userMessageId = memory.saveMessage(
            message = SimpleMessage(
                role = MessageRole.USER,
                content = "User message"
            ),
            threadId = threadId
        )

        // 保存助手消息（应该是中等优先级）
        val assistantMessageId = memory.saveMessage(
            message = SimpleMessage(
                role = MessageRole.ASSISTANT,
                content = "Assistant response"
            ),
            threadId = threadId
        )

        // 保存工具消息（应该是低优先级）
        val toolMessageId = memory.saveMessage(
            message = SimpleMessage(
                role = MessageRole.TOOL,
                content = "Tool result"
            ),
            threadId = threadId
        )

        // 验证优先级
        assertEquals(MemoryPriority.CRITICAL, memory.getMessagePriority(systemMessageId))
        assertEquals(MemoryPriority.HIGH, memory.getMessagePriority(userMessageId))
        assertEquals(MemoryPriority.MEDIUM, memory.getMessagePriority(assistantMessageId))
        assertEquals(MemoryPriority.LOW, memory.getMessagePriority(toolMessageId))
    }

    @Test
    fun `test update message priority`() = runBlocking {
        // 创建线程
        val threadId = memory.createThread("Test Thread")

        // 保存消息
        val messageId = memory.saveMessage(
            message = SimpleMessage(
                role = MessageRole.USER,
                content = "Test message"
            ),
            threadId = threadId
        )

        // 验证初始优先级
        assertEquals(MemoryPriority.HIGH, memory.getMessagePriority(messageId))

        // 更新优先级
        val updateResult = memory.updateMessagePriority(messageId, MemoryPriority.CRITICAL)
        assertTrue(updateResult)

        // 验证更新后的优先级
        assertEquals(MemoryPriority.CRITICAL, memory.getMessagePriority(messageId))
    }

    @Test
    fun `test priority decay`() = runBlocking {
        // 创建线程
        val threadId = memory.createThread("Test Thread")

        // 保存高优先级消息
        val messageId = memory.saveMessage(
            message = SimpleMessage(
                role = MessageRole.USER,
                content = "Important message"
            ),
            threadId = threadId,
            priority = MemoryPriority.HIGH
        )

        // 模拟消息访问信息
        val lastAccessed = Clock.System.now().minus(2.days)
        storage.updateMessageAccess(messageId, lastAccessed, 1)

        // 应用优先级衰减
        val decayedCount = memory.applyPriorityDecay(priorityConfig)
        assertEquals(1, decayedCount)

        // 验证优先级已降低
        val newPriority = memory.getMessagePriority(messageId)
        assertEquals(MemoryPriority.MEDIUM, newPriority)
    }

    @Test
    fun `test cleanup low priority messages`() = runBlocking {
        // 创建线程
        val threadId = memory.createThread("Test Thread")

        // 保存低优先级消息
        val messageId = memory.saveMessage(
            message = SimpleMessage(
                role = MessageRole.TOOL,
                content = "Temporary result"
            ),
            threadId = threadId
        )

        // 验证消息已保存
        val messages = memory.getMessages(threadId, 10)
        assertEquals(1, messages.size)

        // 模拟消息创建时间为2天前
        val createdAt = Clock.System.now().minus(2.days)
        val memoryMessage = messages[0].copy(createdAt = createdAt)
        storage.saveMessage(memoryMessage)

        // 清理低优先级消息（保留期为1天）
        val cleanedCount = memory.cleanupLowPriorityMessages(priorityConfig)
        assertEquals(1, cleanedCount)

        // 验证消息已被清理
        val remainingMessages = memory.getMessages(threadId, 10)
        assertEquals(0, remainingMessages.size)
    }

    @Test
    fun `test priority filter processor`() = runBlocking {
        // 创建线程
        val threadId = memory.createThread("Test Thread")

        // 保存不同优先级的消息
        memory.saveMessage(
            message = SimpleMessage(role = MessageRole.TOOL, content = "Low priority"),
            threadId = threadId,
            priority = MemoryPriority.LOW
        )

        memory.saveMessage(
            message = SimpleMessage(role = MessageRole.ASSISTANT, content = "Medium priority"),
            threadId = threadId,
            priority = MemoryPriority.MEDIUM
        )

        memory.saveMessage(
            message = SimpleMessage(role = MessageRole.USER, content = "High priority"),
            threadId = threadId,
            priority = MemoryPriority.HIGH
        )

        // 创建优先级过滤器
        val priorityFilter = PriorityFilter(minPriority = MemoryPriority.MEDIUM)

        // 获取消息并应用过滤器
        val allMessages = memory.getMessages(threadId, 10)
        assertEquals(3, allMessages.size)

        val filteredMessages = priorityFilter.process(allMessages, MemoryProcessorOptions())
        assertEquals(2, filteredMessages.size)

        // 验证只有中等和高优先级的消息被保留
        val priorities = filteredMessages.mapNotNull { it.priority }.toSet()
        assertTrue(priorities.contains(MemoryPriority.MEDIUM))
        assertTrue(priorities.contains(MemoryPriority.HIGH))
        assertFalse(priorities.contains(MemoryPriority.LOW))
    }

    @Test
    fun `test priority sorter processor`() = runBlocking {
        // 创建线程
        val threadId = memory.createThread("Test Thread")

        // 保存不同优先级的消息
        memory.saveMessage(
            message = SimpleMessage(role = MessageRole.TOOL, content = "Low priority"),
            threadId = threadId,
            priority = MemoryPriority.LOW
        )

        memory.saveMessage(
            message = SimpleMessage(role = MessageRole.ASSISTANT, content = "Medium priority"),
            threadId = threadId,
            priority = MemoryPriority.MEDIUM
        )

        memory.saveMessage(
            message = SimpleMessage(role = MessageRole.USER, content = "High priority"),
            threadId = threadId,
            priority = MemoryPriority.HIGH
        )

        // 创建优先级排序器
        val prioritySorter = PrioritySorter(prioritizeRecent = false)

        // 获取消息并应用排序器
        val allMessages = memory.getMessages(threadId, 10)
        val sortedMessages = prioritySorter.process(allMessages, MemoryProcessorOptions())

        // 验证消息按优先级排序
        assertEquals(3, sortedMessages.size)
        assertEquals(MemoryPriority.HIGH, sortedMessages[0].priority)
        assertEquals(MemoryPriority.MEDIUM, sortedMessages[1].priority)
        assertEquals(MemoryPriority.LOW, sortedMessages[2].priority)
    }
}
