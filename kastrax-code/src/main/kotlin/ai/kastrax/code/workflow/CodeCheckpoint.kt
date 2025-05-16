package ai.kastrax.code.workflow

import java.nio.file.Path
import java.time.Instant

/**
 * 代码检查点接口
 *
 * 定义代码检查点的基本操作
 */
interface CodeCheckpoint {
    /**
     * 检查点ID
     */
    val id: String
    
    /**
     * 检查点名称
     */
    val name: String
    
    /**
     * 检查点描述
     */
    val description: String
    
    /**
     * 创建时间
     */
    val createdAt: Instant
    
    /**
     * 文件快照
     */
    val fileSnapshots: List<FileSnapshot>
    
    /**
     * 元数据
     */
    val metadata: Map<String, Any>
    
    /**
     * 恢复检查点
     *
     * @return 是否成功恢复
     */
    suspend fun restore(): Boolean
    
    /**
     * 删除检查点
     *
     * @return 是否成功删除
     */
    suspend fun delete(): Boolean
    
    /**
     * 获取文件快照
     *
     * @param filePath 文件路径
     * @return 文件快照
     */
    fun getFileSnapshot(filePath: Path): FileSnapshot?
    
    /**
     * 比较与另一个检查点的差异
     *
     * @param other 另一个检查点
     * @return 差异列表
     */
    fun diff(other: CodeCheckpoint): List<FileDiff>
}

/**
 * 文件快照
 *
 * @property filePath 文件路径
 * @property content 文件内容
 * @property hash 文件哈希
 * @property metadata 元数据
 */
data class FileSnapshot(
    val filePath: Path,
    val content: String,
    val hash: String,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * 文件差异
 *
 * @property filePath 文件路径
 * @property changeType 变更类型
 * @property oldContent 旧内容
 * @property newContent 新内容
 * @property hunks 差异块列表
 */
data class FileDiff(
    val filePath: Path,
    val changeType: ChangeType,
    val oldContent: String?,
    val newContent: String?,
    val hunks: List<DiffHunk> = emptyList()
)

/**
 * 差异块
 *
 * @property oldStart 旧内容起始行
 * @property oldCount 旧内容行数
 * @property newStart 新内容起始行
 * @property newCount 新内容行数
 * @property lines 差异行列表
 */
data class DiffHunk(
    val oldStart: Int,
    val oldCount: Int,
    val newStart: Int,
    val newCount: Int,
    val lines: List<DiffLine>
)

/**
 * 差异行
 *
 * @property type 行类型
 * @property content 行内容
 * @property oldLineNumber 旧行号
 * @property newLineNumber 新行号
 */
data class DiffLine(
    val type: DiffLineType,
    val content: String,
    val oldLineNumber: Int?,
    val newLineNumber: Int?
)

/**
 * 差异行类型
 */
enum class DiffLineType {
    /**
     * 上下文行
     */
    CONTEXT,
    
    /**
     * 添加行
     */
    ADDED,
    
    /**
     * 删除行
     */
    REMOVED
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
    DELETED,
    
    /**
     * 重命名
     */
    RENAMED,
    
    /**
     * 未变更
     */
    UNCHANGED
}
