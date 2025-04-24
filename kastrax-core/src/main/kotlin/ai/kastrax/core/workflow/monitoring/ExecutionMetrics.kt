package ai.kastrax.core.workflow.monitoring

import java.time.Duration
import java.time.Instant

/**
 * Represents metrics for a single workflow execution.
 *
 * @property workflowId The ID of the workflow.
 * @property runId The ID of the workflow run.
 * @property startTime The time when the workflow execution started.
 * @property endTime The time when the workflow execution ended, or null if still running.
 * @property status The current status of the workflow execution.
 * @property stepMetrics A map of step IDs to their metrics.
 * @property totalSteps The total number of steps in the workflow.
 * @property completedSteps The number of completed steps.
 * @property failedSteps The number of failed steps.
 * @property skippedSteps The number of skipped steps.
 * @property tags Optional tags associated with this execution.
 */
data class ExecutionMetrics(
    val workflowId: String,
    val runId: String,
    val startTime: Instant,
    var endTime: Instant? = null,
    var status: ExecutionStatus = ExecutionStatus.RUNNING,
    val stepMetrics: MutableMap<String, StepMetrics> = mutableMapOf(),
    var totalSteps: Int = 0,
    var completedSteps: Int = 0,
    var failedSteps: Int = 0,
    var skippedSteps: Int = 0,
    val tags: Map<String, String> = emptyMap()
) {
    /**
     * Calculates the duration of the workflow execution.
     * If the workflow is still running, calculates the duration from start time to now.
     *
     * @return The duration of the workflow execution.
     */
    fun getDuration(): Duration {
        val end = endTime ?: Instant.now()
        return Duration.between(startTime, end)
    }

    /**
     * Calculates the progress of the workflow execution as a percentage.
     *
     * @return The progress as a percentage (0-100).
     */
    fun getProgress(): Double {
        if (totalSteps == 0) return 0.0
        return (completedSteps + skippedSteps).toDouble() / totalSteps * 100
    }

    /**
     * Adds a step metric to this execution.
     *
     * @param stepMetric The step metric to add.
     */
    fun addStepMetric(stepMetric: StepMetrics) {
        stepMetrics[stepMetric.stepId] = stepMetric
        
        // Update counters based on step status
        when (stepMetric.status) {
            StepStatus.COMPLETED -> completedSteps++
            StepStatus.FAILED -> failedSteps++
            StepStatus.SKIPPED -> skippedSteps++
            else -> {} // No action for other statuses
        }
    }

    /**
     * Completes this execution metrics with the given status.
     *
     * @param status The final status of the execution.
     * @param endTime The end time of the execution, defaults to now if not provided.
     */
    fun complete(status: ExecutionStatus, endTime: Instant = Instant.now()) {
        this.status = status
        this.endTime = endTime
    }
}

/**
 * Represents the status of a workflow execution.
 */
enum class ExecutionStatus {
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELED,
    SUSPENDED
}
