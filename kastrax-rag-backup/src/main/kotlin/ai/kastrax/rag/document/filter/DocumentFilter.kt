package ai.kastrax.rag.document.filter

import ai.kastrax.rag.document.Document
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 文档过滤器接口，用于过滤文档，只保留符合条件的文档。
 */
interface DocumentFilter {
    /**
     * 判断文档是否应该保留。
     *
     * @param document 要判断的文档
     * @return 如果文档应该保留，则返回 true；否则返回 false
     */
    fun shouldKeep(document: Document): Boolean

    /**
     * 过滤文档列表，只保留符合条件的文档。
     *
     * @param documents 要过滤的文档列表
     * @return 过滤后的文档列表
     */
    fun filter(documents: List<Document>): List<Document> {
        return documents.filter { shouldKeep(it) }
    }
}

/**
 * 组合文档过滤器，组合多个过滤器。
 *
 * @property filters 要组合的过滤器列表
 * @property mode 组合模式，默认为 FilterMode.ALL
 */
class CompositeDocumentFilter(
    private val filters: List<DocumentFilter>,
    private val mode: FilterMode = FilterMode.ALL
) : DocumentFilter {
    /**
     * 组合模式。
     */
    enum class FilterMode {
        /**
         * 所有过滤器都必须返回 true。
         */
        ALL,
        
        /**
         * 任意一个过滤器返回 true 即可。
         */
        ANY
    }

    constructor(vararg filters: DocumentFilter, mode: FilterMode = FilterMode.ALL) : this(filters.toList(), mode)

    override fun shouldKeep(document: Document): Boolean {
        if (filters.isEmpty()) {
            return true
        }

        return when (mode) {
            FilterMode.ALL -> filters.all { it.shouldKeep(document) }
            FilterMode.ANY -> filters.any { it.shouldKeep(document) }
        }
    }
}

/**
 * 内容长度过滤器，根据文档内容长度过滤文档。
 *
 * @property minLength 最小长度，默认为 0
 * @property maxLength 最大长度，默认为 Int.MAX_VALUE
 */
class ContentLengthFilter(
    private val minLength: Int = 0,
    private val maxLength: Int = Int.MAX_VALUE
) : DocumentFilter {
    override fun shouldKeep(document: Document): Boolean {
        val length = document.content.length
        return length in minLength..maxLength
    }
}

/**
 * 内容正则表达式过滤器，根据正则表达式过滤文档内容。
 *
 * @property pattern 正则表达式模式
 * @property mode 匹配模式，默认为 MatchMode.INCLUDE
 */
class ContentRegexFilter(
    private val pattern: Regex,
    private val mode: MatchMode = MatchMode.INCLUDE
) : DocumentFilter {
    /**
     * 匹配模式。
     */
    enum class MatchMode {
        /**
         * 包含匹配项。
         */
        INCLUDE,
        
        /**
         * 排除匹配项。
         */
        EXCLUDE
    }

    constructor(pattern: String, mode: MatchMode = MatchMode.INCLUDE) : this(Regex(pattern), mode)

    override fun shouldKeep(document: Document): Boolean {
        val matches = pattern.containsMatchIn(document.content)
        return when (mode) {
            MatchMode.INCLUDE -> matches
            MatchMode.EXCLUDE -> !matches
        }
    }
}

/**
 * 内容关键词过滤器，根据关键词过滤文档内容。
 *
 * @property keywords 关键词列表
 * @property mode 匹配模式，默认为 MatchMode.INCLUDE
 * @property matchAll 是否匹配所有关键词，默认为 false
 * @property caseSensitive 是否区分大小写，默认为 false
 */
class ContentKeywordFilter(
    private val keywords: List<String>,
    private val mode: MatchMode = MatchMode.INCLUDE,
    private val matchAll: Boolean = false,
    private val caseSensitive: Boolean = false
) : DocumentFilter {
    /**
     * 匹配模式。
     */
    enum class MatchMode {
        /**
         * 包含关键词。
         */
        INCLUDE,
        
        /**
         * 排除关键词。
         */
        EXCLUDE
    }

    constructor(
        vararg keywords: String,
        mode: MatchMode = MatchMode.INCLUDE,
        matchAll: Boolean = false,
        caseSensitive: Boolean = false
    ) : this(keywords.toList(), mode, matchAll, caseSensitive)

    override fun shouldKeep(document: Document): Boolean {
        val content = if (caseSensitive) document.content else document.content.lowercase()
        val keywordList = if (caseSensitive) keywords else keywords.map { it.lowercase() }

        val matches = if (matchAll) {
            keywordList.all { content.contains(it) }
        } else {
            keywordList.any { content.contains(it) }
        }

        return when (mode) {
            MatchMode.INCLUDE -> matches
            MatchMode.EXCLUDE -> !matches
        }
    }
}

/**
 * 元数据过滤器，根据元数据过滤文档。
 *
 * @property key 元数据键
 * @property value 元数据值
 * @property mode 匹配模式，默认为 MatchMode.EQUALS
 */
