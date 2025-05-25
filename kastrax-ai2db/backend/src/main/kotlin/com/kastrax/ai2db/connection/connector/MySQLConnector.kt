package com.kastrax.ai2db.connection.connector

import com.kastrax.ai2db.connection.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import org.springframework.stereotype.Component
import java.sql.Connection as JavaSqlConnection
import java.sql.Types
import java.sql.Timestamp
import java.time.Instant
import java.util.*
import javax.sql.DataSource

/**
 * MySQL implementation of the DatabaseConnector interface using JdbcClient
 */
@Component
class MySQLConnector : DatabaseConnector {
    
    /**
     * Connect to a MySQL database
     */
    override suspend fun connect(config: ConnectionConfig): Connection = withContext(Dispatchers.IO) {
        try {
            val dataSource = createDataSource(config)
            
            // Create JdbcClient from dataSource to test the connection
            val jdbcClient = JdbcClient.create(dataSource)
            
            // Validate connection
            jdbcClient.sql("SELECT 1").query(Int::class.java).single()
            
            return@withContext Connection(
                id = UUID.randomUUID().toString(),
                config = config,
                connectedAt = Instant.now(),
                dataSource = dataSource
            )
        } catch (e: Exception) {
            throw ConnectionException("Failed to connect to MySQL database: ${e.message}", e)
        }
    }
    
    /**
     * Disconnect from a MySQL database
     */
    override suspend fun disconnect(connection: Connection): Boolean = withContext(Dispatchers.IO) {
        try {
            (connection.dataSource as? SingleConnectionDataSource)?.destroy()
            return@withContext true
        } catch (e: Exception) {
            throw ConnectionException("Failed to disconnect from MySQL database: ${e.message}", e)
        }
    }
    
    /**
     * Test a MySQL database connection
     */
    override suspend fun testConnection(config: ConnectionConfig): ConnectionStatus = withContext(Dispatchers.IO) {
        try {
            val dataSource = createDataSource(config)
            val jdbcClient = JdbcClient.create(dataSource)
            
            // Test connection
            jdbcClient.sql("SELECT 1").query(Int::class.java).single()
            
            // Close connection
            (dataSource as? SingleConnectionDataSource)?.destroy()
            
            return@withContext ConnectionStatus.CONNECTED
        } catch (e: Exception) {
            return@withContext ConnectionStatus.FAILED
        }
    }
    
