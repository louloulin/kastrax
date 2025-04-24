package ai.kastrax.core.workflow.version

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of the WorkflowVersionStorage interface.
 * This implementation is suitable for testing and development,
 * but not for production use as versions are lost when the application restarts.
 */
class InMemoryWorkflowVersionStorage : WorkflowVersionStorage {
    private val workflows = ConcurrentHashMap<String, MutableMap<String, VersionedWorkflow>>()
    private val activeVersions = ConcurrentHashMap<String, String>()

    override fun saveWorkflow(workflow: VersionedWorkflow): Boolean {
        val workflowVersions = workflows.computeIfAbsent(workflow.id) { mutableMapOf() }
        workflowVersions[workflow.version.version] = workflow

        // If this is the first version or it's marked as active, set it as the active version
        if (workflowVersions.size == 1 || workflow.version.isActive) {
            activeVersions[workflow.id] = workflow.version.version
        }

        return true
    }

    override fun getWorkflow(workflowId: String, version: String?): VersionedWorkflow? {
        val workflowVersions = workflows[workflowId] ?: return null

        return if (version != null) {
            workflowVersions[version]
        } else {
            val activeVersion = activeVersions[workflowId]
            if (activeVersion != null) {
                workflowVersions[activeVersion]
            } else {
                // If no active version is set, return the latest version
                workflowVersions.values.maxByOrNull { it.version.createdAt }
            }
        }
    }

    override fun getWorkflowVersions(workflowId: String, limit: Int, offset: Int): List<WorkflowVersion> {
        val workflowVersions = workflows[workflowId] ?: return emptyList()

        return workflowVersions.values
            .map { it.version }
            .sortedWith(compareByDescending<WorkflowVersion> { it.createdAt }.thenByDescending { it.version })
            .drop(offset)
            .take(limit)
    }

    override fun getActiveWorkflowVersion(workflowId: String): WorkflowVersion? {
        val activeVersion = activeVersions[workflowId] ?: return null
        return workflows[workflowId]?.get(activeVersion)?.version
    }

    override fun setActiveWorkflowVersion(workflowId: String, version: String): Boolean {
        val workflowVersions = workflows[workflowId] ?: return false

        if (!workflowVersions.containsKey(version)) {
            return false
        }

        // Update the active version
        activeVersions[workflowId] = version

        // Update the isActive flag in all versions
        workflowVersions.forEach { (ver, workflow) ->
            val isActive = ver == version
            if (workflow.version.isActive != isActive) {
                val updatedVersion = workflow.version.copy(isActive = isActive)
                workflowVersions[ver] = workflow.copy(version = updatedVersion)
            }
        }

        return true
    }

    override fun getAllWorkflows(limit: Int, offset: Int): List<VersionedWorkflow> {
        return workflows.values
            .flatMap { it.values }
            .sortedByDescending { it.version.createdAt }
            .drop(offset)
            .take(limit)
    }

    override fun getWorkflowsCreatedAfter(time: Instant, limit: Int, offset: Int): List<VersionedWorkflow> {
        return workflows.values
            .flatMap { it.values }
            .filter { it.version.createdAt.isAfter(time) }
            .sortedByDescending { it.version.createdAt }
            .drop(offset)
            .take(limit)
    }

    override fun getWorkflowsByTags(tags: Map<String, String>, limit: Int, offset: Int): List<VersionedWorkflow> {
        return workflows.values
            .flatMap { it.values }
            .filter { workflow ->
                tags.all { (key, value) -> workflow.version.tags[key] == value }
            }
            .sortedByDescending { it.version.createdAt }
            .drop(offset)
            .take(limit)
    }

    override fun deleteWorkflowVersion(workflowId: String, version: String): Boolean {
        val workflowVersions = workflows[workflowId] ?: return false

        val removed = workflowVersions.remove(version) != null

        // If the active version was deleted, set a new active version
        if (removed && activeVersions[workflowId] == version) {
            if (workflowVersions.isEmpty()) {
                activeVersions.remove(workflowId)
            } else {
                // Set the latest version as active
                val latestVersion = workflowVersions.values.maxByOrNull { it.version.createdAt }
                if (latestVersion != null) {
                    activeVersions[workflowId] = latestVersion.version.version

                    // Update the isActive flag
                    workflowVersions[latestVersion.version.version] = latestVersion.copy(
                        version = latestVersion.version.copy(isActive = true)
                    )
                }
            }
        }

        return removed
    }

    override fun deleteWorkflow(workflowId: String): Int {
        val workflowVersions = workflows.remove(workflowId) ?: return 0
        activeVersions.remove(workflowId)
        return workflowVersions.size
    }
}
