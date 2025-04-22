package ai.kastrax.memory.impl

import ai.kastrax.memory.api.Message
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
        val memory = enhancedMemory {
            lastMessages(5)
        }
        
        // 创建线程
        val threadId = memory.createThread("Test Thread")
        assertNotNull(threadId)
        
        // 保存消息
        val messageId1 = memory.saveMessage(
            Message(role = "user", content = "Hello, how are you?"),
            threadId
        )
        assertNotNull(messageId1)
        
        val messageId2 = memory.saveMessage(
            Message(role = "assistant", content = "I'm doing well, thank you!"),
            threadId
        )
        assertNotNull(messageId2)
        
        // 获取消息
        val messages = memory.getMessages(threadId)
        assertEquals(2, messages.size)
        assertEquals("user", messages[0].message.role)
        assertEquals("Hello, how are you?", messages[0].message.content)
        assertEquals("assistant", messages[1].message.role)
        assertEquals("I'm doing well, thank you!", messages[1].message.content)
    }
    
    @Test
    fun `test memory processors`() = runTest {
        // 创建增强型内存，添加令牌限制器
        val memory = enhancedMemory {
            lastMessages(10)
            processor(TokenLimiter(100))
        }
        
        // 创建线程
        val threadId = memory.createThread("Test Thread")
        
        // 保存多条消息
        for (i in 1..10) {
            memory.saveMessage(
                Message(role = if (i % 2 == 0) "assistant" else "user", content = "Message $i with some additional text to increase token count"),
                threadId
            )
        }
        
        // 获取消息，应该被限制
        val messages = memory.getMessages(threadId)
        assertTrue(messages.size < 10, "应该被令牌限制器限制")
    }
    
    @Test
    fun `test tool call filter`() = runTest {
        // 创建增强型内存，添加工具调用过滤器
        val memory = enhancedMemory {
            lastMessages(10)
            processor(ToolCallFilter())
        }
        
        // 创建线程
        val threadId = memory.createThread("Test Thread")
        
        // 保存带工具调用的消息
        memory.saveMessage(
            Message(
                role = "assistant",
                content = "Let me calculate that for you.",
                toolCalls = listOf(
                    Message.ToolCall(
                        id = "call1",
                        name = "calculator",
                        arguments = """{"expression": "2+2"}"""
                    )
                )
            ),
            threadId
        )
        
        // 保存工具调用结果
        memory.saveMessage(
            Message(
                role = "tool",
                content = "4",
                toolCallId = "call1"
            ),
            threadId
        )
        
        // 保存普通消息
        memory.saveMessage(
            Message(role = "user", content = "Thanks!"),
            threadId
        )
        
        // 获取消息，工具调用应该被过滤
        val messages = memory.getMessages(threadId)
        assertEquals(1, messages.size, "工具调用和结果应该被过滤")
        assertEquals("user", messages[0].message.role)
        assertEquals("Thanks!", messages[0].message.content)
    }
    
    @Test
    fun `test working memory`() = runTest {
        // 创建增强型内存，启用工作内存
        val memory = enhancedMemory {
            lastMessages(10)
            workingMemory(
                WorkingMemoryConfig(
                    enabled = true,
                    mode = WorkingMemoryMode.TEXT_STREAM,
                    template = """
                        # User Information
                        - Name: Unknown
                        - Location: Unknown
                        
                        # Conversation Context
                        - Topic: Testing
                    """.trimIndent()
                )
            )
        }
        
        // 创建线程
        val threadId = memory.createThread("Test Thread")
        
        // 获取工作内存系统消息
        val systemMessage = (memory as EnhancedMemory).getWorkingMemorySystemMessage(threadId)
        assertNotNull(systemMessage)
        assertTrue(systemMessage.contains("User Information"))
        assertTrue(systemMessage.contains("Topic: Testing"))
    }
    
    @Test
    fun `test semantic search`() = runTest {
        // 创建增强型内存，启用语义搜索
        val memory = enhancedMemory {
            lastMessages(10)
            semanticRecall(true)
            embeddingGenerator(SimpleEmbeddingGenerator())
            vectorStorage(InMemoryVectorStorage())
        }
        
        // 创建线程
        val threadId = memory.createThread("Test Thread")
        
        // 保存消息
        memory.saveMessage(
            Message(role = "user", content = "What is the capital of France?"),
            threadId
        )
        
        memory.saveMessage(
            Message(role = "assistant", content = "The capital of France is Paris."),
            threadId
        )
        
        memory.saveMessage(
            Message(role = "user", content = "What is the population of Tokyo?"),
            threadId
        )
        
        memory.saveMessage(
            Message(role = "assistant", content = "Tokyo is the most populous city in Japan with approximately 14 million people."),
            threadId
        )
        
        // 语义搜索
        val results = memory.semanticSearch(
            query = "Tell me about France",
            threadId = threadId,
            config = SemanticRecallConfig(topK = 2, minScore = 0.1f)
        )
        
        assertTrue(results.isNotEmpty(), "应该返回搜索结果")
    }
}
