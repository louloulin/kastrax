package com.kastrax.ai2db.connection.service

import com.kastrax.ai2db.connection.entity.ConnectionConfigEntity
import com.kastrax.ai2db.connection.manager.ConnectionManager
import com.kastrax.ai2db.connection.model.*
import com.kastrax.ai2db.connection.repository.ConnectionConfigRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

/**
 * 连接服务类，提供数据库连接的业务逻辑
 * 从Spring Service迁移到Micronaut Singleton
 */
@Singleton
class ConnectionService @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val connectionConfigRepository: ConnectionConfigRepository
) {
    
    private val logger = LoggerFactory.getLogger(ConnectionService::class.java)
    
    /**
     * 创建新的连接配置
     */
    suspend fun createConnectionConfig(config: ConnectionConfig): ConnectionConfig = withContext(Dispatchers.IO) {
        try {
            // 检查名称是否已存在
            if (connectionConfigRepository.existsByName(config.name)) {
                throw ConnectionException("连接配置名称 '${config.name}' 已存在")
            }
            
            // 测试连接
            val testResult = testConnection(config)
            if (testResult.status != ConnectionStatus.CONNECTED) {
                throw ConnectionException("连接测试失败: ${testResult.message}")
            }
            
            // 保存配置
            val entity = ConnectionConfigEntity.fromDomainModel(config.copy(
                id = UUID.randomUUID().toString(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                lastTestAt = Instant.now(),
                lastTestResult = "SUCCESS"
            ))
            
            val savedEntity = connectionConfigRepository.save(entity)
            logger.info("创建连接配置成功: {}", savedEntity.name)
            
            return@withContext savedEntity.toDomainModel()
        } catch (e: Exception) {
            logger.error("创建连接配置失败: {}", config.name, e)
            throw e
        }
    }
    
    /**
     * 更新连接配置
     */
    suspend fun updateConnectionConfig(config: ConnectionConfig): ConnectionConfig = withContext(Dispatchers.IO) {
        try {
            val existingEntity = connectionConfigRepository.findById(config.id)
                .orElseThrow { ConnectionException("连接配置不存在: ${config.id}") }
            
            // 如果名称发生变化，检查新名称是否已存在
            if (existingEntity.name != config.name && connectionConfigRepository.existsByName(config.name)) {
                throw ConnectionException("连接配置名称 '${config.name}' 已存在")
            }
            
            // 测试连接
            val testResult = testConnection(config)
            if (testResult.status != ConnectionStatus.CONNECTED) {
                throw ConnectionException("连接测试失败: ${testResult.message}")
            }
            
            // 更新配置
            val updatedEntity = ConnectionConfigEntity.fromDomainModel(config.copy(
                updatedAt = Instant.now(),
                lastTestAt = Instant.now(),
                lastTestResult = "SUCCESS",
                version = existingEntity.version
            ))
            
            val savedEntity = connectionConfigRepository.update(updatedEntity)
            logger.info("更新连接配置成功: {}", savedEntity.name)
            
            return@withContext savedEntity.toDomainModel()
        } catch (e: Exception) {
            logger.error("更新连接配置失败: {}", config.id, e)
            throw e
        }
    }
    
    /**
     * 删除连接配置
     */
    suspend fun deleteConnectionConfig(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val entity = connectionConfigRepository.findById(id)
                .orElseThrow { ConnectionException("连接配置不存在: $id") }
            
            // 断开现有连接
            connectionManager.disconnectByConfigId(id)
            
            // 删除配置
            connectionConfigRepository.deleteById(id)
            logger.info("删除连接配置成功: {}", entity.name)
            
            return@withContext true
        } catch (e: Exception) {
            logger.error("删除连接配置失败: {}", id, e)
            throw e
        }
    }
    
    /**
     * 根据ID获取连接配置
     */
    suspend fun getConnectionConfig(id: String): ConnectionConfig? = withContext(Dispatchers.IO) {
        return@withContext connectionConfigRepository.findById(id)
            .map { it.toDomainModel() }
            .orElse(null)
    }
    
    /**
     * 根据名称获取连接配置
     */
    suspend fun getConnectionConfigByName(name: String): ConnectionConfig? = withContext(Dispatchers.IO) {
        return@withContext connectionConfigRepository.findByName(name)
            .map { it.toDomainModel() }
            .orElse(null)
    }
    
    /**
     * 获取用户的所有连接配置
     */
    suspend fun getUserConnectionConfigs(userId: String): List<ConnectionConfig> = withContext(Dispatchers.IO) {
        return@withContext connectionConfigRepository.findByUserId(userId)
            .map { it.toDomainModel() }
    }
    
    /**
     * 获取活跃的连接配置
     */
    suspend fun getActiveConnectionConfigs(): List<ConnectionConfig> = withContext(Dispatchers.IO) {
        return@withContext connectionConfigRepository.findActiveConnections()
            .map { it.toDomainModel() }
    }
    
    /**
     * 根据数据库类型获取连接配置
     */
    suspend fun getConnectionConfigsByType(databaseType: DatabaseType): List<ConnectionConfig> = withContext(Dispatchers.IO) {
        return@withContext connectionConfigRepository.findByDatabaseType(databaseType)
            .map { it.toDomainModel() }
    }
    
    /**
     * 获取最近使用的连接配置
     */
    suspend fun getRecentlyUsedConfigs(userId: String, limit: Int = 10): List<ConnectionConfig> = withContext(Dispatchers.IO) {
        return@withContext connectionConfigRepository.findRecentlyUsed(userId, limit)
            .map { it.toDomainModel() }
    }
    
    /**
     * 测试连接配置
     */
    suspend fun testConnection(config: ConnectionConfig): ConnectionTestResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val startTime = System.currentTimeMillis()
            val status = connectionManager.testConnection(config)
            val endTime = System.currentTimeMillis()
            
            // 更新测试结果
            if (config.id.isNotEmpty()) {
                updateConnectionTestResult(config.id, status, null)
            }
            
            ConnectionTestResult(
                status = status,
                message = if (status == ConnectionStatus.CONNECTED) "连接成功" else "连接失败",
                responseTime = endTime - startTime,
                testedAt = Instant.now()
            )
        } catch (e: Exception) {
            logger.error("测试连接失败: {}", config.name, e)
            
            // 更新测试结果
            if (config.id.isNotEmpty()) {
                updateConnectionTestResult(config.id, ConnectionStatus.FAILED, e.message)
            }
            
            ConnectionTestResult(
                status = ConnectionStatus.FAILED,
                message = e.message ?: "连接测试失败",
                responseTime = 0,
                testedAt = Instant.now(),
                error = e
            )
        }
    }
    
    /**
     * 建立数据库连接
     */
    suspend fun connect(configId: String): Connection {
        val config = getConnectionConfig(configId)
            ?: throw ConnectionException("连接配置不存在: $configId")
        
        val connection = connectionManager.connect(config)
        
        // 更新最后使用时间
        connectionConfigRepository.updateLastUsedTime(configId)
        
        return connection
    }
    
    /**
     * 断开数据库连接
     */
    suspend fun disconnect(connectionId: String): Boolean {
        return connectionManager.disconnect(connectionId)
    }
    
    /**
     * 获取连接
     */
    suspend fun getConnection(connectionId: String): Connection? {
        return connectionManager.getConnection(connectionId)
    }
    
    /**
     * 获取所有活跃连接
     */
    suspend fun getActiveConnections(): List<Connection> {
        return connectionManager.getActiveConnections()
    }
    
    /**
     * 获取数据库元数据
     */
    suspend fun getDatabaseMetadata(connectionId: String): DatabaseMetadata {
        return connectionManager.getMetadata(connectionId)
    }
    
    /**
     * 执行查询
     */
    suspend fun executeQuery(
        connectionId: String,
        query: String,
        parameters: List<Any> = emptyList(),
        timeout: Long = 30000
    ): QueryResult {
        return connectionManager.executeQuery(connectionId, query, parameters, timeout)
    }
    
    /**
     * 执行更新
     */
    suspend fun executeUpdate(
        connectionId: String,
        query: String,
        parameters: List<Any> = emptyList()
    ): UpdateResult {
        return connectionManager.executeUpdate(connectionId, query, parameters)
    }
    
    /**
     * 开始事务
     */
    suspend fun beginTransaction(connectionId: String): Transaction {
        return connectionManager.beginTransaction(connectionId)
    }
    
    /**
     * 提交事务
     */
    suspend fun commitTransaction(transactionId: String) {
        connectionManager.commitTransaction(transactionId)
    }
    
    /**
     * 回滚事务
     */
    suspend fun rollbackTransaction(transactionId: String) {
        connectionManager.rollbackTransaction(transactionId)
    }
    
    /**
     * 搜索连接配置
     */
    suspend fun searchConnectionConfigs(pattern: String): List<ConnectionConfig> = withContext(Dispatchers.IO) {
        return@withContext connectionConfigRepository.findByPattern("%$pattern%")
            .map { it.toDomainModel() }
    }
    
    /**
     * 根据标签获取连接配置
     */
    suspend fun getConnectionConfigsByTag(tag: String): List<ConnectionConfig> = withContext(Dispatchers.IO) {
        return@withContext connectionConfigRepository.findByTag(tag)
            .map { it.toDomainModel() }
    }
    
    /**
     * 获取共享连接配置
     */
    suspend fun getSharedConnectionConfigs(): List<ConnectionConfig> = withContext(Dispatchers.IO) {
        return@withContext connectionConfigRepository.findSharedConnections()
            .map { it.toDomainModel() }
    }
    
    /**
     * 批量更新连接状态
     */
    suspend fun updateConnectionStatus(ids: List<String>, isActive: Boolean): Int = withContext(Dispatchers.IO) {
        return@withContext connectionConfigRepository.updateActiveStatus(ids, isActive)
    }
    
    /**
     * 获取连接统计信息
     */
    suspend fun getConnectionStatistics(userId: String? = null): ConnectionStatistics = withContext(Dispatchers.IO) {
        val totalConnections = if (userId != null) {
            connectionConfigRepository.countByUserId(userId)
        } else {
            connectionConfigRepository.count()
        }
        
        val activeConnections = connectionManager.getActiveConnections().size
        val connectionsByType = DatabaseType.values().associateWith { type ->
            connectionConfigRepository.countByDatabaseType(type)
        }
        
        return@withContext ConnectionStatistics(
            totalConfigurations = totalConnections,
            activeConnections = activeConnections.toLong(),
            connectionsByType = connectionsByType,
            lastUpdated = Instant.now()
        )
    }
    
    /**
     * 清理过期连接
     */
    suspend fun cleanupExpiredConnections(days: Int = 30): Int = withContext(Dispatchers.IO) {
        val expiredConfigs = connectionConfigRepository.findExpiredConnections(days)
        var cleanedCount = 0
        
        expiredConfigs.forEach { config ->
            try {
                connectionManager.disconnectByConfigId(config.id)
                connectionConfigRepository.deleteById(config.id)
                cleanedCount++
                logger.info("清理过期连接配置: {}", config.name)
            } catch (e: Exception) {
                logger.error("清理连接配置失败: {}", config.name, e)
            }
        }
        
        return@withContext cleanedCount
    }
    
    /**
     * 更新连接测试结果
     */
    private suspend fun updateConnectionTestResult(
        configId: String,
        status: ConnectionStatus,
        errorMessage: String?
    ) = withContext(Dispatchers.IO) {
        try {
            val entity = connectionConfigRepository.findById(configId).orElse(null)
            if (entity != null) {
                val updatedEntity = entity.copy(
                    lastTestAt = Instant.now(),
                    lastTestResult = if (status == ConnectionStatus.CONNECTED) "SUCCESS" else "FAILED: ${errorMessage ?: "Unknown error"}",
                    updatedAt = Instant.now()
                )
                connectionConfigRepository.update(updatedEntity)
            }
        } catch (e: Exception) {
            logger.error("更新连接测试结果失败: {}", configId, e)
        }
    }
}

/**
 * 连接测试结果
 */
data class ConnectionTestResult(
    val status: ConnectionStatus,
    val message: String,
    val responseTime: Long,
    val testedAt: Instant,
    val error: Throwable? = null
)

/**
 * 连接统计信息
 */
data class ConnectionStatistics(
    val totalConfigurations: Long,
    val activeConnections: Long,
    val connectionsByType: Map<DatabaseType, Long>,
    val lastUpdated: Instant
)