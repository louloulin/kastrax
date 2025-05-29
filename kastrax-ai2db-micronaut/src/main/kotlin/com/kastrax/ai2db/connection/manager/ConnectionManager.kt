package com.kastrax.ai2db.connection.manager

import com.kastrax.ai2db.connection.connector.DatabaseConnector
import com.kastrax.ai2db.connection.model.*
import com.kastrax.ai2db.connection.pool.ConnectionPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Micronaut版本的连接管理器
 * 负责创建、缓存和管理数据库连接
 * 迁移自Spring Boot，使用Jakarta注解替代Spring注解
 */
@Singleton
class ConnectionManager(
    private val connectorRegistry: DatabaseConnectorRegistry,
    private val connectionPool: ConnectionPool
) {
    private val logger = LoggerFactory.getLogger(ConnectionManager::class.java)
    
    // 活跃连接映射
    private val activeConnections = ConcurrentHashMap<String, Connection>()
    
    // 配置到连接ID的映射，用于复用
    private val configToConnectionMap = ConcurrentHashMap<String, String>()
    
    /**
     * 创建新的数据库连接或返回现有连接
     *
     * @param config 连接配置
     * @param forceNew 是否强制创建新连接
     * @return 连接对象
     */
    suspend fun createConnection(config: ConnectionConfig, forceNew: Boolean = false): Connection = withContext(Dispatchers.IO) {
        val configHash = generateConfigHash(config)
        
        // 检查是否有现有连接
        if (!forceNew && configToConnectionMap.containsKey(configHash)) {
            val connectionId = configToConnectionMap[configHash]
            val existingConnection = activeConnections[connectionId]
            
            if (existingConnection != null && existingConnection.isActive()) {
                logger.debug("复用现有连接: {}", connectionId)
                return@withContext existingConnection.markAsUsed()
            }
        }
        
        // 创建新连接
        val connector = connectorRegistry.getConnector(config.type)
        val connection = connector.connect(config)
        
        // 缓存连接
        activeConnections[connection.id] = connection
        configToConnectionMap[configHash] = connection.id
        
        logger.info("创建新连接: {} for {}", connection.id, config.name)
        return@withContext connection
    }
    
    /**
     * 关闭连接
     *
     * @param connectionId 连接ID
     * @return 是否成功关闭
     */
    suspend fun closeConnection(connectionId: String): Boolean = withContext(Dispatchers.IO) {
        val connection = activeConnections[connectionId]
        if (connection != null) {
            try {
                val connector = connectorRegistry.getConnector(connection.config.type)
                val success = connector.disconnect(connection)
                
                if (success) {
                    activeConnections.remove(connectionId)
                    // 从配置映射中移除
                    val configHash = generateConfigHash(connection.config)
                    configToConnectionMap.remove(configHash)
                    logger.info("连接已关闭: {}", connectionId)
                }
                
                return@withContext success
            } catch (e: Exception) {
                logger.error("关闭连接失败: {}", connectionId, e)
                return@withContext false
            }
        }
        return@withContext false
    }
    
    /**
     * 测试连接
     *
     * @param config 连接配置
     * @return 连接状态
     */
    suspend fun testConnection(config: ConnectionConfig): ConnectionStatus = withContext(Dispatchers.IO) {
        try {
            val connector = connectorRegistry.getConnector(config.type)
            return@withContext connector.testConnection(config)
        } catch (e: Exception) {
            logger.error("测试连接失败", e)
            return@withContext ConnectionStatus.FAILED
        }
    }
    
    /**
     * 获取连接
     *
     * @param connectionId 连接ID
     * @return 连接对象或null
     */
    fun getConnection(connectionId: String): Connection? {
        return activeConnections[connectionId]
    }
    
    /**
     * 获取所有活跃连接
     *
     * @return 活跃连接列表
     */
    fun getActiveConnections(): List<Connection> {
        return activeConnections.values.toList()
    }
    
    /**
     * 清理过期连接
     */
    suspend fun cleanupExpiredConnections() = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val expiredConnections = activeConnections.values.filter { connection ->
            connection.lastUsedAt?.let { lastUsed ->
                now.minusSeconds(3600).isAfter(lastUsed) // 1小时超时
            } ?: false
        }
        
        expiredConnections.forEach { connection ->
            logger.info("清理过期连接: {}", connection.id)
            closeConnection(connection.id)
        }
    }
    
    /**
     * 生成配置哈希
     */
    private fun generateConfigHash(config: ConnectionConfig): String {
        return "${config.type}_${config.host}_${config.port}_${config.database}_${config.username}"
    }
}