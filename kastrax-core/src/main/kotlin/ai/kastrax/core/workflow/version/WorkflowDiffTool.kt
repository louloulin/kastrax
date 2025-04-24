package ai.kastrax.core.workflow.version

import ai.kastrax.core.workflow.visualization.StepConnection
import ai.kastrax.core.workflow.visualization.WorkflowStep

/**
 * Tool for comparing workflow versions and generating diffs.
 */
class WorkflowDiffTool {
    /**
     * Compares two workflow versions and generates a diff.
     *
     * @param source The source workflow.
     * @param target The target workflow.
     * @return A workflow diff.
     */
    fun compareWorkflows(source: VersionedWorkflow, target: VersionedWorkflow): WorkflowDiff {
        // Check if the workflows have the same ID
        if (source.id != target.id) {
            return WorkflowDiff(
                sourceWorkflow = source,
                targetWorkflow = target,
                differences = listOf(WorkflowDifference(
                    type = DifferenceType.DIFFERENT_WORKFLOW,
                    description = "Workflows have different IDs: ${source.id} vs ${target.id}"
                ))
            )
        }
        
        val differences = mutableListOf<WorkflowDifference>()
        
        // Compare versions
        if (source.version.version != target.version.version) {
            differences.add(WorkflowDifference(
                type = DifferenceType.VERSION_CHANGED,
                description = "Version changed from ${source.version.version} to ${target.version.version}"
            ))
        }
        
        // Compare names
        if (source.name != target.name) {
            differences.add(WorkflowDifference(
                type = DifferenceType.NAME_CHANGED,
                description = "Name changed from '${source.name}' to '${target.name}'"
            ))
        }
        
        // Compare descriptions
        if (source.description != target.description) {
            differences.add(WorkflowDifference(
                type = DifferenceType.DESCRIPTION_CHANGED,
                description = "Description changed"
            ))
        }
        
        // Compare steps
        compareSteps(source.steps, target.steps, differences)
        
        // Compare connections
        compareConnections(source.connections, target.connections, differences)
        
        // Compare metadata
        compareMetadata(source.metadata, target.metadata, differences)
        
        return WorkflowDiff(
            sourceWorkflow = source,
            targetWorkflow = target,
            differences = differences
        )
    }
    
    /**
     * Compares steps between two workflow versions.
     *
     * @param sourceSteps The steps in the source workflow.
     * @param targetSteps The steps in the target workflow.
     * @param differences The list to add differences to.
     */
    private fun compareSteps(
        sourceSteps: List<WorkflowStep>,
        targetSteps: List<WorkflowStep>,
        differences: MutableList<WorkflowDifference>
    ) {
        // Find added steps
        val addedSteps = targetSteps.filter { targetStep ->
            sourceSteps.none { it.id == targetStep.id }
        }
        if (addedSteps.isNotEmpty()) {
            differences.add(WorkflowDifference(
                type = DifferenceType.STEPS_ADDED,
                description = "Steps added: ${addedSteps.map { it.id }}",
                details = addedSteps.map { StepDiff(it.id, it.name, it.type, DiffAction.ADDED) }
            ))
        }
        
        // Find removed steps
        val removedSteps = sourceSteps.filter { sourceStep ->
            targetSteps.none { it.id == sourceStep.id }
        }
        if (removedSteps.isNotEmpty()) {
            differences.add(WorkflowDifference(
                type = DifferenceType.STEPS_REMOVED,
                description = "Steps removed: ${removedSteps.map { it.id }}",
                details = removedSteps.map { StepDiff(it.id, it.name, it.type, DiffAction.REMOVED) }
            ))
        }
        
        // Find modified steps
        val modifiedSteps = mutableListOf<StepDiff>()
        for (sourceStep in sourceSteps) {
            val targetStep = targetSteps.find { it.id == sourceStep.id } ?: continue
            
            val changes = mutableListOf<String>()
            
            if (sourceStep.name != targetStep.name) {
                changes.add("name changed from '${sourceStep.name}' to '${targetStep.name}'")
            }
            
            if (sourceStep.type != targetStep.type) {
                changes.add("type changed from '${sourceStep.type}' to '${targetStep.type}'")
            }
            
            if (sourceStep.description != targetStep.description) {
                changes.add("description changed")
            }
            
            if (changes.isNotEmpty()) {
                modifiedSteps.add(StepDiff(
                    id = sourceStep.id,
                    name = targetStep.name,
                    type = targetStep.type,
                    action = DiffAction.MODIFIED,
                    changes = changes
                ))
            }
        }
        
        if (modifiedSteps.isNotEmpty()) {
            differences.add(WorkflowDifference(
                type = DifferenceType.STEPS_MODIFIED,
                description = "Steps modified: ${modifiedSteps.map { it.id }}",
                details = modifiedSteps
            ))
        }
    }
    
