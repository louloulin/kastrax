package com.kastrax.ai2db.connection.repository

import com.kastrax.ai2db.connection.model.ConnectionConfig
import com.kastrax.ai2db.connection.model.DatabaseType
import com.kastrax.ai2db.persistence.entity.ConnectionEntity
import com.kastrax.ai2db.persistence.repository.ConnectionJpaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import java.util.*

/**
 * JPA implementation of the ConnectionRepository interface
 */
@Repository
class JpaConnectionRepository(
    private val jpaRepository: ConnectionJpaRepository
) : ConnectionRepository {
    /**
     * Find all connections
     */
    override suspend fun findAll(): List<ConnectionConfig> = withContext(Dispatchers.IO) {
        jpaRepository.findAll().map { it.toConnectionConfig() }
    }
    
    /**
     * Find a connection by ID
     */
    override suspend fun findById(id: String): ConnectionConfig? = withContext(Dispatchers.IO) {
        jpaRepository.findById(id).map { it.toConnectionConfig() }.orElse(null)
    }
    
    /**
     * Save a connection
     */
    override suspend fun save(connection: ConnectionConfig): ConnectionConfig = withContext(Dispatchers.IO) {
        val entity = if (jpaRepository.existsById(connection.id)) {
            // Update existing entity
            val existingEntity = jpaRepository.findById(connection.id).orElseThrow()
            ConnectionEntity.fromConnectionConfig(connection, existingEntity.createdBy)
        } else {
            // Create new entity with generated ID if necessary
            val id = if (connection.id.isBlank()) UUID.randomUUID().toString() else connection.id
            val connectionWithId = if (connection.id.isBlank()) connection.copy(id = id) else connection
            ConnectionEntity.fromConnectionConfig(connectionWithId)
        }
        
        jpaRepository.save(entity).toConnectionConfig()
    }
    
    /**
     * Delete a connection
     */
    override suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        if (jpaRepository.existsById(id)) {
            jpaRepository.deleteById(id)
            true
        } else {
            false
        }
    }
    
    /**
     * Find connections by name (partial match)
     */
    override suspend fun findByNameContaining(name: String): List<ConnectionConfig> = withContext(Dispatchers.IO) {
        jpaRepository.findByNameContainingIgnoreCase(name).map { it.toConnectionConfig() }
    }
    
    /**
     * Find connections by type
     */
    override suspend fun findByType(type: DatabaseType): List<ConnectionConfig> = withContext(Dispatchers.IO) {
        jpaRepository.findByType(type).map { it.toConnectionConfig() }
    }
} 