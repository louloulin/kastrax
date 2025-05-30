package com.kastrax.ai2db.nl2sql.tool

import ai.kastrax.core.tool.ZodTool
import ai.kastrax.zod.*
import com.kastrax.ai2db.schema.SchemaManager
import com.kastrax.ai2db.schema.model.DatabaseSchema
import com.kastrax.ai2db.schema.model.TableSchema
import com.kastrax.ai2db.schema.model.ColumnSchema
import com.kastrax.ai2db.connection.manager.ConnectionManager
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * 模式查询输入
 */
data class SchemaQueryInput(
    val database: String,
    val tables: List<String>? = null,
    val includeRelationships: Boolean = true,
    val includeConstraints: Boolean = true
)

/**
 * 模式查询输出
 */
data class SchemaQueryOutput(
    val schema: DatabaseSchemaInfo,
    val tables: List<TableInfo>,
    val relationships: List<RelationshipInfo>,
    val constraints: List<ConstraintInfo>
)

/**
 * 数据库模式信息
 */
data class DatabaseSchemaInfo(
    val name: String,
    val type: String,
    val version: String,
    val tableCount: Int,
    val description: String?
)

/**
 * 表信息
 */
data class TableInfo(
    val name: String,
    val type: String,
    val columns: List<ColumnInfo>,
    val primaryKeys: List<String>,
    val foreignKeys: List<ForeignKeyInfo>,
    val indexes: List<IndexInfo>,
    val rowCount: Long?,
    val description: String?
)

/**
 * 列信息
 */
data class ColumnInfo(
    val name: String,
    val type: String,
    val nullable: Boolean,
    val defaultValue: String?,
    val isPrimaryKey: Boolean,
    val isForeignKey: Boolean,
    val isUnique: Boolean,
    val description: String?
)

/**
 * 外键信息
 */
data class ForeignKeyInfo(
    val columnName: String,
    val referencedTable: String,
    val referencedColumn: String,
    val constraintName: String
)

/**
 * 索引信息
 */
data class IndexInfo(
    val name: String,
    val columns: List<String>,
    val isUnique: Boolean,
    val type: String
)

/**
 * 关系信息
 */
data class RelationshipInfo(
    val fromTable: String,
    val toTable: String,
    val relationshipType: String,
    val foreignKey: ForeignKeyInfo
)

/**
 * 约束信息
 */
data class ConstraintInfo(
    val name: String,
    val type: String,
    val table: String,
    val columns: List<String>,
    val definition: String
)

/**
 * 数据库模式工具
 * 
 * 用于获取数据库表结构、关系和约束信息
 */
