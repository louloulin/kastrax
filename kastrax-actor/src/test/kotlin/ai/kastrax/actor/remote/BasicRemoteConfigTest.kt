package ai.kastrax.actor.remote

import actor.proto.ActorSystem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertNotNull

/**
 * 基本的远程 Actor 配置测试
 */
class BasicRemoteConfigTest {
    private var randomPort: Int = 0
    private var randomName: String = ""
    private lateinit var system: ActorSystem

    @BeforeEach
    fun setup() {
        // 生成随机端口和系统名称
        randomPort = 30000 + (Math.random() * 1000).toInt() + (System.nanoTime() % 1000).toInt()
        randomName = "kastrax-remote-" + UUID.randomUUID().toString() + "-" + System.currentTimeMillis()
    }

    @AfterEach
    fun teardown() {
        if (::system.isInitialized) {
            system.shutdown()
        }
    }

    @Test
    fun `should create remote actor system with new API`() {
        // 创建远程 Actor 系统
        system = configureRemoteActorSystem(randomPort, randomName)

        // 验证系统已创建
        assertNotNull(system)

        // 验证系统名称
        assert(system.name == randomName)
    }
}
