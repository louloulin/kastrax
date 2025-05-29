package com.kastrax.ai2db.plugins

import com.kastrax.ai2db.connection.connector.DatabaseConnector
import com.kastrax.ai2db.connection.model.DatabaseType

/**
 * Plugin interface for database connectors
 * 
 * This interface allows for dynamic loading of database connector implementations
 * using Java's ServiceLoader mechanism, inspired by Kestra's plugin architecture.
 */
interface DatabaseConnectorPlugin {
    
    /**
     * Get the name of this plugin
     */
    fun getName(): String
    
    /**
     * Get the version of this plugin
     */
    fun getVersion(): String
    
    /**
     * Get the database types supported by this plugin
     */
    fun getSupportedTypes(): Set<DatabaseType>
    
    /**
     * Create a database connector instance for the specified type
     * 
     * @param type The database type to create a connector for
     * @return A DatabaseConnector instance
     * @throws IllegalArgumentException if the type is not supported
     */
    fun createConnector(type: DatabaseType): DatabaseConnector
    
    /**
     * Check if this plugin supports the given database type
     */
    fun supports(type: DatabaseType): Boolean = getSupportedTypes().contains(type)
    
    /**
     * Get plugin description
     */
    fun getDescription(): String = "Database connector plugin for ${getSupportedTypes().joinToString(", ")}"
    
    /**
     * Get plugin author
     */
    fun getAuthor(): String = "KastraX AI2DB"
    
    /**
     * Initialize the plugin (called when plugin is loaded)
     */
    fun initialize() {
        // Default implementation does nothing
    }
    
    /**
     * Cleanup resources when plugin is unloaded
     */
    fun cleanup() {
        // Default implementation does nothing
    }
}