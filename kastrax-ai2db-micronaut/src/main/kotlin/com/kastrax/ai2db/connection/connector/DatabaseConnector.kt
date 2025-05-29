package com.kastrax.ai2db.connection.connector

import com.kastrax.ai2db.connection.model.*

/**
 * Interface for database connectors
 * 
 * This interface defines the API for connecting to and interacting with various 
 * database systems. Migrated from Spring Boot to Micronaut.
 */
interface DatabaseConnector {
    
    /**
     * Connect to a database with the provided configuration
     * 
     * @param config The connection configuration
     * @return A Connection object representing the active connection
     * @throws ConnectionException if the connection fails
     */
    suspend fun connect(config: ConnectionConfig): Connection
    
    /**
     * Disconnect from a database
     * 
     * @param connection The connection to disconnect
     * @return true if disconnected successfully, false otherwise
     * @throws ConnectionException if the disconnection fails
     */
    suspend fun disconnect(connection: Connection): Boolean
    
    /**
     * Test a database connection
     * 
     * @param config The connection configuration to test
     * @return ConnectionStatus.CONNECTED if successful, ConnectionStatus.FAILED otherwise
     */
    suspend fun testConnection(config: ConnectionConfig): ConnectionStatus
    
    /**
     * Get metadata about a database
     * 
     * @param connection The active connection
     * @return DatabaseMetadata containing information about the database
     */
    suspend fun getMetadata(connection: Connection): DatabaseMetadata
    
    /**
     * Execute a query on a database
     * 
     * @param connection The active connection
     * @param query The SQL query to execute
     * @param parameters Parameters to bind to the query
     * @param timeout Query timeout in milliseconds
     * @return QueryResult containing the results of the query
     * @throws QueryException if the query fails
     */
    suspend fun executeQuery(
        connection: Connection, 
        query: String,
        parameters: List<Any> = emptyList(),
        timeout: Long = 30000
    ): QueryResult
    
    /**
     * Execute an update operation on a database
     * 
     * @param connection The active connection
     * @param query The SQL update statement to execute
     * @param parameters Parameters to bind to the statement
     * @return UpdateResult containing information about the update
     * @throws QueryException if the update fails
     */
    suspend fun executeUpdate(
        connection: Connection,
        query: String,
        parameters: List<Any> = emptyList()
    ): UpdateResult
    
    /**
     * Begin a transaction
     * 
     * @param connection The active connection
     * @return A Transaction object representing the active transaction
     * @throws TransactionException if starting the transaction fails
     */
    suspend fun beginTransaction(connection: Connection): Transaction
    
    /**
     * Commit a transaction
     * 
     * @param transaction The transaction to commit
     * @throws TransactionException if committing the transaction fails
     */
    suspend fun commitTransaction(transaction: Transaction)
    
    /**
     * Rollback a transaction
     * 
     * @param transaction The transaction to rollback
     * @throws TransactionException if rolling back the transaction fails
     */
    suspend fun rollbackTransaction(transaction: Transaction)
}