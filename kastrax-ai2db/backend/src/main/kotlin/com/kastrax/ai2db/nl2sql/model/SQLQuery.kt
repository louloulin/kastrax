package com.kastrax.ai2db.nl2sql.model

/**
 * Represents a SQL query
 */
data class SQLQuery(
    val sql: String,
    val parameters: List<Any> = listOf(),
    val queryType: QueryType,
    val tables: List<String>,
    val columns: List<String> = listOf(),
    val conditions: List<String> = listOf(),
    val estimatedComplexity: QueryComplexity = QueryComplexity.MEDIUM
)

/**
 * Types of SQL queries
 */
enum class QueryType {
    SELECT,
    INSERT,
    UPDATE,
    DELETE,
    CREATE,
    ALTER,
    DROP,
    OTHER
}

/**
 * Complexity levels for queries
 */
enum class QueryComplexity {
    SIMPLE,
    MEDIUM,
    COMPLEX,
    VERY_COMPLEX
}

/**
 * Explanation of the SQL query conversion process
 */
data class ConversionExplanation(
    val steps: List<ConversionStep>,
    val entityRecognition: Map<String, String>,
    val alternatives: List<String>,
    val confidence: Float
)

/**
 * A step in the conversion process
 */
data class ConversionStep(
    val step: String,
    val reasoning: String,
    val intermediateResult: String
) 