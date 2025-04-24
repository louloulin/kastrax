package ai.kastrax.core.workflow.monitoring

import ai.kastrax.core.workflow.history.ExecutionRecord
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages alert conditions and handlers.
 *
 * @property defaultHandler The default alert handler to use if no specific handler is registered for a condition.
 */
class AlertManager(
    private val defaultHandler: AlertHandler = LoggingAlertHandler()
) {
    private val conditions = ConcurrentHashMap<String, Pair<AlertCondition, AlertSeverity>>()
    private val handlers = ConcurrentHashMap<String, AlertHandler>()
    private val alerts = ConcurrentHashMap<String, Alert>()
    
    /**
     * Registers an alert condition.
     *
     * @param condition The condition to register.
     * @param severity The severity of alerts triggered by this condition.
     * @param handler The handler for alerts triggered by this condition, or null to use the default handler.
     * @return The ID of the registered condition.
     */
    fun registerCondition(
        condition: AlertCondition,
        severity: AlertSeverity,
        handler: AlertHandler? = null
    ): String {
        val id = UUID.randomUUID().toString()
        conditions[id] = Pair(condition, severity)
        if (handler != null) {
            handlers[id] = handler
        }
        return id
    }
    
    /**
     * Unregisters an alert condition.
     *
     * @param conditionId The ID of the condition to unregister.
     * @return True if the condition was unregistered, false otherwise.
     */
    fun unregisterCondition(conditionId: String): Boolean {
        val removed = conditions.remove(conditionId) != null
        handlers.remove(conditionId)
        return removed
    }
    
    /**
     * Evaluates all registered conditions against execution metrics.
     *
     * @param metrics The execution metrics to evaluate.
     * @return A list of alerts that were triggered.
     */
    fun evaluateConditions(metrics: ExecutionMetrics): List<Alert> {
        val triggeredAlerts = mutableListOf<Alert>()
        
        conditions.forEach { (conditionId, pair) ->
            val (condition, severity) = pair
            if (condition.evaluate(metrics)) {
                val alert = createAlert(
                    conditionId = conditionId,
                    workflowId = metrics.workflowId,
                    runId = metrics.runId,
                    condition = condition,
                    severity = severity
                )
                
                alerts[alert.id] = alert
                triggeredAlerts.add(alert)
                
                val handler = handlers[conditionId] ?: defaultHandler
                handler.handleAlert(alert, metrics)
            }
        }
        
        return triggeredAlerts
    }
    
    /**
     * Evaluates all registered conditions against an execution record.
     *
     * @param record The execution record to evaluate.
     * @return A list of alerts that were triggered.
     */
    fun evaluateConditions(record: ExecutionRecord): List<Alert> {
        val triggeredAlerts = mutableListOf<Alert>()
        
        conditions.forEach { (conditionId, pair) ->
            val (condition, severity) = pair
            if (condition.evaluate(record)) {
                val alert = createAlert(
                    conditionId = conditionId,
                    workflowId = record.workflowId,
                    runId = record.runId,
                    condition = condition,
                    severity = severity
                )
                
                alerts[alert.id] = alert
                triggeredAlerts.add(alert)
                
                val handler = handlers[conditionId] ?: defaultHandler
                handler.handleAlert(alert, record)
            }
        }
        
        return triggeredAlerts
    }
    
    /**
     * Gets all alerts.
     *
     * @return A list of all alerts.
     */
    fun getAlerts(): List<Alert> {
        return alerts.values.toList()
    }
    
    /**
     * Gets alerts for a specific workflow.
     *
     * @param workflowId The ID of the workflow.
     * @return A list of alerts for the workflow.
     */
    fun getAlertsForWorkflow(workflowId: String): List<Alert> {
        return alerts.values.filter { it.workflowId == workflowId }
    }
    
    /**
     * Gets alerts for a specific workflow execution.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @return A list of alerts for the workflow execution.
     */
    fun getAlertsForExecution(workflowId: String, runId: String): List<Alert> {
        return alerts.values.filter { it.workflowId == workflowId && it.runId == runId }
    }
    
    /**
     * Gets alerts with a specific severity.
     *
     * @param severity The severity to filter by.
     * @return A list of alerts with the specified severity.
     */
    fun getAlertsBySeverity(severity: AlertSeverity): List<Alert> {
        return alerts.values.filter { it.severity == severity }
    }
    
    /**
     * Gets alerts within a time range.
     *
     * @param startTime The start of the time range.
     * @param endTime The end of the time range.
     * @return A list of alerts within the time range.
     */
    fun getAlertsInTimeRange(startTime: Instant, endTime: Instant): List<Alert> {
        return alerts.values.filter { it.timestamp >= startTime && it.timestamp <= endTime }
    }
    
    /**
     * Clears all alerts.
     */
    fun clearAlerts() {
        alerts.clear()
    }
    
    /**
     * Clears alerts for a specific workflow.
     *
     * @param workflowId The ID of the workflow.
     * @return The number of alerts cleared.
     */
    fun clearAlertsForWorkflow(workflowId: String): Int {
        val alertsToRemove = alerts.entries.filter { it.value.workflowId == workflowId }
        alertsToRemove.forEach { alerts.remove(it.key) }
        return alertsToRemove.size
    }
    
    /**
     * Clears alerts for a specific workflow execution.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @return The number of alerts cleared.
     */
    fun clearAlertsForExecution(workflowId: String, runId: String): Int {
        val alertsToRemove = alerts.entries.filter { it.value.workflowId == workflowId && it.value.runId == runId }
        alertsToRemove.forEach { alerts.remove(it.key) }
        return alertsToRemove.size
    }
    
    private fun createAlert(
        conditionId: String,
        workflowId: String,
        runId: String,
        condition: AlertCondition,
        severity: AlertSeverity
    ): Alert {
        return Alert(
            id = UUID.randomUUID().toString(),
            workflowId = workflowId,
            runId = runId,
            condition = condition,
            message = "Alert triggered: ${condition.getDescription()}",
            severity = severity,
            timestamp = Instant.now(),
            metadata = mapOf("conditionId" to conditionId)
        )
    }
}
