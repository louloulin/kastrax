package com.kastrax.ai2db.nl2sql

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.memory.Memory
import ai.kastrax.core.rag.RAG
import ai.kastrax.core.tool.Tool
import com.kastrax.ai2db.nl2sql.agent.NL2SQLAgent
import com.kastrax.ai2db.nl2sql.tool.DatabaseSchemaTool
import com.kastrax.ai2db.nl2sql.tool.SQLKnowledgeBaseTool
import com.kastrax.ai2db.nl2sql.tool.SQLValidationTool
import com.kastrax.ai2db.schema.model.DatabaseSchema
import com.kastrax.ai2db.schema.model.TableInfo
import com.kastrax.ai2db.schema.model.ColumnInfo
import com.kastrax.ai2db.connection.manager.ConnectionManager
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import jakarta.inject.Inject

/**
 * NL2SQL代理测试
 */
@MicronautTest
class NL2SQLAgentTest {
    
    @Inject
    private lateinit var connectionManager: ConnectionManager
    
    private lateinit var mockMemory: Memory
    private lateinit var mockRAG: RAG
    private lateinit var mockSQLKnowledgeBaseTool: SQLKnowledgeBaseTool
    private lateinit var mockDatabaseSchemaTool: DatabaseSchemaTool
    private lateinit var mockSQLValidationTool: SQLValidationTool
    private lateinit var nl2sqlAgent: NL2SQLAgent
    
    @BeforeEach
    fun setup() {
        // 创建Mock对象
        mockMemory = mockk()
        mockRAG = mockk()
        mockSQLKnowledgeBaseTool = mockk()
        mockDatabaseSchemaTool = mockk()
        mockSQLValidationTool = mockk()
        
        // 创建NL2SQL代理
        nl2sqlAgent = NL2SQLAgent(
            memory = mockMemory,
            rag = mockRAG,
            tools = listOf(
                mockSQLKnowledgeBaseTool,
                mockDatabaseSchemaTool,
                mockSQLValidationTool
            )
        )
    }
    
    @AfterEach
    fun cleanup() {
        clearAllMocks()
    }
    
    @Nested
    @DisplayName("基础SQL转换测试")
    inner class BasicSQLConversionTests {
        
        @Test
        @DisplayName("简单SELECT查询转换")
        fun `should convert simple select query`() = runBlocking {
            // Given
            val query = "查询所有用户的姓名和邮箱"
            val databaseId = "test-db"
            val threadId = "thread-1"
            
            val mockSchema = DatabaseSchema(
                name = "test_schema",
                tables = listOf(
                    TableInfo(
                        name = "users",
                        columns = listOf(
                            ColumnInfo(name = "id", type = "BIGINT", isPrimaryKey = true),
                            ColumnInfo(name = "name", type = "VARCHAR(100)"),
                            ColumnInfo(name = "email", type = "VARCHAR(255)")
                        )
                    )
                )
            )
            
            // Mock工具调用
            coEvery { mockDatabaseSchemaTool.execute(any()) } returns mockSchema
            coEvery { mockSQLKnowledgeBaseTool.execute(any()) } returns listOf(
                "SELECT name, email FROM users;"
            )
            coEvery { mockSQLValidationTool.execute(any()) } returns mockk {
                every { isValid } returns true
                every { errors } returns emptyList()
            }
            coEvery { mockMemory.saveMessage(any(), any(), any()) } just Runs
            coEvery { mockMemory.getMessages(any(), any()) } returns emptyList()
            
            // When
            val result = nl2sqlAgent.convertToSQL(query, databaseId, threadId)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isSuccess)
            assertNotNull(result.sql)
            assertTrue(result.sql!!.contains("SELECT"))
            assertTrue(result.sql!!.contains("name"))
            assertTrue(result.sql!!.contains("email"))
            assertTrue(result.sql!!.contains("users"))
            
            // 验证工具调用
            coVerify { mockDatabaseSchemaTool.execute(any()) }
            coVerify { mockSQLKnowledgeBaseTool.execute(any()) }
            coVerify { mockSQLValidationTool.execute(any()) }
            coVerify { mockMemory.saveMessage(any(), any(), any()) }
        }
        
