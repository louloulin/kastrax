package ai.kastrax.rag.reranker

import ai.kastrax.rag.vectorstore.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val logger = KotlinLogging.logger {}

/**
 * 元数据字段类型。
 */
enum class MetadataFieldType {
    /**
     * 数值类型，用于数字比较。
     */
    NUMERIC,
    
    /**
     * 日期类型，用于日期比较。
     */
    DATE,
    
    /**
     * 布尔类型，用于布尔值比较。
     */
    BOOLEAN,
    
    /**
     * 文本类型，用于文本匹配。
     */
    TEXT,
    
    /**
     * 列表类型，用于列表匹配。
     */
    LIST
}

/**
 * 元数据字段排序方向。
 */
enum class SortDirection {
    /**
     * 升序排序。
     */
    ASCENDING,
    
    /**
     * 降序排序。
     */
    DESCENDING
}

/**
 * 元数据字段配置。
 *
 * @property fieldName 字段名称
 * @property fieldType 字段类型
 * @property weight 字段权重
 * @property direction 排序方向
 * @property dateFormat 日期格式，仅当 fieldType 为 DATE 时使用
 * @property defaultValue 默认值，当字段不存在时使用
 */
data class MetadataFieldConfig(
    val fieldName: String,
    val fieldType: MetadataFieldType,
    val weight: Double = 1.0,
    val direction: SortDirection = SortDirection.DESCENDING,
    val dateFormat: String = "yyyy-MM-dd",
    val defaultValue: Any? = null
) {
    init {
        require(weight >= 0) { "Weight must be non-negative" }
    }
}

/**
 * 增强版基于元数据的重排序器配置。
 *
 * @property fields 元数据字段配置列表
 * @property originalScoreWeight 原始分数的权重，默认为 0.3
 * @property metadataScoreWeight 元数据分数的权重，默认为 0.7
 * @property normalizeScores 是否归一化分数，默认为 true
 */
data class EnhancedMetadataRerankerConfig(
    val fields: List<MetadataFieldConfig>,
    val originalScoreWeight: Double = 0.3,
    val metadataScoreWeight: Double = 0.7,
    val normalizeScores: Boolean = true
) {
    init {
        require(fields.isNotEmpty()) { "At least one metadata field must be specified" }
        require(originalScoreWeight >= 0) { "Original score weight must be non-negative" }
        require(metadataScoreWeight >= 0) { "Metadata score weight must be non-negative" }
        require(originalScoreWeight + metadataScoreWeight > 0) { "At least one weight must be positive" }
    }
}

/**
 * 增强版基于元数据的重排序器，根据文档元数据对搜索结果进行重排序。
 *
 * @property config 重排序器配置
 */
