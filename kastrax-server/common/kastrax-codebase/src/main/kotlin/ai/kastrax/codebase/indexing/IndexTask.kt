package ai.kastrax.codebase.indexing

import java.nio.file.Path
import java.util.UUID

/**
 * 索引任务类型
 */
enum class IndexTaskType {
    /**
     * 添加文件
     */
    ADD,
    
    /**
     * 更新文件
     */
    UPDATE,
    
    /**
     * 删除文件
     */
    DELETE,
    
    /**
     * 分支变更
     */
    BRANCH_CHANGE,
    
    /**
     * 完全重新索引
     */
    FULL_REINDEX
}

/**
 * 索引任务
 *
 * @property id 任务ID
 * @property type 任务类型
 * @property path 文件路径
 * @property metadata 元数据
 */
open class IndexTask(
    val id: String = UUID.randomUUID().toString(),
    val type: IndexTaskType,
    val path: Path,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 添加文件任务
 *
 * @property filePath 文件路径
 * @property content 文件内容
 */
class AddFileTask(
    val filePath: Path,
    val content: String,
    metadata: Map<String, String> = emptyMap()
) : IndexTask(
    type = IndexTaskType.ADD,
    path = filePath,
    metadata = metadata
)

/**
 * 更新文件任务
 *
 * @property filePath 文件路径
 * @property content 文件内容
 */
class UpdateFileTask(
    val filePath: Path,
    val content: String,
    metadata: Map<String, String> = emptyMap()
) : IndexTask(
    type = IndexTaskType.UPDATE,
    path = filePath,
    metadata = metadata
)

/**
 * 删除文件任务
 *
 * @property filePath 文件路径
 */
class DeleteFileTask(
    val filePath: Path,
    metadata: Map<String, String> = emptyMap()
) : IndexTask(
    type = IndexTaskType.DELETE,
    path = filePath,
    metadata = metadata
)

/**
 * 索引任务结果
 *
 * @property success 是否成功
 * @property message 消息
 * @property metadata 元数据
 */
data class IndexTaskResult(
    val success: Boolean,
    val message: String,
    val metadata: Map<String, Any> = emptyMap()
)
