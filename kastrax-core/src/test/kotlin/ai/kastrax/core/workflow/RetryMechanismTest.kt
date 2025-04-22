package ai.kastrax.core.workflow

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 重试机制的单元测试。
 */
class RetryMechanismTest {

    /**
     * 测试重试配置。
     */
    @Test
    fun testRetryConfig() {
        val config = RetryConfig(
            maxRetries = 3,
            initialDelay = Duration.ofMillis(100),
            maxDelay = Duration.ofSeconds(5),
            backoffFactor = 2.0,
            jitter = 0.1,
            retryableExceptions = setOf(IllegalStateException::class.java)
        )

        // 测试异常类型检查
        assertTrue(config.isRetryable(IllegalStateException()))
        assertFalse(config.isRetryable(IllegalArgumentException()))

        // 测试延迟计算
        val delay1 = config.calculateDelay(1)
        val delay2 = config.calculateDelay(2)
        val delay3 = config.calculateDelay(3)

        // 验证延迟时间随着尝试次数增加而增加
        assertTrue(delay1 <= delay2)
        assertTrue(delay2 <= delay3)

        // 验证延迟时间在合理范围内
        assertTrue(delay1 >= config.initialDelay.toMillis())
        assertTrue(delay3 <= config.maxDelay.toMillis())
    }

    /**
     * 测试withRetry函数 - 成功情况。
     */
    @Test
    fun testWithRetrySuccess() = runTest {
        val config = RetryConfig(maxRetries = 3)

        // 不需要重试的情况
        val result = withRetry(config) {
            "success"
        }

        assertEquals("success", result)
    }

    /**
     * 测试withRetry函数 - 失败后重试成功。
     */
    @Test
    fun testWithRetryFailureThenSuccess() = runTest {
        val config = RetryConfig(
            maxRetries = 3,
            initialDelay = Duration.ofMillis(10) // 使用短延迟以加快测试
        )

        var attempts = 0

        // 前两次失败，第三次成功
        val result = withRetry(config) {
            attempts++
            if (attempts < 3) {
                error("Simulated failure")
            }
            "success after retry"
        }

        assertEquals(3, attempts)
        assertEquals("success after retry", result)
    }

    /**
     * 测试withRetry函数 - 超过最大重试次数。
     */
    @Test
    fun testWithRetryMaxAttemptsExceeded() = runTest {
        val config = RetryConfig(
            maxRetries = 2,
            initialDelay = Duration.ofMillis(10) // 使用短延迟以加快测试
        )

        var attempts = 0

        // 所有尝试都失败
        try {
            withRetry(config) {
                attempts++
                error("Simulated failure")
            }

            // 如果没有抛出异常，测试失败
            assertTrue(false, "Expected exception was not thrown")
        } catch (e: IllegalStateException) {
            // 验证尝试次数
            assertEquals(3, attempts) // 初始尝试 + 2次重试
            assertEquals("Simulated failure", e.message)
        }
    }

    /**
     * 测试withRetry函数 - 不可重试的异常。
     */
    @Test
    fun testWithRetryNonRetryableException() = runTest {
        val config = RetryConfig(
            maxRetries = 3,
            initialDelay = Duration.ofMillis(10),
            retryableExceptions = setOf(IllegalStateException::class.java)
        )

        var attempts = 0

        // 抛出不可重试的异常
        try {
            withRetry(config) {
                attempts++
                throw IllegalArgumentException("Non-retryable exception")
            }

            // 如果没有抛出异常，测试失败
            assertTrue(false, "Expected exception was not thrown")
        } catch (e: IllegalArgumentException) {
            // 验证只尝试了一次
            assertEquals(1, attempts)
            assertEquals("Non-retryable exception", e.message)
        }
    }
}
