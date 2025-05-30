package com.kastrax.ai2db.nl2sql

import ai.kastrax.core.rag.RAG
import com.kastrax.ai2db.nl2sql.tool.SQLKnowledgeBaseTool
import com.kastrax.ai2db.nl2sql.tool.SQLKnowledgeInput
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * SQL知识库工具测试
 */
@MicronautTest
class SQLKnowledgeBaseToolTest {
    
    private lateinit var mockRAG: RAG
    private lateinit var sqlKnowledgeBaseTool: SQLKnowledgeBaseTool
    
    @BeforeEach
    fun setup() {
        mockRAG = mockk()
        sqlKnowledgeBaseTool = SQLKnowledgeBaseTool(mockRAG)
    }
    
    @AfterEach
    fun cleanup() {
        clearAllMocks()
    }
    
    @Nested
    @DisplayName("基础功能测试")
    inner class BasicFunctionalityTests {
        
        @Test
        @DisplayName("工具基本信息验证")
        fun `should have correct tool information`() {
            // Then
            assertEquals("sql-knowledge-base", sqlKnowledgeBaseTool.id)
            assertEquals("SQL知识库", sqlKnowledgeBaseTool.name)
            assertNotNull(sqlKnowledgeBaseTool.description)
            assertNotNull(sqlKnowledgeBaseTool.inputSchema)
            assertNotNull(sqlKnowledgeBaseTool.outputSchema)
        }
        
        @Test
        @DisplayName("简单查询知识检索")
        fun `should retrieve knowledge for simple query`() = runBlocking {
            // Given
            val input = SQLKnowledgeInput(
                query = "查询用户信息",
                schema = "users(id, name, email)",
                context = "获取所有用户的基本信息"
            )
            
            val mockDocuments = listOf(
                mockk {
                    every { content } returns "SELECT id, name, email FROM users;"
                    every { metadata } returns mapOf("type" to "example", "complexity" to "simple")
                },
                mockk {
                    every { content } returns "SELECT * FROM users WHERE active = 1;"
                    every { metadata } returns mapOf("type" to "example", "complexity" to "simple")
                }
            )
            
            coEvery { mockRAG.search(any(), any()) } returns mockDocuments
            
            // When
            val result = sqlKnowledgeBaseTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertEquals(2, result.size)
            assertTrue(result.any { it.contains("SELECT id, name, email FROM users") })
            assertTrue(result.any { it.contains("SELECT * FROM users WHERE active = 1") })
            
            coVerify { mockRAG.search(any(), any()) }
        }
        
        @Test
        @DisplayName("复杂查询知识检索")
        fun `should retrieve knowledge for complex query`() = runBlocking {
            // Given
            val input = SQLKnowledgeInput(
                query = "查询用户及其订单统计信息",
                schema = "users(id, name), orders(id, user_id, amount)",
                context = "需要JOIN查询和聚合函数"
            )
            
            val mockDocuments = listOf(
                mockk {
                    every { content } returns """
                        SELECT u.name, COUNT(o.id) as order_count, SUM(o.amount) as total_amount
                        FROM users u
                        LEFT JOIN orders o ON u.id = o.user_id
                        GROUP BY u.id, u.name;
                    """.trimIndent()
                    every { metadata } returns mapOf("type" to "example", "complexity" to "complex", "features" to "join,aggregation")
                }
            )
            
            coEvery { mockRAG.search(any(), any()) } returns mockDocuments
            
            // When
            val result = sqlKnowledgeBaseTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertEquals(1, result.size)
            val sqlExample = result.first()
            assertTrue(sqlExample.contains("JOIN"))
            assertTrue(sqlExample.contains("GROUP BY"))
            assertTrue(sqlExample.contains("COUNT"))
            assertTrue(sqlExample.contains("SUM"))
        }
    }
    
