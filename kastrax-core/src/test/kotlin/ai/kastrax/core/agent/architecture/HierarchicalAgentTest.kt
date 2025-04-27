package ai.kastrax.core.agent.architecture

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentState
import ai.kastrax.core.agent.AgentStatus
import ai.kastrax.core.agent.agent
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HierarchicalAgentTest {

    private lateinit var coordinator: Agent
    private lateinit var techAgent: Agent
    private lateinit var creativeAgent: Agent
    private lateinit var hierarchicalAgent: HierarchicalAgent

    @BeforeEach
    fun setup() {
        // 创建协调器Agent，使用Deepseek LLM Provider
        coordinator = agent {
            name = "Coordinator"
            instructions = "你是一个协调器Agent，负责决策如何处理用户请求。"

            // 使用Deepseek模型
            model = deepSeek {
                model(DeepSeekModel.DEEPSEEK_CHAT)
                apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "test-api-key")
                temperature(0.7)
                maxTokens(2000)
                timeout(60000) // 60秒超时
            }
        }

        // 创建技术Agent
        techAgent = agent {
            name = "TechAgent"
            instructions = "你是一个技术专家Agent，专注于回答技术相关的问题。"

            // 使用Deepseek模型
            model = deepSeek {
                model(DeepSeekModel.DEEPSEEK_CHAT)
                apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "test-api-key")
                temperature(0.7)
                maxTokens(2000)
                timeout(60000) // 60秒超时
            }
        }

        // 创建创意Agent
        creativeAgent = agent {
            name = "CreativeAgent"
            instructions = "你是一个创意专家Agent，专注于回答创意和设计相关的问题。"

            // 使用Deepseek模型
            model = deepSeek {
                model(DeepSeekModel.DEEPSEEK_CHAT)
                apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "test-api-key")
                temperature(0.7)
                maxTokens(2000)
                timeout(60000) // 60秒超时
            }
        }

        // 使用DSL创建HierarchicalAgent
        hierarchicalAgent = hierarchicalAgent {
            coordinator(coordinator)
            addSubAgent("tech", techAgent)
            addSubAgent("creative", creativeAgent)
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".*")
    fun `test generate with coordinator handling`() = runBlocking {
        // 准备测试数据
        val prompt = "这是一个一般问题"
        val options = AgentGenerateOptions()

        // 执行测试
        val response = hierarchicalAgent.generate(prompt, options)

        // 验证结果
        assertNotNull(response)
        assertNotNull(response.text)
        assertTrue(response.text.isNotEmpty())
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".*")
    fun `test generate with sub-agent handling`() = runBlocking {
        // 准备测试数据
        val prompt = "这是一个技术问题"
        val options = AgentGenerateOptions()

        // 执行测试
        val response = hierarchicalAgent.generate(prompt, options)

        // 验证结果
        assertNotNull(response)
        assertNotNull(response.text)
        assertTrue(response.text.isNotEmpty())
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".*")
    fun `test generate with sub-tasks`() = runBlocking {
        // 准备测试数据
        val prompt = "这是一个复杂问题"
        val options = AgentGenerateOptions()

        // 执行测试
        val response = hierarchicalAgent.generate(prompt, options)

        // 验证结果
        assertNotNull(response)
        assertNotNull(response.text)
        assertTrue(response.text.isNotEmpty())
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".*")
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
        assertNotNull(response.text)
        assertTrue(response.text.isNotEmpty())
    }

    // 注意：在新的DSL实现中，没有getSubAgents和getCoordinator方法
    // 这些测试已经被移除
}
