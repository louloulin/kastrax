package com.kastrax.ai2db.persistence.entity

import com.kastrax.ai2db.connection.model.ConnectionConfig
import com.kastrax.ai2db.connection.model.DatabaseType
import jakarta.persistence.*
import java.time.Instant
import java.util.*

/**
 * JPA entity for database connections
 */
@Entity
@Table(name = "connections")
data class ConnectionEntity(
    @Id
    val id: String = UUID.randomUUID().toString(),
    
    @Column(nullable = false)
    val name: String,
    
    @Column(nullable = true, length = 1000)
    val description: String? = null,
    
    @Column(name = "db_type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: DatabaseType,
    
    @Column(nullable = false)
    val host: String,
    
    @Column(nullable = false)
    val port: Int,
    
    @Column(name = "database_name", nullable = false)
    val database: String,
    
    @Column(nullable = false)
    val username: String,
    
    @Column(name = "password_encrypted", nullable = false)
    val password: String,
    
    @Column(nullable = false)
    val ssl: Boolean = false,
    
    @Column(name = "connection_timeout")
    val connectionTimeout: Long = 30000L,
    
    @Column(name = "idle_timeout")
    val idleTimeout: Long = 600000L,
    
    @Column(name = "max_pool_size")
    val maxPoolSize: Int = 10,
    
    @Column(name = "parameters", columnDefinition = "jsonb")
    val parameters: String = "{}",
    
    @Column(name = "created_by")
    val createdBy: String? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    /**
     * Convert entity to connection config
     */
    fun toConnectionConfig(): ConnectionConfig {
        return ConnectionConfig(
            id = id,
            name = name,
            type = type,
            host = host,
            port = port,
            database = database,
            username = username,
            password = password,
            ssl = ssl,
            connectionTimeout = connectionTimeout,
            idleTimeout = idleTimeout,
            maxPoolSize = maxPoolSize,
            parameters = emptyMap() // Parameters would need to be parsed from JSON
        )
    }
    
    companion object {
        /**
         * Create entity from connection config
         */
        fun fromConnectionConfig(config: ConnectionConfig, createdBy: String? = null): ConnectionEntity {
            return ConnectionEntity(
                id = config.id,
                name = config.name,
                type = config.type,
                host = config.host,
                port = config.port,
                database = config.database,
                username = config.username,
                password = config.password,
                ssl = config.ssl,
                connectionTimeout = config.connectionTimeout,
                idleTimeout = config.idleTimeout,
                maxPoolSize = config.maxPoolSize,
                parameters = "{}", // Parameters would need to be serialized to JSON
                createdBy = createdBy
            )
        }
    }
} 