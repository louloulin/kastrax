package ai.kastrax.core.workflow.version

import java.time.Instant

/**
 * Interface for storing and retrieving workflow versions.
 */
interface WorkflowVersionStorage {
    /**
     * Saves a workflow version.
     *
     * @param workflow The workflow version to save.
     * @return True if the version was saved successfully, false otherwise.
     */
    fun saveWorkflow(workflow: VersionedWorkflow): Boolean
    
    /**
     * Gets a workflow by ID and version.
     *
     * @param workflowId The ID of the workflow.
     * @param version The version of the workflow, or null to get the latest version.
     * @return The workflow, or null if not found.
     */
    fun getWorkflow(workflowId: String, version: String? = null): VersionedWorkflow?
    
    /**
     * Gets all versions of a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @param limit The maximum number of versions to return.
     * @param offset The offset to start from.
     * @return A list of workflow versions.
     */
    fun getWorkflowVersions(workflowId: String, limit: Int = 100, offset: Int = 0): List<WorkflowVersion>
    
    /**
     * Gets the active version of a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @return The active version of the workflow, or null if not found.
     */
    fun getActiveWorkflowVersion(workflowId: String): WorkflowVersion?
    
    /**
     * Sets the active version of a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @param version The version to set as active.
     * @return True if the version was set as active successfully, false otherwise.
     */
    fun setActiveWorkflowVersion(workflowId: String, version: String): Boolean
    
    /**
     * Gets all workflows.
     *
     * @param limit The maximum number of workflows to return.
     * @param offset The offset to start from.
     * @return A list of workflows.
     */
    fun getAllWorkflows(limit: Int = 100, offset: Int = 0): List<VersionedWorkflow>
    
    /**
     * Gets all workflows created after a specific time.
     *
     * @param time The time threshold.
     * @param limit The maximum number of workflows to return.
     * @param offset The offset to start from.
     * @return A list of workflows.
     */
    fun getWorkflowsCreatedAfter(time: Instant, limit: Int = 100, offset: Int = 0): List<VersionedWorkflow>
    
    /**
     * Gets all workflows with specific tags.
     *
     * @param tags The tags to filter by.
     * @param limit The maximum number of workflows to return.
     * @param offset The offset to start from.
     * @return A list of workflows.
     */
    fun getWorkflowsByTags(tags: Map<String, String>, limit: Int = 100, offset: Int = 0): List<VersionedWorkflow>
    
    /**
     * Deletes a workflow version.
     *
     * @param workflowId The ID of the workflow.
     * @param version The version to delete.
     * @return True if the version was deleted successfully, false otherwise.
     */
    fun deleteWorkflowVersion(workflowId: String, version: String): Boolean
    
    /**
     * Deletes all versions of a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @return The number of versions deleted.
     */
    fun deleteWorkflow(workflowId: String): Int
}
