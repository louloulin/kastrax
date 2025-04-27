package ai.kastrax.core.agent.version

import ai.kastrax.core.common.KastraXBase
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Agent版本管理器接口，用于管理Agent的版本历史。
 */
interface AgentVersionManager {
    /**
     * 创建新版本
     *
     * @param agentId Agent ID
     * @param instructions 指令内容
     * @param name 版本名称
     * @param description 版本描述
     * @param metadata 元数据
     * @param activateImmediately 是否立即激活
     * @return 创建的版本
     */
    suspend fun createVersion(
        agentId: String,
        instructions: String,
        name: String? = null,
        description: String? = null,
        metadata: Map<String, String> = emptyMap(),
        activateImmediately: Boolean = false
    ): AgentVersion

    /**
     * 获取版本
     *
     * @param versionId 版本ID
     * @return 版本信息
     */
    suspend fun getVersion(versionId: String): AgentVersion?

    /**
     * 获取Agent的所有版本
     *
     * @param agentId Agent ID
     * @param limit 限制数量
     * @param offset 偏移量
     * @return 版本列表
     */
    suspend fun getVersions(
        agentId: String,
        limit: Int = 100,
        offset: Int = 0
    ): List<AgentVersion>

    /**
     * 获取Agent的当前激活版本
     *
     * @param agentId Agent ID
     * @return 当前激活的版本
     */
    suspend fun getActiveVersion(agentId: String): AgentVersion?

    /**
     * 激活版本
     *
     * @param versionId 版本ID
     * @return 激活的版本
     */
    suspend fun activateVersion(versionId: String): AgentVersion

    /**
     * 回滚到指定版本
     *
     * @param versionId 版本ID
     * @return 回滚后激活的版本
     */
    suspend fun rollbackToVersion(versionId: String): AgentVersion

    /**
     * 更新版本
     *
     * @param versionId 版本ID
     * @param updates 更新内容
     * @return 更新后的版本
     */
    suspend fun updateVersion(
        versionId: String,
        updates: Map<String, Any>
    ): AgentVersion

    /**
     * 删除版本
     *
     * @param versionId 版本ID
     * @return 是否成功
     */
    suspend fun deleteVersion(versionId: String): Boolean

    /**
     * 比较两个版本
     *
     * @param versionId1 版本ID 1
     * @param versionId2 版本ID 2
     * @return 版本差异
     */
    suspend fun compareVersions(
        versionId1: String,
        versionId2: String
    ): VersionDiff
}

/**
 * 版本差异
 *
 * @property versionId1 版本ID 1
 * @property versionId2 版本ID 2
 * @property instructionsDiff 指令差异
 * @property metadataDiff 元数据差异
 */
data class VersionDiff(
    val versionId1: String,
    val versionId2: String,
    val instructionsDiff: String,
    val metadataDiff: Map<String, Pair<String?, String?>> = emptyMap()
)

/**
 * 内存版本管理器实现
 */
class InMemoryAgentVersionManager : KastraXBase(component = "VERSION_MANAGER", name = "内存版本管理器"), AgentVersionManager {
    // 版本存储
    private val versions = ConcurrentHashMap<String, AgentVersion>()
    
    // Agent版本计数器
    private val versionCounters = ConcurrentHashMap<String, AtomicInteger>()
    
    // Agent当前激活版本
    private val activeVersions = ConcurrentHashMap<String, String>()

