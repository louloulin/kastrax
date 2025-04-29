package actor.proto.throttle

import actor.proto.logging.Logger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ThrottleStrategiesTest {
    
    @Test
    fun `CountBasedThrottleStrategy should allow messages under limit`() {
        // 创建一个基于计数的节流策略，在1秒内最多允许10个事件
        val strategy = CountBasedThrottleStrategy(10, Duration.ofSeconds(1))
        
        // 处理9个消息，应该都被允许通过
        repeat(9) {
            assertTrue(strategy.processMessage("test message"), "Message should be allowed")
        }
    }
    
    @Test
    fun `CountBasedThrottleStrategy should allow message at limit`() {
        // 创建一个基于计数的节流策略，在1秒内最多允许10个事件
        val strategy = CountBasedThrottleStrategy(10, Duration.ofSeconds(1))
        
        // 处理9个消息
        repeat(9) {
            strategy.processMessage("test message")
        }
        
        // 第10个消息应该被允许通过（CLOSING状态）
        assertTrue(strategy.processMessage("test message"), "Message at limit should be allowed")
    }
    
    @Test
    fun `CountBasedThrottleStrategy should block messages over limit`() {
        // 创建一个基于计数的节流策略，在1秒内最多允许10个事件
        val strategy = CountBasedThrottleStrategy(10, Duration.ofSeconds(1))
        
        // 处理10个消息
        repeat(10) {
            strategy.processMessage("test message")
        }
        
        // 第11个消息应该被阻止
        assertFalse(strategy.processMessage("test message"), "Message over limit should be blocked")
    }
    
    @Test
    fun `CountBasedThrottleStrategy should call onThrottled callback`() {
        val latch = CountDownLatch(1)
        var throttledCount = 0
        
        // 创建一个基于计数的节流策略，在100毫秒内最多允许10个事件
        val strategy = CountBasedThrottleStrategy(10, Duration.ofMillis(100)) { count ->
            throttledCount = count
            latch.countDown()
        }
        
        // 处理15个消息，超过限制5个
        repeat(15) {
            strategy.processMessage("test message")
        }
        
        // 等待回调被调用
        assertTrue(latch.await(1, TimeUnit.SECONDS), "Callback should have been called")
        
        // 验证回调接收到的节流计数
        assertEquals(5, throttledCount, "Should have throttled 5 events")
    }
    
    @Test
    fun `CountBasedThrottleStrategy should reset after period`() {
        val latch = CountDownLatch(1)
        
        // 创建一个基于计数的节流策略，在100毫秒内最多允许10个事件
        val strategy = CountBasedThrottleStrategy(10, Duration.ofMillis(100)) { _ ->
            latch.countDown()
        }
        
        // 处理15个消息，超过限制
        repeat(15) {
            strategy.processMessage("test message")
        }
        
        // 等待周期结束
        assertTrue(latch.await(1, TimeUnit.SECONDS), "Period should have elapsed")
        
        // 周期结束后，应该允许新的消息通过
        assertTrue(strategy.processMessage("test message"), "Message should be allowed after period")
    }
}
