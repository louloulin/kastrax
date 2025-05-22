package com.kastrax.ai2db.nl2sql.parser

import com.kastrax.ai2db.connection.model.DatabaseType
import com.kastrax.ai2db.nl2sql.model.QueryType
import com.kastrax.ai2db.nl2sql.model.SQLQuery

/**
 * Interface for parsing SQL queries
 */
interface SQLParser {
    /**
     * Parse a SQL query
     */
    fun parse(sql: String, databaseType: DatabaseType): SQLQuery
    
    /**
     * Determine the type of a SQL query
     */
    fun determineQueryType(sql: String): QueryType
    
    /**
     * Extract tables from a SQL query
     */
    fun extractTables(sql: String): List<String>
    
    /**
     * Extract columns from a SQL query
     */
    fun extractColumns(sql: String): List<String>
    
    /**
     * Extract conditions from a SQL query
     */
    fun extractConditions(sql: String): List<String>
    
    /**
     * Validate a SQL query
     */
    fun validate(sql: String, databaseType: DatabaseType): Boolean
} 