package ai.kastrax.actor.remote

import actor.proto.ActorSystem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertNotNull

/**
 * 简单的远程 Actor 配置测试
 */
class SimpleRemoteConfigTest {
    private var randomPort1: Int = 0
    private var randomName1: String = ""
    private var randomPort2: Int = 0
    private var randomName2: String = ""
    private lateinit var system1: ActorSystem
    private lateinit var system2: ActorSystem

    @BeforeEach
    fun setup() {
        // 生成随机端口和系统名称
        randomPort1 = 29094 + (Math.random() * 1000).toInt() + (System.nanoTime() % 1000).toInt()
        randomName1 = "kastrax-remote-" + UUID.randomUUID().toString() + "-" + System.currentTimeMillis()

        randomPort2 = 29095 + (Math.random() * 1000).toInt() + (System.nanoTime() % 1000).toInt()
        randomName2 = "kastrax-remote-" + UUID.randomUUID().toString() + "-" + (System.currentTimeMillis() + 1)
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
    fun `should create remote actor system with new API`() {
        // 创建远程 Actor 系统
        system1 = configureRemoteActorSystem(randomPort1, randomName1)

        // 验证系统已创建
        assertNotNull(system1)

        // 验证系统名称
        assert(system1.name == randomName1)
    }

    @Test
    fun `should create remote actor system with config object`() {
        // 创建配置对象
        val config = RemoteActorConfig(
            port = randomPort2,
            advertisedHostname = "localhost"
        )

        // 创建远程 Actor 系统
        system2 = configureRemoteActorSystemWithConfig(randomName2, config)

        // 验证系统已创建
        assertNotNull(system2)

        // 验证系统名称
        assert(system2.name == randomName2)
    }
}
