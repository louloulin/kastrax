package ai.kastrax.actor.cluster

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.cluster.Cluster
import actor.proto.fromProducer
import ai.kastrax.actor.KastraxActor
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
import java.time.Duration

class ClusterSystemTest {
    
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
    fun `test configureCluster creates ActorSystem with cluster`() {
        // 配置集群
        val config = ClusterConfig(
            hostname = "localhost",
            port = 0, // 使用随机端口
            clusterName = "test-cluster",
            seeds = listOf("localhost:8090")
        )
        
        // 创建集群系统
        val system = configureCluster("test-system", config)
        
        // 验证系统名称
        assertEquals("test-system", system.name)
        
        // 验证集群实例已创建
        val cluster = system.getCluster()
        assertNotNull(cluster)
        
        // 关闭系统
        system.shutdown()
    }
    
    @Test
    fun `test getCluster returns cached instance`() {
        // 获取集群实例
        val cluster1 = system.getCluster()
        val cluster2 = system.getCluster()
        
        // 验证两次获取的是同一个实例
        assertSame(cluster1, cluster2)
    }
    
    @Test
    fun `test registerClusterAgent creates and registers agent`() = runBlocking {
        // 创建测试 Agent
        val agent = TestAgent("test-agent")
        
        // 注册 Agent
        val pid = system.registerClusterAgent(agent, "test-kind", "test-id")
        
        // 验证 PID
        assertNotNull(pid)
        assertTrue(pid.id.contains("test-id"))
        
        // 获取 Agent
        val retrievedPid = system.getClusterAgent("test-kind", "test-id")
        
        // 验证获取的 PID
        assertNotNull(retrievedPid)
        assertEquals(pid.id, retrievedPid?.id)
    }
    
    @Test
    fun `test getClusterMembers returns member list`() {
        // 获取集群成员列表
        val members = system.getClusterMembers()
        
        // 验证成员列表
        assertNotNull(members)
        // 注意：在单元测试中，可能没有实际的集群成员
    }
    
    /**
     * 测试用 Agent 实现
     */
    private class TestAgent(override val name: String) : Agent {
        override val versionManager: AgentVersionManager? = null
        
        override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): AgentResponse {
            return AgentResponse(
                text = "Test response",
                toolCalls = emptyList()
            )
        }
        
        override suspend fun generate(prompt: String, options: AgentGenerateOptions): AgentResponse {
            return AgentResponse(
                text = "Test response for: $prompt",
                toolCalls = emptyList()
            )
        }
        
        override suspend fun stream(prompt: String, options: AgentStreamOptions): AgentResponse {
            return AgentResponse(
                text = "Test stream response for: $prompt",
                toolCalls = emptyList(),
                textStream = flowOf("Test", " stream", " response", " for: ", prompt)
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
