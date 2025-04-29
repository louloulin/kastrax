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
        // 创建远程 Actor 系统
        val system = configureRemoteActorSystem(28092)
        
        try {
            // 验证系统已创建
            assertNotNull(system)
            
            // 验证系统名称
            assert(system.name == "kastrax-remote")
        } finally {
            // 关闭系统
            system.shutdown()
        }
    }
    
    @Test
    fun `should configure remote actor system with config object`() {
        // 创建配置对象
        val config = RemoteActorConfig(
            port = 28093,
            advertisedHostname = "localhost"
        )
        
        // 创建远程 Actor 系统
        val system = configureRemoteActorSystemWithConfig("kastrax-remote-test", config)
        
        try {
            // 验证系统已创建
            assertNotNull(system)
            
            // 验证系统名称
            assert(system.name == "kastrax-remote-test")
        } finally {
            // 关闭系统
            system.shutdown()
        }
    }
}
