package ai.kastrax.store.alert

import ai.kastrax.store.health.HealthStatus
import ai.kastrax.store.health.IndexHealthCheckResult
import ai.kastrax.store.metrics.OperationMetric
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/**
 * 告警级别。
 */
enum class AlertLevel {
    /**
     * 信息。
     */
    INFO,

    /**
     * 警告。
     */
    WARNING,

    /**
     * 错误。
     */
    ERROR,

    /**
     * 严重。
     */
    CRITICAL
}

/**
 * 告警类型。
 */
enum class AlertType {
    /**
     * 健康检查。
     */
    HEALTH_CHECK,

    /**
     * 性能。
     */
    PERFORMANCE,

    /**
     * 容量。
     */
    CAPACITY,

    /**
     * 错误。
     */
    ERROR,

    /**
     * 系统。
     */
    SYSTEM
}

/**
 * 告警。
 *
 * @property id 告警 ID
 * @property level 告警级别
 * @property type 告警类型
 * @property message 告警消息
 * @property details 详细信息
 * @property timestamp 时间戳
 * @property source 来源
 * @property resolved 是否已解决
 * @property resolvedAt 解决时间
 */
data class Alert(
    val id: String,
    val level: AlertLevel,
    val type: AlertType,
    val message: String,
    val details: Map<String, Any> = emptyMap(),
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val source: String = "vector-store",
    var resolved: Boolean = false,
    var resolvedAt: LocalDateTime? = null
)

/**
 * 告警规则。
 *
 * @property id 规则 ID
 * @property name 规则名称
 * @property description 规则描述
 * @property type 告警类型
 * @property level 告警级别
 * @property enabled 是否启用
 * @property condition 条件函数
 */
data class AlertRule(
    val id: String,
    val name: String,
    val description: String,
    val type: AlertType,
    val level: AlertLevel,
    var enabled: Boolean = true,
    val condition: (Map<String, Any>) -> Boolean
)

/**
 * 通知处理器。
 */
interface NotificationHandler {
    /**
     * 处理告警。
     *
     * @param alert 告警
     */
    suspend fun handleAlert(alert: Alert)
}

/**
 * 日志通知处理器。
 */
class LogNotificationHandler : NotificationHandler {
    override suspend fun handleAlert(alert: Alert) {
        when (alert.level) {
            AlertLevel.INFO -> logger.info { "ALERT [${alert.type}]: ${alert.message}" }
            AlertLevel.WARNING -> logger.warn { "ALERT [${alert.type}]: ${alert.message}" }
            AlertLevel.ERROR -> logger.error { "ALERT [${alert.type}]: ${alert.message}" }
            AlertLevel.CRITICAL -> logger.error { "CRITICAL ALERT [${alert.type}]: ${alert.message}" }
        }
    }
}

/**
 * 告警管理器。
 */
object AlertManager {
    private val rules = ConcurrentHashMap<String, AlertRule>()
    private val alerts = ConcurrentHashMap<String, Alert>()
    private val notificationHandlers = mutableListOf<NotificationHandler>()
    private val monitoringJob = Job()
    private val monitoringScope = CoroutineScope(Dispatchers.Default + monitoringJob)
    private val isMonitoring = AtomicBoolean(false)
    private var monitoringInterval = 1.minutes

    init {
        // 添加默认通知处理器
        addNotificationHandler(LogNotificationHandler())
        
        // 添加默认规则
        addDefaultRules()
    }

    /**
     * 添加通知处理器。
     *
     * @param handler 通知处理器
     */
    fun addNotificationHandler(handler: NotificationHandler) {
        notificationHandlers.add(handler)
    }

    /**
     * 添加告警规则。
     *
     * @param rule 告警规则
     */
    fun addRule(rule: AlertRule) {
        rules[rule.id] = rule
    }

    /**
     * 删除告警规则。
     *
     * @param ruleId 规则 ID
     * @return 是否成功删除
     */
    fun removeRule(ruleId: String): Boolean {
        return rules.remove(ruleId) != null
    }

    /**
     * 获取所有规则。
     *
     * @return 规则列表
     */
    fun getRules(): List<AlertRule> {
        return rules.values.toList()
    }

