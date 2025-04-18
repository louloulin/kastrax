package ai.kastrax.memory

import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MemoryTest {
    
    @Test
    fun `test memory with in-memory storage`() = runTest {
        // 创建内存系统
        val memory = memory {
            storage = inMemoryStorage()
            lastMessages = 5
            semanticRecall = false
        }
        
        // 创建线程
        val threadId = memory.createThread("Test Conversation")
        
        // 保存消息
        val userMessage1 = LlmMessage(role = LlmMessageRole.USER, content = "Hello")
        val assistantMessage1 = LlmMessage(role = LlmMessageRole.ASSISTANT, content = "Hi there!")
        val userMessage2 = LlmMessage(role = LlmMessageRole.USER, content = "How are you?")
        val assistantMessage2 = LlmMessage(role = LlmMessageRole.ASSISTANT, content = "I'm doing well, thanks for asking!")
        
        memory.saveMessage(userMessage1, threadId)
        memory.saveMessage(assistantMessage1, threadId)
        memory.saveMessage(userMessage2, threadId)
        memory.saveMessage(assistantMessage2, threadId)
        
        // 获取消息
        val messages = memory.getMessages(threadId)
        assertEquals(4, messages.size)
        
        // 验证消息顺序（最新的在前）
        assertEquals(LlmMessageRole.ASSISTANT, messages[0].message.role)
        assertEquals("I'm doing well, thanks for asking!", messages[0].message.content)
        
        // 获取线程
        val thread = memory.getThread(threadId)
        assertNotNull(thread)
        assertEquals("Test Conversation", thread.title)
        assertEquals(4, thread.messageCount)
        
        // 列出线程
        val threads = memory.listThreads()
        assertEquals(1, threads.size)
        assertEquals(threadId, threads[0].id)
        
        // 搜索消息
        val searchResults = memory.searchMessages("thanks", threadId)
        assertEquals(1, searchResults.size)
        assertEquals("I'm doing well, thanks for asking!", searchResults[0].message.content)
        
        // 删除线程
        val deleted = memory.deleteThread(threadId)
        assertTrue(deleted)
        
        // 验证线程已删除
        val deletedThreads = memory.listThreads()
        assertEquals(0, deletedThreads.size)
    }
    
    @Test
    fun `test memory DSL`() = runTest {
        // 使用DSL创建内存系统
        val memory = memory {
            storage = inMemoryStorage()
            lastMessages = 10
            semanticRecall = true
        }
        
        // 创建线程
        val threadId = memory.createThread()
        
        // 保存系统消息
        val systemMessage = LlmMessage(role = LlmMessageRole.SYSTEM, content = "You are a helpful assistant.")
        memory.saveMessage(systemMessage, threadId)
        
        // 保存用户消息
        val userMessage = LlmMessage(role = LlmMessageRole.USER, content = "Hello")
        memory.saveMessage(userMessage, threadId)
        
        // 获取消息
        val messages = memory.getMessages(threadId)
        assertEquals(2, messages.size)
        
        // 验证系统消息
        val retrievedSystemMessage = messages.find { it.message.role == LlmMessageRole.SYSTEM }
        assertNotNull(retrievedSystemMessage)
        assertEquals("You are a helpful assistant.", retrievedSystemMessage.message.content)
    }
}
