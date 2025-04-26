package ai.kastrax.memory.impl

import ai.kastrax.memory.api.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MemoryManagerImplTest {

    private lateinit var memory: Memory
    private lateinit var manager: MemoryManager
    private lateinit var storage: MemoryStorage
    private lateinit var threadId: String

    @BeforeEach
    fun setup() = runBlocking {
        // 创建内存存储
        storage = InMemoryStorage()

        // 创建内存和管理器
        memory = MemoryImpl(storage)
        manager = MemoryManagerImpl(storage)

        // 创建测试线程
        threadId = memory.createThread("测试线程")

        // 添加测试消息
        addTestMessages()
    }

    private suspend fun addTestMessages() {
        // 创建一个简单的消息适配器
        class TestMessage(
            override val role: MessageRole,
            override val content: String,
            override val name: String? = null,
            override val toolCalls: List<ToolCall> = emptyList(),
            override val toolCallId: String? = null
        ) : Message

        // 添加系统消息
        memory.saveMessage(
            TestMessage(
                role = MessageRole.SYSTEM,
                content = "你是一个有帮助的助手。"
            ),
            threadId
        )

        // 添加用户消息
        memory.saveMessage(
            TestMessage(
                role = MessageRole.USER,
                content = "你好，请介绍一下自己。"
            ),
            threadId
        )

        // 添加助手消息
        memory.saveMessage(
            TestMessage(
                role = MessageRole.ASSISTANT,
                content = "你好！我是一个AI助手，我可以回答问题、提供信息和帮助你完成各种任务。有什么我可以帮你的吗？"
            ),
            threadId
        )

        // 添加带优先级的用户消息
        memory.saveMessage(
            TestMessage(
                role = MessageRole.USER,
                content = "我想了解一下人工智能的发展历史。"
            ),
            threadId,
            MemoryPriority.HIGH
        )

        // 添加带优先级的助手消息
        memory.saveMessage(
            TestMessage(
                role = MessageRole.ASSISTANT,
                content = "人工智能的发展历史可以追溯到20世纪50年代。1956年的达特茅斯会议被认为是人工智能研究的正式开始。之后经历了几次起伏，包括两次AI冬天。近年来，由于深度学习的突破，AI领域取得了显著进展。"
            ),
            threadId,
            MemoryPriority.HIGH
        )
    }

    @Test
    fun `test query messages with role filter`() = runBlocking {
        // 使用角色过滤器查询消息
        val userMessages = manager.queryMessages(
            threadId,
            MemoryQueryOptions(
                filters = listOf(RoleFilter(roles = listOf(MessageRole.USER)))
            )
        )

        // 验证结果
        assertEquals(2, userMessages.size)
        assertTrue(userMessages.all { it.message.role == MessageRole.USER })
    }

    @Test
    fun `test query messages with content filter`() = runBlocking {
        // 使用内容过滤器查询消息
        val aiMessages = manager.queryMessages(
            threadId,
            MemoryQueryOptions(
                filters = listOf(ContentFilter(query = "人工智能", matchType = ContentMatchType.CONTAINS))
            )
        )

        // 验证结果
        assertTrue(aiMessages.isNotEmpty())
        assertTrue(aiMessages.all { it.message.content.contains("人工智能") })
    }

    @Test
    fun `test get message context`() = runBlocking {
        // 获取一个消息ID
        val messages = memory.getMessages(threadId)
        val messageId = messages.find { it.message.role == MessageRole.USER }?.id

        assertNotNull(messageId)

        // 获取消息上下文
        val contextMessages = manager.getMessageContext(
            ContextSelector(
                messageId = messageId!!,
                beforeCount = 1,
                afterCount = 1
            )
        )

        // 验证结果
        assertEquals(3, contextMessages.size)
        assertTrue(contextMessages.any { it.id == messageId })
    }

    @Test
    fun `test get message stats`() = runBlocking {
        // 获取消息统计
        val stats = manager.getMessageStats(threadId)

        // 验证结果
        assertEquals(5, stats.totalMessages)
        assertEquals(2, stats.messagesByRole[MessageRole.USER])
        assertEquals(2, stats.messagesByRole[MessageRole.ASSISTANT])
        assertEquals(1, stats.messagesByRole[MessageRole.SYSTEM])
        assertTrue(stats.averageMessageLength > 0)
    }

    @Test
    fun `test export and import thread`() = runBlocking {
        // 导出线程
        val jsonExport = manager.exportThread(threadId, ExportFormat.JSON)

        // 验证导出结果
        assertTrue(jsonExport.contains("\"线程\""))
        assertTrue(jsonExport.contains("\"消息\""))

        // 导入线程
        val newThreadId = manager.importThread(jsonExport, ExportFormat.JSON)

        // 验证导入结果
        assertNotEquals(threadId, newThreadId)

        // 获取导入的消息
        val importedMessages = memory.getMessages(newThreadId)

        // 验证导入的消息数量
        assertEquals(1, importedMessages.size) // 现在只创建一条系统消息
    }

    @Test
    fun `test batch update messages`() = runBlocking {
        // 批量更新消息
        val updatedCount = manager.batchUpdateMessages(
            options = MessageBatchOptions(
                threadId = threadId,
                filters = listOf(RoleFilter(roles = listOf(MessageRole.USER)))
            ),
            updates = MessageUpdateOptions(
                priority = MemoryPriority.CRITICAL
            )
        )

        // 验证更新结果
        assertEquals(2, updatedCount)

        // 获取更新后的用户消息
        val updatedUserMessages = manager.queryMessages(
            threadId,
            MemoryQueryOptions(
                filters = listOf(RoleFilter(roles = listOf(MessageRole.USER)))
            )
        )

        // 验证优先级已更新
        assertTrue(updatedUserMessages.all { it.priority == MemoryPriority.CRITICAL })
    }

    @Test
    fun `test merge threads`() = runBlocking {
        // 创建第二个线程
        val threadId2 = memory.createThread("第二个线程")

        // 添加消息到第二个线程
        class TestMessage(
            override val role: MessageRole,
            override val content: String,
            override val name: String? = null,
            override val toolCalls: List<ToolCall> = emptyList(),
            override val toolCallId: String? = null
        ) : Message

        memory.saveMessage(
            TestMessage(
                role = MessageRole.USER,
                content = "这是第二个线程的消息。"
            ),
            threadId2
        )

        // 合并线程
        val mergedThreadId = manager.mergeThreads(
            sourceThreadIds = listOf(threadId, threadId2),
            title = "合并的线程"
        )

        // 验证合并结果
        assertNotEquals(threadId, mergedThreadId)
        assertNotEquals(threadId2, mergedThreadId)

        // 获取合并后的消息
        val mergedMessages = memory.getMessages(mergedThreadId)

        // 验证合并后的消息数量
        assertEquals(6, mergedMessages.size) // 5 + 1
    }
}
