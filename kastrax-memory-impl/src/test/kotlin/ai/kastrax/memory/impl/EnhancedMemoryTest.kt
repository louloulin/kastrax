package ai.kastrax.memory.impl

import ai.kastrax.memory.api.Memory
import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MessageRole
import ai.kastrax.memory.api.SemanticRecallConfig
import ai.kastrax.memory.api.TokenLimiter
import ai.kastrax.memory.api.ToolCallFilter
import ai.kastrax.memory.api.WorkingMemoryConfig
import ai.kastrax.memory.api.WorkingMemoryMode
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EnhancedMemoryTest {

    @Test
    fun `test basic memory operations`() = runTest {
        // 创建增强型内存
        val memory = createTestMemory {
            storage(MemoryFactory.createInMemoryStorage())
            lastMessages(10)
        }

        // 创建线程
        val threadId = memory.createThread("Test Thread")

        // 保存消息
        val messageId1 = memory.saveMessage(
            SimpleMessage(role = MessageRole.USER, content = "Hello, how are you?"),
            threadId
        )

        val messageId2 = memory.saveMessage(
            SimpleMessage(role = MessageRole.ASSISTANT, content = "I'm doing well, thank you!"),
            threadId
        )

        // 获取消息
        val messages = memory.getMessages(threadId)

        // 验证结果
        assertEquals(2, messages.size)
        assertEquals(messageId1, messages[0].id)
        assertEquals(messageId2, messages[1].id)
        assertEquals("Hello, how are you?", messages[0].message.content)
        assertEquals("I'm doing well, thank you!", messages[1].message.content)
    }

    @Test
    fun `test memory compression`() = runTest {
        // 创建带压缩功能的增强型内存
        val memory = createTestMemory {
            storage(MemoryFactory.createInMemoryStorage())
            lastMessages(5)
            memoryCompressor(MockMemoryCompressor())
            compressionConfig(ai.kastrax.memory.api.MemoryCompressionConfig(
                enabled = true,
                threshold = 10,
                preserveSystemMessages = true,
                preserveRecentMessages = 5
            ))
        }

        // 创建线程
        val threadId = memory.createThread("Test Thread")

        // 保存多条消息，触发压缩
        for (i in 1..20) {
            memory.saveMessage(
                SimpleMessage(role = if (i % 2 == 0) MessageRole.ASSISTANT else MessageRole.USER, content = "Message $i with some additional text to increase token count"),
                threadId
            )
        }

        // 获取消息
        val messages = memory.getMessages(threadId)

        // 验证结果
        assertTrue(messages.size <= 20) // 压缩后的消息数量应该小于等于20

        // 打印所有消息的内容，以便调试
        println("Messages size: ${messages.size}")
        messages.forEach { message ->
            println("Message in test: ${message.message.content}")
        }

        // 直接检查第一条消息是否包含 Summary
        assertTrue(messages.first().message.content.contains("Summary")) // 第一条消息应该是摘要消息
    }

    @Test
    fun `test memory processors`() = runTest {
        // 创建带处理器的增强型内存
        val memory = createTestMemory {
            storage(MemoryFactory.createInMemoryStorage())
            lastMessages(10)
            processor(TokenLimiter(maxTokens = 100))
            processor(ToolCallFilter())
        }

        // 创建线程
        val threadId = memory.createThread("Test Thread")

        // 保存带工具调用的消息
        memory.saveMessage(
            SimpleMessage(
                role = MessageRole.ASSISTANT,
                content = "Let me calculate that for you.",
                toolCalls = listOf(
                    SimpleToolCall(
                        id = "call1",
                        name = "calculator"
                    )
                )
            ),
            threadId
        )

        // 保存工具调用结果
        memory.saveMessage(
            SimpleMessage(
                role = MessageRole.TOOL,
                content = "4",
                toolCallId = "call1"
            ),
            threadId
        )

        // 保存一条正常消息（不包含工具调用）
        memory.saveMessage(
            SimpleMessage(
                role = MessageRole.USER,
                content = "Thank you for the calculation."
            ),
            threadId
        )

        // 获取消息，应用处理器
        val messages = memory.getMessages(
            threadId,
            processors = listOf(ToolCallFilter())
        )

        // 验证结果
        assertEquals(1, messages.size) // 工具调用和结果应该被过滤掉
    }

    @Test
    fun `test working memory`() = runTest {
        // 创建带工作内存的增强型内存
        val memory = createTestMemory {
            storage(MemoryFactory.createInMemoryStorage())
            lastMessages(10)
            workingMemory(
                WorkingMemoryConfig(
                    enabled = true,
                    mode = WorkingMemoryMode.TOOL_CALL,
                    template = """
                        {
                            "memory": "",
                            "context": []
                        }
                    """.trimIndent()
                )
            )
        }

        // 创建线程
        val threadId = memory.createThread("Test Thread")

        // 获取工作内存工具
        val tools = (memory as EnhancedMemory).getWorkingMemoryTools()

        // 验证结果
        assertNotNull(tools["update_working_memory"])
        assertNotNull(tools["get_working_memory"])
    }

    @Test
    fun `test semantic search`() = runTest {
        // 创建带语义搜索的增强型内存
        val memory = createTestMemory {
            storage(MemoryFactory.createInMemoryStorage())
            lastMessages(10)
            semanticRecall(true)
            embeddingGenerator(MockEmbeddingGenerator())
            vectorStorage(InMemoryVectorStorage())
        }

        // 创建线程
        val threadId = memory.createThread("Test Thread")

        // 保存消息
        memory.saveMessage(
            SimpleMessage(role = MessageRole.USER, content = "Python is a programming language."),
            threadId
        )

        memory.saveMessage(
            SimpleMessage(role = MessageRole.ASSISTANT, content = "Yes, Python is widely used in AI and data science."),
            threadId
        )

        memory.saveMessage(
            SimpleMessage(role = MessageRole.USER, content = "Java is another programming language."),
            threadId
        )

        memory.saveMessage(
            SimpleMessage(role = MessageRole.ASSISTANT, content = "Java is popular for enterprise applications."),
            threadId
        )

        // 执行语义搜索
        val results = memory.semanticSearch(
            query = "Tell me about Python",
            threadId = threadId,
            config = SemanticRecallConfig(topK = 2)
        )

        // 验证结果
        assertEquals(2, results.size)
        assertTrue(results[0].message.message.content.contains("Python"))
    }
}
