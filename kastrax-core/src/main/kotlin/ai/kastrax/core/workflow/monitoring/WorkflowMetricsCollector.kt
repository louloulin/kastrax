package ai.kastrax.core.workflow.monitoring

import java.time.Instant
import java.util.UUID

/**
 * Collects and manages metrics for workflow executions.
 *
 * @property metricsStorage The storage for metrics.
 */
class WorkflowMetricsCollector(
    private val metricsStorage: MetricsStorage
) {
    /**
     * Starts tracking a new workflow execution.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run, or null to generate a new one.
     * @param totalSteps The total number of steps in the workflow.
     * @param tags Optional tags associated with this execution.
     * @return The execution metrics for the new execution.
     */
    fun startExecution(
        workflowId: String,
        runId: String = UUID.randomUUID().toString(),
        totalSteps: Int = 0,
        tags: Map<String, String> = emptyMap()
    ): ExecutionMetrics {
        val metrics = ExecutionMetrics(
            workflowId = workflowId,
            runId = runId,
            startTime = Instant.now(),
            totalSteps = totalSteps,
            tags = tags
        )
        
        metricsStorage.saveExecutionMetrics(metrics)
        return metrics
    }

    /**
     * Completes a workflow execution.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @param status The final status of the execution.
     * @return The updated execution metrics, or null if the execution was not found.
     */
    fun completeExecution(
        workflowId: String,
        runId: String,
        status: ExecutionStatus
    ): ExecutionMetrics? {
        val metrics = metricsStorage.getExecutionMetrics(workflowId, runId) ?: return null
        
        metrics.complete(status)
        metricsStorage.saveExecutionMetrics(metrics)
        
        return metrics
    }

    /**
     * Starts tracking a new step execution.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @param stepId The ID of the step.
     * @param stepName The name of the step.
     * @param stepType The type of the step.
     * @return The step metrics for the new step execution.
     */
    fun startStep(
        workflowId: String,
        runId: String,
        stepId: String,
        stepName: String,
        stepType: String
    ): StepMetrics {
        val metrics = StepMetrics(
            workflowId = workflowId,
            runId = runId,
            stepId = stepId,
            stepName = stepName,
            stepType = stepType,
            startTime = Instant.now()
        )
        
        metricsStorage.saveStepMetrics(metrics)
        return metrics
    }

    /**
     * Completes a step execution.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @param stepId The ID of the step.
     * @param status The final status of the step.
     * @param errorMessage The error message if the step failed, or null otherwise.
     * @param outputSize The size of the output data in bytes, if available.
     * @param memoryUsage The memory usage of the step in bytes, if available.
     * @param cpuTime The CPU time used by the step in milliseconds, if available.
     * @param customMetrics A map of custom metrics specific to this step.
     * @return The updated step metrics.
     */
    fun completeStep(
        workflowId: String,
        runId: String,
        stepId: String,
        status: StepStatus,
        errorMessage: String? = null,
        outputSize: Long? = null,
        memoryUsage: Long? = null,
        cpuTime: Long? = null,
        customMetrics: Map<String, Any> = emptyMap()
    ): StepMetrics? {
        val stepMetrics = metricsStorage.getStepMetricsForExecution(workflowId, runId)
            .find { it.stepId == stepId } ?: return null
        
        stepMetrics.complete(status, Instant.now(), errorMessage)
        stepMetrics.outputSize = outputSize
        stepMetrics.memoryUsage = memoryUsage
        stepMetrics.cpuTime = cpuTime
        customMetrics.forEach { (key, value) -> stepMetrics.addCustomMetric(key, value) }
        
        metricsStorage.saveStepMetrics(stepMetrics)
        
        // Update execution metrics
        val executionMetrics = metricsStorage.getExecutionMetrics(workflowId, runId)
        if (executionMetrics != null) {
            executionMetrics.addStepMetric(stepMetrics)
            metricsStorage.saveExecutionMetrics(executionMetrics)
        }
        
        return stepMetrics
    }

    /**
     * Records a step retry.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @param stepId The ID of the step.
     * @return The updated step metrics, or null if the step was not found.
     */
    fun recordStepRetry(
        workflowId: String,
        runId: String,
        stepId: String
    ): StepMetrics? {
        val stepMetrics = metricsStorage.getStepMetricsForExecution(workflowId, runId)
            .find { it.stepId == stepId } ?: return null
        
        stepMetrics.incrementRetryCount()
        metricsStorage.saveStepMetrics(stepMetrics)
        
        return stepMetrics
    }

    /**
     * Gets execution metrics by workflow ID and run ID.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @return The execution metrics, or null if not found.
     */
    fun getExecutionMetrics(workflowId: String, runId: String): ExecutionMetrics? {
        return metricsStorage.getExecutionMetrics(workflowId, runId)
    }

    /**
     * Gets all execution metrics for a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @param limit The maximum number of metrics to return.
     * @param offset The offset to start from.
     * @return A list of execution metrics.
     */
    fun getExecutionMetricsForWorkflow(workflowId: String, limit: Int = 100, offset: Int = 0): List<ExecutionMetrics> {
        return metricsStorage.getExecutionMetricsForWorkflow(workflowId, limit, offset)
    }

    /**
     * Gets all step metrics for a workflow execution.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @return A list of step metrics.
     */
    fun getStepMetricsForExecution(workflowId: String, runId: String): List<StepMetrics> {
        return metricsStorage.getStepMetricsForExecution(workflowId, runId)
    }
}
