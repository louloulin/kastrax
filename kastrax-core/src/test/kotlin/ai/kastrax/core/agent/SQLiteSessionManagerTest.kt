package ai.kastrax.core.agent

import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class SQLiteSessionManagerTest {

    private lateinit var sessionManager: SQLiteSessionManager
    private lateinit var dbPath: String

    @BeforeEach
    fun setup(@TempDir tempDir: Path) {
        dbPath = tempDir.resolve("test_session.db").toString()
        sessionManager = SQLiteSessionManager(dbPath)
    }

    @AfterEach
    fun cleanup() {
        // 删除测试数据库文件
        File(dbPath).delete()
    }

    @Test
    fun `test create and get session`() = runBlocking {
        // 创建会话
        val session = sessionManager.createSession(
            title = "Test Session",
            resourceId = "test-resource",
            metadata = mapOf("key" to "value")
        )

        assertNotNull(session)
        assertEquals("Test Session", session.title)
        assertEquals("test-resource", session.resourceId)
        assertEquals("value", session.metadata["key"])
        assertEquals(0, session.messageCount)

        // 获取会话
        val retrievedSession = sessionManager.getSession(session.id)
        assertNotNull(retrievedSession)
        assertEquals(session.id, retrievedSession?.id)
        assertEquals("Test Session", retrievedSession?.title)
        assertEquals("test-resource", retrievedSession?.resourceId)
        assertEquals("value", retrievedSession?.metadata?.get("key"))
    }

    @Test
    fun `test update session`() = runBlocking {
        // 创建会话
        val session = sessionManager.createSession(
            title = "Original Title",
            resourceId = "original-resource",
            metadata = mapOf("original" to "value")
        )

        // 更新会话
        val updatedSession = sessionManager.updateSession(
            sessionId = session.id,
            updates = mapOf(
                "title" to "Updated Title",
                "metadata" to mapOf("original" to "value", "updated" to "true")
            )
        )

        assertNotNull(updatedSession)
        assertEquals("Updated Title", updatedSession.title)
        assertEquals("original-resource", updatedSession.resourceId)
        assertEquals("value", updatedSession.metadata["original"])
        assertEquals("true", updatedSession.metadata["updated"])
    }

    @Test
    fun `test get sessions by resource`() = runBlocking {
        // 创建会话
        val resourceId = "shared-resource"
        val session1 = sessionManager.createSession(
            title = "Session 1",
            resourceId = resourceId
        )
        val session2 = sessionManager.createSession(
            title = "Session 2",
            resourceId = resourceId
        )

        // 获取资源的会话
        val sessions = sessionManager.getSessionsByResource(resourceId)
        assertEquals(2, sessions.size)
        assertTrue(sessions.any { it.id == session1.id })
        assertTrue(sessions.any { it.id == session2.id })
    }

    @Test
    fun `test save and get messages`() = runBlocking {
        // 创建会话
        val session = sessionManager.createSession(title = "Message Test")

        // 保存消息
        val message1 = LlmMessage(role = LlmMessageRole.USER, content = "Hello")
        val message2 = LlmMessage(role = LlmMessageRole.ASSISTANT, content = "Hi there")

        sessionManager.saveMessage(message1, session.id)
        sessionManager.saveMessage(message2, session.id)

        // 获取消息
        val messages = sessionManager.getMessages(session.id)
        assertEquals(2, messages.size)
        
        // 验证消息内容（注意：消息按时间倒序排列）
        assertEquals(LlmMessageRole.ASSISTANT, messages[0].message.role)
        assertEquals("Hi there", messages[0].message.content)
        assertEquals(LlmMessageRole.USER, messages[1].message.role)
        assertEquals("Hello", messages[1].message.content)

        // 验证会话消息计数已更新
        val updatedSession = sessionManager.getSession(session.id)
        assertEquals(2, updatedSession?.messageCount)
    }

    @Test
    fun `test delete session`() = runBlocking {
        // 创建会话
        val session = sessionManager.createSession(title = "To Be Deleted")

        // 删除会话
        val result = sessionManager.deleteSession(session.id)
        assertTrue(result)

        // 确认会话已删除
        val retrievedSession = sessionManager.getSession(session.id)
        assertNull(retrievedSession)
    }

    @Test
    fun `test message limit`() = runBlocking {
        // 创建会话
        val session = sessionManager.createSession(title = "Limit Test")

        // 保存多条消息
        for (i in 1..10) {
            val message = LlmMessage(
                role = LlmMessageRole.USER,
                content = "Message $i"
            )
            sessionManager.saveMessage(message, session.id)
        }

        // 获取有限数量的消息
        val limitedMessages = sessionManager.getMessages(session.id, limit = 5)
        assertEquals(5, limitedMessages.size)
        
        // 验证是最新的5条消息
        for (i in 0 until 5) {
            assertEquals("Message ${10 - i}", limitedMessages[i].message.content)
        }
    }
}
