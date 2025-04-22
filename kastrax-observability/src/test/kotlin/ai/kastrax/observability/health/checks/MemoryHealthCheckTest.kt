package ai.kastrax.observability.health.checks

import ai.kastrax.observability.health.HealthStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MemoryHealthCheckTest {
    @Test
    fun testMemoryHealthCheck() {
        // 创建内存健康检查，使用非常高的阈值，确保测试通过
        val healthCheck = MemoryHealthCheck(
            warningThreshold = 0.99,
            criticalThreshold = 0.999
        )

        // 执行健康检查
        val result = healthCheck.check()

        // 验证结果
        assertEquals(HealthStatus.UP, result.status)
        assertNull(result.error)
        assertTrue(result.details.isNotEmpty())
        assertTrue(result.details.containsKey("max_memory"))
        assertTrue(result.details.containsKey("total_memory"))
        assertTrue(result.details.containsKey("free_memory"))
        assertTrue(result.details.containsKey("used_memory"))
        assertTrue(result.details.containsKey("usage_ratio"))
    }

    @Test
    fun testMemoryHealthCheckWithLowThresholds() {
        // 创建内存健康检查，使用非常低的阈值，确保测试触发警告或错误
        val healthCheck = MemoryHealthCheck(
            warningThreshold = 0.0001,
            criticalThreshold = 0.001
        )

        // 执行健康检查
        val result = healthCheck.check()

        // 验证结果 - 应该是 DEGRADED 或 DOWN
        assertTrue(result.status == HealthStatus.DEGRADED || result.status == HealthStatus.DOWN)
        assertNotNull(result.error)
        assertTrue(result.details.isNotEmpty())
    }

    @Test
    fun testGetName() {
        val healthCheck = MemoryHealthCheck()
        assertEquals("memory", healthCheck.getName())
    }
}
