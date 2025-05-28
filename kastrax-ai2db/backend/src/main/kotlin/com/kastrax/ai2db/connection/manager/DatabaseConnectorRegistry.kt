package com.kastrax.ai2db.connection.manager

import com.kastrax.ai2db.connection.connector.DatabaseConnector
import com.kastrax.ai2db.connection.model.DatabaseType
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

/**
 * Registry for database connectors
 *
 * This class maintains a registry of all available database connectors,
 * allowing the system to retrieve the appropriate connector for a given database type.
 */
@Component
class DatabaseConnectorRegistry(
    private val connectors: List<DatabaseConnector>
) {
    private val connectorMap = mutableMapOf<DatabaseType, DatabaseConnector>()

    @PostConstruct
    fun initialize() {
        // Register all available connectors by their database type
        for (connector in connectors) {
            when (connector) {
                is MySQLConnector -> connectorMap[DatabaseType.MYSQL] = connector
                is PostgreSQLConnector -> connectorMap[DatabaseType.POSTGRESQL] = connector
                is SQLServerConnector -> connectorMap[DatabaseType.SQLSERVER] = connector
                // Add more connector types as they are implemented
            }
        }
    }

    /**
     * Get the appropriate connector for a database type
     *
     * @param type The database type
     * @return The connector instance for the specified type
     * @throws IllegalArgumentException if no connector is registered for the given type
     */
    fun getConnector(type: DatabaseType): DatabaseConnector {
        return connectorMap[type] ?: throw IllegalArgumentException("No connector registered for database type: ${type.name}")
    }

    /**
     * Register a connector for a database type
     *
     * @param type The database type
     * @param connector The connector instance
     */
    fun registerConnector(type: DatabaseType, connector: DatabaseConnector) {
        connectorMap[type] = connector
    }

    /**
     * Check if a connector is registered for a database type
     *
     * @param type The database type
     * @return true if a connector is registered for the given type, false otherwise
     */
    fun hasConnector(type: DatabaseType): Boolean {
        return connectorMap.containsKey(type)
    }

    /**
     * Get all registered database types
     *
     * @return A list of all database types that have registered connectors
     */
    fun getSupportedDatabaseTypes(): List<DatabaseType> {
        return connectorMap.keys.toList()
    }
}
