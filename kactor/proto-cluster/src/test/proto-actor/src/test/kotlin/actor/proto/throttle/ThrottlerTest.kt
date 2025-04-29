package actor.proto.throttle

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ThrottlerTest {
    
    @Test
    fun `should throttle after max events`() {
        // 创建一个计数器，用于记录回调函数被调用的次数
        val callbackCounter = AtomicInteger(0)
        
        // 创建一个节流器，在1秒内最多允许10个事件
        val throttle = newThrottle(10, Duration.ofSeconds(1)) { throttledCount ->
            callbackCounter.addAndGet(throttledCount)
        }
        
        // 第一次调用应该返回OPEN
        val firstResult = throttle()
        assertEquals(Valve.OPEN, firstResult)
        
        // 再调用8次，应该都返回OPEN
        for (i in 1..8) {
            val result = throttle()
            assertEquals(Valve.OPEN, result)
        }
        
        // 第10次调用应该返回CLOSING
        val tenthResult = throttle()
        assertEquals(Valve.CLOSING, tenthResult)
        
        // 第11次调用应该返回CLOSED
        val eleventhResult = throttle()
        assertEquals(Valve.CLOSED, eleventhResult)
        
        // 再调用5次，应该都返回CLOSED
        for (i in 1..5) {
            val result = throttle()
            assertEquals(Valve.CLOSED, result)
        }
        
        // 等待1.5秒，让节流器重置
        Thread.sleep(1500)
        
        // 重置后，应该返回OPEN
        val afterResetResult = throttle()
        assertEquals(Valve.OPEN, afterResetResult)
        
        // 验证回调函数被调用，且参数正确
        // 我们调用了17次，超过了10次的限制，所以应该有7次被节流
        assertEquals(7, callbackCounter.get())
    }
    
    @Test
    fun `should throttle with logger`() {
        // 创建一个计数器，用于记录回调函数被调用的次数
        val callbackCounter = AtomicInteger(0)
        
        // 创建一个测试日志记录器
        val testLogger = TestLogger()
        
        // 创建一个带有日志记录的节流器，在1秒内最多允许5个事件
        val throttle = newThrottleWithLogger(testLogger, 5, Duration.ofSeconds(1)) { _, throttledCount ->
            callbackCounter.addAndGet(throttledCount)
        }
        
        // 第一次调用应该返回OPEN
        val firstResult = throttle()
        assertEquals(Valve.OPEN, firstResult)
        
        // 再调用3次，应该都返回OPEN
        for (i in 1..3) {
            val result = throttle()
            assertEquals(Valve.OPEN, result)
        }
        
        // 第5次调用应该返回CLOSING
        val fifthResult = throttle()
        assertEquals(Valve.CLOSING, fifthResult)
        
        // 第6次调用应该返回CLOSED
        val sixthResult = throttle()
        assertEquals(Valve.CLOSED, sixthResult)
        
        // 再调用4次，应该都返回CLOSED
        for (i in 1..4) {
            val result = throttle()
            assertEquals(Valve.CLOSED, result)
        }
        
        // 等待1.5秒，让节流器重置
        Thread.sleep(1500)
        
        // 重置后，应该返回OPEN
        val afterResetResult = throttle()
        assertEquals(Valve.OPEN, afterResetResult)
        
        // 验证回调函数被调用，且参数正确
        // 我们调用了11次，超过了5次的限制，所以应该有6次被节流
        assertEquals(6, callbackCounter.get())
    }
    
    @Test
    fun `should throttle messages with strategy`() {
        // 创建一个计数器，用于记录处理的消息数
        val processedCounter = AtomicInteger(0)
        
        // 创建一个计数器，用于记录被节流的消息数
        val throttledCounter = AtomicInteger(0)
        
        // 创建一个基于计数的节流策略，在1秒内最多允许3个事件
        val strategy = CountBasedThrottleStrategy(3, Duration.ofSeconds(1)) { throttledCount ->
            throttledCounter.addAndGet(throttledCount)
        }
        
        // 创建一个消息处理函数
        val handler = throttle<String>(strategy) { message ->
            processedCounter.incrementAndGet()
        }
        
        // 处理5个消息
        for (i in 1..5) {
            handler("Message $i")
        }
        
        // 应该只有3个消息被处理，2个被节流
        assertEquals(3, processedCounter.get())
        
        // 等待1.5秒，让节流器重置
        Thread.sleep(1500)
        
        // 处理2个消息
        for (i in 6..7) {
            handler("Message $i")
        }
        
        // 应该有5个消息被处理
        assertEquals(5, processedCounter.get())
        
        // 验证回调函数被调用，且参数正确
        assertEquals(2, throttledCounter.get())
    }
    
    @Test
    fun `should throttle with middleware`() {
        // 创建一个计数器，用于记录处理的消息数
        val processedCounter = AtomicInteger(0)
        
        // 创建一个计数器，用于记录被节流的消息数
        val throttledCounter = AtomicInteger(0)
        
        // 创建一个等待锁，用于等待所有消息处理完成
        val latch = CountDownLatch(1)
        
        // 创建一个接收中间件，在1秒内最多允许3个事件
        val middleware = throttleReceiveMiddleware(3, Duration.ofSeconds(1)) { throttledCount ->
            throttledCounter.addAndGet(throttledCount)
            latch.countDown()
        }
        
        // 创建一个测试上下文
        val testContext = TestContext()
        
        // 创建一个处理函数
        val handler = middleware { ctx ->
            processedCounter.incrementAndGet()
        }
        
        // 处理5个消息
        for (i in 1..5) {
            handler(testContext)
        }
        
        // 等待所有消息处理完成
        latch.await(2, TimeUnit.SECONDS)
        
        // 应该只有3个消息被处理，2个被节流
        assertEquals(3, processedCounter.get())
        
        // 验证回调函数被调用，且参数正确
        assertEquals(2, throttledCounter.get())
    }
}