        @Test
        @DisplayName("带条件的SELECT查询转换")
        fun `should convert select query with conditions`() = runBlocking {
            // Given
            val query = "查询年龄大于25岁的用户姓名"
            val databaseId = "test-db"
            val threadId = "thread-1"
            
            val mockSchema = DatabaseSchema(
                name = "test_schema",
                tables = listOf(
                    TableInfo(
                        name = "users",
                        columns = listOf(
                            ColumnInfo(name = "id", type = "BIGINT", isPrimaryKey = true),
                            ColumnInfo(name = "name", type = "VARCHAR(100)"),
                            ColumnInfo(name = "age", type = "INT")
                        )
                    )
                )
            )
            
            // Mock工具调用
            coEvery { mockDatabaseSchemaTool.execute(any()) } returns mockSchema
            coEvery { mockSQLKnowledgeBaseTool.execute(any()) } returns listOf(
                "SELECT name FROM users WHERE age > 25;"
            )
            coEvery { mockSQLValidationTool.execute(any()) } returns mockk {
                every { isValid } returns true
                every { errors } returns emptyList()
            }
            coEvery { mockMemory.saveMessage(any(), any(), any()) } just Runs
            coEvery { mockMemory.getMessages(any(), any()) } returns emptyList()
            
            // When
            val result = nl2sqlAgent.convertToSQL(query, databaseId, threadId)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isSuccess)
            assertNotNull(result.sql)
            assertTrue(result.sql!!.contains("WHERE"))
            assertTrue(result.sql!!.contains("age > 25"))
        }
        
        @Test
        @DisplayName("JOIN查询转换")
        fun `should convert join query`() = runBlocking {
            // Given
            val query = "查询用户及其订单信息"
            val databaseId = "test-db"
            val threadId = "thread-1"
            
            val mockSchema = DatabaseSchema(
                name = "test_schema",
                tables = listOf(
                    TableInfo(
                        name = "users",
                        columns = listOf(
                            ColumnInfo(name = "id", type = "BIGINT", isPrimaryKey = true),
                            ColumnInfo(name = "name", type = "VARCHAR(100)")
                        )
                    ),
                    TableInfo(
                        name = "orders",
                        columns = listOf(
                            ColumnInfo(name = "id", type = "BIGINT", isPrimaryKey = true),
                            ColumnInfo(name = "user_id", type = "BIGINT"),
                            ColumnInfo(name = "amount", type = "DECIMAL(10,2)")
                        )
                    )
                )
            )
            
            // Mock工具调用
            coEvery { mockDatabaseSchemaTool.execute(any()) } returns mockSchema
            coEvery { mockSQLKnowledgeBaseTool.execute(any()) } returns listOf(
                "SELECT u.name, o.amount FROM users u JOIN orders o ON u.id = o.user_id;"
            )
            coEvery { mockSQLValidationTool.execute(any()) } returns mockk {
                every { isValid } returns true
                every { errors } returns emptyList()
            }
            coEvery { mockMemory.saveMessage(any(), any(), any()) } just Runs
            coEvery { mockMemory.getMessages(any(), any()) } returns emptyList()
            
            // When
            val result = nl2sqlAgent.convertToSQL(query, databaseId, threadId)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isSuccess)
            assertNotNull(result.sql)
            assertTrue(result.sql!!.contains("JOIN"))
            assertTrue(result.sql!!.contains("users"))
            assertTrue(result.sql!!.contains("orders"))
        }
    }
    
    @Nested
    @DisplayName("聚合查询测试")
    inner class AggregateQueryTests {
        
        @Test
        @DisplayName("COUNT查询转换")
        fun `should convert count query`() = runBlocking {
            // Given
            val query = "统计用户总数"
            val databaseId = "test-db"
            val threadId = "thread-1"
            
            val mockSchema = DatabaseSchema(
                name = "test_schema",
                tables = listOf(
                    TableInfo(
                        name = "users",
                        columns = listOf(
                            ColumnInfo(name = "id", type = "BIGINT", isPrimaryKey = true),
                            ColumnInfo(name = "name", type = "VARCHAR(100)")
                        )
                    )
                )
            )
            
            // Mock工具调用
            coEvery { mockDatabaseSchemaTool.execute(any()) } returns mockSchema
            coEvery { mockSQLKnowledgeBaseTool.execute(any()) } returns listOf(
                "SELECT COUNT(*) FROM users;"
            )
            coEvery { mockSQLValidationTool.execute(any()) } returns mockk {
                every { isValid } returns true
                every { errors } returns emptyList()
            }
            coEvery { mockMemory.saveMessage(any(), any(), any()) } just Runs
            coEvery { mockMemory.getMessages(any(), any()) } returns emptyList()
            
            // When
            val result = nl2sqlAgent.convertToSQL(query, databaseId, threadId)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isSuccess)
            assertNotNull(result.sql)
            assertTrue(result.sql!!.contains("COUNT"))
        }
        
        @Test
        @DisplayName("GROUP BY查询转换")
        fun `should convert group by query`() = runBlocking {
            // Given
            val query = "按部门统计员工数量"
            val databaseId = "test-db"
            val threadId = "thread-1"
            
            val mockSchema = DatabaseSchema(
                name = "test_schema",
                tables = listOf(
                    TableInfo(
                        name = "employees",
                        columns = listOf(
                            ColumnInfo(name = "id", type = "BIGINT", isPrimaryKey = true),
                            ColumnInfo(name = "name", type = "VARCHAR(100)"),
                            ColumnInfo(name = "department", type = "VARCHAR(50)")
                        )
                    )
                )
            )
            
            // Mock工具调用
            coEvery { mockDatabaseSchemaTool.execute(any()) } returns mockSchema
            coEvery { mockSQLKnowledgeBaseTool.execute(any()) } returns listOf(
                "SELECT department, COUNT(*) FROM employees GROUP BY department;"
            )
            coEvery { mockSQLValidationTool.execute(any()) } returns mockk {
                every { isValid } returns true
                every { errors } returns emptyList()
            }
            coEvery { mockMemory.saveMessage(any(), any(), any()) } just Runs
            coEvery { mockMemory.getMessages(any(), any()) } returns emptyList()
            
            // When
            val result = nl2sqlAgent.convertToSQL(query, databaseId, threadId)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isSuccess)
            assertNotNull(result.sql)
            assertTrue(result.sql!!.contains("GROUP BY"))
            assertTrue(result.sql!!.contains("department"))
        }
    }
    
    @Nested
    @DisplayName("错误处理测试")
    inner class ErrorHandlingTests {
        
        @Test
        @DisplayName("无效查询处理")
        fun `should handle invalid query`() = runBlocking {
            // Given
            val query = "这不是一个有效的SQL查询请求"
            val databaseId = "test-db"
            val threadId = "thread-1"
            
            // Mock工具调用
            coEvery { mockDatabaseSchemaTool.execute(any()) } returns DatabaseSchema("test", emptyList())
            coEvery { mockSQLKnowledgeBaseTool.execute(any()) } returns emptyList()
            coEvery { mockMemory.saveMessage(any(), any(), any()) } just Runs
            coEvery { mockMemory.getMessages(any(), any()) } returns emptyList()
            
            // When
            val result = nl2sqlAgent.convertToSQL(query, databaseId, threadId)
            
            // Then
            assertNotNull(result)
            assertFalse(result.isSuccess)
            assertNotNull(result.error)
            assertTrue(result.error!!.contains("无法理解") || result.error!!.contains("无效"))
        }
        
        @Test
        @DisplayName("SQL验证失败处理")
        fun `should handle sql validation failure`() = runBlocking {
            // Given
            val query = "查询用户信息"
            val databaseId = "test-db"
            val threadId = "thread-1"
            
            val mockSchema = DatabaseSchema(
                name = "test_schema",
                tables = listOf(
                    TableInfo(
                        name = "users",
                        columns = listOf(
                            ColumnInfo(name = "id", type = "BIGINT", isPrimaryKey = true)
                        )
                    )
                )
            )
            
            // Mock工具调用 - SQL验证失败
            coEvery { mockDatabaseSchemaTool.execute(any()) } returns mockSchema
            coEvery { mockSQLKnowledgeBaseTool.execute(any()) } returns listOf(
                "INVALID SQL SYNTAX"
            )
            coEvery { mockSQLValidationTool.execute(any()) } returns mockk {
                every { isValid } returns false
                every { errors } returns listOf(
                    mockk {
                        every { type } returns "SYNTAX_ERROR"
                        every { message } returns "SQL语法错误"
                    }
                )
            }
            coEvery { mockMemory.saveMessage(any(), any(), any()) } just Runs
            coEvery { mockMemory.getMessages(any(), any()) } returns emptyList()
            
            // When
            val result = nl2sqlAgent.convertToSQL(query, databaseId, threadId)
            
            // Then
            assertNotNull(result)
            assertFalse(result.isSuccess)
            assertNotNull(result.error)
            assertTrue(result.error!!.contains("验证失败") || result.error!!.contains("语法错误"))
        }
        
        @Test
        @DisplayName("数据库连接失败处理")
        fun `should handle database connection failure`() = runBlocking {
            // Given
            val query = "查询用户信息"
            val databaseId = "invalid-db"
            val threadId = "thread-1"
            
            // Mock工具调用 - 数据库连接失败
            coEvery { mockDatabaseSchemaTool.execute(any()) } throws Exception("数据库连接失败")
            coEvery { mockMemory.saveMessage(any(), any(), any()) } just Runs
            coEvery { mockMemory.getMessages(any(), any()) } returns emptyList()
            
            // When
            val result = nl2sqlAgent.convertToSQL(query, databaseId, threadId)
            
            // Then
            assertNotNull(result)
            assertFalse(result.isSuccess)
            assertNotNull(result.error)
            assertTrue(result.error!!.contains("数据库") || result.error!!.contains("连接"))
        }
    }
    
    @Nested
    @DisplayName("记忆系统测试")
    inner class MemorySystemTests {
        
        @Test
        @DisplayName("对话历史记录")
        fun `should save conversation history`() = runBlocking {
            // Given
            val query = "查询用户信息"
            val databaseId = "test-db"
            val threadId = "thread-1"
            
            val mockSchema = DatabaseSchema("test", emptyList())
            
            // Mock工具调用
            coEvery { mockDatabaseSchemaTool.execute(any()) } returns mockSchema
            coEvery { mockSQLKnowledgeBaseTool.execute(any()) } returns listOf("SELECT * FROM users;")
            coEvery { mockSQLValidationTool.execute(any()) } returns mockk {
                every { isValid } returns true
                every { errors } returns emptyList()
            }
            coEvery { mockMemory.saveMessage(any(), any(), any()) } just Runs
            coEvery { mockMemory.getMessages(any(), any()) } returns emptyList()
            
            // When
            nl2sqlAgent.convertToSQL(query, databaseId, threadId)
            
            // Then
            coVerify(exactly = 2) { mockMemory.saveMessage(threadId, any(), any()) }
        }
        
        @Test
        @DisplayName("上下文感知查询")
        fun `should use conversation context`() = runBlocking {
            // Given
            val query = "显示他们的邮箱"
            val databaseId = "test-db"
            val threadId = "thread-1"
            
            val mockSchema = DatabaseSchema("test", emptyList())
            val previousMessages = listOf(
                mockk {
                    every { content } returns "查询所有用户"
                    every { role } returns "user"
                },
                mockk {
                    every { content } returns "SELECT * FROM users;"
                    every { role } returns "assistant"
                }
            )
            
            // Mock工具调用
            coEvery { mockDatabaseSchemaTool.execute(any()) } returns mockSchema
            coEvery { mockSQLKnowledgeBaseTool.execute(any()) } returns listOf("SELECT email FROM users;")
            coEvery { mockSQLValidationTool.execute(any()) } returns mockk {
                every { isValid } returns true
                every { errors } returns emptyList()
            }
            coEvery { mockMemory.saveMessage(any(), any(), any()) } just Runs
            coEvery { mockMemory.getMessages(threadId, any()) } returns previousMessages
            
            // When
            val result = nl2sqlAgent.convertToSQL(query, databaseId, threadId)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isSuccess)
            coVerify { mockMemory.getMessages(threadId, any()) }
        }
    }
    
    @Nested
    @DisplayName("RAG系统测试")
    inner class RAGSystemTests {
        
        @Test
        @DisplayName("知识库检索")
        fun `should retrieve knowledge from rag`() = runBlocking {
            // Given
            val query = "查询用户信息"
            val databaseId = "test-db"
            val threadId = "thread-1"
            
            val mockSchema = DatabaseSchema("test", emptyList())
            val knowledgeExamples = listOf(
                "SELECT id, name, email FROM users;",
                "SELECT * FROM users WHERE active = 1;"
            )
            
            // Mock工具调用
            coEvery { mockDatabaseSchemaTool.execute(any()) } returns mockSchema
            coEvery { mockSQLKnowledgeBaseTool.execute(any()) } returns knowledgeExamples
            coEvery { mockSQLValidationTool.execute(any()) } returns mockk {
                every { isValid } returns true
                every { errors } returns emptyList()
            }
            coEvery { mockMemory.saveMessage(any(), any(), any()) } just Runs
            coEvery { mockMemory.getMessages(any(), any()) } returns emptyList()
            
            // When
            val result = nl2sqlAgent.convertToSQL(query, databaseId, threadId)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isSuccess)
            coVerify { mockSQLKnowledgeBaseTool.execute(any()) }
        }
    }
    
    @Nested
    @DisplayName("性能测试")
    inner class PerformanceTests {
        
        @Test
        @DisplayName("批量查询处理")
        fun `should handle batch queries efficiently`() = runBlocking {
            // Given
            val queries = listOf(
                "查询用户信息",
                "统计订单数量",
                "查询产品列表"
            )
            val databaseId = "test-db"
            val threadId = "thread-1"
            
            val mockSchema = DatabaseSchema("test", emptyList())
            
            // Mock工具调用
            coEvery { mockDatabaseSchemaTool.execute(any()) } returns mockSchema
            coEvery { mockSQLKnowledgeBaseTool.execute(any()) } returns listOf("SELECT * FROM table;")
            coEvery { mockSQLValidationTool.execute(any()) } returns mockk {
                every { isValid } returns true
                every { errors } returns emptyList()
            }
            coEvery { mockMemory.saveMessage(any(), any(), any()) } just Runs
            coEvery { mockMemory.getMessages(any(), any()) } returns emptyList()
            
            // When
            val startTime = System.currentTimeMillis()
            val results = queries.map { query ->
                nl2sqlAgent.convertToSQL(query, databaseId, threadId)
            }
            val endTime = System.currentTimeMillis()
            
            // Then
            assertEquals(3, results.size)
            results.forEach { result ->
                assertNotNull(result)
            }
            
            // 性能检查 - 批量处理应该在合理时间内完成
            val processingTime = endTime - startTime
            assertTrue(processingTime < 5000, "批量处理时间过长: ${processingTime}ms")
        }
    }
}