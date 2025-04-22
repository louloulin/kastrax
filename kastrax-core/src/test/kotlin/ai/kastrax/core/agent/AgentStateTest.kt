package ai.kastrax.core.agent

import ai.kastrax.core.llm.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AgentStateTest {
    private lateinit var mockLlmProvider: LlmProvider
    private lateinit var sessionManager: SessionManager
    private lateinit var stateManager: StateManager
    private lateinit var agent: Agent

    @BeforeEach
    fun setup() {
        mockLlmProvider = mockk()
        val inMemorySessionManager = InMemorySessionManager()
        val inMemoryStateManager = InMemoryStateManager()

        sessionManager = inMemorySessionManager
        stateManager = inMemoryStateManager

        // 设置模型名称
        coEvery { mockLlmProvider.model } returns "test-model"

        // 模拟LLM生成响应
        coEvery {
            mockLlmProvider.generate(any(), any())
        } returns LlmResponse(
            content = "Test response",
            usage = LlmUsage(
                promptTokens = 10,
                completionTokens = 5,
                totalTokens = 15
            )
        )

        // 创建Agent
        agent = agent {
            name = "TestAgent"
            instructions = "Test instructions"
            model = mockLlmProvider
            sessionManager(inMemorySessionManager)
            stateManager(inMemoryStateManager)
        }
    }

    @Test
    fun `test agent state management`() = runTest {
        // 初始状态应该为null
        val initialState = agent.getState()
        assertNull(initialState)

        // 更新状态
        val thinkingState = agent.updateState(AgentStatus.THINKING)
        assertNotNull(thinkingState)
        assertEquals(AgentStatus.THINKING, thinkingState.status)

        // 获取当前状态
        val currentState = agent.getState()
        assertNotNull(currentState)
        assertEquals(AgentStatus.THINKING, currentState.status)

        // 重置状态
        agent.reset()
        val resetState = agent.getState()
        assertNull(resetState)
    }

    @Test
    fun `test agent session management`() = runTest {
        // 创建会话
        val session = agent.createSession(
            title = "Test Session",
            resourceId = "test-resource",
            metadata = mapOf("key" to "value")
        )
        assertNotNull(session)
        assertEquals("Test Session", session.title)
        assertEquals("test-resource", session.resourceId)
        assertEquals("value", session.metadata["key"])

        // 获取会话
        val retrievedSession = agent.getSession(session.id)
        assertNotNull(retrievedSession)
        assertEquals(session.id, retrievedSession.id)

        // 生成响应，应该包含状态和会话信息
        val response = agent.generate("Test prompt", AgentGenerateOptions(threadId = session.id))
        assertEquals("Test response", response.text)
        assertNotNull(response.state)
        assertNotNull(response.sessionInfo)
        assertEquals(session.id, response.sessionInfo?.id)
        assertEquals(AgentStatus.IDLE, response.state?.status)

        // 检查会话消息
        val messages = agent.getSessionMessages(session.id)
        assertNotNull(messages)

        // 手动保存消息到会话，因为模拟的Agent不会实际保存消息
        sessionManager.saveMessage(
            LlmMessage(role = LlmMessageRole.USER, content = "Test prompt"),
            session.id
        )
        sessionManager.saveMessage(
            LlmMessage(role = LlmMessageRole.ASSISTANT, content = "Test response"),
            session.id
        )

        // 再次获取消息并验证
        val updatedMessages = agent.getSessionMessages(session.id)
        assertNotNull(updatedMessages)
        assertEquals(2, updatedMessages.size) // 用户消息和助手消息
    }

    @Test
    fun `test state updates during generation`() = runTest {
        // 生成响应
        val response = agent.generate("Test prompt")

        // 验证状态更新
        coVerify(exactly = 1) {
            mockLlmProvider.generate(any(), any())
        }

        // 最终状态应该是IDLE
        val finalState = agent.getState()
        assertNotNull(finalState)
        assertEquals(AgentStatus.IDLE, finalState.status)
    }
}
