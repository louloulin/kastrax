package ai.kastrax.store.examples

import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.alert.AlertLevel
import ai.kastrax.store.alert.AlertManager
import ai.kastrax.store.alert.AlertRule
import ai.kastrax.store.alert.AlertType
import ai.kastrax.store.alert.NotificationHandler
import ai.kastrax.store.health.HealthStatus
import ai.kastrax.store.health.IndexHealthCheckResult
import ai.kastrax.store.health.VectorStoreHealthCheck
import ai.kastrax.store.metrics.MetricType
import ai.kastrax.store.metrics.OperationMetric
import ai.kastrax.store.metrics.VectorStoreMetrics
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 告警示例。
 */
object AlertExample {

    /**
     * 运行示例。
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 创建内存向量存储
        val vectorStore = VectorStoreFactory.createInMemoryVectorStore()

        // 添加自定义通知处理器
        AlertManager.addNotificationHandler(object : NotificationHandler {
            override suspend fun handleAlert(alert: ai.kastrax.store.alert.Alert) {
                println("CUSTOM NOTIFICATION: [${alert.level}] ${alert.message}")
            }
        })

        // 添加自定义规则
        AlertManager.addRule(AlertRule(
            id = "low-vector-count",
            name = "Low Vector Count",
            description = "Index has very few vectors",
            type = AlertType.CAPACITY,
            level = AlertLevel.INFO,
            condition = { context ->
                val indexResults = context["indexResults"] as? List<*>
                indexResults?.any { result ->
                    val details = (result as? IndexHealthCheckResult)?.details
                    details?.get("count") as? Int ?: 0 < 10
                } == true
            }
        ))

        // 创建索引
        val indexName = "example_index"
        val dimension = 3
        vectorStore.createIndex(indexName, dimension)

        // 添加向量
        val vectors = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f)
        )
        val metadata = listOf(
            mapOf("name" to "vector1"),
            mapOf("name" to "vector2")
        )
        vectorStore.upsert(indexName, vectors, metadata)

        // 执行健康检查
        println("\nPerforming health check...")
        val healthResult = VectorStoreHealthCheck.checkHealth(vectorStore)
        println("Health status: ${healthResult.status}")
        println("Message: ${healthResult.message}")

        // 评估健康检查结果
        val indexResults = healthResult.details["indexResults"] as List<*>
        AlertManager.evaluateHealthCheck(indexResults.filterIsInstance<IndexHealthCheckResult>())

        // 显示触发的告警
        println("\nTriggered alerts:")
        val alerts = AlertManager.getAlerts()
        if (alerts.isEmpty()) {
            println("  No alerts triggered")
        } else {
            alerts.forEach { alert ->
                println("  [${alert.level}] ${alert.message}")
            }
        }

        // 手动触发告警
        println("\nManually triggering alerts...")
        val alertId1 = AlertManager.triggerAlert(
            level = AlertLevel.WARNING,
            type = AlertType.PERFORMANCE,
            message = "High query latency detected",
            details = mapOf("latency" to 500)
        )
        println("  Triggered alert: $alertId1")

        val alertId2 = AlertManager.triggerAlert(
            level = AlertLevel.ERROR,
            type = AlertType.ERROR,
            message = "Failed to connect to database",
            details = mapOf("error" to "Connection timeout")
        )
        println("  Triggered alert: $alertId2")

        // 显示所有告警
        println("\nAll alerts:")
        AlertManager.getAlerts().forEach { alert ->
            println("  [${alert.level}] ${alert.type}: ${alert.message}")
            println("    Details: ${alert.details}")
            println("    Timestamp: ${alert.timestamp}")
            println("    Resolved: ${alert.resolved}")
            println()
        }

        // 解决告警
        println("Resolving alert: $alertId1")
        val resolved = AlertManager.resolveAlert(alertId1)
        println("  Alert resolved: $resolved")

        // 显示未解决的告警
        println("\nUnresolved alerts:")
        val unresolvedAlerts = AlertManager.getAlerts(includeResolved = false)
        unresolvedAlerts.forEach { alert ->
            println("  [${alert.level}] ${alert.type}: ${alert.message}")
        }

        // 模拟性能指标
        println("\nSimulating performance metrics...")
        val metrics = mapOf(
            MetricType.QUERY to OperationMetric(
                count = 100,
                totalDuration = 50.seconds,
                minDuration = 100.milliseconds,
                maxDuration = 1.seconds,
                errorCount = 5
            ),
            MetricType.UPSERT to OperationMetric(
                count = 50,
                totalDuration = 10.seconds,
                minDuration = 150.milliseconds,
                maxDuration = 500.milliseconds,
                errorCount = 0
            )
        )

        // 评估性能指标
        val metricsMap = metrics.mapKeys { it.key.name }
        AlertManager.evaluatePerformanceMetrics(metricsMap)

        // 显示触发的告警
        println("\nAlerts after performance evaluation:")
        AlertManager.getAlerts(includeResolved = false).forEach { alert ->
            println("  [${alert.level}] ${alert.type}: ${alert.message}")
        }

        // 启动监控
        println("\nStarting monitoring...")
        AlertManager.startMonitoring(interval = 5.seconds)

        // 等待一段时间
        println("Waiting for monitoring to run...")
        delay(6.seconds)

        // 停止监控
        AlertManager.stopMonitoring()
        println("Monitoring stopped")

        // 清除所有告警
        println("\nClearing all alerts...")
        AlertManager.clearAlerts()
        println("Alerts cleared: ${AlertManager.getAlerts().isEmpty()}")
    }
}
