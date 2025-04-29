package ai.kastrax.actor.remote

import actor.proto.ActorSystem
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertNotNull

/**
 * 基本的远程 Actor 配置测试
 */
class BasicRemoteConfigTest {

    @Test
    fun `should create remote actor system with new API`() {
        // 生成随机端口
        val randomPort = 30000 + (Math.random() * 1000).toInt()
        
        // 创建远程 Actor 系统
        val system = configureRemoteActorSystem(randomPort)
        
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
}
