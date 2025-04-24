package ai.kastrax.core.workflow.monitoring

import java.time.Duration
import java.time.Instant

/**
 * Represents metrics for a single workflow step execution.
 *
 * @property workflowId The ID of the workflow.
 * @property runId The ID of the workflow run.
 * @property stepId The ID of the step.
 * @property stepName The name of the step.
 * @property stepType The type of the step.
 * @property startTime The time when the step execution started.
 * @property endTime The time when the step execution ended, or null if still running.
 * @property status The current status of the step execution.
 * @property retryCount The number of times this step has been retried.
 * @property errorMessage The error message if the step failed, or null otherwise.
 * @property inputSize The size of the input data in bytes, if available.
 * @property outputSize The size of the output data in bytes, if available.
 * @property memoryUsage The memory usage of the step in bytes, if available.
 * @property cpuTime The CPU time used by the step in milliseconds, if available.
 * @property customMetrics A map of custom metrics specific to this step.
 */
data class StepMetrics(
    val workflowId: String,
    val runId: String,
    val stepId: String,
    val stepName: String,
    val stepType: String,
    val startTime: Instant,
    var endTime: Instant? = null,
    var status: StepStatus = StepStatus.RUNNING,
    var retryCount: Int = 0,
    var errorMessage: String? = null,
    var inputSize: Long? = null,
    var outputSize: Long? = null,
    var memoryUsage: Long? = null,
    var cpuTime: Long? = null,
    val customMetrics: MutableMap<String, Any> = mutableMapOf()
) {
    /**
     * Calculates the duration of the step execution.
     * If the step is still running, calculates the duration from start time to now.
     *
     * @return The duration of the step execution.
     */
    fun getDuration(): Duration {
        val end = endTime ?: Instant.now()
        return Duration.between(startTime, end)
    }

    /**
     * Completes this step metrics with the given status.
     *
     * @param status The final status of the step.
     * @param endTime The end time of the step, defaults to now if not provided.
     * @param errorMessage The error message if the step failed, or null otherwise.
     */
    fun complete(status: StepStatus, endTime: Instant = Instant.now(), errorMessage: String? = null) {
        this.status = status
        this.endTime = endTime
        this.errorMessage = errorMessage
    }

    /**
     * Increments the retry count for this step.
     */
    fun incrementRetryCount() {
        retryCount++
    }

    /**
     * Adds a custom metric to this step.
     *
     * @param key The key of the custom metric.
     * @param value The value of the custom metric.
     */
    fun addCustomMetric(key: String, value: Any) {
        customMetrics[key] = value
    }
}

/**
 * Represents the status of a workflow step execution.
 */
enum class StepStatus {
    RUNNING,
    COMPLETED,
    FAILED,
    SKIPPED,
    SUSPENDED,
    WAITING
}
