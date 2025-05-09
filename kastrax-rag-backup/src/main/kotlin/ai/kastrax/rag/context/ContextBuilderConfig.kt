package ai.kastrax.rag.context

/**
 * 上下文构建器配置。
 *
 * @property maxTokens 上下文的最大令牌数，默认为 4000
 * @property separator 文档分隔符，默认为两个换行符
 * @property headerTemplate 上下文头部模板，默认为空。可以使用 {query} 占位符
 * @property footerTemplate 上下文尾部模板，默认为空。可以使用 {query} 占位符
 * @property includeMetadata 是否包含元数据，默认为 false
 * @property compressionEnabled 是否启用压缩，默认为 false
 * @property compressionRatio 压缩比例，默认为 0.7
 */
data class ContextBuilderConfig(
    val maxTokens: Int = 4000,
    val separator: String = "\n\n",
    val headerTemplate: String = "",
    val footerTemplate: String = "",
    val includeMetadata: Boolean = false,
    val compressionEnabled: Boolean = false,
    val compressionRatio: Double = 0.7
)