    /**
     * Compares connections between two workflow versions.
     *
     * @param sourceConnections The connections in the source workflow.
     * @param targetConnections The connections in the target workflow.
     * @param differences The list to add differences to.
     */
    private fun compareConnections(
        sourceConnections: List<StepConnection>,
        targetConnections: List<StepConnection>,
        differences: MutableList<WorkflowDifference>
    ) {
        // Find added connections
        val addedConnections = targetConnections.filter { targetConn ->
            sourceConnections.none { it.sourceId == targetConn.sourceId && it.targetId == targetConn.targetId }
        }
        if (addedConnections.isNotEmpty()) {
            differences.add(WorkflowDifference(
                type = DifferenceType.CONNECTIONS_ADDED,
                description = "Connections added: ${addedConnections.map { "${it.sourceId} -> ${it.targetId}" }}",
                details = addedConnections.map { 
                    ConnectionDiff(it.sourceId, it.targetId, it.condition, DiffAction.ADDED) 
                }
            ))
        }
        
        // Find removed connections
        val removedConnections = sourceConnections.filter { sourceConn ->
            targetConnections.none { it.sourceId == sourceConn.sourceId && it.targetId == sourceConn.targetId }
        }
        if (removedConnections.isNotEmpty()) {
            differences.add(WorkflowDifference(
                type = DifferenceType.CONNECTIONS_REMOVED,
                description = "Connections removed: ${removedConnections.map { "${it.sourceId} -> ${it.targetId}" }}",
                details = removedConnections.map { 
                    ConnectionDiff(it.sourceId, it.targetId, it.condition, DiffAction.REMOVED) 
                }
            ))
        }
        
        // Find modified connections (changed conditions)
        val modifiedConnections = mutableListOf<ConnectionDiff>()
        for (sourceConn in sourceConnections) {
            val targetConn = targetConnections.find { 
                it.sourceId == sourceConn.sourceId && it.targetId == sourceConn.targetId 
            } ?: continue
            
            if (sourceConn.condition != targetConn.condition) {
                modifiedConnections.add(ConnectionDiff(
                    sourceId = sourceConn.sourceId,
                    targetId = sourceConn.targetId,
                    condition = targetConn.condition,
                    action = DiffAction.MODIFIED,
                    changes = listOf("condition changed from '${sourceConn.condition}' to '${targetConn.condition}'")
                ))
            }
        }
        
        if (modifiedConnections.isNotEmpty()) {
            differences.add(WorkflowDifference(
                type = DifferenceType.CONNECTIONS_MODIFIED,
                description = "Connection conditions modified: ${modifiedConnections.map { "${it.sourceId} -> ${it.targetId}" }}",
                details = modifiedConnections
            ))
        }
    }
    
    /**
     * Compares metadata between two workflow versions.
     *
     * @param sourceMetadata The metadata in the source workflow.
     * @param targetMetadata The metadata in the target workflow.
     * @param differences The list to add differences to.
     */
    private fun compareMetadata(
        sourceMetadata: Map<String, Any>,
        targetMetadata: Map<String, Any>,
        differences: MutableList<WorkflowDifference>
    ) {
        // Find added metadata
        val addedMetadata = targetMetadata.keys.filter { !sourceMetadata.containsKey(it) }
        if (addedMetadata.isNotEmpty()) {
            differences.add(WorkflowDifference(
                type = DifferenceType.METADATA_ADDED,
                description = "Metadata added: $addedMetadata",
                details = addedMetadata.map { MetadataDiff(it, targetMetadata[it], DiffAction.ADDED) }
            ))
        }
        
        // Find removed metadata
        val removedMetadata = sourceMetadata.keys.filter { !targetMetadata.containsKey(it) }
        if (removedMetadata.isNotEmpty()) {
            differences.add(WorkflowDifference(
                type = DifferenceType.METADATA_REMOVED,
                description = "Metadata removed: $removedMetadata",
                details = removedMetadata.map { MetadataDiff(it, sourceMetadata[it], DiffAction.REMOVED) }
            ))
        }
        
        // Find modified metadata
        val modifiedMetadata = mutableListOf<MetadataDiff>()
        for (key in sourceMetadata.keys.intersect(targetMetadata.keys)) {
            if (sourceMetadata[key] != targetMetadata[key]) {
                modifiedMetadata.add(MetadataDiff(
                    key = key,
                    value = targetMetadata[key],
                    action = DiffAction.MODIFIED,
                    changes = listOf("value changed from '${sourceMetadata[key]}' to '${targetMetadata[key]}'")
                ))
            }
        }
        
        if (modifiedMetadata.isNotEmpty()) {
            differences.add(WorkflowDifference(
                type = DifferenceType.METADATA_MODIFIED,
                description = "Metadata modified: ${modifiedMetadata.map { it.key }}",
                details = modifiedMetadata
            ))
        }
    }
    
