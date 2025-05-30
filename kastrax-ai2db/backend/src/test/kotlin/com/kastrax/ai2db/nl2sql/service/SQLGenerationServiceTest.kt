package com.kastrax.ai2db.nl2sql.service

import com.kastrax.ai2db.nl2sql.model.QueryType
import com.kastrax.ai2db.nl2sql.model.QueryComplexity
import com.kastrax.ai2db.schema.model.DatabaseSchema
import com.kastrax.ai2db.schema.model.TableInfo
import com.kastrax.ai2db.schema.model.ColumnInfo
import com.kastrax.ai2db.connection.model.DatabaseType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SQLGenerationServiceTest {
    
    private lateinit var sqlGenerationService: SQLGenerationService
    
    @BeforeEach
    fun setUp() {
        sqlGenerationService = SQLGenerationService()
    }
    
    @Test
    fun `should generate SELECT SQL for query prompt`() = runTest {
        // Given
        val prompt = "用户查询: 查询所有用户信息\n请生成对应的SQL语句"
        val schema = createTestSchema()
        
        // When
        val result = sqlGenerationService.generateSQL(prompt, schema)
        
        // Then
        assertNotNull(result)
        assertTrue(result.sql.contains("SELECT"))
        assertTrue(result.sql.contains("users"))
        assertEquals(QueryType.SELECT, result.queryType)
        assertTrue(result.confidence > 0.0)
        assertNotNull(result.explanation)
    }
    
    @Test
    fun `should generate INSERT SQL for insert prompt`() = runTest {
        // Given
        val prompt = "用户查询: 插入新用户数据\n请生成对应的SQL语句"
        val schema = createTestSchema()
        
        // When
        val result = sqlGenerationService.generateSQL(prompt, schema)
        
        // Then
        assertNotNull(result)
        assertTrue(result.sql.contains("INSERT"))
        assertEquals(QueryType.INSERT, result.queryType)
        assertTrue(result.confidence > 0.0)
    }
    
    @Test
    fun `should generate UPDATE SQL for update prompt`() = runTest {
        // Given
        val prompt = "用户查询: 更新用户信息\n请生成对应的SQL语句"
        val schema = createTestSchema()
        
        // When
        val result = sqlGenerationService.generateSQL(prompt, schema)
        
        // Then
        assertNotNull(result)
        assertTrue(result.sql.contains("UPDATE"))
        assertEquals(QueryType.UPDATE, result.queryType)
        assertTrue(result.confidence > 0.0)
    }
    
    @Test
    fun `should generate DELETE SQL for delete prompt`() = runTest {
        // Given
        val prompt = "用户查询: 删除用户数据\n请生成对应的SQL语句"
        val schema = createTestSchema()
        
        // When
        val result = sqlGenerationService.generateSQL(prompt, schema)
        
        // Then
        assertNotNull(result)
        assertTrue(result.sql.contains("DELETE"))
        assertEquals(QueryType.DELETE, result.queryType)
        assertTrue(result.confidence > 0.0)
    }
    
    @Test
    fun `should use schema table name when available`() = runTest {
        // Given
        val prompt = "用户查询: 查询数据\n请生成对应的SQL语句"
        val schema = DatabaseSchema(
            name = "test_schema",
            tables = listOf(
                TableInfo(
                    name = "products",
                    columns = listOf(
                        ColumnInfo(
                            name = "id",
                            type = "INT",
                            size = 11,
                            nullable = false,
                            isPrimaryKey = true
                        ),
                        ColumnInfo(
                            name = "name",
                            type = "VARCHAR",
                            size = 255,
                            nullable = false
                        )
                    ),
                    primaryKeys = listOf("id")
                )
            ),
            databaseType = DatabaseType.MYSQL
        )
        
        // When
        val result = sqlGenerationService.generateSQL(prompt, schema)
        
        // Then
        assertTrue(result.sql.contains("products"))
    }
    
    @Test
    fun `should handle null schema gracefully`() = runTest {
        // Given
        val prompt = "用户查询: 查询用户信息\n请生成对应的SQL语句"
        
        // When
        val result = sqlGenerationService.generateSQL(prompt, null)
        
        // Then
        assertNotNull(result)
        assertTrue(result.sql.contains("SELECT"))
        assertTrue(result.confidence > 0.0)
    }
    
    @Test
    fun `should calculate higher confidence with schema`() = runTest {
        // Given
        val prompt = "用户查询: 查询用户信息\n请生成对应的SQL语句"
        val schema = createTestSchema()
        
        // When
        val resultWithSchema = sqlGenerationService.generateSQL(prompt, schema)
        val resultWithoutSchema = sqlGenerationService.generateSQL(prompt, null)
        
        // Then
        assertTrue(resultWithSchema.confidence > resultWithoutSchema.confidence)
    }
    
    @Test
    fun `should detect query complexity correctly`() = runTest {
        // Given - Simple query
        val simplePrompt = "用户查询: 查询用户\n请生成对应的SQL语句"
        
        // When
        val simpleResult = sqlGenerationService.generateSQL(simplePrompt, null)
        
        // Then
        assertTrue(simpleResult.complexity in listOf(QueryComplexity.LOW, QueryComplexity.MEDIUM))
    }
    
    @Test
    fun `should generate proper explanation`() = runTest {
        // Given
        val prompt = "用户查询: 查询所有活跃用户\n请生成对应的SQL语句"
        val schema = createTestSchema()
        
        // When
        val result = sqlGenerationService.generateSQL(prompt, schema)
        
        // Then
        assertNotNull(result.explanation)
        assertTrue(result.explanation.isNotEmpty())
        assertTrue(result.explanation.contains("查询"))
    }
    
    @Test
    fun `should handle English prompts`() = runTest {
        // Given
        val prompt = "User query: select all users\nGenerate corresponding SQL statement"
        val schema = createTestSchema()
        
        // When
        val result = sqlGenerationService.generateSQL(prompt, schema)
        
        // Then
        assertNotNull(result)
        assertTrue(result.sql.contains("SELECT"))
        assertEquals(QueryType.SELECT, result.queryType)
    }
    
    @Test
    fun `should handle mixed language prompts`() = runTest {
        // Given
        val prompt = "用户查询: select用户信息 from users table\n请生成对应的SQL语句"
        val schema = createTestSchema()
        
        // When
        val result = sqlGenerationService.generateSQL(prompt, schema)
        
        // Then
        assertNotNull(result)
        assertTrue(result.sql.contains("SELECT"))
        assertTrue(result.confidence > 0.0)
    }
    
    private fun createTestSchema(): DatabaseSchema {
        return DatabaseSchema(
            name = "test_schema",
            tables = listOf(
                TableInfo(
                    name = "users",
                    columns = listOf(
                        ColumnInfo(
                            name = "id",
                            type = "INT",
                            size = 11,
                            nullable = false,
                            isPrimaryKey = true
                        ),
                        ColumnInfo(
                            name = "name",
                            type = "VARCHAR",
                            size = 255,
                            nullable = false
                        ),
                        ColumnInfo(
                            name = "email",
                            type = "VARCHAR",
                            size = 255,
                            nullable = false
                        ),
                        ColumnInfo(
                            name = "age",
                            type = "INT",
                            size = 11,
                            nullable = true
                        ),
                        ColumnInfo(
                            name = "created_at",
                            type = "TIMESTAMP",
                            size = 0,
                            nullable = false
                        )
                    ),
                    primaryKeys = listOf("id")
                ),
                TableInfo(
                    name = "orders",
                    columns = listOf(
                        ColumnInfo(
                            name = "id",
                            type = "INT",
                            size = 11,
                            nullable = false,
                            isPrimaryKey = true
                        ),
                        ColumnInfo(
                            name = "user_id",
                            type = "INT",
                            size = 11,
                            nullable = false,
                            isForeignKey = true
                        ),
                        ColumnInfo(
                            name = "amount",
                            type = "DECIMAL",
                            size = 10,
                            nullable = false
                        )
                    ),
                    primaryKeys = listOf("id"),
                    foreignKeys = listOf("user_id")
                )
            ),
            databaseType = DatabaseType.MYSQL
        )
    }
}