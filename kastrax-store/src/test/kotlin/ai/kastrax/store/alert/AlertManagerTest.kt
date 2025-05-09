package ai.kastrax.store.alert

import ai.kastrax.store.health.HealthStatus
import ai.kastrax.store.health.IndexHealthCheckResult
import ai.kastrax.store.metrics.OperationMetric
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

class AlertManagerTest {

    private val testNotificationHandler = TestNotificationHandler()

    @BeforeEach
    fun setUp() {
        AlertManager.clearAlerts()
        AlertManager.addNotificationHandler(testNotificationHandler)
    }

    @AfterEach
    fun tearDown() {
        AlertManager.clearAlerts()
    }

    @Test
    fun `test trigger alert`() = runBlocking {
        // 触发告警
        val alertId = AlertManager.triggerAlert(
            level = AlertLevel.WARNING,
            type = AlertType.PERFORMANCE,
            message = "High latency detected",
            details = mapOf("latency" to 500)
        )

        // 验证告警是否已触发
        val alerts = AlertManager.getAlerts()
        assertEquals(1, alerts.size)
        assertEquals(alertId, alerts[0].id)
        assertEquals(AlertLevel.WARNING, alerts[0].level)
        assertEquals(AlertType.PERFORMANCE, alerts[0].type)
        assertEquals("High latency detected", alerts[0].message)
        assertEquals(500, alerts[0].details["latency"])
        assertFalse(alerts[0].resolved)

        // 验证通知是否已发送
        assertEquals(1, testNotificationHandler.alerts.size)
        assertEquals(alertId, testNotificationHandler.alerts[0].id)
    }

    @Test
    fun `test resolve alert`() = runBlocking {
        // 触发告警
        val alertId = AlertManager.triggerAlert(
            level = AlertLevel.ERROR,
            type = AlertType.ERROR,
            message = "Database connection failed"
        )

        // 解决告警
        val resolved = AlertManager.resolveAlert(alertId)
        assertTrue(resolved)

        // 验证告警是否已解决
        val alerts = AlertManager.getAlerts(includeResolved = true)
        assertEquals(1, alerts.size)
        assertEquals(alertId, alerts[0].id)
        assertTrue(alerts[0].resolved)
        assertNotNull(alerts[0].resolvedAt)

        // 验证未解决的告警列表是否为空
        val unresolvedAlerts = AlertManager.getAlerts(includeResolved = false)
        assertEquals(0, unresolvedAlerts.size)
    }

    @Test
    fun `test add and remove rule`() {
        // 添加规则
        val rule = AlertRule(
            id = "test-rule",
            name = "Test Rule",
            description = "A test rule",
            type = AlertType.PERFORMANCE,
            level = AlertLevel.WARNING,
            condition = { true }
        )
        AlertManager.addRule(rule)

        // 验证规则是否已添加
        val rules = AlertManager.getRules()
        assertTrue(rules.any { it.id == "test-rule" })

        // 删除规则
        val removed = AlertManager.removeRule("test-rule")
        assertTrue(removed)

        // 验证规则是否已删除
        val updatedRules = AlertManager.getRules()
        assertFalse(updatedRules.any { it.id == "test-rule" })
    }

    @Test
    fun `test enable and disable rule`() {
        // 添加规则
        val rule = AlertRule(
            id = "test-rule",
            name = "Test Rule",
            description = "A test rule",
            type = AlertType.PERFORMANCE,
            level = AlertLevel.WARNING,
            condition = { true }
        )
        AlertManager.addRule(rule)

        // 禁用规则
        val disabled = AlertManager.disableRule("test-rule")
        assertTrue(disabled)

        // 验证规则是否已禁用
        val rules = AlertManager.getRules()
        val updatedRule = rules.find { it.id == "test-rule" }
        assertNotNull(updatedRule)
        assertFalse(updatedRule!!.enabled)

        // 启用规则
        val enabled = AlertManager.enableRule("test-rule")
        assertTrue(enabled)

        // 验证规则是否已启用
        val updatedRules = AlertManager.getRules()
        val reEnabledRule = updatedRules.find { it.id == "test-rule" }
        assertNotNull(reEnabledRule)
        assertTrue(reEnabledRule!!.enabled)
    }

    @Test
    fun `test evaluate health check`() = runBlocking {
        // 创建健康检查结果
        val indexResults = listOf(
            IndexHealthCheckResult(
                indexName = "index1",
                status = HealthStatus.HEALTHY,
                message = "Index is healthy",
                details = mapOf("dimension" to 3, "count" to 100),
                timestamp = System.currentTimeMillis(),
                duration = 100.milliseconds
            ),
            IndexHealthCheckResult(
                indexName = "index2",
                status = HealthStatus.UNHEALTHY,
                message = "Index is unhealthy",
                details = mapOf("error" to "Connection failed"),
                timestamp = System.currentTimeMillis(),
                duration = 200.milliseconds
            )
        )

        // 评估健康检查结果
        AlertManager.evaluateHealthCheck(indexResults)

        // 验证是否触发了告警
        val alerts = AlertManager.getAlerts()
        assertTrue(alerts.isNotEmpty())
        assertTrue(alerts.any { it.type == AlertType.HEALTH_CHECK })
    }

    @Test
    fun `test evaluate performance metrics`() = runBlocking {
        // 创建性能指标
        val metrics = mapOf(
            "QUERY" to OperationMetric(
                count = 100,
                totalDuration = 10000.milliseconds,
                minDuration = 50.milliseconds,
                maxDuration = 500.milliseconds,
                errorCount = 5
            ),
            "UPSERT" to OperationMetric(
                count = 50,
                totalDuration = 5000.milliseconds,
                minDuration = 80.milliseconds,
                maxDuration = 300.milliseconds,
                errorCount = 0
            )
        )

        // 添加高错误率规则
        val rule = AlertRule(
            id = "high-error-rate-test",
            name = "High Error Rate Test",
            description = "Test rule for high error rate",
            type = AlertType.ERROR,
            level = AlertLevel.ERROR,
            condition = { context ->
                val errorRates = context["errorRates"] as? Map<*, Double>
                errorRates?.any { it.value > 0.01 } == true // 1% 错误率
            }
        )
        AlertManager.addRule(rule)

        // 评估性能指标
        AlertManager.evaluatePerformanceMetrics(metrics)

        // 验证是否触发了告警
        val alerts = AlertManager.getAlerts()
        assertTrue(alerts.isNotEmpty())
        assertTrue(alerts.any { it.type == AlertType.ERROR })
    }

    /**
     * 测试通知处理器。
     */
    class TestNotificationHandler : NotificationHandler {
        val alerts = mutableListOf<Alert>()

        override suspend fun handleAlert(alert: Alert) {
            alerts.add(alert)
        }
    }
}
