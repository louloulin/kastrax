package ai.kastrax.actor

import actor.proto.ActorSystem
import actor.proto.PID
import ai.kastrax.agent.Agent
import ai.kastrax.agent.AgentGenerateOptions
import ai.kastrax.agent.AgentGenerateResponse
import ai.kastrax.agent.AgentStreamOptions
import ai.kastrax.agent.ToolCall
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KastraxActorTest {
    
    @Test
    fun `test agent request and response`() = runTest {
        // 创建模拟 Agent
        val mockAgent = mockk<Agent>()
        coEvery { 
            mockAgent.generate(any(), any()) 
        } returns AgentGenerateResponse("这是一个测试回复", emptyList())
        
        // 创建 Actor 系统
        val system = ActorSystem("test-system")
        
        // 创建 KastraxActor
        val props = actor.proto.Props.fromProducer { KastraxActor(mockAgent) }
        val pid = system.root.spawn(props)
        
        // 发送请求并获取响应
        val response = system.root.requestAwait<AgentResponse>(
            pid, 
            AgentRequest("测试请求", AgentGenerateOptions())
        )
        
        // 验证响应
        assertEquals("这是一个测试回复", response.text)
        assertTrue(response.toolCalls.isEmpty())
        
        // 关闭系统
        system.shutdown()
    }
    
    @Test
    fun `test tool call request and response`() = runTest {
        // 创建模拟 Agent
        val mockAgent = mockk<Agent>()
        val mockResult = JsonObject(mapOf("result" to kotlinx.serialization.json.JsonPrimitive(42.0)))
        coEvery { 
            mockAgent.executeTool(any(), any()) 
        } returns mockResult
        
        // 创建 Actor 系统
        val system = ActorSystem("test-system")
        
        // 创建 KastraxActor
        val props = actor.proto.Props.fromProducer { KastraxActor(mockAgent) }
        val pid = system.root.spawn(props)
        
        // 创建工具调用请求
        val toolCallRequest = ToolCallRequest(
            "calculator",
            JsonObject(mapOf("expression" to kotlinx.serialization.json.JsonPrimitive("2 + 2")))
        )
        
        // 发送请求并获取响应
        val response = system.root.requestAwait<ToolCallResponse>(pid, toolCallRequest)
        
        // 验证响应
        assertEquals(mockResult, response.result)
        
        // 关闭系统
        system.shutdown()
    }
    
    @Test
    fun `test collaboration request and response`() = runTest {
        // 创建模拟 Agent
        val mockAgent = mockk<Agent>()
        coEvery { 
            mockAgent.generate(any(), any()) 
        } returns AgentGenerateResponse("协作回复", emptyList())
        
        // 创建 Actor 系统
        val system = ActorSystem("test-system")
        
        // 创建 KastraxActor
        val props = actor.proto.Props.fromProducer { KastraxActor(mockAgent) }
        val pid = system.root.spawn(props)
        
        // 创建协作请求
        val collaborationRequest = CollaborationRequest(
            "协作任务",
            "发送者",
            mapOf("key" to "value")
        )
        
        // 发送请求并获取响应
        val response = system.root.requestAwait<CollaborationResponse>(pid, collaborationRequest)
        
        // 验证响应
        assertEquals("协作回复", response.result)
        
        // 关闭系统
        system.shutdown()
    }
}
