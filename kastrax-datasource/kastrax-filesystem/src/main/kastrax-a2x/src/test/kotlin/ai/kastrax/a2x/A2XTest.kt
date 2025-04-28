package ai.kastrax.a2x

import ai.kastrax.a2a.agent.A2AAgent
import ai.kastrax.a2a.model.AgentCard
import ai.kastrax.a2a.model.Capability
import ai.kastrax.a2a.model.Parameter
import ai.kastrax.a2x.entity.Entity
import ai.kastrax.a2x.model.*
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A2X 测试
 */
class A2XTest {
    /**
     * A2X 实例
     */
    private lateinit var a2x: A2X
    
    /**
     * 模拟的 kastrax 代理
     */
    private lateinit var mockAgent: Agent
    
    /**
     * 模拟的 A2A 代理
     */
    private lateinit var mockA2AAgent: A2AAgent
    
    @BeforeEach
    fun setup() {
        // 创建 A2X 实例
        a2x = A2X.getInstance()
        
        // 创建模拟的 kastrax 代理
        mockAgent = mockk<Agent>()
        
        // 配置模拟代理的行为
        coEvery { mockAgent.name } returns "mock-agent"
        coEvery { mockAgent.generate(any<String>()) } returns AgentResponse(
            text = "This is a mock response",
            toolCalls = emptyList()
        )
        
        // 创建模拟的 A2A 代理
        mockA2AAgent = mockk<A2AAgent>()
        
        // 配置模拟 A2A 代理的行为
        coEvery { mockA2AAgent.getAgentCard() } returns AgentCard(
            id = "mock-a2a-agent",
            name = "Mock A2A Agent",
            description = "A mock A2A agent for testing",
            version = "1.0.0",
            endpoint = "http://localhost:8080/agents/mock-a2a-agent",
            capabilities = listOf(
                Capability(
                    id = "test_capability",
                    name = "Test Capability",
                    description = "A test capability",
                    parameters = listOf(
                        Parameter(
                            name = "param1",
                            type = "string",
                            description = "A test parameter",
                            required = true
                        )
                    ),
                    returnType = "json"
                )
            ),
            authentication = ai.kastrax.a2a.model.Authentication(
                type = ai.kastrax.a2a.model.AuthType.NONE
            )
        )
        
        coEvery { mockA2AAgent.getCapabilities() } returns mockA2AAgent.getAgentCard().capabilities
        
        coEvery { mockA2AAgent.invoke(any()) } returns ai.kastrax.a2a.model.InvokeResponse(
            id = "test-response",
            result = JsonPrimitive("This is a mock response"),
            metadata = emptyMap()
        )
        
        coEvery { mockA2AAgent.query(any()) } returns ai.kastrax.a2a.model.QueryResponse(
            id = "test-response",
            result = JsonPrimitive("This is a mock response"),
            metadata = emptyMap()
        )
        
        coEvery { mockA2AAgent.processMessage(any()) } returns ai.kastrax.a2a.model.InvokeResponse(
            id = "test-response",
            result = JsonPrimitive("This is a mock response"),
            metadata = emptyMap()
        )
        
        coEvery { mockA2AAgent.start() } returns Unit
        coEvery { mockA2AAgent.stop() } returns Unit
    }
    
    @AfterEach
    fun tearDown() {
        // 清理注册的实体
        a2x.getAllEntities().forEach { entity ->
            a2x.unregisterEntity(entity.getEntityCard().id)
        }
        
        // 停止服务器
        a2x.stopServer()
    }
    
    @Test
    fun `test adapt agent`() {
        // 适配 kastrax 代理
        val entity = a2x.adaptAgent(mockAgent)
        
        // 验证实体
        assertNotNull(entity)
        assertEquals("mock-agent", entity.getEntityCard().id)
        assertEquals(EntityType.AGENT, entity.getEntityCard().type)
    }
    
    @Test
    fun `test adapt a2a agent`() {
        // 适配 A2A 代理
        val entity = a2x.adaptA2AAgent(mockA2AAgent)
        
        // 验证实体
        assertNotNull(entity)
        assertEquals("mock-a2a-agent", entity.getEntityCard().id)
        assertEquals(EntityType.AGENT, entity.getEntityCard().type)
    }
    
