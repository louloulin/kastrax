package ai.kastrax.code.memory

/**
 * 内存ID
 *
 * @property id ID
 * @property type 类型
 * @property namespace 命名空间
 */
data class MemoryId(
    val id: String,
    val type: MemoryType,
    val namespace: String
)
