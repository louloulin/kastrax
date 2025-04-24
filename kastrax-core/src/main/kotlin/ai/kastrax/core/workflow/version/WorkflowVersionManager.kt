package ai.kastrax.core.workflow.version

import ai.kastrax.core.workflow.visualization.StepConnection
import ai.kastrax.core.workflow.visualization.WorkflowStep
import java.time.Instant
import java.util.UUID

/**
 * Manages workflow versions.
 *
 * @property versionStorage The storage for workflow versions.
 */
class WorkflowVersionManager(
    private val versionStorage: WorkflowVersionStorage
) {
    /**
     * Creates a new workflow.
     *
     * @param name The name of the workflow.
     * @param description A description of the workflow.
     * @param steps The steps in the workflow.
     * @param connections The connections between steps.
     * @param metadata Additional metadata about the workflow.
     * @param createdBy The user who created the workflow.
     * @param tags Optional tags for the workflow.
     * @return The created workflow.
     */
    fun createWorkflow(
        name: String,
        description: String = "",
        steps: List<WorkflowStep>,
        connections: List<StepConnection>,
        metadata: Map<String, Any> = emptyMap(),
        createdBy: String = "system",
        tags: Map<String, String> = emptyMap()
    ): VersionedWorkflow {
        val workflowId = UUID.randomUUID().toString()
        val initialVersion = "1.0.0"
        
        val workflowVersion = WorkflowVersion(
            workflowId = workflowId,
            version = initialVersion,
            description = "Initial version",
            createdAt = Instant.now(),
            createdBy = createdBy,
            isActive = true,
            tags = tags
        )
        
        val workflow = VersionedWorkflow(
            id = workflowId,
            name = name,
            description = description,
            version = workflowVersion,
            steps = steps,
            connections = connections,
            metadata = metadata
        )
        
        versionStorage.saveWorkflow(workflow)
        return workflow
    }
    
    /**
     * Creates a new version of a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @param newVersion The new version number, or null to auto-increment.
     * @param description A description of the new version.
     * @param steps The steps in the new version, or null to use the current steps.
     * @param connections The connections in the new version, or null to use the current connections.
     * @param metadata The metadata for the new version, or null to use the current metadata.
     * @param createdBy The user who created the new version.
     * @param setActive Whether to set the new version as the active version.
     * @param tags Optional tags for the new version.
     * @param incrementMajor Whether to increment the major version when auto-incrementing.
     * @param incrementMinor Whether to increment the minor version when auto-incrementing.
     * @return The new version of the workflow, or null if the workflow was not found.
     */
    fun createNewVersion(
        workflowId: String,
        newVersion: String? = null,
        description: String = "",
        steps: List<WorkflowStep>? = null,
        connections: List<StepConnection>? = null,
        metadata: Map<String, Any>? = null,
        createdBy: String = "system",
        setActive: Boolean = false,
        tags: Map<String, String> = emptyMap(),
        incrementMajor: Boolean = false,
        incrementMinor: Boolean = false
    ): VersionedWorkflow? {
        val currentWorkflow = versionStorage.getWorkflow(workflowId) ?: return null
        
        val version = if (newVersion != null) {
            newVersion
        } else {
            WorkflowVersion.incrementVersion(
                currentWorkflow.version.version,
                incrementMajor = incrementMajor,
                incrementMinor = incrementMinor,
                incrementPatch = !(incrementMajor || incrementMinor)
            )
        }
        
        val newWorkflow = currentWorkflow.createNewVersion(
            newVersion = version,
            description = description,
            createdBy = createdBy,
            isActive = setActive,
            steps = steps,
            connections = connections,
            metadata = metadata
        )
        
        versionStorage.saveWorkflow(newWorkflow)
        
        if (setActive) {
            versionStorage.setActiveWorkflowVersion(workflowId, version)
        }
        
        return newWorkflow
    }
    
    /**
     * Gets a workflow by ID and version.
     *
     * @param workflowId The ID of the workflow.
     * @param version The version of the workflow, or null to get the latest version.
     * @return The workflow, or null if not found.
     */
    fun getWorkflow(workflowId: String, version: String? = null): VersionedWorkflow? {
        return versionStorage.getWorkflow(workflowId, version)
    }
    
    /**
     * Gets all versions of a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @param limit The maximum number of versions to return.
     * @param offset The offset to start from.
     * @return A list of workflow versions.
     */
    fun getWorkflowVersions(workflowId: String, limit: Int = 100, offset: Int = 0): List<WorkflowVersion> {
        return versionStorage.getWorkflowVersions(workflowId, limit, offset)
    }
    
    /**
     * Gets the active version of a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @return The active version of the workflow, or null if not found.
     */
    fun getActiveWorkflowVersion(workflowId: String): WorkflowVersion? {
        return versionStorage.getActiveWorkflowVersion(workflowId)
    }
    
    /**
     * Sets the active version of a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @param version The version to set as active.
     * @return True if the version was set as active successfully, false otherwise.
     */
    fun setActiveWorkflowVersion(workflowId: String, version: String): Boolean {
        return versionStorage.setActiveWorkflowVersion(workflowId, version)
    }
    
    /**
     * Gets all workflows.
     *
     * @param limit The maximum number of workflows to return.
     * @param offset The offset to start from.
     * @return A list of workflows.
     */
    fun getAllWorkflows(limit: Int = 100, offset: Int = 0): List<VersionedWorkflow> {
        return versionStorage.getAllWorkflows(limit, offset)
    }
    
    /**
     * Deletes a workflow version.
     *
     * @param workflowId The ID of the workflow.
     * @param version The version to delete.
     * @return True if the version was deleted successfully, false otherwise.
     */
    fun deleteWorkflowVersion(workflowId: String, version: String): Boolean {
        return versionStorage.deleteWorkflowVersion(workflowId, version)
    }
    
    /**
     * Deletes all versions of a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @return The number of versions deleted.
     */
    fun deleteWorkflow(workflowId: String): Int {
        return versionStorage.deleteWorkflow(workflowId)
    }
}
