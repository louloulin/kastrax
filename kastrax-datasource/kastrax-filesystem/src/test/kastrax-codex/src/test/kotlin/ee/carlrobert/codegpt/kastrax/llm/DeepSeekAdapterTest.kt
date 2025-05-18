package ee.carlrobert.codegpt.kastrax.llm

import ai.kastrax.core.KastraX
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.AgentStreamResponse
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.DeepSeekProvider
import com.intellij.openapi.project.Project
import ee.carlrobert.codegpt.kastrax.KastraxAdapter
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

class DeepSeekAdapterTest {
    private lateinit var project: Project
    private lateinit var kastraxAdapter: KastraxAdapter
    private lateinit var deepSeekAdapter: DeepSeekAdapter
    private lateinit var mockKastraX: KastraX
    private lateinit var mockAgent: Agent

    @Before
    fun setup() {
        // 创建mock对象
        project = mockk(relaxed = true)
        kastraxAdapter = mockk(relaxed = true)
        mockKastraX = mockk(relaxed = true)
        mockAgent = mockk(relaxed = true)

        // 配置mock行为
        every { project.getService(KastraxAdapter::class.java) } returns kastraxAdapter
        every { kastraxAdapter.getKastraX() } returns mockKastraX
        every { kastraxAdapter.getDefaultAgent() } returns mockAgent

        // 创建DeepSeekAdapter实例
        deepSeekAdapter = DeepSeekAdapter(project)
    }

    @Test
    fun testInitialize() {
        // 测试初始化
        deepSeekAdapter.initialize()

        // 验证调用了getDefaultAgent
        verify { kastraxAdapter.getDefaultAgent() }
    }

    @Test
    fun testGenerateWithKastrax() = runBlocking {
        // 模拟Agent生成响应
        val mockResponse = mockk<AgentResponse>().apply {
            every { text } returns "这是DeepSeek生成的回答"
        }

        // 配置mock行为
        val optionsSlot = slot<AgentGenerateOptions>()
        coEvery { mockAgent.generate(any(), capture(optionsSlot)) } returns mockResponse

        // 初始化适配器
        deepSeekAdapter.initialize()

        // 测试生成文本
        val prompt = "如何实现一个Java单例模式？"
        val result = deepSeekAdapter.generate(prompt)

        // 验证结果
        assertEquals("这是DeepSeek生成的回答", result)

        // 验证传递的选项
        assertTrue(optionsSlot.captured.temperature in 0.6..0.8)
        assertTrue(optionsSlot.captured.maxTokens > 1000)
    }

    @Test
    fun testGenerateStreamWithKastrax() = runBlocking {
        // 模拟Agent流式生成响应
        val mockResponses = listOf(
            mockk<AgentStreamResponse>().apply { every { text } returns "这是" },
            mockk<AgentStreamResponse>().apply { every { text } returns "DeepSeek" },
            mockk<AgentStreamResponse>().apply { every { text } returns "生成的回答" }
        )

        // 配置mock行为
        val optionsSlot = slot<AgentStreamOptions>()
        coEvery { mockAgent.generateStream(any(), capture(optionsSlot)) } returns flowOf(*mockResponses.toTypedArray())

        // 初始化适配器
        deepSeekAdapter.initialize()

        // 测试流式生成文本
        val prompt = "如何实现一个Java单例模式？"
        val results = mutableListOf<String>()
        deepSeekAdapter.generateStream(prompt).collect { results.add(it) }

        // 验证结果
        assertEquals(3, results.size)
        assertEquals("这是", results[0])
        assertEquals("DeepSeek", results[1])
        assertEquals("生成的回答", results[2])

        // 验证传递的选项
        assertTrue(optionsSlot.captured.temperature in 0.6..0.8)
        assertTrue(optionsSlot.captured.maxTokens > 1000)
    }

    @Test
    fun testCreateDeepSeekProvider() {
        // 测试创建DeepSeek提供者
        val provider = deepSeekAdapter.createDeepSeekProvider("test-api-key")

        // 验证提供者不为空
        assertNotNull(provider)
    }
}