/**
 * 测试用的日志记录器
 */
class TestLogger : actor.proto.logging.Logger {
    val messages = mutableListOf<String>()
    
    override fun debug(message: String, vararg args: Pair<String, Any?>) {
        messages.add("DEBUG: $message ${args.joinToString { "${it.first}=${it.second}" }}")
    }
    
    override fun info(message: String, vararg args: Pair<String, Any?>) {
        messages.add("INFO: $message ${args.joinToString { "${it.first}=${it.second}" }}")
    }
    
    override fun warning(message: String, vararg args: Pair<String, Any?>) {
        messages.add("WARNING: $message ${args.joinToString { "${it.first}=${it.second}" }}")
    }
    
    override fun error(message: String, vararg args: Pair<String, Any?>) {
        messages.add("ERROR: $message ${args.joinToString { "${it.first}=${it.second}" }}")
    }
    
    override fun error(message: String, exception: Throwable, vararg args: Pair<String, Any?>) {
        messages.add("ERROR: $message ${exception.message} ${args.joinToString { "${it.first}=${it.second}" }}")
    }
    
    override fun withContext(vararg args: Pair<String, Any?>): actor.proto.logging.Logger {
        return this
    }
}

/**
 * 测试用的上下文
 */
class TestContext : actor.proto.Context {
    override val message: Any = "Test message"
    override val sender: actor.proto.PID? = null
    override val self: actor.proto.PID = actor.proto.PID("test", "test")
    override val parent: actor.proto.PID? = null
    override val children: Set<actor.proto.PID> = emptySet()
    override val logger: actor.proto.logging.Logger = TestLogger()
    
    override fun actorSystem(): actor.proto.ActorSystem {
        throw NotImplementedError("Not implemented for test")
    }
    
    override fun respond(message: Any) {
        // Do nothing for test
    }
    
    override fun stash() {
        // Do nothing for test
    }
    
    override fun spawnChild(props: actor.proto.Props): actor.proto.PID {
        throw NotImplementedError("Not implemented for test")
    }
    
    override fun spawnPrefixChild(props: actor.proto.Props, prefix: String): actor.proto.PID {
        throw NotImplementedError("Not implemented for test")
    }
    
    override fun spawnNamedChild(props: actor.proto.Props, name: String): actor.proto.PID {
        throw NotImplementedError("Not implemented for test")
    }
    
    override fun watch(pid: actor.proto.PID) {
        // Do nothing for test
    }
    
    override fun unwatch(pid: actor.proto.PID) {
        // Do nothing for test
    }
    
    override fun setReceiveTimeout(duration: java.time.Duration) {
        // Do nothing for test
    }
    
    override fun cancelReceiveTimeout() {
        // Do nothing for test
    }
    
    override fun send(target: actor.proto.PID, message: Any) {
        // Do nothing for test
    }
    
    override fun request(target: actor.proto.PID, message: Any) {
        // Do nothing for test
    }
    
    override fun request(target: actor.proto.PID, message: Any, sender: actor.proto.PID) {
        // Do nothing for test
    }
    
    override fun reenterAfter(target: java.util.concurrent.CompletableFuture<*>, action: (Any) -> Unit) {
        // Do nothing for test
    }
}
