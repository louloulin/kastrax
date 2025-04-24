package ai.kastrax.core.workflow.history

import java.time.Instant

/**
 * Interface for storing and retrieving workflow execution history.
 */
interface HistoryStorage {
    /**
     * Saves an execution record.
     *
     * @param record The execution record to save.
     */
    fun saveExecutionRecord(record: ExecutionRecord)

    /**
     * Gets an execution record by workflow ID and run ID.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @return The execution record, or null if not found.
     */
    fun getExecutionRecord(workflowId: String, runId: String): ExecutionRecord?

    /**
     * Gets all execution records for a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @param limit The maximum number of records to return.
     * @param offset The offset to start from.
     * @return A list of execution records.
     */
    fun getExecutionRecordsForWorkflow(workflowId: String, limit: Int = 100, offset: Int = 0): List<ExecutionRecord>

    /**
     * Gets all execution records within a time range.
     *
     * @param startTime The start of the time range.
     * @param endTime The end of the time range.
     * @param limit The maximum number of records to return.
     * @param offset The offset to start from.
     * @return A list of execution records.
     */
    fun getExecutionRecordsInTimeRange(startTime: Instant, endTime: Instant, limit: Int = 100, offset: Int = 0): List<ExecutionRecord>

    /**
     * Gets all execution records with a specific status.
     *
     * @param status The status to filter by.
     * @param limit The maximum number of records to return.
     * @param offset The offset to start from.
     * @return A list of execution records.
     */
    fun getExecutionRecordsByStatus(status: String, limit: Int = 100, offset: Int = 0): List<ExecutionRecord>

    /**
     * Gets all execution records with specific metadata.
     *
     * @param metadata The metadata to filter by.
     * @param limit The maximum number of records to return.
     * @param offset The offset to start from.
     * @return A list of execution records.
     */
    fun getExecutionRecordsByMetadata(metadata: Map<String, Any>, limit: Int = 100, offset: Int = 0): List<ExecutionRecord>

    /**
     * Deletes an execution record by workflow ID and run ID.
     *
     * @param workflowId The ID of the workflow.
     * @param runId The ID of the workflow run.
     * @return True if the record was deleted, false otherwise.
     */
    fun deleteExecutionRecord(workflowId: String, runId: String): Boolean

    /**
     * Deletes all execution records for a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @return The number of records deleted.
     */
    fun deleteExecutionRecordsForWorkflow(workflowId: String): Int

    /**
     * Deletes all execution records older than a specific time.
     *
     * @param time The time threshold.
     * @return The number of records deleted.
     */
    fun deleteExecutionRecordsOlderThan(time: Instant): Int

    /**
     * Searches for execution records matching a query.
     *
     * @param query The search query.
     * @param limit The maximum number of records to return.
     * @param offset The offset to start from.
     * @return A list of execution records.
     */
    fun searchExecutionRecords(query: String, limit: Int = 100, offset: Int = 0): List<ExecutionRecord>
}
