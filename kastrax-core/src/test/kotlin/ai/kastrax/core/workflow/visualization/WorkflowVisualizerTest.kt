package ai.kastrax.core.workflow.visualization

import ai.kastrax.core.workflow.history.ExecutionRecord
import ai.kastrax.core.workflow.history.StepRecord
import ai.kastrax.core.workflow.monitoring.ExecutionMetrics
import ai.kastrax.core.workflow.monitoring.ExecutionStatus
import ai.kastrax.core.workflow.monitoring.StepMetrics
import ai.kastrax.core.workflow.monitoring.StepStatus
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Instant

class WorkflowVisualizerTest {
    private lateinit var visualizer: WorkflowVisualizer
    private lateinit var steps: List<WorkflowStep>
    private lateinit var connections: List<StepConnection>
    private lateinit var executionMetrics: ExecutionMetrics
    private lateinit var executionRecord: ExecutionRecord
    
    @BeforeEach
    fun setUp() {
        visualizer = WorkflowVisualizer()
        
        // Create sample steps
        steps = listOf(
            WorkflowStep(
                id = "step1",
                name = "Start",
                type = "start",
                description = "Start step"
            ),
            WorkflowStep(
                id = "step2",
                name = "Process",
                type = "process",
                description = "Process step"
            ),
            WorkflowStep(
                id = "step3",
                name = "End",
                type = "end",
                description = "End step"
            )
        )
        
        // Create sample connections
        connections = listOf(
            StepConnection(
                sourceId = "step1",
                targetId = "step2"
            ),
            StepConnection(
                sourceId = "step2",
                targetId = "step3"
            )
        )
        
        // Create sample execution metrics
        val now = Instant.now()
        val stepMetrics = mutableMapOf<String, StepMetrics>()
        
        stepMetrics["step1"] = StepMetrics(
            workflowId = "test-workflow",
            runId = "test-run",
            stepId = "step1",
            stepName = "Start",
            stepType = "start",
            startTime = now.minusSeconds(30),
            endTime = now.minusSeconds(25),
            status = StepStatus.COMPLETED
        )
        
        stepMetrics["step2"] = StepMetrics(
            workflowId = "test-workflow",
            runId = "test-run",
            stepId = "step2",
            stepName = "Process",
            stepType = "process",
            startTime = now.minusSeconds(25),
            endTime = now.minusSeconds(10),
            status = StepStatus.COMPLETED
        )
        
        stepMetrics["step3"] = StepMetrics(
            workflowId = "test-workflow",
            runId = "test-run",
            stepId = "step3",
            stepName = "End",
            stepType = "end",
            startTime = now.minusSeconds(10),
            endTime = now.minusSeconds(5),
            status = StepStatus.COMPLETED
        )
        
        executionMetrics = ExecutionMetrics(
            workflowId = "test-workflow",
            runId = "test-run",
            startTime = now.minusSeconds(30),
            endTime = now.minusSeconds(5),
            status = ExecutionStatus.COMPLETED,
            stepMetrics = stepMetrics,
            totalSteps = 3,
            completedSteps = 3,
            failedSteps = 0,
            skippedSteps = 0
        )
        
        // Create sample execution record
        val stepRecords = listOf(
            StepRecord(
                stepId = "step1",
                stepName = "Start",
                stepType = "start",
                startTime = now.minusSeconds(30),
                endTime = now.minusSeconds(25),
                status = "COMPLETED"
            ),
            StepRecord(
                stepId = "step2",
                stepName = "Process",
                stepType = "process",
                startTime = now.minusSeconds(25),
                endTime = now.minusSeconds(10),
                status = "COMPLETED"
            ),
            StepRecord(
                stepId = "step3",
                stepName = "End",
                stepType = "end",
                startTime = now.minusSeconds(10),
                endTime = now.minusSeconds(5),
                status = "COMPLETED"
            )
        )
        
        executionRecord = ExecutionRecord(
            workflowId = "test-workflow",
            runId = "test-run",
            startTime = now.minusSeconds(30),
            endTime = now.minusSeconds(5),
            status = ExecutionStatus.COMPLETED,
            stepRecords = stepRecords
        )
    }
    
    @Test
    fun `test visualize workflow in DOT format`() {
        val visualization = visualizer.visualizeWorkflow(
            workflowId = "test-workflow",
            steps = steps,
            connections = connections,
            format = VisualizationFormat.DOT,
            executionMetrics = executionMetrics
        )
        
        // Verify DOT format
        assertTrue(visualization.startsWith("digraph \"test-workflow\" {"))
        assertTrue(visualization.contains("\"step1\" ["))
        assertTrue(visualization.contains("\"step2\" ["))
        assertTrue(visualization.contains("\"step3\" ["))
        assertTrue(visualization.contains("\"step1\" -> \"step2\""))
        assertTrue(visualization.contains("\"step2\" -> \"step3\""))
    }
    
