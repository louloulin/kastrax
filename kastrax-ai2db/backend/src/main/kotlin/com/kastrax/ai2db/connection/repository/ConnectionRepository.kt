package com.kastrax.ai2db.connection.repository

import com.kastrax.ai2db.connection.model.ConnectionConfig

/**
 * Repository interface for managing connection configurations
 */
interface ConnectionRepository {
    /**
     * Find all connections
     */
    suspend fun findAll(): List<ConnectionConfig>
    
    /**
     * Find a connection by ID
     */
    suspend fun findById(id: String): ConnectionConfig?
    
    /**
     * Save a connection
     */
    suspend fun save(connection: ConnectionConfig): ConnectionConfig
    
    /**
     * Delete a connection
     */
    suspend fun delete(id: String): Boolean
    
    /**
     * Find connections by name (partial match)
     */
    suspend fun findByNameContaining(name: String): List<ConnectionConfig>
    
    /**
     * Find connections by type
     */
    suspend fun findByType(type: com.kastrax.ai2db.connection.model.DatabaseType): List<ConnectionConfig>
} 