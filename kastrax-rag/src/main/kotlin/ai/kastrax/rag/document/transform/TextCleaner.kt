package ai.kastrax.rag.document.transform

import ai.kastrax.rag.document.Document
import io.github.oshai.kotlinlogging.KotlinLogging
import java.text.Normalizer
import java.util.regex.Pattern

private val logger = KotlinLogging.logger {}

/**
 * 文本清理器，用于清理和规范化文本。
 *
 * 这个类提供了一系列方法来清理和规范化文本，包括去除多余空白、
 * 标准化标点符号、转换大小写、移除特殊字符等。
 */
class TextCleaner {

    /**
     * 清理文档的文本内容。
     *
     * @param document 要清理的文档
     * @param options 清理选项
     * @return 清理后的文档
     */
    fun clean(document: Document, options: CleaningOptions = CleaningOptions()): Document {
        val cleanedText = clean(document.content, options)
        return Document(cleanedText, document.metadata)
    }

    /**
     * 清理文本。
     *
     * @param text 要清理的文本
     * @param options 清理选项
     * @return 清理后的文本
     */
    fun clean(text: String, options: CleaningOptions = CleaningOptions()): String {
        var result = text

        try {
            // 应用选定的清理操作
            if (options.trimWhitespace) {
                result = result.trim()
            }

            if (options.normalizeWhitespace) {
                result = normalizeWhitespace(result)
            }

            if (options.normalizePunctuation) {
                result = normalizePunctuation(result)
            }

            if (options.toLowerCase) {
                result = result.lowercase()
            }

            if (options.toUpperCase) {
                result = result.uppercase()
            }

            if (options.removeExtraSpaces) {
                result = removeExtraSpaces(result)
            }

            if (options.removeSpecialCharacters) {
                result = removeSpecialCharacters(result)
            }

            if (options.removeEmptyLines) {
                result = removeEmptyLines(result)
            }

            if (options.normalizeLineEndings) {
                result = normalizeLineEndings(result)
            }

            if (options.normalizeUnicode) {
                result = normalizeUnicode(result)
            }

            if (options.removeUrls) {
                result = removeUrls(result)
            }

            if (options.removeHtmlTags) {
                result = removeHtmlTags(result)
            }

            if (options.removeNumbers) {
                result = removeNumbers(result)
            }

            if (options.customReplacements.isNotEmpty()) {
                result = applyCustomReplacements(result, options.customReplacements)
            }

            return result
        } catch (e: Exception) {
            logger.error(e) { "Error cleaning text" }
            return text  // 如果清理失败，返回原始文本
        }
    }

    /**
     * 规范化空白字符，将多个连续空白字符替换为单个空格。
     *
     * @param text 要处理的文本
     * @return 处理后的文本
     */
    private fun normalizeWhitespace(text: String): String {
        return text.replace("\\s+".toRegex(), " ")
    }

    /**
     * 规范化标点符号，统一使用标准标点符号。
     *
     * @param text 要处理的文本
     * @return 处理后的文本
     */
    private fun normalizePunctuation(text: String): String {
        var result = text

        // 使用字符串替换而不是字符替换
        result = result.replace("\u201c", "\"").replace("\u201d", "\"")
        result = result.replace("\u2018", "'").replace("\u2019", "'")

        // 统一破折号
        result = result.replace("\u2014", "-").replace("\u2013", "-")

        // 统一省略号
        result = result.replace("..", "...").replace("....", "...")

        // 统一中文标点
        result = result.replace("，", ", ")
        result = result.replace("。", ". ")
        result = result.replace("；", "; ")
        result = result.replace("：", ": ")
        result = result.replace("！", "! ")
        result = result.replace("？", "? ")
        result = result.replace("（", " (")
        result = result.replace("）", ") ")

        return result
    }

    /**
     * 移除多余的空格，包括连续空格和行首行尾的空格。
     *
     * @param text 要处理的文本
     * @return 处理后的文本
     */
    private fun removeExtraSpaces(text: String): String {
        // 只移除行首行尾的空格，保留文本中的多个连续空格
        return text.lines().joinToString("\n") { it.trim() }
    }

    /**
     * 移除特殊字符。
     *
     * @param text 要处理的文本
     * @return 处理后的文本
     */
    private fun removeSpecialCharacters(text: String): String {
        // 使用字符遍历而不是正则表达式
        val result = StringBuilder()
        for (c in text) {
            if (c.isLetterOrDigit() || c.isWhitespace() || c in ".,;:!?()[]{}'\"-+=/\\*@#$%&")
                result.append(c)
        }
        return result.toString()
    }