    /**
     * Generates a textual representation of a workflow diff.
     *
     * @param diff The workflow diff.
     * @return A string representation of the diff.
     */
    fun formatDiff(diff: WorkflowDiff): String {
        val sb = StringBuilder()
        
        sb.appendLine("Workflow Diff: ${diff.sourceWorkflow.id}")
        sb.appendLine("Source Version: ${diff.sourceWorkflow.version.version}")
        sb.appendLine("Target Version: ${diff.targetWorkflow.version.version}")
        sb.appendLine()
        
        if (diff.differences.isEmpty()) {
            sb.appendLine("No differences found.")
            return sb.toString()
        }
        
        for (difference in diff.differences) {
            sb.appendLine("${difference.type}: ${difference.description}")
            
            when (difference.type) {
                DifferenceType.STEPS_ADDED,
                DifferenceType.STEPS_REMOVED,
                DifferenceType.STEPS_MODIFIED -> {
                    val stepDiffs = difference.details as? List<StepDiff> ?: continue
                    for (stepDiff in stepDiffs) {
                        sb.appendLine("  - ${stepDiff.id} (${stepDiff.name}, ${stepDiff.type}): ${stepDiff.action}")
                        for (change in stepDiff.changes) {
                            sb.appendLine("    * $change")
                        }
                    }
                }
                
                DifferenceType.CONNECTIONS_ADDED,
                DifferenceType.CONNECTIONS_REMOVED,
                DifferenceType.CONNECTIONS_MODIFIED -> {
                    val connectionDiffs = difference.details as? List<ConnectionDiff> ?: continue
                    for (connectionDiff in connectionDiffs) {
                        sb.appendLine("  - ${connectionDiff.sourceId} -> ${connectionDiff.targetId}: ${connectionDiff.action}")
                        if (connectionDiff.condition != null) {
                            sb.appendLine("    * Condition: ${connectionDiff.condition}")
                        }
                        for (change in connectionDiff.changes) {
                            sb.appendLine("    * $change")
                        }
                    }
                }
                
                DifferenceType.METADATA_ADDED,
                DifferenceType.METADATA_REMOVED,
                DifferenceType.METADATA_MODIFIED -> {
                    val metadataDiffs = difference.details as? List<MetadataDiff> ?: continue
                    for (metadataDiff in metadataDiffs) {
                        sb.appendLine("  - ${metadataDiff.key}: ${metadataDiff.action}")
                        sb.appendLine("    * Value: ${metadataDiff.value}")
                        for (change in metadataDiff.changes) {
                            sb.appendLine("    * $change")
                        }
                    }
                }
                
                else -> {
                    // No additional details for other difference types
                }
            }
            
            sb.appendLine()
        }
        
        return sb.toString()
    }
}

/**
 * Represents a diff between two workflow versions.
 *
 * @property sourceWorkflow The source workflow.
 * @property targetWorkflow The target workflow.
 * @property differences The differences between the workflows.
 */
data class WorkflowDiff(
    val sourceWorkflow: VersionedWorkflow,
    val targetWorkflow: VersionedWorkflow,
    val differences: List<WorkflowDifference>
)

/**
 * Represents a difference between two workflow versions.
 *
 * @property type The type of the difference.
 * @property description A description of the difference.
 * @property details Additional details about the difference.
 */
data class WorkflowDifference(
    val type: DifferenceType,
    val description: String,
    val details: List<Any> = emptyList()
)

/**
 * Represents the type of a workflow difference.
 */
enum class DifferenceType {
    DIFFERENT_WORKFLOW,
    VERSION_CHANGED,
    NAME_CHANGED,
    DESCRIPTION_CHANGED,
    STEPS_ADDED,
    STEPS_REMOVED,
    STEPS_MODIFIED,
    CONNECTIONS_ADDED,
    CONNECTIONS_REMOVED,
    CONNECTIONS_MODIFIED,
    METADATA_ADDED,
    METADATA_REMOVED,
    METADATA_MODIFIED
}

/**
 * Represents the action taken in a diff.
 */
enum class DiffAction {
    ADDED,
    REMOVED,
    MODIFIED
}

/**
 * Represents a diff of a workflow step.
 *
 * @property id The ID of the step.
 * @property name The name of the step.
 * @property type The type of the step.
 * @property action The action taken on the step.
 * @property changes A list of changes made to the step.
 */
data class StepDiff(
    val id: String,
    val name: String,
    val type: String,
    val action: DiffAction,
    val changes: List<String> = emptyList()
)

/**
 * Represents a diff of a workflow connection.
 *
 * @property sourceId The ID of the source step.
 * @property targetId The ID of the target step.
 * @property condition The condition of the connection.
 * @property action The action taken on the connection.
 * @property changes A list of changes made to the connection.
 */
data class ConnectionDiff(
    val sourceId: String,
    val targetId: String,
    val condition: String?,
    val action: DiffAction,
    val changes: List<String> = emptyList()
)

/**
 * Represents a diff of workflow metadata.
 *
 * @property key The key of the metadata.
 * @property value The value of the metadata.
 * @property action The action taken on the metadata.
 * @property changes A list of changes made to the metadata.
 */
data class MetadataDiff(
    val key: String,
    val value: Any?,
    val action: DiffAction,
    val changes: List<String> = emptyList()
)
