package ai.kastrax.core.workflow.version

import ai.kastrax.core.workflow.visualization.StepConnection
import ai.kastrax.core.workflow.visualization.WorkflowStep

/**
 * Interface for migrating workflows between versions.
 */
interface WorkflowMigration {
    /**
     * Gets the source version of this migration.
     *
     * @return The source version.
     */
    fun getSourceVersion(): String

    /**
     * Gets the target version of this migration.
     *
     * @return The target version.
     */
    fun getTargetVersion(): String

    /**
     * Checks if this migration can be applied to a workflow.
     *
     * @param workflow The workflow to check.
     * @return True if the migration can be applied, false otherwise.
     */
    fun canApply(workflow: VersionedWorkflow): Boolean

    /**
     * Applies the migration to a workflow.
     *
     * @param workflow The workflow to migrate.
     * @return The migrated workflow.
     */
    fun apply(workflow: VersionedWorkflow): VersionedWorkflow
}

/**
 * Base class for workflow migrations.
 *
 * @property sourceVersion The source version of this migration.
 * @property targetVersion The target version of this migration.
 */
abstract class BaseWorkflowMigration(
    private val sourceVersion: String,
    private val targetVersion: String
) : WorkflowMigration {
    override fun getSourceVersion(): String {
        return sourceVersion
    }

    override fun getTargetVersion(): String {
        return targetVersion
    }

    override fun canApply(workflow: VersionedWorkflow): Boolean {
        return workflow.version.version == sourceVersion
    }
}

/**
 * A migration that adds a step to a workflow.
 *
 * @property sourceVersion The source version of this migration.
 * @property targetVersion The target version of this migration.
 * @property step The step to add.
 * @property connections The connections to add for the new step.
 */
class AddStepMigration(
    sourceVersion: String,
    targetVersion: String,
    private val step: WorkflowStep,
    private val connections: List<StepConnection>
) : BaseWorkflowMigration(sourceVersion, targetVersion) {
    override fun apply(workflow: VersionedWorkflow): VersionedWorkflow {
        // Check if the step already exists
        if (workflow.steps.any { it.id == step.id }) {
            throw IllegalStateException("Step with ID ${step.id} already exists in workflow ${workflow.id}")
        }

        // Add the step and connections
        val newSteps = workflow.steps + step
        val newConnections = workflow.connections + connections

        // Create a new version of the workflow
        return workflow.createNewVersion(
            newVersion = getTargetVersion(),
            description = "Added step ${step.name}",
            steps = newSteps,
            connections = newConnections
        )
    }
}

/**
 * A migration that removes a step from a workflow.
 *
 * @property sourceVersion The source version of this migration.
 * @property targetVersion The target version of this migration.
 * @property stepId The ID of the step to remove.
 */
class RemoveStepMigration(
    sourceVersion: String,
    targetVersion: String,
    private val stepId: String
) : BaseWorkflowMigration(sourceVersion, targetVersion) {
    override fun apply(workflow: VersionedWorkflow): VersionedWorkflow {
        // Check if the step exists
        if (workflow.steps.none { it.id == stepId }) {
            throw IllegalStateException("Step with ID $stepId does not exist in workflow ${workflow.id}")
        }

        // Remove the step and its connections
        val newSteps = workflow.steps.filter { it.id != stepId }
        val newConnections = workflow.connections.filter { it.sourceId != stepId && it.targetId != stepId }

        // Create a new version of the workflow
        return workflow.createNewVersion(
            newVersion = getTargetVersion(),
            description = "Removed step $stepId",
            steps = newSteps,
            connections = newConnections
        )
    }
}

/**
 * A migration that modifies a step in a workflow.
 *
 * @property sourceVersion The source version of this migration.
 * @property targetVersion The target version of this migration.
 * @property stepId The ID of the step to modify.
 * @property newStep The new step to replace the old one.
 */