    /**
     * Get metadata about a MySQL database
     */
    override suspend fun getMetadata(connection: Connection): DatabaseMetadata = withContext(Dispatchers.IO) {
        val jdbcClient = JdbcClient.create(connection.dataSource!!)
        val tables = mutableListOf<TableMetadata>()
        
        // Get list of tables
        val tableRows = jdbcClient.sql(
            """
            SELECT TABLE_NAME, TABLE_COMMENT 
            FROM INFORMATION_SCHEMA.TABLES 
            WHERE TABLE_SCHEMA = :schema
            """
        )
        .param("schema", connection.config.database)
        .query { rs, _ ->
            mapOf(
                "TABLE_NAME" to rs.getString("TABLE_NAME"),
                "TABLE_COMMENT" to (rs.getString("TABLE_COMMENT") ?: "")
            )
        }
        .list()
        
        for (tableRow in tableRows) {
            val tableName = tableRow["TABLE_NAME"] as String
            val tableComment = tableRow["TABLE_COMMENT"] as String
            
            // Get columns for this table
            val columns = jdbcClient.sql(
                """
                SELECT 
                    COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, IS_NULLABLE, 
                    COLUMN_KEY, COLUMN_COMMENT, COLUMN_DEFAULT, ORDINAL_POSITION
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = :schema AND TABLE_NAME = :tableName
                ORDER BY ORDINAL_POSITION
                """
            )
            .param("schema", connection.config.database)
            .param("tableName", tableName)
            .query { rs, _ ->
                ColumnMetadata(
                    name = rs.getString("COLUMN_NAME"),
                    dataType = rs.getString("DATA_TYPE"),
                    typeName = rs.getString("COLUMN_TYPE"),
                    size = null,
                    isNullable = rs.getString("IS_NULLABLE") == "YES",
                    isPrimaryKey = rs.getString("COLUMN_KEY") == "PRI",
                    isForeignKey = false, // Will update later with foreign key info
                    defaultValue = rs.getString("COLUMN_DEFAULT"),
                    description = rs.getString("COLUMN_COMMENT") ?: "",
                    position = rs.getInt("ORDINAL_POSITION")
                )
            }
            .list()
            
            // Get primary keys
            val primaryKeys = columns.filter { it.isPrimaryKey }.map { it.name }
            
            // Get indexes
            val indexes = jdbcClient.sql(
                """
                SELECT 
                    INDEX_NAME, COLUMN_NAME, NON_UNIQUE, INDEX_TYPE
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = :schema AND TABLE_NAME = :tableName
                ORDER BY INDEX_NAME, SEQ_IN_INDEX
                """
            )
            .param("schema", connection.config.database)
            .param("tableName", tableName)
            .query { rs, _ ->
                Triple(
                    rs.getString("INDEX_NAME"),
                    rs.getString("COLUMN_NAME"),
                    mapOf(
                        "NON_UNIQUE" to rs.getBoolean("NON_UNIQUE"),
                        "INDEX_TYPE" to rs.getString("INDEX_TYPE")
                    )
                )
            }
            .list()
            .groupBy { it.first } // Group by INDEX_NAME
            .map { (indexName, indexEntries) ->
                IndexMetadata(
                    name = indexName,
                    columns = indexEntries.map { it.second }, // Column names in this index
                    isUnique = !(indexEntries.first().third["NON_UNIQUE"] as Boolean),
                    type = indexEntries.first().third["INDEX_TYPE"] as String
                )
            }
            
            val tableMetadata = TableMetadata(
                name = tableName,
                columns = columns.toMutableList(),
                primaryKey = primaryKeys,
                indexes = indexes,
                description = tableComment,
                schema = connection.config.database
            )
            
            tables.add(tableMetadata)
        }
        
        // Get foreign keys to update column metadata
        val relationships = mutableListOf<Relationship>()
        for (table in tables) {
            val foreignKeyRows = jdbcClient.sql(
                """
                SELECT
                    COLUMN_NAME, 
                    REFERENCED_TABLE_NAME, 
                    REFERENCED_COLUMN_NAME
                FROM
                    INFORMATION_SCHEMA.KEY_COLUMN_USAGE
                WHERE
                    TABLE_SCHEMA = :schema AND
                    TABLE_NAME = :tableName AND
                    REFERENCED_TABLE_NAME IS NOT NULL
                """
            )
            .param("schema", connection.config.database)
            .param("tableName", table.name)
            .query { rs, _ ->
                mapOf(
                    "COLUMN_NAME" to rs.getString("COLUMN_NAME"),
                    "REFERENCED_TABLE_NAME" to rs.getString("REFERENCED_TABLE_NAME"),
                    "REFERENCED_COLUMN_NAME" to rs.getString("REFERENCED_COLUMN_NAME")
                )
            }
            .list()
            
            for (fkRow in foreignKeyRows) {
                val columnName = fkRow["COLUMN_NAME"] as String
                val refTableName = fkRow["REFERENCED_TABLE_NAME"] as String
                val refColumnName = fkRow["REFERENCED_COLUMN_NAME"] as String
                
                // Update column metadata to mark as foreign key
                val column = table.columns.find { it.name == columnName }
                column?.let {
                    val columnIndex = table.columns.indexOf(it)
                    if (columnIndex >= 0) {
                        val updatedColumn = it.copy(isForeignKey = true)
                        table.columns[columnIndex] = updatedColumn
                    }
                }
                
                // Add relationship
                val relationship = Relationship(
                    id = UUID.randomUUID().toString(),
                    sourceTable = table.name,
                    sourceColumn = columnName,
                    targetTable = refTableName,
                    targetColumn = refColumnName,
                    relationshipType = RelationshipType.MANY_TO_ONE // Assuming MANY_TO_ONE by default
                )
                relationships.add(relationship)
            }
        }
        
        // Get database version
        val version = jdbcClient.sql("SELECT VERSION()")
                .query(String::class.java)
                .optional()
                .orElse("Unknown")
        
        return@withContext DatabaseMetadata(
            tables = tables,
            version = version,
            databaseName = connection.config.database,
            databaseType = connection.config.type,
            relationships = relationships
        )
    }
    
