package com.kastrax.ai2db.nl2sql.controller

import com.kastrax.ai2db.nl2sql.agent.NL2SQLAgent
import com.kastrax.ai2db.nl2sql.agent.SQLConversionResult
import com.kastrax.ai2db.nl2sql.model.ConversationContext
import ai.kastrax.core.agent.AgentGenerateOptions
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.slf4j.LoggerFactory
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * NL2SQL REST API控制器
 */
@RestController
@RequestMapping("/api/v1/nl2sql")
@CrossOrigin(origins = ["*"])
class NL2SQLController(
    private val nl2sqlAgent: NL2SQLAgent
) {
    
    private val logger = LoggerFactory.getLogger(NL2SQLController::class.java)
    
    /**
     * 转换自然语言到SQL
     */
    @PostMapping("/convert")
    suspend fun convertToSQL(
        @Valid @RequestBody request: NL2SQLRequest
    ): ResponseEntity<NL2SQLResponse> {
        logger.info("Converting natural language to SQL: {}", request.query.take(100))
        
        return try {
            val result = nl2sqlAgent.convertToSQL(
                query = request.query,
                databaseId = request.databaseId,
                threadId = request.threadId
            )
            
            val response = NL2SQLResponse(
                success = result.isSuccess,
                sql = result.sql,
                confidence = result.confidence,
                explanation = result.explanation,
                error = result.error,
                metadata = mapOf(
                    "databaseId" to request.databaseId,
                    "threadId" to (request.threadId ?: "default"),
                    "timestamp" to System.currentTimeMillis()
                )
            )
            
            if (result.isSuccess) {
                ResponseEntity.ok(response)
            } else {
                ResponseEntity.badRequest().body(response)
            }
        } catch (e: Exception) {
            logger.error("Error converting natural language to SQL", e)
            ResponseEntity.internalServerError().body(
                NL2SQLResponse(
                    success = false,
                    sql = null,
                    confidence = 0.0,
                    explanation = "",
                    error = "Internal server error: ${e.message}"
                )
            )
        }
    }
    
    /**
     * 批量转换
     */
    @PostMapping("/batch-convert")
    suspend fun batchConvertToSQL(
        @Valid @RequestBody request: BatchNL2SQLRequest
    ): ResponseEntity<BatchNL2SQLResponse> {
        logger.info("Batch converting {} queries to SQL", request.queries.size)
        
        return try {
            val results = request.queries.map { queryRequest ->
                try {
                    val result = nl2sqlAgent.convertToSQL(
                        query = queryRequest.query,
                        databaseId = queryRequest.databaseId,
                        threadId = queryRequest.threadId
                    )
                    
                    NL2SQLResponse(
                        success = result.isSuccess,
                        sql = result.sql,
                        confidence = result.confidence,
                        explanation = result.explanation,
                        error = result.error,
                        metadata = mapOf(
                            "databaseId" to queryRequest.databaseId,
                            "threadId" to (queryRequest.threadId ?: "default")
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Error in batch conversion for query: {}", queryRequest.query.take(50), e)
                    NL2SQLResponse(
                        success = false,
                        sql = null,
                        confidence = 0.0,
                        explanation = "",
                        error = e.message ?: "Unknown error"
                    )
                }
            }
            
            val response = BatchNL2SQLResponse(
                results = results,
                totalCount = results.size,
                successCount = results.count { it.success },
                failureCount = results.count { !it.success }
            )
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error in batch conversion", e)
            ResponseEntity.internalServerError().body(
                BatchNL2SQLResponse(
                    results = emptyList(),
                    totalCount = 0,
                    successCount = 0,
                    failureCount = request.queries.size,
                    error = "Internal server error: ${e.message}"
                )
            )
        }
    }
    
    /**
     * 获取对话历史
     */
    @GetMapping("/conversation/{threadId}")
    suspend fun getConversationHistory(
        @PathVariable threadId: String,
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<ConversationHistoryResponse> {
        logger.info("Getting conversation history for thread: {}", threadId)
        
        return try {
            // 这里需要从Memory中获取对话历史
            // 暂时返回空的响应
            val response = ConversationHistoryResponse(
                threadId = threadId,
                messages = emptyList(),
                totalCount = 0
            )
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error getting conversation history", e)
            ResponseEntity.internalServerError().body(
                ConversationHistoryResponse(
                    threadId = threadId,
                    messages = emptyList(),
                    totalCount = 0,
                    error = "Internal server error: ${e.message}"
                )
            )
        }
    }
    
    /**
     * 清除对话历史
     */
    @DeleteMapping("/conversation/{threadId}")
    suspend fun clearConversationHistory(
        @PathVariable threadId: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Clearing conversation history for thread: {}", threadId)
        
        return try {
            // 这里需要清除Memory中的对话历史
            // 暂时返回成功响应
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Conversation history cleared for thread: $threadId",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Error clearing conversation history", e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to "Internal server error: ${e.message}"
                )
            )
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "service" to "NL2SQL",
                "version" to nl2sqlAgent.version,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
}

/**
 * NL2SQL请求
 */
data class NL2SQLRequest(
    @field:NotBlank(message = "Query cannot be blank")
    @field:Size(max = 1000, message = "Query cannot exceed 1000 characters")
    val query: String,
    
    @field:NotBlank(message = "Database ID cannot be blank")
    val databaseId: String,
    
    val threadId: String? = null,
    val context: ConversationContext? = null,
    val options: Map<String, Any>? = null
)

/**
 * 批量NL2SQL请求
 */
data class BatchNL2SQLRequest(
    @field:Size(min = 1, max = 10, message = "Queries count must be between 1 and 10")
    val queries: List<NL2SQLRequest>
)

/**
 * NL2SQL响应
 */
data class NL2SQLResponse(
    val success: Boolean,
    val sql: String?,
    val confidence: Double,
    val explanation: String,
    val error: String? = null,
    val metadata: Map<String, Any>? = null
)

/**
 * 批量NL2SQL响应
 */
data class BatchNL2SQLResponse(
    val results: List<NL2SQLResponse>,
    val totalCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val error: String? = null
)

/**
 * 对话历史响应
 */
data class ConversationHistoryResponse(
    val threadId: String,
    val messages: List<ConversationMessage>,
    val totalCount: Int,
    val error: String? = null
)

/**
 * 对话消息
 */
data class ConversationMessage(
    val role: String,
    val content: String,
    val timestamp: Long,
    val metadata: Map<String, Any>? = null
)