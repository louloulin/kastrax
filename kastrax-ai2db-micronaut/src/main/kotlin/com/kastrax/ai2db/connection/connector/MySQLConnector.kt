package com.kastrax.ai2db.connection.connector

import com.kastrax.ai2db.connection.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.sql.Connection as JavaSqlConnection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Types
import java.sql.Timestamp
import java.sql.PreparedStatement
import java.time.Instant
import java.util.*
import javax.sql.DataSource
import com.mysql.cj.jdbc.MysqlDataSource

/**
 * MySQL implementation of the DatabaseConnector interface for Micronaut
 */
@Singleton
@Requires(classes = [com.mysql.cj.jdbc.Driver::class])
class MySQLConnector : DatabaseConnector {
    
    private val logger = LoggerFactory.getLogger(MySQLConnector::class.java)
    
    /**
     * Connect to a MySQL database
     */
    override suspend fun connect(config: ConnectionConfig): Connection = withContext(Dispatchers.IO) {
        try {
            val dataSource = createDataSource(config)
            val jdbcConnection = dataSource.connection
            
            // Test connection
            jdbcConnection.prepareStatement("SELECT 1").use { stmt ->
                stmt.executeQuery().use { rs ->
                    rs.next()
                }
            }
            
            return@withContext Connection(
                id = UUID.randomUUID().toString(),
                config = config,
                status = ConnectionStatus.CONNECTED,
                connectedAt = Instant.now(),
                dataSource = dataSource,
                jdbcConnection = jdbcConnection
            )
        } catch (e: Exception) {
            logger.error("Failed to connect to MySQL database", e)
            throw ConnectionException("Failed to connect to MySQL database: ${e.message}", e)
        }
    }
    
    /**
     * Disconnect from a MySQL database
     */
    override suspend fun disconnect(connection: Connection): Boolean = withContext(Dispatchers.IO) {
        try {
            connection.jdbcConnection?.close()
            return@withContext true
        } catch (e: Exception) {
            logger.error("Failed to disconnect from MySQL database", e)
            throw ConnectionException("Failed to disconnect from MySQL database: ${e.message}", e)
        }
    }
    
    /**
     * Test a MySQL database connection
     */
    override suspend fun testConnection(config: ConnectionConfig): ConnectionStatus = withContext(Dispatchers.IO) {
        try {
            val dataSource = createDataSource(config)
            dataSource.connection.use { conn ->
                conn.prepareStatement("SELECT 1").use { stmt ->
                    stmt.executeQuery().use { rs ->
                        rs.next()
                    }
                }
            }
            return@withContext ConnectionStatus.CONNECTED
        } catch (e: Exception) {
            logger.warn("MySQL connection test failed", e)
            return@withContext ConnectionStatus.FAILED
        }
    }
    
