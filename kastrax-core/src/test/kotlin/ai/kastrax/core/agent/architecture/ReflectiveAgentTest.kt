package ai.kastrax.core.agent.architecture

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReflectiveAgentTest {

    private lateinit var mockBaseAgent: Agent
    private lateinit var reflectiveAgent: ReflectiveAgent

    @BeforeEach
    fun setup() {
        mockBaseAgent = mockk<Agent>()

        // 设置基础Agent的行为
        coEvery { mockBaseAgent.name } returns "TestAgent"
        coEvery { mockBaseAgent.versionManager } returns null

        // 模拟预反思响应
        coEvery {
            mockBaseAgent.generate(match { it.contains("进行反思") }, any())
        } returns AgentResponse(
            text = "这个问题是关于技术的，用户可能想了解最新的AI发展。需要提供准确、最新的信息。"
        )

        // 模拟响应反思响应
        coEvery {
            mockBaseAgent.generate(match { it.contains("进行反思和评估") }, any())
        } returns AgentResponse(
            text = """
                1. 回答完全解决了问题
                2. 回答清晰、准确
                3. 没有错误或偏见
                4. 语气适合
                5. 可以增加更多最新研究

                需要改进：否
            """.trimIndent(),
            metadata = emptyMap()
        )

        // 模拟后反思响应
        coEvery {
            mockBaseAgent.generate(match { it.contains("整个问答过程") }, any())
        } returns AgentResponse(
            text = "思考过程高效，下次可以提供更多具体例子。",
            metadata = emptyMap()
        )

        // 模拟学习响应
        coEvery {
            mockBaseAgent.generate(match { it.contains("提取关键经验") }, any())
        } returns AgentResponse(
            text = "1. 技术问题需要提供最新信息\n2. 使用具体例子增强解释",
            metadata = emptyMap()
        )

        // 模拟改进响应
        coEvery {
            mockBaseAgent.generate(match { it.contains("改进的回答") }, any())
        } returns AgentResponse(
            text = "Improved response with more details",
            metadata = emptyMap()
        )

        // 模拟常规响应
        coEvery {
            mockBaseAgent.generate(any<String>(), any())
        } returns AgentResponse(text = "Mock response", metadata = emptyMap())

        // 使用DSL创建ReflectiveAgent
        reflectiveAgent = reflectiveAgent {
            baseAgent(mockBaseAgent)
            config {
                enablePreReflection(true)
                enableResponseReflection(true)
                enablePostReflection(true)
                enableLearningFromReflection(true)
            }
        }
    }

    @Test
    fun `test generate with reflection`() = runBlocking {
        // 准备测试数据
        val prompt = "What are the latest developments in AI?"
        val options = AgentGenerateOptions(
            metadata = mapOf("sessionId" to "test-session")
        )

        // 执行测试
        val response = reflectiveAgent.generate(prompt, options)

        // 验证结果
        assertNotNull(response)
        assertEquals("Mock response", response.text)
    }

    @Test
    fun `test add and retrieve reflections`() = runBlocking {
        // 准备测试数据
        val sessionId = "test-session"
        val prompt = "What are the latest developments in AI?"
        val options = AgentGenerateOptions()
        val metadata = mapOf("sessionId" to sessionId)
        val optionsWithMetadata = options.copy(metadata = metadata)

        // 生成响应，会创建反思
        reflectiveAgent.generate(prompt, optionsWithMetadata)

        // 获取会话反思
        val reflections = reflectiveAgent.getSessionReflections(sessionId)

        // 验证结果
        assertTrue(reflections.isNotEmpty())
    }

    @Test
    fun `test reflection with improvement needed`() = runBlocking {
        // 修改模拟响应反思，使其需要改进
        coEvery {
            mockBaseAgent.generate(match { it.contains("进行反思和评估") }, any())
        } returns AgentResponse(
            text = """
                1. 回答部分解决了问题
                2. 回答不够清晰
                3. 有一些错误
                4. 语气适合
                5. 需要提供更准确的信息

                需要改进：是
            """.trimIndent(),
            metadata = emptyMap()
        )

        // 准备测试数据
        val prompt = "What are the latest developments in AI?"
        val options = AgentGenerateOptions(
            metadata = mapOf("sessionId" to "test-session")
        )

        // 执行测试
        val response = reflectiveAgent.generate(prompt, options)

        // 验证结果 - 应该返回改进的响应
        assertNotNull(response)
        assertEquals("Improved response with more details", response.text)
    }

    @Test
    fun `test session reflections`() = runBlocking {
        // 准备测试数据
        val sessionId = "test-session"
        val prompt = "What are the latest developments in AI?"
        val options = AgentGenerateOptions(
            metadata = mapOf("sessionId" to sessionId)
        )

        // 执行多次生成，积累反思
        repeat(3) {
            reflectiveAgent.generate(prompt, options)
        }

        // 获取会话反思
        val sessionReflections = reflectiveAgent.getSessionReflections(sessionId)

        // 验证结果
        assertTrue(sessionReflections.isNotEmpty())
        assertTrue(sessionReflections.size >= 3) // 每次生成至少产生一个反思
    }
}
