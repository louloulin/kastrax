package com.kastrax.ai2db.connection.controller

import com.kastrax.ai2db.connection.model.*
import com.kastrax.ai2db.connection.service.ConnectionService
import com.kastrax.ai2db.connection.service.ConnectionStatistics
import com.kastrax.ai2db.connection.service.ConnectionTestResult
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.annotation.Error
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.security.Principal
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

/**
 * 连接管理REST控制器
 * 从Spring Web Controller迁移到Micronaut HTTP Controller
 */
@Controller("/api/v1/connections")
@Secured(SecurityRule.IS_AUTHENTICATED)
@Validated
@ExecuteOn(TaskExecutors.IO)
class ConnectionController @Inject constructor(
    private val connectionService: ConnectionService
) {
    
    private val logger = LoggerFactory.getLogger(ConnectionController::class.java)
    
    /**
     * 创建连接配置
     */
    @Post
    suspend fun createConnection(
        @Body @Valid request: CreateConnectionRequest,
        principal: Principal
    ): HttpResponse<ConnectionConfigResponse> {
        return try {
            val config = request.toConnectionConfig(principal.name)
            val createdConfig = connectionService.createConnectionConfig(config)
            HttpResponse.created(ConnectionConfigResponse.fromDomainModel(createdConfig))
        } catch (e: ConnectionException) {
            logger.error("创建连接配置失败", e)
            HttpResponse.badRequest()
        } catch (e: Exception) {
            logger.error("创建连接配置时发生未知错误", e)
            HttpResponse.serverError()
        }
    }
    
    /**
     * 更新连接配置
     */
    @Put("/{id}")
    suspend fun updateConnection(
        @PathVariable @NotBlank id: String,
        @Body @Valid request: UpdateConnectionRequest,
        principal: Principal
    ): HttpResponse<ConnectionConfigResponse> {
        return try {
            val config = request.toConnectionConfig(id, principal.name)
            val updatedConfig = connectionService.updateConnectionConfig(config)
            HttpResponse.ok(ConnectionConfigResponse.fromDomainModel(updatedConfig))
        } catch (e: ConnectionException) {
            logger.error("更新连接配置失败: {}", id, e)
            HttpResponse.badRequest()
        } catch (e: Exception) {
            logger.error("更新连接配置时发生未知错误: {}", id, e)
            HttpResponse.serverError()
        }
    }
    
    /**
     * 删除连接配置
     */
    @Delete("/{id}")
    suspend fun deleteConnection(
        @PathVariable @NotBlank id: String
    ): HttpResponse<Void> {
        return try {
            val deleted = connectionService.deleteConnectionConfig(id)
            if (deleted) {
                HttpResponse.noContent()
            } else {
                HttpResponse.notFound()
            }
        } catch (e: ConnectionException) {
            logger.error("删除连接配置失败: {}", id, e)
            HttpResponse.badRequest()
        } catch (e: Exception) {
            logger.error("删除连接配置时发生未知错误: {}", id, e)
            HttpResponse.serverError()
        }
    }
    
    /**
     * 获取连接配置详情
     */
    @Get("/{id}")
    suspend fun getConnection(
        @PathVariable @NotBlank id: String
    ): HttpResponse<ConnectionConfigResponse> {
        return try {
            val config = connectionService.getConnectionConfig(id)
            if (config != null) {
                HttpResponse.ok(ConnectionConfigResponse.fromDomainModel(config))
            } else {
                HttpResponse.notFound()
            }
        } catch (e: Exception) {
            logger.error("获取连接配置失败: {}", id, e)
            HttpResponse.serverError()
        }
    }
    
    /**
     * 获取用户的连接配置列表
     */
    @Get
    suspend fun getUserConnections(
        principal: Principal,
        @QueryValue("type") databaseType: DatabaseType?,
        @QueryValue("active") active: Boolean?,
        @QueryValue("limit") @Positive limit: Int?
    ): HttpResponse<List<ConnectionConfigResponse>> {
        return try {
            val configs = when {
                databaseType != null -> connectionService.getConnectionConfigsByType(databaseType)
                active == true -> connectionService.getActiveConnectionConfigs()
                limit != null -> connectionService.getRecentlyUsedConfigs(principal.name, limit)
                else -> connectionService.getUserConnectionConfigs(principal.name)
            }
            
            val responses = configs.map { ConnectionConfigResponse.fromDomainModel(it) }
            HttpResponse.ok(responses)
        } catch (e: Exception) {
            logger.error("获取连接配置列表失败", e)
            HttpResponse.serverError()
        }
    }
    
    /**
     * 测试连接
     */
    @Post("/{id}/test")
    suspend fun testConnection(
        @PathVariable @NotBlank id: String
    ): HttpResponse<ConnectionTestResponse> {
        return try {
            val config = connectionService.getConnectionConfig(id)
                ?: return HttpResponse.notFound()
            
            val testResult = connectionService.testConnection(config)
            HttpResponse.ok(ConnectionTestResponse.fromTestResult(testResult))
        } catch (e: Exception) {
            logger.error("测试连接失败: {}", id, e)
            HttpResponse.serverError()
        }
    }
    
    /**
     * 建立连接
     */
    @Post("/{id}/connect")
    suspend fun connect(
        @PathVariable @NotBlank id: String
    ): HttpResponse<ConnectionResponse> {
        return try {
            val connection = connectionService.connect(id)
            HttpResponse.ok(ConnectionResponse.fromDomainModel(connection))
        } catch (e: ConnectionException) {
            logger.error("建立连接失败: {}", id, e)
            HttpResponse.badRequest()
        } catch (e: Exception) {
            logger.error("建立连接时发生未知错误: {}", id, e)
            HttpResponse.serverError()
        }
    }
    
    /**
     * 断开连接
     */
    @Post("/active/{connectionId}/disconnect")
    suspend fun disconnect(
        @PathVariable @NotBlank connectionId: String
    ): HttpResponse<Void> {
        return try {
            val disconnected = connectionService.disconnect(connectionId)
            if (disconnected) {
                HttpResponse.noContent()
            } else {
                HttpResponse.notFound()
            }
        } catch (e: Exception) {
            logger.error("断开连接失败: {}", connectionId, e)
            HttpResponse.serverError()
        }
    }
    
    /**
     * 获取活跃连接列表
     */
    @Get("/active")
    suspend fun getActiveConnections(): HttpResponse<List<ConnectionResponse>> {
        return try {
            val connections = connectionService.getActiveConnections()
            val responses = connections.map { ConnectionResponse.fromDomainModel(it) }
            HttpResponse.ok(responses)
        } catch (e: Exception) {
            logger.error("获取活跃连接列表失败", e)
            HttpResponse.serverError()
        }
    }
    
    /**
     * 获取数据库元数据
     */
    @Get("/active/{connectionId}/metadata")
    suspend fun getDatabaseMetadata(
        @PathVariable @NotBlank connectionId: String
    ): HttpResponse<DatabaseMetadataResponse> {
        return try {
            val metadata = connectionService.getDatabaseMetadata(connectionId)
            HttpResponse.ok(DatabaseMetadataResponse.fromDomainModel(metadata))
        } catch (e: ConnectionException) {
            logger.error("获取数据库元数据失败: {}", connectionId, e)
            HttpResponse.badRequest()
        } catch (e: Exception) {
            logger.error("获取数据库元数据时发生未知错误: {}", connectionId, e)
            HttpResponse.serverError()
        }
    }
    
    /**
     * 执行查询
     */
    @Post("/active/{connectionId}/query")
    suspend fun executeQuery(
        @PathVariable @NotBlank connectionId: String,
        @Body @Valid request: QueryRequest
    ): HttpResponse<QueryResultResponse> {
        return try {
            val result = connectionService.executeQuery(
                connectionId = connectionId,
                query = request.query,
                parameters = request.parameters,
                timeout = request.timeout
            )
            HttpResponse.ok(QueryResultResponse.fromDomainModel(result))
        } catch (e: QueryException) {
            logger.error("执行查询失败: {}", connectionId, e)
            HttpResponse.badRequest()
        } catch (e: Exception) {
            logger.error("执行查询时发生未知错误: {}", connectionId, e)
            HttpResponse.serverError()
        }
    }
    
    /**
     * 执行更新
     */
    @Post("/active/{connectionId}/update")
    suspend fun executeUpdate(
        @PathVariable @NotBlank connectionId: String,
        @Body @Valid request: UpdateRequest
    ): HttpResponse<UpdateResultResponse> {
        return try {
            val result = connectionService.executeUpdate(
                connectionId = connectionId,
                query = request.query,
                parameters = request.parameters
            )
            HttpResponse.ok(UpdateResultResponse.fromDomainModel(result))
        } catch (e: QueryException) {
            logger.error("执行更新失败: {}", connectionId, e)
            HttpResponse.badRequest()
        } catch (e: Exception) {
            logger.error("执行更新时发生未知错误: {}", connectionId, e)
            HttpResponse.serverError()
        }
    }
    
    /**
     * 开始事务
     */
    @Post("/active/{connectionId}/transaction/begin")
    suspend fun beginTransaction(
        @PathVariable @NotBlank connectionId: String
    ): HttpResponse<TransactionResponse> {
        return try {
            val transaction = connectionService.beginTransaction(connectionId)
            HttpResponse.ok(TransactionResponse.fromDomainModel(transaction))
        } catch (e: TransactionException) {
            logger.error("开始事务失败: {}", connectionId, e)
            HttpResponse.badRequest()
        } catch (e: Exception) {
            logger.error("开始事务时发生未知错误: {}", connectionId, e)
            HttpResponse.serverError()
        }
    }
    
    /**
     * 提交事务
     */
    @Post("/transactions/{transactionId}/commit")
    suspend fun commitTransaction(
        @PathVariable @NotBlank transactionId: String
    ): HttpResponse<Void> {
        return try {
            connectionService.commitTransaction(transactionId)
            HttpResponse.noContent()
        } catch (e: TransactionException) {
            logger.error("提交事务失败: {}", transactionId, e)
            HttpResponse.badRequest()
        } catch (e: Exception) {
            logger.error("提交事务时发生未知错误: {}", transactionId, e)
            HttpResponse.serverError()
        }
    }
    
    /**
     * 回滚事务
     */
    @Post("/transactions/{transactionId}/rollback")
    suspend fun rollbackTransaction(
        @PathVariable @NotBlank transactionId: String
    ): HttpResponse<Void> {
        return try {
            connectionService.rollbackTransaction(transactionId)
            HttpResponse.noContent()
        } catch (e: TransactionException) {
            logger.error("回滚事务失败: {}", transactionId, e)
            HttpResponse.badRequest()
        } catch (e: Exception) {
            logger.error("回滚事务时发生未知错误: {}", transactionId, e)
            HttpResponse.serverError()
        }
    }
    
    /**
     * 搜索连接配置
     */
    @Get("/search")
    suspend fun searchConnections(
        @QueryValue("q") @NotBlank query: String
    ): HttpResponse<List<ConnectionConfigResponse>> {
        return try {
            val configs = connectionService.searchConnectionConfigs(query)
            val responses = configs.map { ConnectionConfigResponse.fromDomainModel(it) }
            HttpResponse.ok(responses)
        } catch (e: Exception) {
            logger.error("搜索连接配置失败", e)
            HttpResponse.serverError()
        }
    }
    
    /**
     * 获取连接统计信息
     */
    @Get("/statistics")
    suspend fun getStatistics(
        principal: Principal
    ): HttpResponse<ConnectionStatisticsResponse> {
        return try {
            val statistics = connectionService.getConnectionStatistics(principal.name)
            HttpResponse.ok(ConnectionStatisticsResponse.fromDomainModel(statistics))
        } catch (e: Exception) {
            logger.error("获取连接统计信息失败", e)
            HttpResponse.serverError()
        }
    }
    
    /**
     * 批量更新连接状态
     */
    @Put("/batch/status")
    suspend fun updateConnectionStatus(
        @Body @Valid request: BatchUpdateStatusRequest
    ): HttpResponse<BatchUpdateResponse> {
        return try {
            val updatedCount = connectionService.updateConnectionStatus(request.ids, request.isActive)
            HttpResponse.ok(BatchUpdateResponse(updatedCount))
        } catch (e: Exception) {
            logger.error("批量更新连接状态失败", e)
            HttpResponse.serverError()
        }
    }
    
    /**
     * 清理过期连接
     */
    @Delete("/cleanup")
    @Secured("ROLE_ADMIN")
    suspend fun cleanupExpiredConnections(
        @QueryValue("days") days: Int = 30
    ): HttpResponse<CleanupResponse> {
        return try {
            val cleanedCount = connectionService.cleanupExpiredConnections(days)
            HttpResponse.ok(CleanupResponse(cleanedCount))
        } catch (e: Exception) {
            logger.error("清理过期连接失败", e)
            HttpResponse.serverError()
        }
    }
    
    /**
     * 全局异常处理
     */
    @Error(global = true)
    fun handleConnectionException(request: io.micronaut.http.HttpRequest<*>, ex: ConnectionException): HttpResponse<ErrorResponse> {
        logger.error("连接异常: {}", ex.message, ex)
        return HttpResponse.badRequest(ErrorResponse("CONNECTION_ERROR", ex.message ?: "连接错误"))
    }
    
    @Error(global = true)
    fun handleQueryException(request: io.micronaut.http.HttpRequest<*>, ex: QueryException): HttpResponse<ErrorResponse> {
        logger.error("查询异常: {}", ex.message, ex)
        return HttpResponse.badRequest(ErrorResponse("QUERY_ERROR", ex.message ?: "查询错误"))
    }
    
    @Error(global = true)
    fun handleTransactionException(request: io.micronaut.http.HttpRequest<*>, ex: TransactionException): HttpResponse<ErrorResponse> {
        logger.error("事务异常: {}", ex.message, ex)
        return HttpResponse.badRequest(ErrorResponse("TRANSACTION_ERROR", ex.message ?: "事务错误"))
    }
}

/**
 * 错误响应
 */
data class ErrorResponse(
    val code: String,
    val message: String
)

/**
 * 批量更新响应
 */
data class BatchUpdateResponse(
    val updatedCount: Int
)

/**
 * 清理响应
 */
data class CleanupResponse(
    val cleanedCount: Int
)