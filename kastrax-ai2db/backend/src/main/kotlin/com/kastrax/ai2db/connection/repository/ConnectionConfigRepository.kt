package com.kastrax.ai2db.connection.repository

import com.kastrax.ai2db.connection.model.ConnectionConfig
import com.kastrax.ai2db.connection.model.DatabaseType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.Repository
import java.util.Optional

/**
 * Repository interface for storing and retrieving connection configurations
 */
interface ConnectionConfigRepository : Repository<ConnectionConfigEntity, String> {

    /**
     * Find a connection config by its ID
     */
    fun findById(id: String): Optional<ConnectionConfigEntity>

    /**
     * Find all connection configs
     */
    fun findAll(): List<ConnectionConfigEntity>

    /**
     * Find all connection configs with pagination
     */
    fun findAll(pageable: Pageable): Page<ConnectionConfigEntity>

    /**
     * Find connection configs by name (exact match)
     */
    fun findByName(name: String): List<ConnectionConfigEntity>

    /**
     * Find connection configs by name (containing)
     */
    fun findByNameContainingIgnoreCase(name: String): List<ConnectionConfigEntity>

    /**
     * Find connection configs by database type
     */
    fun findByType(type: DatabaseType): List<ConnectionConfigEntity>

    /**
     * Save a connection config
     */
    fun save(connectionConfig: ConnectionConfigEntity): ConnectionConfigEntity

    /**
     * Delete a connection config by its ID
     */
    fun deleteById(id: String)

    /**
     * Count all connection configs
     */
    fun count(): Long
}

/**
 * Service for managing connection configurations
 */
@Service
class ConnectionConfigService(private val repository: ConnectionConfigRepository) {

    /**
     * Create a new connection configuration
     */
    fun createConnectionConfig(config: ConnectionConfig): ConnectionConfig {
        val entity = ConnectionConfigEntity.fromModel(config)
        val savedEntity = repository.save(entity)
        return savedEntity.toModel()
    }

    /**
     * Get a connection configuration by its ID
     */
    fun getConnectionConfig(id: String): ConnectionConfig? {
        val optionalEntity = repository.findById(id)
        return optionalEntity.map { it.toModel() }.orElse(null)
    }

    /**
     * Update an existing connection configuration
     */
    fun updateConnectionConfig(id: String, config: ConnectionConfig): ConnectionConfig? {
        val optionalEntity = repository.findById(id)

        if (optionalEntity.isPresent) {
            val existingEntity = optionalEntity.get()
            val updatedEntity = ConnectionConfigEntity.fromModel(config).copy(id = existingEntity.id)
            val savedEntity = repository.save(updatedEntity)
            return savedEntity.toModel()
        }

        return null
    }

    /**
     * Delete a connection configuration
     */
    fun deleteConnectionConfig(id: String) {
        repository.deleteById(id)
    }

    /**
     * Get all connection configurations
     */
    fun getAllConnectionConfigs(): List<ConnectionConfig> {
        return repository.findAll().map { it.toModel() }
    }

    /**
     * Get connection configurations by name
     */
    fun getConnectionConfigsByName(name: String, exact: Boolean = false): List<ConnectionConfig> {
        return if (exact) {
            repository.findByName(name).map { it.toModel() }
        } else {
            repository.findByNameContainingIgnoreCase(name).map { it.toModel() }
        }
    }

    /**
     * Get connection configurations by database type
     */
    fun getConnectionConfigsByType(type: DatabaseType): List<ConnectionConfig> {
        return repository.findByType(type).map { it.toModel() }
    }

    /**
     * Get connection configurations with pagination
     */
    fun getConnectionConfigs(pageable: Pageable): Page<ConnectionConfig> {
        return repository.findAll(pageable).map { it.toModel() }
    }
}
