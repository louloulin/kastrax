package ai.kastrax.core.agent

import ai.kastrax.core.llm.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AgentNetworkTest {
    private lateinit var mockLlmProvider: LlmProvider
    private lateinit var mockAgent1: Agent
    private lateinit var mockAgent2: Agent

    @BeforeEach
    fun setup() {
        mockLlmProvider = mockk()
        mockAgent1 = mockk()
        mockAgent2 = mockk()

        // 设置模型名称
        coEvery { mockLlmProvider.model } returns "test-model"

        // 设置代理名称
        coEvery { mockAgent1.name } returns "Agent1"
        coEvery { mockAgent2.name } returns "Agent2"
    }

    @Test
    fun `test agent network creation`() {
        val network = agentNetwork {
            name = "TestNetwork"
            instructions = "Test instructions"
            model = mockLlmProvider
            agent(mockAgent1)
            agent(mockAgent2)
        }

        assertEquals("TestNetwork", network.name)
        assertEquals(2, network.getAgents().size)
        assertNotNull(network.getRoutingAgent())
    }

    @Test
    fun `test agent network generate`() = runTest {

        // 设置专业代理的响应
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
            mockAgent1.generate(any<String>(), any())
        } returns agent1Response

        coEvery {
            mockAgent2.generate(any<String>(), any())
        } returns agent2Response

        // 模拟LLM生成响应
        coEvery {
            mockLlmProvider.generate(any(), any())
        } returns LlmResponse(
            content = "LLM response",
            usage = LlmUsage(
                promptTokens = 10,
                completionTokens = 5,
                totalTokens = 15
            ),
            toolCalls = listOf(
                LlmToolCall(
                    id = "call1",
                    name = "transmit",
                    arguments = buildJsonObject {
                        putJsonArray("actions") {
                            addJsonObject {
                                put("agent", "Agent1")
                                put("input", "Test input for Agent1")
                                put("includeHistory", false)
                            }
                        }
                    }.toString()
                )
            )
        )

        // 创建代理网络
        val network = agentNetwork {
            name = "TestNetwork"
            instructions = "Test instructions"
            model = mockLlmProvider
            agent(mockAgent1)
            agent(mockAgent2)
        }

        // 执行生成
        val result = network.generate("Test input")

        // 验证结果
        assertNotNull(result)

        // 验证调用
        coVerify {
            mockLlmProvider.generate(any(), any())
        }
    }
}
