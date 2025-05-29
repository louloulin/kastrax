package com.kastrax.ai2db.nl2sql.prompt

import com.kastrax.ai2db.nl2sql.model.ConversationContext
import com.kastrax.ai2db.schema.model.DatabaseSchema
import com.kastrax.ai2db.schema.model.TableSchema
import com.kastrax.ai2db.schema.model.ColumnSchema
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * SQL提示构建器 - Micronaut版本
 * 
 * 负责构建高质量的LLM提示，用于生成准确的SQL查询
 */
@Singleton
class SQLPromptBuilder {
    private val logger = LoggerFactory.getLogger(SQLPromptBuilder::class.java)
    
    /**
     * 构建SQL生成提示
     *
     * @param naturalLanguageQuery 自然语言查询
     * @param schema 数据库模式
     * @param context 对话上下文
     * @return 构建的提示
     */
    fun buildPrompt(
        naturalLanguageQuery: String,
        schema: DatabaseSchema,
        context: ConversationContext? = null
    ): String {
        logger.debug("Building SQL prompt for query: {}", naturalLanguageQuery)
        
        val promptBuilder = StringBuilder()
        
        // 1. 系统指令
        promptBuilder.append(buildSystemInstructions())
        promptBuilder.append("\n\n")
        
        // 2. 数据库模式信息
        promptBuilder.append(buildSchemaSection(schema))
        promptBuilder.append("\n\n")
        
        // 3. 示例查询（如果有上下文）
        context?.let {
            val examplesSection = buildExamplesSection(it)
            if (examplesSection.isNotBlank()) {
                promptBuilder.append(examplesSection)
                promptBuilder.append("\n\n")
            }
        }
        
        // 4. 查询规则和约束
        promptBuilder.append(buildRulesSection())
        promptBuilder.append("\n\n")
        
        // 5. 用户查询
        promptBuilder.append(buildQuerySection(naturalLanguageQuery))
        
        val prompt = promptBuilder.toString()
        logger.debug("Generated prompt with {} characters", prompt.length)
        
        return prompt
    }
    
    /**
     * 构建系统指令
     */
    private fun buildSystemInstructions(): String {
        return """
            You are an expert SQL query generator. Your task is to convert natural language questions into valid SQL queries.
            
            IMPORTANT GUIDELINES:
            - Generate ONLY the SQL query, no explanations or additional text
            - Use proper SQL syntax for the specified database type
            - Ensure the query is safe and does not contain destructive operations
            - Use appropriate JOINs when querying multiple tables
            - Apply proper filtering and sorting as requested
            - Use aliases for better readability when needed
            - Consider performance implications in your query design
        """.trimIndent()
    }
    
    /**
     * 构建数据库模式部分
     */
    private fun buildSchemaSection(schema: DatabaseSchema): String {
        val schemaBuilder = StringBuilder()
        
        schemaBuilder.append("DATABASE SCHEMA:\n")
        schemaBuilder.append("Database: ${schema.name} (${schema.type})\n")
        schemaBuilder.append("\n")
        
        // 按重要性排序表（主表在前）
        val sortedTables = schema.tables.sortedBy { table ->
            when {
                table.name.lowercase().contains("user") -> 1
                table.name.lowercase().contains("order") -> 2
                table.name.lowercase().contains("product") -> 3
                table.name.lowercase().contains("customer") -> 4
                else -> 5
            }
        }
        
        sortedTables.forEach { table ->
            schemaBuilder.append(buildTableDescription(table))
            schemaBuilder.append("\n")
        }
        
        // 添加关系信息
        if (schema.relationships.isNotEmpty()) {
            schemaBuilder.append("\nTABLE RELATIONSHIPS:\n")
            schema.relationships.forEach { relationship ->
                schemaBuilder.append("- ${relationship.fromTable}.${relationship.fromColumn} -> ")
                schemaBuilder.append("${relationship.toTable}.${relationship.toColumn} (${relationship.type})\n")
            }
        }
        
        return schemaBuilder.toString()
    }
    
    /**
     * 构建表描述
     */
    private fun buildTableDescription(table: TableSchema): String {
        val tableBuilder = StringBuilder()
        
        tableBuilder.append("Table: ${table.name}")
        if (table.comment.isNotBlank()) {
            tableBuilder.append(" - ${table.comment}")
        }
        tableBuilder.append("\n")
        
        // 主键列
        val primaryKeys = table.columns.filter { it.isPrimaryKey }
        if (primaryKeys.isNotEmpty()) {
            tableBuilder.append("  Primary Key: ${primaryKeys.joinToString(", ") { it.name }}\n")
        }
        
        // 列信息
        tableBuilder.append("  Columns:\n")
        table.columns.forEach { column ->
            tableBuilder.append("    - ${buildColumnDescription(column)}\n")
        }
        
        // 索引信息
        if (table.indexes.isNotEmpty()) {
            tableBuilder.append("  Indexes: ${table.indexes.joinToString(", ") { it.name }}\n")
        }
        
        return tableBuilder.toString()
    }
    
