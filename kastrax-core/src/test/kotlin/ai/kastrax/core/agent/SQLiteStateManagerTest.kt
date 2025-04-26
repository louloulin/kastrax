package ai.kastrax.core.agent

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class SQLiteStateManagerTest {

    private lateinit var stateManager: SQLiteStateManager
    private lateinit var dbPath: String

    @BeforeEach
    fun setup(@TempDir tempDir: Path) {
        dbPath = tempDir.resolve("test_agent_state.db").toString()
        stateManager = SQLiteStateManager(dbPath)
    }

    @AfterEach
    fun cleanup() {
        // 删除测试数据库文件
        File(dbPath).delete()
    }

    @Test
    fun `test save and get state`() = runBlocking {
        // 创建测试状态
        val state = AgentState(
            status = AgentStatus.THINKING,
            metadata = mapOf("test" to "value"),
            variables = mapOf("var1" to JsonPrimitive("test"))
        )

        // 保存状态
        val savedState = stateManager.saveState(state)
        assertNotNull(savedState)
        assertEquals(state.id, savedState.id)
        assertEquals(AgentStatus.THINKING, savedState.status)

        // 获取状态
        val retrievedState = stateManager.getState(state.id)
        assertNotNull(retrievedState)
        assertEquals(state.id, retrievedState?.id)
        assertEquals(AgentStatus.THINKING, retrievedState?.status)
        assertEquals("value", retrievedState?.metadata?.get("test"))
        assertEquals(JsonPrimitive("test"), retrievedState?.variables?.get("var1"))
    }

    @Test
    fun `test update state`() = runBlocking {
        // 创建测试状态
        val state = AgentState(
            status = AgentStatus.IDLE,
            metadata = mapOf("test" to "value")
        )

        // 保存状态
        stateManager.saveState(state)

        // 更新状态
        val updatedState = state.withStatus(AgentStatus.EXECUTING)
            .withMetadata("updated", "true")
        stateManager.saveState(updatedState)

        // 获取更新后的状态
        val retrievedState = stateManager.getState(state.id)
        assertNotNull(retrievedState)
        assertEquals(AgentStatus.EXECUTING, retrievedState?.status)
        assertEquals("value", retrievedState?.metadata?.get("test"))
        assertEquals("true", retrievedState?.metadata?.get("updated"))
    }

    @Test
    fun `test get state by thread`() = runBlocking {
        // 创建测试状态
        val threadId = "test-thread-123"
        val state = AgentState(
            threadId = threadId,
            status = AgentStatus.RESPONDING
        )

        // 保存状态
        stateManager.saveState(state)

        // 通过线程ID获取状态
        val retrievedState = stateManager.getStateByThread(threadId)
        assertNotNull(retrievedState)
        assertEquals(state.id, retrievedState?.id)
        assertEquals(threadId, retrievedState?.threadId)
        assertEquals(AgentStatus.RESPONDING, retrievedState?.status)
    }

    @Test
    fun `test get states by resource`() = runBlocking {
        // 创建测试状态
        val resourceId = "test-resource-123"
        val state1 = AgentState(
            resourceId = resourceId,
            status = AgentStatus.THINKING
        )
        val state2 = AgentState(
            resourceId = resourceId,
            status = AgentStatus.EXECUTING
        )

        // 保存状态
        stateManager.saveState(state1)
        stateManager.saveState(state2)

        // 通过资源ID获取状态
        val states = stateManager.getStatesByResource(resourceId)
        assertEquals(2, states.size)
        assertTrue(states.any { it.id == state1.id })
        assertTrue(states.any { it.id == state2.id })
    }

    @Test
    fun `test delete state`() = runBlocking {
        // 创建测试状态
        val state = AgentState(
            status = AgentStatus.IDLE
        )

        // 保存状态
        stateManager.saveState(state)

        // 删除状态
        val result = stateManager.deleteState(state.id)
        assertTrue(result)

        // 确认状态已删除
        val retrievedState = stateManager.getState(state.id)
        assertNull(retrievedState)
    }
}
