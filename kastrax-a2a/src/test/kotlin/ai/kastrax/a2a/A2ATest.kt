package ai.kastrax.a2a

import ai.kastrax.a2a.agent.A2AAgentImpl
import ai.kastrax.a2a.dsl.a2aAgent
import ai.kastrax.a2a.model.*
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A2A 测试类
 */
class A2ATest {
    /**
     * 模拟的 kastrax 代理
     */
    private lateinit var mockAgent: Agent
    
    /**
     * A2A 实例
     */
    private lateinit var a2a: A2A
    
    @BeforeEach
    fun setup() {
        // 创建模拟的 kastrax 代理
        mockAgent = mockk<Agent>()
        
        // 配置模拟代理的行为
        coEvery { mockAgent.name } returns "mock-agent"
        coEvery { mockAgent.generate(any<String>()) } returns AgentResponse(
            text = "This is a mock response",
            toolCalls = emptyList()
        )
        
        // 创建 A2A 实例
        a2a = A2A.getInstance()
    }
    
    @AfterEach
    fun tearDown() {
        // 停止服务器
        a2a.stopServer()
    }
    
    @Test
    fun `test agent adapter`() = runBlocking {
        // 将 kastrax 代理转换为 A2A 代理
        val a2aAgent = a2a.adaptAgent(mockAgent)
        
        // 验证 A2A 代理
        val agentCard = a2aAgent.getAgentCard()
        assertEquals("mock-agent", agentCard.id)
        assertEquals("mock-agent", agentCard.name)
        assertTrue(agentCard.description.contains("KastraX Agent"))
    }
    
    @Test
    fun `test agent registration`() = runBlocking {
        // 创建 A2A 代理
        val a2aAgent = a2aAgent {
            id = "test-agent"
            name = "Test Agent"
            description = "A test agent"
            baseAgent = mockAgent
            
            capability {
                id = "test_capability"
                name = "Test Capability"
                description = "A test capability"
                
                parameter {
                    name = "test_param"
                    type = "string"
                    description = "A test parameter"
                    required = true
                }
                
                returnType = "json"
            }
        }
        
        // 注册 A2A 代理
        a2a.registerAgent(a2aAgent)
        
        // 验证代理注册
        val registeredAgent = a2a.getAgent("test-agent")
        assertNotNull(registeredAgent)
        assertEquals("test-agent", registeredAgent.getAgentCard().id)
        
        // 注销 A2A 代理
        a2a.unregisterAgent("test-agent")
        
        // 验证代理注销
        assertEquals(null, a2a.getAgent("test-agent"))
    }
    
    @Test
    fun `test agent capability invocation`() = runBlocking {
        // 创建 A2A 代理
        val a2aAgent = a2aAgent {
            id = "test-agent"
            name = "Test Agent"
            description = "A test agent"
            baseAgent = mockAgent
            
            capability {
                id = "test_capability"
                name = "Test Capability"
                description = "A test capability"
                
                parameter {
                    name = "prompt"
                    type = "string"
                    description = "The prompt to send to the agent"
                    required = true
                }
                
                returnType = "json"
            }
        }
        
        // 调用代理能力
        val request = InvokeRequest(
            id = "test-request",
            capabilityId = "test_capability",
            parameters = mapOf(
                "prompt" to JsonPrimitive("Hello, agent!")
            )
        )
        
        val response = a2aAgent.invoke(request)
        
        // 验证响应
        assertEquals("test-request", response.id)
        assertEquals("invoke_response", response.type)
    }
    
    @Test
    fun `test agent discovery`() = runBlocking {
        // 创建 A2A 代理
        val a2aAgent = a2aAgent {
            id = "test-agent"
            name = "Test Agent"
            description = "A test agent"
            baseAgent = mockAgent
            
            capability {
                id = "test_capability"
                name = "Test Capability"
                description = "A test capability"
                
                parameter {
                    name = "prompt"
                    type = "string"
                    description = "The prompt to send to the agent"
                    required = true
                }
                
                returnType = "json"
            }
        }
        
        // 注册 A2A 代理
        a2a.registerAgent(a2aAgent)
        
        // 启动服务器
        a2a.startServer()
        
        // 添加服务器到发现服务
        a2a.addServerToDiscovery("http://localhost:8080")
        
        // 创建客户端
        val client = a2a.createClient("http://localhost:8080")
        
        // 获取代理卡片
        val agentCard = client.getAgentCard()
        assertEquals("test-agent", agentCard.id)
        
        // 获取代理能力
        val capabilities = client.getCapabilities("test-agent")
        assertEquals(1, capabilities.size)
        assertEquals("test_capability", capabilities[0].id)
        
        // 关闭客户端
        client.close()
    }
}