    /**
     * 构建列描述
     */
    private fun buildColumnDescription(column: ColumnSchema): String {
        val columnBuilder = StringBuilder()
        
        columnBuilder.append("${column.name}: ${column.dataType}")
        
        if (column.maxLength > 0) {
            columnBuilder.append("(${column.maxLength})")
        }
        
        val attributes = mutableListOf<String>()
        if (column.isPrimaryKey) attributes.add("PK")
        if (column.isForeignKey) attributes.add("FK")
        if (!column.isNullable) attributes.add("NOT NULL")
        if (column.isUnique) attributes.add("UNIQUE")
        if (column.defaultValue.isNotBlank()) attributes.add("DEFAULT ${column.defaultValue}")
        
        if (attributes.isNotEmpty()) {
            columnBuilder.append(" [${attributes.joinToString(", ")}]")
        }
        
        if (column.comment.isNotBlank()) {
            columnBuilder.append(" // ${column.comment}")
        }
        
        return columnBuilder.toString()
    }
    
    /**
     * 构建示例部分
     */
    private fun buildExamplesSection(context: ConversationContext): String {
        val recentQueries = context.getRecentSuccessfulQueries()
        if (recentQueries.isEmpty()) {
            return ""
        }
        
        val examplesBuilder = StringBuilder()
        examplesBuilder.append("RECENT SUCCESSFUL QUERIES (for reference):\n")
        
        recentQueries.takeLast(3).forEach { query ->
            examplesBuilder.append("Q: ${query.originalQuery}\n")
            examplesBuilder.append("A: ${query.sql}\n\n")
        }
        
        return examplesBuilder.toString()
    }
    
    /**
     * 构建规则部分
     */
    private fun buildRulesSection(): String {
        return """
            QUERY GENERATION RULES:
            1. Always use proper table and column names as defined in the schema
            2. Use appropriate WHERE clauses for filtering
            3. Apply JOINs when data from multiple tables is needed
            4. Use ORDER BY for sorting requirements
            5. Apply LIMIT for result count restrictions
            6. Use aggregate functions (COUNT, SUM, AVG, etc.) when appropriate
            7. Handle date/time comparisons properly
            8. Use LIKE for pattern matching with wildcards
            9. Consider NULL values in comparisons
            10. Ensure the query is syntactically correct and executable
            
            SAFETY CONSTRAINTS:
            - NEVER generate DELETE, UPDATE, INSERT, DROP, or ALTER statements
            - Only generate SELECT queries for data retrieval
            - Avoid queries that could cause performance issues
            - Do not include comments or explanations in the SQL output
        """.trimIndent()
    }
    
    /**
     * 构建查询部分
     */
    private fun buildQuerySection(naturalLanguageQuery: String): String {
        return """
            USER QUERY:
            "$naturalLanguageQuery"
            
            Generate the corresponding SQL query:
        """.trimIndent()
    }
    
    /**
     * 构建优化提示
     */
    fun buildOptimizationPrompt(originalSQL: String, schema: DatabaseSchema): String {
        return """
            You are a SQL optimization expert. Analyze the following SQL query and suggest optimizations.
            
            ORIGINAL QUERY:
            $originalSQL
            
            DATABASE SCHEMA:
            ${buildSchemaSection(schema)}
            
            Please provide an optimized version of the query that:
            1. Improves performance
            2. Maintains the same result set
            3. Uses proper indexing strategies
            4. Minimizes data transfer
            5. Follows SQL best practices
            
            Return ONLY the optimized SQL query:
        """.trimIndent()
    }
    
    /**
     * 构建解释提示
     */
    fun buildExplanationPrompt(sqlQuery: String): String {
        return """
            Explain the following SQL query in simple terms:
            
            SQL QUERY:
            $sqlQuery
            
            Provide a clear, concise explanation of:
            1. What data the query retrieves
            2. Which tables are involved
            3. What conditions are applied
            4. How the results are organized
            
            Keep the explanation user-friendly and avoid technical jargon.
        """.trimIndent()
    }
}