    /**
     * Get metadata about a MySQL database
     */
    override suspend fun getMetadata(connection: Connection): DatabaseMetadata = withContext(Dispatchers.IO) {
        try {
            val jdbcConnection = connection.jdbcConnection!!
            val tables = mutableListOf<TableMetadata>()
            val views = mutableListOf<TableMetadata>()
            val relationships = mutableListOf<Relationship>()

            // Get database metadata
            val metaData = jdbcConnection.metaData
            val databaseProductName = metaData.databaseProductName
            val databaseProductVersion = metaData.databaseProductVersion
            val driverName = metaData.driverName
            val driverVersion = metaData.driverVersion

            // Get list of tables and views
            val tableQuery = """
                SELECT TABLE_NAME, TABLE_TYPE, TABLE_COMMENT 
                FROM INFORMATION_SCHEMA.TABLES 
                WHERE TABLE_SCHEMA = ?
            """
            
            jdbcConnection.prepareStatement(tableQuery).use { stmt ->
                stmt.setString(1, connection.config.database)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val tableName = rs.getString("TABLE_NAME")
                        val tableType = rs.getString("TABLE_TYPE")
                        val tableComment = rs.getString("TABLE_COMMENT") ?: ""

                        // Get columns for this table
                        val columns = getTableColumns(jdbcConnection, connection.config.database, tableName)

                        // Get primary keys for this table
                        val primaryKeys = getPrimaryKeys(jdbcConnection, connection.config.database, tableName)

                        // Get foreign keys for this table
                        val foreignKeys = getForeignKeys(jdbcConnection, connection.config.database, tableName)

                        // Get indexes for this table
                        val indexes = getIndexes(jdbcConnection, connection.config.database, tableName)

                        // Create table metadata
                        val tableMetadata = TableMetadata(
                            name = tableName,
                            schema = connection.config.database,
                            type = tableType,
                            columns = columns,
                            primaryKeys = primaryKeys,
                            foreignKeys = foreignKeys,
                            indexes = indexes,
                            comment = tableComment.ifEmpty { null }
                        )

                        // Add table or view
                        if (tableType.contains("VIEW", ignoreCase = true)) {
                            views.add(tableMetadata)
                        } else {
                            tables.add(tableMetadata)
                        }

                        // Identify relationships from foreign keys
                        foreignKeys.forEach { foreignKey ->
                            val relationship = Relationship(
                                sourceTable = tableName,
                                sourceColumns = foreignKey.columnNames,
                                targetTable = foreignKey.referencedTableName,
                                targetColumns = foreignKey.referencedColumnNames,
                                type = RelationshipType.MANY_TO_ONE,
                                name = foreignKey.name,
                                foreignKeyName = foreignKey.name
                            )
                            relationships.add(relationship)
                        }
                    }
                }
            }

