package ai.kastrax.actor.remote

import actor.proto.ActorSystem
import actor.proto.PID
import ai.kastrax.actor.AgentRequest
import ai.kastrax.actor.AgentResponse
import ai.kastrax.actor.KastraxActor
import ai.kastrax.actor.MockAgent
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * 远程 Actor 配置测试
 */
class RemoteActorConfigTest {
    private lateinit var serverSystem: ActorSystem
    private lateinit var clientSystem: ActorSystem
    private lateinit var agentPid: PID

    // 使用高端口避免冲突
    private val testPort1 = 29092
    private val testPort2 = 29093

    @BeforeEach
    fun setup() {
        // 创建服务器系统 - 使用新的 API
        serverSystem = configureRemoteActorSystem(testPort1)

        // 创建客户端系统
        clientSystem = ActorSystem("client-system")

        // 创建模拟 Agent
        val mockAgent = MockAgent()

        // 在服务器系统中注册 Agent
        val props = actor.proto.fromProducer { KastraxActor(mockAgent) }
        agentPid = serverSystem.root.spawnNamed(props, "remote-agent-config")
    }

    @AfterEach
    fun teardown() {
        // 关闭系统
        serverSystem.shutdown()
        clientSystem.shutdown()
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `should configure remote actor system with new API`() = runBlocking {
        // 连接到远程系统 - 使用新的 API
        val remoteAgent = connectToRemoteSystem("localhost", testPort1)

        // 获取远程 Agent 的 PID
        val remotePid = remoteAgent.connect("remote-agent-config")

        // 验证 PID
        assertNotNull(remotePid)
        assertEquals("localhost:$testPort1", remotePid.address)
        assertEquals("remote-agent-config", remotePid.id)

        // 发送消息并验证响应
        val response = remoteAgent.ask("remote-agent-config", AgentRequest("测试请求"))
        assertNotNull(response)
        assertEquals("测试回复", (response as AgentResponse).text)
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `should configure remote actor system with config object`() = runBlocking {
        // 关闭现有系统
        serverSystem.shutdown()

        // 创建服务器系统 - 使用配置对象
        val config = RemoteActorConfig(
            port = testPort2,
            advertisedHostname = "localhost"
        )
        serverSystem = configureRemoteActorSystemWithConfig("kastrax-remote-test", config)

        // 创建模拟 Agent
        val mockAgent = MockAgent()

        // 在服务器系统中注册 Agent
        val props = actor.proto.fromProducer { KastraxActor(mockAgent) }
        agentPid = serverSystem.root.spawnNamed(props, "remote-agent-config")

        // 连接到远程系统
        val remoteAgent = connectToRemoteSystem("localhost", testPort2)

        // 获取远程 Agent 的 PID
        val remotePid = remoteAgent.connect("remote-agent-config")

        // 验证 PID
        assertNotNull(remotePid)
        assertEquals("localhost:$testPort2", remotePid.address)
        assertEquals("remote-agent-config", remotePid.id)

        // 发送消息并验证响应
        val response = remoteAgent.ask("remote-agent-config", AgentRequest("测试请求"))
        assertNotNull(response)
        assertEquals("测试回复", (response as AgentResponse).text)
    }
}
