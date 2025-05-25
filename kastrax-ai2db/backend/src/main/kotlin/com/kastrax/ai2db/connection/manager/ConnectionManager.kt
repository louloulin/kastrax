package com.kastrax.ai2db.connection.manager

import com.kastrax.ai2db.connection.model.Connection
import com.kastrax.ai2db.connection.model.ConnectionConfig
import com.kastrax.ai2db.connection.model.ConnectionStatus
import com.kastrax.ai2db.connection.model.DatabaseMetadata
import com.kastrax.ai2db.connection.model.DatabaseType
import com.kastrax.ai2db.connection.model.QueryResult
import com.kastrax.ai2db.connection.model.UpdateResult

/**
 * Interface for managing database connections
 */
interface ConnectionManager {
    /**
     * Get all available database connections
     */
    suspend fun getAllConnections(): List<ConnectionConfig>
    
    /**
     * Get a connection configuration by ID
     */
    suspend fun getConnectionConfig(id: String): ConnectionConfig?
    
    /**
     * Create a new connection configuration
     */
    suspend fun createConnection(config: ConnectionConfig): ConnectionConfig
    
    /**
     * Update an existing connection configuration
     */
    suspend fun updateConnection(id: String, config: ConnectionConfig): ConnectionConfig
    
    /**
     * Delete a connection configuration
     */
    suspend fun deleteConnection(id: String): Boolean
    
    /**
     * Test a connection
     */
    suspend fun testConnection(config: ConnectionConfig): ConnectionStatus
    
    /**
     * Get or create an active connection
     */
    suspend fun getConnection(id: String): Connection
    
    /**
     * Close an active connection
     */
    suspend fun closeConnection(id: String): Boolean
    
    /**
     * Get metadata for a database
     */
    suspend fun getDatabaseMetadata(connectionId: String): DatabaseMetadata
    
    /**
     * Execute a query on a database
     */
    suspend fun executeQuery(
        connectionId: String,
        query: String,
        parameters: List<Any> = listOf(),
        timeout: Long = 30000L
    ): QueryResult
    
    /**
     * Execute an update operation on a database
     */
    suspend fun executeUpdate(
        connectionId: String,
        query: String,
        parameters: List<Any> = listOf()
    ): UpdateResult
    
    /**
     * Get a connector for a specific database type
     */
    fun getConnectorForType(type: DatabaseType): com.kastrax.ai2db.connection.connector.DatabaseConnector
} 