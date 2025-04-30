package ai.kastrax.actor

import actor.proto.ActorSystem
import actor.proto.PID
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.SessionInfo
import ai.kastrax.core.agent.SessionMessage
import ai.kastrax.core.agent.AgentState
import ai.kastrax.core.agent.AgentStatus
import ai.kastrax.core.agent.version.AgentVersion
import ai.kastrax.core.agent.version.AgentVersionManager
import ai.kastrax.core.llm.LlmMessage
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration

class DslTest {

    private lateinit var system: ActorSystem

    @BeforeEach
    fun setup() {
        system = ActorSystem("test-system")
    }

    @AfterEach
    fun teardown() {
        system.shutdown()
    }

    @Test
    fun `test actorAgent DSL`() {
        // 使用 actorAgent DSL 创建 Actor 化的 Agent
        val agentPid = system.actorAgent {
            // 配置 Agent 部分
            agent {
                agentBuilder.name = "test-agent"
                agentBuilder.instructions = "你是一个测试助手。"
            }

            // 配置 Actor 部分
            actor {
                oneForOneStrategy {
                    maxRetries = 3
                    withinTimeRange = Duration.ofMinutes(1)
                }
                unboundedMailbox()
            }
        }

        // 验证 PID
        assertNotNull(agentPid)
        assertEquals("test-agent", agentPid.id)
    }

    @Test
    @Disabled("需要实际的 Agent 实现")
    fun `test agentNetwork DSL`() {
        // 使用 agentNetwork DSL 创建 Agent 网络
        val network = system.agentNetwork {
            // 设置协调者
            coordinator {
                agent {
                    agentBuilder.name = "coordinator"
                    agentBuilder.instructions = "你是一个协调者。"
                }
                actor {
                    oneForOneStrategy {
                        maxRetries = 5
                    }
                }
            }

            // 添加专家 Agent
            agent("expert1") {
                agent {
                    agentBuilder.name = "expert1"
                    agentBuilder.instructions = "你是一个专家。"
                }
            }

            agent("expert2") {
                agent {
                    agentBuilder.name = "expert2"
                    agentBuilder.instructions = "你是另一个专家。"
                }
            }
        }

        // 验证网络
        assertNotNull(network)
    }

    @Test
    @Disabled("需要实际的 Agent 实现")
    fun `test messagePassingDsl`() = runBlocking {
        // 创建一个 Agent
        val mockAgent = TestAgent("test-agent")

        // 创建 Actor
        val props = actor.proto.fromProducer { KastraxActor(mockAgent) }
        val agentPid = system.root.spawn(props)

        // 使用 sendMessage DSL 发送消息
        system.sendMessage(agentPid, "你好，我是用户")

        // 使用 askMessage DSL 发送请求并等待响应
        val response = system.askMessage(agentPid, "测试请求")
        assertEquals("[test-agent] 回复: 测试请求", response)
    }

    /**
     * 测试用 Agent 实现
     */
    private class TestAgent(override val name: String) : Agent {
        override val versionManager: AgentVersionManager? = null

        override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): AgentResponse {
            val lastMessage = messages.lastOrNull { it.role.name.equals("USER", ignoreCase = true) }?.content ?: ""
            return generate(lastMessage, options)
        }

        override suspend fun generate(prompt: String, options: AgentGenerateOptions): AgentResponse {
            // 模拟响应
            val sender = options.metadata?.get("sender") ?: "unknown"

            // 如果发送者是自己，返回简单响应以避免递归
            if (sender == name) {
                return AgentResponse(
                    text = "[自我处理] $prompt",
                    toolCalls = emptyList()
                )
            }

            return AgentResponse(
                text = "[$name] 回复: $prompt",
                toolCalls = emptyList()
            )
        }

        override suspend fun stream(prompt: String, options: AgentStreamOptions): AgentResponse {
            // 创建一个安全的选项对象，避免递归
            val safeOptions = AgentGenerateOptions(metadata = null)
            val response = generate(prompt, safeOptions)
            return AgentResponse(
                text = response.text,
                toolCalls = emptyList(),
                textStream = flowOf(response.text)
            )
        }

        override suspend fun reset() {
            // 不做任何操作
        }

        override suspend fun getState(): AgentState? {
            return null
        }

        override suspend fun updateState(status: AgentStatus): AgentState? {
            return null
        }

        override suspend fun createSession(title: String?, resourceId: String?, metadata: Map<String, String>): SessionInfo? {
            return null
        }

        override suspend fun getSession(sessionId: String): SessionInfo? {
            return null
        }

        override suspend fun getSessionMessages(sessionId: String, limit: Int): List<SessionMessage>? {
            return emptyList()
        }

        override suspend fun createVersion(
            instructions: String,
            name: String?,
            description: String?,
            metadata: Map<String, String>,
            activateImmediately: Boolean
        ): AgentVersion? {
            return null
        }

        override suspend fun getVersions(limit: Int, offset: Int): List<AgentVersion>? {
            return emptyList()
        }

        override suspend fun getActiveVersion(): AgentVersion? {
            return null
        }

        override suspend fun activateVersion(versionId: String): AgentVersion? {
            return null
        }

        override suspend fun rollbackToVersion(versionId: String): AgentVersion? {
            return null
        }
    }
}
