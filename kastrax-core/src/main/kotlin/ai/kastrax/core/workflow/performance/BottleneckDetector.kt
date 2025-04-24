package ai.kastrax.core.workflow.performance

import ai.kastrax.core.workflow.monitoring.ExecutionMetrics
import ai.kastrax.core.workflow.monitoring.StepMetrics
import java.time.Duration

/**
 * Detects bottlenecks in workflow executions.
 */
class BottleneckDetector {
    /**
     * Detects bottlenecks in a workflow execution.
     *
     * @param metrics The execution metrics to analyze.
     * @param thresholds The thresholds to use for bottleneck detection.
     * @return A list of detected bottlenecks.
     */
    fun detectBottlenecks(
        metrics: ExecutionMetrics,
        thresholds: BottleneckThresholds = BottleneckThresholds()
    ): List<Bottleneck> {
        val bottlenecks = mutableListOf<Bottleneck>()

        // Check for long-running steps
        val longRunningSteps = detectLongRunningSteps(metrics, thresholds.longStepDuration)
        bottlenecks.addAll(longRunningSteps)

        // Check for high memory usage steps
        val highMemorySteps = detectHighMemoryUsageSteps(metrics, thresholds.highMemoryUsage)
        bottlenecks.addAll(highMemorySteps)

        // Check for high CPU usage steps
        val highCpuSteps = detectHighCpuUsageSteps(metrics, thresholds.highCpuTime)
        bottlenecks.addAll(highCpuSteps)

        // Check for steps with high retry counts
        val highRetrySteps = detectHighRetrySteps(metrics, thresholds.highRetryCount)
        bottlenecks.addAll(highRetrySteps)

        // Check for overall workflow duration
        if (metrics.getDuration() > thresholds.longWorkflowDuration) {
            bottlenecks.add(
                Bottleneck(
                    description = "Workflow execution is taking longer than expected",
                    severity = BottleneckSeverity.MEDIUM,
                    metrics = mapOf(
                        "duration" to metrics.getDuration().toMillis(),
                        "threshold" to thresholds.longWorkflowDuration.toMillis()
                    )
                )
            )
        }

        // Check for sequential execution of steps that could be parallelized
        // This would require knowledge of the workflow structure, which we don't have here

        return bottlenecks
    }

    private fun detectLongRunningSteps(
        metrics: ExecutionMetrics,
        threshold: Duration
    ): List<Bottleneck> {
        val bottlenecks = mutableListOf<Bottleneck>()

        // Calculate average step duration
        val completedSteps = metrics.stepMetrics.values.filter { it.endTime != null }
        if (completedSteps.isEmpty()) return bottlenecks

        val totalDuration = completedSteps.sumOf { it.getDuration().toMillis() }
        val averageDuration = Duration.ofMillis(totalDuration / completedSteps.size)

        // Find steps that take significantly longer than average or exceed the threshold
        val longRunningSteps = metrics.stepMetrics.values.filter {
            it.endTime != null && (it.getDuration() > threshold || it.getDuration() > averageDuration.multipliedBy(3))
        }

        longRunningSteps.forEach { step ->
            val severity = when {
                step.getDuration() > threshold.multipliedBy(3) -> BottleneckSeverity.CRITICAL
                step.getDuration() > threshold.multipliedBy(2) -> BottleneckSeverity.HIGH
                step.getDuration() > threshold -> BottleneckSeverity.MEDIUM
                else -> BottleneckSeverity.LOW
            }

            bottlenecks.add(
                Bottleneck(
                    stepId = step.stepId,
                    description = "Step '${step.stepName}' is taking longer than expected",
                    severity = severity,
                    metrics = mapOf(
                        "duration" to step.getDuration().toMillis(),
                        "averageDuration" to averageDuration.toMillis(),
                        "threshold" to threshold.toMillis()
                    )
                )
            )
        }

        return bottlenecks
    }

