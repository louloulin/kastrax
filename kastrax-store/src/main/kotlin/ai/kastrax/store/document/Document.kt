package ai.kastrax.store.document

/**
 * 文档类，表示一个文档。
 *
 * @property id 文档 ID
 * @property content 文档内容
 * @property metadata 文档元数据
 */
data class Document(
    val id: String,
    val content: String,
    val metadata: Map<String, Any> = emptyMap()
)
