package com.kastrax.ai2db.connection.repository

import com.kastrax.ai2db.connection.model.ConnectionConfig
import com.kastrax.ai2db.connection.model.DatabaseType
import com.kastrax.ai2db.persistence.entity.ConnectionEntity
import com.kastrax.ai2db.persistence.repository.ConnectionJpaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class JpaConnectionRepositoryTest {
    private lateinit var jpaConnectionRepository: JpaConnectionRepository
    private lateinit var mockJpaRepository: ConnectionJpaRepository
    
    @BeforeEach
    fun setup() {
        mockJpaRepository = mockk()
        jpaConnectionRepository = JpaConnectionRepository(mockJpaRepository)
    }
    
    @Test
    fun `findAll should return list of connection configs`() = runBlocking {
        // Arrange
        val entity1 = createConnectionEntity("1", "Connection 1", DatabaseType.MYSQL)
        val entity2 = createConnectionEntity("2", "Connection 2", DatabaseType.POSTGRESQL)
        every { mockJpaRepository.findAll() } returns listOf(entity1, entity2)
        
        // Act
        val result = jpaConnectionRepository.findAll()
        
        // Assert
        assertEquals(2, result.size)
        assertEquals("Connection 1", result[0].name)
        assertEquals(DatabaseType.MYSQL, result[0].type)
        assertEquals("Connection 2", result[1].name)
        assertEquals(DatabaseType.POSTGRESQL, result[1].type)
    }
    
    @Test
    fun `findById should return connection config when exists`() = runBlocking {
        // Arrange
        val id = "1"
        val entity = createConnectionEntity(id, "Connection 1", DatabaseType.MYSQL)
        every { mockJpaRepository.findById(id) } returns Optional.of(entity)
        
        // Act
        val result = jpaConnectionRepository.findById(id)
        
        // Assert
        assertNotNull(result)
        assertEquals(id, result?.id)
        assertEquals("Connection 1", result?.name)
        assertEquals(DatabaseType.MYSQL, result?.type)
    }
    
    @Test
    fun `findById should return null when not exists`() = runBlocking {
        // Arrange
        val id = "nonexistent"
        every { mockJpaRepository.findById(id) } returns Optional.empty()
        
        // Act
        val result = jpaConnectionRepository.findById(id)
        
        // Assert
        assertEquals(null, result)
    }
    
    @Test
    fun `save should create new entity when id doesn't exist`() = runBlocking {
        // Arrange
        val id = UUID.randomUUID().toString()
        val config = createConnectionConfig(id, "New Connection", DatabaseType.MYSQL)
        val savedEntity = createConnectionEntity(id, "New Connection", DatabaseType.MYSQL)
        
        every { mockJpaRepository.existsById(id) } returns false
        every { mockJpaRepository.save(any()) } returns savedEntity
        
        // Act
        val result = jpaConnectionRepository.save(config)
        
        // Assert
        assertEquals(id, result.id)
        assertEquals("New Connection", result.name)
        assertEquals(DatabaseType.MYSQL, result.type)
    }
    
    @Test
    fun `save should update existing entity when id exists`() = runBlocking {
        // Arrange
        val id = "existing"
        val config = createConnectionConfig(id, "Updated Connection", DatabaseType.POSTGRESQL)
        val existingEntity = createConnectionEntity(id, "Old Connection", DatabaseType.MYSQL)
        val updatedEntity = createConnectionEntity(id, "Updated Connection", DatabaseType.POSTGRESQL)
        
        every { mockJpaRepository.existsById(id) } returns true
        every { mockJpaRepository.findById(id) } returns Optional.of(existingEntity)
        every { mockJpaRepository.save(any()) } returns updatedEntity
        
        // Act
        val result = jpaConnectionRepository.save(config)
        
        // Assert
        assertEquals(id, result.id)
        assertEquals("Updated Connection", result.name)
        assertEquals(DatabaseType.POSTGRESQL, result.type)
    }
    
    @Test
    fun `delete should return true when entity exists`() = runBlocking {
        // Arrange
        val id = "existing"
        every { mockJpaRepository.existsById(id) } returns true
        every { mockJpaRepository.deleteById(id) } returns Unit
        
        // Act
        val result = jpaConnectionRepository.delete(id)
        
        // Assert
        assertTrue(result)
        coVerify { mockJpaRepository.deleteById(id) }
    }
    
    private fun createConnectionEntity(
        id: String,
        name: String,
        type: DatabaseType
    ): ConnectionEntity {
        return ConnectionEntity(
            id = id,
            name = name,
            type = type,
            host = "localhost",
            port = 3306,
            database = "test",
            username = "user",
            password = "password"
        )
    }
    
    private fun createConnectionConfig(
        id: String,
        name: String,
        type: DatabaseType
    ): ConnectionConfig {
        return ConnectionConfig(
            id = id,
            name = name,
            type = type,
            host = "localhost",
            port = 3306,
            database = "test",
            username = "user",
            password = "password"
        )
    }
} 