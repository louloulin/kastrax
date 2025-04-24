package ai.kastrax.memory.impl

import ai.kastrax.memory.api.WorkingMemoryConfig
import ai.kastrax.memory.api.WorkingMemoryMode
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import redis.clients.jedis.JedisPool
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable(named = "CI", matches = "true")
@org.junit.jupiter.api.condition.DisabledIfSystemProperty(named = "testcontainers.skip", matches = "true")
class RedisWorkingMemoryTest {

    companion object {
        private const val REDIS_PORT = 6379

        @Container
        val redisContainer = GenericContainer(DockerImageName.parse("redis:alpine"))
            .withExposedPorts(REDIS_PORT)
    }

    private lateinit var jedisPool: JedisPool
    private lateinit var redisWorkingMemory: RedisWorkingMemory

    @BeforeAll
    fun setup() {
        try {
            redisContainer.start()
            val redisHost = redisContainer.host
            val redisPort = redisContainer.getMappedPort(REDIS_PORT)

            jedisPool = JedisPool(redisHost, redisPort)
            redisWorkingMemory = RedisWorkingMemory(jedisPool, "test:working_memory:", 3600)
        } catch (e: Exception) {
            // 如果Docker不可用，则跳过测试
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Docker不可用，跳过测试: ${e.message}")
        }
    }

    @AfterAll
    fun tearDown() {
        try {
            if (::jedisPool.isInitialized) {
                jedisPool.close()
            }
            if (redisContainer.isRunning) {
                redisContainer.stop()
            }
        } catch (e: Exception) {
            // 忽略关闭时的错误
        }
    }

    @Test
    fun `test get and update working memory`() = runTest {
        org.junit.jupiter.api.Assumptions.assumeTrue(::redisWorkingMemory.isInitialized, "Redis工作内存未初始化，跳过测试")
        val threadId = "test-thread-1"
        val content = """
            # User Information
            - Name: John Doe
            - Location: New York
            - Preferences: Technology, Books
        """.trimIndent()

        // 更新工作内存
        val updateResult = redisWorkingMemory.updateWorkingMemory(threadId, content)
        assertTrue(updateResult)

        // 获取工作内存
        val retrievedContent = redisWorkingMemory.getWorkingMemory(threadId)
        assertEquals(content, retrievedContent)
    }

    @Test
    fun `test get system message with TEXT_STREAM mode`() = runTest {
        org.junit.jupiter.api.Assumptions.assumeTrue(::redisWorkingMemory.isInitialized, "Redis工作内存未初始化，跳过测试")
        val threadId = "test-thread-2"
        val content = "# Test Memory\n- Item 1\n- Item 2"

        // 更新工作内存
        redisWorkingMemory.updateWorkingMemory(threadId, content)

        // 获取系统消息
        val config = WorkingMemoryConfig(
            enabled = true,
            mode = WorkingMemoryMode.TEXT_STREAM
        )

        val systemMessage = redisWorkingMemory.getSystemMessage(threadId, config)
        assertNotNull(systemMessage)
        assertTrue(systemMessage.contains("工作内存"))
        assertTrue(systemMessage.contains("# Test Memory"))
        assertTrue(systemMessage.contains("Item 1"))
        assertTrue(systemMessage.contains("Item 2"))
    }

    @Test
    fun `test get system message with TOOL_CALL mode`() = runTest {
        org.junit.jupiter.api.Assumptions.assumeTrue(::redisWorkingMemory.isInitialized, "Redis工作内存未初始化，跳过测试")
        val threadId = "test-thread-3"
        val content = "# Test Memory\n- Item 1\n- Item 2"

        // 更新工作内存
        redisWorkingMemory.updateWorkingMemory(threadId, content)

        // 获取系统消息
        val config = WorkingMemoryConfig(
            enabled = true,
            mode = WorkingMemoryMode.TOOL_CALL
        )

        val systemMessage = redisWorkingMemory.getSystemMessage(threadId, config)
        assertNotNull(systemMessage)
        assertTrue(systemMessage.contains("update_working_memory"))
        assertTrue(systemMessage.contains("# Test Memory"))
    }

    @Test
    fun `test get tools with TOOL_CALL mode`() {
        org.junit.jupiter.api.Assumptions.assumeTrue(::redisWorkingMemory.isInitialized, "Redis工作内存未初始化，跳过测试")
        val config = WorkingMemoryConfig(
            enabled = true,
            mode = WorkingMemoryMode.TOOL_CALL
        )

        val tools = redisWorkingMemory.getTools(config)
        assertEquals(1, tools.size)
        assertTrue(tools.containsKey("update_working_memory"))
    }

    @Test
    fun `test get tools with TEXT_STREAM mode`() {
        org.junit.jupiter.api.Assumptions.assumeTrue(::redisWorkingMemory.isInitialized, "Redis工作内存未初始化，跳过测试")
        val config = WorkingMemoryConfig(
            enabled = true,
            mode = WorkingMemoryMode.TEXT_STREAM
        )

        val tools = redisWorkingMemory.getTools(config)
        assertTrue(tools.isEmpty())
    }

    @Test
    fun `test get tools with disabled config`() {
        val config = WorkingMemoryConfig(
            enabled = false
        )

        val tools = redisWorkingMemory.getTools(config)
        assertTrue(tools.isEmpty())
    }
}
