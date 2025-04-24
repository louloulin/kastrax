package ai.kastrax.core.workflow.history

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of the HistoryStorage interface.
 * This implementation is suitable for testing and development,
 * but not for production use as history records are lost when the application restarts.
 */
class InMemoryHistoryStorage : HistoryStorage {
    private val executionRecords = ConcurrentHashMap<String, ExecutionRecord>()

    private fun getExecutionKey(workflowId: String, runId: String): String {
        return "$workflowId:$runId"
    }

    override fun saveExecutionRecord(record: ExecutionRecord) {
        val key = getExecutionKey(record.workflowId, record.runId)
        executionRecords[key] = record
    }

    override fun getExecutionRecord(workflowId: String, runId: String): ExecutionRecord? {
        val key = getExecutionKey(workflowId, runId)
        return executionRecords[key]
    }

    override fun getExecutionRecordsForWorkflow(workflowId: String, limit: Int, offset: Int): List<ExecutionRecord> {
        return executionRecords.values
            .filter { it.workflowId == workflowId }
            .sortedByDescending { it.startTime }
            .drop(offset)
            .take(limit)
    }

    override fun getExecutionRecordsInTimeRange(startTime: Instant, endTime: Instant, limit: Int, offset: Int): List<ExecutionRecord> {
        return executionRecords.values
            .filter { it.startTime >= startTime && (it.endTime ?: Instant.now()) <= endTime }
            .sortedByDescending { it.startTime }
            .drop(offset)
            .take(limit)
    }

    override fun getExecutionRecordsByStatus(status: String, limit: Int, offset: Int): List<ExecutionRecord> {
        return executionRecords.values
            .filter { it.status.toString() == status }
            .sortedByDescending { it.startTime }
            .drop(offset)
            .take(limit)
    }

    override fun getExecutionRecordsByMetadata(metadata: Map<String, Any>, limit: Int, offset: Int): List<ExecutionRecord> {
        return executionRecords.values
            .filter { record -> metadata.all { (key, value) -> record.metadata[key] == value } }
            .sortedByDescending { it.startTime }
            .drop(offset)
            .take(limit)
    }

    override fun deleteExecutionRecord(workflowId: String, runId: String): Boolean {
        val key = getExecutionKey(workflowId, runId)
        return executionRecords.remove(key) != null
    }

    override fun deleteExecutionRecordsForWorkflow(workflowId: String): Int {
        val keysToRemove = executionRecords.keys.filter { it.startsWith("$workflowId:") }
        keysToRemove.forEach { executionRecords.remove(it) }
        return keysToRemove.size
    }

    override fun deleteExecutionRecordsOlderThan(time: Instant): Int {
        val keysToRemove = executionRecords.entries
            .filter { it.value.startTime < time }
            .map { it.key }
        
        keysToRemove.forEach { executionRecords.remove(it) }
        return keysToRemove.size
    }

    override fun searchExecutionRecords(query: String, limit: Int, offset: Int): List<ExecutionRecord> {
        val lowerQuery = query.lowercase()
        
        return executionRecords.values
            .filter {
                it.workflowId.lowercase().contains(lowerQuery) ||
                it.runId.lowercase().contains(lowerQuery) ||
                it.error?.lowercase()?.contains(lowerQuery) == true ||
                it.metadata.any { (key, value) -> 
                    key.lowercase().contains(lowerQuery) || 
                    value.toString().lowercase().contains(lowerQuery) 
                } ||
                it.stepRecords.any { step ->
                    step.stepName.lowercase().contains(lowerQuery) ||
                    step.stepType.lowercase().contains(lowerQuery) ||
                    step.error?.lowercase()?.contains(lowerQuery) == true
                }
            }
            .sortedByDescending { it.startTime }
            .drop(offset)
            .take(limit)
    }
}
