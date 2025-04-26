package ai.kastrax.memory.impl

import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MemoryThread
import ai.kastrax.memory.api.MessageRole
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import redis.clients.jedis.JedisPool
import java.io.File
import java.nio.file.Path
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.times
import javax.sql.DataSource
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

class StorageBackendTest {

    private lateinit var inMemoryStorage: InMemoryStorage
    private lateinit var sqliteStorage: SQLiteMemoryStorage
    private lateinit var sqlitePath: String

    @BeforeEach
    fun setup(@TempDir tempDir: Path) {
        inMemoryStorage = InMemoryStorage()
        sqlitePath = tempDir.resolve("test.db").toString()
        sqliteStorage = SQLiteMemoryStorage(sqlitePath)
    }

    @AfterEach
    fun cleanup() {
        // 删除SQLite测试数据库
        File(sqlitePath).delete()
    }

    @Test
    fun `test in-memory storage basic operations`() = runBlocking {
        // 创建线程
        val threadId = createTestThread(inMemoryStorage)

        // 创建消息
        val messageId = createTestMessage(inMemoryStorage, threadId)

        // 获取消息
        val messages = inMemoryStorage.getMessages(threadId, 10)
        assertEquals(1, messages.size)
        assertEquals(messageId, messages[0].id)
        assertEquals("Test message", messages[0].message.content)

        // 搜索消息
        val searchResults = inMemoryStorage.searchMessages("Test", threadId, 10)
        assertEquals(1, searchResults.size)

        // 获取线程
        val thread = inMemoryStorage.getThread(threadId)
        assertNotNull(thread)
        assertEquals("Test Thread", thread.title)

        // 列出线程
        val threads = inMemoryStorage.listThreads(10, 0)
        assertTrue(threads.isNotEmpty())

        // 更新线程
        val updateSuccess = inMemoryStorage.updateThread(threadId, mapOf(
            "title" to "Updated Thread",
            "messageCount" to 1
        ))
        assertTrue(updateSuccess)

        // 验证更新
        val updatedThread = inMemoryStorage.getThread(threadId)
        assertNotNull(updatedThread)
        assertEquals("Updated Thread", updatedThread.title)
        assertEquals(1, updatedThread.messageCount)

        // 删除线程
        val deleteSuccess = inMemoryStorage.deleteThread(threadId)
        assertTrue(deleteSuccess)
    }

    @Test
    fun `test SQLite storage basic operations`() = runBlocking {
        // 创建线程
        val threadId = createTestThread(sqliteStorage)

        // 创建消息
        val messageId = createTestMessage(sqliteStorage, threadId)

        // 获取消息
        val messages = sqliteStorage.getMessages(threadId, 10)
        assertEquals(1, messages.size)
        assertEquals(messageId, messages[0].id)
        assertEquals("Test message", messages[0].message.content)

        // 搜索消息
        val searchResults = sqliteStorage.searchMessages("Test", threadId, 10)
        assertEquals(1, searchResults.size)

        // 获取线程
        val thread = sqliteStorage.getThread(threadId)
        assertNotNull(thread)
        assertEquals("Test Thread", thread.title)

        // 列出线程
        val threads = sqliteStorage.listThreads(10, 0)
        assertTrue(threads.isNotEmpty())

        // 更新线程
        val updateSuccess = sqliteStorage.updateThread(threadId, mapOf(
            "title" to "Updated Thread",
            "messageCount" to 1
        ))
        assertTrue(updateSuccess)

        // 验证更新
        val updatedThread = sqliteStorage.getThread(threadId)
        assertNotNull(updatedThread)
        assertEquals("Updated Thread", updatedThread.title)
        assertEquals(1, updatedThread.messageCount)

        // 删除线程
        val deleteSuccess = sqliteStorage.deleteThread(threadId)
        assertTrue(deleteSuccess)
    }