    override suspend fun createVersion(
        agentId: String,
        instructions: String,
        name: String?,
        description: String?,
        metadata: Map<String, String>,
        activateImmediately: Boolean
    ): AgentVersion {
        // 获取下一个版本号
        val versionNumber = versionCounters.computeIfAbsent(agentId) { AtomicInteger(0) }.incrementAndGet()
        
        // 创建版本
        val version = AgentVersion(
            agentId = agentId,
            versionNumber = versionNumber,
            name = name ?: "Version $versionNumber",
            instructions = instructions,
            status = if (activateImmediately) AgentVersionStatus.ACTIVE else AgentVersionStatus.DRAFT,
            description = description,
            metadata = metadata
        )
        
        // 存储版本
        versions[version.id] = version
        
        // 如果需要立即激活
        if (activateImmediately) {
            // 将之前的激活版本设置为已归档
            val previousActiveVersionId = activeVersions[agentId]
            if (previousActiveVersionId != null) {
                val previousActiveVersion = versions[previousActiveVersionId]
                if (previousActiveVersion != null) {
                    versions[previousActiveVersionId] = previousActiveVersion.withStatus(AgentVersionStatus.ARCHIVED)
                }
            }
            
            // 设置新的激活版本
            activeVersions[agentId] = version.id
        }
        
        logger.info { "创建Agent版本: agentId=$agentId, versionNumber=$versionNumber, active=$activateImmediately" }
        return version
    }

    override suspend fun getVersion(versionId: String): AgentVersion? {
        return versions[versionId]
    }

    override suspend fun getVersions(agentId: String, limit: Int, offset: Int): List<AgentVersion> {
        return versions.values
            .filter { it.agentId == agentId }
            .sortedByDescending { it.versionNumber }
            .drop(offset)
            .take(limit)
    }

    override suspend fun getActiveVersion(agentId: String): AgentVersion? {
        val activeVersionId = activeVersions[agentId] ?: return null
        return versions[activeVersionId]
    }

    override suspend fun activateVersion(versionId: String): AgentVersion {
        // 获取要激活的版本
        val version = versions[versionId] ?: throw IllegalArgumentException("版本不存在: $versionId")
        
        // 将之前的激活版本设置为已归档
        val previousActiveVersionId = activeVersions[version.agentId]
        if (previousActiveVersionId != null && previousActiveVersionId != versionId) {
            val previousActiveVersion = versions[previousActiveVersionId]
            if (previousActiveVersion != null) {
                versions[previousActiveVersionId] = previousActiveVersion.withStatus(AgentVersionStatus.ARCHIVED)
            }
        }
        
        // 激活新版本
        val activatedVersion = version.withStatus(AgentVersionStatus.ACTIVE)
        versions[versionId] = activatedVersion
        activeVersions[version.agentId] = versionId
        
        logger.info { "激活Agent版本: agentId=${version.agentId}, versionId=$versionId" }
        return activatedVersion
    }

    override suspend fun rollbackToVersion(versionId: String): AgentVersion {
        // 回滚就是激活指定版本
        return activateVersion(versionId)
    }

    override suspend fun updateVersion(versionId: String, updates: Map<String, Any>): AgentVersion {
        val version = versions[versionId] ?: throw IllegalArgumentException("版本不存在: $versionId")
        
        var updatedVersion = version
        
        // 应用更新
        updates.forEach { (key, value) ->
            when (key) {
                "name" -> updatedVersion = updatedVersion.withName(value as String)
                "description" -> updatedVersion = updatedVersion.withDescription(value as String)
                "status" -> updatedVersion = updatedVersion.withStatus(AgentVersionStatus.valueOf(value as String))
                "metadata" -> {
                    @Suppress("UNCHECKED_CAST")
                    (value as Map<String, String>).forEach { (metaKey, metaValue) ->
                        updatedVersion = updatedVersion.withMetadata(metaKey, metaValue)
                    }
                }
            }
        }
        
        // 更新版本
        versions[versionId] = updatedVersion
        
        // 如果状态变为ACTIVE，更新activeVersions
        if (updatedVersion.status == AgentVersionStatus.ACTIVE) {
            // 将之前的激活版本设置为已归档
            val previousActiveVersionId = activeVersions[updatedVersion.agentId]
            if (previousActiveVersionId != null && previousActiveVersionId != versionId) {
                val previousActiveVersion = versions[previousActiveVersionId]
                if (previousActiveVersion != null) {
                    versions[previousActiveVersionId] = previousActiveVersion.withStatus(AgentVersionStatus.ARCHIVED)
                }
            }
            
            activeVersions[updatedVersion.agentId] = versionId
        }
        
        logger.info { "更新Agent版本: versionId=$versionId, updates=$updates" }
        return updatedVersion
    }

