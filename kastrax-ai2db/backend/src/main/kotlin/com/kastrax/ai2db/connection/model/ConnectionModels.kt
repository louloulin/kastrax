package com.kastrax.ai2db.connection.model

import java.sql.Connection as JavaSqlConnection
import java.time.Instant
import javax.sql.DataSource

/**
 * Types of databases supported by the application
 */
enum class DatabaseType(val displayName: String, val driverClassName: String) {
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver"),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver"),
    SQLSERVER("SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver"),
    ORACLE("Oracle", "oracle.jdbc.OracleDriver"),
    MARIADB("MariaDB", "org.mariadb.jdbc.Driver"),
    MONGODB("MongoDB", "mongodb.jdbc.MongoDriver"),
    SQLITE("SQLite", "org.sqlite.JDBC"),
    H2("H2", "org.h2.Driver"),
    REDIS("Redis", ""),  // No JDBC driver for Redis
    ELASTICSEARCH("Elasticsearch", ""); // No JDBC driver for Elasticsearch
    
    companion object {
        /**
         * Convert from string to DatabaseType, ignoring case
         */
        fun fromString(type: String): DatabaseType {
            return values().find { it.name.equals(type, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown database type: $type")
        }
        
        /**
         * Check if a database type supports JDBC
         */
        fun supportsJdbc(type: DatabaseType): Boolean {
            return type.driverClassName.isNotEmpty()
        }
        
        /**
         * Get the default port for a database type
         */
        fun getDefaultPort(type: DatabaseType): Int {
            return when (type) {
                MYSQL -> 3306
                POSTGRESQL -> 5432
                SQLSERVER -> 1433
                ORACLE -> 1521
                MARIADB -> 3306
                MONGODB -> 27017
                SQLITE -> -1 // File-based, no port
                H2 -> 9092
                REDIS -> 6379
                ELASTICSEARCH -> 9200
            }
        }
    }
}

/**
 * Status of a database connection
 */
enum class ConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    FAILED,
    PENDING
}

/**
 * Types of relationships between database tables
 */
enum class RelationshipType {
    ONE_TO_ONE,
    ONE_TO_MANY,
    MANY_TO_ONE,
    MANY_TO_MANY
}

/**
 * Configuration for a database connection
 */
data class ConnectionConfig(
    val name: String,
    val type: DatabaseType,
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val password: String,
    val properties: Map<String, String> = emptyMap(),
    val sslEnabled: Boolean = false,
    val timezone: String = "UTC"
) {
    /**
     * Convert this configuration to a JDBC URL
     */
    fun toJdbcUrl(): String {
        return when (type) {
            DatabaseType.MYSQL -> {
                val props = buildJdbcParams()
                "jdbc:mysql://$host:$port/$database$props"
            }
            DatabaseType.POSTGRESQL -> {
                val props = buildJdbcParams()
                "jdbc:postgresql://$host:$port/$database$props"
            }
            DatabaseType.SQLSERVER -> {
                val props = buildJdbcParams(";")
                "jdbc:sqlserver://$host:$port;databaseName=$database$props"
            }
            DatabaseType.ORACLE -> {
                "jdbc:oracle:thin:@$host:$port/$database"
            }
            DatabaseType.MONGODB -> {
                val authParams = if (username.isNotEmpty() && password.isNotEmpty()) {
                    "$username:$password@"
                } else {
                    ""
                }
                "mongodb://$authParams$host:$port/$database"
            }
            DatabaseType.MARIADB -> {
                val props = buildJdbcParams()
                "jdbc:mariadb://$host:$port/$database$props"
            }
            // Add more database types as needed
            else -> throw IllegalArgumentException("Unsupported database type: $type")
        }
    }
    
    /**
     * Build JDBC parameters string
     */
    private fun buildJdbcParams(separator: String = "&", prefix: String = "?"): String {
        val params = mutableListOf<String>()
        
        // Add common parameters
        if (type == DatabaseType.MYSQL || type == DatabaseType.MARIADB) {
            params.add("useSSL=$sslEnabled")
            params.add("serverTimezone=$timezone")
        }
        
        // Add custom properties
        properties.forEach { (key, value) ->
            params.add("$key=$value")
        }
        
        return if (params.isEmpty()) {
            ""
        } else {
            prefix + params.joinToString(separator)
        }
    }
}

/**
 * Represents an active database connection
 */
data class Connection(
    val id: String,
    val config: ConnectionConfig,
    val connectedAt: Instant = Instant.now(),
    val dataSource: DataSource? = null, // DataSource for JDBC connections
    val rawConnection: Any? = null // The actual native connection (MongoDB Client, Redis Client, etc.)
)

/**
 * Represents a database transaction
 */
data class Transaction(
    val id: String,
    val connection: Connection,
    val jdbcConnection: JavaSqlConnection,
    val startTime: Long = System.currentTimeMillis()
)

/**
 * Information about a column in a query result
 */
data class Column(
    val name: String,
    val label: String,
    val type: String,
    val typeName: String
)

/**
 * Result of a database query
 */
data class QueryResult(
    val columns: List<Column>,
    val rows: List<List<Any?>>,
    val rowCount: Int,
    val executionTimeMs: Long,
    val warnings: List<String> = emptyList(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Result of a database update operation
 */
data class UpdateResult(
    val rowsAffected: Int,
    val generatedKeys: List<Any> = listOf(),
    val executionTimeMs: Long
)

/**
 * Metadata about a database table column
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
 * Metadata about a database index
 */
data class IndexMetadata(
    val name: String,
    val columns: List<String>,
    val isUnique: Boolean,
    val type: String? = null
)

/**
 * Metadata about a database table
 */
data class TableMetadata(
    val name: String,
    val schema: String? = null,
    val columns: MutableList<ColumnMetadata>,
    val primaryKey: List<String>,
    val indexes: List<IndexMetadata> = listOf(),
    val rowCount: Long? = null,
    val description: String? = null
)

/**
 * Represents a relationship between two database tables
 */
data class Relationship(
    val id: String,
    val sourceTable: String,
    val sourceColumn: String,
    val targetTable: String,
    val targetColumn: String,
    val relationshipType: RelationshipType
)

/**
 * Metadata about a database
 */
data class DatabaseMetadata(
    val databaseName: String,
    val databaseType: DatabaseType,
    val version: String,
    val tables: List<TableMetadata>,
    val relationships: List<Relationship> = emptyList(),
    val schemas: List<String> = emptyList(),
    val properties: Map<String, String> = emptyMap()
) 