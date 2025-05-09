package ai.kastrax.rag.document.transform

import ai.kastrax.store.document.Document

/**
 * 文档清洁器，用于清理文档内容。
 *
 * @property removeExtraWhitespace 是否移除多余的空白字符
 * @property removeUrls 是否移除 URL
 * @property removeEmails 是否移除电子邮件地址
 * @property removeHtmlTags 是否移除 HTML 标签
 * @property removeSpecialChars 是否移除特殊字符
 * @property toLowerCase 是否转换为小写
 */
class DocumentCleaner(
    private val removeExtraWhitespace: Boolean = true,
    private val removeUrls: Boolean = false,
    private val removeEmails: Boolean = false,
    private val removeHtmlTags: Boolean = true,
    private val removeSpecialChars: Boolean = false,
    private val toLowerCase: Boolean = false
) : DocumentTransformer {
    /**
     * 转换文档。
     *
     * @param document 文档
     * @return 转换后的文档
     */
    override fun transform(document: Document): Document {
        var content = document.content
        
        // 移除 HTML 标签
        if (removeHtmlTags) {
            content = content.replace(Regex("<[^>]*>"), "")
        }
        
        // 移除 URL
        if (removeUrls) {
            content = content.replace(Regex("https?://\\S+"), "")
            content = content.replace(Regex("www\\.\\S+"), "")
        }
        
        // 移除电子邮件地址
        if (removeEmails) {
            content = content.replace(Regex("\\S+@\\S+\\.\\S+"), "")
        }
        
        // 移除特殊字符
        if (removeSpecialChars) {
            content = content.replace(Regex("[^\\w\\s]"), "")
        }
        
        // 移除多余的空白字符
        if (removeExtraWhitespace) {
            content = content.replace(Regex("\\s+"), " ")
            content = content.trim()
        }
        
        // 转换为小写
        if (toLowerCase) {
            content = content.lowercase()
        }
        
        return Document(
            id = document.id,
            content = content,
            metadata = document.metadata + mapOf("cleaned" to true)
        )
    }
}
