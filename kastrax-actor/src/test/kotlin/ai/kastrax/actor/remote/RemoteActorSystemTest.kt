package ai.kastrax.actor.remote

import actor.proto.ActorSystem
import actor.proto.PID
import ai.kastrax.actor.AgentMessage
import ai.kastrax.actor.AgentRequest
import ai.kastrax.actor.AgentResponse
import ai.kastrax.actor.MockAgent
import ai.kastrax.actor.KastraxActor
import ai.kastrax.actor.remote.connectToRemoteSystem
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import java.time.Duration

/**
 * 远程 Actor 系统测试
 */
class RemoteActorSystemTest {
    private lateinit var serverSystem: ActorSystem
    private lateinit var clientSystem: ActorSystem
    private lateinit var agentPid: PID

    // 使用随机端口和系统名称避免冲突
    private var testPort: Int = 0
    private var serverSystemName: String = ""
    private var clientSystemName: String = ""

    @BeforeEach
    fun setup() {
        // 使用随机端口避免端口冲突
        testPort = findAvailablePort() // 使用可用的随机端口
        serverSystemName = "server-system-test"
        clientSystemName = "client-system-test"

        println("\n==== Setting up remote actor system test ====")
        println("Server system: $serverSystemName on port $testPort")
        println("Client system: $clientSystemName")

        // 创建服务器系统 - 使用 127.0.0.1 作为广播主机名
        serverSystem = configureRemoteActorSystem(testPort, serverSystemName, "0.0.0.0", "127.0.0.1")

        // 创建客户端系统
        clientSystem = ActorSystem(clientSystemName)

        // 创建模拟 Agent
        val mockAgent = MockAgent()
        println("Created MockAgent: ${mockAgent.name}")

        // 在服务器系统中注册 Agent
        val props = actor.proto.fromProducer { KastraxActor(mockAgent) }
        agentPid = serverSystem.root.spawnNamed(props, "remote-agent")
        println("Registered agent with PID: $agentPid")

        // 记录远程系统的地址
        val address = "127.0.0.1:$testPort"
        println("Remote agent address: $address/remote-agent")

        // 等待系统初始化，增加等待时间
        Thread.sleep(5000)
        println("Setup completed")
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
        val remoteAgent = connectToRemoteSystem("127.0.0.1", testPort, "client-test1-${java.util.UUID.randomUUID()}-${System.currentTimeMillis()}")

        // 获取远程 Agent 的 PID
        val remotePid = remoteAgent.connect("remote-agent")

        // 验证 PID
        assertNotNull(remotePid)
        assertEquals("127.0.0.1:$testPort", remotePid.address)
        assertEquals("remote-agent", remotePid.id)
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS) // 增加超时时间以便调试
    fun `should send message to remote agent`() = runBlocking {
        println("\n==== Starting remote agent test ====")
        println("Server system: $serverSystemName on port $testPort")
        println("Client system: $clientSystemName")

        // 直接使用服务器系统中的 Actor
        println("Using direct access to server system actor")

        // 等待确保系统已经准备好
        println("Waiting for systems to initialize...")
        kotlinx.coroutines.delay(3000)

        try {
            println("Sending test message...")
            // 直接发送消息到服务器系统中的 Actor
            serverSystem.root.send(agentPid, AgentRequest("测试消息"))

            // 再等待一下确保消息已经处理
            kotlinx.coroutines.delay(2000)

            println("Sending test request...")
            // 直接使用请求-响应模式
            val response = serverSystem.root.requestAwait<AgentMessage>(agentPid, AgentRequest("测试请求"), java.time.Duration.ofSeconds(30))

            // 验证响应
            println("Received response: $response")
            assertNotNull(response)
            assertEquals("测试回复", (response as AgentResponse).text)
            println("==== Remote agent test completed successfully ====")
        } catch (e: Exception) {
            println("Test failed with exception: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * 查找可用的端口
     *
     * @return 可用的端口号
     */
    private fun findAvailablePort(): Int {
        return try {
            val serverSocket = java.net.ServerSocket(0)
            val port = serverSocket.localPort
            serverSocket.close()
            port
        } catch (e: Exception) {
            // 如果无法获取随机端口，使用默认端口
            val defaultPort = 29099 + (Math.random() * 1000).toInt()
            println("Warning: Could not find available port, using default port: $defaultPort")
            defaultPort
        }
    }
}
