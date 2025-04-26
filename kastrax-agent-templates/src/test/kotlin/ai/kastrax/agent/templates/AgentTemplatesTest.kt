package ai.kastrax.agent.templates

import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.core.llm.LlmResponse
import ai.kastrax.core.tools.Tool
import ai.kastrax.memory.api.Memory
import ai.kastrax.memory.impl.InMemoryStorage
import ai.kastrax.memory.impl.MemoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class AgentTemplatesTest {

    // 模拟LLM提供者
    private class MockLlmProvider : LlmProvider {
        override val model: String = "mock-model"

        override suspend fun generate(
            messages: List<LlmMessage>,
            options: ai.kastrax.core.llm.LlmOptions
        ): LlmResponse {
            // 简单返回一个固定响应
            return LlmResponse(
                content = "This is a mock response",
                toolCalls = emptyList()
            )
        }

        override suspend fun streamGenerate(
            messages: List<LlmMessage>,
            options: ai.kastrax.core.llm.LlmOptions
        ): Flow<String> {
            return flowOf("This", " is", " a", " mock", " stream", " response")
        }

        override suspend fun embedText(text: String): List<Float> {
            // 返回一个简单的嵌入向量
            return List(128) { 0.0f }
        }
    }

    // 模拟工具
    private class MockTool : Tool {
        override val id: String = "mock-tool"
        override val name: String = "Mock Tool"
        override val description: String = "A mock tool for testing"
        override val inputSchema: JsonElement = buildJsonObject {}
        override val outputSchema: JsonElement? = null

        override suspend fun execute(input: JsonElement): JsonElement {
            return buildJsonObject {}
        }
    }

    @Test
    fun `test create customer service agent`() {
        val mockLlm = MockLlmProvider()
        val mockTool = MockTool()
        val tools = mapOf("mock-tool" to mockTool)

        val agent = AgentTemplates.createCustomerServiceAgent(
            name = "CustomerService",
            model = mockLlm,
            additionalTools = tools
        )

        assertNotNull(agent)
        assertEquals("CustomerService", agent.name)

        // 测试代理生成响应
        runBlocking {
            val response = agent.generate("Hello, I need help with my order")
            assertEquals("This is a mock response", response.text)
        }
    }

    @Test
    fun `test create research assistant agent`() {
        val mockLlm = MockLlmProvider()
        val mockTool = MockTool()
        val tools = mapOf("mock-tool" to mockTool)

        val agent = AgentTemplates.createResearchAssistantAgent(
            name = "ResearchAssistant",
            model = mockLlm,
            additionalTools = tools
        )

        assertNotNull(agent)
        assertEquals("ResearchAssistant", agent.name)

        // 测试代理生成响应
        runBlocking {
            val response = agent.generate("Can you help me research quantum computing?")
            assertEquals("This is a mock response", response.text)
        }
    }

    @Test
    fun `test create creative writing agent`() {
        val mockLlm = MockLlmProvider()
        val mockTool = MockTool()
        val tools = mapOf("mock-tool" to mockTool)

        val agent = AgentTemplates.createCreativeWritingAgent(
            name = "CreativeWriter",
            model = mockLlm,
            additionalTools = tools
        )

        assertNotNull(agent)
        assertEquals("CreativeWriter", agent.name)

        // 测试代理生成响应
        runBlocking {
            val response = agent.generate("Write a short story about a robot learning to paint")
            assertEquals("This is a mock response", response.text)
        }
    }

    @Test
    fun `test create programming assistant agent`() {
        val mockLlm = MockLlmProvider()
        val mockTool = MockTool()
        val tools = mapOf("mock-tool" to mockTool)

        val agent = AgentTemplates.createProgrammingAssistantAgent(
            name = "ProgrammingAssistant",
            model = mockLlm,
            additionalTools = tools
        )

        assertNotNull(agent)
        assertEquals("ProgrammingAssistant", agent.name)

        // 测试代理生成响应
        runBlocking {
            val response = agent.generate("How do I implement a binary search tree in Kotlin?")
            assertEquals("This is a mock response", response.text)
        }
    }

    @Test
    fun `test create educational tutor agent`() {
        val mockLlm = MockLlmProvider()
        val mockTool = MockTool()
        val tools = mapOf("mock-tool" to mockTool)

        val agent = AgentTemplates.createEducationalTutorAgent(
            name = "EducationalTutor",
            model = mockLlm,
            additionalTools = tools
        )

        assertNotNull(agent)
        assertEquals("EducationalTutor", agent.name)

        // 测试代理生成响应
        runBlocking {
            val response = agent.generate("Can you help me understand photosynthesis?")
            assertEquals("This is a mock response", response.text)
        }
    }

    @Test
    fun `test agent with memory`() {
        val mockLlm = MockLlmProvider()
        val memory = MemoryImpl(InMemoryStorage())

        val agent = AgentTemplates.createCustomerServiceAgent(
            name = "CustomerServiceWithMemory",
            model = mockLlm,
            memory = memory
        )

        assertNotNull(agent)

        // 测试带记忆的代理
        runBlocking {
            val response = agent.generate("Hello, I need help with my order")
            assertNotNull(response.threadId)
        }
    }
}
