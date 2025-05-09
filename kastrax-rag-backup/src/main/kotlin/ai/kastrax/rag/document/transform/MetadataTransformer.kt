package ai.kastrax.rag.document.transform

import ai.kastrax.rag.document.Document
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.Date

private val logger = KotlinLogging.logger {}

/**
 * 元数据转换器，用于转换文档元数据。
 *
 * @property addMetadata 要添加的元数据
 * @property removeKeys 要移除的键列表
 * @property renameKeys 要重命名的键映射
 * @property transformKeys 是否转换键名（如小写、移除空格等）
 * @property keysToLowercase 是否将键名转换为小写
 * @property replaceSpacesInKeys 是否替换键名中的空格
 * @property spaceReplacement 替换空格的字符
 */
class MetadataTransformer(
    private val addMetadata: Map<String, Any> = emptyMap(),
    private val removeKeys: List<String> = emptyList(),
    private val renameKeys: Map<String, String> = emptyMap(),
    private val transformKeys: Boolean = false,
    private val keysToLowercase: Boolean = false,
    private val replaceSpacesInKeys: Boolean = false,
    private val spaceReplacement: String = "_"
) : DocumentTransformer {
    override fun transform(document: Document): Document {
        val metadata = document.metadata.toMutableMap()

        // 移除指定的键
        removeKeys.forEach { key ->
            metadata.remove(key)
        }

        // 重命名键
        renameKeys.forEach { (oldKey, newKey) ->
            if (metadata.containsKey(oldKey)) {
                metadata[newKey] = metadata[oldKey]!!
                metadata.remove(oldKey)
            }
        }

        // 转换键名
        if (transformKeys) {
            val transformedMetadata = mutableMapOf<String, Any>()
            metadata.forEach { (key, value) ->
                var transformedKey = key

                if (keysToLowercase) {
                    transformedKey = transformedKey.lowercase()
                }

                if (replaceSpacesInKeys) {
                    transformedKey = transformedKey.replace("\\s+".toRegex(), spaceReplacement)
                }

                transformedMetadata[transformedKey] = value
            }
            metadata.clear()
            metadata.putAll(transformedMetadata)
        }

        // 添加新的元数据
        metadata.putAll(addMetadata)

        return Document(document.content, metadata)
    }
}

/**
 * 元数据过滤转换器，根据元数据过滤文档。
 *
 * @property filter 元数据过滤条件
 * @property mode 过滤模式
 */
class MetadataFilterTransformer(
    private val filter: Map<String, Any>,
    private val mode: FilterMode = FilterMode.INCLUDE
) : DocumentTransformer {
    /**
     * 过滤模式。
     */
    enum class FilterMode {
        /**
         * 包含匹配的文档。
         */
        INCLUDE,

        /**
         * 排除匹配的文档。
         */
        EXCLUDE
    }

    override fun transform(document: Document): Document {
        val matches = matchesFilter(document.metadata)

        return when (mode) {
            FilterMode.INCLUDE -> if (matches) document else Document("", emptyMap())
            FilterMode.EXCLUDE -> if (matches) Document("", emptyMap()) else document
        }
    }

    override fun transform(documents: List<Document>): List<Document> {
        return documents.filter { document ->
            val matches = matchesFilter(document.metadata)
            when (mode) {
                FilterMode.INCLUDE -> matches
                FilterMode.EXCLUDE -> !matches
            }
        }
    }

    /**
     * 检查元数据是否匹配过滤条件。
     *
     * @param metadata 要检查的元数据
     * @return 是否匹配
     */
    private fun matchesFilter(metadata: Map<String, Any>): Boolean {
        for ((key, value) in filter) {
            if (!metadata.containsKey(key)) {
                return false
            }

            val metadataValue = metadata[key]

            if (value is List<*>) {
                // 如果过滤值是列表，检查元数据值是否在列表中
                if (metadataValue !in value) {
                    return false
                }
            } else {
                // 否则直接比较值
                if (metadataValue != value) {
                    return false
                }
            }
        }

        return true
    }
}

/**
 * 元数据提取转换器，从文档内容中提取元数据。
 *
 * @property patterns 提取模式映射，键为元数据键，值为正则表达式
 * @property overwrite 是否覆盖现有元数据
 * @property removeExtracted 是否从内容中移除提取的文本
 */
