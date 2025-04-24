package ai.kastrax.core.workflow.monitoring

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class WorkflowMetricsCollectorTest {
    private lateinit var metricsStorage: InMemoryMetricsStorage
    private lateinit var metricsCollector: WorkflowMetricsCollector
    
    @BeforeEach
    fun setUp() {
        metricsStorage = InMemoryMetricsStorage()
        metricsCollector = WorkflowMetricsCollector(metricsStorage)
    }
    
    @Test
    fun `test start execution`() {
        val workflowId = "test-workflow"
        val runId = "test-run"
        val totalSteps = 5
        val tags = mapOf("env" to "test")
        
        val metrics = metricsCollector.startExecution(
            workflowId = workflowId,
            runId = runId,
            totalSteps = totalSteps,
            tags = tags
        )
        
        assertEquals(workflowId, metrics.workflowId)
        assertEquals(runId, metrics.runId)
        assertEquals(totalSteps, metrics.totalSteps)
        assertEquals(tags, metrics.tags)
        assertEquals(ExecutionStatus.RUNNING, metrics.status)
        assertNotNull(metrics.startTime)
        assertEquals(null, metrics.endTime)
        
        // Verify metrics were saved to storage
        val storedMetrics = metricsStorage.getExecutionMetrics(workflowId, runId)
        assertNotNull(storedMetrics)
        assertEquals(metrics, storedMetrics)
    }
    
    @Test
    fun `test complete execution`() {
        val workflowId = "test-workflow"
        val runId = "test-run"
        
        // Start execution
        metricsCollector.startExecution(
            workflowId = workflowId,
            runId = runId
        )
        
        // Complete execution
        val updatedMetrics = metricsCollector.completeExecution(
            workflowId = workflowId,
            runId = runId,
            status = ExecutionStatus.COMPLETED
        )
        
        assertNotNull(updatedMetrics)
        assertEquals(ExecutionStatus.COMPLETED, updatedMetrics!!.status)
        assertNotNull(updatedMetrics.endTime)
        
        // Verify metrics were updated in storage
        val storedMetrics = metricsStorage.getExecutionMetrics(workflowId, runId)
        assertNotNull(storedMetrics)
        assertEquals(updatedMetrics, storedMetrics)
    }
    
    @Test
    fun `test start step`() {
        val workflowId = "test-workflow"
        val runId = "test-run"
        val stepId = "test-step"
        val stepName = "Test Step"
        val stepType = "test-type"
        
        val stepMetrics = metricsCollector.startStep(
            workflowId = workflowId,
            runId = runId,
            stepId = stepId,
            stepName = stepName,
            stepType = stepType
        )
        
        assertEquals(workflowId, stepMetrics.workflowId)
        assertEquals(runId, stepMetrics.runId)
        assertEquals(stepId, stepMetrics.stepId)
        assertEquals(stepName, stepMetrics.stepName)
        assertEquals(stepType, stepMetrics.stepType)
        assertEquals(StepStatus.RUNNING, stepMetrics.status)
        assertNotNull(stepMetrics.startTime)
        assertEquals(null, stepMetrics.endTime)
        
        // Verify step metrics were saved to storage
        val storedStepMetrics = metricsStorage.getStepMetricsForExecution(workflowId, runId)
        assertEquals(1, storedStepMetrics.size)
        assertEquals(stepMetrics, storedStepMetrics[0])
    }
    
    @Test
    fun `test complete step`() {
        val workflowId = "test-workflow"
        val runId = "test-run"
        val stepId = "test-step"
        
        // Start execution
        metricsCollector.startExecution(
            workflowId = workflowId,
            runId = runId,
            totalSteps = 1
        )
        
        // Start step
        metricsCollector.startStep(
            workflowId = workflowId,
            runId = runId,
            stepId = stepId,
            stepName = "Test Step",
            stepType = "test-type"
        )
        
        // Complete step
        val updatedStepMetrics = metricsCollector.completeStep(
            workflowId = workflowId,
            runId = runId,
            stepId = stepId,
            status = StepStatus.COMPLETED,
            outputSize = 1024,
            memoryUsage = 2048,
            cpuTime = 500,
            customMetrics = mapOf("custom" to "value")
        )
        
        assertNotNull(updatedStepMetrics)
        assertEquals(StepStatus.COMPLETED, updatedStepMetrics!!.status)
        assertNotNull(updatedStepMetrics.endTime)
        assertEquals(1024L, updatedStepMetrics.outputSize)
        assertEquals(2048L, updatedStepMetrics.memoryUsage)
        assertEquals(500L, updatedStepMetrics.cpuTime)
        assertEquals("value", updatedStepMetrics.customMetrics["custom"])
        
        // Verify step metrics were updated in storage
        val storedStepMetrics = metricsStorage.getStepMetricsForExecution(workflowId, runId)
        assertEquals(1, storedStepMetrics.size)
        assertEquals(updatedStepMetrics, storedStepMetrics[0])
        
        // Verify execution metrics were updated
        val executionMetrics = metricsStorage.getExecutionMetrics(workflowId, runId)
        assertNotNull(executionMetrics)
        assertEquals(1, executionMetrics!!.completedSteps)
        assertEquals(updatedStepMetrics, executionMetrics.stepMetrics[stepId])
    }
    
    @Test
    fun `test record step retry`() {
        val workflowId = "test-workflow"
        val runId = "test-run"
        val stepId = "test-step"
        
        // Start step
        metricsCollector.startStep(
            workflowId = workflowId,
            runId = runId,
            stepId = stepId,
            stepName = "Test Step",
            stepType = "test-type"
        )
        
        // Record retry
        val updatedStepMetrics = metricsCollector.recordStepRetry(
            workflowId = workflowId,
            runId = runId,
            stepId = stepId
        )
        
        assertNotNull(updatedStepMetrics)
        assertEquals(1, updatedStepMetrics!!.retryCount)
        
        // Record another retry
        val updatedStepMetrics2 = metricsCollector.recordStepRetry(
            workflowId = workflowId,
            runId = runId,
            stepId = stepId
        )
        
        assertNotNull(updatedStepMetrics2)
        assertEquals(2, updatedStepMetrics2!!.retryCount)
        
        // Verify step metrics were updated in storage
        val storedStepMetrics = metricsStorage.getStepMetricsForExecution(workflowId, runId)
        assertEquals(1, storedStepMetrics.size)
        assertEquals(2, storedStepMetrics[0].retryCount)
    }
    
    @Test
    fun `test get execution metrics`() {
        val workflowId = "test-workflow"
        val runId = "test-run"
        
        // Start execution
        metricsCollector.startExecution(
            workflowId = workflowId,
            runId = runId
        )
        
        // Get execution metrics
        val metrics = metricsCollector.getExecutionMetrics(workflowId, runId)
        
        assertNotNull(metrics)
        assertEquals(workflowId, metrics!!.workflowId)
        assertEquals(runId, metrics.runId)
    }
    
    @Test
    fun `test get execution metrics for workflow`() {
        val workflowId = "test-workflow"
        
        // Start multiple executions
        metricsCollector.startExecution(
            workflowId = workflowId,
            runId = "run-1"
        )
        
        metricsCollector.startExecution(
            workflowId = workflowId,
            runId = "run-2"
        )
        
        // Get execution metrics for workflow
        val metrics = metricsCollector.getExecutionMetricsForWorkflow(workflowId)
        
        assertEquals(2, metrics.size)
        assertEquals(workflowId, metrics[0].workflowId)
        assertEquals(workflowId, metrics[1].workflowId)
    }
    
    @Test
    fun `test get step metrics for execution`() {
        val workflowId = "test-workflow"
        val runId = "test-run"
        
        // Start steps
        metricsCollector.startStep(
            workflowId = workflowId,
            runId = runId,
            stepId = "step-1",
            stepName = "Step 1",
            stepType = "test-type"
        )
        
        metricsCollector.startStep(
            workflowId = workflowId,
            runId = runId,
            stepId = "step-2",
            stepName = "Step 2",
            stepType = "test-type"
        )
        
        // Get step metrics for execution
        val stepMetrics = metricsCollector.getStepMetricsForExecution(workflowId, runId)
        
        assertEquals(2, stepMetrics.size)
        assertEquals(workflowId, stepMetrics[0].workflowId)
        assertEquals(runId, stepMetrics[0].runId)
        assertEquals(workflowId, stepMetrics[1].workflowId)
        assertEquals(runId, stepMetrics[1].runId)
    }
}
