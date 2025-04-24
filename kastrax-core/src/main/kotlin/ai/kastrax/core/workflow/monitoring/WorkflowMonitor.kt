package ai.kastrax.core.workflow.monitoring

import ai.kastrax.core.workflow.history.ExecutionRecord
import ai.kastrax.core.workflow.history.WorkflowExecutionHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Monitors workflow executions in real-time.
 *
 * @property metricsCollector The metrics collector to use.
 * @property executionHistory The execution history to use.
 * @property alertManager The alert manager to use.
 */
class WorkflowMonitor(
    private val metricsCollector: WorkflowMetricsCollector,
    private val executionHistory: WorkflowExecutionHistory,
    private val alertManager: AlertManager
) {
    private val monitoringJobs = ConcurrentHashMap<String, Job>()
    private val monitoringScope = CoroutineScope(Dispatchers.Default)
    
    private val _metricsFlow = MutableSharedFlow<ExecutionMetrics>()
    val metricsFlow: Flow<ExecutionMetrics> = _metricsFlow.asSharedFlow()
    
    private val _alertsFlow = MutableSharedFlow<Alert>()
    val alertsFlow: Flow<Alert> = _alertsFlow.asSharedFlow()
    
    /**
     * Starts monitoring a workflow execution.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @param pollingInterval The interval at which to poll for updates.
     * @return True if monitoring was started, false if it was already being monitored.
     */
    fun startMonitoring(
        workflowId: String,
        runId: String,
        pollingInterval: Duration = Duration.ofSeconds(5)
    ): Boolean {
        val key = getMonitoringKey(workflowId, runId)
        if (monitoringJobs.containsKey(key)) {
            return false
        }
        
        val job = monitoringScope.launch {
            while (true) {
                val metrics = metricsCollector.getExecutionMetrics(workflowId, runId)
                if (metrics != null) {
                    _metricsFlow.emit(metrics)
                    
                    val alerts = alertManager.evaluateConditions(metrics)
                    alerts.forEach { _alertsFlow.emit(it) }
                    
                    if (metrics.status != ExecutionStatus.RUNNING && metrics.status != ExecutionStatus.SUSPENDED) {
                        // Execution is complete, stop monitoring
                        stopMonitoring(workflowId, runId)
                        break
                    }
                }
                
                delay(pollingInterval.toMillis())
            }
        }
        
        monitoringJobs[key] = job
        return true
    }
    
    /**
     * Stops monitoring a workflow execution.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @return True if monitoring was stopped, false if it wasn't being monitored.
     */
    fun stopMonitoring(workflowId: String, runId: String): Boolean {
        val key = getMonitoringKey(workflowId, runId)
        val job = monitoringJobs.remove(key) ?: return false
        
        job.cancel()
        return true
    }
    
    /**
     * Checks if a workflow execution is being monitored.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @return True if the execution is being monitored, false otherwise.
     */
    fun isMonitoring(workflowId: String, runId: String): Boolean {
        val key = getMonitoringKey(workflowId, runId)
        return monitoringJobs.containsKey(key)
    }
    
    /**
     * Gets the current status of a workflow execution.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @return The execution metrics, or null if not found.
     */
    fun getExecutionStatus(workflowId: String, runId: String): ExecutionMetrics? {
        return metricsCollector.getExecutionMetrics(workflowId, runId)
    }
    
    /**
     * Gets the execution history for a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @param limit The maximum number of records to return.
     * @param offset The offset to start from.
     * @return A list of execution records.
     */
    fun getExecutionHistory(workflowId: String, limit: Int = 100, offset: Int = 0): List<ExecutionRecord> {
        return executionHistory.getExecutionRecordsForWorkflow(workflowId, limit, offset)
    }
    
    /**
     * Gets the execution record for a workflow execution.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @return The execution record, or null if not found.
     */
    fun getExecutionRecord(workflowId: String, runId: String): ExecutionRecord? {
        return executionHistory.getExecutionRecord(workflowId, runId)
    }
    
    /**
     * Gets alerts for a workflow execution.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @return A list of alerts.
     */
    fun getAlertsForExecution(workflowId: String, runId: String): List<Alert> {
        return alertManager.getAlertsForExecution(workflowId, runId)
    }
    
    /**
     * Gets all active monitoring jobs.
     *
     * @return A map of monitoring keys to jobs.
     */
    fun getActiveMonitoringJobs(): Map<String, Job> {
        return monitoringJobs.toMap()
    }
    
    /**
     * Stops all monitoring jobs.
     */
    fun stopAllMonitoring() {
        monitoringJobs.forEach { (_, job) -> job.cancel() }
        monitoringJobs.clear()
    }
    
    /**
     * Registers an alert condition.
     *
     * @param condition The condition to register.
     * @param severity The severity of alerts triggered by this condition.
     * @param handler The handler for alerts triggered by this condition, or null to use the default handler.
     * @return The ID of the registered condition.
     */
    fun registerAlertCondition(
        condition: AlertCondition,
        severity: AlertSeverity,
        handler: AlertHandler? = null
    ): String {
        return alertManager.registerCondition(condition, severity, handler)
    }
    
    /**
     * Unregisters an alert condition.
     *
     * @param conditionId The ID of the condition to unregister.
     * @return True if the condition was unregistered, false otherwise.
     */
    fun unregisterAlertCondition(conditionId: String): Boolean {
        return alertManager.unregisterCondition(conditionId)
    }
    
    private fun getMonitoringKey(workflowId: String, runId: String): String {
        return "$workflowId:$runId"
    }
}
