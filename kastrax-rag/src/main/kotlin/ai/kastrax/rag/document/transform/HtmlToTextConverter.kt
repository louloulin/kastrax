package ai.kastrax.rag.document.transform

import ai.kastrax.rag.document.Document
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor

private val logger = KotlinLogging.logger {}

/**
 * HTML 到纯文本的转换器，将 HTML 文档转换为纯文本，同时尽可能保留文本结构和格式信息。
 */
class HtmlToTextConverter {

    /**
     * 将 HTML 文档转换为纯文本文档。
     *
     * @param document 包含 HTML 内容的文档
     * @param options 转换选项
     * @return 转换后的纯文本文档
     */
    fun convert(document: Document, options: ConversionOptions = ConversionOptions()): Document {
        val convertedText = convert(document.content, options)
        return Document(convertedText, document.metadata)
    }

    /**
     * 将 HTML 字符串转换为纯文本。
     *
     * @param html HTML 字符串
     * @param options 转换选项
     * @return 转换后的纯文本
     */
    fun convert(html: String, options: ConversionOptions = ConversionOptions()): String {
        try {
            // 解析 HTML
            val jsoupDoc = Jsoup.parse(html)

            // 如果需要，移除不需要的元素
            if (options.removeScriptAndStyleElements) {
                jsoupDoc.select("script, style, iframe, noscript").remove()
            }

            // 如果需要，移除注释
            if (options.removeComments) {
                removeComments(jsoupDoc)
            }

            // 如果需要，移除隐藏元素
            if (options.removeHiddenElements) {
                jsoupDoc.select("[style*=display:none], [style*=visibility:hidden], [hidden]").remove()
            }

            // 转换为纯文本
            val formatter = if (options.preserveLineBreaks) {
                FormattingVisitor(options)
            } else {
                SimpleFormattingVisitor(options)
            }

            NodeTraversor.traverse(formatter, jsoupDoc.body())

            return formatter.toString()
        } catch (e: Exception) {
            logger.error(e) { "Error converting HTML to text" }
            return html  // 如果转换失败，返回原始 HTML
        }
    }

    /**
     * 移除 HTML 文档中的注释节点。
     *
     * @param element 要处理的元素
     */
    private fun removeComments(element: Element) {
        val it = element.childNodes().iterator()
        while (it.hasNext()) {
            val node = it.next()
            if (node.nodeName() == "#comment") {
                it.remove()
            } else if (node is Element) {
                removeComments(node)
            }
        }
    }

    /**
     * 格式化访问者，用于遍历 HTML 文档并生成格式化的纯文本。
     * 这个版本保留换行符和基本格式。
     */
    private class FormattingVisitor(private val options: ConversionOptions) : NodeVisitor {
        private val buffer = StringBuilder()
        private var lastWasBlock = true
        private var listLevel = 0

        override fun head(node: Node, depth: Int) {
            val name = node.nodeName()

            when {
                node is TextNode -> append(node.text())

                name == "li" -> {
                    if (!lastWasBlock) {
                        append("\n")
                    }
                    append("  ".repeat(listLevel))
                    append("• ")
                }

                name == "ol" || name == "ul" -> {
                    if (!lastWasBlock) {
                        append("\n")
                    }
                    listLevel++
                }

                name in BLOCK_TAGS -> {
                    if (!lastWasBlock) {
                        append("\n")
                    }

                    // 添加标题标记
                    if (name.matches(Regex("h[1-6]"))) {
                        val level = name.substring(1).toInt()
                        if (options.preserveHeaderFormatting) {
                            append("#".repeat(level) + " ")
                        }
                    }

                    // 添加水平线
                    if (name == "hr" && options.preserveHorizontalRules) {
                        append("-------------------\n")
                    }
                }

                name == "br" -> append("\n")

                name == "p" && options.addExtraLineForParagraphs -> append("\n")

                name == "a" && options.preserveLinks -> {
                    val href = (node as Element).attr("href")
                    if (href.isNotEmpty()) {
                        buffer.append("[")
                    }
                }

                name == "img" && options.includeImageAltText -> {
                    val alt = (node as Element).attr("alt")
                    if (alt.isNotEmpty()) {
                        append("[Image: $alt]")
                    }
                }

                name == "table" && options.preserveTableStructure -> {
                    if (!lastWasBlock) {
                        append("\n")
                    }
                }

                name == "tr" && options.preserveTableStructure -> {
                    if (!lastWasBlock) {
                        append("\n")
                    }
                    append("| ")

                    // 如果是表头行，添加分隔行
                    if (node.parent()?.nodeName() == "thead") {
                        val headerRow = node
                        val cells = (headerRow as Element).select("th").size
                        lastWasBlock = true  // 设置为 true，以便在 tail 中添加分隔行
                    }
                }

                name == "th" && options.preserveTableStructure -> {
                    // 加粗表头
                    if (options.preserveTextFormatting) {
                        buffer.append("**")
                    }
                }

                name == "td" && options.preserveTableStructure -> append("")

                name in FORMATTING_TAGS && options.preserveTextFormatting -> {
                    when (name) {
                        "b", "strong" -> buffer.append("**")
                        "i", "em" -> buffer.append("*")
                        "u" -> buffer.append("_")
                        "code" -> buffer.append("`")
                        "pre" -> buffer.append("```\n")
                    }
                }
            }
        }

