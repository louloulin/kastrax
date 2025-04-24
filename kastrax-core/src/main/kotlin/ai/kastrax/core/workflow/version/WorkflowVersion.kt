package ai.kastrax.core.workflow.version

import java.time.Instant

/**
 * Represents a version of a workflow.
 *
 * @property workflowId The ID of the workflow.
 * @property version The version number.
 * @property description A description of this version.
 * @property createdAt The time when this version was created.
 * @property createdBy The user who created this version.
 * @property isActive Whether this version is active.
 * @property tags Optional tags associated with this version.
 */
data class WorkflowVersion(
    val workflowId: String,
    val version: String,
    val description: String = "",
    val createdAt: Instant = Instant.now(),
    val createdBy: String = "system",
    val isActive: Boolean = false,
    val tags: Map<String, String> = emptyMap()
) {
    /**
     * Checks if this version is compatible with another version.
     *
     * @param other The other version to check compatibility with.
     * @return True if the versions are compatible, false otherwise.
     */
    fun isCompatibleWith(other: WorkflowVersion): Boolean {
        // In a real implementation, this would check for breaking changes
        // For now, we'll just check if the major version is the same
        val thisMajor = getMajorVersion(this.version)
        val otherMajor = getMajorVersion(other.version)
        return thisMajor == otherMajor
    }
    
    /**
     * Gets the major version from a semantic version string.
     *
     * @param version The semantic version string.
     * @return The major version number.
     */
    private fun getMajorVersion(version: String): Int {
        val parts = version.split(".")
        return if (parts.isNotEmpty()) {
            parts[0].toIntOrNull() ?: 0
        } else {
            0
        }
    }
    
    companion object {
        /**
         * Creates a new version from an existing version.
         *
         * @param existing The existing version.
         * @param newVersion The new version number.
         * @param description The description of the new version.
         * @param createdBy The user who created the new version.
         * @param isActive Whether the new version is active.
         * @param tags Optional tags for the new version.
         * @return The new version.
         */
        fun createNewVersion(
            existing: WorkflowVersion,
            newVersion: String,
            description: String = "",
            createdBy: String = "system",
            isActive: Boolean = false,
            tags: Map<String, String> = emptyMap()
        ): WorkflowVersion {
            return WorkflowVersion(
                workflowId = existing.workflowId,
                version = newVersion,
                description = description,
                createdAt = Instant.now(),
                createdBy = createdBy,
                isActive = isActive,
                tags = tags
            )
        }
        
        /**
         * Parses a version string into a semantic version.
         *
         * @param version The version string to parse.
         * @return A triple of (major, minor, patch) version numbers.
         */
        fun parseVersion(version: String): Triple<Int, Int, Int> {
            val parts = version.split(".")
            val major = if (parts.size > 0) parts[0].toIntOrNull() ?: 0 else 0
            val minor = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
            val patch = if (parts.size > 2) parts[2].toIntOrNull() ?: 0 else 0
            return Triple(major, minor, patch)
        }
        
        /**
         * Increments a version string.
         *
         * @param version The version string to increment.
         * @param incrementMajor Whether to increment the major version.
         * @param incrementMinor Whether to increment the minor version.
         * @param incrementPatch Whether to increment the patch version.
         * @return The incremented version string.
         */
        fun incrementVersion(
            version: String,
            incrementMajor: Boolean = false,
            incrementMinor: Boolean = false,
            incrementPatch: Boolean = true
        ): String {
            val (major, minor, patch) = parseVersion(version)
            val newMajor = if (incrementMajor) major + 1 else major
            val newMinor = if (incrementMinor) minor + 1 else if (incrementMajor) 0 else minor
            val newPatch = if (incrementPatch) patch + 1 else if (incrementMajor || incrementMinor) 0 else patch
            return "$newMajor.$newMinor.$newPatch"
        }
    }
}