    /**
     * 启用规则。
     *
     * @param ruleId 规则 ID
     * @return 是否成功启用
     */
    fun enableRule(ruleId: String): Boolean {
        val rule = rules[ruleId] ?: return false
        rule.enabled = true
        return true
    }

    /**
     * 禁用规则。
     *
     * @param ruleId 规则 ID
     * @return 是否成功禁用
     */
    fun disableRule(ruleId: String): Boolean {
        val rule = rules[ruleId] ?: return false
        rule.enabled = false
        return true
    }

    /**
     * 触发告警。
     *
     * @param level 告警级别
     * @param type 告警类型
     * @param message 告警消息
     * @param details 详细信息
     * @param source 来源
     * @return 告警 ID
     */
    suspend fun triggerAlert(
        level: AlertLevel,
        type: AlertType,
        message: String,
        details: Map<String, Any> = emptyMap(),
        source: String = "vector-store"
    ): String {
        val id = generateAlertId()
        val alert = Alert(
            id = id,
            level = level,
            type = type,
            message = message,
            details = details,
            source = source
        )
        
        alerts[id] = alert
        
        // 通知处理器
        notificationHandlers.forEach { handler ->
            try {
                handler.handleAlert(alert)
            } catch (e: Exception) {
                logger.error(e) { "Error handling alert by ${handler.javaClass.simpleName}" }
            }
        }
        
        return id
    }

    /**
     * 解决告警。
     *
     * @param alertId 告警 ID
     * @return 是否成功解决
     */
    fun resolveAlert(alertId: String): Boolean {
        val alert = alerts[alertId] ?: return false
        if (alert.resolved) {
            return true
        }
        
        alert.resolved = true
        alert.resolvedAt = LocalDateTime.now()
        return true
    }

    /**
     * 获取所有告警。
     *
     * @param includeResolved 是否包含已解决的告警
     * @return 告警列表
     */
    fun getAlerts(includeResolved: Boolean = false): List<Alert> {
        return alerts.values
            .filter { includeResolved || !it.resolved }
            .sortedByDescending { it.timestamp }
    }

    /**
     * 清除所有告警。
     */
    fun clearAlerts() {
        alerts.clear()
    }

    /**
     * 评估健康检查结果。
     *
     * @param indexResults 索引健康检查结果列表
     */
    suspend fun evaluateHealthCheck(indexResults: List<IndexHealthCheckResult>) {
        val context = mutableMapOf<String, Any>()
        context["indexResults"] = indexResults
        
        // 计算状态统计
        val statusCounts = indexResults.groupBy { it.status }
            .mapValues { it.value.size }
        context["statusCounts"] = statusCounts
        
        // 计算总体状态
        val overallStatus = when {
            statusCounts[HealthStatus.UNHEALTHY]?.let { it > 0 } == true -> HealthStatus.UNHEALTHY
            statusCounts[HealthStatus.DEGRADED]?.let { it > 0 } == true -> HealthStatus.DEGRADED
            statusCounts[HealthStatus.HEALTHY]?.let { it == indexResults.size } == true -> HealthStatus.HEALTHY
            else -> HealthStatus.DEGRADED
        }
        context["overallStatus"] = overallStatus
        
        // 评估规则
        evaluateRules(context)
    }

    /**
     * 评估性能指标。
     *
     * @param metrics 操作指标
     */
    suspend fun evaluatePerformanceMetrics(metrics: Map<String, OperationMetric>) {
        val context = mutableMapOf<String, Any>()
        context["metrics"] = metrics
        
        // 计算平均延迟
        val avgLatencies = metrics.mapValues { it.value.avgDuration }
        context["avgLatencies"] = avgLatencies
        
        // 计算错误率
        val errorRates = metrics.mapValues { 
            val metric = it.value
            if (metric.count > 0) {
                metric.errorCount.toDouble() / metric.count
            } else {
                0.0
            }
        }
        context["errorRates"] = errorRates
        
        // 评估规则
        evaluateRules(context)
    }

