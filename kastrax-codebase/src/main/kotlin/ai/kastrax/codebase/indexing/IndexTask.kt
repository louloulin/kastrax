package ai.kastrax.codebase.indexing

import java.nio.file.Path

/**
 * 索引任务
 *
 * @property id 任务ID
 * @property type 任务类型
 * @property path 文件路径
 * @property priority 优先级
 * @property metadata 元数据
 */
data class IndexTask(
    val id: String,
    val type: IndexTaskType,
    val path: Path,
    val priority: Int = 0,
    val metadata: Map<String, String> = emptyMap()
)
