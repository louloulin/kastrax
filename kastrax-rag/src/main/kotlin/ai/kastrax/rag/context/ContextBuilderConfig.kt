package ai.kastrax.rag.context

/**
 * 上下文构建配置。
 *
 * @property maxTokens 最大令牌数
 * @property format 上下文格式
 * @property includeMetadata 是否包含元数据
 * @property metadataFields 元数据字段
 * @property separator 分隔符
 */
data class ContextBuilderConfig(
    val maxTokens: Int = 4000,
    val format: ContextFormat = ContextFormat.TEXT,
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
