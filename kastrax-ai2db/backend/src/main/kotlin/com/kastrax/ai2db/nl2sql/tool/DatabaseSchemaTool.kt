package com.kastrax.ai2db.nl2sql.tool

import ai.kastrax.core.tool.Tool
import ai.kastrax.core.tool.ToolExecuteResult
import ai.kastrax.core.tool.ToolContext
import com.kastrax.ai2db.connection.manager.ConnectionManager
import com.kastrax.ai2db.schema.model.DatabaseSchema
import com.kastrax.ai2db.schema.model.TableInfo
import com.kastrax.ai2db.schema.model.ColumnInfo
import com.kastrax.ai2db.connection.model.DatabaseType
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet

/**
 * 数据库模式工具
 * 
 * 用于获取数据库的表结构、字段信息等模式数据
 */
@Component
class DatabaseSchemaTool(
    private val connectionManager: ConnectionManager
) : Tool {
    
    private val logger = LoggerFactory.getLogger(DatabaseSchemaTool::class.java)
    
    override val id: String = "database-schema"
    override val name: String = "数据库模式工具"
    override val description: String = "获取数据库的表结构、字段信息等模式数据"
    override val version: String = "1.0.0"
    
    override suspend fun execute(
        input: Map<String, Any>,
        context: ToolContext?
    ): ToolExecuteResult {
        logger.info("Executing DatabaseSchemaTool with input: {}", input)
        
        return try {
            val databaseId = input["database"] as? String
                ?: throw IllegalArgumentException("Missing 'database' parameter")
            
            val schemaName = input["schema"] as? String
            val tableNames = input["tables"] as? List<String>
            
            val schema = getSchema(databaseId, schemaName, tableNames)
            
            ToolExecuteResult(
                success = true,
                output = mapOf(
                    "schema" to schema,
                    "tables" to schema.tables.map { it.name },
                    "tableCount" to schema.tables.size
                ),
                metadata = mapOf(
                    "databaseId" to databaseId,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Error executing DatabaseSchemaTool", e)
            ToolExecuteResult(
                success = false,
                output = mapOf("error" to e.message),
                metadata = mapOf("errorType" to e.javaClass.simpleName)
            )
        }
    }
    
    /**
     * 获取数据库模式信息
     */
    private fun getSchema(
        databaseId: String,
        schemaName: String?,
        tableNames: List<String>?
    ): DatabaseSchema {
        return connectionManager.getConnection(databaseId).use { connection ->
            val metaData = connection.metaData
            val catalog = connection.catalog
            val schema = schemaName ?: getDefaultSchema(connection)
            
            val tables = if (tableNames != null) {
                tableNames.mapNotNull { tableName ->
                    getTableInfo(metaData, catalog, schema, tableName)
                }
            } else {
                getAllTables(metaData, catalog, schema)
            }
            
            DatabaseSchema(
                name = schema ?: "default",
                tables = tables,
                databaseType = detectDatabaseType(metaData)
            )
        }
    }
    
    /**
     * 获取所有表信息
     */
    private fun getAllTables(
        metaData: DatabaseMetaData,
        catalog: String?,
        schema: String?
    ): List<TableInfo> {
        val tables = mutableListOf<TableInfo>()
        
        metaData.getTables(catalog, schema, null, arrayOf("TABLE")).use { rs ->
            while (rs.next()) {
                val tableName = rs.getString("TABLE_NAME")
                val tableInfo = getTableInfo(metaData, catalog, schema, tableName)
                if (tableInfo != null) {
                    tables.add(tableInfo)
                }
            }
        }
        
        return tables
    }
    
    /**
     * 获取单个表信息
     */
    private fun getTableInfo(
        metaData: DatabaseMetaData,
        catalog: String?,
        schema: String?,
        tableName: String
    ): TableInfo? {
        return try {
            val columns = getColumns(metaData, catalog, schema, tableName)
            val primaryKeys = getPrimaryKeys(metaData, catalog, schema, tableName)
            val foreignKeys = getForeignKeys(metaData, catalog, schema, tableName)
            val indexes = getIndexes(metaData, catalog, schema, tableName)
            
            TableInfo(
                name = tableName,
                columns = columns,
                primaryKeys = primaryKeys,
                foreignKeys = foreignKeys,
                indexes = indexes
            )
        } catch (e: Exception) {
            logger.warn("Failed to get table info for {}", tableName, e)
            null
        }
    }
    
    /**
     * 获取列信息
     */
    private fun getColumns(
        metaData: DatabaseMetaData,
        catalog: String?,
        schema: String?,
        tableName: String
    ): List<ColumnInfo> {
        val columns = mutableListOf<ColumnInfo>()
        
        metaData.getColumns(catalog, schema, tableName, null).use { rs ->
            while (rs.next()) {
                val columnInfo = ColumnInfo(
                    name = rs.getString("COLUMN_NAME"),
                    type = rs.getString("TYPE_NAME"),
                    size = rs.getInt("COLUMN_SIZE"),
                    nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable,
                    defaultValue = rs.getString("COLUMN_DEF"),
                    comment = rs.getString("REMARKS"),
                    isPrimaryKey = false, // 将在后面设置
                    isForeignKey = false, // 将在后面设置
                    isUnique = false, // 将在后面设置
                    isIndexed = false // 将在后面设置
                )
                columns.add(columnInfo)
            }
        }
        
        return columns
    }
    
    /**
     * 获取主键信息
     */
    private fun getPrimaryKeys(
        metaData: DatabaseMetaData,
        catalog: String?,
        schema: String?,
        tableName: String
    ): List<String> {
        val primaryKeys = mutableListOf<String>()
        
        metaData.getPrimaryKeys(catalog, schema, tableName).use { rs ->
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"))
            }
        }
        
        return primaryKeys
    }
    
    /**
     * 获取外键信息
     */
    private fun getForeignKeys(
        metaData: DatabaseMetaData,
        catalog: String?,
        schema: String?,
        tableName: String
    ): List<String> {
        val foreignKeys = mutableListOf<String>()
        
        metaData.getImportedKeys(catalog, schema, tableName).use { rs ->
            while (rs.next()) {
                foreignKeys.add(rs.getString("FKCOLUMN_NAME"))
            }
        }
        
        return foreignKeys
    }
    
    /**
     * 获取索引信息
     */
    private fun getIndexes(
        metaData: DatabaseMetaData,
        catalog: String?,
        schema: String?,
        tableName: String
    ): List<String> {
        val indexes = mutableListOf<String>()
        
        metaData.getIndexInfo(catalog, schema, tableName, false, false).use { rs ->
            while (rs.next()) {
                val indexName = rs.getString("INDEX_NAME")
                if (indexName != null && !indexes.contains(indexName)) {
                    indexes.add(indexName)
                }
            }
        }
        
        return indexes
    }
    
    /**
     * 获取默认模式名
     */
    private fun getDefaultSchema(connection: Connection): String? {
        return try {
            connection.schema
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 检测数据库类型
     */
    private fun detectDatabaseType(metaData: DatabaseMetaData): DatabaseType {
        val productName = metaData.databaseProductName.lowercase()
        return when {
            productName.contains("mysql") -> DatabaseType.MYSQL
            productName.contains("postgresql") -> DatabaseType.POSTGRESQL
            productName.contains("oracle") -> DatabaseType.ORACLE
            productName.contains("sql server") -> DatabaseType.SQL_SERVER
            productName.contains("sqlite") -> DatabaseType.SQLITE
            else -> DatabaseType.MYSQL // 默认
        }
    }
}