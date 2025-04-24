package ai.kastrax.core.workflow.visualization

import ai.kastrax.core.workflow.history.ExecutionRecord
import ai.kastrax.core.workflow.monitoring.ExecutionMetrics
import java.io.File
import java.nio.file.Path

/**
 * Visualizes workflows and their execution.
 *
 * @property graphGenerator The graph generator to use.
 */
class WorkflowVisualizer(
    private val graphGenerator: WorkflowGraphGenerator = WorkflowGraphGenerator()
) {
    /**
     * Visualizes a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @param steps The steps in the workflow.
     * @param connections The connections between steps.
     * @param format The format of the visualization.
     * @param executionMetrics Optional execution metrics to include in the visualization.
     * @return The visualization as a string.
     */
    fun visualizeWorkflow(
        workflowId: String,
        steps: List<WorkflowStep>,
        connections: List<StepConnection>,
        format: VisualizationFormat = VisualizationFormat.DOT,
        executionMetrics: ExecutionMetrics? = null
    ): String {
        return graphGenerator.generateWorkflowGraph(
            workflowId = workflowId,
            steps = steps,
            connections = connections,
            format = format,
            executionMetrics = executionMetrics
        )
    }

    /**
     * Visualizes a workflow execution from an execution record.
     *
     * @param record The execution record.
     * @param steps The steps in the workflow.
     * @param connections The connections between steps.
     * @param format The format of the visualization.
     * @return The visualization as a string.
     */
    fun visualizeExecution(
        record: ExecutionRecord,
        steps: List<WorkflowStep>,
        connections: List<StepConnection>,
        format: VisualizationFormat = VisualizationFormat.DOT
    ): String {
        // Convert step records to step metrics for visualization
        val stepMetrics = record.stepRecords.associate { stepRecord ->
            stepRecord.stepId to ai.kastrax.core.workflow.monitoring.StepMetrics(
                workflowId = record.workflowId,
                runId = record.runId,
                stepId = stepRecord.stepId,
                stepName = stepRecord.stepName,
                stepType = stepRecord.stepType,
                startTime = stepRecord.startTime,
                endTime = stepRecord.endTime,
                status = ai.kastrax.core.workflow.monitoring.StepStatus.valueOf(stepRecord.status),
                retryCount = stepRecord.retryCount,
                errorMessage = stepRecord.error
            )
        }
        
        // Create execution metrics from the record
        val executionMetrics = ExecutionMetrics(
            workflowId = record.workflowId,
            runId = record.runId,
            startTime = record.startTime,
            endTime = record.endTime,
            status = record.status,
            stepMetrics = stepMetrics.toMutableMap(),
            totalSteps = steps.size,
            completedSteps = stepMetrics.count { it.value.status == ai.kastrax.core.workflow.monitoring.StepStatus.COMPLETED },
            failedSteps = stepMetrics.count { it.value.status == ai.kastrax.core.workflow.monitoring.StepStatus.FAILED },
            skippedSteps = stepMetrics.count { it.value.status == ai.kastrax.core.workflow.monitoring.StepStatus.SKIPPED }
        )
        
        return visualizeWorkflow(
            workflowId = record.workflowId,
            steps = steps,
            connections = connections,
            format = format,
            executionMetrics = executionMetrics
        )
    }

    /**
     * Saves a workflow visualization to a file.
     *
     * @param visualization The visualization to save.
     * @param filePath The path to save the file to.
     * @param format The format of the visualization.
     * @return The path to the saved file.
     */
    fun saveVisualization(
        visualization: String,
        filePath: String,
        format: VisualizationFormat = VisualizationFormat.DOT
    ): Path {
        val extension = when (format) {
            VisualizationFormat.DOT -> "dot"
            VisualizationFormat.MERMAID -> "mmd"
            VisualizationFormat.JSON -> "json"
            VisualizationFormat.TEXT -> "txt"
        }
        
        val file = if (filePath.endsWith(".$extension")) {
            File(filePath)
        } else {
            File("$filePath.$extension")
        }
        
        file.writeText(visualization)
        return file.toPath()
    }

    /**
     * Generates an HTML report for a workflow execution.
     *
     * @param record The execution record.
     * @param steps The steps in the workflow.
     * @param connections The connections between steps.
     * @return The HTML report as a string.
     */
    fun generateHtmlReport(
        record: ExecutionRecord,
        steps: List<WorkflowStep>,
        connections: List<StepConnection>
    ): String {
        val mermaidGraph = visualizeExecution(record, steps, connections, VisualizationFormat.MERMAID)
        
        val sb = StringBuilder()
        sb.appendLine("<!DOCTYPE html>")
        sb.appendLine("<html>")
        sb.appendLine("<head>")
        sb.appendLine("  <title>Workflow Execution Report: ${record.workflowId}</title>")
        sb.appendLine("  <script src=\"https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js\"></script>")
        sb.appendLine("  <script>mermaid.initialize({startOnLoad:true});</script>")
        sb.appendLine("  <style>")
        sb.appendLine("    body { font-family: Arial, sans-serif; margin: 20px; }")
        sb.appendLine("    h1, h2, h3 { color: #333; }")
        sb.appendLine("    .container { max-width: 1200px; margin: 0 auto; }")
        sb.appendLine("    .section { margin-bottom: 30px; }")
        sb.appendLine("    .info-box { background-color: #f5f5f5; padding: 15px; border-radius: 5px; }")
        sb.appendLine("    .step-list { list-style-type: none; padding: 0; }")
        sb.appendLine("    .step-item { margin-bottom: 10px; padding: 10px; border-radius: 5px; }")
        sb.appendLine("    .step-completed { background-color: #e6ffe6; }")
        sb.appendLine("    .step-failed { background-color: #ffe6e6; }")
        sb.appendLine("    .step-running { background-color: #e6f2ff; }")
        sb.appendLine("    .step-suspended { background-color: #fffde6; }")
        sb.appendLine("    .step-waiting { background-color: #f2f2f2; }")
        sb.appendLine("    .step-skipped { background-color: #f0e6ff; }")
        sb.appendLine("  </style>")
        sb.appendLine("</head>")
        sb.appendLine("<body>")
        sb.appendLine("  <div class=\"container\">")
        
        // Header
        sb.appendLine("    <div class=\"section\">")
        sb.appendLine("      <h1>Workflow Execution Report</h1>")
        sb.appendLine("      <div class=\"info-box\">")
        sb.appendLine("        <p><strong>Workflow ID:</strong> ${record.workflowId}</p>")
        sb.appendLine("        <p><strong>Run ID:</strong> ${record.runId}</p>")
        sb.appendLine("        <p><strong>Status:</strong> ${record.status}</p>")
        sb.appendLine("        <p><strong>Start Time:</strong> ${record.startTime}</p>")
        record.endTime?.let { sb.appendLine("        <p><strong>End Time:</strong> $it</p>") }
        record.endTime?.let {
            val duration = java.time.Duration.between(record.startTime, it).toMillis()
            sb.appendLine("        <p><strong>Duration:</strong> $duration ms</p>")
        }
        sb.appendLine("      </div>")
        sb.appendLine("    </div>")
        
        // Visualization
        sb.appendLine("    <div class=\"section\">")
        sb.appendLine("      <h2>Workflow Visualization</h2>")
        sb.appendLine("      <div class=\"mermaid\">")
        // Extract the mermaid diagram content (remove the ```mermaid and ``` lines)
        val mermaidContent = mermaidGraph.lines()
            .filter { !it.startsWith("```") }
            .joinToString("\n")
        sb.appendLine(mermaidContent)
        sb.appendLine("      </div>")
        sb.appendLine("    </div>")
        
        // Step Details
        sb.appendLine("    <div class=\"section\">")
        sb.appendLine("      <h2>Step Details</h2>")
        sb.appendLine("      <ul class=\"step-list\">")
        record.stepRecords.forEach { stepRecord ->
            val stepClass = when (stepRecord.status) {
                "COMPLETED" -> "step-completed"
                "FAILED" -> "step-failed"
                "RUNNING" -> "step-running"
                "SUSPENDED" -> "step-suspended"
                "WAITING" -> "step-waiting"
                "SKIPPED" -> "step-skipped"
                else -> ""
            }
            
            sb.appendLine("        <li class=\"step-item $stepClass\">")
            sb.appendLine("          <h3>${stepRecord.stepName} (${stepRecord.stepType})</h3>")
            sb.appendLine("          <p><strong>ID:</strong> ${stepRecord.stepId}</p>")
            sb.appendLine("          <p><strong>Status:</strong> ${stepRecord.status}</p>")
            sb.appendLine("          <p><strong>Start Time:</strong> ${stepRecord.startTime}</p>")
            stepRecord.endTime?.let { sb.appendLine("          <p><strong>End Time:</strong> $it</p>") }
            stepRecord.endTime?.let {
                val duration = java.time.Duration.between(stepRecord.startTime, it).toMillis()
                sb.appendLine("          <p><strong>Duration:</strong> $duration ms</p>")
            }
            if (stepRecord.retryCount > 0) {
                sb.appendLine("          <p><strong>Retry Count:</strong> ${stepRecord.retryCount}</p>")
            }
            stepRecord.error?.let { sb.appendLine("          <p><strong>Error:</strong> $it</p>") }
            sb.appendLine("        </li>")
        }
        sb.appendLine("      </ul>")
        sb.appendLine("    </div>")
        
        // Input/Output
        sb.appendLine("    <div class=\"section\">")
        sb.appendLine("      <h2>Input/Output</h2>")
        sb.appendLine("      <h3>Workflow Input</h3>")
        sb.appendLine("      <pre>${record.input?.toString() ?: "No input data"}</pre>")
        sb.appendLine("      <h3>Workflow Output</h3>")
        sb.appendLine("      <pre>${record.output?.toString() ?: "No output data"}</pre>")
        sb.appendLine("    </div>")
        
        sb.appendLine("  </div>")
        sb.appendLine("</body>")
        sb.appendLine("</html>")
        
        return sb.toString()
    }

    /**
     * Saves an HTML report to a file.
     *
     * @param report The HTML report to save.
     * @param filePath The path to save the file to.
     * @return The path to the saved file.
     */
    fun saveHtmlReport(report: String, filePath: String): Path {
        val file = if (filePath.endsWith(".html")) {
            File(filePath)
        } else {
            File("$filePath.html")
        }
        
        file.writeText(report)
        return file.toPath()
    }
}