    @Test
    fun `test register entity`() {
        // 适配并注册 kastrax 代理
        val entity = a2x.adaptAgent(mockAgent)
        a2x.registerEntity(entity)
        
        // 验证实体注册
        val registeredEntity = a2x.getEntity("mock-agent")
        assertNotNull(registeredEntity)
        assertEquals("mock-agent", registeredEntity.getEntityCard().id)
    }
    
    @Test
    fun `test get entities by type`() {
        // 适配并注册 kastrax 代理
        val entity = a2x.adaptAgent(mockAgent)
        a2x.registerEntity(entity)
        
        // 获取代理类型的实体
        val agentEntities = a2x.getEntitiesByType(EntityType.AGENT)
        assertEquals(1, agentEntities.size)
        assertEquals("mock-agent", agentEntities[0].getEntityCard().id)
        
        // 获取系统类型的实体
        val systemEntities = a2x.getEntitiesByType(EntityType.SYSTEM)
        assertTrue(systemEntities.isNotEmpty())
        assertEquals(EntityType.SYSTEM, systemEntities[0].getEntityCard().type)
    }
    
    @Test
    fun `test invoke entity capability`() = runBlocking {
        // 适配并注册 kastrax 代理
        val entity = a2x.adaptAgent(mockAgent)
        a2x.registerEntity(entity)
        
        // 创建调用请求
        val request = InvokeRequest(
            id = "test-request",
            source = a2x.createLocalEntityReference("test-client", EntityType.AGENT),
            target = a2x.createLocalEntityReference("mock-agent", EntityType.AGENT),
            capabilityId = "generate",
            parameters = mapOf(
                "prompt" to JsonPrimitive("Hello, agent!")
            )
        )
        
        // 调用实体能力
        val response = entity.invoke(request)
        
        // 验证响应
        assertNotNull(response)
        assertEquals("test-request", response.id)
        assertEquals("mock-agent", response.source.id)
        assertEquals("test-client", response.target.id)
    }
    
    @Test
    fun `test send event`() = runBlocking {
        // 适配并注册 kastrax 代理
        val entity = a2x.adaptAgent(mockAgent)
        a2x.registerEntity(entity)
        
        // 创建事件
        val event = EventMessage(
            id = "test-event",
            source = a2x.createLocalEntityReference("test-client", EntityType.AGENT),
            target = a2x.createLocalEntityReference("*", EntityType.AGENT),
            eventType = "test_event",
            data = buildJsonObject {
                put("message", JsonPrimitive("This is a test event"))
            }
        )
        
        // 收集事件
        val events = mutableListOf<EventMessage>()
        val job = launch {
            a2x.eventFlow.take(1).toList(events)
        }
        
        // 发送事件
        a2x.sendEvent(event)
        
        // 等待事件收集
        withTimeout(5000) {
            while (events.isEmpty()) {
                delay(100)
            }
        }
        
        // 验证事件
        assertEquals(1, events.size)
        assertEquals("test-event", events[0].id)
        assertEquals("test_event", events[0].eventType)
    }
    
    @Test
    fun `test system entity`() = runBlocking {
        // 获取系统实体
        val systemEntities = a2x.getEntitiesByType(EntityType.SYSTEM)
        assertTrue(systemEntities.isNotEmpty())
        
        val systemEntity = systemEntities[0]
        
        // 验证系统实体
        assertEquals(EntityType.SYSTEM, systemEntity.getEntityCard().type)
        assertTrue(systemEntity.getCapabilities().isNotEmpty())
        
        // 创建系统信息请求
        val request = InvokeRequest(
            id = "system-info-request",
            source = a2x.createLocalEntityReference("test-client", EntityType.AGENT),
            target = a2x.createLocalEntityReference(systemEntity.getEntityCard().id, EntityType.SYSTEM),
            capabilityId = "system_info",
            parameters = emptyMap()
        )
        
        // 调用系统信息能力
        val response = systemEntity.invoke(request)
        
        // 验证响应
        assertNotNull(response)
        assertEquals("system-info-request", response.id)
        assertEquals(systemEntity.getEntityCard().id, response.source.id)
        assertEquals("test-client", response.target.id)
    }
}
