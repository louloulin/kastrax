package ai.kastrax.code.workflow

import ai.kastrax.code.common.KastraXCodeBase
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

/**
 * 检查点管理器实现
 *
 * @property project 项目
 */
@Service(Service.Level.PROJECT)
class CheckpointManagerImpl(
    private val project: Project
) : CheckpointManager, KastraXCodeBase(component = "CHECKPOINT_MANAGER") {

    // 使用父类的logger

    // 检查点存储
    private val checkpoints = mutableMapOf<String, CodeCheckpointImpl>()

    /**
     * 创建检查点
     *
     * @param name 检查点名称
     * @param description 检查点描述
     * @param metadata 元数据
     * @return 创建的检查点
     */
    override suspend fun createCheckpoint(
        name: String,
        description: String,
        metadata: Map<String, Any>
    ): CodeCheckpoint = withContext(Dispatchers.IO) {
        logger.info { "创建检查点: $name" }

        // 创建检查点
        val id = UUID.randomUUID().toString()
        val checkpoint = CodeCheckpointImpl(
            id = id,
            name = name,
            description = description,
            createdAt = Instant.now(),
            fileSnapshots = emptyList(),
            metadata = metadata
        )

        // 存储检查点
        checkpoints[id] = checkpoint

        return@withContext checkpoint
    }

    /**
     * 获取检查点
     *
     * @param id 检查点ID
     * @return 检查点
     */
    override suspend fun getCheckpoint(id: String): CodeCheckpoint? = withContext(Dispatchers.IO) {
        return@withContext checkpoints[id]
    }

    /**
     * 获取所有检查点
     *
     * @return 检查点列表
     */
    override suspend fun getAllCheckpoints(): List<CodeCheckpoint> = withContext(Dispatchers.IO) {
        return@withContext checkpoints.values.toList()
    }

    /**
     * 获取最近的检查点
     *
     * @param limit 限制数量
     * @return 检查点列表
     */
    override suspend fun getRecentCheckpoints(limit: Int): List<CodeCheckpoint> = withContext(Dispatchers.IO) {
        return@withContext checkpoints.values
            .sortedByDescending { it.createdAt }
            .take(limit)
    }

    /**
     * 恢复检查点
     *
     * @param id 检查点ID
     * @return 是否成功恢复
     */
    override suspend fun restoreCheckpoint(id: String): Boolean = withContext(Dispatchers.IO) {
        logger.info { "恢复检查点: $id" }

        val checkpoint = checkpoints[id] ?: return@withContext false
        return@withContext checkpoint.restore()
    }

    /**
     * 删除检查点
     *
     * @param id 检查点ID
     * @return 是否成功删除
     */
    override suspend fun deleteCheckpoint(id: String): Boolean = withContext(Dispatchers.IO) {
        logger.info { "删除检查点: $id" }

        val checkpoint = checkpoints.remove(id) ?: return@withContext false
        return@withContext checkpoint.delete()
    }

    /**
     * 比较检查点
     *
     * @param id1 检查点1 ID
     * @param id2 检查点2 ID
     * @return 差异列表
     */
    override suspend fun compareCheckpoints(id1: String, id2: String): List<FileDiff> = withContext(Dispatchers.IO) {
        logger.info { "比较检查点: $id1, $id2" }

        val checkpoint1 = checkpoints[id1] ?: return@withContext emptyList()
        val checkpoint2 = checkpoints[id2] ?: return@withContext emptyList()

        return@withContext checkpoint1.diff(checkpoint2)
    }

    /**
     * 创建自动检查点
     *
     * @return 创建的检查点
     */
    override suspend fun createAutoCheckpoint(): CodeCheckpoint = withContext(Dispatchers.IO) {
        logger.info { "创建自动检查点" }

        // 创建检查点
        val id = UUID.randomUUID().toString()
        val name = "自动检查点 ${Instant.now()}"
        val checkpoint = CodeCheckpointImpl(
            id = id,
            name = name,
            description = "自动创建的检查点",
            createdAt = Instant.now(),
            fileSnapshots = emptyList(),
            metadata = mapOf("auto" to true)
        )

        // 存储检查点
        checkpoints[id] = checkpoint

        return@withContext checkpoint
    }

    /**
     * 获取文件历史
     *
     * @param filePath 文件路径
     * @return 文件历史
     */
    override suspend fun getFileHistory(filePath: Path): List<FileHistoryEntry> = withContext(Dispatchers.IO) {
        logger.info { "获取文件历史: $filePath" }

        // 查找包含该文件的检查点
        val entries = checkpoints.values
            .filter { checkpoint -> checkpoint.getFileSnapshot(filePath) != null }
            .map { checkpoint ->
                val snapshot = checkpoint.getFileSnapshot(filePath)!!
                FileHistoryEntry(
                    checkpointId = checkpoint.id,
                    checkpointName = checkpoint.name,
                    timestamp = checkpoint.createdAt,
                    filePath = filePath,
                    hash = snapshot.hash
                )
            }
            .sortedByDescending { it.timestamp }

        return@withContext entries
    }

    /**
     * 恢复文件
     *
     * @param filePath 文件路径
     * @param checkpointId 检查点ID
     * @return 是否成功恢复
     */
    override suspend fun restoreFile(filePath: Path, checkpointId: String): Boolean = withContext(Dispatchers.IO) {
        logger.info { "恢复文件: $filePath, 检查点: $checkpointId" }

        val checkpoint = checkpoints[checkpointId] ?: return@withContext false
        val snapshot = checkpoint.getFileSnapshot(filePath) ?: return@withContext false

        // TODO: 实现文件恢复逻辑

        return@withContext true
    }

    /**
     * 清理过期检查点
     *
     * @param maxAge 最大年龄（秒）
     * @return 清理的检查点数量
     */
    override suspend fun cleanupExpiredCheckpoints(maxAge: Long): Int = withContext(Dispatchers.IO) {
        logger.info { "清理过期检查点: $maxAge 秒" }

        val now = Instant.now()
        val expiredIds = checkpoints.values
            .filter { checkpoint ->
                val age = now.epochSecond - checkpoint.createdAt.epochSecond
                age > maxAge
            }
            .map { it.id }

        // 删除过期检查点
        expiredIds.forEach { id ->
            checkpoints.remove(id)
        }

        return@withContext expiredIds.size
    }

    companion object {
        /**
         * 获取项目的检查点管理器实例
         *
         * @param project 项目
         * @return 检查点管理器实例
         */
        fun getInstance(project: Project): CheckpointManager {
            return project.service<CheckpointManagerImpl>()
        }
    }
}

