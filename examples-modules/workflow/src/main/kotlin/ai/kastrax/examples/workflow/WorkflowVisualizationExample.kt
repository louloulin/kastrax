package ai.kastrax.examples.workflow

import ai.kastrax.core.workflow.history.ExecutionRecord
import ai.kastrax.core.workflow.history.StepRecord
import ai.kastrax.core.workflow.monitoring.ExecutionStatus
import ai.kastrax.core.workflow.visualization.StepConnection
import ai.kastrax.core.workflow.visualization.VisualizationFormat
import ai.kastrax.core.workflow.visualization.WorkflowStep
import ai.kastrax.core.workflow.visualization.WorkflowVisualizer
import java.time.Instant

/**
 * Example demonstrating workflow visualization.
 */
object WorkflowVisualizationExample {
    @JvmStatic
    fun main(args: Array<String>) {
        // Create workflow visualizer
        val visualizer = WorkflowVisualizer()
        
        // Define a more complex workflow with branches and conditions
        val steps = listOf(
            WorkflowStep(
                id = "start",
                name = "Start",
                type = "start",
                description = "Start of workflow"
            ),
            WorkflowStep(
                id = "fetch",
                name = "Fetch Data",
                type = "http",
                description = "Fetch data from API"
            ),
            WorkflowStep(
                id = "validate",
                name = "Validate Data",
                type = "validation",
                description = "Validate data format and content"
            ),
            WorkflowStep(
                id = "condition",
                name = "Check Data Size",
                type = "condition",
                description = "Check if data size is large"
            ),
            WorkflowStep(
                id = "process_small",
                name = "Process Small Data",
                type = "transform",
                description = "Process small data set"
            ),
            WorkflowStep(
                id = "process_large",
                name = "Process Large Data",
                type = "transform",
                description = "Process large data set"
            ),
            WorkflowStep(
                id = "merge",
                name = "Merge Results",
                type = "merge",
                description = "Merge processing results"
            ),
            WorkflowStep(
                id = "store",
                name = "Store Results",
                type = "database",
                description = "Store results in database"
            ),
            WorkflowStep(
                id = "notify",
                name = "Send Notification",
                type = "notification",
                description = "Send completion notification"
            ),
            WorkflowStep(
                id = "end",
                name = "End",
                type = "end",
                description = "End of workflow"
            )
        )
        
        // Define connections with conditions
        val connections = listOf(
            StepConnection(
                sourceId = "start",
                targetId = "fetch"
            ),
            StepConnection(
                sourceId = "fetch",
                targetId = "validate"
            ),
            StepConnection(
                sourceId = "validate",
                targetId = "condition"
            ),
            StepConnection(
                sourceId = "condition",
                targetId = "process_small",
                condition = "size < 1MB"
            ),
            StepConnection(
                sourceId = "condition",
                targetId = "process_large",
                condition = "size >= 1MB"
            ),
            StepConnection(
                sourceId = "process_small",
                targetId = "merge"
            ),
            StepConnection(
                sourceId = "process_large",
                targetId = "merge"
            ),
            StepConnection(
                sourceId = "merge",
                targetId = "store"
            ),
            StepConnection(
                sourceId = "store",
                targetId = "notify"
            ),
            StepConnection(
                sourceId = "notify",
                targetId = "end"
            )
        )
        
        // Create a sample execution record
        val now = Instant.now()
        val executionRecord = ExecutionRecord(
            workflowId = "complex-workflow",
            runId = "run-123",
            startTime = now.minusSeconds(60),
            endTime = now.minusSeconds(10),
            status = ExecutionStatus.COMPLETED,
            input = mapOf("source" to "api.example.com", "filter" to "products"),
            output = mapOf("processedItems" to 150, "status" to "success"),
            stepRecords = listOf(
                StepRecord(
                    stepId = "start",
                    stepName = "Start",
                    stepType = "start",
                    startTime = now.minusSeconds(60),
                    endTime = now.minusSeconds(59),
                    status = "COMPLETED"
                ),
                StepRecord(
                    stepId = "fetch",
                    stepName = "Fetch Data",
                    stepType = "http",
                    startTime = now.minusSeconds(59),
                    endTime = now.minusSeconds(50),
                    status = "COMPLETED",
                    input = mapOf("url" to "api.example.com/products"),
                    output = mapOf("data" to "product data", "size" to "2.5MB")
                ),
                StepRecord(
                    stepId = "validate",
                    stepName = "Validate Data",
                    stepType = "validation",
                    startTime = now.minusSeconds(50),
                    endTime = now.minusSeconds(48),
                    status = "COMPLETED",
                    input = mapOf("data" to "product data"),
                    output = mapOf("valid" to true)
                ),
                StepRecord(
                    stepId = "condition",
                    stepName = "Check Data Size",
                    stepType = "condition",
                    startTime = now.minusSeconds(48),
                    endTime = now.minusSeconds(47),
                    status = "COMPLETED",
                    input = mapOf("size" to "2.5MB"),
                    output = mapOf("result" to "large")
                ),
                StepRecord(
                    stepId = "process_large",
                    stepName = "Process Large Data",
                    stepType = "transform",
                    startTime = now.minusSeconds(47),
                    endTime = now.minusSeconds(30),
                    status = "COMPLETED",
                    input = mapOf("data" to "product data"),
                    output = mapOf("processedData" to "processed product data")
                ),
                StepRecord(
                    stepId = "merge",
                    stepName = "Merge Results",
                    stepType = "merge",
                    startTime = now.minusSeconds(30),
                    endTime = now.minusSeconds(28),
                    status = "COMPLETED",
                    input = mapOf("data" to "processed product data"),
                    output = mapOf("mergedData" to "merged product data")
                ),
                StepRecord(
                    stepId = "store",
                    stepName = "Store Results",
                    stepType = "database",
                    startTime = now.minusSeconds(28),
                    endTime = now.minusSeconds(20),
                    status = "COMPLETED",
                    input = mapOf("data" to "merged product data"),
                    output = mapOf("status" to "success", "count" to 150)
                ),
                StepRecord(
                    stepId = "notify",
                    stepName = "Send Notification",
                    stepType = "notification",
                    startTime = now.minusSeconds(20),
                    endTime = now.minusSeconds(15),
                    status = "COMPLETED",
                    input = mapOf("message" to "Processing completed"),
                    output = mapOf("sent" to true)
                ),
                StepRecord(
                    stepId = "end",
                    stepName = "End",
                    stepType = "end",
                    startTime = now.minusSeconds(15),
                    endTime = now.minusSeconds(10),
                    status = "COMPLETED"
                )
            )
        )
        
        // Generate visualizations in different formats
        println("Generating workflow visualizations...")
        
        // DOT format (for Graphviz)
        val dotVisualization = visualizer.visualizeExecution(
            record = executionRecord,
            steps = steps,
            connections = connections,
            format = VisualizationFormat.DOT
        )
        
        val dotPath = visualizer.saveVisualization(
            visualization = dotVisualization,
            filePath = "complex-workflow.dot",
            format = VisualizationFormat.DOT
        )
        
        println("Saved DOT visualization to: $dotPath")
        println("To render the DOT file, use: dot -Tpng complex-workflow.dot -o complex-workflow.png")
        
        // Mermaid format (for Markdown)
        val mermaidVisualization = visualizer.visualizeExecution(
            record = executionRecord,
            steps = steps,
            connections = connections,
            format = VisualizationFormat.MERMAID
        )
        
        val mermaidPath = visualizer.saveVisualization(
            visualization = mermaidVisualization,
            filePath = "complex-workflow.mmd",
            format = VisualizationFormat.MERMAID
        )
        
        println("Saved Mermaid visualization to: $mermaidPath")
        println("You can paste the Mermaid content into a Markdown file or use the Mermaid Live Editor")
        
        // JSON format (for custom visualization tools)
        val jsonVisualization = visualizer.visualizeExecution(
            record = executionRecord,
            steps = steps,
            connections = connections,
            format = VisualizationFormat.JSON
        )
        
        val jsonPath = visualizer.saveVisualization(
            visualization = jsonVisualization,
            filePath = "complex-workflow.json",
            format = VisualizationFormat.JSON
        )
        
        println("Saved JSON visualization to: $jsonPath")
        
        // Text format (for simple console output)
        val textVisualization = visualizer.visualizeExecution(
            record = executionRecord,
            steps = steps,
            connections = connections,
            format = VisualizationFormat.TEXT
        )
        
        val textPath = visualizer.saveVisualization(
            visualization = textVisualization,
            filePath = "complex-workflow.txt",
            format = VisualizationFormat.TEXT
        )
        
        println("Saved TEXT visualization to: $textPath")
        
        // Generate HTML report
        println("Generating HTML report...")
        val htmlReport = visualizer.generateHtmlReport(
            record = executionRecord,
            steps = steps,
            connections = connections
        )
        
        val reportPath = visualizer.saveHtmlReport(
            report = htmlReport,
            filePath = "complex-workflow-report.html"
        )
        
        println("Saved HTML report to: $reportPath")
        println("Open the HTML report in a web browser to view the interactive visualization")
    }
}
