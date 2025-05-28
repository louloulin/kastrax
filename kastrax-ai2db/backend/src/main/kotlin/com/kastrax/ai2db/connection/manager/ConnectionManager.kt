package com.kastrax.ai2db.connection.manager

import com.kastrax.ai2db.connection.connector.DatabaseConnector
import com.kastrax.ai2db.connection.model.*
import com.kastrax.ai2db.connection.pool.ConnectionPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Connection manager responsible for creating, caching, and managing database connections
 */
@Service
class ConnectionManager(
    private val connectorRegistry: DatabaseConnectorRegistry,
    private val connectionPool: ConnectionPool
) {
    // Map of connection IDs to active connections
    private val activeConnections = ConcurrentHashMap<String, Connection>()

    // Map of connection configs to connection IDs for reuse
    private val configToConnectionMap = ConcurrentHashMap<String, String>()

    /**
     * Create a new database connection or return an existing one
     *
     * @param config The connection configuration
     * @param forceNew Whether to force creation of a new connection even if one exists
     * @return The connection object
     */
    suspend fun createConnection(config: ConnectionConfig, forceNew: Boolean = false): Connection = withContext(Dispatchers.IO) {
        // Generate a config hash for lookup
        val configHash = generateConfigHash(config)

        // Check if we have an existing connection for this config
        if (!forceNew && configToConnectionMap.containsKey(configHash)) {
            val connectionId = configToConnectionMap[configHash]
            val existingConnection = activeConnections[connectionId]

            if (existingConnection != null && existingConnection.isActive()) {
                // Return the existing connection, updating its last used time
                return@withContext existingConnection.markAsUsed()
            }
        }

        // No active connection exists or force new is true, create a new one
        val connector = connectorRegistry.getConnector(config.type)

        // Connect using the appropriate connector
        val connection = connector.connect(config)

        // Get a connection from the pool
        val pooledConnection = connectionPool.getConnection(connection)

        // Store the connection
        activeConnections[pooledConnection.id] = pooledConnection
        configToConnectionMap[configHash] = pooledConnection.id

        return@withContext pooledConnection
    }

    /**
     * Close a database connection
     *
     * @param connectionId The ID of the connection to close
     * @return true if the connection was closed successfully, false otherwise
     */
    suspend fun closeConnection(connectionId: String): Boolean = withContext(Dispatchers.IO) {
        val connection = activeConnections[connectionId] ?: return@withContext false

        try {
            // Remove from active connections
            activeConnections.remove(connectionId)

            // Remove from config map
            configToConnectionMap.entries
                .find { it.value == connectionId }
                ?.let { configToConnectionMap.remove(it.key) }

            // Return connection to the pool
            connectionPool.releaseConnection(connection)

            // Get the connector and disconnect
            val connector = connectorRegistry.getConnector(connection.config.type)
            connector.disconnect(connection)

            return@withContext true
        } catch (e: Exception) {
            // Log error
            // logger.error("Failed to close connection: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Get a connection by ID
     *
     * @param connectionId The ID of the connection to retrieve
     * @return The connection or null if not found
     */
    suspend fun getConnection(connectionId: String): Connection? = withContext(Dispatchers.IO) {
        val connection = activeConnections[connectionId] ?: return@withContext null

        if (!connection.isActive()) {
            // Connection is no longer active, try to reconnect
            try {
                val connector = connectorRegistry.getConnector(connection.config.type)
                val newConnection = connector.connect(connection.config)

                // Update the connection in the active map
                activeConnections[connectionId] = newConnection

                return@withContext newConnection
            } catch (e: Exception) {
                // Failed to reconnect
                // logger.error("Failed to reconnect: ${e.message}", e)
                closeConnection(connectionId)
                return@withContext null
            }
        }

        // Return the connection, updating its last used time
        return@withContext connection.markAsUsed()
    }

    /**
     * Test a database connection
     *
     * @param config The connection configuration to test
     * @return The connection status
     */
    suspend fun testConnection(config: ConnectionConfig): ConnectionStatus = withContext(Dispatchers.IO) {
        val connector = connectorRegistry.getConnector(config.type)
        return@withContext connector.testConnection(config)
    }

    /**
     * Get all active connections
     *
     * @return A list of all active connections
     */
    suspend fun getAllConnections(): List<Connection> = withContext(Dispatchers.IO) {
        return@withContext activeConnections.values.toList()
    }

    /**
     * Generate a hash for a connection config to uniquely identify it
     *
     * @param config The connection configuration
     * @return A hash string representing the config
     */
    private fun generateConfigHash(config: ConnectionConfig): String {
        return "${config.type.name}:${config.host}:${config.port}:${config.database}:${config.username}"
    }

    /**
     * Clean up idle connections that haven't been used for a while
     *
     * @param maxIdleTimeMs Maximum idle time in milliseconds
     */
    suspend fun cleanupIdleConnections(maxIdleTimeMs: Long = 30 * 60 * 1000) = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val connectionsToClose = mutableListOf<String>()

        for ((connectionId, connection) in activeConnections) {
            val idleTime = java.time.Duration.between(connection.lastUsedAt, now).toMillis()

            if (idleTime > maxIdleTimeMs) {
                connectionsToClose.add(connectionId)
            }
        }

        for (connectionId in connectionsToClose) {
            closeConnection(connectionId)
        }
    }
}
