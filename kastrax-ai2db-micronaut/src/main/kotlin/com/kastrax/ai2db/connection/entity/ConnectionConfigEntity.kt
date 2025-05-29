package com.kastrax.ai2db.connection.entity

import com.kastrax.ai2db.connection.model.DatabaseType
import io.micronaut.data.annotation.*
import io.micronaut.data.model.naming.NamingStrategies
import io.micronaut.serde.annotation.Serdeable
import java.time.Instant
import java.util.*

/**
 * 连接配置实体类，使用Micronaut Data注解
 * 从JPA实体迁移到Micronaut Data实体以提升性能
 */
@Serdeable
@MappedEntity(value = "connection_configs", namingStrategy = NamingStrategies.UnderScoreSeparatedLowerCase::class)
data class ConnectionConfigEntity(
    
    @field:Id
    @field:GeneratedValue
    val id: String = UUID.randomUUID().toString(),
    
    @field:Column(name = "name", nullable = false, unique = true)
    val name: String,
    
    @field:Column(name = "description")
    val description: String? = null,
    
    @field:Column(name = "database_type", nullable = false)
    val databaseType: DatabaseType,
    
    @field:Column(name = "host", nullable = false)
    val host: String,
    
    @field:Column(name = "port", nullable = false)
    val port: Int,
    
    @field:Column(name = "database_name", nullable = false)
    val databaseName: String,
    
    @field:Column(name = "username", nullable = false)
    val username: String,
    
    @field:Column(name = "password", nullable = false)
    val password: String,
    
    @field:Column(name = "schema_name")
    val schemaName: String? = null,
    
    @field:Column(name = "ssl_enabled", nullable = false)
    val sslEnabled: Boolean = false,
    
    @field:Column(name = "ssl_mode")
    val sslMode: String? = null,
    
    @field:Column(name = "ssl_cert_path")
    val sslCertPath: String? = null,
    
    @field:Column(name = "ssl_key_path")
    val sslKeyPath: String? = null,
    
    @field:Column(name = "ssl_ca_path")
    val sslCaPath: String? = null,
    
    @field:Column(name = "connection_timeout", nullable = false)
    val connectionTimeout: Int = 30000,
    
    @field:Column(name = "socket_timeout", nullable = false)
    val socketTimeout: Int = 60000,
    
    @field:Column(name = "max_pool_size", nullable = false)
    val maxPoolSize: Int = 10,
    
    @field:Column(name = "min_pool_size", nullable = false)
    val minPoolSize: Int = 1,
    
    @field:Column(name = "max_idle_time", nullable = false)
    val maxIdleTime: Long = 600000, // 10分钟
    
    @field:Column(name = "validation_query")
    val validationQuery: String? = null,
    
    @field:Column(name = "test_on_borrow", nullable = false)
    val testOnBorrow: Boolean = true,
    
    @field:Column(name = "test_while_idle", nullable = false)
    val testWhileIdle: Boolean = true,
    
    @field:Column(name = "properties", columnDefinition = "jsonb")
    val properties: Map<String, String> = emptyMap(),
    
    @field:Column(name = "tags", columnDefinition = "jsonb")
    val tags: List<String> = emptyList(),
    
    @field:Column(name = "environment")
    val environment: String? = null,
    
    @field:Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,
    
    @field:Column(name = "is_shared", nullable = false)
    val isShared: Boolean = false,
    
    @field:Column(name = "user_id", nullable = false)
    val userId: String,
    
    @field:Column(name = "created_by")
    val createdBy: String? = null,
    
    @field:Column(name = "updated_by")
    val updatedBy: String? = null,
    
    @field:DateCreated
    @field:Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @field:DateUpdated
    @field:Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
    
    @field:Column(name = "last_used_at")
    val lastUsedAt: Instant? = null,
    
    @field:Column(name = "last_test_at")
    val lastTestAt: Instant? = null,
    
    @field:Column(name = "last_test_result")
    val lastTestResult: String? = null,
    
    @field:Version
    @field:Column(name = "version", nullable = false)
    val version: Long = 0
) {
    
    /**
     * 转换为领域模型
     */
    fun toDomainModel(): com.kastrax.ai2db.connection.model.ConnectionConfig {
        return com.kastrax.ai2db.connection.model.ConnectionConfig(
            id = this.id,
            name = this.name,
            description = this.description,
            databaseType = this.databaseType,
            host = this.host,
            port = this.port,
            database = this.databaseName,
            username = this.username,
            password = this.password,
            schema = this.schemaName,
            sslEnabled = this.sslEnabled,
            sslMode = this.sslMode,
            sslCertPath = this.sslCertPath,
            sslKeyPath = this.sslKeyPath,
            sslCaPath = this.sslCaPath,
            connectionTimeout = this.connectionTimeout,
            socketTimeout = this.socketTimeout,
            maxPoolSize = this.maxPoolSize,
            minPoolSize = this.minPoolSize,
            maxIdleTime = this.maxIdleTime,
            validationQuery = this.validationQuery,
            testOnBorrow = this.testOnBorrow,
            testWhileIdle = this.testWhileIdle,
            properties = this.properties,
            tags = this.tags,
            environment = this.environment,
            isActive = this.isActive,
            isShared = this.isShared,
            userId = this.userId,
            createdBy = this.createdBy,
            updatedBy = this.updatedBy,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            lastUsedAt = this.lastUsedAt,
            lastTestAt = this.lastTestAt,
            lastTestResult = this.lastTestResult
        )
    }
    
    companion object {
        /**
         * 从领域模型创建实体
         */
        fun fromDomainModel(config: com.kastrax.ai2db.connection.model.ConnectionConfig): ConnectionConfigEntity {
            return ConnectionConfigEntity(
                id = config.id,
                name = config.name,
                description = config.description,
                databaseType = config.databaseType,
                host = config.host,
                port = config.port,
                databaseName = config.database,
                username = config.username,
                password = config.password,
                schemaName = config.schema,
                sslEnabled = config.sslEnabled,
                sslMode = config.sslMode,
                sslCertPath = config.sslCertPath,
                sslKeyPath = config.sslKeyPath,
                sslCaPath = config.sslCaPath,
                connectionTimeout = config.connectionTimeout,
                socketTimeout = config.socketTimeout,
                maxPoolSize = config.maxPoolSize,
                minPoolSize = config.minPoolSize,
                maxIdleTime = config.maxIdleTime,
                validationQuery = config.validationQuery,
                testOnBorrow = config.testOnBorrow,
                testWhileIdle = config.testWhileIdle,
                properties = config.properties,
                tags = config.tags,
                environment = config.environment,
                isActive = config.isActive,
                isShared = config.isShared,
                userId = config.userId,
                createdBy = config.createdBy,
                updatedBy = config.updatedBy,
                createdAt = config.createdAt,
                updatedAt = config.updatedAt,
                lastUsedAt = config.lastUsedAt,
                lastTestAt = config.lastTestAt,
                lastTestResult = config.lastTestResult
            )
        }
    }
}