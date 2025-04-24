package ai.kastrax.core.workflow.monitoring

import ai.kastrax.core.workflow.history.ExecutionRecord
import java.time.Instant

/**
 * Represents an alert for a workflow execution.
 *
 * @property id The ID of the alert.
 * @property workflowId The ID of the workflow.
 * @property runId The ID of the workflow run.
 * @property condition The condition that triggered the alert.
 * @property message The alert message.
 * @property severity The severity of the alert.
 * @property timestamp The time when the alert was generated.
 * @property metadata Additional metadata about the alert.
 */
data class Alert(
    val id: String,
    val workflowId: String,
    val runId: String,
    val condition: AlertCondition,
    val message: String,
    val severity: AlertSeverity,
    val timestamp: Instant = Instant.now(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Represents the severity of an alert.
 */
enum class AlertSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

/**
 * Interface for handling alerts.
 */
interface AlertHandler {
    /**
     * Handles an alert.
     *
     * @param alert The alert to handle.
     * @param metrics The execution metrics that triggered the alert.
     */
    fun handleAlert(alert: Alert, metrics: ExecutionMetrics)
    
    /**
     * Handles an alert.
     *
     * @param alert The alert to handle.
     * @param record The execution record that triggered the alert.
     */
    fun handleAlert(alert: Alert, record: ExecutionRecord)
}

/**
 * A handler that logs alerts.
 */
class LoggingAlertHandler : AlertHandler {
    override fun handleAlert(alert: Alert, metrics: ExecutionMetrics) {
        println("[${alert.timestamp}] [${alert.severity}] ${alert.message} - Workflow: ${alert.workflowId}, Run: ${alert.runId}")
    }
    
    override fun handleAlert(alert: Alert, record: ExecutionRecord) {
        println("[${alert.timestamp}] [${alert.severity}] ${alert.message} - Workflow: ${alert.workflowId}, Run: ${alert.runId}")
    }
}

/**
 * A handler that sends email alerts.
 *
 * @property emailConfig The email configuration.
 */
class EmailAlertHandler(private val emailConfig: EmailConfig) : AlertHandler {
    data class EmailConfig(
        val smtpHost: String,
        val smtpPort: Int,
        val username: String,
        val password: String,
        val fromAddress: String,
        val toAddresses: List<String>
    )
    
    override fun handleAlert(alert: Alert, metrics: ExecutionMetrics) {
        // In a real implementation, this would send an email
        println("Sending email alert: ${alert.message} to ${emailConfig.toAddresses.joinToString(", ")}")
    }
    
    override fun handleAlert(alert: Alert, record: ExecutionRecord) {
        // In a real implementation, this would send an email
        println("Sending email alert: ${alert.message} to ${emailConfig.toAddresses.joinToString(", ")}")
    }
}

/**
 * A handler that sends webhook alerts.
 *
 * @property webhookUrl The URL to send the webhook to.
 * @property headers Additional headers to include in the webhook request.
 */
class WebhookAlertHandler(
    private val webhookUrl: String,
    private val headers: Map<String, String> = emptyMap()
) : AlertHandler {
    override fun handleAlert(alert: Alert, metrics: ExecutionMetrics) {
        // In a real implementation, this would send a webhook
        println("Sending webhook alert to $webhookUrl: ${alert.message}")
    }
    
    override fun handleAlert(alert: Alert, record: ExecutionRecord) {
        // In a real implementation, this would send a webhook
        println("Sending webhook alert to $webhookUrl: ${alert.message}")
    }
}

/**
 * A composite handler that delegates to multiple handlers.
 *
 * @property handlers The handlers to delegate to.
 */
class CompositeAlertHandler(private val handlers: List<AlertHandler>) : AlertHandler {
    override fun handleAlert(alert: Alert, metrics: ExecutionMetrics) {
        handlers.forEach { it.handleAlert(alert, metrics) }
    }
    
    override fun handleAlert(alert: Alert, record: ExecutionRecord) {
        handlers.forEach { it.handleAlert(alert, record) }
    }
}
