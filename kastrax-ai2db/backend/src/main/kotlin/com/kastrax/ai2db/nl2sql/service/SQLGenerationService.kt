package com.kastrax.ai2db.nl2sql.service

import com.kastrax.ai2db.nl2sql.model.SQLGenerationResult
import com.kastrax.ai2db.nl2sql.model.QueryType
import com.kastrax.ai2db.nl2sql.model.QueryComplexity
import com.kastrax.ai2db.schema.model.DatabaseSchema
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQL生成服务
 * 
 * 负责将自然语言转换为SQL语句的核心逻辑
 */
@Service
class SQLGenerationService {
    
    private val logger = LoggerFactory.getLogger(SQLGenerationService::class.java)
    
    /**
     * 生成SQL语句
     */
    suspend fun generateSQL(
        prompt: String,
        schema: DatabaseSchema?
    ): SQLGenerationResult = withContext(Dispatchers.IO) {
        logger.info("Generating SQL for prompt: {}", prompt.take(100))
        
        try {
            // 这里是简化的实现，实际应该调用LLM服务
            val sql = generateSQLFromPrompt(prompt, schema)
            val confidence = calculateConfidence(sql, schema)
            val explanation = generateExplanation(prompt, sql)
            val queryType = detectQueryType(sql)
            val complexity = estimateComplexity(sql)
            
            SQLGenerationResult(
                sql = sql,
                confidence = confidence,
                explanation = explanation,
                queryType = queryType,
                complexity = complexity
            )
        } catch (e: Exception) {
            logger.error("Failed to generate SQL", e)
            throw e
        }
    }
    
    /**
     * 从提示生成SQL（简化实现）
     */
    private fun generateSQLFromPrompt(prompt: String, schema: DatabaseSchema?): String {
        // 这是一个简化的实现，实际应该调用LLM API
        // 这里只是为了演示，返回一个基本的SELECT语句
        
        val lowerPrompt = prompt.lowercase()
        
        return when {
            lowerPrompt.contains("查询") || lowerPrompt.contains("select") || lowerPrompt.contains("获取") -> {
                if (schema?.tables?.isNotEmpty() == true) {
                    val firstTable = schema.tables.first()
                    "SELECT * FROM ${firstTable.name} LIMIT 10;"
                } else {
                    "SELECT * FROM users LIMIT 10;"
                }
            }
            lowerPrompt.contains("插入") || lowerPrompt.contains("insert") || lowerPrompt.contains("添加") -> {
                "INSERT INTO users (name, email) VALUES ('示例用户', 'example@email.com');"
            }
            lowerPrompt.contains("更新") || lowerPrompt.contains("update") || lowerPrompt.contains("修改") -> {
                "UPDATE users SET name = '新名称' WHERE id = 1;"
            }
            lowerPrompt.contains("删除") || lowerPrompt.contains("delete") -> {
                "DELETE FROM users WHERE id = 1;"
            }
            else -> {
                "SELECT * FROM users LIMIT 10;"
            }
        }
    }
    
    /**
     * 计算置信度
     */
    private fun calculateConfidence(sql: String, schema: DatabaseSchema?): Double {
        var confidence = 0.5 // 基础置信度
        
        // 如果有模式信息，提高置信度
        if (schema != null) {
            confidence += 0.2
        }
        
        // 如果SQL语法看起来正确，提高置信度
        if (sql.trim().endsWith(";")) {
            confidence += 0.1
        }
        
        // 如果包含常见的SQL关键字，提高置信度
        val keywords = listOf("SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE")
        val upperSQL = sql.uppercase()
        val keywordCount = keywords.count { upperSQL.contains(it) }
        confidence += keywordCount * 0.05
        
        return minOf(confidence, 1.0)
    }
    
    /**
     * 生成解释
     */
    private fun generateExplanation(prompt: String, sql: String): String {
        return "根据您的查询'${prompt.take(50)}...'，生成了SQL语句：$sql"
    }
    
    /**
     * 检测查询类型
     */
    private fun detectQueryType(sql: String): QueryType {
        val upperSQL = sql.uppercase().trim()
        return when {
            upperSQL.startsWith("SELECT") -> QueryType.SELECT
            upperSQL.startsWith("INSERT") -> QueryType.INSERT
            upperSQL.startsWith("UPDATE") -> QueryType.UPDATE
            upperSQL.startsWith("DELETE") -> QueryType.DELETE
            upperSQL.startsWith("CREATE") -> QueryType.CREATE
            upperSQL.startsWith("DROP") -> QueryType.DROP
            upperSQL.startsWith("ALTER") -> QueryType.ALTER
            else -> QueryType.SELECT
        }
    }
    
    /**
     * 估算复杂度
     */
    private fun estimateComplexity(sql: String): QueryComplexity {
        val upperSQL = sql.uppercase()
        var complexityScore = 0
        
        // 检查各种复杂度指标
        if (upperSQL.contains("JOIN")) complexityScore += 2
        if (upperSQL.contains("SUBQUERY") || upperSQL.contains("(")) complexityScore += 2
        if (upperSQL.contains("GROUP BY")) complexityScore += 1
        if (upperSQL.contains("ORDER BY")) complexityScore += 1
        if (upperSQL.contains("HAVING")) complexityScore += 2
        if (upperSQL.contains("UNION")) complexityScore += 3
        
        return when {
            complexityScore <= 1 -> QueryComplexity.LOW
            complexityScore <= 4 -> QueryComplexity.MEDIUM
            else -> QueryComplexity.HIGH
        }
    }
}