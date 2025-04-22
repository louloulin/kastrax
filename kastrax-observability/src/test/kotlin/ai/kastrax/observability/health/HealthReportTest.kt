package ai.kastrax.observability.health

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HealthReportTest {
    @Test
    fun testIsAvailable() {
        // UP 状态的报告应该是可用的
        val upReport = HealthReport(HealthStatus.UP, emptyMap())
        assertTrue(upReport.isAvailable())

        // DEGRADED 状态的报告应该是可用的
        val degradedReport = HealthReport(HealthStatus.DEGRADED, emptyMap())
        assertTrue(degradedReport.isAvailable())

        // DOWN 状态的报告应该是不可用的
        val downReport = HealthReport(HealthStatus.DOWN, emptyMap())
        assertFalse(downReport.isAvailable())
    }

    @Test
    fun testGetUnhealthyChecks() {
        // 创建测试健康检查结果
        val checks = mapOf(
            "up" to HealthResult.up(),
            "degraded" to HealthResult.degraded(),
            "down" to HealthResult.down()
        )

        // 创建健康报告
        val report = HealthReport(HealthStatus.DOWN, checks)

        // 获取不健康的检查
        val unhealthyChecks = report.getUnhealthyChecks()

        // 验证结果
        assertEquals(2, unhealthyChecks.size)
        assertTrue(unhealthyChecks.containsKey("degraded"))
        assertTrue(unhealthyChecks.containsKey("down"))
        assertFalse(unhealthyChecks.containsKey("up"))
    }

    @Test
    fun testToJson() {
        // 创建测试健康检查结果
        val upResult = HealthResult.up(mapOf("message" to "All good"))
        val degradedResult = HealthResult.degraded(
            mapOf("message" to "Slow response"),
            RuntimeException("Response time exceeded threshold")
        )
        val downResult = HealthResult.down(
            mapOf("message" to "Connection failed"),
            RuntimeException("Connection refused")
        )

        val checks = mapOf(
            "up" to upResult,
            "degraded" to degradedResult,
            "down" to downResult
        )

        // 创建健康报告
        val timestamp = Instant.parse("2023-01-01T12:00:00Z")
        val report = HealthReport(HealthStatus.DOWN, checks, timestamp)

        // 转换为 JSON
        val json = report.toJson()

        // 验证 JSON 包含预期的内容
        assertTrue(json.contains("\"status\":\"DOWN\""))
        assertTrue(json.contains("\"timestamp\":\"2023-01-01T12:00:00Z\""))
        assertTrue(json.contains("\"up\":{\"status\":\"UP\""))
        assertTrue(json.contains("\"degraded\":{\"status\":\"DEGRADED\""))
        assertTrue(json.contains("\"down\":{\"status\":\"DOWN\""))
        assertTrue(json.contains("\"message\":\"All good\""))
        assertTrue(json.contains("\"message\":\"Slow response\""))
        assertTrue(json.contains("\"message\":\"Connection failed\""))
        assertTrue(json.contains("\"error\":\"Response time exceeded threshold\""))
        assertTrue(json.contains("\"error\":\"Connection refused\""))
    }
}
