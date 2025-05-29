package com.kastrax.ai2db.nl2sql.model

import java.time.Instant
import java.util.*

/**
 * SQL查询结果
 */
data class SQLQuery(
    val sql: String,
    val originalQuery: String,
    val confidence: Double,
    val error: String? = null,
    val timestamp: Instant = Instant.now(),
    val id: String = UUID.randomUUID().toString(),
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * 检查查询是否成功
     */
    fun isSuccessful(): Boolean = error == null && sql.isNotBlank()
    
    /**
     * 获取查询类型
     */
    fun getQueryType(): QueryType {
        val upperSQL = sql.uppercase().trim()
        return when {
            upperSQL.startsWith("SELECT") -> QueryType.SELECT
            upperSQL.startsWith("INSERT") -> QueryType.INSERT
            upperSQL.startsWith("UPDATE") -> QueryType.UPDATE
            upperSQL.startsWith("DELETE") -> QueryType.DELETE
            upperSQL.startsWith("CREATE") -> QueryType.CREATE
            upperSQL.startsWith("DROP") -> QueryType.DROP
            upperSQL.startsWith("ALTER") -> QueryType.ALTER
            else -> QueryType.UNKNOWN
        }
    }
}

/**
 * 查询类型枚举
 */
enum class QueryType {
    SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, UNKNOWN
}

/**
 * 对话上下文
 */
data class ConversationContext(
    val sessionId: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val previousQueries: List<SQLQuery> = emptyList(),
    val currentSchema: String? = null,
    val preferences: UserPreferences = UserPreferences(),
    val timestamp: Instant = Instant.now()
) {
    /**
     * 添加查询到历史记录
     */
    fun addQuery(query: SQLQuery): ConversationContext {
        val updatedQueries = (previousQueries + query).takeLast(10) // 保留最近10个查询
        return copy(previousQueries = updatedQueries)
    }
    
    /**
     * 获取最近的成功查询
     */
    fun getRecentSuccessfulQueries(): List<SQLQuery> {
        return previousQueries.filter { it.isSuccessful() }.takeLast(5)
    }
}

/**
 * 用户偏好设置
 */
data class UserPreferences(
    val preferredDateFormat: String = "YYYY-MM-DD",
    val preferredTimeZone: String = "UTC",
    val maxResultRows: Int = 1000,
    val enableQueryOptimization: Boolean = true,
    val enableQueryExplanation: Boolean = false,
    val language: String = "en"
)

/**
 * 查询执行结果
 */
data class QueryExecutionResult(
    val sqlQuery: SQLQuery,
    val success: Boolean,
    val resultSet: List<Map<String, Any?>> = emptyList(),
    val rowCount: Int = 0,
    val executionTime: Long = 0, // 毫秒
    val error: String? = null,
    val warnings: List<String> = emptyList(),
    val timestamp: Instant = Instant.now()
) {
    /**
     * 获取结果摘要
     */
    fun getSummary(): String {
        return if (success) {
            "Query executed successfully. Returned $rowCount rows in ${executionTime}ms."
        } else {
            "Query failed: ${error ?: "Unknown error"}"
        }
    }
}

/**
 * 查询统计信息
 */
data class QueryStats(
    val totalQueries: Int = 0,
    val successfulQueries: Int = 0,
    val failedQueries: Int = 0,
    val averageConfidence: Double = 0.0,
    val averageExecutionTime: Long = 0,
    val queryTypeDistribution: Map<QueryType, Int> = emptyMap(),
    val timestamp: Instant = Instant.now()
) {
    /**
     * 计算成功率
     */
    fun getSuccessRate(): Double {
        return if (totalQueries > 0) {
            successfulQueries.toDouble() / totalQueries.toDouble()
        } else {
            0.0
        }
    }
}

/**
 * 查询建议
 */
data class QuerySuggestion(
    val originalQuery: String,
    val suggestedSQL: String,
    val explanation: String,
    val confidence: Double,
    val improvements: List<String> = emptyList(),
    val timestamp: Instant = Instant.now()
)

/**
 * 查询验证结果
 */
data class QueryValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList(),
    val warnings: List<ValidationWarning> = emptyList(),
    val suggestions: List<String> = emptyList()
)

/**
 * 验证错误
 */
data class ValidationError(
    val type: ErrorType,
    val message: String,
    val position: Int? = null,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
)

/**
 * 验证警告
 */
data class ValidationWarning(
    val type: WarningType,
    val message: String,
    val suggestion: String? = null
)

/**
 * 错误类型
 */
enum class ErrorType {
    SYNTAX_ERROR,
    SEMANTIC_ERROR,
    PERMISSION_ERROR,
    RESOURCE_ERROR,
    UNKNOWN_TABLE,
    UNKNOWN_COLUMN,
    TYPE_MISMATCH,
    CONSTRAINT_VIOLATION
}

/**
 * 错误严重程度
 */
enum class ErrorSeverity {
    INFO, WARNING, ERROR, CRITICAL
}

/**
 * 警告类型
 */
enum class WarningType {
    PERFORMANCE_WARNING,
    DEPRECATED_SYNTAX,
    POTENTIAL_ISSUE,
    OPTIMIZATION_SUGGESTION,
    SECURITY_WARNING
}