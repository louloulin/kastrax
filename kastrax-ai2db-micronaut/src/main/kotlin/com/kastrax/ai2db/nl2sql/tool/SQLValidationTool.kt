package com.kastrax.ai2db.nl2sql.tool

import ai.kastrax.core.tool.ZodTool
import ai.kastrax.zod.*
import com.kastrax.ai2db.schema.model.DatabaseSchema
import com.kastrax.ai2db.connection.manager.ConnectionManager
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.util.regex.Pattern

/**
 * SQL验证输入
 */
data class SQLValidationInput(
    val sql: String,
    val schema: String? = null,
    val database: String? = null,
    val strictMode: Boolean = false
)

/**
 * SQL验证输出
 */
data class SQLValidationOutput(
    val isValid: Boolean,
    val errors: List<ValidationError>,
    val warnings: List<ValidationWarning>,
    val suggestions: List<String>,
    val securityIssues: List<SecurityIssue>,
    val performanceHints: List<PerformanceHint>
)

/**
 * 验证错误
 */
data class ValidationError(
    val type: String,
    val message: String,
    val line: Int? = null,
    val column: Int? = null,
    val severity: String = "ERROR"
)

/**
 * 验证警告
 */
data class ValidationWarning(
    val type: String,
    val message: String,
    val suggestion: String? = null
)

/**
 * 安全问题
 */
data class SecurityIssue(
    val type: String,
    val description: String,
    val risk: String,
    val recommendation: String
)

/**
 * 性能提示
 */
data class PerformanceHint(
    val type: String,
    val description: String,
    val impact: String,
    val suggestion: String
)

/**
 * SQL验证工具
 * 
 * 验证生成的SQL语句的语法和语义正确性
 */