class MetadataExtractorTransformer private constructor(
    private val patterns: Map<String, Regex>,
    private val overwrite: Boolean = true,
    private val removeExtracted: Boolean = false
) : DocumentTransformer {
    companion object {
        /**
         * 创建一个元数据提取转换器，使用字符串模式。
         *
         * @param patterns 提取模式映射，键为元数据键，值为正则表达式字符串
         * @param overwrite 是否覆盖现有元数据
         * @param removeExtracted 是否从内容中移除提取的文本
         * @return 元数据提取转换器
         */
        fun fromStringPatterns(
            patterns: Map<String, String>,
            overwrite: Boolean = true,
            removeExtracted: Boolean = false
        ): MetadataExtractorTransformer {
            return MetadataExtractorTransformer(
                patterns.mapValues { (_, pattern) -> pattern.toRegex() },
                overwrite,
                removeExtracted
            )
        }

        /**
         * 创建一个元数据提取转换器，使用正则表达式模式。
         *
         * @param patterns 提取模式映射，键为元数据键，值为正则表达式
         * @param overwrite 是否覆盖现有元数据
         * @param removeExtracted 是否从内容中移除提取的文本
         * @return 元数据提取转换器
         */
        fun fromRegexPatterns(
            patterns: Map<String, Regex>,
            overwrite: Boolean = true,
            removeExtracted: Boolean = false
        ): MetadataExtractorTransformer {
            return MetadataExtractorTransformer(
                patterns,
                overwrite,
                removeExtracted
            )
        }
    }

    override fun transform(document: Document): Document {
        var content = document.content
        val metadata = document.metadata.toMutableMap()

        for ((key, pattern) in patterns) {
            val matchResult = pattern.find(content)

            if (matchResult != null) {
                // 提取值
                val value = if (matchResult.groupValues.size > 1) {
                    matchResult.groupValues[1]
                } else {
                    matchResult.value
                }

                // 更新元数据
                if (overwrite || !metadata.containsKey(key)) {
                    metadata[key] = value
                }

                // 从内容中移除提取的文本
                if (removeExtracted) {
                    content = content.replaceRange(matchResult.range, "")
                }
            }
        }

        return Document(content, metadata)
    }
}

/**
 * 元数据标准化转换器，标准化元数据值。
 *
 * @property normalizeKeys 要标准化的键列表
 * @property dateFormat 日期格式
 * @property numberFormat 数字格式
 * @property booleanValues 布尔值映射
 */
class MetadataNormalizerTransformer(
    private val normalizeKeys: List<String> = emptyList(),
    private val dateFormat: String = "yyyy-MM-dd",
    private val numberFormat: String = "#.##",
    private val booleanValues: Map<String, Boolean> = mapOf(
        "true" to true, "yes" to true, "y" to true, "1" to true,
        "false" to false, "no" to false, "n" to false, "0" to false
    )
) : DocumentTransformer {
    override fun transform(document: Document): Document {
        val metadata = document.metadata.toMutableMap()

        // 如果没有指定键，标准化所有键
        val keysToNormalize = if (normalizeKeys.isEmpty()) {
            metadata.keys
        } else {
            normalizeKeys.filter { metadata.containsKey(it) }
        }

        for (key in keysToNormalize) {
            val value = metadata[key] ?: continue

            // 标准化值
            val normalizedValue = normalizeValue(value)
            metadata[key] = normalizedValue
        }

        return Document(document.content, metadata)
    }

    /**
     * 标准化值。
     *
     * @param value 要标准化的值
     * @return 标准化后的值
     */
    private fun normalizeValue(value: Any): Any {
        return when (value) {
            is String -> {
                // 尝试转换为日期
                try {
                    val dateFormats = listOf(
                        "yyyy-MM-dd", "MM/dd/yyyy", "dd/MM/yyyy",
                        "yyyy-MM-dd HH:mm:ss", "MM/dd/yyyy HH:mm:ss", "dd/MM/yyyy HH:mm:ss"
                    )

                    for (format in dateFormats) {
                        try {
                            val date = java.text.SimpleDateFormat(format).parse(value)
                            return java.text.SimpleDateFormat(dateFormat).format(date)
                        } catch (e: Exception) {
                            // 忽略解析错误，尝试下一个格式
                        }
                    }
                } catch (e: Exception) {
                    // 忽略日期解析错误
                }

                // 尝试转换为数字
                try {
                    val number = value.toDouble()
                    return java.text.DecimalFormat(numberFormat).format(number)
                } catch (e: Exception) {
                    // 忽略数字解析错误
                }

                // 尝试转换为布尔值
                val lowercaseValue = value.lowercase()
                if (lowercaseValue in booleanValues) {
                    return booleanValues[lowercaseValue]!!
                }

                // 如果无法转换，返回原始值
                value
            }
            is Number -> {
                try {
                    java.text.DecimalFormat(numberFormat).format(value)
                } catch (e: Exception) {
                    value
                }
            }
            is Date -> {
                try {
                    java.text.SimpleDateFormat(dateFormat).format(value)
                } catch (e: Exception) {
                    value
                }
            }
            else -> value
        }
    }
}
