package ai.kastrax.rag.document.transform

import ai.kastrax.rag.document.Document
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 文本替换转换器，用于替换文档内容中的文本。
 *
 * @property oldText 要替换的文本
 * @property newText 替换为的文本
 * @property caseSensitive 是否区分大小写
 * @property replaceAll 是否替换所有匹配项
 */
class TextReplaceTransformer(
    private val oldText: String,
    private val newText: String,
    private val caseSensitive: Boolean = true,
    private val replaceAll: Boolean = true
) : DocumentTransformer {
    override fun transform(document: Document): Document {
        val content = document.content

        val transformedContent = if (replaceAll) {
            if (caseSensitive) {
                content.replace(oldText, newText)
            } else {
                content.replace(oldText.toRegex(RegexOption.IGNORE_CASE), newText)
            }
        } else {
            if (caseSensitive) {
                content.replaceFirst(oldText, newText)
            } else {
                content.replaceFirst(oldText.toRegex(RegexOption.IGNORE_CASE), newText)
            }
        }

        return Document(transformedContent, document.metadata)
    }
}

/**
 * 文本规范化转换器，用于规范化文档内容中的文本。
 *
 * @property normalizeWhitespace 是否规范化空白字符
 * @property normalizePunctuation 是否规范化标点符号
 * @property normalizeCase 是否规范化大小写
 * @property removeEmptyLines 是否移除空行
 * @property trimLines 是否修剪行
 */
class TextNormalizeTransformer(
    private val normalizeWhitespace: Boolean = true,
    private val normalizePunctuation: Boolean = true,
    private val normalizeCase: Boolean = false,
    private val removeEmptyLines: Boolean = false,
    private val trimLines: Boolean = false
) : DocumentTransformer {
    override fun transform(document: Document): Document {
        var content = document.content

        // 规范化空白字符
        if (normalizeWhitespace) {
            content = content.replace("\\s+".toRegex(), " ")
        }

        // 规范化标点符号
        if (normalizePunctuation) {
            content = content.replace("\\s+([.,;:!?])".toRegex(), "$1")
            content = content.replace("([.,;:!?])(?!\\s|$)".toRegex(), "$1 ")
            content = content.replace("[\u201c\u201d]".toRegex(), "\"")
            content = content.replace("[\u2018\u2019]".toRegex(), "'")
            content = content.replace("[\u2014\u2013]".toRegex(), "-")
            content = content.replace("\\.{2,}".toRegex(), "...")
        }

        // 规范化大小写
        if (normalizeCase) {
            content = content.lowercase()
        }

        // 移除空行
        if (removeEmptyLines) {
            content = content.split("\n")
                .filter { it.trim().isNotEmpty() }
                .joinToString("\n")
        }

        // 修剪行
        if (trimLines) {
            content = content.split("\n")
                .map { it.trim() }
                .joinToString("\n")
        }

        return Document(content, document.metadata)
    }
}

/**
 * 文本截断转换器，用于截断文档内容。
 *
 * @property maxLength 最大长度
 * @property truncateFrom 截断方向
 * @property addEllipsis 是否添加省略号
 */
class TextTruncateTransformer(
    private val maxLength: Int,
    private val truncateFrom: TruncateFrom = TruncateFrom.END,
    private val addEllipsis: Boolean = true
) : DocumentTransformer {
    /**
     * 截断方向。
     */
    enum class TruncateFrom {
        /**
         * 从开头截断。
         */
        START,

        /**
         * 从结尾截断。
         */
        END,

        /**
         * 从中间截断。
         */
        MIDDLE
    }

    override fun transform(document: Document): Document {
        val content = document.content

        if (content.length <= maxLength) {
            return document
        }

        val ellipsis = if (addEllipsis) " ..." else ""
        val effectiveMaxLength = if (addEllipsis) maxLength - 4 else maxLength

        val truncatedContent = when (truncateFrom) {
            TruncateFrom.START -> {
                val startIndex = content.length - effectiveMaxLength
                (if (addEllipsis) "... " else "") + content.substring(startIndex)
            }
            TruncateFrom.END -> {
                content.substring(0, effectiveMaxLength) + ellipsis
            }
            TruncateFrom.MIDDLE -> {
                val firstPart = effectiveMaxLength / 2
                val secondPart = effectiveMaxLength - firstPart
                content.substring(0, firstPart) + ellipsis + content.substring(content.length - secondPart)
            }
        }

        return Document(truncatedContent, document.metadata)
    }
}
