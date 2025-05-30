package com.kastrax.ai2db.nl2sql.agent

import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.tool.Tool
import ai.kastrax.core.tool.ToolExecuteResult
import ai.kastrax.memory.api.Memory
import ai.kastrax.memory.api.Message
import ai.kastrax.memory.api.MessageRole
import ai.kastrax.memory.api.SimpleMessage
import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagResult
import ai.kastrax.rag.Document
import com.kastrax.ai2db.nl2sql.model.SQLGenerationResult
import com.kastrax.ai2db.nl2sql.model.QueryType
import com.kastrax.ai2db.nl2sql.model.QueryComplexity
import com.kastrax.ai2db.nl2sql.service.SQLGenerationService
import com.kastrax.ai2db.schema.model.DatabaseSchema
import com.kastrax.ai2db.schema.model.TableInfo
import com.kastrax.ai2db.schema.model.ColumnInfo
import com.kastrax.ai2db.connection.model.DatabaseType
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NL2SQLAgentTest {
    
    private lateinit var memory: Memory
    private lateinit var rag: RAG
    private lateinit var tools: List<Tool>
    private lateinit var sqlGenerationService: SQLGenerationService
    private lateinit var nl2sqlAgent: NL2SQLAgent
    
    private lateinit var databaseSchemaTool: Tool
    private lateinit var sqlKnowledgeBaseTool: Tool
    private lateinit var sqlValidationTool: Tool
    
    @BeforeEach
    fun setUp() {
        memory = mockk()
        rag = mockk()
        sqlGenerationService = mockk()
        
        // Mock tools
        databaseSchemaTool = mockk {
            every { id } returns "database-schema"
            every { name } returns "Database Schema Tool"
            every { description } returns "Get database schema"
            every { version } returns "1.0.0"
        }
        
        sqlKnowledgeBaseTool = mockk {
            every { id } returns "sql-knowledge-base"
            every { name } returns "SQL Knowledge Base Tool"
            every { description } returns "Retrieve SQL knowledge"
            every { version } returns "1.0.0"
        }
        
        sqlValidationTool = mockk {
            every { id } returns "sql-validation"
            every { name } returns "SQL Validation Tool"
            every { description } returns "Validate SQL"
            every { version } returns "1.0.0"
        }
        
        tools = listOf(databaseSchemaTool, sqlKnowledgeBaseTool, sqlValidationTool)
        
        nl2sqlAgent = NL2SQLAgent(
            memory = memory,
            rag = rag,
            tools = tools,
            sqlGenerationService = sqlGenerationService
        )
    }
    
    @Test
    fun `should have correct agent properties`() {
        assertEquals("nl2sql-agent", nl2sqlAgent.id)
        assertEquals("1.0.0", nl2sqlAgent.version)
        assertEquals("NL2SQL智能代理", nl2sqlAgent.name)
        assertNotNull(nl2sqlAgent.description)
    }
    
    @Test
    fun `should generate SQL successfully for simple SELECT query`() = runTest {
        // Given
        val query = "查询所有用户信息"
        val threadId = "test-thread"
        val databaseId = "test-db"
        
        val conversationHistory = listOf<Message>()
        val ragResult = RagResult(
            documents = listOf(
                Document(
                    content = "SELECT * FROM users;",
                    score = 0.9,
                    metadata = mapOf("type" to "example")
                )
            ),
            query = query,
            metadata = mapOf()
        )
        
        val databaseSchema = DatabaseSchema(
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
                        )
                    ),
                    primaryKeys = listOf("id")
                )
            ),
            databaseType = DatabaseType.MYSQL
        )
        
        val sqlResult = SQLGenerationResult(
            sql = "SELECT * FROM users;",
            confidence = 0.9,
            explanation = "查询users表中的所有记录",
            queryType = QueryType.SELECT,
            complexity = QueryComplexity.LOW
        )
        
        val validationResult = mapOf(
            "valid" to true,
            "confidence" to 0.95,
            "warnings" to emptyList<String>()
        )
        
        // Mock calls
        coEvery { memory.getMessages(threadId, 10) } returns conversationHistory
        coEvery { rag.retrieveContext(any(), any()) } returns ragResult
        coEvery { databaseSchemaTool.execute(any(), any()) } returns ToolExecuteResult(
            success = true,
            output = mapOf("schema" to databaseSchema)
        )
        coEvery { sqlGenerationService.generateSQL(any(), any()) } returns sqlResult
        coEvery { sqlValidationTool.execute(any(), any()) } returns ToolExecuteResult(
            success = true,
            output = validationResult
        )
        coEvery { memory.saveMessage(any(), any()) } just Runs
        
        val options = AgentGenerateOptions(
            threadId = threadId,
            metadata = mapOf("database" to databaseId)
        )
        
        // When
        val result = nl2sqlAgent.generate(query, options)
        
        // Then
        assertTrue(result.content.isNotEmpty())
        assertEquals("SELECT * FROM users;", result.content)
        assertNotNull(result.metadata["confidence"])
        assertNotNull(result.metadata["explanation"])
        assertNotNull(result.metadata["validation"])
        
        // Verify interactions
        coVerify { memory.getMessages(threadId, 10) }
        coVerify { rag.retrieveContext(any(), any()) }
        coVerify { sqlGenerationService.generateSQL(any(), any()) }
        coVerify { memory.saveMessage(any(), threadId) }
    }
    
    @Test
    fun `should generate SQL for SELECT query with conditions`() = runTest {
        // Given
        val query = "查询年龄大于25岁的用户"
        val threadId = "test-thread"
        
        val conversationHistory = listOf<Message>()
        val ragResult = RagResult(
            documents = listOf(
                Document(
                    content = "SELECT * FROM users WHERE age > 25;",
                    score = 0.85,
                    metadata = mapOf("type" to "example")
                )
            ),
            query = query,
            metadata = mapOf()
        )
        
        val sqlResult = SQLGenerationResult(
            sql = "SELECT * FROM users WHERE age > 25;",
            confidence = 0.85,
            explanation = "查询users表中年龄大于25的用户记录",
            queryType = QueryType.SELECT,
            complexity = QueryComplexity.MEDIUM
        )
        
        val validationResult = mapOf(
            "valid" to true,
            "confidence" to 0.9,
            "warnings" to listOf("建议使用LIMIT限制结果数量")
        )
        
        // Mock calls
        coEvery { memory.getMessages(any(), any()) } returns conversationHistory
        coEvery { rag.retrieveContext(any(), any()) } returns ragResult
        coEvery { databaseSchemaTool.execute(any(), any()) } returns ToolExecuteResult(
            success = true,
            output = mapOf("schema" to null)
        )
        coEvery { sqlGenerationService.generateSQL(any(), any()) } returns sqlResult
        coEvery { sqlValidationTool.execute(any(), any()) } returns ToolExecuteResult(
            success = true,
            output = validationResult
        )
        coEvery { memory.saveMessage(any(), any()) } just Runs
        
        val options = AgentGenerateOptions(
            threadId = threadId,
            metadata = mapOf("database" to "test-db")
        )
        
        // When
        val result = nl2sqlAgent.generate(query, options)
        
        // Then
        assertEquals("SELECT * FROM users WHERE age > 25;", result.content)
        assertEquals(0.85, result.metadata["confidence"])
        assertEquals("查询users表中年龄大于25的用户记录", result.metadata["explanation"])
    }
    
    @Test
    fun `should generate SQL for JOIN query`() = runTest {
        // Given
        val query = "查询用户及其订单信息"
        val threadId = "test-thread"
        
        val conversationHistory = listOf<Message>()
        val ragResult = RagResult(
            documents = listOf(
                Document(
                    content = "SELECT u.*, o.* FROM users u JOIN orders o ON u.id = o.user_id;",
                    score = 0.8,
                    metadata = mapOf("type" to "example", "complexity" to "high")
                )
            ),
            query = query,
            metadata = mapOf()
        )
        
        val sqlResult = SQLGenerationResult(
            sql = "SELECT u.*, o.* FROM users u JOIN orders o ON u.id = o.user_id;",
            confidence = 0.8,
            explanation = "连接users和orders表查询用户及其订单信息",
            queryType = QueryType.SELECT,
            complexity = QueryComplexity.HIGH
        )
        
        val validationResult = mapOf(
            "valid" to true,
            "confidence" to 0.85,
            "warnings" to listOf("复杂查询建议添加适当的索引")
        )
        
        // Mock calls
        coEvery { memory.getMessages(any(), any()) } returns conversationHistory
        coEvery { rag.retrieveContext(any(), any()) } returns ragResult
        coEvery { databaseSchemaTool.execute(any(), any()) } returns ToolExecuteResult(
            success = true,
            output = mapOf("schema" to null)
        )
        coEvery { sqlGenerationService.generateSQL(any(), any()) } returns sqlResult
        coEvery { sqlValidationTool.execute(any(), any()) } returns ToolExecuteResult(
            success = true,
            output = validationResult
        )
        coEvery { memory.saveMessage(any(), any()) } just Runs
        
        val options = AgentGenerateOptions(
            threadId = threadId,
            metadata = mapOf("database" to "test-db")
        )
        
        // When
        val result = nl2sqlAgent.generate(query, options)
        
        // Then
        assertEquals("SELECT u.*, o.* FROM users u JOIN orders o ON u.id = o.user_id;", result.content)
        assertEquals(0.8, result.metadata["confidence"])
        assertEquals(QueryComplexity.HIGH.toString(), result.metadata["complexity"])
    }
    
    @Test
    fun `should handle error gracefully`() = runTest {
        // Given
        val query = "无效查询"
        val threadId = "test-thread"
        
        coEvery { memory.getMessages(any(), any()) } throws RuntimeException("Memory error")
        coEvery { memory.saveMessage(any(), any()) } just Runs
        
        val options = AgentGenerateOptions(
            threadId = threadId,
            metadata = mapOf("database" to "test-db")
        )
        
        // When & Then
        assertThrows<RuntimeException> {
            nl2sqlAgent.generate(query, options)
        }
        
        // Verify error is saved to memory
        coVerify { memory.saveMessage(match { it.role == MessageRole.USER }, threadId) }
        coVerify { memory.saveMessage(match { 
            it.role == MessageRole.ASSISTANT && it.content.startsWith("Error:")
        }, threadId) }
    }
    
    @Test
    fun `convertToSQL should return success result`() = runTest {
        // Given
        val query = "查询用户"
        val databaseId = "test-db"
        val threadId = "test-thread"
        
        val conversationHistory = listOf<Message>()
        val ragResult = RagResult(documents = emptyList(), query = query, metadata = mapOf())
        val sqlResult = SQLGenerationResult(
            sql = "SELECT * FROM users;",
            confidence = 0.9,
            explanation = "查询所有用户"
        )
        val validationResult = mapOf("valid" to true)
        
        // Mock calls
        coEvery { memory.getMessages(any(), any()) } returns conversationHistory
        coEvery { rag.retrieveContext(any(), any()) } returns ragResult
        coEvery { databaseSchemaTool.execute(any(), any()) } returns ToolExecuteResult(
            success = true,
            output = mapOf("schema" to null)
        )
        coEvery { sqlGenerationService.generateSQL(any(), any()) } returns sqlResult
        coEvery { sqlValidationTool.execute(any(), any()) } returns ToolExecuteResult(
            success = true,
            output = validationResult
        )
        coEvery { memory.saveMessage(any(), any()) } just Runs
        
        // When
        val result = nl2sqlAgent.convertToSQL(query, databaseId, threadId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("SELECT * FROM users;", result.sql)
        assertEquals(0.9, result.confidence)
        assertEquals("查询所有用户", result.explanation)
        assertEquals(null, result.error)
    }
    
    @Test
    fun `convertToSQL should return failure result on error`() = runTest {
        // Given
        val query = "查询用户"
        val databaseId = "test-db"
        
        coEvery { memory.getMessages(any(), any()) } throws RuntimeException("Database connection failed")
        coEvery { memory.saveMessage(any(), any()) } just Runs
        
        // When
        val result = nl2sqlAgent.convertToSQL(query, databaseId)
        
        // Then
        assertFalse(result.isSuccess)
        assertEquals(null, result.sql)
        assertEquals(0.0, result.confidence)
        assertEquals("", result.explanation)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Database connection failed"))
    }
}