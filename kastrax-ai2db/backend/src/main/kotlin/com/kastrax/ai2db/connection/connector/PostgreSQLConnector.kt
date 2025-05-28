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
 * PostgreSQL implementation of the DatabaseConnector interface using JdbcClient
 */
@Component
class PostgreSQLConnector : DatabaseConnector {

    /**
     * Connect to a PostgreSQL database
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
            throw ConnectionException("Failed to connect to PostgreSQL database: ${e.message}", e)
        }
    }

    /**
     * Disconnect from a PostgreSQL database
     */
    override suspend fun disconnect(connection: Connection): Boolean = withContext(Dispatchers.IO) {
        try {
            (connection.dataSource as? SingleConnectionDataSource)?.destroy()
            return@withContext true
        } catch (e: Exception) {
            throw ConnectionException("Failed to disconnect from PostgreSQL database: ${e.message}", e)
        }
    }

    /**
     * Test a PostgreSQL database connection
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
     * Get metadata about a PostgreSQL database
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
            // In PostgreSQL, we need to exclude tables from pg_catalog and information_schema schemas
            val tableRows = jdbcClient.sql(
                """
                SELECT 
                    table_name, 
                    table_type,
                    obj_description(
                        (table_schema || '.' || table_name)::regclass::oid, 'pg_class'
                    ) as table_comment
                FROM 
                    information_schema.tables 
                WHERE 
                    table_schema = current_schema()
                    AND table_schema NOT IN ('pg_catalog', 'information_schema')
                """
            )
            .query { rs, _ ->
                mapOf(
                    "table_name" to rs.getString("table_name"),
                    "table_type" to rs.getString("table_type"),
                    "table_comment" to (rs.getString("table_comment") ?: "")
                )
            }
            .list()

            // Process each table
            for (tableRow in tableRows) {
                val tableName = tableRow["table_name"] as String
                val tableType = tableRow["table_type"] as String
                val tableComment = tableRow["table_comment"] as String

                // Get columns for this table
                val columns = getTableColumns(jdbcClient, connection.config.database, tableName)

                // Get primary keys for this table
                val primaryKeys = getPrimaryKeys(jdbcClient, connection.config.database, tableName)

                // Get foreign keys for this table
                val foreignKeys = getForeignKeys(jdbcClient, connection.config.database, tableName)

                // Get indexes for this table
                val indexes = getIndexes(jdbcClient, connection.config.database, tableName)

                // Create table metadata
                val tableMetadata = TableMetadata(
                    name = tableName,
                    schema = connection.config.database,
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
                schemaName = connection.config.database,
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
        schema: String,
        tableName: String
    ): List<ColumnMetadata> {
        val columns = jdbcClient.sql(
            """
            SELECT 
                column_name, 
                data_type, 
                udt_name,
                character_maximum_length, 
                is_nullable, 
                column_default,
                ordinal_position,
                pg_catalog.col_description(
                    (current_schema() || '.' || :tableName)::regclass::oid, 
                    ordinal_position
                ) as column_comment
            FROM 
                information_schema.columns
            WHERE 
                table_schema = current_schema() AND table_name = :tableName
            ORDER BY 
                ordinal_position
            """
        )
        .param("tableName", tableName)
        .query { rs, _ ->
            val columnName = rs.getString("column_name")

            // Check if column is a primary key
            val isPrimaryKey = jdbcClient.sql(
                """
                SELECT 1 FROM pg_index idx
                JOIN pg_attribute att ON att.attrelid = idx.indrelid AND att.attnum = ANY(idx.indkey)
                WHERE idx.indrelid = :tableName::regclass
                AND idx.indisprimary
                AND att.attname = :columnName
                """
            )
            .param("tableName", tableName)
            .param("columnName", columnName)
            .query(Int::class.java)
            .listOrEmpty()
            .isNotEmpty()

            // Check if column is auto-increment (serial or identity column)
            val isAutoIncrement = if (rs.getString("column_default")?.contains("nextval") == true) {
                true
            } else {
                jdbcClient.sql(
                    """
                    SELECT 1 FROM pg_attribute att
                    WHERE att.attrelid = :tableName::regclass
                    AND att.attname = :columnName
                    AND pg_get_serial_sequence(current_schema() || '.' || :tableName, :columnName) IS NOT NULL
                    """
                )
                .param("tableName", tableName)
                .param("columnName", columnName)
                .query(Int::class.java)
                .listOrEmpty()
                .isNotEmpty()
            }

            ColumnMetadata(
                name = columnName,
                dataType = rs.getString("data_type"),
                typeName = rs.getString("udt_name"),
                size = rs.getObject("character_maximum_length")?.toString()?.toIntOrNull() ?: 0,
                nullable = rs.getString("is_nullable") == "YES",
                primaryKey = isPrimaryKey,
                autoIncrement = isAutoIncrement,
                defaultValue = rs.getString("column_default"),
                comment = rs.getString("column_comment"),
                ordinalPosition = rs.getInt("ordinal_position")
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
        schema: String,
        tableName: String
    ): List<Index> {
        val primaryKeys = jdbcClient.sql(
            """
            SELECT 
                tc.constraint_name,
                kcu.column_name
            FROM 
                information_schema.table_constraints tc
            JOIN 
                information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name
            WHERE 
                tc.constraint_type = 'PRIMARY KEY' 
                AND tc.table_schema = current_schema() 
                AND tc.table_name = :tableName
            ORDER BY 
                kcu.ordinal_position
            """
        )
        .param("tableName", tableName)
        .query { rs, _ ->
            mapOf(
                "constraint_name" to rs.getString("constraint_name"),
                "column_name" to rs.getString("column_name")
            )
        }
        .list()

        // Group by constraint name
        return primaryKeys
            .groupBy { it["constraint_name"] as String }
            .map { (constraintName, columns) ->
                Index(
                    name = constraintName,
                    columnNames = columns.map { it["column_name"] as String },
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
        schema: String,
        tableName: String
    ): List<ForeignKey> {
        val foreignKeys = jdbcClient.sql(
            """
            SELECT
                tc.constraint_name,
                kcu.column_name,
                ccu.table_name AS foreign_table_name,
                ccu.column_name AS foreign_column_name,
                rc.update_rule,
                rc.delete_rule
            FROM 
                information_schema.table_constraints tc
            JOIN 
                information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name
            JOIN 
                information_schema.constraint_column_usage ccu
              ON ccu.constraint_name = tc.constraint_name
            JOIN
                information_schema.referential_constraints rc
              ON rc.constraint_name = tc.constraint_name
            WHERE 
                tc.constraint_type = 'FOREIGN KEY'
                AND tc.table_schema = current_schema()
                AND tc.table_name = :tableName
            ORDER BY 
                kcu.constraint_name, kcu.ordinal_position
            """
        )
        .param("tableName", tableName)
        .query { rs, _ ->
            mapOf(
                "constraint_name" to rs.getString("constraint_name"),
                "column_name" to rs.getString("column_name"),
                "foreign_table_name" to rs.getString("foreign_table_name"),
                "foreign_column_name" to rs.getString("foreign_column_name"),
                "update_rule" to rs.getString("update_rule"),
                "delete_rule" to rs.getString("delete_rule")
            )
        }
        .list()

        // Group by constraint name
        return foreignKeys
            .groupBy { it["constraint_name"] as String }
            .map { (constraintName, columns) ->
                ForeignKey(
                    name = constraintName,
                    columnNames = columns.map { it["column_name"] as String },
                    referencedTableName = columns.first()["foreign_table_name"] as String,
                    referencedColumnNames = columns.map { it["foreign_column_name"] as String },
                    updateRule = columns.first()["update_rule"] as String,
                    deleteRule = columns.first()["delete_rule"] as String
                )
            }
    }

    /**
     * Get indexes for a specific table
     */
    private fun getIndexes(
        jdbcClient: JdbcClient,
        schema: String,
        tableName: String
    ): List<Index> {
        val indexes = jdbcClient.sql(
            """
            SELECT
                i.relname AS index_name,
                a.attname AS column_name,
                ix.indisunique AS is_unique,
                am.amname AS index_type
            FROM
                pg_index ix
            JOIN
                pg_class i ON i.oid = ix.indexrelid
            JOIN
                pg_class t ON t.oid = ix.indrelid
            JOIN
                pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(ix.indkey)
            JOIN
                pg_am am ON am.oid = i.relam
            LEFT JOIN
                pg_namespace n ON n.oid = t.relnamespace
            WHERE
                t.relname = :tableName
                AND n.nspname = current_schema()
                AND NOT ix.indisprimary  -- Exclude primary keys
            ORDER BY
                i.relname, a.attnum
            """
        )
        .param("tableName", tableName)
        .query { rs, _ ->
            mapOf(
                "index_name" to rs.getString("index_name"),
                "column_name" to rs.getString("column_name"),
                "is_unique" to rs.getBoolean("is_unique"),
                "index_type" to rs.getString("index_type")
            )
        }
        .list()

        // Group by index name
        return indexes
            .groupBy { it["index_name"] as String }
            .map { (indexName, columns) ->
                Index(
                    name = indexName,
                    columnNames = columns.map { it["column_name"] as String },
                    unique = columns.first()["is_unique"] as Boolean,
                    type = columns.first()["index_type"] as String
                )
            }
    }

    /**
     * Execute a query on a PostgreSQL database
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
     * Execute an update operation on a PostgreSQL database
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
     * Create a DataSource for a PostgreSQL connection
     */
    private fun createDataSource(config: ConnectionConfig): DataSource {
        val dataSource = SingleConnectionDataSource()
        dataSource.url = config.toJdbcUrl()
        dataSource.username = config.username
        dataSource.password = config.password
        dataSource.setSuppressClose(true)
        dataSource.setDriverClassName("org.postgresql.Driver")
        return dataSource
    }

    /**
     * Analyze relationships to determine their types
     */
    private fun analyzeRelationships(
        relationships: List<Relationship>,
        tables: List<TableMetadata>
    ): List<Relationship> {
        // Similar implementation as in MySQLConnector
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
