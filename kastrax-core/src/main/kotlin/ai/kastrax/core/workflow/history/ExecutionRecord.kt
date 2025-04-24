package ai.kastrax.core.workflow.history

import ai.kastrax.core.workflow.monitoring.ExecutionStatus
import java.time.Instant

/**
 * Represents a record of a workflow execution.
 *
 * @property workflowId The ID of the workflow.
 * @property runId The ID of the workflow run.
 * @property startTime The time when the workflow execution started.
 * @property endTime The time when the workflow execution ended, or null if still running.
 * @property status The final status of the workflow execution.
 * @property input The input data for the workflow, if available.
 * @property output The output data from the workflow, if available.
 * @property error The error that occurred during execution, if any.
 * @property stepRecords A list of step execution records.
 * @property metadata Additional metadata about the execution.
 */
data class ExecutionRecord(
    val workflowId: String,
    val runId: String,
    val startTime: Instant,
    val endTime: Instant? = null,
    val status: ExecutionStatus,
    val input: Map<String, Any?>? = null,
    val output: Map<String, Any?>? = null,
    val error: String? = null,
    val stepRecords: List<StepRecord> = emptyList(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Represents a record of a workflow step execution.
 *
 * @property stepId The ID of the step.
 * @property stepName The name of the step.
 * @property stepType The type of the step.
 * @property startTime The time when the step execution started.
 * @property endTime The time when the step execution ended, or null if still running.
 * @property status The final status of the step execution.
 * @property input The input data for the step, if available.
 * @property output The output data from the step, if available.
 * @property error The error that occurred during execution, if any.
 * @property retryCount The number of times this step was retried.
 * @property metadata Additional metadata about the step execution.
 */
data class StepRecord(
    val stepId: String,
    val stepName: String,
    val stepType: String,
    val startTime: Instant,
    val endTime: Instant? = null,
    val status: String,
    val input: Map<String, Any?>? = null,
    val output: Map<String, Any?>? = null,
    val error: String? = null,
    val retryCount: Int = 0,
    val metadata: Map<String, Any> = emptyMap()
)
