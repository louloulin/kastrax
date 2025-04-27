package ai.kastrax.core.agent.architecture

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentState
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HierarchicalAgentTest {

    private lateinit var mockCoordinator: Agent
    private lateinit var mockSubAgent1: Agent
    private lateinit var mockSubAgent2: Agent
    private lateinit var hierarchicalAgent: HierarchicalAgent

    @BeforeEach
    fun setup() {
        mockCoordinator = mockk<Agent>()
        mockSubAgent1 = mockk<Agent>()
        mockSubAgent2 = mockk<Agent>()

        // 设置协调器Agent的行为
        coEvery { mockCoordinator.name } returns "Coordinator"
        coEvery { mockCoordinator.versionManager } returns null
        coEvery { mockCoordinator.getState() } returns AgentState(
            status = AgentStatus.EXECUTING,
            metadata = mapOf("description" to "协调器Agent")
        )

        // 设置子Agent的行为
        coEvery { mockSubAgent1.name } returns "SubAgent1"
        coEvery { mockSubAgent1.versionManager } returns null
        coEvery { mockSubAgent1.getState() } returns AgentState(
            status = AgentStatus.EXECUTING,
            metadata = mapOf("description" to "专注于技术问题的Agent")
        )

        coEvery { mockSubAgent2.name } returns "SubAgent2"
        coEvery { mockSubAgent2.versionManager } returns null
        coEvery { mockSubAgent2.getState() } returns AgentState(
            status = AgentStatus.EXECUTING,
            metadata = mapOf("description" to "专注于创意内容的Agent")
        )

        // 模拟协调器决策 - 委派给子Agent1
        coEvery {
            mockCoordinator.generate(match { it.contains("作为一个协调器") && it.contains("技术") }, any())
        } returns AgentResponse(
            text = """
                {
                    "handler": "tech",
                    "reason": "这是一个技术问题，交给技术专家处理更合适",
                    "subTasks": []
                }
            """.trimIndent()
        )

        // 模拟协调器决策 - 委派给子Agent2
        coEvery {
            mockCoordinator.generate(match { it.contains("作为一个协调器") && it.contains("创意") }, any())
        } returns AgentResponse(
            text = """
                {
                    "handler": "creative",
                    "reason": "这是一个创意问题，交给创意专家处理更合适",
                    "subTasks": []
                }
            """.trimIndent()
        )

        // 模拟协调器决策 - 自己处理
        coEvery {
            mockCoordinator.generate(match { it.contains("作为一个协调器") && it.contains("一般") }, any())
        } returns AgentResponse(
            text = """
                {
                    "handler": "coordinator",
                    "reason": "这是一个一般问题，我可以直接处理",
                    "subTasks": []
                }
            """.trimIndent()
        )

        // 模拟协调器决策 - 分解为子任务
        coEvery {
            mockCoordinator.generate(match { it.contains("作为一个协调器") && it.contains("复杂") }, any())
        } returns AgentResponse(
            text = """
                {
                    "handler": "coordinator",
                    "reason": "这是一个复杂问题，需要多个专家协作",
                    "subTasks": [
                        {
                            "handler": "tech",
                            "prompt": "从技术角度分析AI的发展",
                            "description": "技术分析"
                        },
                        {
                            "handler": "creative",
                            "prompt": "从创意角度分析AI的应用",
                            "description": "创意分析"
                        }
                    ]
                }
            """.trimIndent()
        )

        // 模拟子任务结果整合
        coEvery {
            mockCoordinator.generate(match { it.contains("整合以下子任务的结果") }, any())
        } returns AgentResponse(
            text = "Integrated response from multiple sub-agents"
        )

        // 模拟常规响应
        coEvery {
            mockCoordinator.generate(any<String>(), any())
        } returns AgentResponse(text = "Coordinator response")

        coEvery {
            mockSubAgent1.generate(any<String>(), any())
        } returns AgentResponse(text = "SubAgent1 response")

        coEvery {
            mockSubAgent2.generate(any<String>(), any())
        } returns AgentResponse(text = "SubAgent2 response")

        // 模拟多消息响应
        coEvery {
            mockCoordinator.generate(any<List<LlmMessage>>(), any())
        } returns AgentResponse(text = "Coordinator response for messages", metadata = emptyMap())

        // 使用DSL创建HierarchicalAgent
        hierarchicalAgent = hierarchicalAgent {
            coordinator(mockCoordinator)
            addSubAgent("tech", mockSubAgent1)
            addSubAgent("creative", mockSubAgent2)
        }
    }

    @Test
    fun `test generate with coordinator handling`() = runBlocking {
        // 准备测试数据
        val prompt = "这是一个一般问题"
        val options = AgentGenerateOptions()

        // 执行测试
        val response = hierarchicalAgent.generate(prompt, options)

        // 验证结果
        assertNotNull(response)
        assertEquals("Coordinator response", response.text)
    }

    @Test
    fun `test generate with sub-agent handling`() = runBlocking {
        // 准备测试数据
        val prompt = "这是一个技术问题"
        val options = AgentGenerateOptions()

        // 执行测试
        val response = hierarchicalAgent.generate(prompt, options)

        // 验证结果
        assertNotNull(response)
        assertEquals("SubAgent1 response", response.text)
    }

    @Test
    fun `test generate with sub-tasks`() = runBlocking {
        // 准备测试数据
        val prompt = "这是一个复杂问题"
        val options = AgentGenerateOptions()

        // 执行测试
        val response = hierarchicalAgent.generate(prompt, options)

        // 验证结果
        assertNotNull(response)
        assertEquals("Integrated response from multiple sub-agents", response.text)
    }

    @Test
    fun `test generate with messages`() = runBlocking {
        // 准备测试数据
        val messages = listOf(
            LlmMessage(role = LlmMessageRole.SYSTEM, content = "You are a helpful assistant"),
            LlmMessage(role = LlmMessageRole.USER, content = "这是一个一般问题")
        )
        val options = AgentGenerateOptions()

        // 执行测试
        val response = hierarchicalAgent.generate(messages, options)

        // 验证结果
        assertNotNull(response)
        assertEquals("Coordinator response", response.text)
    }

    // 注意：在新的DSL实现中，没有getSubAgents和getCoordinator方法
    // 这些测试已经被移除
}
