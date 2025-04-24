package ai.kastrax.core.workflow.version

import ai.kastrax.core.workflow.visualization.StepConnection
import ai.kastrax.core.workflow.visualization.WorkflowStep

/**
 * Checks compatibility between workflow versions.
 */
class CompatibilityChecker {
    /**
     * Checks if two workflow versions are compatible.
     *
     * @param source The source workflow.
     * @param target The target workflow.
     * @return A compatibility result.
     */
    fun checkCompatibility(source: VersionedWorkflow, target: VersionedWorkflow): CompatibilityResult {
        // Check if the workflows have the same ID
        if (source.id != target.id) {
            return CompatibilityResult(
                isCompatible = false,
                issues = listOf(CompatibilityIssue(
                    type = CompatibilityIssueType.DIFFERENT_WORKFLOW,
                    description = "Workflows have different IDs: ${source.id} vs ${target.id}"
                ))
            )
        }
        
        // Check if the versions are compatible
        if (!source.version.isCompatibleWith(target.version)) {
            return CompatibilityResult(
                isCompatible = false,
                issues = listOf(CompatibilityIssue(
                    type = CompatibilityIssueType.INCOMPATIBLE_VERSIONS,
                    description = "Versions are not compatible: ${source.version.version} vs ${target.version.version}"
                ))
            )
        }
        
        val issues = mutableListOf<CompatibilityIssue>()
        
        // Check for removed steps
        val removedSteps = source.steps.filter { sourceStep ->
            target.steps.none { it.id == sourceStep.id }
        }
        if (removedSteps.isNotEmpty()) {
            issues.add(CompatibilityIssue(
                type = CompatibilityIssueType.REMOVED_STEPS,
                description = "Steps were removed: ${removedSteps.map { it.id }}",
                details = removedSteps.map { it.id }
            ))
        }
        
        // Check for added steps
        val addedSteps = target.steps.filter { targetStep ->
            source.steps.none { it.id == targetStep.id }
        }
        if (addedSteps.isNotEmpty()) {
            issues.add(CompatibilityIssue(
                type = CompatibilityIssueType.ADDED_STEPS,
                description = "Steps were added: ${addedSteps.map { it.id }}",
                details = addedSteps.map { it.id }
            ))
        }
        
        // Check for modified steps
        val modifiedSteps = source.steps.filter { sourceStep ->
            target.steps.any { it.id == sourceStep.id && it.type != sourceStep.type }
        }
        if (modifiedSteps.isNotEmpty()) {
            issues.add(CompatibilityIssue(
                type = CompatibilityIssueType.MODIFIED_STEPS,
                description = "Steps were modified: ${modifiedSteps.map { it.id }}",
                details = modifiedSteps.map { it.id }
            ))
        }
        
        // Check for removed connections
        val removedConnections = source.connections.filter { sourceConn ->
            target.connections.none { it.sourceId == sourceConn.sourceId && it.targetId == sourceConn.targetId }
        }
        if (removedConnections.isNotEmpty()) {
            issues.add(CompatibilityIssue(
                type = CompatibilityIssueType.REMOVED_CONNECTIONS,
                description = "Connections were removed: ${removedConnections.map { "${it.sourceId} -> ${it.targetId}" }}",
                details = removedConnections.map { "${it.sourceId} -> ${it.targetId}" }
            ))
        }
        
        // Check for added connections
        val addedConnections = target.connections.filter { targetConn ->
            source.connections.none { it.sourceId == targetConn.sourceId && it.targetId == targetConn.targetId }
        }
        if (addedConnections.isNotEmpty()) {
            issues.add(CompatibilityIssue(
                type = CompatibilityIssueType.ADDED_CONNECTIONS,
                description = "Connections were added: ${addedConnections.map { "${it.sourceId} -> ${it.targetId}" }}",
                details = addedConnections.map { "${it.sourceId} -> ${it.targetId}" }
            ))
        }
        
        // Check for modified connections (changed conditions)
        val modifiedConnections = source.connections.filter { sourceConn ->
            target.connections.any { 
                it.sourceId == sourceConn.sourceId && 
                it.targetId == sourceConn.targetId && 
                it.condition != sourceConn.condition 
            }
        }
        if (modifiedConnections.isNotEmpty()) {
            issues.add(CompatibilityIssue(
                type = CompatibilityIssueType.MODIFIED_CONNECTIONS,
                description = "Connection conditions were modified: ${modifiedConnections.map { "${it.sourceId} -> ${it.targetId}" }}",
                details = modifiedConnections.map { "${it.sourceId} -> ${it.targetId}" }
            ))
        }
        
        // Determine overall compatibility
        val isCompatible = issues.none { 
            it.type == CompatibilityIssueType.DIFFERENT_WORKFLOW || 
            it.type == CompatibilityIssueType.INCOMPATIBLE_VERSIONS ||
            it.type == CompatibilityIssueType.REMOVED_STEPS ||
            it.type == CompatibilityIssueType.MODIFIED_STEPS
        }
        
        return CompatibilityResult(
            isCompatible = isCompatible,
            issues = issues
        )
    }
    
