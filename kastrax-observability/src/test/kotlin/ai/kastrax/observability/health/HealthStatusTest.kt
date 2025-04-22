package ai.kastrax.observability.health

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HealthStatusTest {
    @Test
    fun testIsAvailable() {
        // UP 状态应该是可用的
        assertTrue(HealthStatus.UP.isAvailable())

        // DEGRADED 状态应该是可用的
        assertTrue(HealthStatus.DEGRADED.isAvailable())

        // DOWN 状态应该是不可用的
        assertFalse(HealthStatus.DOWN.isAvailable())
    }
}
