package ai.kastrax.core.workflow.monitoring

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of the MetricsStorage interface.
 * This implementation is suitable for testing and development,
 * but not for production use as metrics are lost when the application restarts.
 */
class InMemoryMetricsStorage : MetricsStorage {
    private val executionMetrics = ConcurrentHashMap<String, ExecutionMetrics>()
    private val stepMetrics = ConcurrentHashMap<String, MutableList<StepMetrics>>()

    private fun getExecutionKey(workflowId: String, runId: String): String {
        return "$workflowId:$runId"
    }

    private fun getStepKey(workflowId: String, runId: String): String {
        return "$workflowId:$runId"
    }

    override fun saveExecutionMetrics(metrics: ExecutionMetrics) {
        val key = getExecutionKey(metrics.workflowId, metrics.runId)
        executionMetrics[key] = metrics
    }

    override fun saveStepMetrics(metrics: StepMetrics) {
        val key = getStepKey(metrics.workflowId, metrics.runId)
        val existingMetrics = stepMetrics.computeIfAbsent(key) { mutableListOf() }

        // 检查是否已存在相同stepId的指标，如果存在则替换
        val index = existingMetrics.indexOfFirst { it.stepId == metrics.stepId }
        if (index >= 0) {
            existingMetrics[index] = metrics
        } else {
            existingMetrics.add(metrics)
        }
    }

    override fun getExecutionMetrics(workflowId: String, runId: String): ExecutionMetrics? {
        val key = getExecutionKey(workflowId, runId)
        return executionMetrics[key]
    }

    override fun getExecutionMetricsForWorkflow(workflowId: String, limit: Int, offset: Int): List<ExecutionMetrics> {
        return executionMetrics.values
            .filter { it.workflowId == workflowId }
            .sortedByDescending { it.startTime }
            .drop(offset)
            .take(limit)
    }

    override fun getStepMetricsForExecution(workflowId: String, runId: String): List<StepMetrics> {
        val key = getStepKey(workflowId, runId)
        return stepMetrics[key] ?: emptyList()
    }

    override fun getStepMetricsForStep(workflowId: String, stepId: String, limit: Int, offset: Int): List<StepMetrics> {
        return stepMetrics.values
            .flatten()
            .filter { it.workflowId == workflowId && it.stepId == stepId }
            .sortedByDescending { it.startTime }
            .drop(offset)
            .take(limit)
    }

    override fun getExecutionMetricsInTimeRange(startTime: Instant, endTime: Instant, limit: Int, offset: Int): List<ExecutionMetrics> {
        return executionMetrics.values
            .filter { it.startTime >= startTime && (it.endTime ?: Instant.now()) <= endTime }
            .sortedByDescending { it.startTime }
            .drop(offset)
            .take(limit)
    }

    override fun getExecutionMetricsByStatus(status: ExecutionStatus, limit: Int, offset: Int): List<ExecutionMetrics> {
        return executionMetrics.values
            .filter { it.status == status }
            .sortedByDescending { it.startTime }
            .drop(offset)
            .take(limit)
    }

    override fun getExecutionMetricsByTags(tags: Map<String, String>, limit: Int, offset: Int): List<ExecutionMetrics> {
        return executionMetrics.values
            .filter { metrics -> tags.all { (key, value) -> metrics.tags[key] == value } }
            .sortedByDescending { it.startTime }
            .drop(offset)
            .take(limit)
    }

    override fun deleteExecutionMetrics(workflowId: String, runId: String): Boolean {
        val key = getExecutionKey(workflowId, runId)
        val stepKey = getStepKey(workflowId, runId)

        val removed = executionMetrics.remove(key) != null
        stepMetrics.remove(stepKey)

        return removed
    }

    override fun deleteExecutionMetricsForWorkflow(workflowId: String): Int {
        val keysToRemove = executionMetrics.keys.filter { it.startsWith("$workflowId:") }
        val stepKeysToRemove = stepMetrics.keys.filter { it.startsWith("$workflowId:") }

        keysToRemove.forEach { executionMetrics.remove(it) }
        stepKeysToRemove.forEach { stepMetrics.remove(it) }

        return keysToRemove.size
    }

    override fun deleteExecutionMetricsOlderThan(time: Instant): Int {
        val keysToRemove = executionMetrics.entries
            .filter { it.value.startTime < time }
            .map { it.key }

        val stepKeysToRemove = mutableSetOf<String>()
        keysToRemove.forEach { key ->
            val parts = key.split(":")
            if (parts.size == 2) {
                val workflowId = parts[0]
                val runId = parts[1]
                stepKeysToRemove.add(getStepKey(workflowId, runId))
            }
        }

        keysToRemove.forEach { executionMetrics.remove(it) }
        stepKeysToRemove.forEach { stepMetrics.remove(it) }

        return keysToRemove.size
    }
}
