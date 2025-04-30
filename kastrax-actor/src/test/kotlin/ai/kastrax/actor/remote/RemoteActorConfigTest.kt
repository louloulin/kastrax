package ai.kastrax.actor.remote

import actor.proto.ActorSystem
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

/**
 * 远程 Actor 配置测试
 */
class RemoteActorConfigTest {

    @Test
    fun `should configure remote actor system with new API`() {
        // 使用唯一的系统名称和端口
        val uniqueSystemName = "kastrax-remote-${System.currentTimeMillis()}-${System.nanoTime() % 10000}"
        val uniquePort = 28092 + (Math.random() * 1000).toInt() + (System.nanoTime() % 1000).toInt()

        // 创建远程 Actor 系统
        val system = configureRemoteActorSystem(uniquePort, uniqueSystemName)

        try {
            // 验证系统已创建
            assertNotNull(system)

            // 验证系统名称
            assert(system.name == uniqueSystemName)
        } finally {
            // 关闭系统
            system.shutdown()
        }
    }

    @Test
    fun `should configure remote actor system with config object`() {
        // 使用唯一的系统名称和端口
        val uniqueSystemName = "kastrax-remote-test-${System.currentTimeMillis()}-${System.nanoTime() % 10000}"
        val uniquePort = 28093 + (Math.random() * 1000).toInt() + (System.nanoTime() % 1000).toInt()

        // 创建配置对象
        val config = RemoteActorConfig(
            port = uniquePort,
            advertisedHostname = "localhost"
        )

        // 创建远程 Actor 系统
        val system = configureRemoteActorSystemWithConfig(uniqueSystemName, config)

        try {
            // 验证系统已创建
            assertNotNull(system)

            // 验证系统名称
            assert(system.name == uniqueSystemName)
        } finally {
            // 关闭系统
            system.shutdown()
        }
    }
}
