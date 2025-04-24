package ai.kastrax.examples.workflow

import ai.kastrax.core.workflow.history.InMemoryHistoryStorage
import ai.kastrax.core.workflow.history.WorkflowExecutionHistory
import ai.kastrax.core.workflow.monitoring.AlertCondition
import ai.kastrax.core.workflow.monitoring.AlertHandler
import ai.kastrax.core.workflow.monitoring.AlertManager
import ai.kastrax.core.workflow.monitoring.AlertSeverity
import ai.kastrax.core.workflow.monitoring.ExecutionDurationCondition
import ai.kastrax.core.workflow.monitoring.ExecutionStatus
import ai.kastrax.core.workflow.monitoring.InMemoryMetricsStorage
import ai.kastrax.core.workflow.monitoring.LoggingAlertHandler
import ai.kastrax.core.workflow.monitoring.StepDurationCondition
import ai.kastrax.core.workflow.monitoring.StepFailureCondition
import ai.kastrax.core.workflow.monitoring.StepStatus
import ai.kastrax.core.workflow.monitoring.WorkflowFailureCondition
import ai.kastrax.core.workflow.monitoring.WorkflowMetricsCollector
import ai.kastrax.core.workflow.monitoring.WorkflowMonitor
import ai.kastrax.core.workflow.performance.BottleneckDetector
import ai.kastrax.core.workflow.performance.PerformanceAnalyzer
import ai.kastrax.core.workflow.visualization.StepConnection
import ai.kastrax.core.workflow.visualization.VisualizationFormat
import ai.kastrax.core.workflow.visualization.WorkflowStep
import ai.kastrax.core.workflow.visualization.WorkflowVisualizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.util.UUID

/**
 * Example demonstrating workflow monitoring and visualization.
 */
