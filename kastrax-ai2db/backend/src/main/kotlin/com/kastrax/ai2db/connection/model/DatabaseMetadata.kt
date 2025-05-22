package com.kastrax.ai2db.connection.model

import java.time.Instant

/**
 * Metadata about a database
 */
data class DatabaseMetadata(
    val databaseName: String,
    val tables: List<TableMetadata>,
    val version: String,
    val extractedAt: Instant = Instant.now(),
    val serverInfo: Map<String, String> = mapOf()
)

/**
 * Metadata about a database table
 */
data class TableMetadata(
    val name: String,
    val schema: String? = null,
    val columns: List<ColumnMetadata>,
    val primaryKey: List<String>,
    val indexes: List<IndexMetadata>,
    val rowCount: Long? = null,
    val description: String? = null
)

/**
 * Metadata about a table column
 */
data class ColumnMetadata(
    val name: String,
    val dataType: String,
    val typeName: String,
    val size: Int?,
    val isNullable: Boolean,
    val isPrimaryKey: Boolean,
    val isForeignKey: Boolean,
    val defaultValue: String?,
    val description: String? = null,
    val position: Int
)

/**
 * Metadata about a table index
 */
data class IndexMetadata(
    val name: String,
    val columns: List<String>,
    val isUnique: Boolean,
    val type: String? = null
) 