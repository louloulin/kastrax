package com.kastrax.ai2db.connection.repository

import com.kastrax.ai2db.connection.model.ConnectionConfig
import com.kastrax.ai2db.connection.model.DatabaseType
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.*

/**
 * 连接配置Repository接口，使用Micronaut Data JDBC
 * 从Spring Data JPA迁移到Micronaut Data JDBC以提升性能
 */
@Repository
@JdbcRepository(dialect = Dialect.POSTGRES)
interface ConnectionConfigRepository : CrudRepository<ConnectionConfig, String> {
    
    /**
     * 根据名称查找连接配置
     */
    fun findByName(name: String): Optional<ConnectionConfig>
    
    /**
     * 根据数据库类型查找连接配置
     */
    fun findByDatabaseType(databaseType: DatabaseType): List<ConnectionConfig>
    
    /**
     * 根据主机和端口查找连接配置
     */
    fun findByHostAndPort(host: String, port: Int): List<ConnectionConfig>
    
    /**
     * 根据用户ID查找连接配置
     */
    fun findByUserId(userId: String): List<ConnectionConfig>
    
    /**
     * 检查连接配置名称是否存在
     */
    fun existsByName(name: String): Boolean
    
    /**
     * 根据名称删除连接配置
     */
    fun deleteByName(name: String): Int
    
    /**
     * 根据用户ID删除连接配置
     */
    fun deleteByUserId(userId: String): Int
    
    /**
     * 查找活跃的连接配置
     */
    @Query("SELECT * FROM connection_configs WHERE is_active = true ORDER BY created_at DESC")
    fun findActiveConnections(): List<ConnectionConfig>
    
    /**
     * 根据数据库类型和用户ID查找连接配置
     */
    @Query("SELECT * FROM connection_configs WHERE database_type = :databaseType AND user_id = :userId ORDER BY name")
    fun findByDatabaseTypeAndUserId(databaseType: DatabaseType, userId: String): List<ConnectionConfig>
    
    /**
     * 查找最近使用的连接配置
     */
    @Query("SELECT * FROM connection_configs WHERE user_id = :userId ORDER BY last_used_at DESC NULLS LAST LIMIT :limit")
    fun findRecentlyUsed(userId: String, limit: Int = 10): List<ConnectionConfig>
    
    /**
     * 更新最后使用时间
     */
    @Query("UPDATE connection_configs SET last_used_at = NOW() WHERE id = :id")
    fun updateLastUsedTime(id: String): Int
    
    /**
     * 根据连接字符串模糊查找
     */
    @Query("SELECT * FROM connection_configs WHERE host ILIKE :pattern OR database_name ILIKE :pattern OR name ILIKE :pattern")
    fun findByPattern(pattern: String): List<ConnectionConfig>
    
    /**
     * 统计用户的连接配置数量
     */
    @Query("SELECT COUNT(*) FROM connection_configs WHERE user_id = :userId")
    fun countByUserId(userId: String): Long
    
    /**
     * 根据数据库类型统计连接配置数量
     */
    @Query("SELECT COUNT(*) FROM connection_configs WHERE database_type = :databaseType")
    fun countByDatabaseType(databaseType: DatabaseType): Long
    
    /**
     * 查找共享的连接配置
     */
    @Query("SELECT * FROM connection_configs WHERE is_shared = true AND is_active = true ORDER BY name")
    fun findSharedConnections(): List<ConnectionConfig>
    
    /**
     * 根据标签查找连接配置
     */
    @Query("SELECT * FROM connection_configs WHERE tags @> CAST(:tag AS jsonb) ORDER BY name")
    fun findByTag(tag: String): List<ConnectionConfig>
    
    /**
     * 批量更新连接状态
     */
    @Query("UPDATE connection_configs SET is_active = :isActive WHERE id = ANY(:ids)")
    fun updateActiveStatus(ids: List<String>, isActive: Boolean): Int
    
    /**
     * 查找过期的连接配置（超过指定天数未使用）
     */
    @Query("SELECT * FROM connection_configs WHERE last_used_at < NOW() - INTERVAL ':days days' OR last_used_at IS NULL")
    fun findExpiredConnections(days: Int = 30): List<ConnectionConfig>
    
    /**
     * 根据环境查找连接配置
     */
    @Query("SELECT * FROM connection_configs WHERE environment = :environment ORDER BY name")
    fun findByEnvironment(environment: String): List<ConnectionConfig>
    
    /**
     * 查找需要SSL的连接配置
     */
    @Query("SELECT * FROM connection_configs WHERE ssl_enabled = true ORDER BY name")
    fun findSslEnabledConnections(): List<ConnectionConfig>
    
    /**
     * 根据创建时间范围查找连接配置
     */
    @Query("SELECT * FROM connection_configs WHERE created_at BETWEEN :startDate AND :endDate ORDER BY created_at DESC")
    fun findByCreatedAtBetween(startDate: Date, endDate: Date): List<ConnectionConfig>
    
    /**
     * 查找具有自定义属性的连接配置
     */
    @Query("SELECT * FROM connection_configs WHERE properties IS NOT NULL AND properties != '{}' ORDER BY name")
    fun findWithCustomProperties(): List<ConnectionConfig>
    
    /**
     * 根据连接池配置查找
     */
    @Query("SELECT * FROM connection_configs WHERE max_pool_size >= :minPoolSize ORDER BY max_pool_size DESC")
    fun findByMinPoolSize(minPoolSize: Int): List<ConnectionConfig>
}