        override fun tail(node: Node, depth: Int) {
            val name = node.nodeName()

            when {
                name in BLOCK_TAGS -> {
                    append("\n")
                    lastWasBlock = true
                }

                name == "ol" || name == "ul" -> {
                    append("\n")
                    listLevel--
                    lastWasBlock = true
                }

                name == "a" && options.preserveLinks -> {
                    val href = (node as Element).attr("href")
                    if (href.isNotEmpty()) {
                        buffer.append("](")
                        buffer.append(href)
                        buffer.append(")")
                    }
                }

                name == "th" && options.preserveTableStructure -> {
                    if (options.preserveTextFormatting) {
                        buffer.append("**")
                    }
                    buffer.append(" | ")
                }

                name == "td" && options.preserveTableStructure -> buffer.append(" | ")

                name == "tr" && options.preserveTableStructure -> {
                    append("\n")

                    // 如果是表头行的下一行，添加分隔行
                    val prevSibling = node.previousSibling()
                    if (prevSibling != null && prevSibling.nodeName() == "tr") {
                        val prevParent = prevSibling.parent()
                        if (prevParent != null && prevParent.nodeName() == "thead") {
                            // 添加表格分隔行
                            val cells = (prevSibling as Element).select("th").size
                            append("| ")
                            append("--- | ".repeat(cells))
                            append("\n| ")
                        }
                    }
                }

                name in FORMATTING_TAGS && options.preserveTextFormatting -> {
                    when (name) {
                        "b", "strong" -> buffer.append("**")
                        "i", "em" -> buffer.append("*")
                        "u" -> buffer.append("_")
                        "code" -> buffer.append("`")
                        "pre" -> buffer.append("\n```")
                    }
                }

                else -> lastWasBlock = false
            }
        }

        /**
         * 添加文本到缓冲区，处理空白字符。
         *
         * @param text 要添加的文本
         */
        private fun append(text: String) {
            if (text.isEmpty()) {
                return
            }

            // 如果上一个是块级元素，并且文本以空白开头，去除开头的空白
            if (lastWasBlock && text[0].isWhitespace()) {
                buffer.append(text.trimStart())
            } else {
                buffer.append(text)
            }

            lastWasBlock = false
        }

        override fun toString(): String {
            return buffer.toString().trim()
        }
    }

    /**
     * 简单格式化访问者，用于生成简单的纯文本，不保留换行符和格式。
     */
    private class SimpleFormattingVisitor(private val options: ConversionOptions) : NodeVisitor {
        private val buffer = StringBuilder()

        override fun head(node: Node, depth: Int) {
            if (node is TextNode) {
                buffer.append(node.text())
            } else if (node.nodeName() == "img" && options.includeImageAltText) {
                val alt = (node as Element).attr("alt")
                if (alt.isNotEmpty()) {
                    buffer.append("[Image: $alt]")
                }
            }
        }

        override fun tail(node: Node, depth: Int) {
            val name = node.nodeName()

            if (name in BLOCK_TAGS || name == "br" || name == "li") {
                buffer.append(" ")
            }
        }

        override fun toString(): String {
            return buffer.toString().trim().replace(Regex("\\s+"), " ")
        }
    }

