package ai.kastrax.core.workflow.version

import ai.kastrax.core.workflow.visualization.StepConnection
import ai.kastrax.core.workflow.visualization.WorkflowStep
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class WorkflowRollbackTest {
    private lateinit var versionManager: WorkflowVersionManager
    private lateinit var versionStorage: InMemoryWorkflowVersionStorage
    private lateinit var rollback: WorkflowRollback

    @BeforeEach
    fun setUp() {
        versionStorage = InMemoryWorkflowVersionStorage()
        versionManager = WorkflowVersionManager(versionStorage)
        rollback = WorkflowRollback(versionManager)
    }

    @Test
    fun `test rollback to previous version`() {
        // Create initial workflow
        val initialWorkflow = createTestWorkflow(versionManager, "1.0.0")

        // Create new versions
        val v110Workflow = versionManager.createNewVersion(
            workflowId = initialWorkflow.id,
            newVersion = "1.1.0",
            description = "Version 1.1.0",
            steps = initialWorkflow.steps.map {
                if (it.id == "step1") {
                    it.copy(name = "Modified Step 1")
                } else {
                    it
                }
            }
        )

        val v200Workflow = versionManager.createNewVersion(
            workflowId = initialWorkflow.id,
            newVersion = "2.0.0",
            description = "Version 2.0.0",
            steps = initialWorkflow.steps + WorkflowStep(
                id = "step3",
                name = "Step 3",
                type = "test",
                description = "New step"
            ),
            setActive = true
        )

        // Verify active version is 2.0.0
        val activeVersion = versionManager.getActiveWorkflowVersion(initialWorkflow.id)
        assertEquals("2.0.0", activeVersion!!.version)

        // Rollback to version 1.1.0
        val rolledBackWorkflow = rollback.rollbackToVersion(
            workflowId = initialWorkflow.id,
            targetVersion = "1.1.0",
            setActive = true
        )

        // Verify rollback
        assertNotNull(rolledBackWorkflow)
        assertEquals("1.1.0", rolledBackWorkflow!!.version.version)

        // Verify active version is now 1.1.0
        val newActiveVersion = versionManager.getActiveWorkflowVersion(initialWorkflow.id)
        assertEquals("1.1.0", newActiveVersion!!.version)

        // Verify the workflow content
        assertEquals(2, rolledBackWorkflow.steps.size)
        val modifiedStep = rolledBackWorkflow.steps.find { it.id == "step1" }
        assertNotNull(modifiedStep)
        assertEquals("Modified Step 1", modifiedStep!!.name)
    }

    @Test
    fun `test rollback to non-existent version`() {
        // Create initial workflow
        val initialWorkflow = createTestWorkflow(versionManager, "1.0.0")

        // Attempt to rollback to non-existent version
        val rolledBackWorkflow = rollback.rollbackToVersion(
            workflowId = initialWorkflow.id,
            targetVersion = "2.0.0",
            setActive = true
        )

        // Verify rollback failed
        assertNull(rolledBackWorkflow)

        // Verify active version is still 1.0.0
        val activeVersion = versionManager.getActiveWorkflowVersion(initialWorkflow.id)
        assertEquals("1.0.0", activeVersion!!.version)
    }

    @Test
    fun `test rollback to previous version automatically`() {
        // Create initial workflow
        val initialWorkflow = createTestWorkflow(versionManager, "1.0.0")

        // Create new version
        val v110Workflow = versionManager.createNewVersion(
            workflowId = initialWorkflow.id,
            newVersion = "1.1.0",
            description = "Version 1.1.0",
            setActive = true
        )

        // Verify active version is 1.1.0
        val activeVersion = versionManager.getActiveWorkflowVersion(initialWorkflow.id)
        assertEquals("1.1.0", activeVersion!!.version)

        // Rollback to previous version
        val rolledBackWorkflow = rollback.rollbackToPreviousVersion(
            workflowId = initialWorkflow.id,
            setActive = true
        )

        // Verify rollback
        assertNotNull(rolledBackWorkflow)
        assertEquals("1.0.0", rolledBackWorkflow!!.version.version)

        // Verify active version is now 1.0.0
        val newActiveVersion = versionManager.getActiveWorkflowVersion(initialWorkflow.id)
        assertEquals("1.0.0", newActiveVersion!!.version)
    }

    @Test
    fun `test create version from previous`() {
        // Create initial workflow
        val initialWorkflow = createTestWorkflow(versionManager, "1.0.0")

        // Create new version
        val v110Workflow = versionManager.createNewVersion(
            workflowId = initialWorkflow.id,
            newVersion = "1.1.0",
            description = "Version 1.1.0",
            steps = initialWorkflow.steps.map {
                if (it.id == "step1") {
                    it.copy(name = "Modified Step 1")
                } else {
                    it
                }
            }
        )

        val v200Workflow = versionManager.createNewVersion(
            workflowId = initialWorkflow.id,
            newVersion = "2.0.0",
            description = "Version 2.0.0",
            steps = initialWorkflow.steps + WorkflowStep(
                id = "step3",
                name = "Step 3",
                type = "test",
                description = "New step"
            ),
            setActive = true
        )

        // Create new version based on 1.0.0
        val newVersionFromPrevious = rollback.createVersionFromPrevious(
            workflowId = initialWorkflow.id,
            sourceVersion = "1.0.0",
            newVersion = "1.0.1",
            description = "New version based on 1.0.0",
            createdBy = "test-user",
            setActive = true
        )

        // Verify new version
        assertNotNull(newVersionFromPrevious)
        assertEquals("1.0.1", newVersionFromPrevious!!.version.version)
        assertEquals("New version based on 1.0.0", newVersionFromPrevious.version.description)
        assertEquals("test-user", newVersionFromPrevious.version.createdBy)

        // Verify active version is now 1.0.1
        val activeVersion = versionManager.getActiveWorkflowVersion(initialWorkflow.id)
        assertEquals("1.0.1", activeVersion!!.version)

        // Verify the workflow content is based on 1.0.0
        assertEquals(2, newVersionFromPrevious.steps.size)
        val step1 = newVersionFromPrevious.steps.find { it.id == "step1" }
        assertNotNull(step1)
        assertEquals("Step 1", step1!!.name) // Not modified
    }

    @Test
    fun `test create version from non-existent previous version`() {
        // Create initial workflow
        val initialWorkflow = createTestWorkflow(versionManager, "1.0.0")

        // Attempt to create new version based on non-existent version
        val newVersionFromPrevious = rollback.createVersionFromPrevious(
            workflowId = initialWorkflow.id,
            sourceVersion = "2.0.0",
            newVersion = "2.0.1",
            description = "New version based on non-existent version",
            createdBy = "test-user",
            setActive = true
        )

        // Verify creation failed
        assertNull(newVersionFromPrevious)

        // Verify active version is still 1.0.0
        val activeVersion = versionManager.getActiveWorkflowVersion(initialWorkflow.id)
        assertEquals("1.0.0", activeVersion!!.version)
    }

    // Helper method to create a test workflow
    private fun createTestWorkflow(versionManager: WorkflowVersionManager, version: String): VersionedWorkflow {
        return versionManager.createWorkflow(
            name = "Test Workflow",
            description = "Test workflow description",
            steps = listOf(
                WorkflowStep(
                    id = "step1",
                    name = "Step 1",
                    type = "test",
                    description = "Test step 1"
                ),
                WorkflowStep(
                    id = "step2",
                    name = "Step 2",
                    type = "test",
                    description = "Test step 2"
                )
            ),
            connections = listOf(
                StepConnection(
                    sourceId = "step1",
                    targetId = "step2"
                )
            ),
            metadata = mapOf("key" to "value")
        )
    }
}
