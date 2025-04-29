package ai.kastrax.actor.cluster

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.fromProducer
import ai.kastrax.actor.AgentRequest
import ai.kastrax.actor.AgentResponse
import ai.kastrax.actor.KastraxActor
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse as CoreAgentResponse
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.system.measureTimeMillis

/**
 * 集群性能测试
 * 
 * 注意：这些测试默认是禁用的，因为它们需要实际的网络环境和较长的运行时间
 */
class ClusterPerformanceTest {
    
    private lateinit var system: ActorSystem
    
    @BeforeEach
    fun setup() {
        system = ActorSystem("performance-test-system")
    }
    
    @AfterEach
    fun teardown() {
        system.shutdown()
    }
    
    /**
     * 测试集群配置性能
     */
    @Test
    @Disabled("性能测试，需要手动运行")
    fun `test cluster configuration performance`() {
        val iterations = 1000
        
        // 测试创建集群配置的性能
        val configTime = measureTimeMillis {
            repeat(iterations) {
                val config = ClusterConfig(
                    hostname = "localhost",
                    port = 0,
                    clusterName = "test-cluster",
                    seeds = listOf("localhost:8090")
                )
                config.toKactorClusterConfig()
            }
        }
        
        println("创建 $iterations 个集群配置耗时: ${configTime}ms (平均: ${configTime.toDouble() / iterations}ms)")
        
        // 测试获取集群实例的性能（使用缓存）
        val getClusterTime = measureTimeMillis {
            repeat(iterations) {
                system.getCluster()
            }
        }
        
        println("获取 $iterations 次集群实例耗时: ${getClusterTime}ms (平均: ${getClusterTime.toDouble() / iterations}ms)")
    }
    
    /**
     * 测试集群 Agent 注册性能
     */
    @Test
    @Disabled("性能测试，需要手动运行")
    fun `test cluster agent registration performance`() = runBlocking {
        val iterations = 100
        
        // 创建测试 Agent
        val agent = PerformanceTestAgent("performance-test-agent")
        
        // 测试注册 Agent 的性能
        val registerTime = measureTimeMillis {
            repeat(iterations) { i ->
                system.registerClusterAgent(agent, "test-kind", "test-id-$i")
            }
        }
        
        println("注册 $iterations 个集群 Agent 耗时: ${registerTime}ms (平均: ${registerTime.toDouble() / iterations}ms)")
        
        // 测试获取 Agent 的性能
        val getAgentTime = measureTimeMillis {
            repeat(iterations) { i ->
                system.getClusterAgent("test-kind", "test-id-$i")
            }
        }
        
        println("获取 $iterations 个集群 Agent 耗时: ${getAgentTime}ms (平均: ${getAgentTime.toDouble() / iterations}ms)")
    }
    
    /**
     * 测试集群消息传递性能
     */
    @Test
    @Disabled("性能测试，需要手动运行")
    fun `test cluster message passing performance`() = runBlocking {
        val iterations = 100
        
        // 创建测试 Agent
        val agent = PerformanceTestAgent("performance-test-agent")
        
        // 注册 Agent
        val pid = system.registerClusterAgent(agent, "test-kind", "test-id")
        
        // 测试发送消息的性能
        val sendTime = measureTimeMillis {
            repeat(iterations) { i ->
                system.root.send(pid, AgentRequest("Test message $i"))
            }
        }
        
        println("发送 $iterations 条消息耗时: ${sendTime}ms (平均: ${sendTime.toDouble() / iterations}ms)")
        
        // 测试请求-响应的性能
        val requestTime = measureTimeMillis {
            repeat(iterations) { i ->
                system.root.requestAwait<AgentResponse>(
                    pid,
                    AgentRequest("Test request $i"),
                    Duration.ofSeconds(5)
                )
            }
        }
        
        println("发送 $iterations 个请求-响应耗时: ${requestTime}ms (平均: ${requestTime.toDouble() / iterations}ms)")
    }
    
    /**
     * 性能测试用 Agent 实现
     */
    private class PerformanceTestAgent(override val name: String) : Agent {
        override val versionManager: AgentVersionManager? = null
        
        override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): CoreAgentResponse {
            return CoreAgentResponse(
                text = "Performance test response",
                toolCalls = emptyList()
            )
        }
        
        override suspend fun generate(prompt: String, options: AgentGenerateOptions): CoreAgentResponse {
            return CoreAgentResponse(
                text = "Performance test response for: $prompt",
                toolCalls = emptyList()
            )
        }
        
        override suspend fun stream(prompt: String, options: AgentStreamOptions): CoreAgentResponse {
            return CoreAgentResponse(
                text = "Performance test stream response for: $prompt",
                toolCalls = emptyList(),
                textStream = flowOf("Performance", " test", " stream", " response", " for: ", prompt)
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
