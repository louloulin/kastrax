package ai.kastrax.core.workflow.monitoring

import java.time.Instant

/**
 * Interface for storing and retrieving workflow execution metrics.
 */
interface MetricsStorage {
    /**
     * Saves execution metrics.
     *
     * @param metrics The execution metrics to save.
     */
    fun saveExecutionMetrics(metrics: ExecutionMetrics)

    /**
     * Saves step metrics.
     *
     * @param metrics The step metrics to save.
     */
    fun saveStepMetrics(metrics: StepMetrics)

    /**
     * Gets execution metrics by workflow ID and run ID.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @return The execution metrics, or null if not found.
     */
    fun getExecutionMetrics(workflowId: String, runId: String): ExecutionMetrics?

    /**
     * Gets all execution metrics for a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @param limit The maximum number of metrics to return.
     * @param offset The offset to start from.
     * @return A list of execution metrics.
     */
    fun getExecutionMetricsForWorkflow(workflowId: String, limit: Int = 100, offset: Int = 0): List<ExecutionMetrics>

    /**
     * Gets all step metrics for a workflow execution.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @return A list of step metrics.
     */
    fun getStepMetricsForExecution(workflowId: String, runId: String): List<StepMetrics>

    /**
     * Gets all step metrics for a specific step across all executions of a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @param stepId The ID of the step.
     * @param limit The maximum number of metrics to return.
     * @param offset The offset to start from.
     * @return A list of step metrics.
     */
    fun getStepMetricsForStep(workflowId: String, stepId: String, limit: Int = 100, offset: Int = 0): List<StepMetrics>

    /**
     * Gets all execution metrics within a time range.
     *
     * @param startTime The start of the time range.
     * @param endTime The end of the time range.
     * @param limit The maximum number of metrics to return.
     * @param offset The offset to start from.
     * @return A list of execution metrics.
     */
    fun getExecutionMetricsInTimeRange(startTime: Instant, endTime: Instant, limit: Int = 100, offset: Int = 0): List<ExecutionMetrics>

    /**
     * Gets all execution metrics with a specific status.
     *
     * @param status The status to filter by.
     * @param limit The maximum number of metrics to return.
     * @param offset The offset to start from.
     * @return A list of execution metrics.
     */
    fun getExecutionMetricsByStatus(status: ExecutionStatus, limit: Int = 100, offset: Int = 0): List<ExecutionMetrics>

    /**
     * Gets all execution metrics with specific tags.
     *
     * @param tags The tags to filter by.
     * @param limit The maximum number of metrics to return.
     * @param offset The offset to start from.
     * @return A list of execution metrics.
     */
    fun getExecutionMetricsByTags(tags: Map<String, String>, limit: Int = 100, offset: Int = 0): List<ExecutionMetrics>

    /**
     * Deletes execution metrics by workflow ID and run ID.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @return True if the metrics were deleted, false otherwise.
     */
    fun deleteExecutionMetrics(workflowId: String, runId: String): Boolean

    /**
     * Deletes all execution metrics for a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @return The number of metrics deleted.
     */
    fun deleteExecutionMetricsForWorkflow(workflowId: String): Int

    /**
     * Deletes all execution metrics older than a specific time.
     *
     * @param time The time threshold.
     * @return The number of metrics deleted.
     */
    fun deleteExecutionMetricsOlderThan(time: Instant): Int
}