class MetadataFilter(
    private val key: String,
    private val value: Any?,
    private val mode: MatchMode = MatchMode.EQUALS
) : DocumentFilter {
    /**
     * 匹配模式。
     */
    enum class MatchMode {
        /**
         * 等于。
         */
        EQUALS,
        
        /**
         * 不等于。
         */
        NOT_EQUALS,
        
        /**
         * 包含（仅适用于字符串值）。
         */
        CONTAINS,
        
        /**
         * 不包含（仅适用于字符串值）。
         */
        NOT_CONTAINS,
        
        /**
         * 大于（仅适用于数值）。
         */
        GREATER_THAN,
        
        /**
         * 小于（仅适用于数值）。
         */
        LESS_THAN,
        
        /**
         * 存在。
         */
        EXISTS,
        
        /**
         * 不存在。
         */
        NOT_EXISTS
    }

    override fun shouldKeep(document: Document): Boolean {
        val metadata = document.metadata

        return when (mode) {
            MatchMode.EXISTS -> metadata.containsKey(key)
            MatchMode.NOT_EXISTS -> !metadata.containsKey(key)
            else -> {
                if (!metadata.containsKey(key)) {
                    return mode == MatchMode.NOT_EQUALS
                }

                val metadataValue = metadata[key]

                when (mode) {
                    MatchMode.EQUALS -> metadataValue == value
                    MatchMode.NOT_EQUALS -> metadataValue != value
                    MatchMode.CONTAINS -> {
                        if (metadataValue is String && value is String) {
                            metadataValue.contains(value)
                        } else {
                            false
                        }
                    }
                    MatchMode.NOT_CONTAINS -> {
                        if (metadataValue is String && value is String) {
                            !metadataValue.contains(value)
                        } else {
                            true
                        }
                    }
                    MatchMode.GREATER_THAN -> {
                        compareValues(metadataValue, value) > 0
                    }
                    MatchMode.LESS_THAN -> {
                        compareValues(metadataValue, value) < 0
                    }
                    else -> false
                }
            }
        }
    }

    /**
     * 比较两个值。
     *
     * @param value1 第一个值
     * @param value2 第二个值
     * @return 比较结果
     */
    @Suppress("UNCHECKED_CAST")
    private fun compareValues(value1: Any?, value2: Any?): Int {
        if (value1 == null || value2 == null) {
            return if (value1 == null && value2 == null) 0 else if (value1 == null) -1 else 1
        }

        return when {
            value1 is Number && value2 is Number -> {
                value1.toDouble().compareTo(value2.toDouble())
            }
            value1 is String && value2 is String -> {
                value1.compareTo(value2)
            }
            value1 is Comparable<*> && value1::class == value2::class -> {
                (value1 as Comparable<Any>).compareTo(value2)
            }
            else -> {
                value1.toString().compareTo(value2.toString())
            }
        }
    }
}

/**
 * 语言过滤器，根据文档语言过滤文档。
 *
 * @property languages 语言列表
 * @property mode 匹配模式，默认为 MatchMode.INCLUDE
 * @property metadataKey 语言元数据键，默认为 "language"
 */
class LanguageFilter(
    private val languages: List<String>,
    private val mode: MatchMode = MatchMode.INCLUDE,
    private val metadataKey: String = "language"
) : DocumentFilter {
    /**
     * 匹配模式。
     */
    enum class MatchMode {
        /**
         * 包含语言。
         */
        INCLUDE,
        
        /**
         * 排除语言。
         */
        EXCLUDE
    }

    constructor(
        vararg languages: String,
        mode: MatchMode = MatchMode.INCLUDE,
        metadataKey: String = "language"
    ) : this(languages.toList(), mode, metadataKey)

    override fun shouldKeep(document: Document): Boolean {
        val metadata = document.metadata
        if (!metadata.containsKey(metadataKey)) {
            // 如果没有语言元数据，默认保留
            return true
        }

        val language = metadata[metadataKey]?.toString()?.lowercase() ?: return true
        val matches = languages.any { it.lowercase() == language }

        return when (mode) {
            MatchMode.INCLUDE -> matches
            MatchMode.EXCLUDE -> !matches
        }
    }
}

/**
 * 重复内容过滤器，过滤掉重复内容的文档。
 *
 * @property checkContent 是否检查内容，默认为 true
 * @property checkMetadata 是否检查元数据，默认为 false
 * @property metadataKeys 要检查的元数据键列表，默认为空列表（检查所有键）
 */
