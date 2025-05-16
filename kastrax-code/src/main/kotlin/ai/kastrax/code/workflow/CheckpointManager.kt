package ai.kastrax.code.workflow

import java.nio.file.Path
import java.time.Instant

/**
 * 检查点管理器接口
 *
 * 定义检查点管理器的基本操作
 */
interface CheckpointManager {
    /**
     * 创建检查点
     *
     * @param name 检查点名称
     * @param description 检查点描述
     * @param metadata 元数据
     * @return 创建的检查点
     */
    suspend fun createCheckpoint(
        name: String,
        description: String = "",
        metadata: Map<String, Any> = emptyMap()
    ): CodeCheckpoint
    
    /**
     * 获取检查点
     *
     * @param id 检查点ID
     * @return 检查点
     */
    suspend fun getCheckpoint(id: String): CodeCheckpoint?
    
    /**
     * 获取所有检查点
     *
     * @return 检查点列表
     */
    suspend fun getAllCheckpoints(): List<CodeCheckpoint>
    
    /**
     * 获取最近的检查点
     *
     * @param limit 限制数量
     * @return 检查点列表
     */
    suspend fun getRecentCheckpoints(limit: Int = 10): List<CodeCheckpoint>
    
    /**
     * 恢复检查点
     *
     * @param id 检查点ID
     * @return 是否成功恢复
     */
    suspend fun restoreCheckpoint(id: String): Boolean
    
    /**
     * 删除检查点
     *
     * @param id 检查点ID
     * @return 是否成功删除
     */
    suspend fun deleteCheckpoint(id: String): Boolean
    
    /**
     * 比较检查点
     *
     * @param id1 检查点1 ID
     * @param id2 检查点2 ID
     * @return 差异列表
     */
    suspend fun compareCheckpoints(id1: String, id2: String): List<FileDiff>
    
    /**
     * 创建自动检查点
     *
     * @return 创建的检查点
     */
    suspend fun createAutoCheckpoint(): CodeCheckpoint
    
    /**
     * 获取文件历史
     *
     * @param filePath 文件路径
     * @return 文件历史
     */
    suspend fun getFileHistory(filePath: Path): List<FileHistoryEntry>
    
    /**
     * 恢复文件
     *
     * @param filePath 文件路径
     * @param checkpointId 检查点ID
     * @return 是否成功恢复
     */
    suspend fun restoreFile(filePath: Path, checkpointId: String): Boolean
    
    /**
     * 清理过期检查点
     *
     * @param maxAge 最大年龄（秒）
     * @return 清理的检查点数量
     */
    suspend fun cleanupExpiredCheckpoints(maxAge: Long): Int
}

/**
 * 文件历史条目
 *
 * @property checkpointId 检查点ID
 * @property checkpointName 检查点名称
 * @property timestamp 时间戳
 * @property filePath 文件路径
 * @property hash 文件哈希
 */
data class FileHistoryEntry(
    val checkpointId: String,
    val checkpointName: String,
    val timestamp: Instant,
    val filePath: Path,
    val hash: String
)
