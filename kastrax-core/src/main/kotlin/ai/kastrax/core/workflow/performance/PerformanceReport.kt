package ai.kastrax.core.workflow.performance

import ai.kastrax.core.workflow.monitoring.ExecutionMetrics
import ai.kastrax.core.workflow.monitoring.StepMetrics
import java.time.Duration
import java.time.Instant

/**
 * Represents a performance report for a workflow execution.
 *
 * @property workflowId The ID of the workflow.
 * @property runId The ID of the workflow run.
 * @property startTime The time when the workflow execution started.
 * @property endTime The time when the workflow execution ended, or null if still running.
 * @property totalDuration The total duration of the workflow execution.
 * @property stepReports A list of step performance reports.
 * @property bottlenecks A list of identified bottlenecks.
 * @property recommendations A list of performance recommendations.
 * @property metrics Additional performance metrics.
 */
data class PerformanceReport(
    val workflowId: String,
    val runId: String,
    val startTime: Instant,
    val endTime: Instant?,
    val totalDuration: Duration,
    val stepReports: List<StepPerformanceReport>,
    val bottlenecks: List<Bottleneck>,
    val recommendations: List<Recommendation>,
    val metrics: Map<String, Any> = emptyMap()
) {
    /**
     * Gets the average step duration.
     *
     * @return The average step duration, or null if there are no steps.
     */
    fun getAverageStepDuration(): Duration? {
        if (stepReports.isEmpty()) return null
        
        val totalStepDuration = stepReports.sumOf { it.duration.toMillis() }
        return Duration.ofMillis(totalStepDuration / stepReports.size)
    }
    
    /**
     * Gets the step with the longest duration.
     *
     * @return The step performance report for the step with the longest duration, or null if there are no steps.
     */
    fun getLongestStep(): StepPerformanceReport? {
        return stepReports.maxByOrNull { it.duration }
    }
    
    /**
     * Gets the step with the highest resource usage.
     *
     * @return The step performance report for the step with the highest resource usage, or null if there are no steps.
     */
    fun getHighestResourceUsageStep(): StepPerformanceReport? {
        return stepReports.maxByOrNull { it.memoryUsage ?: 0 }
    }
    
    /**
     * Gets the critical path of the workflow execution.
     * The critical path is the sequence of steps that determines the minimum time needed to complete the workflow.
     *
     * @return A list of step IDs representing the critical path.
     */
    fun getCriticalPath(): List<String> {
        // In a real implementation, this would calculate the critical path based on step dependencies
        // For now, we'll just return the steps sorted by start time
        return stepReports.sortedBy { it.startTime }.map { it.stepId }
    }
    
    /**
     * Generates a summary of the performance report.
     *
     * @return A string summary of the performance report.
     */
    fun generateSummary(): String {
        val sb = StringBuilder()
        sb.appendLine("Performance Report Summary")
        sb.appendLine("=========================")
        sb.appendLine("Workflow: $workflowId")
        sb.appendLine("Run: $runId")
        sb.appendLine("Start Time: $startTime")
        endTime?.let { sb.appendLine("End Time: $it") }
        sb.appendLine("Total Duration: ${totalDuration.toMillis()} ms")
        
        getAverageStepDuration()?.let { sb.appendLine("Average Step Duration: ${it.toMillis()} ms") }
        
        sb.appendLine()
        sb.appendLine("Bottlenecks:")
        if (bottlenecks.isEmpty()) {
            sb.appendLine("  No bottlenecks identified")
        } else {
            bottlenecks.forEach { sb.appendLine("  - ${it.description} (${it.severity})") }
        }
        
        sb.appendLine()
        sb.appendLine("Recommendations:")
        if (recommendations.isEmpty()) {
            sb.appendLine("  No recommendations")
        } else {
            recommendations.forEach { sb.appendLine("  - ${it.description} (${it.priority})") }
        }
        
        return sb.toString()
    }
}

/**
 * Represents a performance report for a workflow step.
 *
 * @property stepId The ID of the step.
 * @property stepName The name of the step.
 * @property stepType The type of the step.
 * @property startTime The time when the step execution started.
 * @property endTime The time when the step execution ended, or null if still running.
 * @property duration The duration of the step execution.
 * @property memoryUsage The memory usage of the step in bytes, if available.
 * @property cpuTime The CPU time used by the step in milliseconds, if available.
 * @property metrics Additional performance metrics.
 */
data class StepPerformanceReport(
    val stepId: String,
    val stepName: String,
    val stepType: String,
    val startTime: Instant,
    val endTime: Instant?,
    val duration: Duration,
    val memoryUsage: Long? = null,
    val cpuTime: Long? = null,
    val metrics: Map<String, Any> = emptyMap()
)

/**
 * Represents a bottleneck in a workflow execution.
 *
 * @property stepId The ID of the step that is a bottleneck, or null if the bottleneck is not associated with a specific step.
 * @property description A description of the bottleneck.
 * @property severity The severity of the bottleneck.
 * @property metrics Additional metrics related to the bottleneck.
 */
data class Bottleneck(
    val stepId: String? = null,
    val description: String,
    val severity: BottleneckSeverity,
    val metrics: Map<String, Any> = emptyMap()
)

/**
 * Represents the severity of a bottleneck.
 */
enum class BottleneckSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Represents a performance recommendation.
 *
 * @property description A description of the recommendation.
 * @property priority The priority of the recommendation.
 * @property relatedBottlenecks A list of bottlenecks that this recommendation addresses.
 * @property metrics Additional metrics related to the recommendation.
 */
data class Recommendation(
    val description: String,
    val priority: RecommendationPriority,
    val relatedBottlenecks: List<Bottleneck> = emptyList(),
    val metrics: Map<String, Any> = emptyMap()
)

/**
 * Represents the priority of a recommendation.
 */
enum class RecommendationPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
