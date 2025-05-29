package com.kastrax.ai2db.nl2sql.controller

import com.kastrax.ai2db.nl2sql.NL2SQLConverter
import com.kastrax.ai2db.nl2sql.model.*
import com.kastrax.ai2db.connection.service.ConnectionService
import com.kastrax.ai2db.schema.SchemaManager
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * NL2SQL REST控制器 - Micronaut版本
 * 
 * 提供自然语言转SQL的REST API接口
 */
@Controller("/api/v1/nl2sql")
@Secured(SecurityRule.IS_AUTHENTICATED)
class NL2SQLController {
    
    private val logger = LoggerFactory.getLogger(NL2SQLController::class.java)
    
    @Inject
    private lateinit var nl2sqlConverter: NL2SQLConverter
    
    @Inject
    private lateinit var connectionService: ConnectionService
    
    @Inject
    private lateinit var schemaManager: SchemaManager
    
    /**
     * 转换自然语言为SQL
     */
    @Post("/convert")
    suspend fun convertToSQL(@Body request: NL2SQLRequest): HttpResponse<NL2SQLResponse> {
        logger.info("Converting natural language query: {}", request.query)
        
        return try {
            // 验证连接
            val connection = connectionService.getConnection(request.connectionId)
                ?: return HttpResponse.badRequest(NL2SQLResponse(
                    success = false,
                    error = "Connection not found: ${request.connectionId}"
                ))
            
            // 转换查询
            val result = nl2sqlConverter.convertToSQL(
                naturalLanguageQuery = request.query,
                connection = connection,
                context = request.context
            )
            
            logger.info("Successfully converted query for connection: {}", request.connectionId)
            
            HttpResponse.ok(NL2SQLResponse(
                success = true,
                sqlQuery = result
            ))
            
        } catch (e: Exception) {
            logger.error("Failed to convert natural language query", e)
            HttpResponse.serverError(NL2SQLResponse(
                success = false,
                error = "Conversion failed: ${e.message}"
            ))
        }
    }
    
    /**
     * 执行SQL查询
     */
    @Post("/execute")
    suspend fun executeSQL(@Body request: SQLExecutionRequest): HttpResponse<SQLExecutionResponse> {
        logger.info("Executing SQL query for connection: {}", request.connectionId)
        
        return try {
            // 验证连接
            val connection = connectionService.getConnection(request.connectionId)
                ?: return HttpResponse.badRequest(SQLExecutionResponse(
                    success = false,
                    error = "Connection not found: ${request.connectionId}"
                ))
            
            // 执行查询
            val result = nl2sqlConverter.executeSQL(
                sqlQuery = request.sqlQuery,
                connection = connection,
                limit = request.limit
            )
            
            logger.info("Successfully executed SQL query, returned {} rows", result.data.size)
            
            HttpResponse.ok(SQLExecutionResponse(
                success = true,
                result = result
            ))
            
        } catch (e: Exception) {
            logger.error("Failed to execute SQL query", e)
            HttpResponse.serverError(SQLExecutionResponse(
                success = false,
                error = "Execution failed: ${e.message}"
            ))
        }
    }
    
    /**
     * 转换并执行查询（一步完成）
     */
    @Post("/convert-and-execute")
    suspend fun convertAndExecute(@Body request: NL2SQLExecutionRequest): HttpResponse<NL2SQLExecutionResponse> {
        logger.info("Converting and executing natural language query: {}", request.query)
        
        return try {
            // 验证连接
            val connection = connectionService.getConnection(request.connectionId)
                ?: return HttpResponse.badRequest(NL2SQLExecutionResponse(
                    success = false,
                    error = "Connection not found: ${request.connectionId}"
                ))
            
            // 转换查询
            val sqlQuery = nl2sqlConverter.convertToSQL(
                naturalLanguageQuery = request.query,
                connection = connection,
                context = request.context
            )
            
            // 执行查询
            val result = nl2sqlConverter.executeSQL(
                sqlQuery = sqlQuery,
                connection = connection,
                limit = request.limit
            )
            
            logger.info("Successfully converted and executed query, returned {} rows", result.data.size)
            
            HttpResponse.ok(NL2SQLExecutionResponse(
                success = true,
                sqlQuery = sqlQuery,
                result = result
            ))
            
        } catch (e: Exception) {
            logger.error("Failed to convert and execute natural language query", e)
            HttpResponse.serverError(NL2SQLExecutionResponse(
                success = false,
                error = "Conversion and execution failed: ${e.message}"
            ))
        }
    }
    
    /**
     * 验证SQL查询
     */
    @Post("/validate")
    suspend fun validateSQL(@Body request: SQLValidationRequest): HttpResponse<SQLValidationResponse> {
        logger.info("Validating SQL query for connection: {}", request.connectionId)
        
        return try {
            // 验证连接
            val connection = connectionService.getConnection(request.connectionId)
                ?: return HttpResponse.badRequest(SQLValidationResponse(
                    success = false,
                    error = "Connection not found: ${request.connectionId}"
                ))
            
            // 验证查询
            val validationResult = nl2sqlConverter.validateSQL(
                sqlQuery = request.sqlQuery,
                connection = connection
            )
            
            logger.info("SQL validation completed: valid={}", validationResult.isValid)
            
            HttpResponse.ok(SQLValidationResponse(
                success = true,
                validationResult = validationResult
            ))
            
        } catch (e: Exception) {
            logger.error("Failed to validate SQL query", e)
            HttpResponse.serverError(SQLValidationResponse(
                success = false,
                error = "Validation failed: ${e.message}"
            ))
        }
    }
    
