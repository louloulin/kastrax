package ai.kastrax.actor.remote

import actor.proto.ActorSystem
import ai.kastrax.actor.AgentRequest
import ai.kastrax.actor.AgentResponse
import ai.kastrax.actor.MockAgent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RemoteActorTest {
    @Test
    fun `test remote actor configuration`() {
        // 创建远程配置
        val config = RemoteActorConfig(
            hostname = "127.0.0.1",
            port = 8091,
            advertisedHostname = "test-host",
            advertisedPort = 8092
        )

        // 转换为 kactor 配置
        val kactorConfig = config.toKactorRemoteConfig()

        // 验证配置转换正确
        assertEquals("127.0.0.1", kactorConfig.hostname)
        assertEquals(8091, kactorConfig.port)
        assertEquals("test-host", kactorConfig.advertisedHostname)
        assertEquals(8092, kactorConfig.advertisedPort)
    }

    @Test
    fun `test remote address formatting`() {
        val address = remoteAddress("localhost", 8090)
        assertEquals("localhost:8090", address)
    }

    @Test
    fun `test remote agent connection`() {
        // 创建本地 Actor 系统
        val system = ActorSystem("test-system")

        // 创建远程 Agent 连接
        val remoteAgent = RemoteAgent(system, "localhost:8090")

        // 连接到远程 Agent
        val pid = remoteAgent.connect("testAgent")

        // 验证 PID 正确
        assertEquals("localhost:8090", pid.address)
        assertEquals("testAgent", pid.id)
    }
}