    private fun detectHighMemoryUsageSteps(
        metrics: ExecutionMetrics,
        threshold: Long
    ): List<Bottleneck> {
        val bottlenecks = mutableListOf<Bottleneck>()

        // Find steps with high memory usage
        val highMemorySteps = metrics.stepMetrics.values.filter {
            val memUsage = it.memoryUsage
            memUsage != null && memUsage > threshold
        }

        highMemorySteps.forEach { step ->
            val memoryUsage = step.memoryUsage ?: 0L
            val severity = when {
                memoryUsage > threshold * 3 -> BottleneckSeverity.CRITICAL
                memoryUsage > threshold * 2 -> BottleneckSeverity.HIGH
                memoryUsage > threshold -> BottleneckSeverity.MEDIUM
                else -> BottleneckSeverity.LOW
            }

            bottlenecks.add(
                Bottleneck(
                    stepId = step.stepId,
                    description = "Step '${step.stepName}' is using more memory than expected",
                    severity = severity,
                    metrics = mapOf(
                        "memoryUsage" to memoryUsage,
                        "threshold" to threshold
                    )
                )
            )
        }

        return bottlenecks
    }

    private fun detectHighCpuUsageSteps(
        metrics: ExecutionMetrics,
        threshold: Long
    ): List<Bottleneck> {
        val bottlenecks = mutableListOf<Bottleneck>()

        // Find steps with high CPU usage
        val highCpuSteps = metrics.stepMetrics.values.filter {
            val cpuT = it.cpuTime
            cpuT != null && cpuT > threshold
        }

        highCpuSteps.forEach { step ->
            val cpuTime = step.cpuTime ?: 0L
            val severity = when {
                cpuTime > threshold * 3 -> BottleneckSeverity.CRITICAL
                cpuTime > threshold * 2 -> BottleneckSeverity.HIGH
                cpuTime > threshold -> BottleneckSeverity.MEDIUM
                else -> BottleneckSeverity.LOW
            }

            bottlenecks.add(
                Bottleneck(
                    stepId = step.stepId,
                    description = "Step '${step.stepName}' is using more CPU time than expected",
                    severity = severity,
                    metrics = mapOf(
                        "cpuTime" to cpuTime,
                        "threshold" to threshold
                    )
                )
            )
        }

        return bottlenecks
    }

    private fun detectHighRetrySteps(
        metrics: ExecutionMetrics,
        threshold: Int
    ): List<Bottleneck> {
        val bottlenecks = mutableListOf<Bottleneck>()

        // Find steps with high retry counts
        val highRetrySteps = metrics.stepMetrics.values.filter {
            it.retryCount > threshold
        }

        highRetrySteps.forEach { step ->
            val severity = when {
                step.retryCount > threshold * 3 -> BottleneckSeverity.CRITICAL
                step.retryCount > threshold * 2 -> BottleneckSeverity.HIGH
                step.retryCount > threshold -> BottleneckSeverity.MEDIUM
                else -> BottleneckSeverity.LOW
            }

            bottlenecks.add(
                Bottleneck(
                    stepId = step.stepId,
                    description = "Step '${step.stepName}' has been retried more times than expected",
                    severity = severity,
                    metrics = mapOf(
                        "retryCount" to step.retryCount,
                        "threshold" to threshold
                    )
                )
            )
        }

        return bottlenecks
    }
}

/**
 * Thresholds for bottleneck detection.
 *
 * @property longStepDuration The threshold for long step duration.
 * @property longWorkflowDuration The threshold for long workflow duration.
 * @property highMemoryUsage The threshold for high memory usage in bytes.
 * @property highCpuTime The threshold for high CPU time in milliseconds.
 * @property highRetryCount The threshold for high retry count.
 */
data class BottleneckThresholds(
    val longStepDuration: Duration = Duration.ofSeconds(30),
    val longWorkflowDuration: Duration = Duration.ofMinutes(5),
    val highMemoryUsage: Long = 100 * 1024 * 1024, // 100 MB
    val highCpuTime: Long = 10000, // 10 seconds
    val highRetryCount: Int = 3
)