    /**
     * 获取查询建议
     */
    @Post("/suggestions")
    suspend fun getSuggestions(@Body request: QuerySuggestionRequest): HttpResponse<QuerySuggestionResponse> {
        logger.info("Getting query suggestions for: {}", request.partialQuery)
        
        return try {
            // 验证连接
            val connection = connectionService.getConnection(request.connectionId)
                ?: return HttpResponse.badRequest(QuerySuggestionResponse(
                    success = false,
                    error = "Connection not found: ${request.connectionId}"
                ))
            
            // 获取数据库模式
            val schema = schemaManager.getSchema(connection)
            
            // 生成建议
            val suggestions = nl2sqlConverter.generateSuggestions(
                partialQuery = request.partialQuery,
                schema = schema,
                context = request.context
            )
            
            logger.info("Generated {} suggestions", suggestions.size)
            
            HttpResponse.ok(QuerySuggestionResponse(
                success = true,
                suggestions = suggestions
            ))
            
        } catch (e: Exception) {
            logger.error("Failed to generate query suggestions", e)
            HttpResponse.serverError(QuerySuggestionResponse(
                success = false,
                error = "Suggestion generation failed: ${e.message}"
            ))
        }
    }
    
    /**
     * 获取数据库模式信息
     */
    @Get("/schema/{connectionId}")
    suspend fun getSchema(@PathVariable connectionId: String): HttpResponse<SchemaResponse> {
        logger.info("Getting schema for connection: {}", connectionId)
        
        return try {
            // 验证连接
            val connection = connectionService.getConnection(connectionId)
                ?: return HttpResponse.badRequest(SchemaResponse(
                    success = false,
                    error = "Connection not found: $connectionId"
                ))
            
            // 获取模式
            val schema = schemaManager.getSchema(connection)
            
            logger.info("Retrieved schema with {} tables", schema.tables.size)
            
            HttpResponse.ok(SchemaResponse(
                success = true,
                schema = schema
            ))
            
        } catch (e: Exception) {
            logger.error("Failed to get schema", e)
            HttpResponse.serverError(SchemaResponse(
                success = false,
                error = "Schema retrieval failed: ${e.message}"
            ))
        }
    }
    
    /**
     * 刷新数据库模式缓存
     */
    @Post("/schema/{connectionId}/refresh")
    suspend fun refreshSchema(@PathVariable connectionId: String): HttpResponse<SchemaResponse> {
        logger.info("Refreshing schema cache for connection: {}", connectionId)
        
        return try {
            // 验证连接
            val connection = connectionService.getConnection(connectionId)
                ?: return HttpResponse.badRequest(SchemaResponse(
                    success = false,
                    error = "Connection not found: $connectionId"
                ))
            
            // 强制刷新模式
            val schema = schemaManager.getSchema(connection, forceRefresh = true)
            
            logger.info("Refreshed schema with {} tables", schema.tables.size)
            
            HttpResponse.ok(SchemaResponse(
                success = true,
                schema = schema
            ))
            
        } catch (e: Exception) {
            logger.error("Failed to refresh schema", e)
            HttpResponse.serverError(SchemaResponse(
                success = false,
                error = "Schema refresh failed: ${e.message}"
            ))
        }
    }
    
    /**
     * 获取查询历史
     */
    @Get("/history/{connectionId}")
    suspend fun getQueryHistory(
        @PathVariable connectionId: String,
        @QueryValue limit: Int = 50
    ): HttpResponse<QueryHistoryResponse> {
        logger.info("Getting query history for connection: {}", connectionId)
        
        return try {
            // 这里可以实现查询历史的获取逻辑
            // 暂时返回空列表
            val history = emptyList<SQLQuery>()
            
            HttpResponse.ok(QueryHistoryResponse(
                success = true,
                history = history
            ))
            
        } catch (e: Exception) {
            logger.error("Failed to get query history", e)
            HttpResponse.serverError(QueryHistoryResponse(
                success = false,
                error = "History retrieval failed: ${e.message}"
            ))
        }
    }
}

// 请求和响应数据类

data class NL2SQLRequest(
    val query: String,
    val connectionId: String,
    val context: ConversationContext? = null
)

data class NL2SQLResponse(
    val success: Boolean,
    val sqlQuery: SQLQuery? = null,
    val error: String? = null
)

data class SQLExecutionRequest(
    val sqlQuery: SQLQuery,
    val connectionId: String,
    val limit: Int = 1000
)

data class SQLExecutionResponse(
    val success: Boolean,
    val result: QueryExecutionResult? = null,
    val error: String? = null
)

data class NL2SQLExecutionRequest(
    val query: String,
    val connectionId: String,
    val context: ConversationContext? = null,
    val limit: Int = 1000
)

data class NL2SQLExecutionResponse(
    val success: Boolean,
    val sqlQuery: SQLQuery? = null,
    val result: QueryExecutionResult? = null,
    val error: String? = null
)

data class SQLValidationRequest(
    val sqlQuery: SQLQuery,
    val connectionId: String
)

data class SQLValidationResponse(
    val success: Boolean,
    val validationResult: QueryValidationResult? = null,
    val error: String? = null
)

data class QuerySuggestionRequest(
    val partialQuery: String,
    val connectionId: String,
    val context: ConversationContext? = null
)

data class QuerySuggestionResponse(
    val success: Boolean,
    val suggestions: List<QuerySuggestion> = emptyList(),
    val error: String? = null
)

data class SchemaResponse(
    val success: Boolean,
    val schema: com.kastrax.ai2db.schema.model.DatabaseSchema? = null,
    val error: String? = null
)

data class QueryHistoryResponse(
    val success: Boolean,
    val history: List<SQLQuery> = emptyList(),
    val error: String? = null
)