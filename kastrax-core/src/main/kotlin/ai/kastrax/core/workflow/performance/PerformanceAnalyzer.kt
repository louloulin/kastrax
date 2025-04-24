package ai.kastrax.core.workflow.performance

import ai.kastrax.core.workflow.monitoring.ExecutionMetrics
import ai.kastrax.core.workflow.monitoring.StepMetrics
import java.time.Duration

/**
 * Analyzes workflow performance and generates reports and recommendations.
 *
 * @property bottleneckDetector The bottleneck detector to use.
 */
class PerformanceAnalyzer(
    private val bottleneckDetector: BottleneckDetector = BottleneckDetector()
) {
    /**
     * Analyzes a workflow execution and generates a performance report.
     *
     * @param metrics The execution metrics to analyze.
     * @param thresholds The thresholds to use for bottleneck detection.
     * @return A performance report.
     */
    fun analyzeExecution(
        metrics: ExecutionMetrics,
        thresholds: BottleneckThresholds = BottleneckThresholds()
    ): PerformanceReport {
        // Detect bottlenecks
        val bottlenecks = bottleneckDetector.detectBottlenecks(metrics, thresholds)
        
        // Generate step performance reports
        val stepReports = metrics.stepMetrics.values.map { stepMetrics ->
            StepPerformanceReport(
                stepId = stepMetrics.stepId,
                stepName = stepMetrics.stepName,
                stepType = stepMetrics.stepType,
                startTime = stepMetrics.startTime,
                endTime = stepMetrics.endTime,
                duration = stepMetrics.getDuration(),
                memoryUsage = stepMetrics.memoryUsage,
                cpuTime = stepMetrics.cpuTime,
                metrics = stepMetrics.customMetrics
            )
        }
        
        // Generate recommendations based on bottlenecks
        val recommendations = generateRecommendations(bottlenecks, metrics)
        
        // Calculate additional performance metrics
        val performanceMetrics = calculatePerformanceMetrics(metrics)
        
        return PerformanceReport(
            workflowId = metrics.workflowId,
            runId = metrics.runId,
            startTime = metrics.startTime,
            endTime = metrics.endTime,
            totalDuration = metrics.getDuration(),
            stepReports = stepReports,
            bottlenecks = bottlenecks,
            recommendations = recommendations,
            metrics = performanceMetrics
        )
    }
    
    /**
     * Compares the performance of two workflow executions.
     *
     * @param baselineMetrics The baseline execution metrics.
     * @param currentMetrics The current execution metrics.
     * @return A comparison report.
     */
    fun compareExecutions(
        baselineMetrics: ExecutionMetrics,
        currentMetrics: ExecutionMetrics
    ): PerformanceComparisonReport {
        // Generate performance reports for both executions
        val baselineReport = analyzeExecution(baselineMetrics)
        val currentReport = analyzeExecution(currentMetrics)
        
        // Calculate duration differences
        val totalDurationDiff = calculateDurationDifference(
            baselineReport.totalDuration,
            currentReport.totalDuration
        )
        
        // Compare step durations
        val stepDurationDiffs = mutableMapOf<String, DurationDifference>()
        val baselineStepMap = baselineReport.stepReports.associateBy { it.stepId }
        val currentStepMap = currentReport.stepReports.associateBy { it.stepId }
        
        // Find steps in both executions
        val commonStepIds = baselineStepMap.keys.intersect(currentStepMap.keys)
        commonStepIds.forEach { stepId ->
            val baselineStep = baselineStepMap[stepId]!!
            val currentStep = currentStepMap[stepId]!!
            
            stepDurationDiffs[stepId] = calculateDurationDifference(
                baselineStep.duration,
                currentStep.duration
            )
        }
        
        // Find steps only in baseline
        val baselineOnlySteps = baselineStepMap.keys.minus(currentStepMap.keys)
        
        // Find steps only in current
        val currentOnlySteps = currentStepMap.keys.minus(baselineStepMap.keys)
        
        // Generate improvement and regression recommendations
        val recommendations = generateComparisonRecommendations(
            baselineReport,
            currentReport,
            stepDurationDiffs
        )
        
        return PerformanceComparisonReport(
            baselineWorkflowId = baselineMetrics.workflowId,
            baselineRunId = baselineMetrics.runId,
            currentWorkflowId = currentMetrics.workflowId,
            currentRunId = currentMetrics.runId,
            totalDurationDiff = totalDurationDiff,
            stepDurationDiffs = stepDurationDiffs,
            baselineOnlySteps = baselineOnlySteps.toList(),
            currentOnlySteps = currentOnlySteps.toList(),
            recommendations = recommendations
        )
    }
    
    /**
     * Generates recommendations for improving workflow performance.
     *
     * @param bottlenecks The detected bottlenecks.
     * @param metrics The execution metrics.
     * @return A list of recommendations.
     */
    private fun generateRecommendations(
        bottlenecks: List<Bottleneck>,
        metrics: ExecutionMetrics
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        
        // Group bottlenecks by step ID
        val bottlenecksByStep = bottlenecks.groupBy { it.stepId }
        
        // Generate recommendations for each step with bottlenecks
        bottlenecksByStep.forEach { (stepId, stepBottlenecks) ->
            if (stepId != null) {
                val stepMetrics = metrics.stepMetrics[stepId]
                if (stepMetrics != null) {
                    // Check for long-running steps
                    val durationBottlenecks = stepBottlenecks.filter { it.description.contains("taking longer") }
                    if (durationBottlenecks.isNotEmpty()) {
                        recommendations.add(
                            Recommendation(
                                description = "Optimize step '${stepMetrics.stepName}' to reduce execution time",
                                priority = getHighestPriority(durationBottlenecks),
                                relatedBottlenecks = durationBottlenecks
                            )
                        )
                    }
                    
                    // Check for high memory usage steps
                    val memoryBottlenecks = stepBottlenecks.filter { it.description.contains("memory") }
                    if (memoryBottlenecks.isNotEmpty()) {
                        recommendations.add(
                            Recommendation(
                                description = "Reduce memory usage in step '${stepMetrics.stepName}'",
                                priority = getHighestPriority(memoryBottlenecks),
                                relatedBottlenecks = memoryBottlenecks
                            )
                        )
                    }
                    
                    // Check for high CPU usage steps
                    val cpuBottlenecks = stepBottlenecks.filter { it.description.contains("CPU") }
                    if (cpuBottlenecks.isNotEmpty()) {
                        recommendations.add(
                            Recommendation(
                                description = "Optimize CPU usage in step '${stepMetrics.stepName}'",
                                priority = getHighestPriority(cpuBottlenecks),
                                relatedBottlenecks = cpuBottlenecks
                            )
                        )
                    }
                    
                    // Check for steps with high retry counts
                    val retryBottlenecks = stepBottlenecks.filter { it.description.contains("retried") }
                    if (retryBottlenecks.isNotEmpty()) {
                        recommendations.add(
                            Recommendation(
                                description = "Improve reliability of step '${stepMetrics.stepName}' to reduce retries",
                                priority = getHighestPriority(retryBottlenecks),
                                relatedBottlenecks = retryBottlenecks
                            )
                        )
                    }
                }
            }
        }
        
        // Check for workflow-level bottlenecks
        val workflowBottlenecks = bottlenecksByStep[null] ?: emptyList()
        if (workflowBottlenecks.isNotEmpty()) {
            // Check for long-running workflows
            val durationBottlenecks = workflowBottlenecks.filter { it.description.contains("taking longer") }
            if (durationBottlenecks.isNotEmpty()) {
                recommendations.add(
                    Recommendation(
                        description = "Optimize overall workflow execution time",
                        priority = getHighestPriority(durationBottlenecks),
                        relatedBottlenecks = durationBottlenecks
                    )
                )
            }
        }
        
        // Add general recommendations
        if (metrics.stepMetrics.size > 1) {
            recommendations.add(
                Recommendation(
                    description = "Consider parallelizing steps where possible to reduce overall execution time",
                    priority = RecommendationPriority.MEDIUM
                )
            )
        }
        
        return recommendations
    }
    
    /**
     * Generates recommendations based on a comparison of two workflow executions.
     *
     * @param baselineReport The baseline performance report.
     * @param currentReport The current performance report.
     * @param stepDurationDiffs The step duration differences.
     * @return A list of recommendations.
     */
    private fun generateComparisonRecommendations(
        baselineReport: PerformanceReport,
        currentReport: PerformanceReport,
        stepDurationDiffs: Map<String, DurationDifference>
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        
        // Check for overall performance improvement or regression
        val totalDurationDiff = calculateDurationDifference(
            baselineReport.totalDuration,
            currentReport.totalDuration
        )
        
        if (totalDurationDiff.percentChange < -10) {
            // Performance improvement
            recommendations.add(
                Recommendation(
                    description = "Overall workflow performance has improved by ${-totalDurationDiff.percentChange.toInt()}%",
                    priority = RecommendationPriority.LOW
                )
            )
        } else if (totalDurationDiff.percentChange > 10) {
            // Performance regression
            recommendations.add(
                Recommendation(
                    description = "Overall workflow performance has regressed by ${totalDurationDiff.percentChange.toInt()}%",
                    priority = RecommendationPriority.HIGH
                )
            )
        }
        
        // Check for significant step performance changes
        stepDurationDiffs.forEach { (stepId, diff) ->
            val baselineStep = baselineReport.stepReports.find { it.stepId == stepId }
            val currentStep = currentReport.stepReports.find { it.stepId == stepId }
            
            if (baselineStep != null && currentStep != null) {
                if (diff.percentChange < -20) {
                    // Significant improvement
                    recommendations.add(
                        Recommendation(
                            description = "Step '${currentStep.stepName}' performance has improved by ${-diff.percentChange.toInt()}%",
                            priority = RecommendationPriority.LOW
                        )
                    )
                } else if (diff.percentChange > 20) {
                    // Significant regression
                    recommendations.add(
                        Recommendation(
                            description = "Step '${currentStep.stepName}' performance has regressed by ${diff.percentChange.toInt()}%",
                            priority = RecommendationPriority.HIGH
                        )
                    )
                }
            }
        }
        
        return recommendations
    }
    
    /**
     * Calculates additional performance metrics for a workflow execution.
     *
     * @param metrics The execution metrics.
     * @return A map of performance metrics.
     */
    private fun calculatePerformanceMetrics(metrics: ExecutionMetrics): Map<String, Any> {
        val performanceMetrics = mutableMapOf<String, Any>()
        
        // Calculate average step duration
        val completedSteps = metrics.stepMetrics.values.filter { it.endTime != null }
        if (completedSteps.isNotEmpty()) {
            val totalDuration = completedSteps.sumOf { it.getDuration().toMillis() }
            val averageStepDuration = Duration.ofMillis(totalDuration / completedSteps.size)
            performanceMetrics["averageStepDuration"] = averageStepDuration.toMillis()
        }
        
        // Calculate total memory usage
        val totalMemoryUsage = metrics.stepMetrics.values.sumOf { it.memoryUsage ?: 0 }
        performanceMetrics["totalMemoryUsage"] = totalMemoryUsage
        
        // Calculate total CPU time
        val totalCpuTime = metrics.stepMetrics.values.sumOf { it.cpuTime ?: 0 }
        performanceMetrics["totalCpuTime"] = totalCpuTime
        
        // Calculate total retry count
        val totalRetryCount = metrics.stepMetrics.values.sumOf { it.retryCount }
        performanceMetrics["totalRetryCount"] = totalRetryCount
        
        // Calculate step completion rate
        val completionRate = if (metrics.totalSteps > 0) {
            metrics.completedSteps.toDouble() / metrics.totalSteps * 100
        } else {
            0.0
        }
        performanceMetrics["stepCompletionRate"] = completionRate
        
        // Calculate step failure rate
        val failureRate = if (metrics.totalSteps > 0) {
            metrics.failedSteps.toDouble() / metrics.totalSteps * 100
        } else {
            0.0
        }
        performanceMetrics["stepFailureRate"] = failureRate
        
        return performanceMetrics
    }
    
    /**
     * Calculates the difference between two durations.
     *
     * @param baseline The baseline duration.
     * @param current The current duration.
     * @return The duration difference.
     */
    private fun calculateDurationDifference(
        baseline: Duration,
        current: Duration
    ): DurationDifference {
        val absoluteDiff = current.minus(baseline)
        val percentChange = if (baseline.toMillis() > 0) {
            absoluteDiff.toMillis().toDouble() / baseline.toMillis() * 100
        } else {
            0.0
        }
        
        return DurationDifference(absoluteDiff, percentChange)
    }
    
    /**
     * Gets the highest priority based on bottleneck severities.
     *
     * @param bottlenecks The bottlenecks to check.
     * @return The highest priority.
     */
    private fun getHighestPriority(bottlenecks: List<Bottleneck>): RecommendationPriority {
        val highestSeverity = bottlenecks.maxOfOrNull { it.severity } ?: return RecommendationPriority.LOW
        
        return when (highestSeverity) {
            BottleneckSeverity.CRITICAL -> RecommendationPriority.CRITICAL
            BottleneckSeverity.HIGH -> RecommendationPriority.HIGH
            BottleneckSeverity.MEDIUM -> RecommendationPriority.MEDIUM
            BottleneckSeverity.LOW -> RecommendationPriority.LOW
        }
    }
}

