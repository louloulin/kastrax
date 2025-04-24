package ai.kastrax.core.workflow.version

import ai.kastrax.core.workflow.visualization.StepConnection
import ai.kastrax.core.workflow.visualization.WorkflowStep

/**
 * Represents a workflow with version information.
 *
 * @property id The ID of the workflow.
 * @property name The name of the workflow.
 * @property description A description of the workflow.
 * @property version The current version of the workflow.
 * @property steps The steps in the workflow.
 * @property connections The connections between steps.
 * @property metadata Additional metadata about the workflow.
 */
data class VersionedWorkflow(
    val id: String,
    val name: String,
    val description: String = "",
    val version: WorkflowVersion,
    val steps: List<WorkflowStep>,
    val connections: List<StepConnection>,
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * Creates a new version of this workflow.
     *
     * @param newVersion The new version number.
     * @param description The description of the new version.
     * @param createdBy The user who created the new version.
     * @param isActive Whether the new version is active.
     * @param steps The steps in the new version, or null to use the current steps.
     * @param connections The connections in the new version, or null to use the current connections.
     * @param metadata The metadata for the new version, or null to use the current metadata.
     * @return The new version of the workflow.
     */
    fun createNewVersion(
        newVersion: String,
        description: String = "",
        createdBy: String = "system",
        isActive: Boolean = false,
        steps: List<WorkflowStep>? = null,
        connections: List<StepConnection>? = null,
        metadata: Map<String, Any>? = null
    ): VersionedWorkflow {
        val newWorkflowVersion = WorkflowVersion.createNewVersion(
            existing = this.version,
            newVersion = newVersion,
            description = description,
            createdBy = createdBy,
            isActive = isActive
        )
        
        return VersionedWorkflow(
            id = this.id,
            name = this.name,
            description = this.description,
            version = newWorkflowVersion,
            steps = steps ?: this.steps,
            connections = connections ?: this.connections,
            metadata = metadata ?: this.metadata
        )
    }
    
    /**
     * Checks if this workflow is compatible with another workflow.
     *
     * @param other The other workflow to check compatibility with.
     * @return True if the workflows are compatible, false otherwise.
     */
    fun isCompatibleWith(other: VersionedWorkflow): Boolean {
        // Check if the workflow IDs match
        if (this.id != other.id) {
            return false
        }
        
        // Check if the versions are compatible
        if (!this.version.isCompatibleWith(other.version)) {
            return false
        }
        
        // In a real implementation, we would check for compatibility of steps and connections
        // For now, we'll just return true
        return true
    }
}
