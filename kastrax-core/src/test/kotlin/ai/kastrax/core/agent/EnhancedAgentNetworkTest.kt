package ai.kastrax.core.agent

import ai.kastrax.core.agent.routing.ContextAwareRoutingStrategy
import ai.kastrax.core.agent.routing.DefaultRoutingStrategy
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.core.llm.LlmResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EnhancedAgentNetworkTest {

    @Test
    fun `test agent network with default routing strategy`() = runBlocking {
        // 创建模拟LLM提供者
        val mockLlm = mockk<LlmProvider>()
        coEvery {
            mockLlm.generate(any(), any())
        } returns LlmResponse(
            content = "这是一个测试响应",
            toolCalls = emptyList()
        )

        // 创建模拟代理
        val mockAgent1 = mockk<Agent>()
        coEvery {
            mockAgent1.generate(any<List<LlmMessage>>(), any())
        } returns AgentResponse(
            text = "代理1的响应",
            toolCalls = emptyList(),
            toolResults = emptyMap()
        )
        coEvery { mockAgent1.name } returns "agent1"

        val mockAgent2 = mockk<Agent>()
        coEvery {
            mockAgent2.generate(any<List<LlmMessage>>(), any())
        } returns AgentResponse(
            text = "代理2的响应",
            toolCalls = emptyList(),
            toolResults = emptyMap()
        )
        coEvery { mockAgent2.name } returns "agent2"

        // 创建代理网络
        val network = AgentNetwork(
            AgentNetworkConfig(
                name = "test_network",
                instructions = "测试指令",
                model = mockLlm,
                agents = listOf(mockAgent1, mockAgent2),
                routingStrategy = DefaultRoutingStrategy()
            )
        )

        // 测试生成
        val response = network.generate("测试查询")

        // 验证结果
        assertNotNull(response)
        assertEquals("这是一个测试响应", response.text)
    }

    @Test
    fun `test agent network with context aware routing strategy`() = runBlocking {
        // 创建模拟LLM提供者
        val mockLlm = mockk<LlmProvider>()
        coEvery {
            mockLlm.generate(any(), any())
        } returns LlmResponse(
            content = "这是一个上下文感知路由的测试响应",
            toolCalls = emptyList()
        )

        // 创建模拟代理
        val mockAgent1 = mockk<Agent>()
        coEvery {
            mockAgent1.generate(any<List<LlmMessage>>(), any())
        } returns AgentResponse(
            text = "代理1的响应",
            toolCalls = emptyList(),
            toolResults = emptyMap()
        )
        coEvery { mockAgent1.name } returns "agent1"

        val mockAgent2 = mockk<Agent>()
        coEvery {
            mockAgent2.generate(any<List<LlmMessage>>(), any())
        } returns AgentResponse(
            text = "代理2的响应",
            toolCalls = emptyList(),
            toolResults = emptyMap()
        )
        coEvery { mockAgent2.name } returns "agent2"

        // 创建代理网络
        val network = AgentNetwork(
            AgentNetworkConfig(
                name = "test_network",
                instructions = "测试指令",
                model = mockLlm,
                agents = listOf(mockAgent1, mockAgent2),
                routingStrategy = ContextAwareRoutingStrategy(),
                visualizeInteractions = true
            )
        )

        // 测试生成
        val response = network.generate("测试查询")

        // 验证结果
        assertNotNull(response)
        assertEquals("这是一个上下文感知路由的测试响应", response.text)

        // 测试可视化
        val visualization = network.getAgentInteractionVisualization()
        assertNotNull(visualization)
        assertTrue(visualization!!.contains("<!DOCTYPE html>"))
        assertTrue(visualization.contains("代理网络交互可视化"))
    }

    @Test
    fun `test agent network builder with context aware routing`() = runBlocking {
        // 创建模拟LLM提供者
        val mockLlm = mockk<LlmProvider>()
        coEvery {
            mockLlm.generate(any(), any())
        } returns LlmResponse(
            content = "这是一个构建器测试响应",
            toolCalls = emptyList()
        )

        // 创建模拟代理
        val mockAgent = mockk<Agent>()
        coEvery {
            mockAgent.generate(any<List<LlmMessage>>(), any())
        } returns AgentResponse(
            text = "代理的响应",
            toolCalls = emptyList(),
            toolResults = emptyMap()
        )
        coEvery { mockAgent.name } returns "test_agent"

        // 使用构建器创建代理网络
        val network = agentNetwork {
            name = "builder_test_network"
            instructions = "构建器测试指令"
            model = mockLlm
            agent(mockAgent)
            useContextAwareRouting()
            enableVisualization()
        }

        // 测试生成
        val response = network.generate("构建器测试查询")

        // 验证结果
        assertNotNull(response)
        assertEquals("这是一个构建器测试响应", response.text)

        // 验证可视化启用
        val visualization = network.getAgentInteractionVisualization()
        assertNotNull(visualization)
    }
}
