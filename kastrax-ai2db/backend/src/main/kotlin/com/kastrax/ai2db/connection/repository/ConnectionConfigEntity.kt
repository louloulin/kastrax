package com.kastrax.ai2db.connection.repository

import com.kastrax.ai2db.connection.model.ConnectionConfig
import com.kastrax.ai2db.connection.model.DatabaseType
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Entity class for storing connection configurations in the database
 */
@Entity
@Table(name = "connection_configs")
data class ConnectionConfigEntity(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: DatabaseType,

    @Column(nullable = false)
    val host: String,

    @Column(nullable = false)
    val port: Int,

    @Column(nullable = false)
    val database: String,

    @Column(nullable = false)
    val username: String,

    @Column(nullable = false)
    val password: String,

    @Column(name = "ssl_enabled")
    val sslEnabled: Boolean = false,

    @Column(nullable = false)
    val timezone: String = "UTC",

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "connection_properties",
        joinColumns = [JoinColumn(name = "connection_id")]
    )
    @MapKeyColumn(name = "property_key")
    @Column(name = "property_value")
    val properties: Map<String, String> = emptyMap(),

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    /**
     * Convert entity to model
     */
    fun toModel(): ConnectionConfig {
        return ConnectionConfig(
            name = name,
            type = type,
            host = host,
            port = port,
            database = database,
            username = username,
            password = password,
            sslEnabled = sslEnabled,
            timezone = timezone,
            properties = properties
        )
    }

    companion object {
        /**
         * Create entity from model
         */
        fun fromModel(model: ConnectionConfig): ConnectionConfigEntity {
            return ConnectionConfigEntity(
                name = model.name,
                type = model.type,
                host = model.host,
                port = model.port,
                database = model.database,
                username = model.username,
                password = model.password,
                sslEnabled = model.sslEnabled,
                timezone = model.timezone,
                properties = model.properties
            )
        }
    }
}
