package ai.kastrax.core.agent

import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmOptions
import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.core.llm.LlmResponse
import ai.kastrax.core.llm.LlmStreamResponse
import ai.kastrax.core.llm.LlmToolCall
import ai.kastrax.runtime.coroutines.jvm.JvmCoroutineRuntime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class KastraxRuntimeAgentTest {

    @Test
    fun testKastraxRuntimeAgent() = runBlocking {
        // 创建kastrax运行时
        val runtime = JvmCoroutineRuntime()

        // 创建模拟LLM提供者
        val mockLlmProvider = MockLlmProvider()

        // 创建Agent
        val agent = AgentBuilder().apply {
            name = "test-agent"
            instructions = "You are a test agent."
            model = mockLlmProvider
        }.buildWithKastraxRuntime(runtime)

        // 测试生成
        val response = agent.generate("Hello", AgentGenerateOptions())

        // 验证响应
        assertNotNull(response)
        assertEquals("Mock response for: Hello", response.text)

        // 测试流式生成
        val streamResponse = agent.stream("Hello stream", AgentStreamOptions())

        // 验证流式响应
        assertNotNull(streamResponse)
        assertEquals("Mock stream response for: Hello stream", streamResponse.text)

        // 测试流式生成流
        val streamResponseFlow = agent.generateStream("Hello stream flow", AgentStreamOptions())
        val streamResponseList = streamResponseFlow.toList()

        // 验证流式响应流
        assertNotNull(streamResponseList)
        assertEquals(1, streamResponseList.size)
        assertEquals("Mock stream response for: Hello stream flow", streamResponseList[0].text)
    }
}

/**
 * 模拟LLM提供者
 */
class MockLlmProvider : LlmProvider {
    override val model: String = "mock-model"

    override suspend fun generate(messages: List<LlmMessage>, options: LlmOptions): LlmResponse {
        val lastMessage = messages.lastOrNull { it.role == LlmMessageRole.USER }
        val content = lastMessage?.content ?: ""
        return LlmResponse(
            content = "Mock response for: $content",
            toolCalls = emptyList()
        )
    }

    override suspend fun streamGenerate(messages: List<LlmMessage>, options: LlmOptions): Flow<String> {
        val lastMessage = messages.lastOrNull { it.role == LlmMessageRole.USER }
        val content = lastMessage?.content ?: ""
        return flowOf("Mock stream response for: $content")
    }

    override suspend fun streamGenerateWithTools(messages: List<LlmMessage>, options: LlmOptions): LlmStreamResponse {
        val lastMessage = messages.lastOrNull { it.role == LlmMessageRole.USER }
        val content = lastMessage?.content ?: ""
        return LlmStreamResponse(
            textStream = flowOf("Mock stream response for: $content"),
            toolCallStream = null
        )
    }

    override suspend fun embedText(text: String): List<Float> {
        return List(128) { 0.0f }
    }
}
