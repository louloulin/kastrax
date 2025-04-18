package ai.kastrax.memory

import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InMemoryStorageTest {
    
    private lateinit var storage: InMemoryStorage
    
    @BeforeEach
    fun setup() {
        storage = InMemoryStorage()
    }
    
    @Test
    fun `test create and get thread`() = runTest {
        // 创建线程
        val thread = MemoryThread(
            id = "thread-1",
            title = "Test Thread",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val threadId = storage.createThread(thread)
        assertEquals("thread-1", threadId)
        
        // 获取线程
        val retrievedThread = storage.getThread(threadId)
        assertNotNull(retrievedThread)
        assertEquals("Test Thread", retrievedThread.title)
    }
    
    @Test
    fun `test save and get messages`() = runTest {
        // 创建线程
        val thread = MemoryThread(
            id = "thread-1",
            title = "Test Thread",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        storage.createThread(thread)
        
        // 保存消息
        val message1 = MemoryMessage(
            id = "msg-1",
            threadId = "thread-1",
            message = LlmMessage(role = LlmMessageRole.USER, content = "Hello"),
            createdAt = Clock.System.now()
        )
        
        val message2 = MemoryMessage(
            id = "msg-2",
            threadId = "thread-1",
            message = LlmMessage(role = LlmMessageRole.ASSISTANT, content = "Hi there!"),
            createdAt = Clock.System.now()
        )
        
        storage.saveMessage(message1)
        storage.saveMessage(message2)
        
        // 获取消息
        val messages = storage.getMessages("thread-1", 10)
        assertEquals(2, messages.size)
        
        // 验证消息按时间倒序排序
        assertEquals("msg-2", messages[0].id)
        assertEquals("msg-1", messages[1].id)
    }
    
    @Test
    fun `test search messages`() = runTest {
        // 创建线程
        val thread = MemoryThread(
            id = "thread-1",
            title = "Test Thread",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        storage.createThread(thread)
        
        // 保存消息
        val message1 = MemoryMessage(
            id = "msg-1",
            threadId = "thread-1",
            message = LlmMessage(role = LlmMessageRole.USER, content = "Tell me about AI"),
            createdAt = Clock.System.now()
        )
        
        val message2 = MemoryMessage(
            id = "msg-2",
            threadId = "thread-1",
            message = LlmMessage(role = LlmMessageRole.ASSISTANT, content = "AI stands for Artificial Intelligence"),
            createdAt = Clock.System.now()
        )
        
        storage.saveMessage(message1)
        storage.saveMessage(message2)
        
        // 搜索消息
        val results = storage.searchMessages("artificial", "thread-1", 10)
        assertEquals(1, results.size)
        assertEquals("msg-2", results[0].id)
    }
    
    @Test
    fun `test update thread`() = runTest {
        // 创建线程
        val thread = MemoryThread(
            id = "thread-1",
            title = "Test Thread",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        storage.createThread(thread)
        
        // 更新线程
        val success = storage.updateThread("thread-1", mapOf(
            "title" to "Updated Thread",
            "messageCount" to 5
        ))
        
        assertTrue(success)
        
        // 验证更新
        val updatedThread = storage.getThread("thread-1")
        assertNotNull(updatedThread)
        assertEquals("Updated Thread", updatedThread.title)
        assertEquals(5, updatedThread.messageCount)
    }
    
    @Test
    fun `test delete thread`() = runTest {
        // 创建线程
        val thread = MemoryThread(
            id = "thread-1",
            title = "Test Thread",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        storage.createThread(thread)
        
        // 保存消息
        val message = MemoryMessage(
            id = "msg-1",
            threadId = "thread-1",
            message = LlmMessage(role = LlmMessageRole.USER, content = "Hello"),
            createdAt = Clock.System.now()
        )
        storage.saveMessage(message)
        
        // 删除线程
        val success = storage.deleteThread("thread-1")
        assertTrue(success)
        
        // 验证线程和消息都被删除
        val deletedThread = storage.getThread("thread-1")
        assertEquals(null, deletedThread)
        
        val messages = storage.getMessages("thread-1", 10)
        assertEquals(0, messages.size)
    }
    
    @Test
    fun `test list threads`() = runTest {
        // 创建多个线程
        for (i in 1..5) {
            val thread = MemoryThread(
                id = "thread-$i",
                title = "Thread $i",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
            storage.createThread(thread)
        }
        
        // 列出所有线程
        val allThreads = storage.listThreads(10, 0)
        assertEquals(5, allThreads.size)
        
        // 测试分页
        val page1 = storage.listThreads(2, 0)
        assertEquals(2, page1.size)
        
        val page2 = storage.listThreads(2, 2)
        assertEquals(2, page2.size)
        
        val page3 = storage.listThreads(2, 4)
        assertEquals(1, page3.size)
    }
}
