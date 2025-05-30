package com.kastrax.ai2db.nl2sql.model

/**
 * SQL生成结果
 */
data class SQLGenerationResult(
    val sql: String,
    val confidence: Double,
    val explanation: String,
    val queryType: QueryType = QueryType.SELECT,
    val complexity: QueryComplexity = QueryComplexity.MEDIUM,
    val estimatedExecutionTime: Long? = null,
    val warnings: List<String> = emptyList()
)

/**
 * 转换解释
 */
data class ConversionExplanation(
    val steps: List<ConversionStep>,
    val entityRecognition: Map<String, String>,
    val alternatives: List<String>,
    val confidence: Float
)

/**
 * 转换步骤
 */
data class ConversionStep(
    val stepNumber: Int,
    val description: String,
    val sqlFragment: String? = null,
    val reasoning: String? = null
)