    @Test
    @Disabled("Redis serialization issue needs to be fixed")
    fun `test Redis storage with mocks`() = runBlocking {
        // 创建模拟的JedisPool
        val mockJedisPool = mock<JedisPool>()
        val mockJedis = mock<redis.clients.jedis.Jedis>()
        val mockPipeline = mock<redis.clients.jedis.Pipeline>()
        val mockResponse = mock<redis.clients.jedis.Response<String>>()

        // 配置模拟行为
        whenever(mockJedisPool.resource).thenReturn(mockJedis)
        whenever(mockJedis.pipelined()).thenReturn(mockPipeline)
        whenever(mockResponse.get()).thenReturn("{\"id\":\"test-id\",\"threadId\":\"test-thread\",\"role\":\"USER\",\"content\":\"Test message\",\"createdAt\":\"2023-01-01T00:00:00Z\"}")
        whenever(mockPipeline.get(any<String>())).thenReturn(mockResponse)
        whenever(mockJedis.setex(any<String>(), any<Long>(), any<String>())).thenReturn("OK")
        whenever(mockJedis.zadd(any<String>(), any<Double>(), any<String>())).thenReturn(1L)
        whenever(mockJedis.expire(any<String>(), any<Long>())).thenReturn(1L)
        whenever(mockJedis.zrevrange(any<String>(), any<Long>(), any<Long>())).thenReturn(listOf("test-id"))
        whenever(mockJedis.get(any<String>())).thenReturn("{\"id\":\"test-thread\",\"title\":\"Test Thread\",\"createdAt\":\"2023-01-01T00:00:00Z\",\"updatedAt\":\"2023-01-01T00:00:00Z\",\"messageCount\":0}")

        // 创建Redis存储
        val redisStorage = RedisMemoryStorage(mockJedisPool)

        // 测试创建线程
        val thread = MemoryThread(
            id = "test-thread",
            title = "Test Thread",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        val threadId = redisStorage.createThread(thread)
        assertEquals("test-thread", threadId)

        // 验证Jedis调用
        verify(mockJedis, times(1)).setex(any<String>(), any<Long>(), any<String>())
        verify(mockJedis, times(1)).zadd(any<String>(), any<Double>(), any<String>())

        // 测试获取线程
        val retrievedThread = redisStorage.getThread("test-thread")
        assertNotNull(retrievedThread)
        assertEquals("Test Thread", retrievedThread.title)

        // 测试创建消息
        val message = SimpleMessage(
            role = MessageRole.USER,
            content = "Test message"
        )
        val memoryMessage = MemoryMessage(
            id = "test-id",
            threadId = "test-thread",
            message = message,
            createdAt = Clock.System.now()
        )
        val messageId = redisStorage.saveMessage(memoryMessage)
        assertEquals("test-id", messageId)

        // 测试获取消息
        val messages = redisStorage.getMessages("test-thread", 10)
        assertEquals(1, messages.size)
    }

    @Test
    fun `test Postgres storage with mocks`() = runBlocking {
        // 创建模拟的DataSource
        val mockDataSource = mock<DataSource>()
        val mockConnection = mock<Connection>()
        val mockStatement = mock<java.sql.Statement>()
        val mockPreparedStatement = mock<PreparedStatement>()
        val mockResultSet = mock<ResultSet>()

        // 配置模拟行为
        whenever(mockDataSource.connection).thenReturn(mockConnection)
        whenever(mockConnection.createStatement()).thenReturn(mockStatement)
        whenever(mockStatement.execute(any())).thenReturn(true)
        whenever(mockConnection.prepareStatement(any())).thenReturn(mockPreparedStatement)
        whenever(mockPreparedStatement.executeUpdate()).thenReturn(1)
        whenever(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet)

        // 模拟ResultSet行为
        whenever(mockResultSet.next()).thenReturn(true, false) // 第一次调用返回true，之后返回false
        whenever(mockResultSet.getString("id")).thenReturn("test-thread")
        whenever(mockResultSet.getString("title")).thenReturn("Test Thread")
        whenever(mockResultSet.getTimestamp("created_at")).thenReturn(java.sql.Timestamp(System.currentTimeMillis()))
        whenever(mockResultSet.getTimestamp("updated_at")).thenReturn(java.sql.Timestamp(System.currentTimeMillis()))
        whenever(mockResultSet.getInt("message_count")).thenReturn(0)

        // 创建Postgres存储
        val postgresStorage = PostgresMemoryStorage(mockDataSource)

        // 测试创建线程
        val thread = MemoryThread(
            id = "test-thread",
            title = "Test Thread",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        val threadId = postgresStorage.createThread(thread)
        assertEquals("test-thread", threadId)

        // 验证调用
        verify(mockConnection, times(1)).prepareStatement(any())
        verify(mockPreparedStatement, times(1)).executeUpdate()

        // 测试获取线程
        val retrievedThread = postgresStorage.getThread("test-thread")
        assertNotNull(retrievedThread)
        assertEquals("Test Thread", retrievedThread.title)
    }

    // 辅助方法：创建测试线程
    private suspend fun createTestThread(storage: MemoryStorage): String {
        val thread = MemoryThread(
            id = UUID.randomUUID().toString(),
            title = "Test Thread",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        return storage.createThread(thread)
    }

    // 辅助方法：创建测试消息
    private suspend fun createTestMessage(storage: MemoryStorage, threadId: String): String {
        val message = SimpleMessage(
            role = MessageRole.USER,
            content = "Test message"
        )
        val memoryMessage = MemoryMessage(
            id = UUID.randomUUID().toString(),
            threadId = threadId,
            message = message,
            createdAt = Clock.System.now()
        )
        return storage.saveMessage(memoryMessage)
    }
}
