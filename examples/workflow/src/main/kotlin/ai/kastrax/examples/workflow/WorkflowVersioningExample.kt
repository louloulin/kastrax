package ai.kastrax.examples.workflow

import ai.kastrax.core.workflow.version.CompatibilityChecker
import ai.kastrax.core.workflow.version.InMemoryWorkflowVersionStorage
import ai.kastrax.core.workflow.version.WorkflowDiffTool
import ai.kastrax.core.workflow.version.WorkflowRollback
import ai.kastrax.core.workflow.version.WorkflowVersionManager
import ai.kastrax.core.workflow.visualization.StepConnection
import ai.kastrax.core.workflow.visualization.WorkflowStep

/**
 * Example demonstrating workflow versioning.
 */
object WorkflowVersioningExample {
    @JvmStatic
    fun main(args: Array<String>) {
        // Create components
        val versionStorage = InMemoryWorkflowVersionStorage()
        val versionManager = WorkflowVersionManager(versionStorage)
        val compatibilityChecker = CompatibilityChecker()
        val diffTool = WorkflowDiffTool()
        val rollback = WorkflowRollback(versionManager)
        
        println("=== Workflow Versioning Example ===")
        
        // Create initial workflow
        println("\n1. Creating initial workflow (v1.0.0)")
        val initialWorkflow = versionManager.createWorkflow(
            name = "Sample Workflow",
            description = "A sample workflow for demonstration",
            steps = listOf(
                WorkflowStep(
                    id = "start",
                    name = "Start",
                    type = "start",
                    description = "Start of workflow"
                ),
                WorkflowStep(
                    id = "process",
                    name = "Process Data",
                    type = "process",
                    description = "Process input data"
                ),
                WorkflowStep(
                    id = "end",
                    name = "End",
                    type = "end",
                    description = "End of workflow"
                )
            ),
            connections = listOf(
                StepConnection(
                    sourceId = "start",
                    targetId = "process"
                ),
                StepConnection(
                    sourceId = "process",
                    targetId = "end"
                )
            ),
            metadata = mapOf(
                "author" to "John Doe",
                "category" to "sample"
            )
        )
        
        println("Created workflow: ${initialWorkflow.id}")
        println("Version: ${initialWorkflow.version.version}")
        println("Steps: ${initialWorkflow.steps.map { it.id }}")
        println("Connections: ${initialWorkflow.connections.map { "${it.sourceId} -> ${it.targetId}" }}")
        
        // Create a minor update (v1.1.0)
        println("\n2. Creating minor update (v1.1.0)")
        val minorUpdateWorkflow = versionManager.createNewVersion(
            workflowId = initialWorkflow.id,
            incrementMinor = true,
            description = "Minor update - modified process step",
            steps = initialWorkflow.steps.map { 
                if (it.id == "process") {
                    it.copy(name = "Enhanced Process", description = "Enhanced data processing")
                } else {
                    it
                }
            },
            createdBy = "Jane Smith"
        )
        
        println("Created version: ${minorUpdateWorkflow!!.version.version}")
        println("Description: ${minorUpdateWorkflow.version.description}")
        println("Created by: ${minorUpdateWorkflow.version.createdBy}")
        
        // Create a major update (v2.0.0) with breaking changes
        println("\n3. Creating major update (v2.0.0) with breaking changes")
        val majorUpdateSteps = initialWorkflow.steps.toMutableList()
        
        // Remove the process step
        majorUpdateSteps.removeIf { it.id == "process" }
        
        // Add new steps
        majorUpdateSteps.addAll(listOf(
            WorkflowStep(
                id = "validate",
                name = "Validate Data",
                type = "validate",
                description = "Validate input data"
            ),
            WorkflowStep(
                id = "transform",
                name = "Transform Data",
                type = "transform",
                description = "Transform validated data"
            )
        ))
        
        // Update connections
        val majorUpdateConnections = listOf(
            StepConnection(
                sourceId = "start",
                targetId = "validate"
            ),
            StepConnection(
                sourceId = "validate",
                targetId = "transform"
            ),
            StepConnection(
                sourceId = "transform",
                targetId = "end"
            )
        )
        
        val majorUpdateWorkflow = versionManager.createNewVersion(
            workflowId = initialWorkflow.id,
            newVersion = "2.0.0",
            description = "Major update - restructured workflow",
            steps = majorUpdateSteps,
            connections = majorUpdateConnections,
            metadata = initialWorkflow.metadata + mapOf("updated" to "true"),
            createdBy = "John Doe",
            setActive = true
        )
        
        println("Created version: ${majorUpdateWorkflow!!.version.version}")
        println("Description: ${majorUpdateWorkflow.version.description}")
        println("Steps: ${majorUpdateWorkflow.steps.map { it.id }}")
        println("Connections: ${majorUpdateWorkflow.connections.map { "${it.sourceId} -> ${it.targetId}" }}")
        
        // List all versions
        println("\n4. Listing all versions")
        val versions = versionManager.getWorkflowVersions(initialWorkflow.id)
        versions.forEachIndexed { index, version ->
            println("${index + 1}. Version ${version.version} (${if (version.isActive) "active" else "inactive"}) - ${version.description}")
        }
        
        // Check compatibility between versions
        println("\n5. Checking compatibility between v1.0.0 and v1.1.0")
        val minorCompatibility = compatibilityChecker.checkCompatibility(initialWorkflow, minorUpdateWorkflow)
        println("Compatible: ${minorCompatibility.isCompatible}")
        if (minorCompatibility.issues.isNotEmpty()) {
            println("Issues:")
            minorCompatibility.issues.forEach { issue ->
                println("- ${issue.type}: ${issue.description}")
            }
        } else {
            println("No compatibility issues found")
        }
        
        println("\n6. Checking compatibility between v1.0.0 and v2.0.0")
        val majorCompatibility = compatibilityChecker.checkCompatibility(initialWorkflow, majorUpdateWorkflow)
        println("Compatible: ${majorCompatibility.isCompatible}")
        println("Issues:")
        majorCompatibility.issues.forEach { issue ->
            println("- ${issue.type}: ${issue.description}")
        }
        
        // Generate and display diff between versions
        println("\n7. Generating diff between v1.0.0 and v2.0.0")
        val diff = diffTool.compareWorkflows(initialWorkflow, majorUpdateWorkflow)
        println(diffTool.formatDiff(diff))
        
        // Generate migration plan
        println("\n8. Generating migration plan from v1.0.0 to v2.0.0")
        val migrationPlan = compatibilityChecker.generateMigrationPlan(initialWorkflow, majorUpdateWorkflow)
        if (migrationPlan != null) {
            println("Migration plan generated:")
            println("Source version: ${migrationPlan.sourceWorkflow.version.version}")
            println("Target version: ${migrationPlan.targetWorkflow.version.version}")
            println("Migration type: ${migrationPlan.migration.javaClass.simpleName}")
            
            // Apply migration
            println("\n9. Applying migration")
            val migratedWorkflow = migrationPlan.migration.apply(initialWorkflow)
            println("Migration applied successfully")
            println("Migrated version: ${migratedWorkflow.version.version}")
            println("Steps: ${migratedWorkflow.steps.map { it.id }}")
            println("Connections: ${migratedWorkflow.connections.map { "${it.sourceId} -> ${it.targetId}" }}")
        } else {
            println("No migration plan could be generated")
        }
        
        // Rollback to previous version
        println("\n10. Rolling back to v1.1.0")
        val rolledBackWorkflow = rollback.rollbackToVersion(
            workflowId = initialWorkflow.id,
            targetVersion = "1.1.0",
            setActive = true
        )
        
        println("Rolled back to version: ${rolledBackWorkflow!!.version.version}")
        println("Active version: ${versionManager.getActiveWorkflowVersion(initialWorkflow.id)!!.version}")
        
        // Create a new version based on a previous version
        println("\n11. Creating new version (v2.1.0) based on v2.0.0")
        val newVersionFromPrevious = rollback.createVersionFromPrevious(
            workflowId = initialWorkflow.id,
            sourceVersion = "2.0.0",
            newVersion = "2.1.0",
            description = "New version based on v2.0.0",
            createdBy = "Jane Smith",
            setActive = true
        )
        
        println("Created version: ${newVersionFromPrevious!!.version.version}")
        println("Based on: 2.0.0")
        println("Active version: ${versionManager.getActiveWorkflowVersion(initialWorkflow.id)!!.version}")
        
        // List all versions again
        println("\n12. Final list of all versions")
        val finalVersions = versionManager.getWorkflowVersions(initialWorkflow.id)
        finalVersions.forEachIndexed { index, version ->
            println("${index + 1}. Version ${version.version} (${if (version.isActive) "active" else "inactive"}) - ${version.description}")
        }
    }
}
