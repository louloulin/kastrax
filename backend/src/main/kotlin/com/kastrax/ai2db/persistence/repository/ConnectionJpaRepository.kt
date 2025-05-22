package com.kastrax.ai2db.persistence.repository

import com.kastrax.ai2db.connection.model.DatabaseType
import com.kastrax.ai2db.persistence.entity.ConnectionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * JPA repository for database connections
 */
@Repository
interface ConnectionJpaRepository : JpaRepository<ConnectionEntity, String> {
    /**
     * Find connections by name containing the given text
     */
    fun findByNameContainingIgnoreCase(name: String): List<ConnectionEntity>
    
    /**
     * Find connections by database type
     */
    fun findByType(type: DatabaseType): List<ConnectionEntity>
    
    /**
     * Find connections by created by
     */
    fun findByCreatedBy(createdBy: String): List<ConnectionEntity>
} 