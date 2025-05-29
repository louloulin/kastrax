package com.kastrax.ai2db.schema.model

/**
 * 数据库模式
 */
data class DatabaseSchema(
    val name: String,
    val type: String,
    val tables: List<TableSchema>,
    val relationships: List<RelationshipSchema>,
    val version: String = ""
)

/**
 * 表模式
 */
data class TableSchema(
    val name: String,
    val type: String = "TABLE", // TABLE, VIEW
    val comment: String = "",
    val columns: MutableList<ColumnSchema>,
    val indexes: List<IndexSchema> = emptyList(),
    val rowCount: Long = 0
)

/**
 * 列模式
 */
data class ColumnSchema(
    val name: String,
    val dataType: String,
    val maxLength: Int = 0,
    val precision: Int = 0,
    val scale: Int = 0,
    val isNullable: Boolean = true,
    var isPrimaryKey: Boolean = false,
    var isForeignKey: Boolean = false,
    val isUnique: Boolean = false,
    val isAutoIncrement: Boolean = false,
    val defaultValue: String = "",
    val comment: String = "",
    val ordinalPosition: Int = 0
)

/**
 * 索引模式
 */
data class IndexSchema(
    val name: String,
    val columns: List<String>,
    val isUnique: Boolean = false,
    val isPrimary: Boolean = false,
    val type: String = "BTREE" // BTREE, HASH, etc.
)

/**
 * 关系模式
 */
data class RelationshipSchema(
    val name: String,
    val fromTable: String,
    val fromColumn: String,
    val toTable: String,
    val toColumn: String,
    val type: String, // FOREIGN_KEY, ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY
    val onDelete: String = "RESTRICT", // CASCADE, SET_NULL, RESTRICT
    val onUpdate: String = "RESTRICT"
)

/**
 * 数据库统计信息
 */
data class DatabaseStats(
    val tableCount: Int,
    val totalRows: Long,
    val totalSize: Long, // bytes
    val indexCount: Int,
    val relationshipCount: Int
)

/**
 * 表统计信息
 */
data class TableStats(
    val tableName: String,
    val rowCount: Long,
    val dataSize: Long, // bytes
    val indexSize: Long, // bytes
    val lastUpdated: String = ""
)

/**
 * 列统计信息
 */
data class ColumnStats(
    val columnName: String,
    val distinctCount: Long,
    val nullCount: Long,
    val minValue: String = "",
    val maxValue: String = "",
    val avgLength: Double = 0.0
)

/**
 * 数据库性能指标
 */
data class DatabasePerformance(
    val connectionCount: Int,
    val activeQueries: Int,
    val avgQueryTime: Double, // milliseconds
    val slowQueries: List<SlowQuery> = emptyList()
)

/**
 * 慢查询信息
 */
data class SlowQuery(
    val sql: String,
    val executionTime: Long, // milliseconds
    val timestamp: String,
    val affectedRows: Long = 0
)