    /**
     * 评估规则。
     *
     * @param context 上下文
     */
    private suspend fun evaluateRules(context: Map<String, Any>) {
        rules.values
            .filter { it.enabled }
            .forEach { rule ->
                try {
                    if (rule.condition(context)) {
                        triggerAlert(
                            level = rule.level,
                            type = rule.type,
                            message = rule.name,
                            details = mapOf("description" to rule.description) + context,
                            source = "rule-engine"
                        )
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error evaluating rule ${rule.id}" }
                }
            }
    }

    /**
     * 开始监控。
     *
     * @param interval 监控间隔
     */
    fun startMonitoring(interval: Duration = 1.minutes) {
        if (isMonitoring.getAndSet(true)) {
            return
        }
        
        monitoringInterval = interval
        
        monitoringScope.launch {
            while (isMonitoring.get()) {
                try {
                    // 检查未解决的告警
                    checkUnresolvedAlerts()
                } catch (e: Exception) {
                    logger.error(e) { "Error in monitoring job" }
                }
                
                delay(monitoringInterval)
            }
        }
    }

    /**
     * 停止监控。
     */
    fun stopMonitoring() {
        isMonitoring.set(false)
    }

    /**
     * 检查未解决的告警。
     */
    private suspend fun checkUnresolvedAlerts() {
        val unresolvedAlerts = getAlerts(includeResolved = false)
        
        // 检查是否有需要自动解决的告警
        val now = LocalDateTime.now()
        unresolvedAlerts.forEach { alert ->
            val alertAge = java.time.Duration.between(alert.timestamp, now)
            
            // 如果告警超过一定时间（根据级别），自动解决
            val autoResolveThreshold = when (alert.level) {
                AlertLevel.INFO -> 1L // 1 小时
                AlertLevel.WARNING -> 4L // 4 小时
                AlertLevel.ERROR -> 24L // 24 小时
                AlertLevel.CRITICAL -> 72L // 72 小时
            }
            
            if (alertAge.toHours() >= autoResolveThreshold) {
                resolveAlert(alert.id)
                
                // 记录自动解决
                logger.info { "Auto-resolved alert ${alert.id} after ${alertAge.toHours()} hours" }
            }
        }
    }

    /**
     * 生成告警 ID。
     *
     * @return 告警 ID
     */
    private fun generateAlertId(): String {
        return "alert-${System.currentTimeMillis()}-${(Math.random() * 1000).toInt()}"
    }

    /**
     * 添加默认规则。
     */
    private fun addDefaultRules() {
        // 健康检查规则
        addRule(AlertRule(
            id = "health-check-unhealthy",
            name = "Unhealthy Index Detected",
            description = "One or more indexes are in unhealthy state",
            type = AlertType.HEALTH_CHECK,
            level = AlertLevel.ERROR,
            condition = { context ->
                val statusCounts = context["statusCounts"] as? Map<*, Int>
                statusCounts?.get(HealthStatus.UNHEALTHY)?.let { it > 0 } == true
            }
        ))
        
        addRule(AlertRule(
            id = "health-check-degraded",
            name = "Degraded Index Performance",
            description = "One or more indexes are showing degraded performance",
            type = AlertType.HEALTH_CHECK,
            level = AlertLevel.WARNING,
            condition = { context ->
                val statusCounts = context["statusCounts"] as? Map<*, Int>
                statusCounts?.get(HealthStatus.DEGRADED)?.let { it > 0 } == true
            }
        ))
        
        // 性能规则
        addRule(AlertRule(
            id = "high-query-latency",
            name = "High Query Latency",
            description = "Query operations are experiencing high latency",
            type = AlertType.PERFORMANCE,
            level = AlertLevel.WARNING,
            condition = { context ->
                val avgLatencies = context["avgLatencies"] as? Map<*, Duration>
                avgLatencies?.get("QUERY")?.let { it > 500.seconds } == true
            }
        ))
        
        addRule(AlertRule(
            id = "high-error-rate",
            name = "High Error Rate",
            description = "Operations are experiencing a high error rate",
            type = AlertType.ERROR,
            level = AlertLevel.ERROR,
            condition = { context ->
                val errorRates = context["errorRates"] as? Map<*, Double>
                errorRates?.any { it.value > 0.1 } == true // 10% 错误率
            }
        ))
    }
}
