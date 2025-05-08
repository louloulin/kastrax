package ai.kastrax.codex.agent

import ai.kastrax.codex.adapter.LlmProviderAdapter
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.agent
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.core.llm.LlmResponse
import ai.kastrax.core.llm.LlmStreamResponse
import ai.kastrax.core.llm.LlmToolCall
import ai.kastrax.core.llm.LlmUsage
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightPlatformTestCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * CodexAgent 测试类
 */
class CodexAgentTest : LightPlatformTestCase() {
    
    /**
     * 测试代码补全 Agent
     */
    @Test
    fun testCodeCompletionAgent() = runBlocking {
        // 创建模拟的 LlmProvider
        val mockLlmProvider = createMockLlmProvider(
            "这是一个示例函数：\n\n```kotlin\nfun example(input: String): String {\n    return \"Hello, $input!\"\n}\n```"
        )
        
        // 创建基础 Agent
        val baseAgent = agent {
            name = "测试 Agent"
            instructions = "你是一个测试 Agent"
            model = mockLlmProvider
        }
        
        // 创建 CodexAgent
        val project = mock<Project>()
        val codexAgent = CodexAgent(baseAgent, project)
        
        // 测试生成
        val response = codexAgent.generate("生成一个简单的 Kotlin 函数")
        
        // 验证结果
        assertNotNull("响应不应为空", response)
        assertTrue("响应应包含 Kotlin 代码", response.text.contains("```kotlin"))
        assertTrue("响应应包含函数定义", response.text.contains("fun example"))
    }
    
    /**
     * 测试代码解释 Agent
     */
    @Test
    fun testCodeExplanationAgent() = runBlocking {
        // 创建模拟的 LlmProvider
        val mockLlmProvider = createMockLlmProvider(
            "这段代码定义了一个名为 `example` 的函数，它接受一个 String 类型的参数 `input`，并返回一个包含问候语的字符串。"
        )
        
        // 创建基础 Agent
        val baseAgent = agent {
            name = "测试 Agent"
            instructions = "你是一个测试 Agent"
            model = mockLlmProvider
        }
        
        // 创建 CodexAgent
        val project = mock<Project>()
        val codexAgent = CodexAgent(baseAgent, project)
        
        // 测试生成
        val code = "fun example(input: String): String {\n    return \"Hello, $input!\"\n}"
        val response = codexAgent.generate("解释这段代码：\n```kotlin\n$code\n```")
        
        // 验证结果
        assertNotNull("响应不应为空", response)
        assertTrue("响应应包含解释", response.text.contains("函数"))
    }
    
    /**
     * 创建模拟的 LlmProvider
     */
    private fun createMockLlmProvider(responseText: String): LlmProvider {
        return object : LlmProvider {
            override val model: String = "test-model"
            
            override suspend fun generate(messages: List<LlmMessage>, options: ai.kastrax.core.llm.LlmOptions): LlmResponse {
                return LlmResponse(
                    content = responseText,
                    toolCalls = emptyList(),
                    usage = LlmUsage(
                        promptTokens = 10,
                        completionTokens = 20,
                        totalTokens = 30
                    ),
                    finishReason = "stop"
                )
            }
            
            override suspend fun streamGenerate(messages: List<LlmMessage>, options: ai.kastrax.core.llm.LlmOptions): Flow<String> {
                return flow {
                    emit(responseText)
                }
            }
            
            override suspend fun streamGenerateWithTools(messages: List<LlmMessage>, options: ai.kastrax.core.llm.LlmOptions): LlmStreamResponse {
                return LlmStreamResponse(
                    textStream = flow { emit(responseText) },
                    toolCallStream = null
                )
            }
            
            override suspend fun embedText(text: String): List<Float> {
                return List(10) { 0.1f * it }
            }
        }
    }
}