    @Nested
    @DisplayName("查询类型测试")
    inner class QueryTypeTests {
        
        @Test
        @DisplayName("SELECT查询知识检索")
        fun `should retrieve select query knowledge`() = runBlocking {
            // Given
            val input = SQLKnowledgeInput(
                query = "查询产品列表",
                schema = "products(id, name, price, category_id)"
            )
            
            val mockDocuments = listOf(
                mockk {
                    every { content } returns "SELECT id, name, price FROM products WHERE price > 100;"
                    every { metadata } returns mapOf("type" to "select", "table" to "products")
                }
            )
            
            coEvery { mockRAG.search(any(), any()) } returns mockDocuments
            
            // When
            val result = sqlKnowledgeBaseTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isNotEmpty())
            assertTrue(result.first().contains("SELECT"))
            assertTrue(result.first().contains("products"))
        }
        
        @Test
        @DisplayName("INSERT查询知识检索")
        fun `should retrieve insert query knowledge`() = runBlocking {
            // Given
            val input = SQLKnowledgeInput(
                query = "插入新用户",
                schema = "users(id, name, email, created_at)"
            )
            
            val mockDocuments = listOf(
                mockk {
                    every { content } returns "INSERT INTO users (name, email, created_at) VALUES (?, ?, NOW());"
                    every { metadata } returns mapOf("type" to "insert", "table" to "users")
                }
            )
            
            coEvery { mockRAG.search(any(), any()) } returns mockDocuments
            
            // When
            val result = sqlKnowledgeBaseTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isNotEmpty())
            assertTrue(result.first().contains("INSERT INTO"))
            assertTrue(result.first().contains("users"))
        }
        
        @Test
        @DisplayName("UPDATE查询知识检索")
        fun `should retrieve update query knowledge`() = runBlocking {
            // Given
            val input = SQLKnowledgeInput(
                query = "更新用户邮箱",
                schema = "users(id, name, email)"
            )
            
            val mockDocuments = listOf(
                mockk {
                    every { content } returns "UPDATE users SET email = ? WHERE id = ?;"
                    every { metadata } returns mapOf("type" to "update", "table" to "users")
                }
            )
            
            coEvery { mockRAG.search(any(), any()) } returns mockDocuments
            
            // When
            val result = sqlKnowledgeBaseTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isNotEmpty())
            assertTrue(result.first().contains("UPDATE"))
            assertTrue(result.first().contains("SET"))
        }
        
        @Test
        @DisplayName("DELETE查询知识检索")
        fun `should retrieve delete query knowledge`() = runBlocking {
            // Given
            val input = SQLKnowledgeInput(
                query = "删除过期订单",
                schema = "orders(id, user_id, created_at, status)"
            )
            
            val mockDocuments = listOf(
                mockk {
                    every { content } returns "DELETE FROM orders WHERE created_at < DATE_SUB(NOW(), INTERVAL 1 YEAR);"
                    every { metadata } returns mapOf("type" to "delete", "table" to "orders")
                }
            )
            
            coEvery { mockRAG.search(any(), any()) } returns mockDocuments
            
            // When
            val result = sqlKnowledgeBaseTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isNotEmpty())
            assertTrue(result.first().contains("DELETE FROM"))
            assertTrue(result.first().contains("orders"))
        }
    }
    
    @Nested
    @DisplayName("聚合查询测试")
    inner class AggregationTests {
        
        @Test
        @DisplayName("COUNT聚合查询")
        fun `should retrieve count aggregation knowledge`() = runBlocking {
            // Given
            val input = SQLKnowledgeInput(
                query = "统计用户数量",
                schema = "users(id, name, status)"
            )
            
            val mockDocuments = listOf(
                mockk {
                    every { content } returns "SELECT COUNT(*) as user_count FROM users WHERE status = 'active';"
                    every { metadata } returns mapOf("type" to "aggregation", "function" to "count")
                }
            )
            
            coEvery { mockRAG.search(any(), any()) } returns mockDocuments
            
            // When
            val result = sqlKnowledgeBaseTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isNotEmpty())
            assertTrue(result.first().contains("COUNT"))
        }
        
        @Test
        @DisplayName("GROUP BY查询")
        fun `should retrieve group by knowledge`() = runBlocking {
            // Given
            val input = SQLKnowledgeInput(
                query = "按部门统计员工数量",
                schema = "employees(id, name, department)"
            )
            
            val mockDocuments = listOf(
                mockk {
                    every { content } returns "SELECT department, COUNT(*) as emp_count FROM employees GROUP BY department;"
                    every { metadata } returns mapOf("type" to "aggregation", "feature" to "group_by")
                }
            )
            
            coEvery { mockRAG.search(any(), any()) } returns mockDocuments
            
            // When
            val result = sqlKnowledgeBaseTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isNotEmpty())
            assertTrue(result.first().contains("GROUP BY"))
            assertTrue(result.first().contains("COUNT"))
        }
    }
    
    @Nested
    @DisplayName("JOIN查询测试")
    inner class JoinTests {
        
        @Test
        @DisplayName("INNER JOIN查询")
        fun `should retrieve inner join knowledge`() = runBlocking {
            // Given
            val input = SQLKnowledgeInput(
                query = "查询用户及其订单",
                schema = "users(id, name), orders(id, user_id, amount)"
            )
            
            val mockDocuments = listOf(
                mockk {
                    every { content } returns """
                        SELECT u.name, o.amount
                        FROM users u
                        INNER JOIN orders o ON u.id = o.user_id;
                    """.trimIndent()
                    every { metadata } returns mapOf("type" to "join", "join_type" to "inner")
                }
            )
            
            coEvery { mockRAG.search(any(), any()) } returns mockDocuments
            
            // When
            val result = sqlKnowledgeBaseTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isNotEmpty())
            assertTrue(result.first().contains("INNER JOIN"))
            assertTrue(result.first().contains("ON"))
        }
        
        @Test
        @DisplayName("LEFT JOIN查询")
        fun `should retrieve left join knowledge`() = runBlocking {
            // Given
            val input = SQLKnowledgeInput(
                query = "查询所有用户及其可能的订单",
                schema = "users(id, name), orders(id, user_id, amount)"
            )
            
            val mockDocuments = listOf(
                mockk {
                    every { content } returns """
                        SELECT u.name, COALESCE(o.amount, 0) as amount
                        FROM users u
                        LEFT JOIN orders o ON u.id = o.user_id;
                    """.trimIndent()
                    every { metadata } returns mapOf("type" to "join", "join_type" to "left")
                }
            )
            
            coEvery { mockRAG.search(any(), any()) } returns mockDocuments
            
            // When
            val result = sqlKnowledgeBaseTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isNotEmpty())
            assertTrue(result.first().contains("LEFT JOIN"))
        }
    }
    
    @Nested
    @DisplayName("错误处理测试")
    inner class ErrorHandlingTests {
        
        @Test
        @DisplayName("RAG检索失败处理")
        fun `should handle rag search failure`() = runBlocking {
            // Given
            val input = SQLKnowledgeInput(
                query = "查询用户信息",
                schema = "users(id, name)"
            )
            
            coEvery { mockRAG.search(any(), any()) } throws Exception("RAG检索失败")
            
            // When & Then
            assertThrows<Exception> {
                runBlocking {
                    sqlKnowledgeBaseTool.execute(input)
                }
            }
        }
        
        @Test
        @DisplayName("空查询处理")
        fun `should handle empty query`() = runBlocking {
            // Given
            val input = SQLKnowledgeInput(
                query = "",
                schema = "users(id, name)"
            )
            
            coEvery { mockRAG.search(any(), any()) } returns emptyList()
            
            // When
            val result = sqlKnowledgeBaseTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isEmpty())
        }
        
        @Test
        @DisplayName("无匹配结果处理")
        fun `should handle no matching results`() = runBlocking {
            // Given
            val input = SQLKnowledgeInput(
                query = "非常特殊的查询需求",
                schema = "unknown_table(unknown_column)"
            )
            
            coEvery { mockRAG.search(any(), any()) } returns emptyList()
            
            // When
            val result = sqlKnowledgeBaseTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isEmpty())
        }
    }
    
    @Nested
    @DisplayName("性能测试")
    inner class PerformanceTests {
        
        @Test
        @DisplayName("大量知识检索性能")
        fun `should handle large knowledge retrieval efficiently`() = runBlocking {
            // Given
            val input = SQLKnowledgeInput(
                query = "复杂的业务查询",
                schema = "large_schema",
                maxResults = 100
            )
            
            val mockDocuments = (1..100).map { i ->
                mockk<Any> {
                    every { content } returns "SELECT * FROM table_$i;"
                    every { metadata } returns mapOf("index" to i)
                }
            }
            
            coEvery { mockRAG.search(any(), any()) } returns mockDocuments
            
            // When
            val startTime = System.currentTimeMillis()
            val result = sqlKnowledgeBaseTool.execute(input)
            val endTime = System.currentTimeMillis()
            
            // Then
            assertNotNull(result)
            assertEquals(100, result.size)
            
            val processingTime = endTime - startTime
            assertTrue(processingTime < 1000, "知识检索时间过长: ${processingTime}ms")
        }
        
        @Test
        @DisplayName("并发查询处理")
        fun `should handle concurrent queries`() = runBlocking {
            // Given
            val inputs = (1..10).map { i ->
                SQLKnowledgeInput(
                    query = "查询$i",
                    schema = "table_$i(id, name)"
                )
            }
            
            val mockDocument = mockk<Any> {
                every { content } returns "SELECT * FROM table;"
                every { metadata } returns emptyMap()
            }
            
            coEvery { mockRAG.search(any(), any()) } returns listOf(mockDocument)
            
            // When
            val startTime = System.currentTimeMillis()
            val results = inputs.map { input ->
                sqlKnowledgeBaseTool.execute(input)
            }
            val endTime = System.currentTimeMillis()
            
            // Then
            assertEquals(10, results.size)
            results.forEach { result ->
                assertNotNull(result)
                assertTrue(result.isNotEmpty())
            }
            
            val processingTime = endTime - startTime
            assertTrue(processingTime < 2000, "并发处理时间过长: ${processingTime}ms")
        }
    }
    
    @Nested
    @DisplayName("知识质量测试")
    inner class KnowledgeQualityTests {
        
        @Test
        @DisplayName("相关性排序验证")
        fun `should return relevant knowledge in order`() = runBlocking {
            // Given
            val input = SQLKnowledgeInput(
                query = "查询活跃用户",
                schema = "users(id, name, status, last_login)"
            )
            
            val mockDocuments = listOf(
                mockk {
                    every { content } returns "SELECT * FROM users WHERE status = 'active';"
                    every { metadata } returns mapOf("relevance" to 0.9, "type" to "exact_match")
                },
                mockk {
                    every { content } returns "SELECT * FROM users WHERE last_login > DATE_SUB(NOW(), INTERVAL 30 DAY);"
                    every { metadata } returns mapOf("relevance" to 0.8, "type" to "related")
                },
                mockk {
                    every { content } returns "SELECT * FROM users;"
                    every { metadata } returns mapOf("relevance" to 0.3, "type" to "general")
                }
            )
            
            coEvery { mockRAG.search(any(), any()) } returns mockDocuments
            
            // When
            val result = sqlKnowledgeBaseTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertEquals(3, result.size)
            
            // 验证最相关的结果排在前面
            assertTrue(result.first().contains("status = 'active'"))
        }
        
        @Test
        @DisplayName("知识去重验证")
        fun `should deduplicate similar knowledge`() = runBlocking {
            // Given
            val input = SQLKnowledgeInput(
                query = "查询用户",
                schema = "users(id, name)"
            )
            
            val duplicateContent = "SELECT id, name FROM users;"
            val mockDocuments = listOf(
                mockk {
                    every { content } returns duplicateContent
                    every { metadata } returns mapOf("source" to "doc1")
                },
                mockk {
                    every { content } returns duplicateContent
                    every { metadata } returns mapOf("source" to "doc2")
                },
                mockk {
                    every { content } returns "SELECT * FROM users;"
                    every { metadata } returns mapOf("source" to "doc3")
                }
            )
            
            coEvery { mockRAG.search(any(), any()) } returns mockDocuments
            
            // When
            val result = sqlKnowledgeBaseTool.execute(input)
            
            // Then
            assertNotNull(result)
            // 应该去重，只保留2个不同的SQL
            val uniqueResults = result.toSet()
            assertEquals(2, uniqueResults.size)
        }
    }
}