object WorkflowMonitoringExample {
    @JvmStatic
    fun main(args: Array<String>) {
        // Create components
        val metricsStorage = InMemoryMetricsStorage()
        val metricsCollector = WorkflowMetricsCollector(metricsStorage)
        val historyStorage = InMemoryHistoryStorage()
        val executionHistory = WorkflowExecutionHistory(historyStorage)
        val alertManager = AlertManager(LoggingAlertHandler())
        val workflowMonitor = WorkflowMonitor(metricsCollector, executionHistory, alertManager)
        val performanceAnalyzer = PerformanceAnalyzer(BottleneckDetector())
        val visualizer = WorkflowVisualizer()
        
        // Register alert conditions
        workflowMonitor.registerAlertCondition(
            WorkflowFailureCondition(),
            AlertSeverity.ERROR
        )
        
        workflowMonitor.registerAlertCondition(
            ExecutionDurationCondition(Duration.ofSeconds(30)),
            AlertSeverity.WARNING
        )
        
        workflowMonitor.registerAlertCondition(
            StepFailureCondition(),
            AlertSeverity.ERROR
        )
        
        workflowMonitor.registerAlertCondition(
            StepDurationCondition(threshold = Duration.ofSeconds(10)),
            AlertSeverity.WARNING
        )
        
        // Define workflow steps
        val steps = listOf(
            WorkflowStep(
                id = "step1",
                name = "Fetch Data",
                type = "http",
                description = "Fetch data from API"
            ),
            WorkflowStep(
                id = "step2",
                name = "Process Data",
                type = "transform",
                description = "Process and transform data"
            ),
            WorkflowStep(
                id = "step3",
                name = "Store Results",
                type = "database",
                description = "Store results in database"
            )
        )
        
        // Define connections
        val connections = listOf(
            StepConnection(
                sourceId = "step1",
                targetId = "step2"
            ),
            StepConnection(
                sourceId = "step2",
                targetId = "step3"
            )
        )
        
        // Start collecting metrics and alerts
        val scope = CoroutineScope(Dispatchers.Default)
        
        scope.launch {
            workflowMonitor.metricsFlow.collect { metrics ->
                println("Received metrics update: ${metrics.workflowId} (${metrics.runId}) - Status: ${metrics.status}")
                println("Progress: ${metrics.getProgress()}%")
            }
        }
        
        scope.launch {
            workflowMonitor.alertsFlow.collect { alert ->
                println("Received alert: ${alert.message} (${alert.severity})")
            }
        }
        
        // Simulate workflow execution
        runBlocking {
            val workflowId = "sample-workflow"
            val runId = UUID.randomUUID().toString()
            
            // Start monitoring
            workflowMonitor.startMonitoring(workflowId, runId)
            
            // Start workflow execution
            println("Starting workflow execution: $workflowId ($runId)")
            val executionMetrics = metricsCollector.startExecution(
                workflowId = workflowId,
                runId = runId,
                totalSteps = steps.size
            )
            
            executionHistory.recordExecutionStart(
                workflowId = workflowId,
                runId = runId,
                input = mapOf("param1" to "value1", "param2" to 42)
            )
            
            // Execute step 1
            println("Executing step: ${steps[0].name}")
            val step1Metrics = metricsCollector.startStep(
                workflowId = workflowId,
                runId = runId,
                stepId = steps[0].id,
                stepName = steps[0].name,
                stepType = steps[0].type
            )
            
            // Simulate step execution
            delay(5000)
            
            metricsCollector.completeStep(
                workflowId = workflowId,
                runId = runId,
                stepId = steps[0].id,
                status = StepStatus.COMPLETED,
                outputSize = 1024,
                memoryUsage = 2048,
                cpuTime = 4500
            )
            
            executionHistory.recordStepExecution(
                workflowId = workflowId,
                runId = runId,
                stepId = steps[0].id,
                stepName = steps[0].name,
                stepType = steps[0].type,
                status = StepStatus.COMPLETED,
                startTime = step1Metrics.startTime,
                endTime = step1Metrics.startTime.plusMillis(5000),
                input = mapOf("url" to "https://api.example.com/data"),
                output = mapOf("data" to "sample data")
            )
            
            // Execute step 2
            println("Executing step: ${steps[1].name}")
            val step2Metrics = metricsCollector.startStep(
                workflowId = workflowId,
                runId = runId,
                stepId = steps[1].id,
                stepName = steps[1].name,
                stepType = steps[1].type
            )
            
            // Simulate step execution (this one takes longer)
            delay(12000)
            
            metricsCollector.completeStep(
                workflowId = workflowId,
                runId = runId,
                stepId = steps[1].id,
                status = StepStatus.COMPLETED,
                outputSize = 512,
                memoryUsage = 4096,
                cpuTime = 11500
            )
            
            executionHistory.recordStepExecution(
                workflowId = workflowId,
                runId = runId,
                stepId = steps[1].id,
                stepName = steps[1].name,
                stepType = steps[1].type,
                status = StepStatus.COMPLETED,
                startTime = step2Metrics.startTime,
                endTime = step2Metrics.startTime.plusMillis(12000),
                input = mapOf("data" to "sample data"),
                output = mapOf("processedData" to "processed sample data")
            )
            
            // Execute step 3
            println("Executing step: ${steps[2].name}")
            val step3Metrics = metricsCollector.startStep(
                workflowId = workflowId,
                runId = runId,
                stepId = steps[2].id,
                stepName = steps[2].name,
                stepType = steps[2].type
            )
            
            // Simulate step execution
            delay(3000)
            
            metricsCollector.completeStep(
                workflowId = workflowId,
                runId = runId,
                stepId = steps[2].id,
                status = StepStatus.COMPLETED,
                outputSize = 256,
                memoryUsage = 1024,
                cpuTime = 2500
            )
            
            executionHistory.recordStepExecution(
                workflowId = workflowId,
                runId = runId,
                stepId = steps[2].id,
                stepName = steps[2].name,
                stepType = steps[2].type,
                status = StepStatus.COMPLETED,
                startTime = step3Metrics.startTime,
                endTime = step3Metrics.startTime.plusMillis(3000),
                input = mapOf("processedData" to "processed sample data"),
                output = mapOf("result" to "success")
            )
            
            // Complete workflow execution
            println("Completing workflow execution")
            metricsCollector.completeExecution(
                workflowId = workflowId,
                runId = runId,
                status = ExecutionStatus.COMPLETED
            )
            
            executionHistory.recordExecutionCompletion(
                workflowId = workflowId,
                runId = runId,
                status = ExecutionStatus.COMPLETED,
                output = mapOf("result" to "success")
            )
            
            // Wait for monitoring to complete
            delay(1000)
            workflowMonitor.stopMonitoring(workflowId, runId)
            
            // Get execution record
            val executionRecord = executionHistory.getExecutionRecord(workflowId, runId)
            if (executionRecord != null) {
                // Generate visualization
                val dotVisualization = visualizer.visualizeExecution(
                    record = executionRecord,
                    steps = steps,
                    connections = connections,
                    format = VisualizationFormat.DOT
                )
                
                val mermaidVisualization = visualizer.visualizeExecution(
                    record = executionRecord,
                    steps = steps,
                    connections = connections,
                    format = VisualizationFormat.MERMAID
                )
                
                // Save visualizations
                val dotPath = visualizer.saveVisualization(
                    visualization = dotVisualization,
                    filePath = "workflow-visualization.dot",
                    format = VisualizationFormat.DOT
                )
                
                val mermaidPath = visualizer.saveVisualization(
                    visualization = mermaidVisualization,
                    filePath = "workflow-visualization.mmd",
                    format = VisualizationFormat.MERMAID
                )
                
                println("Saved DOT visualization to: $dotPath")
                println("Saved Mermaid visualization to: $mermaidPath")
                
                // Generate HTML report
                val htmlReport = visualizer.generateHtmlReport(
                    record = executionRecord,
                    steps = steps,
                    connections = connections
                )
                
                val reportPath = visualizer.saveHtmlReport(
                    report = htmlReport,
                    filePath = "workflow-report.html"
                )
                
                println("Saved HTML report to: $reportPath")
                
                // Analyze performance
                val finalMetrics = metricsCollector.getExecutionMetrics(workflowId, runId)
                if (finalMetrics != null) {
                    val performanceReport = performanceAnalyzer.analyzeExecution(finalMetrics)
                    println("\nPerformance Report:")
                    println(performanceReport.generateSummary())
                }
            }
        }
    }
}