class EnhancedMetadataReranker(
    private val config: EnhancedMetadataRerankerConfig
) : Reranker {

    /**
     * 对搜索结果进行重排序。
     *
     * @param query 查询文本
     * @param results 原始搜索结果
     * @return 重排序后的搜索结果
     */
    override suspend fun rerank(query: String, results: List<SearchResult>): List<SearchResult> {
        if (results.isEmpty()) {
            return results
        }

        logger.debug { "Reranking ${results.size} results using metadata" }

        try {
            // 计算每个结果的元数据分数
            val metadataScores = results.map { result ->
                calculateMetadataScore(result)
            }

            // 如果需要，归一化元数据分数
            val normalizedMetadataScores = if (config.normalizeScores) {
                normalizeScores(metadataScores)
            } else {
                metadataScores
            }

            // 计算组合分数并创建新的搜索结果
            val rerankedResults = results.zip(normalizedMetadataScores) { result, metadataScore ->
                val combinedScore = (result.score * config.originalScoreWeight +
                    metadataScore * config.metadataScoreWeight) /
                    (config.originalScoreWeight + config.metadataScoreWeight)

                SearchResult(result.document, combinedScore)
            }

            // 按组合分数降序排序
            return rerankedResults.sortedByDescending { it.score }
        } catch (e: Exception) {
            logger.error(e) { "Error reranking results using metadata" }
            return results
        }
    }

    /**
     * 计算文档的元数据分数。
     *
     * @param result 搜索结果
     * @return 元数据分数
     */
    private fun calculateMetadataScore(result: SearchResult): Double {
        val metadata = result.document.metadata
        var totalScore = 0.0
        var totalWeight = 0.0

        for (fieldConfig in config.fields) {
            val fieldScore = calculateFieldScore(metadata, fieldConfig)
            totalScore += fieldScore * fieldConfig.weight
            totalWeight += fieldConfig.weight
        }

        return if (totalWeight > 0) totalScore / totalWeight else 0.0
    }

    /**
     * 计算单个元数据字段的分数。
     *
     * @param metadata 文档元数据
     * @param fieldConfig 字段配置
     * @return 字段分数
     */
    private fun calculateFieldScore(metadata: Map<String, String>, fieldConfig: MetadataFieldConfig): Double {
        val fieldValue = metadata[fieldConfig.fieldName] ?: fieldConfig.defaultValue ?: return 0.0

        return when (fieldConfig.fieldType) {
            MetadataFieldType.NUMERIC -> calculateNumericScore(fieldValue, fieldConfig)
            MetadataFieldType.DATE -> calculateDateScore(fieldValue, fieldConfig)
            MetadataFieldType.BOOLEAN -> calculateBooleanScore(fieldValue)
            MetadataFieldType.TEXT -> calculateTextScore(fieldValue, fieldConfig)
            MetadataFieldType.LIST -> calculateListScore(fieldValue, fieldConfig)
        }
    }

    /**
     * 计算数值类型字段的分数。
     *
     * @param fieldValue 字段值
     * @param fieldConfig 字段配置
     * @return 字段分数
     */
    private fun calculateNumericScore(fieldValue: Any, fieldConfig: MetadataFieldConfig): Double {
        val numericValue = when (fieldValue) {
            is Number -> fieldValue.toDouble()
            is String -> fieldValue.toDoubleOrNull() ?: return 0.0
            else -> return 0.0
        }

        // 对于数值，我们假设较大的值更好（除非方向是升序）
        return if (fieldConfig.direction == SortDirection.DESCENDING) {
            numericValue
        } else {
            -numericValue
        }
    }

    /**
     * 计算日期类型字段的分数。
     *
     * @param fieldValue 字段值
     * @param fieldConfig 字段配置
     * @return 字段分数
     */
    private fun calculateDateScore(fieldValue: Any, fieldConfig: MetadataFieldConfig): Double {
        val date = when (fieldValue) {
            is String -> parseDate(fieldValue, fieldConfig.dateFormat)
            is Number -> Instant.ofEpochMilli(fieldValue.toLong()).atZone(ZoneId.systemDefault()).toLocalDate()
            else -> return 0.0
        } ?: return 0.0

        // 计算日期距今的天数
        val daysFromNow = java.time.temporal.ChronoUnit.DAYS.between(date, LocalDate.now())

        // 对于日期，我们假设较新的日期更好（除非方向是升序）
        return if (fieldConfig.direction == SortDirection.DESCENDING) {
            -daysFromNow.toDouble()
        } else {
            daysFromNow.toDouble()
        }
    }

    /**
     * 计算布尔类型字段的分数。
     *
     * @param fieldValue 字段值
     * @return 字段分数
     */
    private fun calculateBooleanScore(fieldValue: Any): Double {
        val booleanValue = when (fieldValue) {
            is Boolean -> fieldValue
            is String -> fieldValue.lowercase() == "true" || fieldValue == "1"
            is Number -> fieldValue.toInt() != 0
            else -> false
        }

        return if (booleanValue) 1.0 else 0.0
    }

    /**
     * 计算文本类型字段的分数。
     *
     * @param fieldValue 字段值
     * @param fieldConfig 字段配置
     * @return 字段分数
     */
    private fun calculateTextScore(fieldValue: Any, fieldConfig: MetadataFieldConfig): Double {
        val textValue = fieldValue.toString()
        
        // 对于文本，我们简单地检查是否存在
        return if (textValue.isNotEmpty()) 1.0 else 0.0
    }

    /**
     * 计算列表类型字段的分数。
     *
     * @param fieldValue 字段值
     * @param fieldConfig 字段配置
     * @return 字段分数
     */
    private fun calculateListScore(fieldValue: Any, fieldConfig: MetadataFieldConfig): Double {
        val list = when (fieldValue) {
            is List<*> -> fieldValue
            is Array<*> -> fieldValue.toList()
            is String -> fieldValue.split(",").map { it.trim() }
            else -> return 0.0
        }

        // 对于列表，我们使用列表大小作为分数
        return list.size.toDouble()
    }

    /**
     * 解析日期字符串。
     *
     * @param dateString 日期字符串
     * @param format 日期格式
     * @return 解析后的日期，如果解析失败则返回 null
     */
    private fun parseDate(dateString: String, format: String): LocalDate? {
        return try {
            LocalDate.parse(dateString, DateTimeFormatter.ofPattern(format))
        } catch (e: DateTimeParseException) {
            try {
                // 尝试使用 ISO 日期格式
                LocalDate.parse(dateString)
            } catch (e: DateTimeParseException) {
                null
            }
        }
    }

    /**
     * 归一化分数列表，使其范围在 [0, 1] 之间。
     *
     * @param scores 分数列表
     * @return 归一化后的分数列表
     */
    private fun normalizeScores(scores: List<Double>): List<Double> {
        if (scores.isEmpty()) {
            return emptyList()
        }

        val min = scores.minOrNull() ?: 0.0
        val max = scores.maxOrNull() ?: 1.0

        return if (max > min) {
            scores.map { (it - min) / (max - min) }
        } else {
            scores.map { 0.5 } // 如果所有分数相同，则返回 0.5
        }
    }
}
