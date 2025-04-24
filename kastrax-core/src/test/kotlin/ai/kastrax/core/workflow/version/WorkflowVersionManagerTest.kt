package ai.kastrax.core.workflow.version

import ai.kastrax.core.workflow.visualization.StepConnection
import ai.kastrax.core.workflow.visualization.WorkflowStep
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WorkflowVersionManagerTest {
    private lateinit var versionStorage: InMemoryWorkflowVersionStorage
    private lateinit var versionManager: WorkflowVersionManager
    
    @BeforeEach
    fun setUp() {
        versionStorage = InMemoryWorkflowVersionStorage()
        versionManager = WorkflowVersionManager(versionStorage)
    }
    
    @Test
    fun `test create workflow`() {
        val steps = listOf(
            WorkflowStep(
                id = "step1",
                name = "Step 1",
                type = "test",
                description = "Test step"
            )
        )
        
        val connections = listOf(
            StepConnection(
                sourceId = "step1",
                targetId = "step1"
            )
        )
        
        val workflow = versionManager.createWorkflow(
            name = "Test Workflow",
            description = "Test workflow description",
            steps = steps,
            connections = connections,
            metadata = mapOf("key" to "value"),
            createdBy = "test-user",
            tags = mapOf("tag" to "value")
        )
        
        assertNotNull(workflow)
        assertEquals("Test Workflow", workflow.name)
        assertEquals("Test workflow description", workflow.description)
        assertEquals(steps, workflow.steps)
        assertEquals(connections, workflow.connections)
        assertEquals(mapOf("key" to "value"), workflow.metadata)
        assertEquals("1.0.0", workflow.version.version)
        assertEquals("test-user", workflow.version.createdBy)
        assertEquals(mapOf("tag" to "value"), workflow.version.tags)
        assertTrue(workflow.version.isActive)
        
        // Verify the workflow was saved to storage
        val savedWorkflow = versionStorage.getWorkflow(workflow.id)
        assertNotNull(savedWorkflow)
        assertEquals(workflow, savedWorkflow)
    }
    
    @Test
    fun `test create new version`() {
        // Create initial workflow
        val workflow = versionManager.createWorkflow(
            name = "Test Workflow",
            description = "Test workflow description",
            steps = listOf(
                WorkflowStep(
                    id = "step1",
                    name = "Step 1",
                    type = "test",
                    description = "Test step"
                )
            ),
            connections = listOf(
                StepConnection(
                    sourceId = "step1",
                    targetId = "step1"
                )
            )
        )
        
        // Create new version
        val newSteps = listOf(
            WorkflowStep(
                id = "step1",
                name = "Step 1 Updated",
                type = "test",
                description = "Updated test step"
            ),
            WorkflowStep(
                id = "step2",
                name = "Step 2",
                type = "test",
                description = "New test step"
            )
        )
        
        val newConnections = listOf(
            StepConnection(
                sourceId = "step1",
                targetId = "step2"
            )
        )
        
        val newWorkflow = versionManager.createNewVersion(
            workflowId = workflow.id,
            newVersion = "2.0.0",
            description = "Updated version",
            steps = newSteps,
            connections = newConnections,
            metadata = mapOf("key" to "updated value"),
            createdBy = "test-user",
            setActive = true,
            tags = mapOf("tag" to "updated value")
        )
        
        assertNotNull(newWorkflow)
        assertEquals(workflow.id, newWorkflow!!.id)
        assertEquals("Test Workflow", newWorkflow.name)
        assertEquals("Test workflow description", newWorkflow.description)
        assertEquals(newSteps, newWorkflow.steps)
        assertEquals(newConnections, newWorkflow.connections)
        assertEquals(mapOf("key" to "updated value"), newWorkflow.metadata)
        assertEquals("2.0.0", newWorkflow.version.version)
        assertEquals("test-user", newWorkflow.version.createdBy)
        assertEquals(mapOf("tag" to "updated value"), newWorkflow.version.tags)
        assertTrue(newWorkflow.version.isActive)
        
        // Verify both versions are saved to storage
        val versions = versionStorage.getWorkflowVersions(workflow.id)
        assertEquals(2, versions.size)
        assertEquals("2.0.0", versions[0].version)
        assertEquals("1.0.0", versions[1].version)
        
        // Verify the active version is set correctly
        val activeVersion = versionStorage.getActiveWorkflowVersion(workflow.id)
        assertNotNull(activeVersion)
        assertEquals("2.0.0", activeVersion!!.version)
    }
    
    @Test
    fun `test auto-increment version`() {
        // Create initial workflow
        val workflow = versionManager.createWorkflow(
            name = "Test Workflow",
            steps = listOf(
                WorkflowStep(
                    id = "step1",
                    name = "Step 1",
                    type = "test"
                )
            ),
            connections = emptyList()
        )
        
        // Create new version with auto-incremented patch version
        val patchWorkflow = versionManager.createNewVersion(
            workflowId = workflow.id,
            description = "Patch update"
        )
        
        assertNotNull(patchWorkflow)
        assertEquals("1.0.1", patchWorkflow!!.version.version)
        
        // Create new version with auto-incremented minor version
        val minorWorkflow = versionManager.createNewVersion(
            workflowId = workflow.id,
            description = "Minor update",
            incrementMinor = true
        )
        
        assertNotNull(minorWorkflow)
        assertEquals("1.1.0", minorWorkflow!!.version.version)
        
        // Create new version with auto-incremented major version
        val majorWorkflow = versionManager.createNewVersion(
            workflowId = workflow.id,
            description = "Major update",
            incrementMajor = true
        )
        
        assertNotNull(majorWorkflow)
        assertEquals("2.0.0", majorWorkflow!!.version.version)
    }
    
    @Test
    fun `test get workflow versions`() {
        // Create initial workflow
        val workflow = versionManager.createWorkflow(
            name = "Test Workflow",
            steps = listOf(
                WorkflowStep(
                    id = "step1",
                    name = "Step 1",
                    type = "test"
                )
            ),
            connections = emptyList()
        )
        
        // Create multiple versions
        versionManager.createNewVersion(
            workflowId = workflow.id,
            newVersion = "1.1.0",
            description = "Version 1.1.0"
        )
        
        versionManager.createNewVersion(
            workflowId = workflow.id,
            newVersion = "1.2.0",
            description = "Version 1.2.0"
        )
        
        versionManager.createNewVersion(
            workflowId = workflow.id,
            newVersion = "2.0.0",
            description = "Version 2.0.0"
        )
        
        // Get all versions
        val versions = versionManager.getWorkflowVersions(workflow.id)
        assertEquals(4, versions.size)
        assertEquals("2.0.0", versions[0].version)
        assertEquals("1.2.0", versions[1].version)
        assertEquals("1.1.0", versions[2].version)
        assertEquals("1.0.0", versions[3].version)
        
        // Get limited versions
        val limitedVersions = versionManager.getWorkflowVersions(workflow.id, limit = 2)
        assertEquals(2, limitedVersions.size)
        assertEquals("2.0.0", limitedVersions[0].version)
        assertEquals("1.2.0", limitedVersions[1].version)
        
        // Get versions with offset
        val offsetVersions = versionManager.getWorkflowVersions(workflow.id, offset = 2)
        assertEquals(2, offsetVersions.size)
        assertEquals("1.1.0", offsetVersions[0].version)
        assertEquals("1.0.0", offsetVersions[1].version)
    }
    
    @Test
    fun `test set active version`() {
        // Create initial workflow
        val workflow = versionManager.createWorkflow(
            name = "Test Workflow",
            steps = listOf(
                WorkflowStep(
                    id = "step1",
                    name = "Step 1",
                    type = "test"
                )
            ),
            connections = emptyList()
        )
        
        // Create new version
        versionManager.createNewVersion(
            workflowId = workflow.id,
            newVersion = "2.0.0",
            description = "Version 2.0.0"
        )
        
        // Set active version
        val result = versionManager.setActiveWorkflowVersion(workflow.id, "2.0.0")
        assertTrue(result)
        
        // Verify the active version is set correctly
        val activeVersion = versionManager.getActiveWorkflowVersion(workflow.id)
        assertNotNull(activeVersion)
        assertEquals("2.0.0", activeVersion!!.version)
        
        // Get the workflow with no version specified (should return the active version)
        val activeWorkflow = versionManager.getWorkflow(workflow.id)
        assertNotNull(activeWorkflow)
        assertEquals("2.0.0", activeWorkflow!!.version.version)
    }
    
    @Test
    fun `test delete workflow version`() {
        // Create initial workflow
        val workflow = versionManager.createWorkflow(
            name = "Test Workflow",
            steps = listOf(
                WorkflowStep(
                    id = "step1",
                    name = "Step 1",
                    type = "test"
                )
            ),
            connections = emptyList()
        )
        
        // Create new version
        versionManager.createNewVersion(
            workflowId = workflow.id,
            newVersion = "2.0.0",
            description = "Version 2.0.0"
        )
        
        // Delete the initial version
        val result = versionManager.deleteWorkflowVersion(workflow.id, "1.0.0")
        assertTrue(result)
        
        // Verify the version was deleted
        val versions = versionManager.getWorkflowVersions(workflow.id)
        assertEquals(1, versions.size)
        assertEquals("2.0.0", versions[0].version)
        
        // Verify the active version was updated
        val activeVersion = versionManager.getActiveWorkflowVersion(workflow.id)
        assertNotNull(activeVersion)
        assertEquals("2.0.0", activeVersion!!.version)
    }
    
    @Test
    fun `test delete workflow`() {
        // Create initial workflow
        val workflow = versionManager.createWorkflow(
            name = "Test Workflow",
            steps = listOf(
                WorkflowStep(
                    id = "step1",
                    name = "Step 1",
                    type = "test"
                )
            ),
            connections = emptyList()
        )
        
        // Create new version
        versionManager.createNewVersion(
            workflowId = workflow.id,
            newVersion = "2.0.0",
            description = "Version 2.0.0"
        )
        
        // Delete the workflow
        val count = versionManager.deleteWorkflow(workflow.id)
        assertEquals(2, count)
        
        // Verify the workflow was deleted
        val deletedWorkflow = versionManager.getWorkflow(workflow.id)
        assertEquals(null, deletedWorkflow)
        
        // Verify no versions exist
        val versions = versionManager.getWorkflowVersions(workflow.id)
        assertEquals(0, versions.size)
    }
}