class DuplicateContentFilter(
    private val checkContent: Boolean = true,
    private val checkMetadata: Boolean = false,
    private val metadataKeys: List<String> = emptyList()
) : DocumentFilter {
    private val seenContents = mutableSetOf<String>()
    private val seenMetadata = mutableSetOf<String>()

    override fun shouldKeep(document: Document): Boolean {
        var isDuplicate = false

        if (checkContent) {
            val content = document.content
            if (content in seenContents) {
                isDuplicate = true
            } else {
                seenContents.add(content)
            }
        }

        if (checkMetadata && !isDuplicate) {
            val metadata = document.metadata
            val metadataString = if (metadataKeys.isEmpty()) {
                metadata.toString()
            } else {
                metadataKeys.mapNotNull { key ->
                    metadata[key]?.let { "$key=$it" }
                }.sorted().joinToString(",")
            }

            if (metadataString in seenMetadata) {
                isDuplicate = true
            } else {
                seenMetadata.add(metadataString)
            }
        }

        return !isDuplicate
    }

    /**
     * 重置过滤器，清除已见内容和元数据。
     */
    fun reset() {
        seenContents.clear()
        seenMetadata.clear()
    }
}

/**
 * 质量过滤器，根据文档质量过滤文档。
 *
 * @property minQualityScore 最小质量分数，默认为 0.5
 */
class QualityFilter(
    private val minQualityScore: Double = 0.5
) : DocumentFilter {
    override fun shouldKeep(document: Document): Boolean {
        val content = document.content
        
        // 计算质量分数
        val score = calculateQualityScore(content)
        
        return score >= minQualityScore
    }

    /**
     * 计算文本质量分数。
     *
     * @param text 要计算的文本
     * @return 质量分数，范围为 [0, 1]
     */
    private fun calculateQualityScore(text: String): Double {
        if (text.isEmpty()) {
            return 0.0
        }

        var score = 1.0

        // 检查文本长度
        val lengthScore = (text.length.toDouble() / 1000).coerceAtMost(1.0)
        score *= lengthScore

        // 检查句子数量
        val sentences = text.split(Regex("[.!?]+\\s+"))
        val sentenceScore = (sentences.size.toDouble() / 10).coerceAtMost(1.0)
        score *= sentenceScore

        // 检查平均句子长度
        val avgSentenceLength = text.length.toDouble() / sentences.size.coerceAtLeast(1)
        val sentenceLengthScore = if (avgSentenceLength < 5) {
            avgSentenceLength / 5
        } else if (avgSentenceLength > 50) {
            1 - ((avgSentenceLength - 50) / 50).coerceAtMost(1.0)
        } else {
            1.0
        }
        score *= sentenceLengthScore

        // 检查标点符号比例
        val punctuationCount = text.count { it in ".,;:!?()[]{}\"'" }
        val punctuationRatio = punctuationCount.toDouble() / text.length
        val punctuationScore = if (punctuationRatio > 0.2) {
            1 - ((punctuationRatio - 0.2) / 0.3).coerceAtMost(1.0)
        } else {
            1.0
        }
        score *= punctuationScore

        // 检查大写字母比例
        val uppercaseCount = text.count { it.isUpperCase() }
        val uppercaseRatio = uppercaseCount.toDouble() / text.length
        val uppercaseScore = if (uppercaseRatio > 0.3) {
            1 - ((uppercaseRatio - 0.3) / 0.7).coerceAtMost(1.0)
        } else {
            1.0
        }
        score *= uppercaseScore

        return score
    }
}

/**
 * 相似度过滤器，过滤掉与参考文档相似度高于阈值的文档。
 *
 * @property referenceDocuments 参考文档列表
 * @property similarityThreshold 相似度阈值，默认为 0.8
 * @property mode 匹配模式，默认为 MatchMode.EXCLUDE
 */
class SimilarityFilter(
    private val referenceDocuments: List<Document>,
    private val similarityThreshold: Double = 0.8,
    private val mode: MatchMode = MatchMode.EXCLUDE
) : DocumentFilter {
    /**
     * 匹配模式。
     */
    enum class MatchMode {
        /**
         * 包含相似文档。
         */
        INCLUDE,
        
        /**
         * 排除相似文档。
         */
        EXCLUDE
    }

    constructor(
        referenceDocument: Document,
        similarityThreshold: Double = 0.8,
        mode: MatchMode = MatchMode.EXCLUDE
    ) : this(listOf(referenceDocument), similarityThreshold, mode)

    override fun shouldKeep(document: Document): Boolean {
        if (referenceDocuments.isEmpty()) {
            return true
        }

        val maxSimilarity = referenceDocuments.maxOfOrNull { calculateSimilarity(it.content, document.content) } ?: 0.0
        val isSimilar = maxSimilarity >= similarityThreshold

        return when (mode) {
            MatchMode.INCLUDE -> isSimilar
            MatchMode.EXCLUDE -> !isSimilar
        }
    }

    /**
     * 计算两个文本的相似度。
     *
     * @param text1 第一个文本
     * @param text2 第二个文本
     * @return 相似度，范围为 [0, 1]
     */
    private fun calculateSimilarity(text1: String, text2: String): Double {
        if (text1.isEmpty() || text2.isEmpty()) {
            return 0.0
        }

        // 使用 Jaccard 相似度
        val words1 = text1.lowercase().split(Regex("\\W+")).toSet()
        val words2 = text2.lowercase().split(Regex("\\W+")).toSet()

        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size

        return intersection.toDouble() / union
    }
}
