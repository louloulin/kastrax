package actor.proto.guardian

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.Failure
import actor.proto.PID
import actor.proto.Props
import actor.proto.SupervisorDirective
import actor.proto.SupervisorStrategy
import actor.proto.Supervisor
import actor.proto.fromProducer
import actor.proto.send
import actor.proto.stop
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class GuardianTest {
    private lateinit var system: ActorSystem
    
    @BeforeEach
    fun setup() {
        system = ActorSystem("test-system")
    }
    
    @AfterEach
    fun teardown() {
        // 清理资源
    }
    
    @Test
    fun `should create guardian actor with strategy`() {
        // 创建一个自定义监督策略
        val strategy = TestStrategy()
        
        // 使用监督策略创建根上下文
        val rootContext = system.root().withGuardian(strategy)
        
        // 获取根上下文的 self PID，这应该是 guardian actor 的 PID
        val guardianPid = rootContext.self
        
        // 验证 guardian PID 不为空
        assertNotNull(guardianPid)
        
        // 验证 guardian PID 的地址是系统地址
        assertEquals(system.address, guardianPid.address)
        
        // 验证 guardian PID 的 ID 以 "guardian" 开头
        assert(guardianPid.id.startsWith("guardian"))
    }
    
    @Test
    fun `guardian should handle child failures`() {
        // 创建一个计数器来跟踪重启次数
        val restartCount = AtomicInteger(0)
        
        // 创建一个监督策略，总是重启失败的子 actor
        val strategy = object : SupervisorStrategy {
            override fun handleFailure(
                actorSystem: ActorSystem,
                supervisor: Supervisor,
                child: PID,
                restartStatistics: RestartStatistics,
                reason: Any,
                message: Any?
            ) {
                // 增加重启计数
                restartCount.incrementAndGet()
                
                // 重启子 actor
                supervisor.restartChildren(child)
            }
        }
        
        // 使用监督策略创建根上下文
        val rootContext = system.root().withGuardian(strategy)
        
        // 创建一个会抛出异常的 actor
        val props = fromProducer { FailingActor() }
        val childPid = rootContext.spawn(props)
        
        // 创建一个锁来等待 actor 处理完成
        val latch = CountDownLatch(1)
        
        // 发送一个消息，这将导致 actor 抛出异常
        system.send(childPid, FailMessage(latch))
        
        // 等待处理完成
        latch.await(5, TimeUnit.SECONDS)
        
        // 验证 actor 被重启了
        assertEquals(1, restartCount.get())
    }
    
    // 测试用的失败消息
    data class FailMessage(val latch: CountDownLatch)
    
    // 一个会抛出异常的 actor
    class FailingActor : Actor {
        override suspend fun Context.receive(msg: Any) {
            when (msg) {
                is FailMessage -> {
                    // 抛出一个异常，这将触发监督策略
                    try {
                        throw RuntimeException("Test exception")
                    } finally {
                        // 确保锁被释放，即使发生异常
                        msg.latch.countDown()
                    }
                }
            }
        }
    }
    
    // 测试用的监督策略
    class TestStrategy : SupervisorStrategy {
        override fun handleFailure(
            actorSystem: ActorSystem,
            supervisor: Supervisor,
            child: PID,
            restartStatistics: RestartStatistics,
            reason: Any,
            message: Any?
        ) {
            // 默认实现，不做任何事情
        }
    }
}