    @Test
    fun `test visualize workflow in Mermaid format`() {
        val visualization = visualizer.visualizeWorkflow(
            workflowId = "test-workflow",
            steps = steps,
            connections = connections,
            format = VisualizationFormat.MERMAID,
            executionMetrics = executionMetrics
        )
        
        // Verify Mermaid format
        assertTrue(visualization.startsWith("```mermaid"))
        assertTrue(visualization.contains("graph LR"))
        assertTrue(visualization.contains("step1[\""))
        assertTrue(visualization.contains("step2[\""))
        assertTrue(visualization.contains("step3[\""))
        assertTrue(visualization.contains("step1 --> step2"))
        assertTrue(visualization.contains("step2 --> step3"))
    }
    
    @Test
    fun `test visualize workflow in JSON format`() {
        val visualization = visualizer.visualizeWorkflow(
            workflowId = "test-workflow",
            steps = steps,
            connections = connections,
            format = VisualizationFormat.JSON,
            executionMetrics = executionMetrics
        )
        
        // Verify JSON format
        assertTrue(visualization.contains("\"id\":\"test-workflow\""))
        assertTrue(visualization.contains("\"id\":\"step1\""))
        assertTrue(visualization.contains("\"id\":\"step2\""))
        assertTrue(visualization.contains("\"id\":\"step3\""))
        assertTrue(visualization.contains("\"source\":\"step1\",\"target\":\"step2\""))
        assertTrue(visualization.contains("\"source\":\"step2\",\"target\":\"step3\""))
    }
    
    @Test
    fun `test visualize workflow in TEXT format`() {
        val visualization = visualizer.visualizeWorkflow(
            workflowId = "test-workflow",
            steps = steps,
            connections = connections,
            format = VisualizationFormat.TEXT,
            executionMetrics = executionMetrics
        )
        
        // Verify TEXT format
        assertTrue(visualization.startsWith("Workflow: test-workflow"))
        assertTrue(visualization.contains("Steps:"))
        assertTrue(visualization.contains("- step1: Start (start)"))
        assertTrue(visualization.contains("- step2: Process (process)"))
        assertTrue(visualization.contains("- step3: End (end)"))
        assertTrue(visualization.contains("Connections:"))
        assertTrue(visualization.contains("- step1 -> step2"))
        assertTrue(visualization.contains("- step2 -> step3"))
    }
    
    @Test
    fun `test visualize execution`() {
        val visualization = visualizer.visualizeExecution(
            record = executionRecord,
            steps = steps,
            connections = connections,
            format = VisualizationFormat.DOT
        )
        
        // Verify visualization
        assertTrue(visualization.startsWith("digraph \"test-workflow\" {"))
        assertTrue(visualization.contains("\"step1\" ["))
        assertTrue(visualization.contains("\"step2\" ["))
        assertTrue(visualization.contains("\"step3\" ["))
        assertTrue(visualization.contains("\"step1\" -> \"step2\""))
        assertTrue(visualization.contains("\"step2\" -> \"step3\""))
    }
    
    @Test
    fun `test save visualization`(@TempDir tempDir: Path) {
        val visualization = visualizer.visualizeWorkflow(
            workflowId = "test-workflow",
            steps = steps,
            connections = connections,
            format = VisualizationFormat.DOT
        )
        
        val filePath = tempDir.resolve("test-workflow.dot").toString()
        val savedPath = visualizer.saveVisualization(
            visualization = visualization,
            filePath = filePath,
            format = VisualizationFormat.DOT
        )
        
        // Verify file was saved
        assertTrue(savedPath.toFile().exists())
        assertTrue(savedPath.toFile().readText() == visualization)
    }
    
    @Test
    fun `test generate HTML report`() {
        val report = visualizer.generateHtmlReport(
            record = executionRecord,
            steps = steps,
            connections = connections
        )
        
        // Verify HTML report
        assertTrue(report.startsWith("<!DOCTYPE html>"))
        assertTrue(report.contains("<title>Workflow Execution Report: test-workflow</title>"))
        assertTrue(report.contains("<h1>Workflow Execution Report</h1>"))
        assertTrue(report.contains("<strong>Workflow ID:</strong> test-workflow"))
        assertTrue(report.contains("<strong>Run ID:</strong> test-run"))
        assertTrue(report.contains("<h2>Workflow Visualization</h2>"))
        assertTrue(report.contains("<h2>Step Details</h2>"))
    }
    
    @Test
    fun `test save HTML report`(@TempDir tempDir: Path) {
        val report = visualizer.generateHtmlReport(
            record = executionRecord,
            steps = steps,
            connections = connections
        )
        
        val filePath = tempDir.resolve("test-workflow-report.html").toString()
        val savedPath = visualizer.saveHtmlReport(
            report = report,
            filePath = filePath
        )
        
        // Verify file was saved
        assertTrue(savedPath.toFile().exists())
        assertTrue(savedPath.toFile().readText() == report)
    }
}
