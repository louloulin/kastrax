package ai.kastrax.actor.remote

import actor.proto.ActorSystem
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertNotNull

/**
 * 简单的远程 Actor 配置测试
 */
class SimpleRemoteConfigTest {

    @Test
    fun `should create remote actor system with new API`() {
        // 生成随机端口和系统名称
        val randomPort = 29094 + (Math.random() * 1000).toInt()
        val randomName = "kastrax-remote-" + UUID.randomUUID().toString().substring(0, 8)

        // 创建远程 Actor 系统
        val system = configureRemoteActorSystem(randomPort, randomName)

        try {
            // 验证系统已创建
            assertNotNull(system)

            // 验证系统名称
            assert(system.name == randomName)
        } finally {
            // 关闭系统
            system.shutdown()
        }
    }

    @Test
    fun `should create remote actor system with config object`() {
        // 生成随机端口和系统名称
        val randomPort = 29095 + (Math.random() * 1000).toInt()
        val randomName = "kastrax-remote-" + UUID.randomUUID().toString().substring(0, 8)

        // 创建配置对象
        val config = RemoteActorConfig(
            port = randomPort,
            advertisedHostname = "localhost"
        )

        // 创建远程 Actor 系统
        val system = configureRemoteActorSystemWithConfig(randomName, config)

        try {
            // 验证系统已创建
            assertNotNull(system)

            // 验证系统名称
            assert(system.name == randomName)
        } finally {
            // 关闭系统
            system.shutdown()
        }
    }
}
