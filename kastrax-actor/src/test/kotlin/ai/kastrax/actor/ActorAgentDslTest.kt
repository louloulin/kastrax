package ai.kastrax.actor

import actor.proto.ActorSystem
import ai.kastrax.agent.Agent
import ai.kastrax.agent.AgentGenerateOptions
import ai.kastrax.agent.AgentGenerateResponse
import ai.kastrax.integrations.deepseek.DeepSeekModel
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.minutes

class ActorAgentDslTest {
    
    @Test
    fun `test actorAgent DSL creates actor with proper configuration`() = runTest {
        // 创建模拟 Agent 构建器
        val mockAgent = mockk<Agent>()
        coEvery { 
            mockAgent.generate(any(), any()) 
        } returns AgentGenerateResponse("测试回复", emptyList())
        
        // 创建 Actor 系统
        val system = ActorSystem("test-system")
        
        // 使用 DSL 创建 Actor
        val agentPid = system.actorAgent {
            agent {
                name = "测试助手"
                instructions = "你是一个测试助手。"
                // 注意：这里我们不实际创建 DeepSeek 模型，而是在构建器中设置属性
            }
            
            actor {
                oneForOneStrategy {
                    maxRetries = 3
                    withinTimeRange = 1.minutes
                }
                unboundedMailbox()
            }
        }
        
        // 验证 PID 不为空
        assertNotNull(agentPid)
        
        // 关闭系统
        system.shutdown()
    }
    
    @Test
    fun `test actorAgent DSL with message passing`() = runTest {
        // 创建模拟 Agent
        val mockAgent = mockk<Agent>()
        coEvery { 
            mockAgent.generate(any(), any()) 
        } returns AgentGenerateResponse("测试回复", emptyList())
        
        // 创建带有 spy 的 AgentBuilder
        val spyAgentBuilder = spyk(ai.kastrax.agent.AgentBuilder())
        coEvery { spyAgentBuilder.build() } returns mockAgent
        
        // 创建 Actor 系统
        val system = ActorSystem("test-system")
        
        // 使用自定义的 ActorAgentBuilder 创建 Actor
        val customBuilder = ActorAgentBuilder().apply {
            agentBuilder = spyAgentBuilder
            agent {
                name = "测试助手"
                instructions = "你是一个测试助手。"
            }
            actor {
                oneForOneStrategy {
                    maxRetries = 3
                    withinTimeRange = 1.minutes
                }
                unboundedMailbox()
            }
        }
        
        // 创建 Props
        var props = actor.proto.Props.fromProducer { KastraxActor(customBuilder.agentBuilder.build()) }
            .withMailbox(customBuilder.actorBuilder.mailbox)
            .withSupervisor(customBuilder.actorBuilder.supervisionStrategy)
        
        // 创建 Actor
        val pid = system.root.spawn(props)
        
        // 发送请求并获取响应
        val response = system.root.requestAwait<AgentResponse>(
            pid, 
            AgentRequest("测试请求", AgentGenerateOptions())
        )
        
        // 验证响应
        assertEquals("测试回复", response.text)
        
        // 关闭系统
        system.shutdown()
    }
}
