package com.kastrax.ai2db.schema

import com.kastrax.ai2db.connection.model.Connection
import com.kastrax.ai2db.schema.model.DatabaseSchema
import com.kastrax.ai2db.schema.model.Relationship
import com.kastrax.ai2db.schema.model.TableSchema

/**
 * Interface for managing database schemas
 */
interface SchemaManager {
    /**
     * Extract schema from a database
     */
    suspend fun extractSchema(connection: Connection): DatabaseSchema
    
    /**
     * Cache schema information
     */
    suspend fun cacheSchema(connectionId: String, schema: DatabaseSchema): Boolean
    
    /**
     * Get cached schema information
     */
    suspend fun getSchema(connectionId: String): DatabaseSchema?
    
    /**
     * Synchronize schema information
     */
    suspend fun syncSchema(connectionId: String): DatabaseSchema
    
    /**
     * Get details for a specific table
     */
    suspend fun getTableDetails(
        connectionId: String, 
        tableName: String
    ): TableSchema?
    
    /**
     * Get relationships between tables
     */
    suspend fun getRelationships(connectionId: String): List<Relationship>
    
    /**
     * Find tables relevant to a query
     */
    suspend fun findRelevantTables(
        query: String,
        connectionId: String,
        limit: Int = 5
    ): List<TableSchema>
} 