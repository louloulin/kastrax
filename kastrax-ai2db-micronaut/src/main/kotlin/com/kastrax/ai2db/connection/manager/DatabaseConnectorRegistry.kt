package com.kastrax.ai2db.connection.manager

import com.kastrax.ai2db.connection.connector.DatabaseConnector
import com.kastrax.ai2db.connection.model.DatabaseType
import jakarta.inject.Singleton
import io.micronaut.context.ApplicationContext
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Micronaut版本的数据库连接器注册表
 * 负责管理和提供不同类型的数据库连接器
 * 使用Micronaut的依赖注入替代Spring的@Service
 */
@Singleton
class DatabaseConnectorRegistry(
    private val applicationContext: ApplicationContext
) {
    private val logger = LoggerFactory.getLogger(DatabaseConnectorRegistry::class.java)
    
    // 连接器缓存
    private val connectors = ConcurrentHashMap<DatabaseType, DatabaseConnector>()
    
    /**
     * 获取指定类型的数据库连接器
     *
     * @param type 数据库类型
     * @return 数据库连接器
     * @throws IllegalArgumentException 如果不支持该数据库类型
     */
    fun getConnector(type: DatabaseType): DatabaseConnector {
        return connectors.computeIfAbsent(type) { dbType ->
            createConnector(dbType)
        }
    }
    
    /**
     * 创建数据库连接器
     */
    private fun createConnector(type: DatabaseType): DatabaseConnector {
        logger.debug("创建数据库连接器: {}", type)
        
        return when (type) {
            DatabaseType.MYSQL -> {
                applicationContext.getBean(DatabaseConnector::class.java) { bean ->
                    bean.name.contains("MySQL", ignoreCase = true)
                }
            }
            DatabaseType.POSTGRESQL -> {
                applicationContext.getBean(DatabaseConnector::class.java) { bean ->
                    bean.name.contains("PostgreSQL", ignoreCase = true)
                }
            }
            DatabaseType.SQLSERVER -> {
                applicationContext.getBean(DatabaseConnector::class.java) { bean ->
                    bean.name.contains("SQLServer", ignoreCase = true)
                }
            }
            else -> {
                throw IllegalArgumentException("不支持的数据库类型: $type")
            }
        }
    }
    
    /**
     * 注册连接器
     *
     * @param type 数据库类型
     * @param connector 连接器实例
     */
    fun registerConnector(type: DatabaseType, connector: DatabaseConnector) {
        connectors[type] = connector
        logger.info("注册数据库连接器: {} -> {}", type, connector::class.simpleName)
    }
    
    /**
     * 获取所有支持的数据库类型
     *
     * @return 支持的数据库类型列表
     */
    fun getSupportedTypes(): List<DatabaseType> {
        return listOf(
            DatabaseType.MYSQL,
            DatabaseType.POSTGRESQL,
            DatabaseType.SQLSERVER
        )
    }
    
    /**
     * 检查是否支持指定的数据库类型
     *
     * @param type 数据库类型
     * @return 是否支持
     */
    fun isSupported(type: DatabaseType): Boolean {
        return getSupportedTypes().contains(type)
    }
}