    /**
     * HTML 到文本转换选项。
     *
     * @property preserveLineBreaks 是否保留换行符
     * @property preserveHeaderFormatting 是否保留标题格式
     * @property preserveTextFormatting 是否保留文本格式（粗体、斜体等）
     * @property preserveLinks 是否保留链接
     * @property preserveTableStructure 是否保留表格结构
     * @property preserveHorizontalRules 是否保留水平线
     * @property includeImageAltText 是否包含图片的 alt 文本
     * @property removeScriptAndStyleElements 是否移除脚本和样式元素
     * @property removeComments 是否移除注释
     * @property removeHiddenElements 是否移除隐藏元素
     * @property addExtraLineForParagraphs 是否为段落添加额外的换行符
     */
    data class ConversionOptions(
        val preserveLineBreaks: Boolean = true,
        val preserveHeaderFormatting: Boolean = true,
        val preserveTextFormatting: Boolean = true,
        val preserveLinks: Boolean = true,
        val preserveTableStructure: Boolean = true,
        val preserveHorizontalRules: Boolean = true,
        val includeImageAltText: Boolean = true,
        val removeScriptAndStyleElements: Boolean = true,
        val removeComments: Boolean = true,
        val removeHiddenElements: Boolean = true,
        val addExtraLineForParagraphs: Boolean = false
    ) {
        /**
         * 创建一个新的 ConversionOptions 构建器。
         *
         * @return ConversionOptionsBuilder 实例
         */
        fun toBuilder(): ConversionOptionsBuilder {
            return ConversionOptionsBuilder(this)
        }

        /**
         * ConversionOptions 构建器，用于链式配置转换选项。
         */
        class ConversionOptionsBuilder(options: ConversionOptions) {
            private var preserveLineBreaks = options.preserveLineBreaks
            private var preserveHeaderFormatting = options.preserveHeaderFormatting
            private var preserveTextFormatting = options.preserveTextFormatting
            private var preserveLinks = options.preserveLinks
            private var preserveTableStructure = options.preserveTableStructure
            private var preserveHorizontalRules = options.preserveHorizontalRules
            private var includeImageAltText = options.includeImageAltText
            private var removeScriptAndStyleElements = options.removeScriptAndStyleElements
            private var removeComments = options.removeComments
            private var removeHiddenElements = options.removeHiddenElements
            private var addExtraLineForParagraphs = options.addExtraLineForParagraphs

            fun preserveLineBreaks(value: Boolean) = apply { preserveLineBreaks = value }
            fun preserveHeaderFormatting(value: Boolean) = apply { preserveHeaderFormatting = value }
            fun preserveTextFormatting(value: Boolean) = apply { preserveTextFormatting = value }
            fun preserveLinks(value: Boolean) = apply { preserveLinks = value }
            fun preserveTableStructure(value: Boolean) = apply { preserveTableStructure = value }
            fun preserveHorizontalRules(value: Boolean) = apply { preserveHorizontalRules = value }
            fun includeImageAltText(value: Boolean) = apply { includeImageAltText = value }
            fun removeScriptAndStyleElements(value: Boolean) = apply { removeScriptAndStyleElements = value }
            fun removeComments(value: Boolean) = apply { removeComments = value }
            fun removeHiddenElements(value: Boolean) = apply { removeHiddenElements = value }
            fun addExtraLineForParagraphs(value: Boolean) = apply { addExtraLineForParagraphs = value }

            fun build(): ConversionOptions {
                return ConversionOptions(
                    preserveLineBreaks = preserveLineBreaks,
                    preserveHeaderFormatting = preserveHeaderFormatting,
                    preserveTextFormatting = preserveTextFormatting,
                    preserveLinks = preserveLinks,
                    preserveTableStructure = preserveTableStructure,
                    preserveHorizontalRules = preserveHorizontalRules,
                    includeImageAltText = includeImageAltText,
                    removeScriptAndStyleElements = removeScriptAndStyleElements,
                    removeComments = removeComments,
                    removeHiddenElements = removeHiddenElements,
                    addExtraLineForParagraphs = addExtraLineForParagraphs
                )
            }
        }
    }

    companion object {
        /**
         * 块级 HTML 标签列表。
         */
        private val BLOCK_TAGS = setOf(
            "address", "article", "aside", "blockquote", "canvas", "dd", "div",
            "dl", "dt", "fieldset", "figcaption", "figure", "footer", "form",
            "h1", "h2", "h3", "h4", "h5", "h6", "header", "hr", "li", "main",
            "nav", "noscript", "ol", "p", "pre", "section", "table", "tfoot",
            "ul", "video"
        )

        /**
         * 格式化 HTML 标签列表。
         */
        private val FORMATTING_TAGS = setOf(
            "b", "strong", "i", "em", "u", "code", "pre"
        )

        /**
         * 创建一个预设的转换选项，用于简单转换。
         *
         * @return 简单转换选项
         */
        fun simpleConversionOptions(): ConversionOptions {
            return ConversionOptions(
                preserveLineBreaks = false,
                preserveHeaderFormatting = false,
                preserveTextFormatting = false,
                preserveLinks = false,
                preserveTableStructure = false,
                preserveHorizontalRules = false,
                includeImageAltText = true,
                removeScriptAndStyleElements = true,
                removeComments = true,
                removeHiddenElements = true,
                addExtraLineForParagraphs = false
            )
        }

        /**
         * 创建一个预设的转换选项，用于保留结构。
         *
         * @return 保留结构选项
         */
        fun structurePreservingOptions(): ConversionOptions {
            return ConversionOptions(
                preserveLineBreaks = true,
                preserveHeaderFormatting = true,
                preserveTextFormatting = false,
                preserveLinks = false,
                preserveTableStructure = true,
                preserveHorizontalRules = true,
                includeImageAltText = true,
                removeScriptAndStyleElements = true,
                removeComments = true,
                removeHiddenElements = true,
                addExtraLineForParagraphs = true
            )
        }

        /**
         * 创建一个预设的转换选项，用于 Markdown 转换。
         *
         * @return Markdown 转换选项
         */
        fun markdownConversionOptions(): ConversionOptions {
            return ConversionOptions(
                preserveLineBreaks = true,
                preserveHeaderFormatting = true,
                preserveTextFormatting = true,
                preserveLinks = true,
                preserveTableStructure = true,
                preserveHorizontalRules = true,
                includeImageAltText = true,
                removeScriptAndStyleElements = true,
                removeComments = true,
                removeHiddenElements = true,
                addExtraLineForParagraphs = true
            )
        }
    }
}
