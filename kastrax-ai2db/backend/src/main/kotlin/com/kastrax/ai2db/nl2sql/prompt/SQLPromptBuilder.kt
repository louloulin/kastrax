package com.kastrax.ai2db.nl2sql.prompt

import com.kastrax.ai2db.connection.model.DatabaseType
import com.kastrax.ai2db.nl2sql.model.ConversationContext
import com.kastrax.ai2db.schema.model.DatabaseSchema

/**
 * Interface for building prompts for NL2SQL conversion
 */
interface SQLPromptBuilder {
    /**
     * Build a prompt for SQL generation
     */
    fun buildSQLPrompt(
        query: String,
        databaseType: DatabaseType,
        schema: DatabaseSchema,
        context: ConversationContext? = null
    ): String
    
    /**
     * Build a prompt for SQL explanation
     */
    fun buildExplanationPrompt(
        query: String,
        databaseType: DatabaseType,
        schema: DatabaseSchema
    ): String
    
    /**
     * Build a prompt for SQL quality estimation
     */
    fun buildQualityCheckPrompt(
        query: String,
        sql: String,
        databaseType: DatabaseType,
        schema: DatabaseSchema
    ): String
} 