class ModifyStepMigration(
    sourceVersion: String,
    targetVersion: String,
    private val stepId: String,
    private val newStep: WorkflowStep
) : BaseWorkflowMigration(sourceVersion, targetVersion) {
    override fun apply(workflow: VersionedWorkflow): VersionedWorkflow {
        // Check if the step exists
        if (workflow.steps.none { it.id == stepId }) {
            throw IllegalStateException("Step with ID $stepId does not exist in workflow ${workflow.id}")
        }

        // Replace the step
        val newSteps = workflow.steps.map {
            if (it.id == stepId) {
                // 确保新步骤使用相同的ID
                if (newStep.id != stepId) {
                    newStep.copy(id = stepId)
                } else {
                    newStep
                }
            } else {
                it
            }
        }

        // Create a new version of the workflow
        return workflow.createNewVersion(
            newVersion = getTargetVersion(),
            description = "Modified step $stepId",
            steps = newSteps
        )
    }
}

/**
 * A migration that adds a connection to a workflow.
 *
 * @property sourceVersion The source version of this migration.
 * @property targetVersion The target version of this migration.
 * @property connection The connection to add.
 */
class AddConnectionMigration(
    sourceVersion: String,
    targetVersion: String,
    private val connection: StepConnection
) : BaseWorkflowMigration(sourceVersion, targetVersion) {
    override fun apply(workflow: VersionedWorkflow): VersionedWorkflow {
        // Check if the source and target steps exist
        if (workflow.steps.none { it.id == connection.sourceId }) {
            throw IllegalStateException("Source step with ID ${connection.sourceId} does not exist in workflow ${workflow.id}")
        }
        if (workflow.steps.none { it.id == connection.targetId }) {
            throw IllegalStateException("Target step with ID ${connection.targetId} does not exist in workflow ${workflow.id}")
        }

        // Add the connection
        val newConnections = workflow.connections + connection

        // Create a new version of the workflow
        return workflow.createNewVersion(
            newVersion = getTargetVersion(),
            description = "Added connection from ${connection.sourceId} to ${connection.targetId}",
            connections = newConnections
        )
    }
}

/**
 * A migration that removes a connection from a workflow.
 *
 * @property sourceVersion The source version of this migration.
 * @property targetVersion The target version of this migration.
 * @property sourceId The ID of the source step.
 * @property targetId The ID of the target step.
 */
class RemoveConnectionMigration(
    sourceVersion: String,
    targetVersion: String,
    private val sourceId: String,
    private val targetId: String
) : BaseWorkflowMigration(sourceVersion, targetVersion) {
    override fun apply(workflow: VersionedWorkflow): VersionedWorkflow {
        // Check if the connection exists
        if (workflow.connections.none { it.sourceId == sourceId && it.targetId == targetId }) {
            throw IllegalStateException("Connection from $sourceId to $targetId does not exist in workflow ${workflow.id}")
        }

        // Remove the connection
        val newConnections = workflow.connections.filter { !(it.sourceId == sourceId && it.targetId == targetId) }

        // Create a new version of the workflow
        return workflow.createNewVersion(
            newVersion = getTargetVersion(),
            description = "Removed connection from $sourceId to $targetId",
            connections = newConnections
        )
    }
}

/**
 * A composite migration that applies multiple migrations in sequence.
 *
 * @property sourceVersion The source version of this migration.
 * @property targetVersion The target version of this migration.
 * @property migrations The migrations to apply.
 */
class CompositeMigration(
    sourceVersion: String,
    targetVersion: String,
    private val migrations: List<WorkflowMigration>
) : BaseWorkflowMigration(sourceVersion, targetVersion) {
    override fun apply(workflow: VersionedWorkflow): VersionedWorkflow {
        var currentWorkflow = workflow

        // 如果没有迁移，直接创建新版本
        if (migrations.isEmpty()) {
            return currentWorkflow.createNewVersion(
                newVersion = getTargetVersion(),
                description = "Applied empty migration from ${getSourceVersion()} to ${getTargetVersion()}"
            )
        }

        // 应用所有迁移
        for (migration in migrations) {
            if (migration.canApply(currentWorkflow)) {
                currentWorkflow = migration.apply(currentWorkflow)
            } else {
                // 如果无法应用迁移，则跳过并继续
                continue
            }
        }

        // 确保最终版本正确
        if (currentWorkflow.version.version != getTargetVersion()) {
            return currentWorkflow.createNewVersion(
                newVersion = getTargetVersion(),
                description = "Applied composite migration from ${getSourceVersion()} to ${getTargetVersion()}"
            )
        }

        return currentWorkflow
    }
}
