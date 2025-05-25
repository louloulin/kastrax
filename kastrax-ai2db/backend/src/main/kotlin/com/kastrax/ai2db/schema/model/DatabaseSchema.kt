package com.kastrax.ai2db.schema.model

import java.time.Instant

/**
 * Schema information for a database
 */
data class DatabaseSchema(
    val tables: List<TableSchema>,
    val relationships: List<Relationship>,
    val version: String,
    val lastSynced: Instant,
    val metadata: Map<String, Any> = mapOf()
)

/**
 * Schema information for a table
 */
data class TableSchema(
    val name: String,
    val columns: List<ColumnSchema>,
    val primaryKey: List<String>,
    val indexes: List<IndexInfo> = listOf(),
    val description: String? = null,
    val estimatedRowCount: Long? = null,
    val schema: String? = null
)

/**
 * Schema information for a column
 */
data class ColumnSchema(
    val name: String,
    val dataType: String,
    val typeName: String,
    val size: Int? = null,
    val isNullable: Boolean,
    val isPrimaryKey: Boolean,
    val isForeignKey: Boolean,
    val defaultValue: String? = null,
    val description: String? = null,
    val position: Int
)

/**
 * Information about an index
 */
data class IndexInfo(
    val name: String,
    val columns: List<String>,
    val isUnique: Boolean,
    val type: String? = null
)

/**
 * Information about a relationship between tables
 */
data class Relationship(
    val id: String,
    val sourceTable: String,
    val sourceColumn: String,
    val targetTable: String,
    val targetColumn: String,
    val relationshipType: RelationshipType,
    val name: String? = null
)

/**
 * Type of relationship between tables
 */
enum class RelationshipType {
    ONE_TO_ONE,
    ONE_TO_MANY,
    MANY_TO_ONE,
    MANY_TO_MANY
} 