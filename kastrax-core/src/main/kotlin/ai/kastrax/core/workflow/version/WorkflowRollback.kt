package ai.kastrax.core.workflow.version

/**
 * Manages workflow rollbacks.
 *
 * @property versionManager The workflow version manager.
 */
class WorkflowRollback(
    private val versionManager: WorkflowVersionManager
) {
    /**
     * Rolls back a workflow to a previous version.
     *
     * @param workflowId The ID of the workflow.
     * @param targetVersion The version to roll back to.
     * @param setActive Whether to set the rolled back version as the active version.
     * @return The rolled back workflow, or null if the workflow or version was not found.
     */
    fun rollbackToVersion(
        workflowId: String,
        targetVersion: String,
        setActive: Boolean = true
    ): VersionedWorkflow? {
        // Get the current workflow
        val currentWorkflow = versionManager.getWorkflow(workflowId) ?: return null
        
        // Get the target workflow version
        val targetWorkflow = versionManager.getWorkflow(workflowId, targetVersion) ?: return null
        
        // If the current version is already the target version, just return it
        if (currentWorkflow.version.version == targetVersion) {
            return currentWorkflow
        }
        
        // Set the target version as active if requested
        if (setActive) {
            versionManager.setActiveWorkflowVersion(workflowId, targetVersion)
        }
        
        return targetWorkflow
    }
    
    /**
     * Rolls back a workflow to the previous version.
     *
     * @param workflowId The ID of the workflow.
     * @param setActive Whether to set the rolled back version as the active version.
     * @return The rolled back workflow, or null if the workflow has no previous version.
     */
    fun rollbackToPreviousVersion(
        workflowId: String,
        setActive: Boolean = true
    ): VersionedWorkflow? {
        // Get all versions of the workflow
        val versions = versionManager.getWorkflowVersions(workflowId)
        
        // If there are less than 2 versions, we can't roll back
        if (versions.size < 2) {
            return null
        }
        
        // Get the current active version
        val activeVersion = versionManager.getActiveWorkflowVersion(workflowId)
        
        // If there's no active version, use the latest version
        val currentVersion = activeVersion ?: versions.first()
        
        // Find the previous version
        val previousVersion = versions.find { it.version != currentVersion.version }
            ?: return null
        
        // Roll back to the previous version
        return rollbackToVersion(workflowId, previousVersion.version, setActive)
    }
    
    /**
     * Creates a new version based on a previous version.
     *
     * @param workflowId The ID of the workflow.
     * @param sourceVersion The version to base the new version on.
     * @param newVersion The new version number, or null to auto-increment.
     * @param description A description of the new version.
     * @param createdBy The user who created the new version.
     * @param setActive Whether to set the new version as the active version.
     * @return The new version of the workflow, or null if the workflow or source version was not found.
     */
    fun createVersionFromPrevious(
        workflowId: String,
        sourceVersion: String,
        newVersion: String? = null,
        description: String = "Created from version $sourceVersion",
        createdBy: String = "system",
        setActive: Boolean = false
    ): VersionedWorkflow? {
        // Get the source workflow version
        val sourceWorkflow = versionManager.getWorkflow(workflowId, sourceVersion) ?: return null
        
        // Create a new version based on the source version
        return versionManager.createNewVersion(
            workflowId = workflowId,
            newVersion = newVersion,
            description = description,
            steps = sourceWorkflow.steps,
            connections = sourceWorkflow.connections,
            metadata = sourceWorkflow.metadata,
            createdBy = createdBy,
            setActive = setActive
        )
    }
}
