package ai.kastrax.core.workflow.monitoring

import ai.kastrax.core.workflow.history.ExecutionRecord
import java.time.Duration

/**
 * Represents a condition that triggers an alert.
 */
interface AlertCondition {
    /**
     * Evaluates whether the condition is met.
     *
     * @param metrics The execution metrics to evaluate.
     * @return True if the condition is met, false otherwise.
     */
    fun evaluate(metrics: ExecutionMetrics): Boolean
    
    /**
     * Evaluates whether the condition is met.
     *
     * @param record The execution record to evaluate.
     * @return True if the condition is met, false otherwise.
     */
    fun evaluate(record: ExecutionRecord): Boolean
    
    /**
     * Gets a description of this condition.
     *
     * @return A description of the condition.
     */
    fun getDescription(): String
}

/**
 * A condition that triggers an alert when a workflow execution fails.
 */
class WorkflowFailureCondition : AlertCondition {
    override fun evaluate(metrics: ExecutionMetrics): Boolean {
        return metrics.status == ExecutionStatus.FAILED
    }
    
    override fun evaluate(record: ExecutionRecord): Boolean {
        return record.status == ExecutionStatus.FAILED
    }
    
    override fun getDescription(): String {
        return "Workflow execution failed"
    }
}

/**
 * A condition that triggers an alert when a workflow execution takes longer than expected.
 *
 * @property threshold The duration threshold.
 */
class ExecutionDurationCondition(private val threshold: Duration) : AlertCondition {
    override fun evaluate(metrics: ExecutionMetrics): Boolean {
        return metrics.getDuration() > threshold
    }
    
    override fun evaluate(record: ExecutionRecord): Boolean {
        val endTime = record.endTime ?: java.time.Instant.now()
        val duration = Duration.between(record.startTime, endTime)
        return duration > threshold
    }
    
    override fun getDescription(): String {
        return "Workflow execution duration exceeded $threshold"
    }
}

/**
 * A condition that triggers an alert when a step fails.
 *
 * @property stepId The ID of the step to monitor, or null to monitor all steps.
 */
class StepFailureCondition(private val stepId: String? = null) : AlertCondition {
    override fun evaluate(metrics: ExecutionMetrics): Boolean {
        return if (stepId != null) {
            metrics.stepMetrics[stepId]?.status == StepStatus.FAILED
        } else {
            metrics.failedSteps > 0
        }
    }
    
    override fun evaluate(record: ExecutionRecord): Boolean {
        return if (stepId != null) {
            record.stepRecords.any { it.stepId == stepId && it.status == "FAILED" }
        } else {
            record.stepRecords.any { it.status == "FAILED" }
        }
    }
    
    override fun getDescription(): String {
        return if (stepId != null) {
            "Step $stepId failed"
        } else {
            "Any step failed"
        }
    }
}

/**
 * A condition that triggers an alert when a step takes longer than expected.
 *
 * @property stepId The ID of the step to monitor, or null to monitor all steps.
 * @property threshold The duration threshold.
 */
class StepDurationCondition(private val stepId: String? = null, private val threshold: Duration) : AlertCondition {
    override fun evaluate(metrics: ExecutionMetrics): Boolean {
        return if (stepId != null) {
            val stepMetrics = metrics.stepMetrics[stepId] ?: return false
            stepMetrics.getDuration() > threshold
        } else {
            metrics.stepMetrics.values.any { it.getDuration() > threshold }
        }
    }
    
    override fun evaluate(record: ExecutionRecord): Boolean {
        return if (stepId != null) {
            val stepRecord = record.stepRecords.find { it.stepId == stepId } ?: return false
            val endTime = stepRecord.endTime ?: java.time.Instant.now()
            val duration = Duration.between(stepRecord.startTime, endTime)
            duration > threshold
        } else {
            record.stepRecords.any {
                val endTime = it.endTime ?: java.time.Instant.now()
                val duration = Duration.between(it.startTime, endTime)
                duration > threshold
            }
        }
    }
    
    override fun getDescription(): String {
        return if (stepId != null) {
            "Step $stepId duration exceeded $threshold"
        } else {
            "Any step duration exceeded $threshold"
        }
    }
}

/**
 * A condition that triggers an alert when a step is retried more than a specified number of times.
 *
 * @property stepId The ID of the step to monitor, or null to monitor all steps.
 * @property threshold The retry count threshold.
 */
class StepRetryCountCondition(private val stepId: String? = null, private val threshold: Int) : AlertCondition {
    override fun evaluate(metrics: ExecutionMetrics): Boolean {
        return if (stepId != null) {
            val stepMetrics = metrics.stepMetrics[stepId] ?: return false
            stepMetrics.retryCount > threshold
        } else {
            metrics.stepMetrics.values.any { it.retryCount > threshold }
        }
    }
    
    override fun evaluate(record: ExecutionRecord): Boolean {
        return if (stepId != null) {
            val stepRecord = record.stepRecords.find { it.stepId == stepId } ?: return false
            stepRecord.retryCount > threshold
        } else {
            record.stepRecords.any { it.retryCount > threshold }
        }
    }
    
    override fun getDescription(): String {
        return if (stepId != null) {
            "Step $stepId retry count exceeded $threshold"
        } else {
            "Any step retry count exceeded $threshold"
        }
    }
}

/**
 * A composite condition that combines multiple conditions with a logical AND.
 *
 * @property conditions The conditions to combine.
 */
class AndCondition(private val conditions: List<AlertCondition>) : AlertCondition {
    override fun evaluate(metrics: ExecutionMetrics): Boolean {
        return conditions.all { it.evaluate(metrics) }
    }
    
    override fun evaluate(record: ExecutionRecord): Boolean {
        return conditions.all { it.evaluate(record) }
    }
    
    override fun getDescription(): String {
        return conditions.joinToString(" AND ") { it.getDescription() }
    }
}

/**
 * A composite condition that combines multiple conditions with a logical OR.
 *
 * @property conditions The conditions to combine.
 */
class OrCondition(private val conditions: List<AlertCondition>) : AlertCondition {
    override fun evaluate(metrics: ExecutionMetrics): Boolean {
        return conditions.any { it.evaluate(metrics) }
    }
    
    override fun evaluate(record: ExecutionRecord): Boolean {
        return conditions.any { it.evaluate(record) }
    }
    
    override fun getDescription(): String {
        return conditions.joinToString(" OR ") { it.getDescription() }
    }
}
