package com.kastrax.ai2db.connection.pool

import com.kastrax.ai2db.connection.model.Connection
import com.kastrax.ai2db.connection.model.ConnectionConfig
import com.kastrax.ai2db.connection.model.ConnectionStatus
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Connection pool for managing database connections
 *
 * This class manages a pool of database connections, allowing them to be reused
 * rather than creating new connections each time, which is expensive.
 */
@Component
class ConnectionPool(
    private val maxPoolSize: Int = DEFAULT_MAX_POOL_SIZE,
    private val maxIdleTime: Long = DEFAULT_MAX_IDLE_TIME,
    private val connectionTimeout: Long = DEFAULT_CONNECTION_TIMEOUT
) {
    // Map of connection configuration hash to pool of connections
    private val pools = ConcurrentHashMap<String, LinkedBlockingQueue<Connection>>()

    // Map to track when connections were last used
    private val lastUsedTimes = ConcurrentHashMap<String, Instant>()

    /**
     * Get a connection from the pool or create a new one
     *
     * @param connection The connection template to get or create
     * @return A connection from the pool if available, otherwise the input connection
     */
    fun getConnection(connection: Connection): Connection {
        val configHash = generateConfigHash(connection.config)

        // Get or create a pool for this connection configuration
        val pool = pools.computeIfAbsent(configHash) { LinkedBlockingQueue() }

        // Try to get a connection from the pool
        var pooledConnection = pool.poll()

        // If no connection was available in the pool, use the provided connection
        if (pooledConnection == null) {
            pooledConnection = connection
        }

        // Update the last used time
        lastUsedTimes[pooledConnection.id] = Instant.now()

        // Return the connection
        return pooledConnection
    }

    /**
     * Release a connection back to the pool
     *
     * @param connection The connection to release
     * @return true if the connection was released to the pool, false if it was closed
     */
    fun releaseConnection(connection: Connection): Boolean {
        // Only pool connections that are still active
        if (connection.status != ConnectionStatus.CONNECTED || !connection.isActive()) {
            return false
        }

        val configHash = generateConfigHash(connection.config)
        val pool = pools.computeIfAbsent(configHash) { LinkedBlockingQueue() }

        // If the pool is full, don't add the connection back
        if (pool.size >= maxPoolSize) {
            return false
        }

        // Update the last used time
        lastUsedTimes[connection.id] = Instant.now()

        // Add the connection back to the pool
        return pool.offer(connection)
    }

    /**
     * Clean up idle connections that haven't been used for a while
     */
    fun cleanupIdleConnections() {
        val now = Instant.now()
        val connectionsToRemove = mutableListOf<String>()

        // Check all tracked connections for idle ones
        for ((connectionId, lastUsed) in lastUsedTimes) {
            val idleTime = java.time.Duration.between(lastUsed, now).toMillis()

            if (idleTime > maxIdleTime) {
                connectionsToRemove.add(connectionId)
            }
        }

        // Remove idle connections
        for (connectionId in connectionsToRemove) {
            // Remove from last used tracking
            lastUsedTimes.remove(connectionId)

            // Remove from all pools
            for (pool in pools.values) {
                pool.removeIf { it.id == connectionId }
            }
        }
    }

    /**
     * Get the current pool size for a specific connection configuration
     *
     * @param config The connection configuration
     * @return The number of connections in the pool for the given configuration
     */
    fun getPoolSize(config: ConnectionConfig): Int {
        val configHash = generateConfigHash(config)
        val pool = pools[configHash]
        return pool?.size ?: 0
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

    companion object {
        const val DEFAULT_MAX_POOL_SIZE = 10
        const val DEFAULT_MAX_IDLE_TIME = 10 * 60 * 1000L // 10 minutes
        const val DEFAULT_CONNECTION_TIMEOUT = 30 * 1000L // 30 seconds
    }
}
