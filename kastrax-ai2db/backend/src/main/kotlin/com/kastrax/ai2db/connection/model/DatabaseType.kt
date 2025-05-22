package com.kastrax.ai2db.connection.model

/**
 * Supported database types in the system
 */
enum class DatabaseType {
    MYSQL,
    POSTGRESQL,
    MONGODB,
    REDIS,
    SQL_SERVER,
    ORACLE,
    ELASTICSEARCH;
    
    companion object {
        /**
         * Get the default port for a database type
         */
        fun getDefaultPort(type: DatabaseType): Int {
            return when (type) {
                MYSQL -> 3306
                POSTGRESQL -> 5432
                MONGODB -> 27017
                REDIS -> 6379
                SQL_SERVER -> 1433
                ORACLE -> 1521
                ELASTICSEARCH -> 9200
            }
        }
        
        /**
         * Get the JDBC URL prefix for a database type
         */
        fun getJdbcPrefix(type: DatabaseType): String {
            return when (type) {
                MYSQL -> "jdbc:mysql://"
                POSTGRESQL -> "jdbc:postgresql://"
                SQL_SERVER -> "jdbc:sqlserver://"
                ORACLE -> "jdbc:oracle:thin:@"
                else -> throw UnsupportedOperationException("JDBC URL not supported for ${type.name}")
            }
        }
    }
} 