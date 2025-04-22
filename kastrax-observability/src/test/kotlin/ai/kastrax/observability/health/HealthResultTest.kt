package ai.kastrax.observability.health

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HealthResultTest {
    @Test
    fun testFactoryMethods() {
        // 测试 up 工厂方法
        val upResult = HealthResult.up(mapOf("key" to "value"))
        assertEquals(HealthStatus.UP, upResult.status)
        assertEquals("value", upResult.details["key"])
        assertNull(upResult.error)

        // 测试 degraded 工厂方法
        val exception = RuntimeException("Test exception")
        val degradedResult = HealthResult.degraded(mapOf("key" to "value"), exception)
        assertEquals(HealthStatus.DEGRADED, degradedResult.status)
        assertEquals("value", degradedResult.details["key"])
        assertEquals(exception, degradedResult.error)

        // 测试 down 工厂方法
        val downResult = HealthResult.down(mapOf("key" to "value"), exception)
        assertEquals(HealthStatus.DOWN, downResult.status)
        assertEquals("value", downResult.details["key"])
        assertEquals(exception, downResult.error)
    }
}
