package ai.kastrax.rag.document.transform

import ai.kastrax.store.document.Document
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/**
 * HTML 到文本转换器，用于将 HTML 内容转换为纯文本。
 *
 * @property preserveNewlines 是否保留换行符
 * @property includeLinks 是否包含链接
 * @property includeImages 是否包含图像
 * @property includeAltText 是否包含替代文本
 * @property includeMetadata 是否包含元数据
 */
class HtmlToTextConverter(
    private val preserveNewlines: Boolean = true,
    private val includeLinks: Boolean = true,
    private val includeImages: Boolean = true,
    private val includeAltText: Boolean = true,
    private val includeMetadata: Boolean = true
) : DocumentTransformer {
    /**
     * 转换文档。
     *
     * @param document 文档
     * @return 转换后的文档
     */
    override fun transform(document: Document): Document {
        val content = document.content
        
        // 解析 HTML
        val doc = Jsoup.parse(content)
        
        // 提取元数据
        val metadata = mutableMapOf<String, Any>()
        if (includeMetadata) {
            // 提取标题
            doc.title()?.let { title ->
                if (title.isNotBlank()) {
                    metadata["title"] = title
                }
            }
            
            // 提取元标签
            doc.select("meta").forEach { meta ->
                val name = meta.attr("name").takeIf { it.isNotBlank() } ?: meta.attr("property")
                val content = meta.attr("content")
                if (name.isNotBlank() && content.isNotBlank()) {
                    metadata[name] = content
                }
            }
        }
        
        // 转换为文本
        val text = extractText(doc.body())
        
        return Document(
            id = document.id,
            content = text,
            metadata = document.metadata + metadata + mapOf("html_converted" to true)
        )
    }
    
    /**
     * 从元素中提取文本。
     *
     * @param element 元素
     * @return 提取的文本
     */
    private fun extractText(element: Element?): String {
        if (element == null) {
            return ""
        }
        
        val buffer = StringBuilder()
        
        // 处理子节点
        for (node in element.childNodes()) {
            when (node) {
                is TextNode -> {
                    buffer.append(node.text())
                }
                is Element -> {
                    // 处理特殊元素
                    when (node.tagName().lowercase()) {
                        "br" -> {
                            if (preserveNewlines) {
                                buffer.append("\n")
                            } else {
                                buffer.append(" ")
                            }
                        }
                        "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li" -> {
                            val text = extractText(node)
                            if (text.isNotBlank()) {
                                buffer.append(text)
                                if (preserveNewlines) {
                                    buffer.append("\n\n")
                                } else {
                                    buffer.append(" ")
                                }
                            }
                        }
                        "a" -> {
                            val text = extractText(node)
                            if (text.isNotBlank()) {
                                buffer.append(text)
                                if (includeLinks) {
                                    val href = node.attr("href")
                                    if (href.isNotBlank()) {
                                        buffer.append(" (")
                                        buffer.append(href)
                                        buffer.append(")")
                                    }
                                }
                                buffer.append(" ")
                            }
                        }
                        "img" -> {
                            if (includeImages) {
                                if (includeAltText) {
                                    val alt = node.attr("alt")
                                    if (alt.isNotBlank()) {
                                        buffer.append("[Image: ")
                                        buffer.append(alt)
                                        buffer.append("] ")
                                    }
                                }
                                val src = node.attr("src")
                                if (src.isNotBlank()) {
                                    buffer.append("[Image URL: ")
                                    buffer.append(src)
                                    buffer.append("] ")
                                }
                            }
                        }
                        else -> {
                            val text = extractText(node)
                            if (text.isNotBlank()) {
                                buffer.append(text)
                                buffer.append(" ")
                            }
                        }
                    }
                }
            }
        }
        
        return buffer.toString().trim()
    }
}
