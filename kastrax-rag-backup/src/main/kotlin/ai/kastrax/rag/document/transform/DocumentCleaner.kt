package ai.kastrax.rag.document.transform

import ai.kastrax.rag.document.Document
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 文档清洗器，用于清洗文档内容，去除无用信息。
 */
class DocumentCleaner {

    /**
     * 清洗文档。
     *
     * @param document 要清洗的文档
     * @param options 清洗选项
     * @return 清洗后的文档
     */
    fun clean(document: Document, options: CleaningOptions = CleaningOptions()): Document {
        val cleanedContent = clean(document.content, options)
        return Document(cleanedContent, document.metadata)
    }

    /**
     * 清洗文本内容。
     *
     * @param text 要清洗的文本
     * @param options 清洗选项
     * @return 清洗后的文本
     */
    fun clean(text: String, options: CleaningOptions = CleaningOptions()): String {
        var cleanedText = text

        try {
            // 移除 HTML 标签
            if (options.removeHtmlTags) {
                cleanedText = removeHtmlTags(cleanedText)
            }

            // 移除 URL
            if (options.removeUrls) {
                cleanedText = removeUrls(cleanedText)
            }

            // 移除电子邮件地址
            if (options.removeEmails) {
                cleanedText = removeEmails(cleanedText)
            }

            // 移除特殊字符
            if (options.removeSpecialCharacters) {
                cleanedText = removeSpecialCharacters(cleanedText, options.preserveCharacters)
            }

            // 移除额外的空白字符
            if (options.removeExtraWhitespace) {
                cleanedText = removeExtraWhitespace(cleanedText)
            }

            // 移除数字
            if (options.removeNumbers) {
                cleanedText = removeNumbers(cleanedText)
            }

            // 移除标点符号
            if (options.removePunctuation) {
                cleanedText = removePunctuation(cleanedText, options.preserveCharacters)
            }

            // 移除停用词
            if (options.removeStopwords) {
                cleanedText = removeStopwords(cleanedText, options.stopwords, options.language)
            }

            // 移除短词
            if (options.removeShortWords) {
                cleanedText = removeShortWords(cleanedText, options.minWordLength)
            }

            // 移除重复行
            if (options.removeDuplicateLines) {
                cleanedText = removeDuplicateLines(cleanedText)
            }

            // 移除空行
            if (options.removeEmptyLines) {
                cleanedText = removeEmptyLines(cleanedText)
            }

            // 转换为小写
            if (options.convertToLowercase) {
                cleanedText = cleanedText.lowercase()
            }

            // 规范化空白字符
            if (options.normalizeWhitespace) {
                cleanedText = normalizeWhitespace(cleanedText)
            }

            // 规范化标点符号
            if (options.normalizePunctuation) {
                cleanedText = normalizePunctuation(cleanedText)
            }

            // 修剪文本
            if (options.trim) {
                cleanedText = cleanedText.trim()
            }

            return cleanedText
        } catch (e: Exception) {
            logger.error(e) { "Error cleaning text" }
            return text  // 如果清洗失败，返回原始文本
        }
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
     * 移除 URL。
     *
     * @param text 要处理的文本
     * @return 处理后的文本
     */
    private fun removeUrls(text: String): String {
        return text.replace("https?://\\S+|www\\.\\S+".toRegex(), "")
    }

    /**
     * 移除电子邮件地址。
     *
     * @param text 要处理的文本
     * @return 处理后的文本
     */
    private fun removeEmails(text: String): String {
        return text.replace("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b".toRegex(), "")
    }

    /**
     * 移除特殊字符。
     *
     * @param text 要处理的文本
     * @param preserveCharacters 要保留的字符
     * @return 处理后的文本
     */
    private fun removeSpecialCharacters(text: String, preserveCharacters: String): String {
        val pattern = if (preserveCharacters.isEmpty()) {
            "[^\\p{L}\\p{N}\\s]".toRegex()
        } else {
            val escapedChars = Regex.escape(preserveCharacters)
            "[^\\p{L}\\p{N}\\s$escapedChars]".toRegex()
        }
        return text.replace(pattern, "")
    }

    /**
     * 移除额外的空白字符。
     *
     * @param text 要处理的文本
     * @return 处理后的文本
     */
    private fun removeExtraWhitespace(text: String): String {
        return text.replace("\\s+".toRegex(), " ")
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
     * 移除标点符号。
     *
     * @param text 要处理的文本
     * @param preserveCharacters 要保留的字符
     * @return 处理后的文本
     */
    private fun removePunctuation(text: String, preserveCharacters: String): String {
        val pattern = if (preserveCharacters.isEmpty()) {
            "[\\p{P}]".toRegex()
        } else {
            val escapedChars = Regex.escape(preserveCharacters)
            "[\\p{P}&&[^$escapedChars]]".toRegex()
        }
        return text.replace(pattern, "")
    }

    /**
     * 移除停用词。
     *
     * @param text 要处理的文本
     * @param stopwords 停用词列表
     * @param language 语言
     * @return 处理后的文本
     */
    private fun removeStopwords(text: String, stopwords: Set<String>, language: String): String {
        val words = text.split("\\s+".toRegex())
        val filteredWords = words.filter { it.lowercase() !in stopwords }
        return filteredWords.joinToString(" ")
    }

    /**
     * 移除短词。
     *
     * @param text 要处理的文本
     * @param minLength 最小词长
     * @return 处理后的文本
     */
    private fun removeShortWords(text: String, minLength: Int): String {
        val words = text.split("\\s+".toRegex())
        val filteredWords = words.filter { it.length >= minLength }
        return filteredWords.joinToString(" ")
    }

    /**
     * 移除重复行。
     *
     * @param text 要处理的文本
     * @return 处理后的文本
     */
    private fun removeDuplicateLines(text: String): String {
        val lines = text.split("\n")
        val uniqueLines = lines.distinct()
        return uniqueLines.joinToString("\n")
    }

    /**
     * 移除空行。
     *
     * @param text 要处理的文本
     * @return 处理后的文本
     */
    private fun removeEmptyLines(text: String): String {
        val lines = text.split("\n")
        val nonEmptyLines = lines.filter { it.trim().isNotEmpty() }
        return nonEmptyLines.joinToString("\n")
    }

    /**
     * 规范化空白字符。
     *
     * @param text 要处理的文本
     * @return 处理后的文本
     */
    private fun normalizeWhitespace(text: String): String {
        // 将所有空白字符（包括制表符、换行符等）替换为单个空格
        var normalized = text.replace("\\s+".toRegex(), " ")

        // 确保标点符号前没有空格，后有空格
        normalized = normalized.replace("\\s+([.,;:!?])".toRegex(), "$1")
        normalized = normalized.replace("([.,;:!?])(?!\\s|$)".toRegex(), "$1 ")

        return normalized
    }

    /**
     * 规范化标点符号。
     *
     * @param text 要处理的文本
     * @return 处理后的文本
     */
    private fun normalizePunctuation(text: String): String {
        var normalized = text

        // 规范化引号
        normalized = normalized.replace("[\u201c\u201d]".toRegex(), "\"")
        normalized = normalized.replace("[\u2018\u2019]".toRegex(), "'")

        // 规范化破折号
        normalized = normalized.replace("[\u2014\u2013]".toRegex(), "-")

        // 规范化省略号
        normalized = normalized.replace("\\.{2,}".toRegex(), "...")

        return normalized
    }

    /**
     * 文档清洗选项。
     *
     * @property removeHtmlTags 是否移除 HTML 标签
     * @property removeUrls 是否移除 URL
     * @property removeEmails 是否移除电子邮件地址
     * @property removeSpecialCharacters 是否移除特殊字符
     * @property removeExtraWhitespace 是否移除额外的空白字符
     * @property removeNumbers 是否移除数字
     * @property removePunctuation 是否移除标点符号
     * @property removeStopwords 是否移除停用词
     * @property removeShortWords 是否移除短词
     * @property removeDuplicateLines 是否移除重复行
     * @property removeEmptyLines 是否移除空行
     * @property convertToLowercase 是否转换为小写
     * @property normalizeWhitespace 是否规范化空白字符
     * @property normalizePunctuation 是否规范化标点符号
     * @property trim 是否修剪文本
     * @property preserveCharacters 要保留的字符
     * @property minWordLength 最小词长
     * @property stopwords 停用词集合
     * @property language 语言
     */
    data class CleaningOptions(
        val removeHtmlTags: Boolean = true,
        val removeUrls: Boolean = false,
        val removeEmails: Boolean = false,
        val removeSpecialCharacters: Boolean = false,
        val removeExtraWhitespace: Boolean = true,
        val removeNumbers: Boolean = false,
        val removePunctuation: Boolean = false,
        val removeStopwords: Boolean = false,
        val removeShortWords: Boolean = false,
        val removeDuplicateLines: Boolean = false,
        val removeEmptyLines: Boolean = true,
        val convertToLowercase: Boolean = false,
        val normalizeWhitespace: Boolean = true,
        val normalizePunctuation: Boolean = true,
        val trim: Boolean = true,
        val preserveCharacters: String = ".,;:!?()[]{}\"'`-_",
        val minWordLength: Int = 3,
        val stopwords: Set<String> = DEFAULT_STOPWORDS,
        val language: String = "en"
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
         * CleaningOptions 构建器，用于链式配置清洗选项。
         */
        class CleaningOptionsBuilder(options: CleaningOptions) {
            private var removeHtmlTags = options.removeHtmlTags
            private var removeUrls = options.removeUrls
            private var removeEmails = options.removeEmails
            private var removeSpecialCharacters = options.removeSpecialCharacters
            private var removeExtraWhitespace = options.removeExtraWhitespace
            private var removeNumbers = options.removeNumbers
            private var removePunctuation = options.removePunctuation
            private var removeStopwords = options.removeStopwords
            private var removeShortWords = options.removeShortWords
            private var removeDuplicateLines = options.removeDuplicateLines
            private var removeEmptyLines = options.removeEmptyLines
            private var convertToLowercase = options.convertToLowercase
            private var normalizeWhitespace = options.normalizeWhitespace
            private var normalizePunctuation = options.normalizePunctuation
            private var trim = options.trim
            private var preserveCharacters = options.preserveCharacters
            private var minWordLength = options.minWordLength
            private var stopwords = options.stopwords
            private var language = options.language

            fun removeHtmlTags(value: Boolean) = apply { removeHtmlTags = value }
            fun removeUrls(value: Boolean) = apply { removeUrls = value }
            fun removeEmails(value: Boolean) = apply { removeEmails = value }
            fun removeSpecialCharacters(value: Boolean) = apply { removeSpecialCharacters = value }
            fun removeExtraWhitespace(value: Boolean) = apply { removeExtraWhitespace = value }
            fun removeNumbers(value: Boolean) = apply { removeNumbers = value }
            fun removePunctuation(value: Boolean) = apply { removePunctuation = value }
            fun removeStopwords(value: Boolean) = apply { removeStopwords = value }
            fun removeShortWords(value: Boolean) = apply { removeShortWords = value }
            fun removeDuplicateLines(value: Boolean) = apply { removeDuplicateLines = value }
            fun removeEmptyLines(value: Boolean) = apply { removeEmptyLines = value }
            fun convertToLowercase(value: Boolean) = apply { convertToLowercase = value }
            fun normalizeWhitespace(value: Boolean) = apply { normalizeWhitespace = value }
            fun normalizePunctuation(value: Boolean) = apply { normalizePunctuation = value }
            fun trim(value: Boolean) = apply { trim = value }
            fun preserveCharacters(value: String) = apply { preserveCharacters = value }
            fun minWordLength(value: Int) = apply { minWordLength = value }
            fun stopwords(value: Set<String>) = apply { stopwords = value }
            fun language(value: String) = apply { language = value }

            fun build(): CleaningOptions {
                return CleaningOptions(
                    removeHtmlTags = removeHtmlTags,
                    removeUrls = removeUrls,
                    removeEmails = removeEmails,
                    removeSpecialCharacters = removeSpecialCharacters,
                    removeExtraWhitespace = removeExtraWhitespace,
                    removeNumbers = removeNumbers,
                    removePunctuation = removePunctuation,
                    removeStopwords = removeStopwords,
                    removeShortWords = removeShortWords,
                    removeDuplicateLines = removeDuplicateLines,
                    removeEmptyLines = removeEmptyLines,
                    convertToLowercase = convertToLowercase,
                    normalizeWhitespace = normalizeWhitespace,
                    normalizePunctuation = normalizePunctuation,
                    trim = trim,
                    preserveCharacters = preserveCharacters,
                    minWordLength = minWordLength,
                    stopwords = stopwords,
                    language = language
                )
            }
        }
    }

    companion object {
        /**
         * 默认英语停用词集合。
         */
        val DEFAULT_STOPWORDS = setOf(
            "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into",
            "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then",
            "there", "these", "they", "this", "to", "was", "will", "with"
        )

        /**
         * 创建一个预设的清洗选项，用于基本清洗。
         *
         * @return 基本清洗选项
         */
        fun basicCleaningOptions(): CleaningOptions {
            return CleaningOptions(
                removeHtmlTags = true,
                removeUrls = false,
                removeEmails = false,
                removeSpecialCharacters = false,
                removeExtraWhitespace = true,
                removeNumbers = false,
                removePunctuation = false,
                removeStopwords = false,
                removeShortWords = false,
                removeDuplicateLines = false,
                removeEmptyLines = true,
                convertToLowercase = false,
                normalizeWhitespace = true,
                normalizePunctuation = true,
                trim = true
            )
        }

        /**
         * 创建一个预设的清洗选项，用于高级清洗。
         *
         * @return 高级清洗选项
         */
        fun advancedCleaningOptions(): CleaningOptions {
            return CleaningOptions(
                removeHtmlTags = true,
                removeUrls = true,
                removeEmails = true,
                removeSpecialCharacters = true,
                removeExtraWhitespace = true,
                removeNumbers = false,
                removePunctuation = true,
                removeStopwords = true,
                removeShortWords = true,
                removeDuplicateLines = true,
                removeEmptyLines = true,
                convertToLowercase = true,
                normalizeWhitespace = true,
                normalizePunctuation = true,
                trim = true
            )
        }

        /**
         * 创建一个预设的清洗选项，用于搜索引擎优化。
         *
         * @return 搜索引擎优化清洗选项
         */
        fun seoCleaningOptions(): CleaningOptions {
            return CleaningOptions(
                removeHtmlTags = true,
                removeUrls = false,
                removeEmails = false,
                removeSpecialCharacters = false,
                removeExtraWhitespace = true,
                removeNumbers = false,
                removePunctuation = false,
                removeStopwords = false,
                removeShortWords = false,
                removeDuplicateLines = true,
                removeEmptyLines = true,
                convertToLowercase = true,
                normalizeWhitespace = true,
                normalizePunctuation = true,
                trim = true
            )
        }
    }
}
