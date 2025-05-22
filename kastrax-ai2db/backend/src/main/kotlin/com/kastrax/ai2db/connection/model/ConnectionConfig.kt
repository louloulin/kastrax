package com.kastrax.ai2db.connection.model

/**
 * Configuration for a database connection
 */
data class ConnectionConfig(
    val id: String,
    val name: String,
    val type: DatabaseType,
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val password: String,
    val ssl: Boolean = false,
    val connectionTimeout: Long = 30000L, // 30 seconds
    val idleTimeout: Long = 600000L, // 10 minutes
    val maxPoolSize: Int = 10,
    val parameters: Map<String, String> = mapOf()
) {
    /**
     * Create a JDBC URL for this connection
     */
    fun toJdbcUrl(): String {
        return when (type) {
            DatabaseType.MYSQL -> "${DatabaseType.getJdbcPrefix(type)}$host:$port/$database?useSSL=$ssl"
            DatabaseType.POSTGRESQL -> "${DatabaseType.getJdbcPrefix(type)}$host:$port/$database"
            DatabaseType.SQL_SERVER -> "${DatabaseType.getJdbcPrefix(type)}$host:$port;databaseName=$database"
            DatabaseType.ORACLE -> "${DatabaseType.getJdbcPrefix(type)}$host:$port:$database"
            else -> throw UnsupportedOperationException("JDBC URL not supported for ${type.name}")
        }
    }
    
    /**
     * Create a copy of the configuration with password masked for logging
     */
    fun toSafeString(): String {
        return copy(password = "********").toString()
    }
} 