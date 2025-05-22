package com.kastrax.ai2db.connection.model

/**
 * Result of a database query
 */
data class QueryResult(
    val columns: List<Column>,
    val rows: List<List<Any?>>,
    val rowCount: Int,
    val executionTimeMs: Long,
    val metadata: Map<String, Any> = mapOf()
)

/**
 * Information about a column in a query result
 */
data class Column(
    val name: String,
    val label: String,
    val type: String,
    val typeName: String
)

/**
 * Result of an update operation
 */
data class UpdateResult(
    val affectedRows: Int,
    val generatedKeys: List<Any> = listOf(),
    val executionTimeMs: Long
)

/**
 * Represents a database transaction
 */
data class Transaction(
    val id: String,
    val connectionId: String,
    val rawTransaction: Any? = null // The actual native transaction object
) 