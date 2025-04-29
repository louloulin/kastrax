package actor.proto.throttle

import actor.proto.Context
import actor.proto.MessageEnvelope
import actor.proto.PID
import actor.proto.Receive
import actor.proto.Send
import actor.proto.SenderContext
import actor.proto.logging.Logger
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ThrottleMiddlewareTest {

    @Test
    fun `throttleReceiveMiddleware should allow messages under limit`() = runBlocking {
        val messageCount = AtomicInteger(0)
        val maxMessages = 5

        // 创建接收中间件
        val middleware = throttleReceiveMiddleware(maxMessages, Duration.ofSeconds(1))

        // 创建下一个处理器
        val next: Receive = { _ -> messageCount.incrementAndGet() }

        // 应用中间件
        val handler = middleware(next)

        // 创建模拟上下文
        val mockContext = mock(Context::class.java)
        `when`(mockContext.message).thenReturn("test message")
        val mockLogger = mock(Logger::class.java)
        `when`(mockContext.logger).thenReturn(mockLogger)

        // 处理maxMessages个消息，应该都被允许通过
        repeat(maxMessages) {
            handler(mockContext)
        }

        // 验证所有消息都被处理
        assertEquals(maxMessages, messageCount.get(), "All messages should be processed")
    }

    @Test
    fun `throttleReceiveMiddleware should block messages over limit`() = runBlocking {
        val messageCount = AtomicInteger(0)
        val maxMessages = 5

        // 创建接收中间件
        val middleware = throttleReceiveMiddleware(maxMessages, Duration.ofSeconds(1))

        // 创建下一个处理器
        val next: Receive = { _ -> messageCount.incrementAndGet() }

        // 应用中间件
        val handler = middleware(next)

        // 创建模拟上下文
        val mockContext = mock(Context::class.java)
        `when`(mockContext.message).thenReturn("test message")
        val mockLogger = mock(Logger::class.java)
        `when`(mockContext.logger).thenReturn(mockLogger)

        // 处理maxMessages+3个消息，超过限制3个
        repeat(maxMessages + 3) {
            handler(mockContext)
        }

        // 验证只有maxMessages个消息被处理
        assertEquals(maxMessages, messageCount.get(), "Only messages under limit should be processed")
    }

    @Test
    fun `throttleSenderMiddleware should allow messages under limit`() = runBlocking {
        val messageCount = AtomicInteger(0)
        val maxMessages = 5

        // 创建发送中间件
        val middleware = throttleSenderMiddleware(maxMessages, Duration.ofSeconds(1))

        // 创建下一个处理器
        val next: Send = { ctx, pid, envelope -> messageCount.incrementAndGet() }

        // 应用中间件
        val handler = middleware(next)

        // 创建模拟对象
        val mockContext = mock(SenderContext::class.java)
        val realPID = PID("test-address", "test-id")
        val mockLogger = mock(Logger::class.java)
        `when`(mockContext.logger).thenReturn(mockLogger)

        // 处理maxMessages个消息，应该都被允许通过
        repeat(maxMessages) {
            val envelope = MessageEnvelope("test message", null)
            handler(mockContext, realPID, envelope)
        }

        // 验证所有消息都被处理
        assertEquals(maxMessages, messageCount.get(), "All messages should be processed")
    }

    @Test
    fun `throttleSenderMiddleware should block messages over limit`() = runBlocking {
        val messageCount = AtomicInteger(0)
        val maxMessages = 5

        // 创建发送中间件
        val middleware = throttleSenderMiddleware(maxMessages, Duration.ofSeconds(1))

        // 创建下一个处理器
        val next: Send = { ctx, pid, envelope -> messageCount.incrementAndGet() }

        // 应用中间件
        val handler = middleware(next)

        // 创建模拟对象
        val mockContext = mock(SenderContext::class.java)
        val realPID = PID("test-address", "test-id")
        val mockLogger = mock(Logger::class.java)
        `when`(mockContext.logger).thenReturn(mockLogger)

        // 处理maxMessages+3个消息，超过限制3个
        repeat(maxMessages + 3) {
            val envelope = MessageEnvelope("test message", null)
            handler(mockContext, realPID, envelope)
        }

        // 验证只有maxMessages个消息被处理
        assertEquals(maxMessages, messageCount.get(), "Only messages under limit should be processed")
    }

    @Test
    fun `throttleSenderMiddleware should handle MessageEnvelope`() = runBlocking {
        val messageCount = AtomicInteger(0)
        val maxMessages = 5

        // 创建发送中间件
        val middleware = throttleSenderMiddleware(maxMessages, Duration.ofSeconds(1))

        // 创建下一个处理器
        val next: Send = { ctx, pid, envelope -> messageCount.incrementAndGet() }

        // 应用中间件
        val handler = middleware(next)

        // 创建模拟对象
        val mockContext = mock(SenderContext::class.java)
        val realPID = PID("test-address", "test-id")
        val mockLogger = mock(Logger::class.java)
        `when`(mockContext.logger).thenReturn(mockLogger)

        // 创建消息信封
        val envelope = MessageEnvelope("test message", null)

        // 处理maxMessages个消息，应该都被允许通过
        repeat(maxMessages) {
            handler(mockContext, realPID, envelope)
        }

        // 验证所有消息都被处理
        assertEquals(maxMessages, messageCount.get(), "All messages should be processed")
    }

    @Test
    fun `throttleMessageTypeMiddleware should only throttle specific message type`() = runBlocking {
        val stringMessageCount = AtomicInteger(0)
        val intMessageCount = AtomicInteger(0)
        val maxMessages = 5

        // 创建针对String类型的节流中间件
        val middleware = throttleMessageTypeMiddleware<String>(maxMessages, Duration.ofSeconds(1))

        // 创建下一个处理器
        val next: Receive = { ctx ->
            when (ctx.message) {
                is String -> stringMessageCount.incrementAndGet()
                is Int -> intMessageCount.incrementAndGet()
            }
        }

        // 应用中间件
        val handler = middleware(next)

        // 创建模拟上下文
        val mockStringContext = mock(Context::class.java)
        `when`(mockStringContext.message).thenReturn("test message")
        val mockIntContext = mock(Context::class.java)
        `when`(mockIntContext.message).thenReturn(123)
        val mockLogger = mock(Logger::class.java)
        `when`(mockStringContext.logger).thenReturn(mockLogger)
        `when`(mockIntContext.logger).thenReturn(mockLogger)

        // 处理maxMessages+3个String消息和maxMessages+3个Int消息
        repeat(maxMessages + 3) {
            handler(mockStringContext)
            handler(mockIntContext)
        }

        // 验证只有maxMessages个String消息被处理，而所有Int消息都被处理
        assertEquals(maxMessages, stringMessageCount.get(), "Only String messages under limit should be processed")
        assertEquals(maxMessages + 3, intMessageCount.get(), "All Int messages should be processed")
    }
}
