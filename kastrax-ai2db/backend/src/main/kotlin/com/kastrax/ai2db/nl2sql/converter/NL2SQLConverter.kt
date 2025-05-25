package com.kastrax.ai2db.nl2sql.converter

import com.kastrax.ai2db.connection.model.DatabaseType
import com.kastrax.ai2db.nl2sql.model.ConversionExplanation
import com.kastrax.ai2db.nl2sql.model.ConversationContext
import com.kastrax.ai2db.nl2sql.model.SQLQuery
import com.kastrax.ai2db.schema.model.DatabaseSchema

/**
 * Interface for converting natural language to SQL
 */
interface NL2SQLConverter {
    /**
     * Convert a natural language query to SQL
     */
    suspend fun convertToSQL(
        query: String,
        databaseType: DatabaseType,
        schema: DatabaseSchema,
        context: ConversationContext? = null
    ): SQLQuery
    
    /**
     * Explain the conversion process for a natural language query
     */
    suspend fun explainConversion(
        query: String,
        databaseType: DatabaseType,
        schema: DatabaseSchema
    ): ConversionExplanation
    
    /**
     * Estimate the quality of the generated SQL
     */
    suspend fun estimateQuality(
        query: String,
        sql: String,
        databaseType: DatabaseType,
        schema: DatabaseSchema
    ): Float
} 