package com.kastrax.ai2db.connection.model

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Version
import io.micronaut.serde.annotation.Serdeable
import java.sql.Connection as JavaSqlConnection
import java.time.Instant
import javax.sql.DataSource
import java.util.UUID

/**
 * Types of databases supported by the application
 */
@Serdeable
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
@Serdeable
enum class ConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    FAILED,
    PENDING
}

/**
 * Types of relationships between database tables
 */
@Serdeable
enum class RelationshipType {
    ONE_TO_ONE,
    ONE_TO_MANY,
    MANY_TO_ONE,
    MANY_TO_MANY
}

/**
 * Configuration for a database connection
 */
@Serdeable
@Introspected
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
@Serdeable
@Introspected
data class Connection(
    val id: String = UUID.randomUUID().toString(),
    val config: ConnectionConfig,
    val status: ConnectionStatus = ConnectionStatus.PENDING,
    val dataSource: DataSource? = null,
    val jdbcConnection: JavaSqlConnection? = null,
    val createdAt: Instant = Instant.now(),
    val lastUsedAt: Instant = Instant.now(),
    val connectedAt: Instant? = null,
    val disconnectedAt: Instant? = null,
    val errorMessage: String? = null
) {
    /**
     * Check if the connection is active
     */
    fun isActive(): Boolean = status == ConnectionStatus.CONNECTED && jdbcConnection != null &&
                            !(jdbcConnection?.isClosed ?: true)

    /**
     * Create a copy of this connection with updated status
     */
    fun withStatus(newStatus: ConnectionStatus, error: String? = null): Connection {
        return this.copy(
            status = newStatus,
            errorMessage = error ?: this.errorMessage,
            connectedAt = if (newStatus == ConnectionStatus.CONNECTED) Instant.now() else this.connectedAt,
            disconnectedAt = if (newStatus == ConnectionStatus.DISCONNECTED) Instant.now() else this.disconnectedAt
        )
    }

    /**
     * Update the last used timestamp
     */
    fun markAsUsed(): Connection {
        return this.copy(lastUsedAt = Instant.now())
    }
}

/**
 * Represents a database transaction
 */
@Serdeable
@Introspected
data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val connection: Connection,
    val createdAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val isActive: Boolean = true
)

/**
 * Column metadata model
 */
@Serdeable
@Introspected
data class ColumnMetadata(
    val name: String,
    val dataType: String,
    val typeName: String,
    val size: Int,
    val nullable: Boolean,
    val primaryKey: Boolean = false,
    val autoIncrement: Boolean = false,
    val defaultValue: String? = null,
    val comment: String? = null,
    val ordinalPosition: Int
)

/**
 * Table metadata model
 */
@Serdeable
@Introspected
data class TableMetadata(
    val name: String,
    val schema: String,
    val type: String,  // TABLE, VIEW, etc.
    val columns: List<ColumnMetadata> = emptyList(),
    val primaryKeys: List<String> = emptyList(),
    val foreignKeys: List<ForeignKey> = emptyList(),
    val indexes: List<Index> = emptyList(),
    val comment: String? = null
)

/**
 * Foreign key model
 */
@Serdeable
@Introspected
data class ForeignKey(
    val name: String,
    val columnNames: List<String>,
    val referencedTableName: String,
    val referencedColumnNames: List<String>,
    val updateRule: String,
    val deleteRule: String
)

/**
 * Index model
 */
@Serdeable
@Introspected
data class Index(
    val name: String,
    val columnNames: List<String>,
    val unique: Boolean,
    val type: String? = null
)

/**
 * Database metadata model
 */
@Serdeable
@Introspected
data class DatabaseMetadata(
    val databaseName: String,
    val schemaName: String?,
    val tables: List<TableMetadata> = emptyList(),
    val views: List<TableMetadata> = emptyList(),
    val databaseProductName: String,
    val databaseProductVersion: String,
    val driverName: String,
    val driverVersion: String,
    val relationships: List<Relationship> = emptyList(),
    val fetchedAt: Instant = Instant.now()
)

/**
 * Relationship between tables
 */
@Serdeable
@Introspected
data class Relationship(
    val id: String = UUID.randomUUID().toString(),
    val sourceTable: String,
    val sourceColumns: List<String>,
    val targetTable: String,
    val targetColumns: List<String>,
    val type: RelationshipType,
    val name: String? = null,
    val foreignKeyName: String? = null
)

/**
 * Column data for a result set
 */
@Serdeable
@Introspected
data class ColumnData(
    val name: String,
    val label: String,
    val type: String,
    val typeName: String
)

/**
 * Result of a database query
 */
@Serdeable
@Introspected
data class QueryResult(
    val columns: List<ColumnData> = emptyList(),
    val rows: List<Map<String, Any?>> = emptyList(),
    val rowCount: Int = 0,
    val executionTime: Long = 0,
    val truncated: Boolean = false,
    val hasMoreRows: Boolean = false,
    val error: String? = null,
    val success: Boolean = true
)

/**
 * Result of a database update operation
 */
@Serdeable
@Introspected
data class UpdateResult(
    val affectedRows: Int = 0,
    val generatedKeys: List<Any> = emptyList(),
    val executionTime: Long = 0,
    val error: String? = null,
    val success: Boolean = true
)

/**
 * Connection configuration entity for persistence
 */
@MappedEntity("connection_configs")
@Serdeable
@Introspected
data class ConnectionConfigEntity(
    @field:Id
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: DatabaseType,
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val password: String, // Should be encrypted in production
    val properties: String = "{}", // JSON string
    val sslEnabled: Boolean = false,
    val timezone: String = "UTC",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    @field:Version
    val version: Long = 0
) {
    /**
     * Convert entity to domain model
     */
    fun toConnectionConfig(): ConnectionConfig {
        // Parse properties JSON string to Map
        val propertiesMap = if (properties.isBlank() || properties == "{}") {
            emptyMap()
        } else {
            // Simple JSON parsing - in production use proper JSON library
            properties.removeSurrounding("{", "}")
                .split(",")
                .associate {
                    val (key, value) = it.split(":")
                    key.trim().removeSurrounding("\"") to value.trim().removeSurrounding("\"")
                }
        }
        
        return ConnectionConfig(
            name = name,
            type = type,
            host = host,
            port = port,
            database = database,
            username = username,
            password = password,
            properties = propertiesMap,
            sslEnabled = sslEnabled,
            timezone = timezone
        )
    }
    
    companion object {
        /**
         * Create entity from domain model
         */
        fun fromConnectionConfig(config: ConnectionConfig, id: String? = null): ConnectionConfigEntity {
            // Convert properties Map to JSON string
            val propertiesJson = if (config.properties.isEmpty()) {
                "{}"
            } else {
                config.properties.entries.joinToString(",", "{", "}") { (key, value) ->
                    "\"$key\":\"$value\""
                }
            }
            
            return ConnectionConfigEntity(
                id = id ?: UUID.randomUUID().toString(),
                name = config.name,
                type = config.type,
                host = config.host,
                port = config.port,
                database = config.database,
                username = config.username,
                password = config.password,
                properties = propertiesJson,
                sslEnabled = config.sslEnabled,
                timezone = config.timezone
            )
        }
    }
}

/**
 * Connection exception class
 */
class ConnectionException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

/**
 * Query exception class
 */
class QueryException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

/**
 * Transaction exception class
 */
class TransactionException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)