    /**
     * Generates a migration plan to migrate from one workflow version to another.
     *
     * @param source The source workflow.
     * @param target The target workflow.
     * @return A migration plan, or null if migration is not possible.
     */
    fun generateMigrationPlan(source: VersionedWorkflow, target: VersionedWorkflow): MigrationPlan? {
        val compatibilityResult = checkCompatibility(source, target)
        
        if (!compatibilityResult.isCompatible) {
            // If the workflows are not compatible, check if we can still generate a migration plan
            if (compatibilityResult.issues.any { 
                it.type == CompatibilityIssueType.DIFFERENT_WORKFLOW || 
                it.type == CompatibilityIssueType.INCOMPATIBLE_VERSIONS 
            }) {
                // If the workflows are different or the versions are incompatible, we can't generate a migration plan
                return null
            }
        }
        
        val migrations = mutableListOf<WorkflowMigration>()
        
        // Handle removed steps
        val removedStepIds = source.steps.map { it.id }.toSet() - target.steps.map { it.id }.toSet()
        for (stepId in removedStepIds) {
            migrations.add(RemoveStepMigration(
                sourceVersion = source.version.version,
                targetVersion = target.version.version,
                stepId = stepId
            ))
        }
        
        // Handle added steps
        val addedSteps = target.steps.filter { targetStep ->
            source.steps.none { it.id == targetStep.id }
        }
        for (step in addedSteps) {
            val connections = target.connections.filter { it.sourceId == step.id || it.targetId == step.id }
            migrations.add(AddStepMigration(
                sourceVersion = source.version.version,
                targetVersion = target.version.version,
                step = step,
                connections = connections
            ))
        }
        
        // Handle modified steps
        val modifiedSteps = source.steps.filter { sourceStep ->
            target.steps.any { it.id == sourceStep.id && (it.type != sourceStep.type || it.name != sourceStep.name || it.description != sourceStep.description) }
        }
        for (sourceStep in modifiedSteps) {
            val targetStep = target.steps.first { it.id == sourceStep.id }
            migrations.add(ModifyStepMigration(
                sourceVersion = source.version.version,
                targetVersion = target.version.version,
                stepId = sourceStep.id,
                newStep = targetStep
            ))
        }
        
        // Handle removed connections
        val removedConnections = source.connections.filter { sourceConn ->
            target.connections.none { it.sourceId == sourceConn.sourceId && it.targetId == sourceConn.targetId }
        }
        for (connection in removedConnections) {
            migrations.add(RemoveConnectionMigration(
                sourceVersion = source.version.version,
                targetVersion = target.version.version,
                sourceId = connection.sourceId,
                targetId = connection.targetId
            ))
        }
        
        // Handle added connections
        val addedConnections = target.connections.filter { targetConn ->
            source.connections.none { it.sourceId == targetConn.sourceId && it.targetId == targetConn.targetId }
        }
        for (connection in addedConnections) {
            migrations.add(AddConnectionMigration(
                sourceVersion = source.version.version,
                targetVersion = target.version.version,
                connection = connection
            ))
        }
        
        // If there are no migrations, return null
        if (migrations.isEmpty()) {
            return null
        }
        
        // Create a composite migration
        val compositeMigration = CompositeMigration(
            sourceVersion = source.version.version,
            targetVersion = target.version.version,
            migrations = migrations
        )
        
        return MigrationPlan(
            sourceWorkflow = source,
            targetWorkflow = target,
            migration = compositeMigration
        )
    }
}

/**
 * Represents the result of a compatibility check.
 *
 * @property isCompatible Whether the workflows are compatible.
 * @property issues A list of compatibility issues.
 */
data class CompatibilityResult(
    val isCompatible: Boolean,
    val issues: List<CompatibilityIssue> = emptyList()
)

/**
 * Represents a compatibility issue.
 *
 * @property type The type of the issue.
 * @property description A description of the issue.
 * @property details Additional details about the issue.
 */
data class CompatibilityIssue(
    val type: CompatibilityIssueType,
    val description: String,
    val details: List<String> = emptyList()
)

/**
 * Represents the type of a compatibility issue.
 */
enum class CompatibilityIssueType {
    DIFFERENT_WORKFLOW,
    INCOMPATIBLE_VERSIONS,
    REMOVED_STEPS,
    ADDED_STEPS,
    MODIFIED_STEPS,
    REMOVED_CONNECTIONS,
    ADDED_CONNECTIONS,
    MODIFIED_CONNECTIONS
}

/**
 * Represents a plan to migrate from one workflow version to another.
 *
 * @property sourceWorkflow The source workflow.
 * @property targetWorkflow The target workflow.
 * @property migration The migration to apply.
 */
data class MigrationPlan(
    val sourceWorkflow: VersionedWorkflow,
    val targetWorkflow: VersionedWorkflow,
    val migration: WorkflowMigration
)