    /**
     * Execute a query on a MySQL database
     */
    override suspend fun executeQuery(
        connection: Connection, 
        query: String,
        parameters: List<Any>,
        timeout: Long
    ): QueryResult = withContext(Dispatchers.IO) {
        val jdbcClient = JdbcClient.create(connection.dataSource!!)
        
        try {
            val startTime = System.currentTimeMillis()
            
            // Execute query with dynamic parameters
            val sqlOperation = jdbcClient.sql(query)
            
            // Add parameters if provided
            val parameterizedSql = if (parameters.isNotEmpty()) {
                // For positional parameters, add them in order
                parameters.foldIndexed(sqlOperation) { index, acc, param ->
                    acc.param(index + 1, param)
                }
            } else {
                sqlOperation
            }
            
            // Execute query and map results
            val rows = parameterizedSql
                .query { rs, _ ->
                    val rowData = mutableListOf<Any?>()
                    val metaData = rs.metaData
                    val columnCount = metaData.columnCount
                    
                    for (i in 1..columnCount) {
                        rowData.add(rs.getObject(i))
                    }
                    
                    rowData
                }
                .list()
            
            // Get column metadata from the first query execution
            val columns = if (rows.isNotEmpty()) {
                // Execute query again to get metadata only
                sqlOperation
                    .query { rs, _ ->
                        val metaData = rs.metaData
                        val columnCount = metaData.columnCount
                        val columnList = mutableListOf<Column>()
                        
                        for (i in 1..columnCount) {
                            columnList.add(
                                Column(
                                    name = metaData.getColumnName(i),
                                    label = metaData.getColumnLabel(i),
                                    type = metaData.getColumnClassName(i),
                                    typeName = metaData.getColumnTypeName(i)
                                )
                            )
                        }
                        
                        columnList
                    }
                    .single()
            } else {
                emptyList()
            }
            
            val executionTime = System.currentTimeMillis() - startTime
            
            return@withContext QueryResult(
                columns = columns,
                rows = rows,
                rowCount = rows.size,
                executionTimeMs = executionTime
            )
        } catch (e: Exception) {
            throw QueryException("Failed to execute query: ${e.message}", e)
        }
    }
    
    /**
     * Execute an update operation on a MySQL database
     */
    override suspend fun executeUpdate(
        connection: Connection,
        query: String,
        parameters: List<Any>
    ): UpdateResult = withContext(Dispatchers.IO) {
        val jdbcClient = JdbcClient.create(connection.dataSource!!)
        
        try {
            val startTime = System.currentTimeMillis()
            
            // Prepare the query with parameters
            val sqlOperation = jdbcClient.sql(query)
            
            // Add parameters if provided
            val parameterizedSql = if (parameters.isNotEmpty()) {
                // For positional parameters, add them in order
                parameters.foldIndexed(sqlOperation) { index, acc, param ->
                    acc.param(index + 1, param)
                }
            } else {
                sqlOperation
            }
            
            // Execute update
            val updatedRows = parameterizedSql.update()
            
            val executionTime = System.currentTimeMillis() - startTime
            
            return@withContext UpdateResult(
                rowsAffected = updatedRows,
                executionTimeMs = executionTime
            )
        } catch (e: Exception) {
            throw QueryException("Failed to execute update: ${e.message}", e)
        }
    }
    
    /**
     * Begin a transaction
     */
    override suspend fun beginTransaction(connection: Connection): Transaction = withContext(Dispatchers.IO) {
        try {
            val conn = connection.dataSource!!.connection
            conn.autoCommit = false
            
            return@withContext Transaction(
                id = UUID.randomUUID().toString(),
                connection = connection,
                jdbcConnection = conn
            )
        } catch (e: Exception) {
            throw TransactionException("Failed to begin transaction: ${e.message}", e)
        }
    }
    
    /**
     * Commit a transaction
     */
    override suspend fun commitTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        try {
            transaction.jdbcConnection.commit()
            transaction.jdbcConnection.autoCommit = true
            if (!transaction.jdbcConnection.isClosed) {
                transaction.jdbcConnection.close()
            }
        } catch (e: Exception) {
            throw TransactionException("Failed to commit transaction: ${e.message}", e)
        }
    }
    
    /**
     * Rollback a transaction
     */
    override suspend fun rollbackTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        try {
            transaction.jdbcConnection.rollback()
            transaction.jdbcConnection.autoCommit = true
            if (!transaction.jdbcConnection.isClosed) {
                transaction.jdbcConnection.close()
            }
        } catch (e: Exception) {
            throw TransactionException("Failed to rollback transaction: ${e.message}", e)
        }
    }
    
    /**
     * Create a DataSource for a MySQL connection
     */
    private fun createDataSource(config: ConnectionConfig): DataSource {
        val dataSource = SingleConnectionDataSource()
        dataSource.url = config.toJdbcUrl()
        dataSource.username = config.username
        dataSource.password = config.password
        dataSource.setSuppressClose(true)
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver")
        return dataSource
    }
}

/**
 * Exception thrown when a connection operation fails
 */
class ConnectionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Exception thrown when a query operation fails
 */
class QueryException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Exception thrown when a transaction operation fails
 */
class TransactionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) 