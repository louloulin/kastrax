package com.kastrax.ai2db.connection.connector

import com.kastrax.ai2db.connection.model.ConnectionConfig
import com.kastrax.ai2db.connection.model.ConnectionStatus
import com.kastrax.ai2db.connection.model.DatabaseMetadata
import com.kastrax.ai2db.connection.model.QueryResult
import com.kastrax.ai2db.connection.model.UpdateResult
import com.kastrax.ai2db.connection.model.Connection
import com.kastrax.ai2db.connection.model.Transaction

/**
 * Interface for database connectors
 */
interface DatabaseConnector {
    /**
     * Connect to a database
     */
    suspend fun connect(config: ConnectionConfig): Connection
    
    /**
     * Disconnect from a database
     */
    suspend fun disconnect(connection: Connection): Boolean
    
    /**
     * Test a database connection
     */
    suspend fun testConnection(config: ConnectionConfig): ConnectionStatus
    
    /**
     * Get metadata about a database
     */
    suspend fun getMetadata(connection: Connection): DatabaseMetadata
    
    /**
     * Execute a query
     */
    suspend fun executeQuery(
        connection: Connection, 
        query: String,
        parameters: List<Any> = listOf(),
        timeout: Long = 30000L
    ): QueryResult
    
    /**
     * Execute an update operation
     */
    suspend fun executeUpdate(
        connection: Connection,
        query: String,
        parameters: List<Any> = listOf()
    ): UpdateResult
    
    /**
     * Begin a transaction
     */
    suspend fun beginTransaction(connection: Connection): Transaction
    
    /**
     * Commit a transaction
     */
    suspend fun commitTransaction(transaction: Transaction)
    
    /**
     * Rollback a transaction
     */
    suspend fun rollbackTransaction(transaction: Transaction)
} 