@Singleton
class SQLValidationTool(
    private val connectionManager: ConnectionManager
) : ZodTool<SQLValidationInput, SQLValidationOutput> {
    
    private val logger = LoggerFactory.getLogger(SQLValidationTool::class.java)
    
    override val id = "sql-validation"
    override val name = "SQL验证"
    override val description = "验证生成的SQL语句的语法和语义正确性"
    
    override val inputSchema = objectInput {
        "sql" to stringField("要验证的SQL语句")
        "schema" to stringField("数据库模式名称", required = false)
        "database" to stringField("数据库连接ID", required = false)
        "strictMode" to booleanField("是否启用严格模式", required = false)
    }
    
    override val outputSchema = objectOutput {
        "isValid" to booleanField("SQL是否有效")
        "errors" to arrayField("错误列表") {
            objectField {
                "type" to stringField("错误类型")
                "message" to stringField("错误消息")
                "line" to numberField("行号", required = false)
                "column" to numberField("列号", required = false)
                "severity" to stringField("严重程度")
            }
        }
        "warnings" to arrayField("警告列表") {
            objectField {
                "type" to stringField("警告类型")
                "message" to stringField("警告消息")
                "suggestion" to stringField("建议", required = false)
            }
        }
        "suggestions" to arrayField("优化建议") { stringField() }
        "securityIssues" to arrayField("安全问题") {
            objectField {
                "type" to stringField("问题类型")
                "description" to stringField("问题描述")
                "risk" to stringField("风险等级")
                "recommendation" to stringField("建议")
            }
        }
        "performanceHints" to arrayField("性能提示") {
            objectField {
                "type" to stringField("提示类型")
                "description" to stringField("描述")
                "impact" to stringField("影响")
                "suggestion" to stringField("建议")
            }
        }
    }
    
    override suspend fun execute(input: SQLValidationInput): SQLValidationOutput = withContext(Dispatchers.IO) {
        logger.info("Validating SQL: {}", input.sql.take(100))
        
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<ValidationWarning>()
        val suggestions = mutableListOf<String>()
        val securityIssues = mutableListOf<SecurityIssue>()
        val performanceHints = mutableListOf<PerformanceHint>()
        
        try {
            // 1. 基础语法检查
            performBasicSyntaxCheck(input.sql, errors, warnings)
            
            // 2. 安全检查
            performSecurityCheck(input.sql, securityIssues)
            
            // 3. 性能检查
            performPerformanceCheck(input.sql, performanceHints)
            
            // 4. 数据库特定验证
            if (!input.database.isNullOrBlank()) {
                performDatabaseValidation(input, errors, warnings)
            }
            
            // 5. 生成建议
            generateSuggestions(input.sql, suggestions)
            
            val isValid = errors.isEmpty() && securityIssues.none { it.risk == "HIGH" }
            
            logger.info("SQL validation completed: valid={}, errors={}, warnings={}", 
                isValid, errors.size, warnings.size)
            
            return@withContext SQLValidationOutput(
                isValid = isValid,
                errors = errors,
                warnings = warnings,
                suggestions = suggestions,
                securityIssues = securityIssues,
                performanceHints = performanceHints
            )
            
        } catch (e: Exception) {
            logger.error("Error during SQL validation", e)
            
            errors.add(ValidationError(
                type = "VALIDATION_ERROR",
                message = "验证过程中发生错误: ${e.message}",
                severity = "ERROR"
            ))
            
            return@withContext SQLValidationOutput(
                isValid = false,
                errors = errors,
                warnings = warnings,
                suggestions = suggestions,
                securityIssues = securityIssues,
                performanceHints = performanceHints
            )
        }
    }
    
    /**
     * 基础语法检查
     */
    private fun performBasicSyntaxCheck(
        sql: String,
        errors: MutableList<ValidationError>,
        warnings: MutableList<ValidationWarning>
    ) {
        val trimmedSQL = sql.trim()
        
        // 检查SQL是否为空
        if (trimmedSQL.isBlank()) {
            errors.add(ValidationError(
                type = "EMPTY_SQL",
                message = "SQL语句不能为空"
            ))
            return
        }
        
        // 检查基本SQL关键字
        val upperSQL = trimmedSQL.uppercase()
        val validStartKeywords = listOf("SELECT", "INSERT", "UPDATE", "DELETE", "WITH", "EXPLAIN")
        
        if (!validStartKeywords.any { upperSQL.startsWith(it) }) {
            errors.add(ValidationError(
                type = "INVALID_START_KEYWORD",
                message = "SQL必须以有效的关键字开始: ${validStartKeywords.joinToString(", ")}"
            ))
        }
        
        // 检查括号匹配
        checkParenthesesBalance(sql, errors)
        
        // 检查引号匹配
        checkQuotesBalance(sql, errors)
        
        // 检查基本语法结构
        checkBasicSQLStructure(upperSQL, errors, warnings)
    }
    
    /**
     * 安全检查
     */
    private fun performSecurityCheck(
        sql: String,
        securityIssues: MutableList<SecurityIssue>
    ) {
        val upperSQL = sql.uppercase()
        
        // 检查危险操作
        val dangerousOperations = mapOf(
            "DROP" to "删除操作",
            "TRUNCATE" to "清空表操作",
            "ALTER" to "修改结构操作",
            "CREATE" to "创建操作",
            "GRANT" to "权限授予操作",
            "REVOKE" to "权限撤销操作"
        )
        
        dangerousOperations.forEach { (keyword, description) ->
            if (upperSQL.contains(keyword)) {
                securityIssues.add(SecurityIssue(
                    type = "DANGEROUS_OPERATION",
                    description = "检测到${description}: $keyword",
                    risk = "HIGH",
                    recommendation = "请确认此操作的必要性和安全性"
                ))
            }
        }
        
        // 检查SQL注入风险
        checkSQLInjectionRisk(sql, securityIssues)
        
        // 检查敏感信息泄露
        checkSensitiveDataExposure(sql, securityIssues)
    }
    
    /**
     * 性能检查
     */
    private fun performPerformanceCheck(
        sql: String,
        performanceHints: MutableList<PerformanceHint>
    ) {
        val upperSQL = sql.uppercase()
        
        // 检查SELECT *
        if (upperSQL.contains("SELECT *")) {
            performanceHints.add(PerformanceHint(
                type = "SELECT_ALL_COLUMNS",
                description = "使用了SELECT *查询所有列",
                impact = "可能影响查询性能和网络传输",
                suggestion = "建议明确指定需要的列名"
            ))
        }
        
        // 检查缺少WHERE条件的UPDATE/DELETE
        if ((upperSQL.contains("UPDATE") || upperSQL.contains("DELETE")) && !upperSQL.contains("WHERE")) {
            performanceHints.add(PerformanceHint(
                type = "MISSING_WHERE_CLAUSE",
                description = "UPDATE/DELETE语句缺少WHERE条件",
                impact = "可能影响所有行，造成性能问题",
                suggestion = "建议添加适当的WHERE条件"
            ))
        }
        
        // 检查ORDER BY without LIMIT
        if (upperSQL.contains("ORDER BY") && !upperSQL.contains("LIMIT")) {
            performanceHints.add(PerformanceHint(
                type = "ORDER_WITHOUT_LIMIT",
                description = "使用ORDER BY但没有LIMIT",
                impact = "可能需要排序大量数据",
                suggestion = "考虑添加LIMIT限制结果数量"
            ))
        }
        
        // 检查复杂的子查询
        val subqueryCount = upperSQL.split("SELECT").size - 1
        if (subqueryCount > 3) {
            performanceHints.add(PerformanceHint(
                type = "COMPLEX_SUBQUERY",
                description = "查询包含多个子查询",
                impact = "可能影响查询性能",
                suggestion = "考虑使用JOIN或临时表优化"
            ))
        }
    }
    
    /**
     * 数据库特定验证
     */
    private suspend fun performDatabaseValidation(
        input: SQLValidationInput,
        errors: MutableList<ValidationError>,
        warnings: MutableList<ValidationWarning>
    ) {
        try {
            val connection = connectionManager.getConnection(input.database!!)
            if (connection != null) {
                // 使用EXPLAIN检查SQL
                val explainSQL = "EXPLAIN ${input.sql}"
                val statement = connection.jdbcConnection.prepareStatement(explainSQL)
                
                try {
                    statement.executeQuery()
                    // 如果EXPLAIN成功，说明SQL语法正确
                } catch (e: SQLException) {
                    errors.add(ValidationError(
                        type = "SYNTAX_ERROR",
                        message = "SQL语法错误: ${e.message}",
                        severity = "ERROR"
                    ))
                } finally {
                    statement.close()
                }
            }
        } catch (e: Exception) {
            logger.warn("Database validation failed", e)
            warnings.add(ValidationWarning(
                type = "DATABASE_VALIDATION_FAILED",
                message = "无法连接到数据库进行验证: ${e.message}"
            ))
        }
    }
    
    /**
     * 生成优化建议
     */
    private fun generateSuggestions(
        sql: String,
        suggestions: MutableList<String>
    ) {
        val upperSQL = sql.uppercase()
        
        // 基于SQL内容生成建议
        if (upperSQL.contains("JOIN")) {
            suggestions.add("使用JOIN时建议在关联字段上创建索引")
        }
        
        if (upperSQL.contains("GROUP BY")) {
            suggestions.add("GROUP BY查询建议在分组字段上创建索引")
        }
        
        if (upperSQL.contains("ORDER BY")) {
            suggestions.add("ORDER BY查询建议在排序字段上创建索引")
        }
        
        if (upperSQL.contains("LIKE '%")) {
            suggestions.add("避免使用前置通配符的LIKE查询，考虑使用全文搜索")
        }
        
        // 添加通用建议
        suggestions.add("建议在执行前测试SQL性能")
        suggestions.add("考虑使用参数化查询防止SQL注入")
    }
    
    /**
     * 检查括号平衡
     */
    private fun checkParenthesesBalance(
        sql: String,
        errors: MutableList<ValidationError>
    ) {
        var balance = 0
        var line = 1
        var column = 1
        
        for (char in sql) {
            when (char) {
                '(' -> balance++
                ')' -> {
                    balance--
                    if (balance < 0) {
                        errors.add(ValidationError(
                            type = "UNMATCHED_PARENTHESIS",
                            message = "多余的右括号",
                            line = line,
                            column = column
                        ))
                        return
                    }
                }
                '\n' -> {
                    line++
                    column = 1
                    continue
                }
            }
            column++
        }
        
        if (balance > 0) {
            errors.add(ValidationError(
                type = "UNMATCHED_PARENTHESIS",
                message = "缺少 $balance 个右括号"
            ))
        }
    }
    
    /**
     * 检查引号平衡
     */
    private fun checkQuotesBalance(
        sql: String,
        errors: MutableList<ValidationError>
    ) {
        var inSingleQuote = false
        var inDoubleQuote = false
        var i = 0
        
        while (i < sql.length) {
            val char = sql[i]
            
            when (char) {
                '\'' -> {
                    if (!inDoubleQuote) {
                        // 检查是否是转义的单引号
                        if (i + 1 < sql.length && sql[i + 1] == '\'') {
                            i++ // 跳过转义的引号
                        } else {
                            inSingleQuote = !inSingleQuote
                        }
                    }
                }
                '"' -> {
                    if (!inSingleQuote) {
                        inDoubleQuote = !inDoubleQuote
                    }
                }
            }
            i++
        }
        
        if (inSingleQuote) {
            errors.add(ValidationError(
                type = "UNMATCHED_QUOTE",
                message = "缺少匹配的单引号"
            ))
        }
        
        if (inDoubleQuote) {
            errors.add(ValidationError(
                type = "UNMATCHED_QUOTE",
                message = "缺少匹配的双引号"
            ))
        }
    }
    
    /**
     * 检查基本SQL结构
     */
    private fun checkBasicSQLStructure(
        upperSQL: String,
        errors: MutableList<ValidationError>,
        warnings: MutableList<ValidationWarning>
    ) {
        // 检查SELECT语句结构
        if (upperSQL.startsWith("SELECT")) {
            if (!upperSQL.contains("FROM") && !upperSQL.contains("DUAL")) {
                warnings.add(ValidationWarning(
                    type = "MISSING_FROM_CLAUSE",
                    message = "SELECT语句通常需要FROM子句"
                ))
            }
        }
        
        // 检查INSERT语句结构
        if (upperSQL.startsWith("INSERT")) {
            if (!upperSQL.contains("INTO")) {
                errors.add(ValidationError(
                    type = "MISSING_INTO_CLAUSE",
                    message = "INSERT语句缺少INTO子句"
                ))
            }
        }
        
        // 检查UPDATE语句结构
        if (upperSQL.startsWith("UPDATE")) {
            if (!upperSQL.contains("SET")) {
                errors.add(ValidationError(
                    type = "MISSING_SET_CLAUSE",
                    message = "UPDATE语句缺少SET子句"
                ))
            }
        }
    }
    
    /**
     * 检查SQL注入风险
     */
    private fun checkSQLInjectionRisk(
        sql: String,
        securityIssues: MutableList<SecurityIssue>
    ) {
        // 检查可疑的字符串拼接模式
        val suspiciousPatterns = listOf(
            "'.*\+.*'",
            "\".*\+.*\"",
            "'.*\|\|.*'",
            "UNION.*SELECT",
            "';.*--",
            "'.*OR.*1=1"
        )
        
        suspiciousPatterns.forEach { pattern ->
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(sql).find()) {
                securityIssues.add(SecurityIssue(
                    type = "SQL_INJECTION_RISK",
                    description = "检测到可能的SQL注入模式",
                    risk = "HIGH",
                    recommendation = "使用参数化查询或预编译语句"
                ))
            }
        }
    }
    
    /**
     * 检查敏感数据暴露
     */
    private fun checkSensitiveDataExposure(
        sql: String,
        securityIssues: MutableList<SecurityIssue>
    ) {
        val upperSQL = sql.uppercase()
        val sensitiveKeywords = listOf("PASSWORD", "TOKEN", "SECRET", "KEY", "CREDENTIAL")
        
        sensitiveKeywords.forEach { keyword ->
            if (upperSQL.contains(keyword)) {
                securityIssues.add(SecurityIssue(
                    type = "SENSITIVE_DATA_EXPOSURE",
                    description = "SQL中可能包含敏感信息: $keyword",
                    risk = "MEDIUM",
                    recommendation = "避免在SQL中直接使用敏感信息"
                ))
            }
        }
    }
}