package com.kastrax.ai2db.nl2sql.tool

import ai.kastrax.core.tool.Tool
import ai.kastrax.core.tool.ToolExecuteResult
import ai.kastrax.core.tool.ToolContext
import com.kastrax.ai2db.connection.manager.ConnectionManager
import com.kastrax.ai2db.schema.model.DatabaseSchema
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException

/**
 * SQL验证工具
 * 
 * 用于验证生成的SQL语句的语法正确性和安全性
 */
@Component
class SQLValidationTool(
    private val connectionManager: ConnectionManager
) : Tool {
    
    private val logger = LoggerFactory.getLogger(SQLValidationTool::class.java)
    
    override val id: String = "sql-validation"
    override val name: String = "SQL验证工具"
    override val description: String = "验证SQL语句的语法正确性、安全性和性能"
    override val version: String = "1.0.0"
    
    // 危险的SQL关键词
    private val dangerousKeywords = setOf(
        "DROP", "DELETE", "TRUNCATE", "ALTER", "CREATE", "INSERT", "UPDATE",
        "EXEC", "EXECUTE", "GRANT", "REVOKE", "SHUTDOWN", "KILL"
    )
    
    // 可疑的SQL模式
    private val suspiciousPatterns = listOf(
        ".*;.*", // 多语句
        "--", // SQL注释
        "/\\*.*\\*/", // 块注释
        "UNION.*SELECT", // UNION注入
        "OR.*1.*=.*1", // 经典注入
        "AND.*1.*=.*1", // 经典注入
        "'.*OR.*'", // 字符串注入
        "SLEEP\\(", // 时间延迟攻击
        "BENCHMARK\\(", // 基准测试攻击
        "LOAD_FILE\\(", // 文件读取
        "INTO.*OUTFILE", // 文件写入
        "INFORMATION_SCHEMA", // 信息模式查询
        "SHOW.*TABLES", // 表结构探测
        "DESCRIBE", // 表结构探测
        "EXPLAIN" // 执行计划探测
    )
    
    override suspend fun execute(
        input: Map<String, Any>,
        context: ToolContext?
    ): ToolExecuteResult {
        logger.info("Executing SQLValidationTool with input: {}", input)
        
        return try {
            val sql = input["sql"] as? String
                ?: throw IllegalArgumentException("Missing 'sql' parameter")
            
            val databaseId = input["database"] as? String
            val schema = input["schema"] as? String
            val strictMode = input["strictMode"] as? Boolean ?: true
            val checkSyntax = input["checkSyntax"] as? Boolean ?: true
            val checkSecurity = input["checkSecurity"] as? Boolean ?: true
            val checkPerformance = input["checkPerformance"] as? Boolean ?: false
            
            val validationResult = validateSQL(
                sql = sql,
                databaseId = databaseId,
                schema = schema,
                strictMode = strictMode,
                checkSyntax = checkSyntax,
                checkSecurity = checkSecurity,
                checkPerformance = checkPerformance
            )
            
            ToolExecuteResult(
                success = true,
                output = validationResult,
                metadata = mapOf(
                    "sql" to sql,
                    "validationTime" to System.currentTimeMillis(),
                    "strictMode" to strictMode
                )
            )
        } catch (e: Exception) {
            logger.error("Error executing SQLValidationTool", e)
            ToolExecuteResult(
                success = false,
                output = mapOf(
                    "valid" to false,
                    "error" to e.message,
                    "errorType" to e.javaClass.simpleName
                ),
                metadata = mapOf("errorType" to e.javaClass.simpleName)
            )
        }
    }
    
    /**
     * 验证SQL语句
     */
    private fun validateSQL(
        sql: String,
        databaseId: String?,
        schema: String?,
        strictMode: Boolean,
        checkSyntax: Boolean,
        checkSecurity: Boolean,
        checkPerformance: Boolean
    ): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()
        
        // 基本验证
        if (sql.isBlank()) {
            errors.add("SQL语句不能为空")
            return mapOf(
                "valid" to false,
                "errors" to errors,
                "warnings" to warnings
            )
        }
        
        // 安全性检查
        if (checkSecurity) {
            val securityIssues = checkSQLSecurity(sql, strictMode)
            if (securityIssues.isNotEmpty()) {
                if (strictMode) {
                    errors.addAll(securityIssues)
                } else {
                    warnings.addAll(securityIssues)
                }
            }
        }
        
        // 语法检查
        if (checkSyntax && databaseId != null) {
            val syntaxIssues = checkSQLSyntax(sql, databaseId)
            errors.addAll(syntaxIssues)
        }
        
        // 性能检查
        if (checkPerformance) {
            val performanceIssues = checkSQLPerformance(sql)
            warnings.addAll(performanceIssues)
        }
        
        // 基本SQL结构检查
        val structureIssues = checkSQLStructure(sql)
        warnings.addAll(structureIssues)
        
        val isValid = errors.isEmpty()
        
        result["valid"] = isValid
        result["errors"] = errors
        result["warnings"] = warnings
        result["confidence"] = calculateValidationConfidence(errors, warnings)
        result["sqlType"] = detectSQLType(sql)
        result["complexity"] = estimateSQLComplexity(sql)
        
        return result
    }
    
    /**
     * 检查SQL安全性
     */
    private fun checkSQLSecurity(sql: String, strictMode: Boolean): List<String> {
        val issues = mutableListOf<String>()
        val upperSQL = sql.uppercase()
        
        // 检查危险关键词
        dangerousKeywords.forEach { keyword ->
            if (upperSQL.contains(keyword)) {
                if (strictMode || keyword in setOf("DROP", "TRUNCATE", "SHUTDOWN", "KILL")) {
                    issues.add("检测到危险的SQL关键词: $keyword")
                }
            }
        }
        
        // 检查可疑模式
        suspiciousPatterns.forEach { pattern ->
            if (upperSQL.contains(pattern.toRegex())) {
                issues.add("检测到可疑的SQL模式: $pattern")
            }
        }
        
        // 检查SQL注入模式
        if (sql.contains("'") && (sql.contains("OR") || sql.contains("AND"))) {
            val suspiciousQuotePattern = "'.*(?:OR|AND).*'".toRegex(RegexOption.IGNORE_CASE)
            if (suspiciousQuotePattern.containsMatchIn(sql)) {
                issues.add("检测到可能的SQL注入模式")
            }
        }
        
        // 检查多语句
        if (sql.count { it == ';' } > 1) {
            issues.add("检测到多个SQL语句，可能存在安全风险")
        }
        
        return issues
    }
    
    /**
     * 检查SQL语法
     */
    private fun checkSQLSyntax(sql: String, databaseId: String): List<String> {
        val issues = mutableListOf<String>()
        
        try {
            connectionManager.getConnection(databaseId).use { connection ->
                // 使用EXPLAIN来检查语法（不实际执行）
                val explainSQL = "EXPLAIN $sql"
                connection.prepareStatement(explainSQL).use { statement ->
                    try {
                        statement.executeQuery().use { rs ->
                            // 如果能成功执行EXPLAIN，说明语法基本正确
                            logger.debug("SQL syntax validation passed for: {}", sql.take(50))
                        }
                    } catch (e: SQLException) {
                        issues.add("SQL语法错误: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to validate SQL syntax", e)
            issues.add("无法连接到数据库进行语法验证")
        }
        
        return issues
    }
    
    /**
     * 检查SQL性能
     */
    private fun checkSQLPerformance(sql: String): List<String> {
        val issues = mutableListOf<String>()
        val upperSQL = sql.uppercase()
        
        // 检查可能的性能问题
        if (upperSQL.contains("SELECT *")) {
            issues.add("建议避免使用SELECT *，明确指定需要的列")
        }
        
        if (upperSQL.contains("WHERE") && upperSQL.contains("LIKE '%")) {
            issues.add("LIKE查询以通配符开头可能影响性能")
        }
        
        if (!upperSQL.contains("LIMIT") && upperSQL.startsWith("SELECT")) {
            issues.add("建议在SELECT查询中使用LIMIT限制结果数量")
        }
        
        if (upperSQL.contains("ORDER BY") && !upperSQL.contains("LIMIT")) {
            issues.add("ORDER BY查询建议配合LIMIT使用")
        }
        
        // 检查子查询
        val subqueryCount = sql.count { it == '(' }
        if (subqueryCount > 3) {
            issues.add("复杂的嵌套子查询可能影响性能")
        }
        
        return issues
    }
    
    /**
     * 检查SQL结构
     */
    private fun checkSQLStructure(sql: String): List<String> {
        val issues = mutableListOf<String>()
        
        // 检查括号匹配
        val openParens = sql.count { it == '(' }
        val closeParens = sql.count { it == ')' }
        if (openParens != closeParens) {
            issues.add("括号不匹配")
        }
        
        // 检查引号匹配
        val singleQuotes = sql.count { it == '\'' }
        if (singleQuotes % 2 != 0) {
            issues.add("单引号不匹配")
        }
        
        val doubleQuotes = sql.count { it == '"' }
        if (doubleQuotes % 2 != 0) {
            issues.add("双引号不匹配")
        }
        
        // 检查是否以分号结尾
        if (!sql.trim().endsWith(";")) {
            issues.add("建议SQL语句以分号结尾")
        }
        
        return issues
    }
    
    /**
     * 计算验证置信度
     */
    private fun calculateValidationConfidence(errors: List<String>, warnings: List<String>): Double {
        if (errors.isNotEmpty()) {
            return 0.0
        }
        
        val warningPenalty = warnings.size * 0.1
        return maxOf(0.0, 1.0 - warningPenalty)
    }
    
    /**
     * 检测SQL类型
     */
    private fun detectSQLType(sql: String): String {
        val upperSQL = sql.uppercase().trim()
        return when {
            upperSQL.startsWith("SELECT") -> "SELECT"
            upperSQL.startsWith("INSERT") -> "INSERT"
            upperSQL.startsWith("UPDATE") -> "UPDATE"
            upperSQL.startsWith("DELETE") -> "DELETE"
            upperSQL.startsWith("CREATE") -> "CREATE"
            upperSQL.startsWith("DROP") -> "DROP"
            upperSQL.startsWith("ALTER") -> "ALTER"
            else -> "UNKNOWN"
        }
    }
    
    /**
     * 估算SQL复杂度
     */
    private fun estimateSQLComplexity(sql: String): String {
        val upperSQL = sql.uppercase()
        var complexity = 0
        
        // 基于各种SQL特性计算复杂度
        if (upperSQL.contains("JOIN")) complexity += 2
        if (upperSQL.contains("SUBQUERY") || sql.count { it == '(' } > 1) complexity += 2
        if (upperSQL.contains("GROUP BY")) complexity += 1
        if (upperSQL.contains("ORDER BY")) complexity += 1
        if (upperSQL.contains("HAVING")) complexity += 2
        if (upperSQL.contains("UNION")) complexity += 3
        if (upperSQL.contains("CASE")) complexity += 1
        
        return when {
            complexity <= 1 -> "LOW"
            complexity <= 4 -> "MEDIUM"
            else -> "HIGH"
        }
    }
}