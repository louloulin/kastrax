package ai.kastrax.core.agent.architecture

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.AgentState
import ai.kastrax.core.agent.version.AgentVersionManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AdaptiveAgentTest {

    private lateinit var mockBaseAgent: Agent
    private lateinit var adaptiveAgent: AdaptiveAgent

    @BeforeEach
    fun setup() {
        mockBaseAgent = mockk<Agent>()

        // 设置基础Agent的行为
        coEvery { mockBaseAgent.name } returns "TestAgent"
        coEvery { mockBaseAgent.versionManager } returns null
        coEvery {
            mockBaseAgent.generate(any<String>(), any())
        } returns AgentResponse(text = "Mock response")

        // 使用DSL创建AdaptiveAgent
        adaptiveAgent = adaptiveAgent {
            baseAgent(mockBaseAgent)
            config {
                enableAutoLearning(true)
                maxInteractionHistory(10)
            }
        }
    }

    @Test
    fun `test generate with prompt`() = runBlocking {
        // 准备测试数据
        val prompt = "Hello, how are you?"
        val options = AgentGenerateOptions()
        val metadata = mapOf("userId" to "test-user")
        val optionsWithMetadata = options.copy(metadata = metadata)

        // 执行测试
        val response = adaptiveAgent.generate(prompt, optionsWithMetadata)

        // 验证结果
        assertNotNull(response)
        assertEquals("Mock response", response.text)
    }

    @Test
    fun `test user preference application`() = runBlocking {
        // 设置用户偏好
        val userId = "test-user"
        val preference = UserPreference(
            communicationStyle = "友好",
            detailLevel = "详细",
            topics = listOf("科技", "编程"),
            avoidTopics = listOf("政治")
        )

        adaptiveAgent.setUserPreference(userId, preference)

        // 准备测试数据
        val prompt = "Tell me about programming"
        val options = AgentGenerateOptions()
        val metadata = mapOf("userId" to userId)
        val optionsWithMetadata = options.copy(metadata = metadata)

        // 执行测试
        val response = adaptiveAgent.generate(prompt, optionsWithMetadata)

        // 验证结果
        assertNotNull(response)
        assertEquals("Mock response", response.text)
    }

    @Test
    fun `test feedback processing`() = runBlocking {
        // 准备测试数据
        val prompt = "What is artificial intelligence?"
        val options = AgentGenerateOptions()
        val metadata = mapOf(
            "userId" to "test-user",
            "sessionId" to "test-session"
        )
        val optionsWithMetadata = options.copy(metadata = metadata)

        // 生成响应
        val response = adaptiveAgent.generate(prompt, optionsWithMetadata)

        // 提取交互ID（这里我们无法直接获取，因为它是内部生成的）
        // 在实际应用中，交互ID应该从响应中获取或通过其他方式传递
        // 这里我们模拟一个交互ID
        val interactionId = "test-interaction-id"

        // 提供反馈
        adaptiveAgent.provideFeedback(
            interactionId = interactionId,
            rating = 4,
            feedback = "很好的回答，但可以更详细一些",
            userId = "test-user"
        )

        // 验证结果 - 由于我们无法直接验证内部状态，这里只是验证方法不会抛出异常
        // 在实际应用中，可以通过公开的API来验证反馈是否被正确处理
    }
}