            return@withContext DatabaseMetadata(
                databaseName = connection.config.database,
                schemaName = connection.config.database,
                tables = tables,
                views = views,
                databaseProductName = databaseProductName,
                databaseProductVersion = databaseProductVersion,
                driverName = driverName,
                driverVersion = driverVersion,
                relationships = relationships,
                fetchedAt = Instant.now()
            )
        } catch (e: Exception) {
            logger.error("Failed to get database metadata", e)
            throw QueryException("Failed to get database metadata: ${e.message}", e)
        }
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
        try {
            val startTime = System.currentTimeMillis()
            val jdbcConnection = connection.jdbcConnection!!
            
            jdbcConnection.prepareStatement(query).use { stmt ->
                stmt.queryTimeout = (timeout / 1000).toInt()
                
                // Set parameters
                parameters.forEachIndexed { index, param ->
                    stmt.setObject(index + 1, param)
                }
                
                stmt.executeQuery().use { rs ->
                    val metaData = rs.metaData
                    val columnCount = metaData.columnCount
                    
                    // Build column information
                    val columns = (1..columnCount).map { i ->
                        ColumnData(
                            name = metaData.getColumnName(i),
                            label = metaData.getColumnLabel(i),
                            type = metaData.getColumnType(i).toString(),
                            typeName = metaData.getColumnTypeName(i)
                        )
                    }
                    
                    // Build rows
                    val rows = mutableListOf<Map<String, Any?>>()
                    while (rs.next()) {
                        val row = mutableMapOf<String, Any?>()
                        columns.forEach { column ->
                            row[column.name] = rs.getObject(column.name)
                        }
                        rows.add(row)
                    }
                    
                    val executionTime = System.currentTimeMillis() - startTime
                    
                    return@withContext QueryResult(
                        columns = columns,
                        rows = rows,
                        rowCount = rows.size,
                        executionTime = executionTime,
                        success = true
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to execute query", e)
            val executionTime = System.currentTimeMillis() - System.currentTimeMillis()
            return@withContext QueryResult(
                error = e.message,
                success = false,
                executionTime = executionTime
            )
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
        try {
            val startTime = System.currentTimeMillis()
            val jdbcConnection = connection.jdbcConnection!!
            
            jdbcConnection.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS).use { stmt ->
                // Set parameters
                parameters.forEachIndexed { index, param ->
                    stmt.setObject(index + 1, param)
                }
                
                val affectedRows = stmt.executeUpdate()
                
                // Get generated keys
                val generatedKeys = mutableListOf<Any>()
                stmt.generatedKeys.use { rs ->
                    while (rs.next()) {
                        generatedKeys.add(rs.getObject(1))
                    }
                }
                
                val executionTime = System.currentTimeMillis() - startTime
                
                return@withContext UpdateResult(
                    affectedRows = affectedRows,
                    generatedKeys = generatedKeys,
                    executionTime = executionTime,
                    success = true
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to execute update", e)
            val executionTime = System.currentTimeMillis() - System.currentTimeMillis()
            return@withContext UpdateResult(
                error = e.message,
                success = false,
                executionTime = executionTime
            )
        }
    }
    
    /**
     * Begin a transaction
     */
    override suspend fun beginTransaction(connection: Connection): Transaction = withContext(Dispatchers.IO) {
        try {
            val jdbcConnection = connection.jdbcConnection!!
            jdbcConnection.autoCommit = false
            
            return@withContext Transaction(
                connection = connection,
                createdAt = Instant.now(),
                isActive = true
            )
        } catch (e: Exception) {
            logger.error("Failed to begin transaction", e)
            throw TransactionException("Failed to begin transaction: ${e.message}", e)
        }
    }
    
    /**
     * Commit a transaction
     */
    override suspend fun commitTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        try {
            val jdbcConnection = transaction.connection.jdbcConnection!!
            jdbcConnection.commit()
            jdbcConnection.autoCommit = true
        } catch (e: Exception) {
            logger.error("Failed to commit transaction", e)
            throw TransactionException("Failed to commit transaction: ${e.message}", e)
        }
    }
    
    /**
     * Rollback a transaction
     */
    override suspend fun rollbackTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        try {
            val jdbcConnection = transaction.connection.jdbcConnection!!
            jdbcConnection.rollback()
            jdbcConnection.autoCommit = true
        } catch (e: Exception) {
            logger.error("Failed to rollback transaction", e)
            throw TransactionException("Failed to rollback transaction: ${e.message}", e)
        }
    }
    
    /**
     * Create a MySQL DataSource
     */
    private fun createDataSource(config: ConnectionConfig): DataSource {
        val dataSource = MysqlDataSource()
        dataSource.setURL(config.toJdbcUrl())
        dataSource.user = config.username
        dataSource.setPassword(config.password)
        
        // Set additional properties
        dataSource.connectTimeout = 30000
        dataSource.socketTimeout = 60000
        
        return dataSource
    }
    
    // Helper methods for metadata extraction
    private fun getTableColumns(jdbcConnection: JavaSqlConnection, schema: String, tableName: String): List<ColumnMetadata> {
        val columns = mutableListOf<ColumnMetadata>()
        val query = """
            SELECT 
                COLUMN_NAME, 
                DATA_TYPE, 
                COLUMN_TYPE,
                CHARACTER_MAXIMUM_LENGTH,
                IS_NULLABLE,
                COLUMN_KEY,
                EXTRA,
                COLUMN_DEFAULT,
                COLUMN_COMMENT,
                ORDINAL_POSITION
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
            ORDER BY ORDINAL_POSITION
        """
        
        jdbcConnection.prepareStatement(query).use { stmt ->
            stmt.setString(1, schema)
            stmt.setString(2, tableName)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    columns.add(ColumnMetadata(
                        name = rs.getString("COLUMN_NAME"),
                        dataType = rs.getString("DATA_TYPE"),
                        typeName = rs.getString("COLUMN_TYPE"),
                        size = rs.getInt("CHARACTER_MAXIMUM_LENGTH"),
                        nullable = rs.getString("IS_NULLABLE") == "YES",
                        primaryKey = rs.getString("COLUMN_KEY") == "PRI",
                        autoIncrement = rs.getString("EXTRA").contains("auto_increment"),
                        defaultValue = rs.getString("COLUMN_DEFAULT"),
                        comment = rs.getString("COLUMN_COMMENT"),
                        ordinalPosition = rs.getInt("ORDINAL_POSITION")
                    ))
                }
            }
        }
        return columns
    }
    
    private fun getPrimaryKeys(jdbcConnection: JavaSqlConnection, schema: String, tableName: String): List<String> {
        val primaryKeys = mutableListOf<String>()
        val query = """
            SELECT COLUMN_NAME 
            FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
            WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND CONSTRAINT_NAME = 'PRIMARY'
            ORDER BY ORDINAL_POSITION
        """
        
        jdbcConnection.prepareStatement(query).use { stmt ->
            stmt.setString(1, schema)
            stmt.setString(2, tableName)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    primaryKeys.add(rs.getString("COLUMN_NAME"))
                }
            }
        }
        return primaryKeys
    }
    
    private fun getForeignKeys(jdbcConnection: JavaSqlConnection, schema: String, tableName: String): List<ForeignKey> {
        val foreignKeys = mutableListOf<ForeignKey>()
        val query = """
            SELECT 
                CONSTRAINT_NAME,
                COLUMN_NAME,
                REFERENCED_TABLE_NAME,
                REFERENCED_COLUMN_NAME,
                UPDATE_RULE,
                DELETE_RULE
            FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
            JOIN INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS rc 
                ON kcu.CONSTRAINT_NAME = rc.CONSTRAINT_NAME
            WHERE kcu.TABLE_SCHEMA = ? AND kcu.TABLE_NAME = ?
                AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
        """
        
        jdbcConnection.prepareStatement(query).use { stmt ->
            stmt.setString(1, schema)
            stmt.setString(2, tableName)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    foreignKeys.add(ForeignKey(
                        name = rs.getString("CONSTRAINT_NAME"),
                        columnNames = listOf(rs.getString("COLUMN_NAME")),
                        referencedTableName = rs.getString("REFERENCED_TABLE_NAME"),
                        referencedColumnNames = listOf(rs.getString("REFERENCED_COLUMN_NAME")),
                        updateRule = rs.getString("UPDATE_RULE"),
                        deleteRule = rs.getString("DELETE_RULE")
                    ))
                }
            }
        }
        return foreignKeys
    }
    
    private fun getIndexes(jdbcConnection: JavaSqlConnection, schema: String, tableName: String): List<Index> {
        val indexes = mutableListOf<Index>()
        val query = """
            SELECT 
                INDEX_NAME,
                COLUMN_NAME,
                NON_UNIQUE,
                INDEX_TYPE
            FROM INFORMATION_SCHEMA.STATISTICS 
            WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
            ORDER BY INDEX_NAME, SEQ_IN_INDEX
        """
        
        val indexMap = mutableMapOf<String, MutableList<String>>()
        val indexInfo = mutableMapOf<String, Pair<Boolean, String?>>()
        
        jdbcConnection.prepareStatement(query).use { stmt ->
            stmt.setString(1, schema)
            stmt.setString(2, tableName)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val indexName = rs.getString("INDEX_NAME")
                    val columnName = rs.getString("COLUMN_NAME")
                    val nonUnique = rs.getBoolean("NON_UNIQUE")
                    val indexType = rs.getString("INDEX_TYPE")
                    
                    indexMap.computeIfAbsent(indexName) { mutableListOf() }.add(columnName)
                    indexInfo[indexName] = Pair(!nonUnique, indexType)
                }
            }
        }
        
        indexMap.forEach { (indexName, columns) ->
            val (unique, type) = indexInfo[indexName]!!
            indexes.add(Index(
                name = indexName,
                columnNames = columns,
                unique = unique,
                type = type
            ))
        }
        
        return indexes
    }
}