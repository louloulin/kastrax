package ai.kastrax.actor.remote

import actor.proto.ActorSystem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

/**
 * 远程 Actor 配置测试
 */
class RemoteActorConfigTest {
    private var uniqueSystemName1: String = ""
    private var uniquePort1: Int = 0
    private var uniqueSystemName2: String = ""
    private var uniquePort2: Int = 0
    private lateinit var system1: ActorSystem
    private lateinit var system2: ActorSystem

    @BeforeEach
    fun setup() {
        // 生成唯一的系统名称和端口
        uniqueSystemName1 = "kastrax-remote-${System.currentTimeMillis()}-${System.nanoTime() % 10000}"
        uniquePort1 = 28092 + (Math.random() * 1000).toInt() + (System.nanoTime() % 1000).toInt()

        uniqueSystemName2 = "kastrax-remote-test-${System.currentTimeMillis() + 1}-${System.nanoTime() % 10000}"
        uniquePort2 = 28093 + (Math.random() * 1000).toInt() + (System.nanoTime() % 1000).toInt()
    }

    @AfterEach
    fun teardown() {
        if (::system1.isInitialized) {
            system1.shutdown()
        }
        if (::system2.isInitialized) {
            system2.shutdown()
        }
    }

    @Test
    fun `should configure remote actor system with new API`() {
        // 创建远程 Actor 系统
        system1 = configureRemoteActorSystem(uniquePort1, uniqueSystemName1, "0.0.0.0")

        // 验证系统已创建
        assertNotNull(system1)

        // 验证系统名称
        assert(system1.name == uniqueSystemName1)
    }

    @Test
    fun `should configure remote actor system with config object`() {
        // 创建配置对象
        val config = RemoteActorConfig(
            port = uniquePort2,
            advertisedHostname = "localhost"
        )

        // 创建远程 Actor 系统
        system2 = configureRemoteActorSystemWithConfig(uniqueSystemName2, config)

        // 验证系统已创建
        assertNotNull(system2)

        // 验证系统名称
        assert(system2.name == uniqueSystemName2)
    }
}
