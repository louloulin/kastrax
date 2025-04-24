package ai.kastrax.core.workflow.version

import ai.kastrax.core.workflow.visualization.StepConnection
import ai.kastrax.core.workflow.visualization.WorkflowStep
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class WorkflowDiffToolTest {
    private val diffTool = WorkflowDiffTool()

    @Test
    fun `test compare workflows with different IDs`() {
        // Create two workflows with different IDs
        val sourceWorkflow = createTestWorkflow("workflow1", "1.0.0")
        val targetWorkflow = createTestWorkflow("workflow2", "1.0.0")

        // Compare workflows
        val diff = diffTool.compareWorkflows(sourceWorkflow, targetWorkflow)

        // Verify the diff
        assertNotNull(diff)
        assertEquals(sourceWorkflow, diff.sourceWorkflow)
        assertEquals(targetWorkflow, diff.targetWorkflow)
        assertEquals(1, diff.differences.size)
        assertEquals(DifferenceType.DIFFERENT_WORKFLOW, diff.differences[0].type)
        assertTrue(diff.differences[0].description.contains("workflow1"))
        assertTrue(diff.differences[0].description.contains("workflow2"))
    }

    @Test
    fun `test compare workflows with different versions`() {
        // Create two workflows with the same ID but different versions
        val sourceWorkflow = createTestWorkflow("workflow1", "1.0.0")
        val targetWorkflow = createTestWorkflow("workflow1", "2.0.0")

        // Compare workflows
        val diff = diffTool.compareWorkflows(sourceWorkflow, targetWorkflow)

        // Verify the diff
        assertNotNull(diff)
        assertTrue(diff.differences.any { it.type == DifferenceType.VERSION_CHANGED })
        assertTrue(diff.differences.any { it.description.contains("1.0.0") && it.description.contains("2.0.0") })
    }

    @Test
    fun `test compare workflows with different names`() {
        // Create source workflow
        val sourceWorkflow = createTestWorkflow("workflow1", "1.0.0", name = "Source Workflow")

        // Create target workflow with different name
        val targetWorkflow = createTestWorkflow("workflow1", "1.0.0", name = "Target Workflow")

        // Compare workflows
        val diff = diffTool.compareWorkflows(sourceWorkflow, targetWorkflow)

        // Verify the diff
        assertNotNull(diff)
        assertTrue(diff.differences.any { it.type == DifferenceType.NAME_CHANGED })
        assertTrue(diff.differences.any { it.description.contains("Source Workflow") && it.description.contains("Target Workflow") })
    }

    @Test
    fun `test compare workflows with different descriptions`() {
        // Create source workflow
        val sourceWorkflow = createTestWorkflow("workflow1", "1.0.0", description = "Source description")

        // Create target workflow with different description
        val targetWorkflow = createTestWorkflow("workflow1", "1.0.0", description = "Target description")

        // Compare workflows
        val diff = diffTool.compareWorkflows(sourceWorkflow, targetWorkflow)

        // Verify the diff
        assertNotNull(diff)
        assertTrue(diff.differences.any { it.type == DifferenceType.DESCRIPTION_CHANGED })
    }

    @Test
    fun `test compare workflows with different metadata`() {
        // Create source workflow
        val sourceWorkflow = createTestWorkflow(
            "workflow1", "1.0.0",
            metadata = mapOf("key1" to "value1", "key2" to "value2")
        )

        // Create target workflow with different metadata
        val targetWorkflow = createTestWorkflow(
            "workflow1", "1.0.0",
            metadata = mapOf("key1" to "value1", "key3" to "value3")
        )

        // Compare workflows
        val diff = diffTool.compareWorkflows(sourceWorkflow, targetWorkflow)

        // Verify the diff
        assertNotNull(diff)
        assertTrue(diff.differences.any { it.type == DifferenceType.METADATA_MODIFIED || it.type == DifferenceType.METADATA_ADDED || it.type == DifferenceType.METADATA_REMOVED })
    }

    @Test
    fun `test compare workflows with different steps`() {
        // Create source workflow
        val sourceWorkflow = createTestWorkflow(
            "workflow1", "1.0.0",
            steps = listOf(
                WorkflowStep("step1", "Step 1", "type1", "Description 1"),
                WorkflowStep("step2", "Step 2", "type2", "Description 2")
            )
        )

        // Create target workflow with different steps
        val targetWorkflow = createTestWorkflow(
            "workflow1", "1.0.0",
            steps = listOf(
                WorkflowStep("step1", "Step 1", "type1", "Description 1"),
                WorkflowStep("step3", "Step 3", "type3", "Description 3")
            )
        )

        // Compare workflows
        val diff = diffTool.compareWorkflows(sourceWorkflow, targetWorkflow)

        // Verify the diff
        assertNotNull(diff)
        assertTrue(diff.differences.any { it.type == DifferenceType.STEPS_REMOVED })
        assertTrue(diff.differences.any { it.type == DifferenceType.STEPS_ADDED })
    }

    @Test
    fun `test compare workflows with modified steps`() {
        // Create source workflow
        val sourceWorkflow = createTestWorkflow(
            "workflow1", "1.0.0",
            steps = listOf(
                WorkflowStep("step1", "Step 1", "type1", "Description 1"),
                WorkflowStep("step2", "Step 2", "type2", "Description 2")
            )
        )

        // Create target workflow with modified step
        val targetWorkflow = createTestWorkflow(
            "workflow1", "1.0.0",
            steps = listOf(
                WorkflowStep("step1", "Step 1 Modified", "type1", "Description 1 Modified"),
                WorkflowStep("step2", "Step 2", "type2", "Description 2")
            )
        )

        // Compare workflows
        val diff = diffTool.compareWorkflows(sourceWorkflow, targetWorkflow)

        // Verify the diff
        assertNotNull(diff)
        assertTrue(diff.differences.any { it.type == DifferenceType.STEPS_MODIFIED })
    }

    @Test
    fun `test compare workflows with different connections`() {
        // Create source workflow
        val sourceWorkflow = createTestWorkflow(
            "workflow1", "1.0.0",
            connections = listOf(
                StepConnection("step1", "step2"),
                StepConnection("step2", "step3")
            )
        )

        // Create target workflow with different connections
        val targetWorkflow = createTestWorkflow(
            "workflow1", "1.0.0",
            connections = listOf(
                StepConnection("step1", "step2"),
                StepConnection("step1", "step3")
            )
        )

        // Compare workflows
        val diff = diffTool.compareWorkflows(sourceWorkflow, targetWorkflow)

        // Verify the diff
        assertNotNull(diff)
        assertTrue(diff.differences.any { it.type == DifferenceType.CONNECTIONS_REMOVED })
        assertTrue(diff.differences.any { it.type == DifferenceType.CONNECTIONS_ADDED })
    }

    @Test
    fun `test format diff`() {
        // Create source and target workflows
        val sourceWorkflow = createTestWorkflow(
            "workflow1", "1.0.0",
            name = "Source Workflow",
            steps = listOf(
                WorkflowStep("step1", "Step 1", "type1", "Description 1"),
                WorkflowStep("step2", "Step 2", "type2", "Description 2")
            ),
            connections = listOf(
                StepConnection("step1", "step2")
            )
        )

        val targetWorkflow = createTestWorkflow(
            "workflow1", "2.0.0",
            name = "Target Workflow",
            steps = listOf(
                WorkflowStep("step1", "Step 1 Modified", "type1", "Description 1 Modified"),
                WorkflowStep("step3", "Step 3", "type3", "Description 3")
            ),
            connections = listOf(
                StepConnection("step1", "step3")
            )
        )

        // Generate diff
        val diff = diffTool.compareWorkflows(sourceWorkflow, targetWorkflow)

        // Format diff
        val formattedDiff = diffTool.formatDiff(diff)

        // Verify the formatted diff
        assertNotNull(formattedDiff)
        assertTrue(formattedDiff.contains("Workflow Diff"))
        assertTrue(formattedDiff.contains("Source Version"))
        assertTrue(formattedDiff.contains("Target Version"))
    }

    // Helper method to create a test workflow
    private fun createTestWorkflow(
        id: String,
        version: String,
        name: String = "Test Workflow",
        description: String = "Test description",
        steps: List<WorkflowStep> = listOf(WorkflowStep("step1", "Step 1", "test", "Test step")),
        connections: List<StepConnection> = listOf(StepConnection("step1", "step1")),
        metadata: Map<String, Any> = mapOf("key" to "value")
    ): VersionedWorkflow {
        val workflowVersion = WorkflowVersion(
            workflowId = id,
            version = version,
            description = "Test version",
            createdAt = Instant.now(),
            createdBy = "test-user",
            isActive = true
        )

        return VersionedWorkflow(
            id = id,
            name = name,
            description = description,
            version = workflowVersion,
            steps = steps,
            connections = connections,
            metadata = metadata
        )
    }
}
