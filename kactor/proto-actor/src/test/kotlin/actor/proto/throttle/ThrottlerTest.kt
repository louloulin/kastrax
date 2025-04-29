package actor.proto.throttle

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ThrottlerTest {
    
    @Test
    fun `should return OPEN when under limit`() {
        // 创建一个节流器，在1秒内最多允许10个事件
        val throttler = newThrottle(10, Duration.ofSeconds(1)) { _ -> }
        
        // 调用节流器9次，应该都返回OPEN
        repeat(9) {
            assertEquals(Valve.OPEN, throttler())
        }
    }
    
    @Test
    fun `should return CLOSING when at limit`() {
        // 创建一个节流器，在1秒内最多允许10个事件
        val throttler = newThrottle(10, Duration.ofSeconds(1)) { _ -> }
        
        // 调用节流器9次
        repeat(9) {
            throttler()
        }
        
        // 第10次调用应该返回CLOSING
        assertEquals(Valve.CLOSING, throttler())
    }
    
    @Test
    fun `should return CLOSED when over limit`() {
        // 创建一个节流器，在1秒内最多允许10个事件
        val throttler = newThrottle(10, Duration.ofSeconds(1)) { _ -> }
        
        // 调用节流器10次
        repeat(10) {
            throttler()
        }
        
        // 第11次调用应该返回CLOSED
        assertEquals(Valve.CLOSED, throttler())
    }
    
    @Test
    fun `should call callback with throttled count`() {
        val latch = CountDownLatch(1)
        var throttledCount = 0
        
        // 创建一个节流器，在1秒内最多允许10个事件
        val throttler = newThrottle(10, Duration.ofMillis(100)) { count ->
            throttledCount = count
            latch.countDown()
        }
        
        // 调用节流器15次，超过限制5次
        repeat(15) {
            throttler()
        }
        
        // 等待回调被调用
        assertTrue(latch.await(1, TimeUnit.SECONDS), "Callback should have been called")
        
        // 验证回调接收到的节流计数
        assertEquals(5, throttledCount, "Should have throttled 5 events")
    }
    
    @Test
    fun `should reset after period`() {
        val latch = CountDownLatch(1)
        
        // 创建一个节流器，在100毫秒内最多允许10个事件
        val throttler = newThrottle(10, Duration.ofMillis(100)) { _ ->
            latch.countDown()
        }
        
        // 调用节流器15次，超过限制
        repeat(15) {
            throttler()
        }
        
        // 等待周期结束
        assertTrue(latch.await(1, TimeUnit.SECONDS), "Period should have elapsed")
        
        // 周期结束后，阀门应该重新打开
        assertEquals(Valve.OPEN, throttler())
    }
}