    /**
     * 移除空行。
     *
     * @param text 要处理的文本
     * @return 处理后的文本
     */
    private fun removeEmptyLines(text: String): String {
        return text.lines().filter { it.trim().isNotEmpty() }.joinToString("\n")
    }

    /**
     * 规范化行尾，统一使用 LF（\\n）。
     *
     * @param text 要处理的文本
     * @return 处理后的文本
     */
    private fun normalizeLineEndings(text: String): String {
        // 将 CRLF 转换为 LF
        var result = text.replace("\r\n", "\n")

        // 将单独的 CR 转换为 LF
        result = result.replace("\r", "\n")

        return result
    }

    /**
     * 规范化 Unicode 字符，将重音符号等转换为基本 ASCII 字符。
     *
     * @param text 要处理的文本
     * @return 处理后的文本
     */
    private fun normalizeUnicode(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(normalized).replaceAll("")
    }

    /**
     * 移除 URL。
     *
     * @param text 要处理的文本
     * @return 处理后的文本
     */
    private fun removeUrls(text: String): String {
        return text.replace("https?://\\S+".toRegex(), "")
    }

    /**
     * 移除 HTML 标签。
     *
     * @param text 要处理的文本
     * @return 处理后的文本
     */
    private fun removeHtmlTags(text: String): String {
        return text.replace("<[^>]*>".toRegex(), "")
    }

    /**
     * 移除数字。
     *
     * @param text 要处理的文本
     * @return 处理后的文本
     */
    private fun removeNumbers(text: String): String {
        return text.replace("\\d+".toRegex(), "")
    }

    /**
     * 应用自定义替换规则。
     *
     * @param text 要处理的文本
     * @param replacements 替换规则，键为要替换的文本，值为替换后的文本
     * @return 处理后的文本
     */
    private fun applyCustomReplacements(text: String, replacements: Map<String, String>): String {
        var result = text

        for ((oldValue, newValue) in replacements) {
            result = result.replace(oldValue, newValue)
        }

        return result
    }