/**
 * Represents a comparison of two workflow executions.
 *
 * @property baselineWorkflowId The ID of the baseline workflow.
 * @property baselineRunId The ID of the baseline workflow run.
 * @property currentWorkflowId The ID of the current workflow.
 * @property currentRunId The ID of the current workflow run.
 * @property totalDurationDiff The difference in total duration.
 * @property stepDurationDiffs The differences in step durations.
 * @property baselineOnlySteps Steps that are only in the baseline execution.
 * @property currentOnlySteps Steps that are only in the current execution.
 * @property recommendations A list of recommendations based on the comparison.
 */
data class PerformanceComparisonReport(
    val baselineWorkflowId: String,
    val baselineRunId: String,
    val currentWorkflowId: String,
    val currentRunId: String,
    val totalDurationDiff: DurationDifference,
    val stepDurationDiffs: Map<String, DurationDifference>,
    val baselineOnlySteps: List<String>,
    val currentOnlySteps: List<String>,
    val recommendations: List<Recommendation>
) {
    /**
     * Generates a summary of the comparison report.
     *
     * @return A string summary of the comparison report.
     */
    fun generateSummary(): String {
        val sb = StringBuilder()
        sb.appendLine("Performance Comparison Summary")
        sb.appendLine("=============================")
        sb.appendLine("Baseline: $baselineWorkflowId (Run: $baselineRunId)")
        sb.appendLine("Current: $currentWorkflowId (Run: $currentRunId)")
        sb.appendLine()
        
        sb.appendLine("Total Duration Difference:")
        sb.appendLine("  Absolute: ${totalDurationDiff.absoluteDiff.toMillis()} ms")
        sb.appendLine("  Percent: ${totalDurationDiff.percentChange.toInt()}%")
        sb.appendLine()
        
        sb.appendLine("Step Duration Differences:")
        if (stepDurationDiffs.isEmpty()) {
            sb.appendLine("  No common steps to compare")
        } else {
            stepDurationDiffs.entries.sortedByDescending { it.value.percentChange }.forEach { (stepId, diff) ->
                sb.appendLine("  $stepId: ${diff.absoluteDiff.toMillis()} ms (${diff.percentChange.toInt()}%)")
            }
        }
        sb.appendLine()
        
        if (baselineOnlySteps.isNotEmpty()) {
            sb.appendLine("Steps only in baseline:")
            baselineOnlySteps.forEach { sb.appendLine("  - $it") }
            sb.appendLine()
        }
        
        if (currentOnlySteps.isNotEmpty()) {
            sb.appendLine("Steps only in current:")
            currentOnlySteps.forEach { sb.appendLine("  - $it") }
            sb.appendLine()
        }
        
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
 * Represents the difference between two durations.
 *
 * @property absoluteDiff The absolute difference.
 * @property percentChange The percentage change.
 */
data class DurationDifference(
    val absoluteDiff: Duration,
    val percentChange: Double
)
