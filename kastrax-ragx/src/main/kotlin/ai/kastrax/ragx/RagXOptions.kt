package ai.kastrax.ragx

/**
 * RagX 选项。
 *
 * @property indexName 索引名称
 * @property contextFormat 上下文格式
 * @property maxContextLength 最大上下文长度
 * @property includeMetadata 是否包含元数据
 * @property metadataFields 元数据字段
 * @property separator 分隔符
 */
data class RagXOptions(
    val indexName: String = "default",
    val contextFormat: ContextFormat = ContextFormat.TEXT,
    val maxContextLength: Int = 4000,
    val includeMetadata: Boolean = false,
    val metadataFields: List<String> = emptyList(),
    val separator: String = "\n\n"
)

/**
 * 上下文格式。
 */
enum class ContextFormat {
    /**
     * 纯文本格式。
     */
    TEXT,
    
    /**
     * Markdown 格式。
     */
    MARKDOWN,
    
    /**
     * JSON 格式。
     */
    JSON
}