/**
 * 代码检查点实现
 *
 * @property id 检查点ID
 * @property name 检查点名称
 * @property description 检查点描述
 * @property createdAt 创建时间
 * @property fileSnapshots 文件快照
 * @property metadata 元数据
 */
class CodeCheckpointImpl(
    override val id: String,
    override val name: String,
    override val description: String,
    override val createdAt: Instant,
    override val fileSnapshots: List<FileSnapshot>,
    override val metadata: Map<String, Any>
) : CodeCheckpoint {

    /**
     * 恢复检查点
     *
     * @return 是否成功恢复
     */
    override suspend fun restore(): Boolean = withContext(Dispatchers.IO) {
        // TODO: 实现检查点恢复逻辑
        return@withContext true
    }

    /**
     * 删除检查点
     *
     * @return 是否成功删除
     */
    override suspend fun delete(): Boolean = withContext(Dispatchers.IO) {
        // TODO: 实现检查点删除逻辑
        return@withContext true
    }

    /**
     * 获取文件快照
     *
     * @param filePath 文件路径
     * @return 文件快照
     */
    override fun getFileSnapshot(filePath: Path): FileSnapshot? {
        return fileSnapshots.find { it.filePath == filePath }
    }

    /**
     * 比较与另一个检查点的差异
     *
     * @param other 另一个检查点
     * @return 差异列表
     */
    override fun diff(other: CodeCheckpoint): List<FileDiff> {
        // 获取所有文件路径
        val allPaths = (fileSnapshots.map { it.filePath } + other.fileSnapshots.map { it.filePath }).toSet()

        // 比较每个文件
        return allPaths.mapNotNull { path ->
            val thisSnapshot = getFileSnapshot(path)
            val otherSnapshot = other.getFileSnapshot(path)

            when {
                thisSnapshot == null && otherSnapshot != null -> {
                    // 文件在当前检查点中不存在，但在另一个检查点中存在
                    FileDiff(
                        filePath = path,
                        changeType = ChangeType.ADDED,
                        oldContent = null,
                        newContent = otherSnapshot.content
                    )
                }
                thisSnapshot != null && otherSnapshot == null -> {
                    // 文件在当前检查点中存在，但在另一个检查点中不存在
                    FileDiff(
                        filePath = path,
                        changeType = ChangeType.DELETED,
                        oldContent = thisSnapshot.content,
                        newContent = null
                    )
                }
                thisSnapshot != null && otherSnapshot != null && thisSnapshot.hash != otherSnapshot.hash -> {
                    // 文件在两个检查点中都存在，但内容不同
                    FileDiff(
                        filePath = path,
                        changeType = ChangeType.MODIFIED,
                        oldContent = thisSnapshot.content,
                        newContent = otherSnapshot.content
                    )
                }
                else -> null // 文件相同，不返回差异
            }
        }
    }
}

/**
 * 变更类型
 */
enum class ChangeType {
    /**
     * 添加
     */
    ADDED,

    /**
     * 修改
     */
    MODIFIED,

    /**
     * 删除
     */
    DELETED
}

/**
 * 差异块
 *
 * @property startLine 开始行
 * @property endLine 结束行
 * @property lines 行列表
 */
data class DiffHunk(
    val startLine: Int,
    val endLine: Int,
    val lines: List<DiffLine>
)

/**
 * 差异行
 *
 * @property type 类型
 * @property content 内容
 */
data class DiffLine(
    val type: DiffLineType,
    val content: String
)

/**
 * 差异行类型
 */
enum class DiffLineType {
    /**
     * 上下文
     */
    CONTEXT,

    /**
     * 添加
     */
    ADDED,

    /**
     * 删除
     */
    DELETED
}
