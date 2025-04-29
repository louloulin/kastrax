package ai.kastrax.actor.network

import actor.proto.ActorSystem

import ai.kastrax.actor.network.protocols.*
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
import org.junit.jupiter.api.Test

class AgentNetworkTest {

    private lateinit var system: ActorSystem
    private lateinit var network: AgentNetwork

    @BeforeEach
    fun setup() {
        system = ActorSystem("test-system")
        network = AgentNetwork(system)
    }

    @AfterEach
    fun teardown() {
        network.shutdown()
        system.shutdown()
    }

    @Test
    fun `test add and remove agent`() {
        // 创建测试 Agent
        val agent = TestAgent("test-agent")

        // 添加 Agent
        val pid = network.addAgent(agent)

        // 验证 PID
        assertNotNull(pid)

        // 获取 Agent PID
        val retrievedPid = network.getAgentPid("test-agent")

        // 验证获取的 PID
        assertNotNull(retrievedPid)
        assertEquals(pid.id, retrievedPid?.id)

        // 移除 Agent
        network.removeAgent("test-agent")

        // 验证 Agent 已移除
        assertNull(network.getAgentPid("test-agent"))
    }

    @Test
    fun `test connect and disconnect agents`() {
        // 创建测试 Agent
        val agent1 = TestAgent("agent1")
        val agent2 = TestAgent("agent2")

        // 添加 Agent
        network.addAgent(agent1)
        network.addAgent(agent2)

        // 连接 Agent
        network.connectAgents("agent1", "agent2", AgentRelation.PEER)

        // 验证连接
        val connectedAgents = network.getConnectedAgents("agent1")
        assertEquals(1, connectedAgents.size)
        assertEquals("agent2", connectedAgents[0])

        // 验证反向连接
        val reverseConnectedAgents = network.getConnectedAgents("agent2")
        assertEquals(1, reverseConnectedAgents.size)
        assertEquals("agent1", reverseConnectedAgents[0])

        // 断开连接
        network.disconnectAgents("agent1", "agent2")

        // 验证连接已断开
        assertTrue(network.getConnectedAgents("agent1").isEmpty())
        assertTrue(network.getConnectedAgents("agent2").isEmpty())
    }

    @Test
    fun `test agent relations`() {
        // 创建测试 Agent
        val agent1 = TestAgent("agent1")
        val agent2 = TestAgent("agent2")
        val agent3 = TestAgent("agent3")

        // 添加 Agent
        network.addAgent(agent1)
        network.addAgent(agent2)
        network.addAgent(agent3)

        // 建立不同类型的关系
        network.connectAgents("agent1", "agent2", AgentRelation.MASTER)
        network.connectAgents("agent1", "agent3", AgentRelation.PEER)

        // 验证 MASTER 关系
        val slaves = network.getAgentsByRelation("agent1", AgentRelation.MASTER)
        assertEquals(1, slaves.size)
        assertEquals("agent2", slaves[0])

        // 验证 SLAVE 关系（反向关系）
        val masters = network.getAgentsByRelation("agent2", AgentRelation.SLAVE)
        assertEquals(1, masters.size)
        assertEquals("agent1", masters[0])

        // 验证 PEER 关系
        val peers = network.getAgentsByRelation("agent1", AgentRelation.PEER)
        assertEquals(1, peers.size)
        assertEquals("agent3", peers[0])
    }

    @Test
    fun `test sequential collaboration protocol`() = runBlocking {
        // 创建测试 Agent
        val agent1 = TestAgent("agent1")
        val agent2 = TestAgent("agent2")
        val agent3 = TestAgent("agent3")

        // 添加 Agent
        network.addAgent(agent1)
        network.addAgent(agent2)
        network.addAgent(agent3)

        // 创建顺序协作协议
        val protocol = SequentialProtocol(listOf("agent1", "agent2", "agent3"))

        // 执行协作
        val result = network.collaborate(protocol, "agent1", "测试任务")

        // 验证结果
        assertTrue(result.success)
        assertEquals(3, result.participants.size)
        assertEquals(3, result.steps.size)

        // 验证步骤顺序
        assertEquals("agent1", result.steps[0].agentId)
        assertEquals("agent2", result.steps[1].agentId)
        assertEquals("agent3", result.steps[2].agentId)
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
            return AgentResponse(
                text = "[$name] 回复 $sender: $prompt",
                toolCalls = emptyList()
            )
        }

        override suspend fun stream(prompt: String, options: AgentStreamOptions): AgentResponse {
            val response = generate(prompt, AgentGenerateOptions())
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
