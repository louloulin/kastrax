package com.kastrax.ai2db.connection.pool

import com.kastrax.ai2db.connection.model.ConnectionConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

/**
 * Micronaut版本的连接池管理器
 * 使用HikariCP作为底层连接池实现
 * 为每个数据库配置维护独立的连接池
 */
@Singleton
class ConnectionPool {
    private val logger = LoggerFactory.getLogger(ConnectionPool::class.java)
    
    // 数据源缓存
    private val dataSources = ConcurrentHashMap<String, HikariDataSource>()
    
    /**
     * 获取或创建数据源
     *
     * @param config 连接配置
     * @return 数据源
     */
    fun getDataSource(config: ConnectionConfig): DataSource {
        val key = generatePoolKey(config)
        
        return dataSources.computeIfAbsent(key) { _ ->
            createDataSource(config)
        }
    }
    
    /**
     * 创建HikariCP数据源
     */
    private fun createDataSource(config: ConnectionConfig): HikariDataSource {
        logger.info("创建连接池: {}", config.name)
        
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.toJdbcUrl()
            username = config.username
            password = config.password
            driverClassName = config.type.driverClassName
            
            // 连接池配置
            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = 30000 // 30秒
            idleTimeout = 600000 // 10分钟
            maxLifetime = 1800000 // 30分钟
            leakDetectionThreshold = 60000 // 1分钟
            
            // 连接池名称
            poolName = "KastraX-${config.type}-${config.name}"
            
            // 连接测试
            connectionTestQuery = when (config.type) {
                com.kastrax.ai2db.connection.model.DatabaseType.MYSQL -> "SELECT 1"
                com.kastrax.ai2db.connection.model.DatabaseType.POSTGRESQL -> "SELECT 1"
                com.kastrax.ai2db.connection.model.DatabaseType.SQLSERVER -> "SELECT 1"
                else -> "SELECT 1"
            }
            
            // 自定义属性
            config.properties.forEach { (key, value) ->
                addDataSourceProperty(key, value)
            }
            
            // SSL配置
            if (config.sslEnabled) {
                when (config.type) {
                    com.kastrax.ai2db.connection.model.DatabaseType.MYSQL -> {
                        addDataSourceProperty("useSSL", "true")
                        addDataSourceProperty("requireSSL", "true")
                    }
                    com.kastrax.ai2db.connection.model.DatabaseType.POSTGRESQL -> {
                        addDataSourceProperty("ssl", "true")
                        addDataSourceProperty("sslmode", "require")
                    }
                }
            }
            
            // 时区配置
            when (config.type) {
                com.kastrax.ai2db.connection.model.DatabaseType.MYSQL -> {
                    addDataSourceProperty("serverTimezone", config.timezone)
                }
                com.kastrax.ai2db.connection.model.DatabaseType.POSTGRESQL -> {
                    addDataSourceProperty("TimeZone", config.timezone)
                }
            }
        }
        
        return HikariDataSource(hikariConfig)
    }
    
    /**
     * 关闭指定配置的连接池
     *
     * @param config 连接配置
     */
    fun closePool(config: ConnectionConfig) {
        val key = generatePoolKey(config)
        dataSources.remove(key)?.let { dataSource ->
            logger.info("关闭连接池: {}", config.name)
            dataSource.close()
        }
    }
    
    /**
     * 关闭所有连接池
     */
    fun closeAllPools() {
        logger.info("关闭所有连接池")
        dataSources.values.forEach { dataSource ->
            try {
                dataSource.close()
            } catch (e: Exception) {
                logger.error("关闭连接池失败", e)
            }
        }
        dataSources.clear()
    }
    
    /**
     * 获取连接池统计信息
     *
     * @param config 连接配置
     * @return 连接池统计信息
     */
    fun getPoolStats(config: ConnectionConfig): Map<String, Any>? {
        val key = generatePoolKey(config)
        val dataSource = dataSources[key] ?: return null
        
        return mapOf(
            "poolName" to dataSource.poolName,
            "activeConnections" to dataSource.hikariPoolMXBean.activeConnections,
            "idleConnections" to dataSource.hikariPoolMXBean.idleConnections,
            "totalConnections" to dataSource.hikariPoolMXBean.totalConnections,
            "threadsAwaitingConnection" to dataSource.hikariPoolMXBean.threadsAwaitingConnection
        )
    }
    
    /**
     * 生成连接池键
     */
    private fun generatePoolKey(config: ConnectionConfig): String {
        return "${config.type}_${config.host}_${config.port}_${config.database}_${config.username}"
    }
}