@Singleton
class DatabaseSchemaTool(
    private val schemaManager: SchemaManager,
    private val connectionManager: ConnectionManager
) : ZodTool<SchemaQueryInput, SchemaQueryOutput> {
    
    private val logger = LoggerFactory.getLogger(DatabaseSchemaTool::class.java)
    
    override val id = "database-schema"
    override val name = "数据库模式查询"
    override val description = "获取数据库表结构、关系和约束信息"
    
    override val inputSchema = objectInput {
        "database" to stringField("数据库连接ID或名称")
        "tables" to arrayField("指定查询的表名列表", required = false) { stringField() }
        "includeRelationships" to booleanField("是否包含表关系信息", required = false)
        "includeConstraints" to booleanField("是否包含约束信息", required = false)
    }
    
    override val outputSchema = objectOutput {
        "schema" to objectField("数据库模式信息") {
            "name" to stringField("模式名称")
            "type" to stringField("数据库类型")
            "version" to stringField("版本")
            "tableCount" to numberField("表数量")
            "description" to stringField("描述", required = false)
        }
        "tables" to arrayField("表信息列表") {
            objectField {
                "name" to stringField("表名")
                "type" to stringField("表类型")
                "columns" to arrayField("列信息") {
                    objectField {
                        "name" to stringField("列名")
                        "type" to stringField("数据类型")
                        "nullable" to booleanField("是否可空")
                        "defaultValue" to stringField("默认值", required = false)
                        "isPrimaryKey" to booleanField("是否主键")
                        "isForeignKey" to booleanField("是否外键")
                        "isUnique" to booleanField("是否唯一")
                        "description" to stringField("描述", required = false)
                    }
                }
                "primaryKeys" to arrayField("主键列表") { stringField() }
                "foreignKeys" to arrayField("外键列表") {
                    objectField {
                        "columnName" to stringField("列名")
                        "referencedTable" to stringField("引用表")
                        "referencedColumn" to stringField("引用列")
                        "constraintName" to stringField("约束名")
                    }
                }
                "indexes" to arrayField("索引列表") {
                    objectField {
                        "name" to stringField("索引名")
                        "columns" to arrayField("索引列") { stringField() }
                        "isUnique" to booleanField("是否唯一索引")
                        "type" to stringField("索引类型")
                    }
                }
                "rowCount" to numberField("行数", required = false)
                "description" to stringField("表描述", required = false)
            }
        }
        "relationships" to arrayField("表关系信息") {
            objectField {
                "fromTable" to stringField("源表")
                "toTable" to stringField("目标表")
                "relationshipType" to stringField("关系类型")
                "foreignKey" to objectField("外键信息") {
                    "columnName" to stringField("列名")
                    "referencedTable" to stringField("引用表")
                    "referencedColumn" to stringField("引用列")
                    "constraintName" to stringField("约束名")
                }
            }
        }
        "constraints" to arrayField("约束信息") {
            objectField {
                "name" to stringField("约束名")
                "type" to stringField("约束类型")
                "table" to stringField("所属表")
                "columns" to arrayField("约束列") { stringField() }
                "definition" to stringField("约束定义")
            }
        }
    }
    
    override suspend fun execute(input: SchemaQueryInput): SchemaQueryOutput = withContext(Dispatchers.IO) {
        logger.info("Querying database schema for: {}", input.database)
        
        try {
            // 获取数据库连接
            val connection = connectionManager.getConnection(input.database)
                ?: throw IllegalArgumentException("Database connection not found: ${input.database}")
            
            // 获取数据库模式
            val schema = schemaManager.getSchema(connection, input.tables)
            
            // 转换为输出格式
            val schemaInfo = DatabaseSchemaInfo(
                name = schema.name,
                type = schema.type,
                version = schema.version,
                tableCount = schema.tables.size,
                description = schema.description
            )
            
            // 转换表信息
            val tables = schema.tables.map { table ->
                convertTableToInfo(table, connection)
            }
            
            // 获取关系信息
            val relationships = if (input.includeRelationships) {
                extractRelationships(schema)
            } else {
                emptyList()
            }
            
            // 获取约束信息
            val constraints = if (input.includeConstraints) {
                extractConstraints(schema)
            } else {
                emptyList()
            }
            
            logger.info("Successfully retrieved schema with {} tables, {} relationships, {} constraints",
                tables.size, relationships.size, constraints.size)
            
            return@withContext SchemaQueryOutput(
                schema = schemaInfo,
                tables = tables,
                relationships = relationships,
                constraints = constraints
            )
            
        } catch (e: Exception) {
            logger.error("Error querying database schema", e)
            throw e
        }
    }
    
    /**
     * 转换表模式为表信息
     */
    private suspend fun convertTableToInfo(
        table: TableSchema,
        connection: com.kastrax.ai2db.connection.model.DatabaseConnection
    ): TableInfo {
        // 转换列信息
        val columns = table.columns.map { column ->
            ColumnInfo(
                name = column.name,
                type = column.type,
                nullable = column.nullable,
                defaultValue = column.defaultValue,
                isPrimaryKey = column.isPrimaryKey,
                isForeignKey = column.isForeignKey,
                isUnique = column.isUnique,
                description = column.description
            )
        }
        
        // 提取主键
        val primaryKeys = table.columns
            .filter { it.isPrimaryKey }
            .map { it.name }
        
        // 提取外键
        val foreignKeys = table.columns
            .filter { it.isForeignKey }
            .mapNotNull { column ->
                // 这里需要从实际的外键约束中获取信息
                // 暂时使用简化的逻辑
                if (column.referencedTable != null && column.referencedColumn != null) {
                    ForeignKeyInfo(
                        columnName = column.name,
                        referencedTable = column.referencedTable!!,
                        referencedColumn = column.referencedColumn!!,
                        constraintName = "fk_${table.name}_${column.name}"
                    )
                } else {
                    null
                }
            }
        
        // 获取索引信息
        val indexes = getTableIndexes(table, connection)
        
        // 获取行数（可选）
        val rowCount = try {
            getTableRowCount(table.name, connection)
        } catch (e: Exception) {
            logger.warn("Failed to get row count for table: ${table.name}", e)
            null
        }
        
        return TableInfo(
            name = table.name,
            type = table.type ?: "TABLE",
            columns = columns,
            primaryKeys = primaryKeys,
            foreignKeys = foreignKeys,
            indexes = indexes,
            rowCount = rowCount,
            description = table.description
        )
    }
    
    /**
     * 提取表关系信息
     */
    private fun extractRelationships(schema: DatabaseSchema): List<RelationshipInfo> {
        val relationships = mutableListOf<RelationshipInfo>()
        
        schema.tables.forEach { table ->
            table.columns.filter { it.isForeignKey }.forEach { column ->
                if (column.referencedTable != null && column.referencedColumn != null) {
                    relationships.add(RelationshipInfo(
                        fromTable = table.name,
                        toTable = column.referencedTable!!,
                        relationshipType = "FOREIGN_KEY",
                        foreignKey = ForeignKeyInfo(
                            columnName = column.name,
                            referencedTable = column.referencedTable!!,
                            referencedColumn = column.referencedColumn!!,
                            constraintName = "fk_${table.name}_${column.name}"
                        )
                    ))
                }
            }
        }
        
        return relationships
    }
    
    /**
     * 提取约束信息
     */
    private fun extractConstraints(schema: DatabaseSchema): List<ConstraintInfo> {
        val constraints = mutableListOf<ConstraintInfo>()
        
        schema.tables.forEach { table ->
            // 主键约束
            val primaryKeyColumns = table.columns.filter { it.isPrimaryKey }
            if (primaryKeyColumns.isNotEmpty()) {
                constraints.add(ConstraintInfo(
                    name = "pk_${table.name}",
                    type = "PRIMARY_KEY",
                    table = table.name,
                    columns = primaryKeyColumns.map { it.name },
                    definition = "PRIMARY KEY (${primaryKeyColumns.joinToString(", ") { it.name }})"
                ))
            }
            
            // 外键约束
            table.columns.filter { it.isForeignKey }.forEach { column ->
                if (column.referencedTable != null && column.referencedColumn != null) {
                    constraints.add(ConstraintInfo(
                        name = "fk_${table.name}_${column.name}",
                        type = "FOREIGN_KEY",
                        table = table.name,
                        columns = listOf(column.name),
                        definition = "FOREIGN KEY (${column.name}) REFERENCES ${column.referencedTable}(${column.referencedColumn})"
                    ))
                }
            }
            
            // 唯一约束
            table.columns.filter { it.isUnique && !it.isPrimaryKey }.forEach { column ->
                constraints.add(ConstraintInfo(
                    name = "uk_${table.name}_${column.name}",
                    type = "UNIQUE",
                    table = table.name,
                    columns = listOf(column.name),
                    definition = "UNIQUE (${column.name})"
                ))
            }
        }
        
        return constraints
    }
    
    /**
     * 获取表索引信息
     */
    private suspend fun getTableIndexes(
        table: TableSchema,
        connection: com.kastrax.ai2db.connection.model.DatabaseConnection
    ): List<IndexInfo> {
        return try {
            // 这里需要实际查询数据库获取索引信息
            // 暂时返回基于主键和唯一列的基本索引
            val indexes = mutableListOf<IndexInfo>()
            
            // 主键索引
            val primaryKeyColumns = table.columns.filter { it.isPrimaryKey }
            if (primaryKeyColumns.isNotEmpty()) {
                indexes.add(IndexInfo(
                    name = "pk_${table.name}",
                    columns = primaryKeyColumns.map { it.name },
                    isUnique = true,
                    type = "PRIMARY"
                ))
            }
            
            // 唯一索引
            table.columns.filter { it.isUnique && !it.isPrimaryKey }.forEach { column ->
                indexes.add(IndexInfo(
                    name = "uk_${table.name}_${column.name}",
                    columns = listOf(column.name),
                    isUnique = true,
                    type = "UNIQUE"
                ))
            }
            
            indexes
        } catch (e: Exception) {
            logger.warn("Failed to get indexes for table: ${table.name}", e)
            emptyList()
        }
    }
    
    /**
     * 获取表行数
     */
    private suspend fun getTableRowCount(
        tableName: String,
        connection: com.kastrax.ai2db.connection.model.DatabaseConnection
    ): Long? {
        return try {
            val sql = "SELECT COUNT(*) FROM $tableName"
            val statement = connection.jdbcConnection.prepareStatement(sql)
            val resultSet = statement.executeQuery()
            
            if (resultSet.next()) {
                resultSet.getLong(1)
            } else {
                null
            }
        } catch (e: Exception) {
            logger.warn("Failed to get row count for table: $tableName", e)
            null
        }
    }
}