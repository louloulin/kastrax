package com.kastrax.ai2db.schema

import com.kastrax.ai2db.schema.model.DatabaseSchema
import com.kastrax.ai2db.schema.model.TableSchema
import com.kastrax.ai2db.schema.model.ColumnSchema
import com.kastrax.ai2db.schema.model.IndexSchema
import com.kastrax.ai2db.schema.model.RelationshipSchema
import com.kastrax.ai2db.connection.model.DatabaseConnection
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * 数据库模式管理器 - Micronaut版本
 * 
 * 负责获取、缓存和管理数据库结构信息
 */
@Singleton
class SchemaManager(
    @Property(name = "kastrax.ai2db.schema.cache-ttl", defaultValue = "3600")
    private val cacheTtlSeconds: Long,
    
    @Property(name = "kastrax.ai2db.schema.max-cache-size", defaultValue = "100")
    private val maxCacheSize: Int
) {
    private val logger = LoggerFactory.getLogger(SchemaManager::class.java)
    
    // 缓存结构：connectionId -> CachedSchema
    private val schemaCache = ConcurrentHashMap<String, CachedSchema>()
    
    /**
     * 缓存的模式数据
     */
    private data class CachedSchema(
        val schema: DatabaseSchema,
        val cachedAt: LocalDateTime
    ) {
        fun isExpired(ttlSeconds: Long): Boolean {
            return cachedAt.plusSeconds(ttlSeconds).isBefore(LocalDateTime.now())
        }
    }
    
    /**
     * 获取数据库模式
     *
     * @param connection 数据库连接
     * @param forceRefresh 是否强制刷新缓存
     * @return 数据库模式
     */
    suspend fun getSchema(
        connection: DatabaseConnection,
        forceRefresh: Boolean = false
    ): DatabaseSchema = withContext(Dispatchers.IO) {
        val connectionId = connection.id
        
        // 检查缓存
        if (!forceRefresh) {
            schemaCache[connectionId]?.let { cached ->
                if (!cached.isExpired(cacheTtlSeconds)) {
                    logger.debug("Returning cached schema for connection: {}", connectionId)
                    return@withContext cached.schema
                }
            }
        }
        
        logger.info("Fetching schema for connection: {}", connectionId)
        
        try {
            val schema = extractSchema(connection)
            
            // 更新缓存
            updateCache(connectionId, schema)
            
            logger.info("Successfully fetched schema with {} tables for connection: {}", 
                schema.tables.size, connectionId)
            
            schema
        } catch (e: Exception) {
            logger.error("Failed to fetch schema for connection: {}", connectionId, e)
            throw e
        }
    }
    
    /**
     * 提取数据库模式
     */
    private suspend fun extractSchema(connection: DatabaseConnection): DatabaseSchema = withContext(Dispatchers.IO) {
        connection.jdbcConnection.use { jdbcConn ->
            val metaData = jdbcConn.metaData
            val catalog = jdbcConn.catalog
            val schema = jdbcConn.schema
            
            logger.debug("Extracting schema from catalog: {}, schema: {}", catalog, schema)
            
            val tables = extractTables(metaData, catalog, schema)
            val relationships = extractRelationships(metaData, catalog, schema, tables)
            
            DatabaseSchema(
                name = catalog ?: schema ?: "default",
                type = connection.config.type,
                tables = tables,
                relationships = relationships,
                version = extractDatabaseVersion(metaData)
            )
        }
    }
    
    /**
     * 提取表信息
     */
    private fun extractTables(
        metaData: DatabaseMetaData,
        catalog: String?,
        schema: String?
    ): List<TableSchema> {
        val tables = mutableListOf<TableSchema>()
        
        metaData.getTables(catalog, schema, null, arrayOf("TABLE", "VIEW")).use { rs ->
            while (rs.next()) {
                val tableName = rs.getString("TABLE_NAME")
                val tableType = rs.getString("TABLE_TYPE")
                val remarks = rs.getString("REMARKS") ?: ""
                
                logger.debug("Processing table: {} ({})", tableName, tableType)
                
                val columns = extractColumns(metaData, catalog, schema, tableName)
                val indexes = extractIndexes(metaData, catalog, schema, tableName)
                val primaryKeys = extractPrimaryKeys(metaData, catalog, schema, tableName)
                
                // 标记主键列
                columns.forEach { column ->
                    if (primaryKeys.contains(column.name)) {
                        column.isPrimaryKey = true
                    }
                }
                
                tables.add(
                    TableSchema(
                        name = tableName,
                        type = tableType,
                        comment = remarks,
                        columns = columns,
                        indexes = indexes
                    )
                )
            }
        }
        
        logger.debug("Extracted {} tables", tables.size)
        return tables
    }
    
    /**
     * 提取列信息
     */
    private fun extractColumns(
        metaData: DatabaseMetaData,
        catalog: String?,
        schema: String?,
        tableName: String
    ): MutableList<ColumnSchema> {
        val columns = mutableListOf<ColumnSchema>()
        
        metaData.getColumns(catalog, schema, tableName, null).use { rs ->
            while (rs.next()) {
                val columnName = rs.getString("COLUMN_NAME")
                val dataType = rs.getString("TYPE_NAME")
                val columnSize = rs.getInt("COLUMN_SIZE")
                val nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable
                val defaultValue = rs.getString("COLUMN_DEF") ?: ""
                val remarks = rs.getString("REMARKS") ?: ""
                val ordinalPosition = rs.getInt("ORDINAL_POSITION")
                
                columns.add(
                    ColumnSchema(
                        name = columnName,
                        dataType = dataType,
                        maxLength = columnSize,
                        isNullable = nullable,
                        defaultValue = defaultValue,
                        comment = remarks,
                        ordinalPosition = ordinalPosition
                    )
                )
            }
        }
        
        // 按位置排序
        columns.sortBy { it.ordinalPosition }
        
        return columns
    }
    
    /**
     * 提取主键信息
     */
    private fun extractPrimaryKeys(
        metaData: DatabaseMetaData,
        catalog: String?,
        schema: String?,
        tableName: String
    ): Set<String> {
        val primaryKeys = mutableSetOf<String>()
        
        try {
            metaData.getPrimaryKeys(catalog, schema, tableName).use { rs ->
                while (rs.next()) {
                    val columnName = rs.getString("COLUMN_NAME")
                    primaryKeys.add(columnName)
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to extract primary keys for table: {}", tableName, e)
        }
        
        return primaryKeys
    }
    
    /**
     * 提取索引信息
     */
    private fun extractIndexes(
        metaData: DatabaseMetaData,
        catalog: String?,
        schema: String?,
        tableName: String
    ): List<IndexSchema> {
        val indexes = mutableListOf<IndexSchema>()
        
        try {
            metaData.getIndexInfo(catalog, schema, tableName, false, false).use { rs ->
                val indexMap = mutableMapOf<String, MutableList<String>>()
                val uniqueMap = mutableMapOf<String, Boolean>()
                
                while (rs.next()) {
                    val indexName = rs.getString("INDEX_NAME")
                    if (indexName != null) {
                        val columnName = rs.getString("COLUMN_NAME")
                        val nonUnique = rs.getBoolean("NON_UNIQUE")
                        
                        indexMap.computeIfAbsent(indexName) { mutableListOf() }.add(columnName)
                        uniqueMap[indexName] = !nonUnique
                    }
                }
                
                indexMap.forEach { (indexName, columns) ->
                    indexes.add(
                        IndexSchema(
                            name = indexName,
                            columns = columns,
                            isUnique = uniqueMap[indexName] ?: false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to extract indexes for table: {}", tableName, e)
        }
        
        return indexes
    }
    
    /**
     * 提取关系信息
     */
    private fun extractRelationships(
        metaData: DatabaseMetaData,
        catalog: String?,
        schema: String?,
        tables: List<TableSchema>
    ): List<RelationshipSchema> {
        val relationships = mutableListOf<RelationshipSchema>()
        
        tables.forEach { table ->
            try {
                // 获取外键关系
                metaData.getImportedKeys(catalog, schema, table.name).use { rs ->
                    while (rs.next()) {
                        val fkTableName = rs.getString("FKTABLE_NAME")
                        val fkColumnName = rs.getString("FKCOLUMN_NAME")
                        val pkTableName = rs.getString("PKTABLE_NAME")
                        val pkColumnName = rs.getString("PKCOLUMN_NAME")
                        val fkName = rs.getString("FK_NAME")
                        
                        relationships.add(
                            RelationshipSchema(
                                name = fkName ?: "${fkTableName}_${pkTableName}_fk",
                                fromTable = fkTableName,
                                fromColumn = fkColumnName,
                                toTable = pkTableName,
                                toColumn = pkColumnName,
                                type = "FOREIGN_KEY"
                            )
                        )
                        
                        // 标记外键列
                        table.columns.find { it.name == fkColumnName }?.isForeignKey = true
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to extract relationships for table: {}", table.name, e)
            }
        }
        
        logger.debug("Extracted {} relationships", relationships.size)
        return relationships
    }
    
    /**
     * 提取数据库版本
     */
    private fun extractDatabaseVersion(metaData: DatabaseMetaData): String {
        return try {
            "${metaData.databaseProductName} ${metaData.databaseProductVersion}"
        } catch (e: Exception) {
            logger.warn("Failed to extract database version", e)
            "Unknown"
        }
    }
    
    /**
     * 更新缓存
     */
    private fun updateCache(connectionId: String, schema: DatabaseSchema) {
        // 检查缓存大小限制
        if (schemaCache.size >= maxCacheSize) {
            // 移除最旧的缓存项
            val oldestEntry = schemaCache.entries.minByOrNull { it.value.cachedAt }
            oldestEntry?.let {
                schemaCache.remove(it.key)
                logger.debug("Removed oldest cache entry for connection: {}", it.key)
            }
        }
        
        schemaCache[connectionId] = CachedSchema(schema, LocalDateTime.now())
        logger.debug("Updated schema cache for connection: {}", connectionId)
    }
    
    /**
     * 清除缓存
     */
    fun clearCache(connectionId: String? = null) {
        if (connectionId != null) {
            schemaCache.remove(connectionId)
            logger.info("Cleared schema cache for connection: {}", connectionId)
        } else {
            schemaCache.clear()
            logger.info("Cleared all schema cache")
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "cacheSize" to schemaCache.size,
            "maxCacheSize" to maxCacheSize,
            "cacheTtlSeconds" to cacheTtlSeconds,
            "cachedConnections" to schemaCache.keys.toList()
        )
    }
    
    /**
     * 检查表是否存在
     */
    suspend fun tableExists(
        connection: DatabaseConnection,
        tableName: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val schema = getSchema(connection)
            schema.tables.any { it.name.equals(tableName, ignoreCase = true) }
        } catch (e: Exception) {
            logger.error("Failed to check if table exists: {}", tableName, e)
            false
        }
    }
    
    /**
     * 获取表的列信息
     */
    suspend fun getTableColumns(
        connection: DatabaseConnection,
        tableName: String
    ): List<ColumnSchema> = withContext(Dispatchers.IO) {
        try {
            val schema = getSchema(connection)
            schema.tables.find { it.name.equals(tableName, ignoreCase = true) }?.columns ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to get columns for table: {}", tableName, e)
            emptyList()
        }
    }
}