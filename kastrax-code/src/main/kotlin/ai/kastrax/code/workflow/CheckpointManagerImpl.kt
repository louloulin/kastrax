package ai.kastrax.code.workflow

import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.codebase.CodebaseIndexManager
import ai.kastrax.codebase.semantic.CodeSemanticAnalyzer
import ai.kastrax.codebase.semantic.model.CodeElement
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 检查点管理器实现
 *
 * 基于 kastrax-codebase 实现的检查点管理器
 *
 * @property project 项目
 */
@Service(Service.Level.PROJECT)
class CheckpointManagerImpl(
    private val project: Project
) : CheckpointManager, KastraXCodeBase(component = "CHECKPOINT_MANAGER") {

    // 检查点存储
    private val checkpoints = ConcurrentHashMap<String, CodeCheckpointImpl>()

    // 代码语义分析器
    private val codeAnalyzer = CodeSemanticAnalyzer()

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

        // 创建检查点ID
        val id = UUID.randomUUID().toString()

        // 获取项目根路径
        val projectPath = Paths.get(project.basePath ?: "")

        // 创建文件快照
        val fileSnapshots = createFileSnapshots(projectPath)

        // 创建检查点
        val checkpoint = CodeCheckpointImpl(
            id = id,
            description = description,
            createdAt = Instant.now(),
            fileSnapshots = fileSnapshots,
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

        try {
            // 恢复所有文件
            for (snapshot in checkpoint.fileSnapshots) {
                restoreFileFromSnapshot(snapshot)
            }

            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "恢复检查点时出错: $id" }
            return@withContext false
        }
    }

    /**
     * 删除检查点
     *
     * @param id 检查点ID
     * @return 是否成功删除
     */
    override suspend fun deleteCheckpoint(id: String): Boolean = withContext(Dispatchers.IO) {
        logger.info { "删除检查点: $id" }

        return@withContext checkpoints.remove(id) != null
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
        return@withContext createCheckpoint(
            name = "自动检查点 ${Instant.now()}",
            description = "自动创建的检查点",
            metadata = mapOf("auto" to true)
        )
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

        try {
            // 恢复文件
            restoreFileFromSnapshot(snapshot)

            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "恢复文件时出错: $filePath" }
            return@withContext false
        }
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

    /**
     * 创建文件快照
     *
     * @param rootPath 根路径
     * @return 文件快照列表
     */
    private suspend fun createFileSnapshots(rootPath: Path): List<FileSnapshot> = withContext(Dispatchers.IO) {
        val snapshots = mutableListOf<FileSnapshot>()

        try {
            // 遍历项目文件
            Files.walk(rootPath)
                .filter { path -> Files.isRegularFile(path) }
                .filter { path -> isRelevantFile(path) }
                .forEach { path ->
                    try {
                        // 读取文件内容
                        val content = Files.readString(path)

                        // 计算文件哈希
                        val hash = calculateFileHash(content)

                        // 创建文件快照
                        val snapshot = FileSnapshot(
                            filePath = path,
                            content = content,
                            hash = hash
                        )

                        snapshots.add(snapshot)
                    } catch (e: Exception) {
                        logger.error(e) { "创建文件快照时出错: $path" }
                    }
                }
        } catch (e: Exception) {
            logger.error(e) { "遍历项目文件时出错: $rootPath" }
        }

        return@withContext snapshots
    }

    /**
     * 判断文件是否相关
     *
     * @param path 文件路径
     * @return 是否相关
     */
    private fun isRelevantFile(path: Path): Boolean {
        // 排除二进制文件、临时文件等
        val fileName = path.fileName.toString()
        val extension = fileName.substringAfterLast('.', "")

        // 排除隐藏文件和目录
        if (fileName.startsWith(".")) {
            return false
        }

        // 排除构建目录
        val pathStr = path.toString()
        if (pathStr.contains("/build/") || pathStr.contains("/target/") || pathStr.contains("/out/")) {
            return false
        }

        // 排除不相关的文件类型
        val excludedExtensions = setOf(
            "class", "jar", "war", "zip", "tar", "gz", "rar",
            "jpg", "jpeg", "png", "gif", "bmp", "ico",
            "mp3", "mp4", "avi", "mov", "wmv",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
        )

        if (extension in excludedExtensions) {
            return false
        }

        return true
    }

    /**
     * 计算文件哈希
     *
     * @param content 文件内容
     * @return 文件哈希
     */
    private fun calculateFileHash(content: String): String {
        // 使用简单的哈希算法
        return content.hashCode().toString(16)
    }

    /**
     * 从快照恢复文件
     *
     * @param snapshot 文件快照
     */
    private fun restoreFileFromSnapshot(snapshot: FileSnapshot) {
        // 确保目录存在
        Files.createDirectories(snapshot.filePath.parent)

        // 写入文件内容
        Files.writeString(snapshot.filePath, snapshot.content)
    }

    /**
     * 分析代码文件
     *
     * @param filePath 文件路径
     * @return 代码元素
     */
    private suspend fun analyzeCodeFile(filePath: Path): CodeElement = withContext(Dispatchers.IO) {
        return@withContext codeAnalyzer.analyzeFile(filePath)
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
    override val description: String,
    override val createdAt: Instant,
    override val fileSnapshots: List<FileSnapshot>,
    override val metadata: Map<String, Any>
) : CodeCheckpoint, KastraXCodeBase(component = "CODE_CHECKPOINT", name = "CodeCheckpoint") {

    /**
     * 恢复检查点
     *
     * @return 是否成功恢复
     */
    override suspend fun restore(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 恢复所有文件
            for (snapshot in fileSnapshots) {
                // 确保目录存在
                Files.createDirectories(snapshot.filePath.parent)

                // 写入文件内容
                Files.writeString(snapshot.filePath, snapshot.content)
            }

            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "恢复检查点时出错: $id" }
            return@withContext false
        }
    }

    /**
     * 删除检查点
     *
     * @return 是否成功删除
     */
    override suspend fun delete(): Boolean = withContext(Dispatchers.IO) {
        // 检查点删除只需要从存储中移除，不需要删除文件
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
