package ai.kastrax.core.workflow.history

import ai.kastrax.core.workflow.monitoring.ExecutionStatus
import ai.kastrax.core.workflow.monitoring.StepStatus
import java.time.Instant
import java.util.UUID

/**
 * Manages workflow execution history.
 *
 * @property historyStorage The storage for execution history.
 */
class WorkflowExecutionHistory(
    private val historyStorage: HistoryStorage
) {
    /**
     * Records the start of a workflow execution.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run, or null to generate a new one.
     * @param input The input data for the workflow, if available.
     * @param metadata Additional metadata about the execution.
     * @return The execution record.
     */
    fun recordExecutionStart(
        workflowId: String,
        runId: String = UUID.randomUUID().toString(),
        input: Map<String, Any?>? = null,
        metadata: Map<String, Any> = emptyMap()
    ): ExecutionRecord {
        val record = ExecutionRecord(
            workflowId = workflowId,
            runId = runId,
            startTime = Instant.now(),
            status = ExecutionStatus.RUNNING,
            input = input,
            metadata = metadata
        )
        
        historyStorage.saveExecutionRecord(record)
        return record
    }

    /**
     * Records the completion of a workflow execution.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @param status The final status of the execution.
     * @param output The output data from the workflow, if available.
     * @param error The error that occurred during execution, if any.
     * @return The updated execution record, or null if the execution was not found.
     */
    fun recordExecutionCompletion(
        workflowId: String,
        runId: String,
        status: ExecutionStatus,
        output: Map<String, Any?>? = null,
        error: String? = null
    ): ExecutionRecord? {
        val record = historyStorage.getExecutionRecord(workflowId, runId) ?: return null
        
        val updatedRecord = record.copy(
            endTime = Instant.now(),
            status = status,
            output = output,
            error = error
        )
        
        historyStorage.saveExecutionRecord(updatedRecord)
        return updatedRecord
    }

    /**
     * Records a step execution.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @param stepId The ID of the step.
     * @param stepName The name of the step.
     * @param stepType The type of the step.
     * @param status The status of the step execution.
     * @param startTime The time when the step execution started.
     * @param endTime The time when the step execution ended, or null if still running.
     * @param input The input data for the step, if available.
     * @param output The output data from the step, if available.
     * @param error The error that occurred during execution, if any.
     * @param retryCount The number of times this step was retried.
     * @param metadata Additional metadata about the step execution.
     * @return The updated execution record, or null if the execution was not found.
     */
    fun recordStepExecution(
        workflowId: String,
        runId: String,
        stepId: String,
        stepName: String,
        stepType: String,
        status: StepStatus,
        startTime: Instant,
        endTime: Instant? = null,
        input: Map<String, Any?>? = null,
        output: Map<String, Any?>? = null,
        error: String? = null,
        retryCount: Int = 0,
        metadata: Map<String, Any> = emptyMap()
    ): ExecutionRecord? {
        val record = historyStorage.getExecutionRecord(workflowId, runId) ?: return null
        
        val stepRecord = StepRecord(
            stepId = stepId,
            stepName = stepName,
            stepType = stepType,
            startTime = startTime,
            endTime = endTime,
            status = status.toString(),
            input = input,
            output = output,
            error = error,
            retryCount = retryCount,
            metadata = metadata
        )
        
        // Remove any existing step record with the same ID
        val existingStepRecords = record.stepRecords.filter { it.stepId != stepId }
        
        val updatedRecord = record.copy(
            stepRecords = existingStepRecords + stepRecord
        )
        
        historyStorage.saveExecutionRecord(updatedRecord)
        return updatedRecord
    }

    /**
     * Gets an execution record by workflow ID and run ID.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @return The execution record, or null if not found.
     */
    fun getExecutionRecord(workflowId: String, runId: String): ExecutionRecord? {
        return historyStorage.getExecutionRecord(workflowId, runId)
    }

    /**
     * Gets all execution records for a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @param limit The maximum number of records to return.
     * @param offset The offset to start from.
     * @return A list of execution records.
     */
    fun getExecutionRecordsForWorkflow(workflowId: String, limit: Int = 100, offset: Int = 0): List<ExecutionRecord> {
        return historyStorage.getExecutionRecordsForWorkflow(workflowId, limit, offset)
    }

    /**
     * Gets all execution records within a time range.
     *
     * @param startTime The start of the time range.
     * @param endTime The end of the time range.
     * @param limit The maximum number of records to return.
     * @param offset The offset to start from.
     * @return A list of execution records.
     */
    fun getExecutionRecordsInTimeRange(startTime: Instant, endTime: Instant, limit: Int = 100, offset: Int = 0): List<ExecutionRecord> {
        return historyStorage.getExecutionRecordsInTimeRange(startTime, endTime, limit, offset)
    }

    /**
     * Gets all execution records with a specific status.
     *
     * @param status The status to filter by.
     * @param limit The maximum number of records to return.
     * @param offset The offset to start from.
     * @return A list of execution records.
     */
    fun getExecutionRecordsByStatus(status: ExecutionStatus, limit: Int = 100, offset: Int = 0): List<ExecutionRecord> {
        return historyStorage.getExecutionRecordsByStatus(status.toString(), limit, offset)
    }

    /**
     * Searches for execution records matching a query.
     *
     * @param query The search query.
     * @param limit The maximum number of records to return.
     * @param offset The offset to start from.
     * @return A list of execution records.
     */
    fun searchExecutionRecords(query: String, limit: Int = 100, offset: Int = 0): List<ExecutionRecord> {
        return historyStorage.searchExecutionRecords(query, limit, offset)
    }

    /**
     * Deletes an execution record by workflow ID and run ID.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @return True if the record was deleted, false otherwise.
     */
    fun deleteExecutionRecord(workflowId: String, runId: String): Boolean {
        return historyStorage.deleteExecutionRecord(workflowId, runId)
    }

    /**
     * Deletes all execution records for a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @return The number of records deleted.
     */
    fun deleteExecutionRecordsForWorkflow(workflowId: String): Int {
        return historyStorage.deleteExecutionRecordsForWorkflow(workflowId)
    }

    /**
     * Deletes all execution records older than a specific time.
     *
     * @param time The time threshold.
     * @return The number of records deleted.
     */
    fun deleteExecutionRecordsOlderThan(time: Instant): Int {
        return historyStorage.deleteExecutionRecordsOlderThan(time)
    }
}
