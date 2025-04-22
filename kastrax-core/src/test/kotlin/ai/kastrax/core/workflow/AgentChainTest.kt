package ai.kastrax.core.workflow

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.llm.LlmUsage
import io.mockk.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AgentChainTest {
    private lateinit var mockAgent1: Agent
    private lateinit var mockAgent2: Agent

    @BeforeEach
    fun setup() {
        mockAgent1 = mockk()
        mockAgent2 = mockk()

        // 设置代理名称
        coEvery { mockAgent1.name } returns "Agent1"
        coEvery { mockAgent2.name } returns "Agent2"
    }

    @Test
    fun `test agent chain creation`() {
        val chain = agentChain {
            name = "TestChain"
            description = "Test description"
            agent(mockAgent1)
            agent(mockAgent2)
        }

        assertEquals("TestChain", chain.name)
        assertEquals("Test description", chain.description)
        assertEquals(2, chain.agents.size)
        assertEquals("Agent1", chain.agents[0].name)
        assertEquals("Agent2", chain.agents[1].name)
    }

    @Test
    fun `test agent chain execute`() = runTest {
        // 设置代理响应
        val agent1Response = AgentResponse(
            text = "Agent1 response",
            usage = LlmUsage(
                promptTokens = 10,
                completionTokens = 5,
                totalTokens = 15
            )
        )

        val agent2Response = AgentResponse(
            text = "Agent2 response",
            usage = LlmUsage(
                promptTokens = 10,
                completionTokens = 5,
                totalTokens = 15
            )
        )

        // 模拟代理生成响应
        coEvery {
            mockAgent1.generate(any<String>(), any<AgentGenerateOptions>())
        } returns agent1Response

        coEvery {
            mockAgent2.generate(any<String>(), any<AgentGenerateOptions>())
        } returns agent2Response

        // 创建代理链
        val chain = agentChain {
            name = "TestChain"
            description = "Test description"
            agent(mockAgent1)
            agent(mockAgent2)
        }

        // 执行链
        val result = chain.execute("Test input")

        // 验证结果
        assertTrue(result.success)
        assertEquals("Agent2 response", result.output)
        assertEquals(2, result.steps.size)
        assertEquals("Agent1", result.steps[0].agentName)
        assertEquals("Agent2", result.steps[1].agentName)
        assertEquals("Test input", result.steps[0].input)
        assertEquals("Agent1 response", result.steps[0].output)
        assertEquals("Agent1 response", result.steps[1].input)
        assertEquals("Agent2 response", result.steps[1].output)

        // 验证调用
        coVerify {
            mockAgent1.generate("Test input", any())
            mockAgent2.generate("Agent1 response", any())
        }
    }

    @Test
    fun `test agent chain execute with error`() = runTest {
        // 设置代理响应
        val agent1Response = AgentResponse(
            text = "Agent1 response",
            usage = LlmUsage(
                promptTokens = 10,
                completionTokens = 5,
                totalTokens = 15
            )
        )

        // 模拟代理生成响应
        coEvery {
            mockAgent1.generate(any<String>(), any<AgentGenerateOptions>())
        } returns agent1Response

        coEvery {
            mockAgent2.generate(any<String>(), any<AgentGenerateOptions>())
        } throws RuntimeException("Test error")

        // 创建代理链
        val chain = agentChain {
            name = "TestChain"
            description = "Test description"
            agent(mockAgent1)
            agent(mockAgent2)
        }

        // 执行链
        val result = chain.execute("Test input")

        // 验证结果
        assertFalse(result.success)
        assertEquals(2, result.steps.size)
        assertEquals("Agent1", result.steps[0].agentName)
        assertEquals("Agent2", result.steps[1].agentName)
        assertTrue(result.steps[0].success)
        assertFalse(result.steps[1].success)
        assertEquals("Test error", result.steps[1].error)
        assertEquals("Test error", result.error)

        // 验证调用
        coVerify {
            mockAgent1.generate("Test input", any())
            mockAgent2.generate("Agent1 response", any())
        }
    }

    @Test
    fun `test agent chain stream execute`() = runTest {
        // 设置代理响应
        val agent1Response = AgentResponse(
            text = "Agent1 response",
            usage = LlmUsage(
                promptTokens = 10,
                completionTokens = 5,
                totalTokens = 15
            )
        )

        val agent2Response = AgentResponse(
            text = "Agent2 response",
            usage = LlmUsage(
                promptTokens = 10,
                completionTokens = 5,
                totalTokens = 15
            )
        )

        // 模拟代理生成响应
        coEvery {
            mockAgent1.generate(any<String>(), any<AgentGenerateOptions>())
        } returns agent1Response

        coEvery {
            mockAgent2.generate(any<String>(), any<AgentGenerateOptions>())
        } returns agent2Response

        // 创建代理链
        val chain = agentChain {
            name = "TestChain"
            description = "Test description"
            agent(mockAgent1)
            agent(mockAgent2)
        }

        // 执行链
        val updates = chain.streamExecute("Test input").toList()

        // 验证结果
        assertTrue(updates.size >= 3, "Expected at least 3 updates, got ${updates.size}")

        // 验证第一个状态是开始
        assertEquals(AgentChainStatus.STARTED, updates.first().status)

        // 验证最后一个状态是完成
        assertEquals(AgentChainStatus.COMPLETED, updates.last().status)

        // 验证最后一个状态的输出
        assertEquals("Agent2 response", updates.last().output)

        // 验证存在进行中状态
        assertTrue(updates.any { it.status == AgentChainStatus.IN_PROGRESS })

        // 验证存在步骤完成状态
        assertTrue(updates.any { it.status == AgentChainStatus.STEP_COMPLETED })

        // 验证调用
        coVerify {
            mockAgent1.generate("Test input", any())
            mockAgent2.generate("Agent1 response", any())
        }
    }
}