    override suspend fun deleteVersion(versionId: String): Boolean {
        val version = versions[versionId] ?: return false
        
        // 不能删除激活的版本
        if (version.status == AgentVersionStatus.ACTIVE) {
            throw IllegalStateException("不能删除激活的版本: $versionId")
        }
        
        // 删除版本
        versions.remove(versionId)
        
        logger.info { "删除Agent版本: versionId=$versionId" }
        return true
    }

    override suspend fun compareVersions(versionId1: String, versionId2: String): VersionDiff {
        val version1 = versions[versionId1] ?: throw IllegalArgumentException("版本不存在: $versionId1")
        val version2 = versions[versionId2] ?: throw IllegalArgumentException("版本不存在: $versionId2")
        
        // 计算指令差异
        val instructionsDiff = computeTextDiff(version1.instructions, version2.instructions)
        
        // 计算元数据差异
        val metadataDiff = mutableMapOf<String, Pair<String?, String?>>()
        
        // 合并两个版本的所有元数据键
        val allKeys = version1.metadata.keys + version2.metadata.keys
        
        // 比较每个键的值
        allKeys.forEach { key ->
            val value1 = version1.metadata[key]
            val value2 = version2.metadata[key]
            
            if (value1 != value2) {
                metadataDiff[key] = Pair(value1, value2)
            }
        }
        
        return VersionDiff(
            versionId1 = versionId1,
            versionId2 = versionId2,
            instructionsDiff = instructionsDiff,
            metadataDiff = metadataDiff
        )
    }
    
    /**
     * 计算文本差异
     */
    private fun computeTextDiff(text1: String, text2: String): String {
        // 简单实现，实际应用中可以使用更复杂的差异算法
        val lines1 = text1.lines()
        val lines2 = text2.lines()
        
        val diff = StringBuilder()
        
        // 找出不同的行
        val maxLines = maxOf(lines1.size, lines2.size)
        for (i in 0 until maxLines) {
            val line1 = if (i < lines1.size) lines1[i] else null
            val line2 = if (i < lines2.size) lines2[i] else null
            
            if (line1 != line2) {
                if (line1 != null) {
                    diff.appendLine("- $line1")
                }
                if (line2 != null) {
                    diff.appendLine("+ $line2")
                }
            } else {
                diff.appendLine("  ${line1 ?: ""}")
            }
        }
        
        return diff.toString()
    }
}

/**
 * SQLite版本管理器实现
 */
class SQLiteAgentVersionManager(
    private val dbPath: String = "agent_versions.db"
) : KastraXBase(component = "VERSION_MANAGER", name = "SQLite版本管理器"), AgentVersionManager {
    // TODO: 实现SQLite版本管理器
    
    override suspend fun createVersion(
        agentId: String,
        instructions: String,
        name: String?,
        description: String?,
        metadata: Map<String, String>,
        activateImmediately: Boolean
    ): AgentVersion {
        TODO("Not yet implemented")
    }

    override suspend fun getVersion(versionId: String): AgentVersion? {
        TODO("Not yet implemented")
    }

    override suspend fun getVersions(agentId: String, limit: Int, offset: Int): List<AgentVersion> {
        TODO("Not yet implemented")
    }

    override suspend fun getActiveVersion(agentId: String): AgentVersion? {
        TODO("Not yet implemented")
    }

    override suspend fun activateVersion(versionId: String): AgentVersion {
        TODO("Not yet implemented")
    }

    override suspend fun rollbackToVersion(versionId: String): AgentVersion {
        TODO("Not yet implemented")
    }

    override suspend fun updateVersion(versionId: String, updates: Map<String, Any>): AgentVersion {
        TODO("Not yet implemented")
    }

    override suspend fun deleteVersion(versionId: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun compareVersions(versionId1: String, versionId2: String): VersionDiff {
        TODO("Not yet implemented")
    }
}
