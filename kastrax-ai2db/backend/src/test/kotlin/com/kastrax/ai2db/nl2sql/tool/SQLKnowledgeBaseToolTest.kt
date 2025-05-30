package com.kastrax.ai2db.nl2sql.tool

import com.kastrax.core.tool.ToolExecutionContext
import com.kastrax.rag.RAG
import com.kastrax.rag.model.Document
import com.kastrax.rag.model.RetrievalResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SQLKnowledgeBaseToolTest {
    
    private lateinit var rag: RAG
    private lateinit var sqlKnowledgeBaseTool: SQLKnowledgeBaseTool
    
    @BeforeEach
    fun setUp() {
        rag = mockk()
        sqlKnowledgeBaseTool = SQLKnowledgeBaseTool(rag)
    }
    
    @Test
    fun `should retrieve SQL knowledge successfully`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("query" to "如何写JOIN查询")
        )
        
        val mockDocuments = listOf(
            Document(
                id = "doc1",
                content = "JOIN查询用于连接多个表。语法：SELECT * FROM table1 JOIN table2 ON table1.id = table2.foreign_id",
                metadata = mapOf(
                    "type" to "sql_example",
                    "category" to "join",
                    "difficulty" to "intermediate"
                )
            ),
            Document(
                id = "doc2",
                content = "INNER JOIN返回两个表中匹配的记录。LEFT JOIN返回左表的所有记录和右表匹配的记录。",
                metadata = mapOf(
                    "type" to "sql_concept",
                    "category" to "join",
                    "difficulty" to "basic"
                )
            )
        )
        
        val mockRetrievalResult = RetrievalResult(
            documents = mockDocuments,
            query = "如何写JOIN查询 SQL查询 连接表",
            totalResults = 2
        )
        
        coEvery { rag.retrieve(any(), any()) } returns mockRetrievalResult
        
        // When
        val result = sqlKnowledgeBaseTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.data)
        
        val knowledgeResult = result.data as String
        assertTrue(knowledgeResult.contains("JOIN查询"))
        assertTrue(knowledgeResult.contains("SQL示例"))
        assertTrue(knowledgeResult.contains("概念说明"))
        
        coVerify { rag.retrieve("如何写JOIN查询 SQL查询 连接表", 10) }
    }
    
    @Test
    fun `should handle missing query parameter`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = emptyMap()
        )
        
        // When
        val result = sqlKnowledgeBaseTool.execute(context)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.error!!.contains("query parameter is required"))
    }
    
    @Test
    fun `should handle empty query parameter`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("query" to "")
        )
        
        // When
        val result = sqlKnowledgeBaseTool.execute(context)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.error!!.contains("query parameter cannot be empty"))
    }
    
    @Test
    fun `should handle RAG retrieval failure`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("query" to "SELECT查询")
        )
        
        coEvery { rag.retrieve(any(), any()) } throws RuntimeException("RAG service unavailable")
        
        // When
        val result = sqlKnowledgeBaseTool.execute(context)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.error!!.contains("RAG service unavailable"))
    }
    
    @Test
    fun `should handle empty retrieval results`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("query" to "非常特殊的查询")
        )
        
        val emptyRetrievalResult = RetrievalResult(
            documents = emptyList(),
            query = "非常特殊的查询 SQL查询",
            totalResults = 0
        )
        
        coEvery { rag.retrieve(any(), any()) } returns emptyRetrievalResult
        
        // When
        val result = sqlKnowledgeBaseTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val knowledgeResult = result.data as String
        assertTrue(knowledgeResult.contains("未找到相关的SQL知识"))
    }
    
    @Test
    fun `should categorize documents correctly`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("query" to "SELECT查询")
        )
        
        val mockDocuments = listOf(
            Document(
                id = "doc1",
                content = "SELECT * FROM users WHERE age > 18",
                metadata = mapOf("type" to "sql_example")
            ),
            Document(
                id = "doc2",
                content = "SELECT语句用于从数据库中查询数据",
                metadata = mapOf("type" to "sql_concept")
            ),
            Document(
                id = "doc3",
                content = "使用SELECT时要注意性能优化",
                metadata = mapOf("type" to "best_practice")
            ),
            Document(
                id = "doc4",
                content = "其他类型的文档",
                metadata = mapOf("type" to "other")
            )
        )
        
        val mockRetrievalResult = RetrievalResult(
            documents = mockDocuments,
            query = "SELECT查询 SQL查询",
            totalResults = 4
        )
        
        coEvery { rag.retrieve(any(), any()) } returns mockRetrievalResult
        
        // When
        val result = sqlKnowledgeBaseTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val knowledgeResult = result.data as String
        
        assertTrue(knowledgeResult.contains("SQL示例"))
        assertTrue(knowledgeResult.contains("概念说明"))
        assertTrue(knowledgeResult.contains("最佳实践"))
        assertTrue(knowledgeResult.contains("其他相关信息"))
    }
    
    @Test
    fun `should build enhanced query with SQL keywords`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("query" to "查询用户信息")
        )
        
        val mockRetrievalResult = RetrievalResult(
            documents = emptyList(),
            query = "查询用户信息 SQL查询",
            totalResults = 0
        )
        
        coEvery { rag.retrieve("查询用户信息 SQL查询", 10) } returns mockRetrievalResult
        
        // When
        val result = sqlKnowledgeBaseTool.execute(context)
        
        // Then
        coVerify { rag.retrieve("查询用户信息 SQL查询", 10) }
    }
    
    @Test
    fun `should extract keywords correctly`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("query" to "如何使用JOIN连接多个表进行复杂查询")
        )
        
        val mockRetrievalResult = RetrievalResult(
            documents = emptyList(),
            query = "如何使用JOIN连接多个表进行复杂查询 SQL查询 连接 表 查询",
            totalResults = 0
        )
        
        coEvery { rag.retrieve(any(), any()) } returns mockRetrievalResult
        
        // When
        val result = sqlKnowledgeBaseTool.execute(context)
        
        // Then
        coVerify { rag.retrieve(match { it.contains("JOIN") && it.contains("连接") && it.contains("表") && it.contains("查询") }, 10) }
    }
    
    @Test
    fun `should handle custom limit parameter`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf(
                "query" to "SELECT查询",
                "limit" to "5"
            )
        )
        
        val mockRetrievalResult = RetrievalResult(
            documents = emptyList(),
            query = "SELECT查询 SQL查询",
            totalResults = 0
        )
        
        coEvery { rag.retrieve(any(), 5) } returns mockRetrievalResult
        
        // When
        val result = sqlKnowledgeBaseTool.execute(context)
        
        // Then
        coVerify { rag.retrieve("SELECT查询 SQL查询", 5) }
    }
    
    @Test
    fun `should handle invalid limit parameter gracefully`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf(
                "query" to "SELECT查询",
                "limit" to "invalid"
            )
        )
        
        val mockRetrievalResult = RetrievalResult(
            documents = emptyList(),
            query = "SELECT查询 SQL查询",
            totalResults = 0
        )
        
        coEvery { rag.retrieve(any(), 10) } returns mockRetrievalResult
        
        // When
        val result = sqlKnowledgeBaseTool.execute(context)
        
        // Then
        coVerify { rag.retrieve("SELECT查询 SQL查询", 10) }
    }
    
    @Test
    fun `should generate comprehensive summary`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("query" to "聚合函数")
        )
        
        val mockDocuments = listOf(
            Document(
                id = "doc1",
                content = "COUNT()函数用于计算记录数量",
                metadata = mapOf("type" to "sql_example", "category" to "aggregate")
            ),
            Document(
                id = "doc2",
                content = "SUM()函数用于计算数值总和",
                metadata = mapOf("type" to "sql_example", "category" to "aggregate")
            ),
            Document(
                id = "doc3",
                content = "聚合函数通常与GROUP BY一起使用",
                metadata = mapOf("type" to "best_practice", "category" to "aggregate")
            )
        )
        
        val mockRetrievalResult = RetrievalResult(
            documents = mockDocuments,
            query = "聚合函数 SQL查询 函数",
            totalResults = 3
        )
        
        coEvery { rag.retrieve(any(), any()) } returns mockRetrievalResult
        
        // When
        val result = sqlKnowledgeBaseTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val knowledgeResult = result.data as String
        
        assertTrue(knowledgeResult.contains("找到了 3 条相关的SQL知识"))
        assertTrue(knowledgeResult.contains("COUNT()"))
        assertTrue(knowledgeResult.contains("SUM()"))
        assertTrue(knowledgeResult.contains("GROUP BY"))
    }
}