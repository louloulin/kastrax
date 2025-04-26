package ai.kastrax.core.agent.version

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Agent 版本信息
 *
 * @property version 版本号，格式为 "x.y.z"
 * @property description 版本描述
 * @property createdBy 创建者
 * @property createdAt 创建时间
 * @property isActive 是否为活动版本
 */
@Serializable
data class AgentVersion(
    val version: String,
    val description: String = "",
    val createdBy: String = "system",
    val createdAt: Instant = Clock.System.now(),
    val isActive: Boolean = false
) {
    companion object {
        /**
         * 初始版本号
         */
        const val INITIAL_VERSION = "1.0.0"

        /**
         * 增加版本号
         *
         * @param currentVersion 当前版本号
         * @param incrementMajor 是否增加主版本号
         * @param incrementMinor 是否增加次版本号
         * @param incrementPatch 是否增加补丁版本号
         * @return 新的版本号
         */
        fun incrementVersion(
            currentVersion: String,
            incrementMajor: Boolean = false,
            incrementMinor: Boolean = false,
            incrementPatch: Boolean = true
        ): String {
            val parts = currentVersion.split(".")
            if (parts.size != 3) {
                throw IllegalArgumentException("Invalid version format: $currentVersion")
            }

            var major = parts[0].toInt()
            var minor = parts[1].toInt()
            var patch = parts[2].toInt()

            if (incrementMajor) {
                major++
                minor = 0
                patch = 0
            } else if (incrementMinor) {
                minor++
                patch = 0
            } else if (incrementPatch) {
                patch++
            }

            return "$major.$minor.$patch"
        }
    }
}
