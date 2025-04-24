package ai.kastrax.examples.workflow

import ai.kastrax.core.workflow.monitoring.ExecutionMetrics
import ai.kastrax.core.workflow.monitoring.ExecutionStatus
import ai.kastrax.core.workflow.monitoring.StepMetrics
import ai.kastrax.core.workflow.monitoring.StepStatus
import ai.kastrax.core.workflow.performance.BottleneckDetector
import ai.kastrax.core.workflow.performance.BottleneckThresholds
import ai.kastrax.core.workflow.performance.PerformanceAnalyzer
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Example demonstrating workflow performance analysis.
 */
object PerformanceAnalysisExample {
    @JvmStatic
    fun main(args: Array<String>) {
        // Create performance analyzer
        val bottleneckDetector = BottleneckDetector()
        val performanceAnalyzer = PerformanceAnalyzer(bottleneckDetector)
        
        // Create custom thresholds
        val thresholds = BottleneckThresholds(
            longStepDuration = Duration.ofSeconds(5),
            longWorkflowDuration = Duration.ofSeconds(15),
            highMemoryUsage = 50 * 1024 * 1024, // 50 MB
            highCpuTime = 5000, // 5 seconds
            highRetryCount = 2
        )
        
        // Create sample execution metrics for a workflow with performance issues
        val slowWorkflowMetrics = createSlowWorkflowMetrics()
        
        // Create sample execution metrics for an optimized workflow
        val optimizedWorkflowMetrics = createOptimizedWorkflowMetrics()
        
        // Analyze slow workflow
        println("Analyzing slow workflow...")
        val slowWorkflowReport = performanceAnalyzer.analyzeExecution(slowWorkflowMetrics, thresholds)
        println(slowWorkflowReport.generateSummary())
        println()
        
        // Analyze optimized workflow
        println("Analyzing optimized workflow...")
        val optimizedWorkflowReport = performanceAnalyzer.analyzeExecution(optimizedWorkflowMetrics, thresholds)
        println(optimizedWorkflowReport.generateSummary())
        println()
        
        // Compare workflows
        println("Comparing workflows...")
        val comparisonReport = performanceAnalyzer.compareExecutions(
            baselineMetrics = slowWorkflowMetrics,
            currentMetrics = optimizedWorkflowMetrics
        )
        println(comparisonReport.generateSummary())
    }
    
    /**
     * Creates sample execution metrics for a slow workflow.
     */
    private fun createSlowWorkflowMetrics(): ExecutionMetrics {
        val workflowId = "sample-workflow"
        val runId = UUID.randomUUID().toString()
        val now = Instant.now()
        
        // Create step metrics
        val stepMetrics = mutableMapOf<String, StepMetrics>()
        
        // Step 1: Fetch Data (slow)
        stepMetrics["step1"] = StepMetrics(
            workflowId = workflowId,
            runId = runId,
            stepId = "step1",
            stepName = "Fetch Data",
            stepType = "http",
            startTime = now.minusSeconds(30),
            endTime = now.minusSeconds(20),
            status = StepStatus.COMPLETED,
            memoryUsage = 10 * 1024 * 1024,
            cpuTime = 2000,
            retryCount = 1
        )
        
        // Step 2: Process Data (very slow and memory intensive)
        stepMetrics["step2"] = StepMetrics(
            workflowId = workflowId,
            runId = runId,
            stepId = "step2",
            stepName = "Process Data",
            stepType = "transform",
            startTime = now.minusSeconds(20),
            endTime = now.minusSeconds(5),
            status = StepStatus.COMPLETED,
            memoryUsage = 80 * 1024 * 1024,
            cpuTime = 12000,
            retryCount = 0
        )
        
        // Step 3: Store Results (normal)
        stepMetrics["step3"] = StepMetrics(
            workflowId = workflowId,
            runId = runId,
            stepId = "step3",
            stepName = "Store Results",
            stepType = "database",
            startTime = now.minusSeconds(5),
            endTime = now.minusSeconds(2),
            status = StepStatus.COMPLETED,
            memoryUsage = 5 * 1024 * 1024,
            cpuTime = 1000,
            retryCount = 0
        )
        
        return ExecutionMetrics(
            workflowId = workflowId,
            runId = runId,
            startTime = now.minusSeconds(30),
            endTime = now.minusSeconds(2),
            status = ExecutionStatus.COMPLETED,
            stepMetrics = stepMetrics,
            totalSteps = 3,
            completedSteps = 3,
            failedSteps = 0,
            skippedSteps = 0
        )
    }
    
    /**
     * Creates sample execution metrics for an optimized workflow.
     */
    private fun createOptimizedWorkflowMetrics(): ExecutionMetrics {
        val workflowId = "sample-workflow-optimized"
        val runId = UUID.randomUUID().toString()
        val now = Instant.now()
        
        // Create step metrics
        val stepMetrics = mutableMapOf<String, StepMetrics>()
        
        // Step 1: Fetch Data (optimized)
        stepMetrics["step1"] = StepMetrics(
            workflowId = workflowId,
            runId = runId,
            stepId = "step1",
            stepName = "Fetch Data",
            stepType = "http",
            startTime = now.minusSeconds(15),
            endTime = now.minusSeconds(12),
            status = StepStatus.COMPLETED,
            memoryUsage = 8 * 1024 * 1024,
            cpuTime = 1500,
            retryCount = 0
        )
        
        // Step 2: Process Data (optimized)
        stepMetrics["step2"] = StepMetrics(
            workflowId = workflowId,
            runId = runId,
            stepId = "step2",
            stepName = "Process Data",
            stepType = "transform",
            startTime = now.minusSeconds(12),
            endTime = now.minusSeconds(6),
            status = StepStatus.COMPLETED,
            memoryUsage = 40 * 1024 * 1024,
            cpuTime = 5000,
            retryCount = 0
        )
        
        // Step 3: Store Results (same)
        stepMetrics["step3"] = StepMetrics(
            workflowId = workflowId,
            runId = runId,
            stepId = "step3",
            stepName = "Store Results",
            stepType = "database",
            startTime = now.minusSeconds(6),
            endTime = now.minusSeconds(3),
            status = StepStatus.COMPLETED,
            memoryUsage = 5 * 1024 * 1024,
            cpuTime = 1000,
            retryCount = 0
        )
        
        return ExecutionMetrics(
            workflowId = workflowId,
            runId = runId,
            startTime = now.minusSeconds(15),
            endTime = now.minusSeconds(3),
            status = ExecutionStatus.COMPLETED,
            stepMetrics = stepMetrics,
            totalSteps = 3,
            completedSteps = 3,
            failedSteps = 0,
            skippedSteps = 0
        )
    }
}
