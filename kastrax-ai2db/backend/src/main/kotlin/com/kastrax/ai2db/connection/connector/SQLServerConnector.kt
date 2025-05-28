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
import java.sql.PreparedStatement
import java.time.Instant
import java.util.*
import javax.sql.DataSource

/**
 * SQL Server implementation of the DatabaseConnector interface using JdbcClient
 */
@Component
class SQLServerConnector : DatabaseConnector {

    /**
     * Connect to a SQL Server database
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
                status = ConnectionStatus.CONNECTED,
                connectedAt = Instant.now(),
                dataSource = dataSource
            )
        } catch (e: Exception) {
            throw ConnectionException("Failed to connect to SQL Server database: ${e.message}", e)
        }
    }

    /**
     * Disconnect from a SQL Server database
     */
    override suspend fun disconnect(connection: Connection): Boolean = withContext(Dispatchers.IO) {
        try {
            (connection.dataSource as? SingleConnectionDataSource)?.destroy()
            return@withContext true
        } catch (e: Exception) {
            throw ConnectionException("Failed to disconnect from SQL Server database: ${e.message}", e)
        }
    }

    /**
     * Test a SQL Server database connection
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
     * Get metadata about a SQL Server database
     */
    override suspend fun getMetadata(connection: Connection): DatabaseMetadata = withContext(Dispatchers.IO) {
        try {
            val jdbcClient = JdbcClient.create(connection.dataSource!!)
            val tables = mutableListOf<TableMetadata>()
            val views = mutableListOf<TableMetadata>()
            val relationships = mutableListOf<Relationship>()

            // Get database metadata
            val metaData = connection.dataSource!!.connection.metaData
            val databaseProductName = metaData.databaseProductName
            val databaseProductVersion = metaData.databaseProductVersion
            val driverName = metaData.driverName
            val driverVersion = metaData.driverVersion

            // Get list of tables and views
            val tableRows = jdbcClient.sql(
                """
                SELECT 
                    t.TABLE_NAME, 
                    t.TABLE_TYPE,
                    p.value AS TABLE_COMMENT
                FROM 
                    INFORMATION_SCHEMA.TABLES t
                LEFT JOIN 
                    sys.tables st ON t.TABLE_NAME = st.name
                LEFT JOIN 
                    sys.extended_properties p ON p.major_id = st.object_id AND p.minor_id = 0 AND p.name = 'MS_Description'
                WHERE 
                    t.TABLE_SCHEMA = 'dbo'  -- Default schema, change if needed
                """
            )
            .query { rs, _ ->
                mapOf(
                    "TABLE_NAME" to rs.getString("TABLE_NAME"),
                    "TABLE_TYPE" to rs.getString("TABLE_TYPE"),
                    "TABLE_COMMENT" to (rs.getString("TABLE_COMMENT") ?: "")
                )
            }
            .list()

            // Process each table
            for (tableRow in tableRows) {
                val tableName = tableRow["TABLE_NAME"] as String
                val tableType = tableRow["TABLE_TYPE"] as String
                val tableComment = tableRow["TABLE_COMMENT"] as String

                // Get columns for this table
                val columns = getTableColumns(jdbcClient, tableName)

                // Get primary keys for this table
                val primaryKeys = getPrimaryKeys(jdbcClient, tableName)

                // Get foreign keys for this table
                val foreignKeys = getForeignKeys(jdbcClient, tableName)

                // Get indexes for this table
                val indexes = getIndexes(jdbcClient, tableName)

                // Create table metadata
                val tableMetadata = TableMetadata(
                    name = tableName,
                    schema = "dbo", // Default schema for SQL Server
                    type = tableType,
                    columns = columns,
                    primaryKeys = primaryKeys.map { it.columnNames }.flatten(),
                    foreignKeys = foreignKeys,
                    indexes = indexes,
                    comment = tableComment.ifEmpty { null }
                )

                // Add table or view
                if (tableType == "VIEW") {
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
                        type = RelationshipType.MANY_TO_ONE, // Default to MANY_TO_ONE
                        name = foreignKey.name,
                        foreignKeyName = foreignKey.name
                    )
                    relationships.add(relationship)
                }
            }

            // Analyze relationships to determine their types
            val analyzedRelationships = analyzeRelationships(relationships, tables)

            return@withContext DatabaseMetadata(
                databaseName = connection.config.database,
                schemaName = "dbo", // Default schema for SQL Server
                tables = tables,
                views = views,
                databaseProductName = databaseProductName,
                databaseProductVersion = databaseProductVersion,
                driverName = driverName,
                driverVersion = driverVersion,
                relationships = analyzedRelationships,
                fetchedAt = Instant.now()
            )
        } catch (e: Exception) {
            throw QueryException("Failed to get database metadata: ${e.message}", e)
        }
    }

    /**
     * Get columns for a specific table
     */
    private fun getTableColumns(
        jdbcClient: JdbcClient,
        tableName: String
    ): List<ColumnMetadata> {
        val columns = jdbcClient.sql(
            """
            SELECT 
                c.COLUMN_NAME,
                c.DATA_TYPE,
                c.CHARACTER_MAXIMUM_LENGTH,
                c.IS_NULLABLE,
                COLUMNPROPERTY(OBJECT_ID(c.TABLE_SCHEMA + '.' + c.TABLE_NAME), c.COLUMN_NAME, 'IsIdentity') AS IS_IDENTITY,
                c.COLUMN_DEFAULT,
                c.ORDINAL_POSITION,
                ep.value AS COLUMN_COMMENT
            FROM 
                INFORMATION_SCHEMA.COLUMNS c
            LEFT JOIN 
                sys.columns sc ON sc.name = c.COLUMN_NAME AND sc.object_id = OBJECT_ID(c.TABLE_SCHEMA + '.' + c.TABLE_NAME)
            LEFT JOIN 
                sys.extended_properties ep ON ep.major_id = sc.object_id AND ep.minor_id = sc.column_id AND ep.name = 'MS_Description'
            WHERE 
                c.TABLE_NAME = :tableName
                AND c.TABLE_SCHEMA = 'dbo' -- Default schema
            ORDER BY 
                c.ORDINAL_POSITION
            """
        )
        .param("tableName", tableName)
        .query { rs, _ ->
            val columnName = rs.getString("COLUMN_NAME")

            // Check if column is a primary key
            val isPrimaryKey = jdbcClient.sql(
                """
                SELECT 1 
                FROM 
                    INFORMATION_SCHEMA.KEY_COLUMN_USAGE k
                JOIN 
                    INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc 
                    ON k.TABLE_SCHEMA = tc.TABLE_SCHEMA 
                    AND k.TABLE_NAME = tc.TABLE_NAME 
                    AND k.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
                WHERE 
                    k.TABLE_NAME = :tableName
                    AND k.COLUMN_NAME = :columnName
                    AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY'
                """
            )
            .param("tableName", tableName)
            .param("columnName", columnName)
            .query(Int::class.java)
            .listOrEmpty()
            .isNotEmpty()

            ColumnMetadata(
                name = columnName,
                dataType = rs.getString("DATA_TYPE"),
                typeName = rs.getString("DATA_TYPE"),
                size = rs.getObject("CHARACTER_MAXIMUM_LENGTH")?.toString()?.toIntOrNull() ?: 0,
                nullable = rs.getString("IS_NULLABLE") == "YES",
                primaryKey = isPrimaryKey,
                autoIncrement = rs.getInt("IS_IDENTITY") == 1,
                defaultValue = rs.getString("COLUMN_DEFAULT"),
                comment = rs.getString("COLUMN_COMMENT"),
                ordinalPosition = rs.getInt("ORDINAL_POSITION")
            )
        }
        .list()

        return columns
    }

    /**
     * Get primary keys for a specific table
     */
    private fun getPrimaryKeys(
        jdbcClient: JdbcClient,
        tableName: String
    ): List<Index> {
        val primaryKeys = jdbcClient.sql(
            """
            SELECT 
                k.CONSTRAINT_NAME,
                k.COLUMN_NAME
            FROM 
                INFORMATION_SCHEMA.KEY_COLUMN_USAGE k
            JOIN 
                INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc 
                ON k.TABLE_SCHEMA = tc.TABLE_SCHEMA 
                AND k.TABLE_NAME = tc.TABLE_NAME 
                AND k.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
            WHERE 
                k.TABLE_NAME = :tableName
                AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY'
            ORDER BY 
                k.ORDINAL_POSITION
            """
        )
        .param("tableName", tableName)
        .query { rs, _ ->
            mapOf(
                "CONSTRAINT_NAME" to rs.getString("CONSTRAINT_NAME"),
                "COLUMN_NAME" to rs.getString("COLUMN_NAME")
            )
        }
        .list()

        // Group by constraint name
        return primaryKeys
            .groupBy { it["CONSTRAINT_NAME"] as String }
            .map { (constraintName, columns) ->
                Index(
                    name = constraintName,
                    columnNames = columns.map { it["COLUMN_NAME"] as String },
                    unique = true,
                    type = "PRIMARY"
                )
            }
    }

    /**
     * Get foreign keys for a specific table
     */
    private fun getForeignKeys(
        jdbcClient: JdbcClient,
        tableName: String
    ): List<ForeignKey> {
        val foreignKeys = jdbcClient.sql(
            """
            SELECT 
                fk.name AS CONSTRAINT_NAME,
                COL_NAME(fkc.parent_object_id, fkc.parent_column_id) AS COLUMN_NAME,
                OBJECT_NAME(fkc.referenced_object_id) AS REFERENCED_TABLE_NAME,
                COL_NAME(fkc.referenced_object_id, fkc.referenced_column_id) AS REFERENCED_COLUMN_NAME,
                CASE fk.update_referential_action
                    WHEN 0 THEN 'NO ACTION'
                    WHEN 1 THEN 'CASCADE'
                    WHEN 2 THEN 'SET NULL'
                    WHEN 3 THEN 'SET DEFAULT'
                END AS UPDATE_RULE,
                CASE fk.delete_referential_action
                    WHEN 0 THEN 'NO ACTION'
                    WHEN 1 THEN 'CASCADE'
                    WHEN 2 THEN 'SET NULL'
                    WHEN 3 THEN 'SET DEFAULT'
                END AS DELETE_RULE
            FROM 
                sys.foreign_keys fk
            JOIN 
                sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
            WHERE 
                OBJECT_NAME(fkc.parent_object_id) = :tableName
            ORDER BY 
                fk.name, fkc.constraint_column_id
            """
        )
        .param("tableName", tableName)
        .query { rs, _ ->
            mapOf(
                "CONSTRAINT_NAME" to rs.getString("CONSTRAINT_NAME"),
                "COLUMN_NAME" to rs.getString("COLUMN_NAME"),
                "REFERENCED_TABLE_NAME" to rs.getString("REFERENCED_TABLE_NAME"),
                "REFERENCED_COLUMN_NAME" to rs.getString("REFERENCED_COLUMN_NAME"),
                "UPDATE_RULE" to rs.getString("UPDATE_RULE"),
                "DELETE_RULE" to rs.getString("DELETE_RULE")
            )
        }
        .list()

        // Group by constraint name
        return foreignKeys
            .groupBy { it["CONSTRAINT_NAME"] as String }
            .map { (constraintName, columns) ->
                ForeignKey(
                    name = constraintName,
                    columnNames = columns.map { it["COLUMN_NAME"] as String },
                    referencedTableName = columns.first()["REFERENCED_TABLE_NAME"] as String,
                    referencedColumnNames = columns.map { it["REFERENCED_COLUMN_NAME"] as String },
                    updateRule = columns.first()["UPDATE_RULE"] as String,
                    deleteRule = columns.first()["DELETE_RULE"] as String
                )
            }
    }

    /**
     * Get indexes for a specific table
     */
    private fun getIndexes(
        jdbcClient: JdbcClient,
        tableName: String
    ): List<Index> {
        val indexes = jdbcClient.sql(
            """
            SELECT 
                i.name AS INDEX_NAME,
                COL_NAME(ic.object_id, ic.column_id) AS COLUMN_NAME,
                i.is_unique AS IS_UNIQUE,
                i.type_desc AS INDEX_TYPE
            FROM 
                sys.indexes i
            JOIN 
                sys.index_columns ic ON i.object_id = ic.object_id AND i.index_id = ic.index_id
            WHERE 
                OBJECT_NAME(i.object_id) = :tableName
                AND i.is_primary_key = 0  -- Exclude primary keys
            ORDER BY 
                i.name, ic.key_ordinal
            """
        )
        .param("tableName", tableName)
        .query { rs, _ ->
            mapOf(
                "INDEX_NAME" to rs.getString("INDEX_NAME"),
                "COLUMN_NAME" to rs.getString("COLUMN_NAME"),
                "IS_UNIQUE" to rs.getBoolean("IS_UNIQUE"),
                "INDEX_TYPE" to rs.getString("INDEX_TYPE")
            )
        }
        .list()

        // Group by index name
        return indexes
            .groupBy { it["INDEX_NAME"] as String }
            .map { (indexName, columns) ->
                Index(
                    name = indexName,
                    columnNames = columns.map { it["COLUMN_NAME"] as String },
                    unique = columns.first()["IS_UNIQUE"] as Boolean,
                    type = columns.first()["INDEX_TYPE"] as String
                )
            }
    }

    /**
     * Execute a query on a SQL Server database
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

            // Get column metadata
            val columns = mutableListOf<ColumnData>()
            // Store rows as Maps for flexibility
            val rows = mutableListOf<Map<String, Any?>>()

            // Execute query with automatic mapping to Map<String, Any?>
            parameterizedSql
                .query { rs, _ ->
                    // Extract column metadata if we haven't yet
                    if (columns.isEmpty()) {
                        val metaData = rs.metaData
                        val columnCount = metaData.columnCount

                        for (i in 1..columnCount) {
                            columns.add(
                                ColumnData(
                                    name = metaData.getColumnName(i),
                                    label = metaData.getColumnLabel(i),
                                    type = metaData.getColumnClassName(i),
                                    typeName = metaData.getColumnTypeName(i)
                                )
                            )
                        }
                    }

                    // Extract row data as Map
                    val row = mutableMapOf<String, Any?>()
                    for (column in columns) {
                        row[column.name] = rs.getObject(column.name)
                    }
                    rows.add(row)
                }
                .list()

            val executionTime = System.currentTimeMillis() - startTime

            return@withContext QueryResult(
                columns = columns,
                rows = rows,
                rowCount = rows.size,
                executionTime = executionTime,
                success = true
            )
        } catch (e: Exception) {
            // Return error result
            return@withContext QueryResult(
                error = e.message,
                success = false
            )
        }
    }

    /**
     * Execute an update operation on a SQL Server database
     */
    override suspend fun executeUpdate(
        connection: Connection,
        query: String,
        parameters: List<Any>
    ): UpdateResult = withContext(Dispatchers.IO) {
        val jdbcClient = JdbcClient.create(connection.dataSource!!)

        try {
            val startTime = System.currentTimeMillis()
            val keyHolder = GeneratedKeyHolder()

            // Execute update and capture generated keys
            val updatedRows = connection.dataSource!!.connection.use { conn ->
                conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS).use { ps ->
                    // Set parameters
                    parameters.forEachIndexed { index, param ->
                        ps.setObject(index + 1, param)
                    }

                    // Execute update
                    ps.executeUpdate()

                    // Collect generated keys
                    val rs = ps.generatedKeys
                    val keys = mutableListOf<Any>()
                    while (rs.next()) {
                        keys.add(rs.getObject(1))
                    }

                    // Store keys in keyHolder
                    keys.forEach { keyHolder.keyList.add(mapOf("GENERATED_KEY" to it)) }

                    ps.updateCount
                }
            }

            val executionTime = System.currentTimeMillis() - startTime

            return@withContext UpdateResult(
                affectedRows = updatedRows,
                generatedKeys = keyHolder.keyList.map { it["GENERATED_KEY"] ?: it.values.first() },
                executionTime = executionTime,
                success = true
            )
        } catch (e: Exception) {
            return@withContext UpdateResult(
                error = e.message,
                success = false
            )
        }
    }

    /**
     * Begin a transaction
     */
    override suspend fun beginTransaction(connection: Connection): Transaction = withContext(Dispatchers.IO) {
        try {
            val jdbcConn = connection.dataSource!!.connection
            jdbcConn.autoCommit = false

            return@withContext Transaction(
                id = UUID.randomUUID().toString(),
                connection = connection,
                createdAt = Instant.now(),
                isActive = true
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
            val jdbcConn = transaction.connection.dataSource!!.connection
            jdbcConn.commit()
            jdbcConn.autoCommit = true
        } catch (e: Exception) {
            throw TransactionException("Failed to commit transaction: ${e.message}", e)
        }
    }

    /**
     * Rollback a transaction
     */
    override suspend fun rollbackTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        try {
            val jdbcConn = transaction.connection.dataSource!!.connection
            jdbcConn.rollback()
            jdbcConn.autoCommit = true
        } catch (e: Exception) {
            throw TransactionException("Failed to rollback transaction: ${e.message}", e)
        }
    }

    /**
     * Create a DataSource for a SQL Server connection
     */
    private fun createDataSource(config: ConnectionConfig): DataSource {
        val dataSource = SingleConnectionDataSource()
        dataSource.url = config.toJdbcUrl()
        dataSource.username = config.username
        dataSource.password = config.password
        dataSource.setSuppressClose(true)
        dataSource.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver")
        return dataSource
    }

    /**
     * Analyze relationships to determine their types
     */
    private fun analyzeRelationships(
        relationships: List<Relationship>,
        tables: List<TableMetadata>
    ): List<Relationship> {
        // Similar implementation as in other connectors
        // Create a map of tables by name for quick lookup
        val tableMap = tables.associateBy { it.name }

        // Analyze each relationship
        return relationships.map { relationship ->
            // Default type is MANY_TO_ONE
            var type = RelationshipType.MANY_TO_ONE

            // Get source and target tables
            val sourceTable = tableMap[relationship.sourceTable]
            val targetTable = tableMap[relationship.targetTable]

            if (sourceTable != null && targetTable != null) {
                // Check if source column is unique (primary key or unique index)
                val sourceColumnsAreUnique = sourceTable.columns
                    .filter { relationship.sourceColumns.contains(it.name) }
                    .all { it.primaryKey } ||
                    sourceTable.indexes.any { index ->
                        index.unique &&
                        relationship.sourceColumns.containsAll(index.columnNames) &&
                        index.columnNames.containsAll(relationship.sourceColumns)
                    }

                // Check if target column is unique
                val targetColumnsAreUnique = targetTable.columns
                    .filter { relationship.targetColumns.contains(it.name) }
                    .all { it.primaryKey } ||
                    targetTable.indexes.any { index ->
                        index.unique &&
                        relationship.targetColumns.containsAll(index.columnNames) &&
                        index.columnNames.containsAll(relationship.targetColumns)
                    }

                // Determine relationship type
                type = when {
                    sourceColumnsAreUnique && targetColumnsAreUnique -> RelationshipType.ONE_TO_ONE
                    sourceColumnsAreUnique && !targetColumnsAreUnique -> RelationshipType.ONE_TO_MANY
                    !sourceColumnsAreUnique && targetColumnsAreUnique -> RelationshipType.MANY_TO_ONE
                    else -> RelationshipType.MANY_TO_MANY
                }
            }

            // Return updated relationship
            relationship.copy(type = type)
        }
    }
}
