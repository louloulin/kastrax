package ai.kastrax.actor.remote

import actor.proto.ActorSystem
import actor.proto.PID
import ai.kastrax.actor.AgentRequest
import ai.kastrax.actor.AgentResponse
import ai.kastrax.actor.MockAgent
import ai.kastrax.actor.KastraxActor
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * 远程 Actor 系统测试
 */
class RemoteActorSystemTest {
    private lateinit var serverSystem: ActorSystem
    private lateinit var clientSystem: ActorSystem
    private lateinit var agentPid: PID

    // 使用随机端口避免冲突
    private val testPort = 28091

    @BeforeEach
    fun setup() {
        // 创建服务器系统
        serverSystem = configureRemoteActorSystem(testPort)

        // 创建客户端系统
        clientSystem = ActorSystem("client-system")

        // 创建模拟 Agent
        val mockAgent = MockAgent()

        // 在服务器系统中注册 Agent
        val props = actor.proto.fromProducer { KastraxActor(mockAgent) }
        agentPid = serverSystem.root.spawnNamed(props, "remote-agent")
    }

    @AfterEach
    fun teardown() {
        // 关闭系统
        serverSystem.shutdown()
        clientSystem.shutdown()
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `should connect to remote actor system`() = runBlocking {
        // 连接到远程系统
        val remoteAgent = connectToRemoteSystem("localhost", testPort)

        // 获取远程 Agent 的 PID
        val remotePid = remoteAgent.connect("remote-agent")

        // 验证 PID
        assertNotNull(remotePid)
        assertEquals("localhost:$testPort", remotePid.address)
        assertEquals("remote-agent", remotePid.id)
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `should send message to remote agent`() = runBlocking {
        // 连接到远程系统
        val remoteAgent = connectToRemoteSystem("localhost", testPort)

        // 发送消息
        remoteAgent.send("remote-agent", AgentRequest("测试消息"))

        // 使用请求-响应模式
        val response = remoteAgent.ask("remote-agent", AgentRequest("测试请求"))

        // 验证响应
        assertNotNull(response)
        assertEquals("测试回复", (response as AgentResponse).text)
    }
}
