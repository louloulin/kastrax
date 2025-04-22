package ai.kastrax.observability.health.checks

import ai.kastrax.observability.health.HealthStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DiskSpaceHealthCheckTest {
    @Test
    fun testDiskSpaceHealthCheck(@TempDir tempDir: Path) {
        // 创建磁盘空间健康检查，使用非常高的阈值，确保测试通过
        val healthCheck = DiskSpaceHealthCheck(
            path = tempDir.toString(),
            warningThreshold = 0.99,
            criticalThreshold = 0.999
        )

        // 执行健康检查
        val result = healthCheck.check()

        // 验证结果
        assertEquals(HealthStatus.UP, result.status)
        assertNull(result.error)
        assertTrue(result.details.isNotEmpty())
        assertTrue(result.details.containsKey("path"))
        assertTrue(result.details.containsKey("total_space"))
        assertTrue(result.details.containsKey("free_space"))
        assertTrue(result.details.containsKey("usable_space"))
        assertTrue(result.details.containsKey("used_space"))
        assertTrue(result.details.containsKey("usage_ratio"))
    }

    @Test
    fun testDiskSpaceHealthCheckWithNonExistentPath() {
        // 创建磁盘空间健康检查，使用不存在的路径
        val healthCheck = DiskSpaceHealthCheck(
            path = "/path/that/does/not/exist",
            warningThreshold = 0.8,
            criticalThreshold = 0.9
        )

        // 执行健康检查
        val result = healthCheck.check()

        // 验证结果
        assertEquals(HealthStatus.DOWN, result.status)
        assertNotNull(result.error)
        assertEquals("Path does not exist: /path/that/does/not/exist", result.error?.message)
    }

    @Test
    fun testGetName() {
        val healthCheck = DiskSpaceHealthCheck()
        assertEquals("disk_space", healthCheck.getName())
    }
}