    /**
     * 文本清理选项。
     *
     * @property trimWhitespace 是否去除首尾空白
     * @property normalizeWhitespace 是否规范化空白字符
     * @property normalizePunctuation 是否规范化标点符号
     * @property toLowerCase 是否转换为小写
     * @property toUpperCase 是否转换为大写
     * @property removeExtraSpaces 是否移除多余的空格
     * @property removeSpecialCharacters 是否移除特殊字符
     * @property removeEmptyLines 是否移除空行
     * @property normalizeLineEndings 是否规范化行尾
     * @property normalizeUnicode 是否规范化 Unicode 字符
     * @property removeUrls 是否移除 URL
     * @property removeHtmlTags 是否移除 HTML 标签
     * @property removeNumbers 是否移除数字
     * @property customReplacements 自定义替换规则
     */
    data class CleaningOptions(
        val trimWhitespace: Boolean = true,
        val normalizeWhitespace: Boolean = false,
        val normalizePunctuation: Boolean = false,
        val toLowerCase: Boolean = false,
        val toUpperCase: Boolean = false,
        val removeExtraSpaces: Boolean = false,
        val removeSpecialCharacters: Boolean = false,
        val removeEmptyLines: Boolean = false,
        val normalizeLineEndings: Boolean = true,
        val normalizeUnicode: Boolean = false,
        val removeUrls: Boolean = false,
        val removeHtmlTags: Boolean = false,
        val removeNumbers: Boolean = false,
        val customReplacements: Map<String, String> = emptyMap()
    ) {
        /**
         * 创建一个新的 CleaningOptions 构建器。
         *
         * @return CleaningOptionsBuilder 实例
         */
        fun toBuilder(): CleaningOptionsBuilder {
            return CleaningOptionsBuilder(this)
        }

        /**
         * CleaningOptions 构建器，用于链式配置清理选项。
         */
        class CleaningOptionsBuilder(options: CleaningOptions) {
            private var trimWhitespace = options.trimWhitespace
            private var normalizeWhitespace = options.normalizeWhitespace
            private var normalizePunctuation = options.normalizePunctuation
            private var toLowerCase = options.toLowerCase
            private var toUpperCase = options.toUpperCase
            private var removeExtraSpaces = options.removeExtraSpaces
            private var removeSpecialCharacters = options.removeSpecialCharacters
            private var removeEmptyLines = options.removeEmptyLines
            private var normalizeLineEndings = options.normalizeLineEndings
            private var normalizeUnicode = options.normalizeUnicode
            private var removeUrls = options.removeUrls
            private var removeHtmlTags = options.removeHtmlTags
            private var removeNumbers = options.removeNumbers
            private var customReplacements = options.customReplacements.toMutableMap()

            fun trimWhitespace(value: Boolean) = apply { trimWhitespace = value }
            fun normalizeWhitespace(value: Boolean) = apply { normalizeWhitespace = value }
            fun normalizePunctuation(value: Boolean) = apply { normalizePunctuation = value }
            fun toLowerCase(value: Boolean) = apply { toLowerCase = value }
            fun toUpperCase(value: Boolean) = apply { toUpperCase = value }
            fun removeExtraSpaces(value: Boolean) = apply { removeExtraSpaces = value }
            fun removeSpecialCharacters(value: Boolean) = apply { removeSpecialCharacters = value }
            fun removeEmptyLines(value: Boolean) = apply { removeEmptyLines = value }
            fun normalizeLineEndings(value: Boolean) = apply { normalizeLineEndings = value }
            fun normalizeUnicode(value: Boolean) = apply { normalizeUnicode = value }
            fun removeUrls(value: Boolean) = apply { removeUrls = value }
            fun removeHtmlTags(value: Boolean) = apply { removeHtmlTags = value }
            fun removeNumbers(value: Boolean) = apply { removeNumbers = value }

            fun addCustomReplacement(oldValue: String, newValue: String) = apply {
                customReplacements[oldValue] = newValue
            }

            fun setCustomReplacements(replacements: Map<String, String>) = apply {
                customReplacements.clear()
                customReplacements.putAll(replacements)
            }

            fun build(): CleaningOptions {
                return CleaningOptions(
                    trimWhitespace = trimWhitespace,
                    normalizeWhitespace = normalizeWhitespace,
                    normalizePunctuation = normalizePunctuation,
                    toLowerCase = toLowerCase,
                    toUpperCase = toUpperCase,
                    removeExtraSpaces = removeExtraSpaces,
                    removeSpecialCharacters = removeSpecialCharacters,
                    removeEmptyLines = removeEmptyLines,
                    normalizeLineEndings = normalizeLineEndings,
                    normalizeUnicode = normalizeUnicode,
                    removeUrls = removeUrls,
                    removeHtmlTags = removeHtmlTags,
                    removeNumbers = removeNumbers,
                    customReplacements = customReplacements
                )
            }
        }
    }

    companion object {
        /**
         * 创建一个预设的清理选项，用于基本清理。
         *
         * @return 基本清理选项
         */
        fun basicCleaningOptions(): CleaningOptions {
            return CleaningOptions(
                trimWhitespace = true,
                normalizeWhitespace = true,
                removeExtraSpaces = true,
                normalizeLineEndings = true,
                removeEmptyLines = true
            )
        }

        /**
         * 创建一个预设的清理选项，用于标准化文本。
         *
         * @return 标准化文本选项
         */
        fun standardizationOptions(): CleaningOptions {
            return CleaningOptions(
                trimWhitespace = true,
                normalizeWhitespace = true,
                normalizePunctuation = true,
                removeExtraSpaces = true,
                normalizeLineEndings = true,
                normalizeUnicode = true
            )
        }

        /**
         * 创建一个预设的清理选项，用于搜索引擎优化。
         *
         * @return 搜索引擎优化选项
         */
        fun seoOptions(): CleaningOptions {
            return CleaningOptions(
                trimWhitespace = true,
                normalizeWhitespace = true,
                normalizePunctuation = true,
                toLowerCase = true,
                removeExtraSpaces = true,
                normalizeLineEndings = true,
                normalizeUnicode = true,
                removeHtmlTags = true
            )
        }

        /**
         * 创建一个预设的清理选项，用于极简文本。
         *
         * @return 极简文本选项
         */
        fun minimalTextOptions(): CleaningOptions {
            return CleaningOptions(
                trimWhitespace = true,
                normalizeWhitespace = true,
                normalizePunctuation = true,
                toLowerCase = true,
                removeExtraSpaces = true,
                removeSpecialCharacters = true,
                removeEmptyLines = true,
                normalizeLineEndings = true,
                normalizeUnicode = true,
                removeUrls = true,
                removeHtmlTags = true
            )
        }
    }
}
