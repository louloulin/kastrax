package ai.kastrax.core.agent.version

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Agent版本状态枚举
 */
enum class AgentVersionStatus {
    DRAFT,      // 草稿状态
    ACTIVE,     // 当前激活的版本
    ARCHIVED,   // 已归档的版本
    DEPRECATED  // 已弃用的版本
}

/**
 * Agent版本，用于表示Agent的一个版本。
 *
 * @property id 版本ID
 * @property agentId Agent ID
 * @property versionNumber 版本号
 * @property name 版本名称
 * @property instructions 指令内容
 * @property status 版本状态
 * @property description 版本描述
 * @property metadata 元数据
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 */
@Serializable
data class AgentVersion(
    val id: String = UUID.randomUUID().toString(),
    val agentId: String,
    val versionNumber: Int,
    val name: String? = null,
    val instructions: String,
    val status: AgentVersionStatus = AgentVersionStatus.DRAFT,
    val description: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
) {
    /**
     * 更新版本状态
     */
    fun withStatus(status: AgentVersionStatus): AgentVersion {
        return copy(
            status = status,
            updatedAt = Clock.System.now()
        )
    }

    /**
     * 更新版本名称
     */
    fun withName(name: String): AgentVersion {
        return copy(
            name = name,
            updatedAt = Clock.System.now()
        )
    }

    /**
     * 更新版本描述
     */
    fun withDescription(description: String): AgentVersion {
        return copy(
            description = description,
            updatedAt = Clock.System.now()
        )
    }

    /**
     * 添加或更新元数据
     */
    fun withMetadata(key: String, value: String): AgentVersion {
        return copy(
            metadata = metadata + (key to value),
            updatedAt = Clock.System.now()
        )
    }

    companion object {
        /**
         * 创建初始版本
         */
        fun createInitialVersion(agentId: String, instructions: String): AgentVersion {
            return AgentVersion(
                agentId = agentId,
                versionNumber = 1,
                name = "Initial Version",
                instructions = instructions,
                status = AgentVersionStatus.ACTIVE,
                description = "Initial version"
            )
        }
    }
}