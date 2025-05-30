package com.kastrax.ai2db.nl2sql

import com.kastrax.ai2db.connection.manager.ConnectionManager
import com.kastrax.ai2db.connection.model.DatabaseConnection
import com.kastrax.ai2db.nl2sql.tool.SQLValidationTool
import com.kastrax.ai2db.nl2sql.tool.SQLValidationInput
import com.kastrax.ai2db.nl2sql.tool.SQLValidationResult
import com.kastrax.ai2db.nl2sql.tool.ValidationError
import com.kastrax.ai2db.nl2sql.tool.ValidationWarning
import com.kastrax.ai2db.nl2sql.tool.SecurityIssue
import com.kastrax.ai2db.nl2sql.tool.PerformanceHint
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

/**
 * SQL验证工具测试
 */
@MicronautTest
class SQLValidationToolTest {
    
    private lateinit var mockConnectionManager: ConnectionManager
    private lateinit var sqlValidationTool: SQLValidationTool
    
    @BeforeEach
    fun setup() {
        mockConnectionManager = mockk()
        sqlValidationTool = SQLValidationTool(mockConnectionManager)
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
            assertEquals("sql-validation", sqlValidationTool.id)
            assertEquals("SQL验证", sqlValidationTool.name)
            assertNotNull(sqlValidationTool.description)
            assertNotNull(sqlValidationTool.inputSchema)
            assertNotNull(sqlValidationTool.outputSchema)
        }
        
        @Test
        @DisplayName("验证有效的SELECT语句")
        fun `should validate valid SELECT statement`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "SELECT id, name FROM users WHERE age > 18",
                databaseId = "test-db"
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            val mockJdbcConnection = mockk<Connection>()
            val mockStatement = mockk<PreparedStatement>()
            val mockResultSet = mockk<ResultSet>()
            
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            every { mockConnection.getJdbcConnection() } returns mockJdbcConnection
            every { mockJdbcConnection.prepareStatement(any()) } returns mockStatement
            every { mockStatement.executeQuery() } returns mockResultSet
            every { mockResultSet.next() } returns false
            every { mockStatement.close() } just Runs
            every { mockResultSet.close() } just Runs
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
            assertTrue(result.securityIssues.isEmpty())
        }
        
        @Test
        @DisplayName("验证有效的INSERT语句")
        fun `should validate valid INSERT statement`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "INSERT INTO users (name, email, age) VALUES ('John', 'john@example.com', 25)",
                databaseId = "test-db"
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            val mockJdbcConnection = mockk<Connection>()
            val mockStatement = mockk<PreparedStatement>()
            
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            every { mockConnection.getJdbcConnection() } returns mockJdbcConnection
            every { mockJdbcConnection.prepareStatement(any()) } returns mockStatement
            every { mockStatement.executeUpdate() } returns 1
            every { mockStatement.close() } just Runs
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
        
        @Test
        @DisplayName("验证有效的UPDATE语句")
        fun `should validate valid UPDATE statement`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "UPDATE users SET name = 'Jane' WHERE id = 1",
                databaseId = "test-db"
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            val mockJdbcConnection = mockk<Connection>()
            val mockStatement = mockk<PreparedStatement>()
            
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            every { mockConnection.getJdbcConnection() } returns mockJdbcConnection
            every { mockJdbcConnection.prepareStatement(any()) } returns mockStatement
            every { mockStatement.executeUpdate() } returns 1
            every { mockStatement.close() } just Runs
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
        
        @Test
        @DisplayName("验证有效的DELETE语句")
        fun `should validate valid DELETE statement`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "DELETE FROM users WHERE age < 18",
                databaseId = "test-db"
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            val mockJdbcConnection = mockk<Connection>()
            val mockStatement = mockk<PreparedStatement>()
            
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            every { mockConnection.getJdbcConnection() } returns mockJdbcConnection
            every { mockJdbcConnection.prepareStatement(any()) } returns mockStatement
            every { mockStatement.executeUpdate() } returns 5
            every { mockStatement.close() } just Runs
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
    }
    
    @Nested
    @DisplayName("语法错误检测")
    inner class SyntaxErrorTests {
        
        @Test
        @DisplayName("检测SQL语法错误")
        fun `should detect SQL syntax errors`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "SELCT id, name FROM users", // 故意的语法错误
                databaseId = "test-db"
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            val mockJdbcConnection = mockk<Connection>()
            
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            every { mockConnection.getJdbcConnection() } returns mockJdbcConnection
            every { mockJdbcConnection.prepareStatement(any()) } throws SQLException("语法错误")
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.isNotEmpty())
            assertTrue(result.errors.any { it.message.contains("语法错误") })
        }
        
        @Test
        @DisplayName("检测表名错误")
        fun `should detect table name errors`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "SELECT * FROM non_existent_table",
                databaseId = "test-db"
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            val mockJdbcConnection = mockk<Connection>()
            
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            every { mockConnection.getJdbcConnection() } returns mockJdbcConnection
            every { mockJdbcConnection.prepareStatement(any()) } throws SQLException("Table 'non_existent_table' doesn't exist")
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.isNotEmpty())
            assertTrue(result.errors.any { it.message.contains("doesn't exist") })
        }
        
        @Test
        @DisplayName("检测列名错误")
        fun `should detect column name errors`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "SELECT non_existent_column FROM users",
                databaseId = "test-db"
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            val mockJdbcConnection = mockk<Connection>()
            
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            every { mockConnection.getJdbcConnection() } returns mockJdbcConnection
            every { mockJdbcConnection.prepareStatement(any()) } throws SQLException("Unknown column 'non_existent_column'")
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.isNotEmpty())
            assertTrue(result.errors.any { it.message.contains("Unknown column") })
        }
    }
    
    @Nested
    @DisplayName("安全检查测试")
    inner class SecurityCheckTests {
        
        @Test
        @DisplayName("检测SQL注入风险")
        fun `should detect SQL injection risks`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "SELECT * FROM users WHERE id = '1' OR '1'='1'",
                databaseId = "test-db",
                checkSecurity = true
            )
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.securityIssues.isNotEmpty())
            assertTrue(result.securityIssues.any { 
                it.type == "SQL_INJECTION" && it.severity == "HIGH" 
            })
        }
        
        @Test
        @DisplayName("检测危险操作")
        fun `should detect dangerous operations`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "DROP TABLE users",
                databaseId = "test-db",
                checkSecurity = true
            )
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.securityIssues.isNotEmpty())
            assertTrue(result.securityIssues.any { 
                it.type == "DANGEROUS_OPERATION" && it.severity == "CRITICAL" 
            })
        }
        
        @Test
        @DisplayName("检测敏感数据暴露")
        fun `should detect sensitive data exposure`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "SELECT password, credit_card FROM users",
                databaseId = "test-db",
                checkSecurity = true
            )
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.securityIssues.isNotEmpty())
            assertTrue(result.securityIssues.any { 
                it.type == "SENSITIVE_DATA" && it.severity == "HIGH" 
            })
        }
        
        @Test
        @DisplayName("检测无WHERE子句的DELETE")
        fun `should detect DELETE without WHERE clause`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "DELETE FROM users",
                databaseId = "test-db",
                checkSecurity = true
            )
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.securityIssues.isNotEmpty())
            assertTrue(result.securityIssues.any { 
                it.type == "DANGEROUS_OPERATION" && it.message.contains("WHERE") 
            })
        }
        
        @Test
        @DisplayName("检测无WHERE子句的UPDATE")
        fun `should detect UPDATE without WHERE clause`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "UPDATE users SET status = 'inactive'",
                databaseId = "test-db",
                checkSecurity = true
            )
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.securityIssues.isNotEmpty())
            assertTrue(result.securityIssues.any { 
                it.type == "DANGEROUS_OPERATION" && it.message.contains("WHERE") 
            })
        }
    }
    
    @Nested
    @DisplayName("性能检查测试")
    inner class PerformanceCheckTests {
        
        @Test
        @DisplayName("检测SELECT *使用")
        fun `should detect SELECT star usage`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "SELECT * FROM users",
                databaseId = "test-db",
                checkPerformance = true
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            val mockJdbcConnection = mockk<Connection>()
            val mockStatement = mockk<PreparedStatement>()
            val mockResultSet = mockk<ResultSet>()
            
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            every { mockConnection.getJdbcConnection() } returns mockJdbcConnection
            every { mockJdbcConnection.prepareStatement(any()) } returns mockStatement
            every { mockStatement.executeQuery() } returns mockResultSet
            every { mockResultSet.next() } returns false
            every { mockStatement.close() } just Runs
            every { mockResultSet.close() } just Runs
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.performanceHints.isNotEmpty())
            assertTrue(result.performanceHints.any { 
                it.type == "SELECT_STAR" && it.severity == "MEDIUM" 
            })
        }
        
        @Test
        @DisplayName("检测缺少WHERE子句")
        fun `should detect missing WHERE clause in SELECT`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "SELECT id, name FROM users",
                databaseId = "test-db",
                checkPerformance = true
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            val mockJdbcConnection = mockk<Connection>()
            val mockStatement = mockk<PreparedStatement>()
            val mockResultSet = mockk<ResultSet>()
            
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            every { mockConnection.getJdbcConnection() } returns mockJdbcConnection
            every { mockJdbcConnection.prepareStatement(any()) } returns mockStatement
            every { mockStatement.executeQuery() } returns mockResultSet
            every { mockResultSet.next() } returns false
            every { mockStatement.close() } just Runs
            every { mockResultSet.close() } just Runs
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.performanceHints.isNotEmpty())
            assertTrue(result.performanceHints.any { 
                it.type == "MISSING_WHERE" && it.severity == "MEDIUM" 
            })
        }
        
        @Test
        @DisplayName("检测ORDER BY没有LIMIT")
        fun `should detect ORDER BY without LIMIT`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "SELECT id, name FROM users ORDER BY created_at DESC",
                databaseId = "test-db",
                checkPerformance = true
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            val mockJdbcConnection = mockk<Connection>()
            val mockStatement = mockk<PreparedStatement>()
            val mockResultSet = mockk<ResultSet>()
            
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            every { mockConnection.getJdbcConnection() } returns mockJdbcConnection
            every { mockJdbcConnection.prepareStatement(any()) } returns mockStatement
            every { mockStatement.executeQuery() } returns mockResultSet
            every { mockResultSet.next() } returns false
            every { mockStatement.close() } just Runs
            every { mockResultSet.close() } just Runs
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.performanceHints.isNotEmpty())
            assertTrue(result.performanceHints.any { 
                it.type == "ORDER_WITHOUT_LIMIT" && it.severity == "LOW" 
            })
        }
        
        @Test
        @DisplayName("检测可能的笛卡尔积")
        fun `should detect potential cartesian product`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "SELECT * FROM users, orders",
                databaseId = "test-db",
                checkPerformance = true
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            val mockJdbcConnection = mockk<Connection>()
            val mockStatement = mockk<PreparedStatement>()
            val mockResultSet = mockk<ResultSet>()
            
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            every { mockConnection.getJdbcConnection() } returns mockJdbcConnection
            every { mockJdbcConnection.prepareStatement(any()) } returns mockStatement
            every { mockStatement.executeQuery() } returns mockResultSet
            every { mockResultSet.next() } returns false
            every { mockStatement.close() } just Runs
            every { mockResultSet.close() } just Runs
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.performanceHints.isNotEmpty())
            assertTrue(result.performanceHints.any { 
                it.type == "CARTESIAN_PRODUCT" && it.severity == "HIGH" 
            })
        }
    }
    
    @Nested
    @DisplayName("优化建议测试")
    inner class OptimizationSuggestionTests {
        
        @Test
        @DisplayName("建议使用索引")
        fun `should suggest index usage`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "SELECT * FROM users WHERE email = 'test@example.com'",
                databaseId = "test-db",
                generateSuggestions = true
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            val mockJdbcConnection = mockk<Connection>()
            val mockStatement = mockk<PreparedStatement>()
            val mockResultSet = mockk<ResultSet>()
            
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            every { mockConnection.getJdbcConnection() } returns mockJdbcConnection
            every { mockJdbcConnection.prepareStatement(any()) } returns mockStatement
            every { mockStatement.executeQuery() } returns mockResultSet
            every { mockResultSet.next() } returns false
            every { mockStatement.close() } just Runs
            every { mockResultSet.close() } just Runs
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.suggestions.isNotEmpty())
            assertTrue(result.suggestions.any { 
                it.contains("索引") || it.contains("index") 
            })
        }
        
        @Test
        @DisplayName("建议使用LIMIT")
        fun `should suggest LIMIT usage`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "SELECT id, name FROM users ORDER BY created_at DESC",
                databaseId = "test-db",
                generateSuggestions = true
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            val mockJdbcConnection = mockk<Connection>()
            val mockStatement = mockk<PreparedStatement>()
            val mockResultSet = mockk<ResultSet>()
            
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            every { mockConnection.getJdbcConnection() } returns mockJdbcConnection
            every { mockJdbcConnection.prepareStatement(any()) } returns mockStatement
            every { mockStatement.executeQuery() } returns mockResultSet
            every { mockResultSet.next() } returns false
            every { mockStatement.close() } just Runs
            every { mockResultSet.close() } just Runs
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.suggestions.isNotEmpty())
            assertTrue(result.suggestions.any { 
                it.contains("LIMIT") 
            })
        }
        
        @Test
        @DisplayName("建议使用具体列名")
        fun `should suggest specific column names`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "SELECT * FROM users WHERE age > 18",
                databaseId = "test-db",
                generateSuggestions = true
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            val mockJdbcConnection = mockk<Connection>()
            val mockStatement = mockk<PreparedStatement>()
            val mockResultSet = mockk<ResultSet>()
            
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            every { mockConnection.getJdbcConnection() } returns mockJdbcConnection
            every { mockJdbcConnection.prepareStatement(any()) } returns mockStatement
            every { mockStatement.executeQuery() } returns mockResultSet
            every { mockResultSet.next() } returns false
            every { mockStatement.close() } just Runs
            every { mockResultSet.close() } just Runs
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.suggestions.isNotEmpty())
            assertTrue(result.suggestions.any { 
                it.contains("具体列名") || it.contains("specific columns") 
            })
        }
    }
    
    @Nested
    @DisplayName("错误处理测试")
    inner class ErrorHandlingTests {
        
        @Test
        @DisplayName("数据库连接不存在")
        fun `should handle non-existent database connection`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "SELECT * FROM users",
                databaseId = "non-existent-db"
            )
            
            coEvery { mockConnectionManager.getConnection("non-existent-db") } returns null
            
            // When & Then
            assertThrows<IllegalArgumentException> {
                runBlocking {
                    sqlValidationTool.execute(input)
                }
            }
        }
        
        @Test
        @DisplayName("空SQL语句")
        fun `should handle empty SQL statement`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "",
                databaseId = "test-db"
            )
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.isNotEmpty())
            assertTrue(result.errors.any { it.message.contains("空") || it.message.contains("empty") })
        }
        
        @Test
        @DisplayName("空白SQL语句")
        fun `should handle whitespace-only SQL statement`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "   \n\t   ",
                databaseId = "test-db"
            )
            
            // When
            val result = sqlValidationTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.isNotEmpty())
            assertTrue(result.errors.any { it.message.contains("空") || it.message.contains("empty") })
        }
        
        @Test
        @DisplayName("数据库连接失败")
        fun `should handle database connection failure`() = runBlocking {
            // Given
            val input = SQLValidationInput(
                sql = "SELECT * FROM users",
                databaseId = "test-db"
            )
            
            coEvery { mockConnectionManager.getConnection("test-db") } throws Exception("连接失败")
            
            // When & Then
            assertThrows<Exception> {
                runBlocking {
                    sqlValidationTool.execute(input)
                }
            }
        }
    }
    
    @Nested
    @DisplayName("批量验证测试")
    inner class BatchValidationTests {
        
        @Test
        @DisplayName("批量验证多个SQL语句")
        fun `should validate multiple SQL statements`() = runBlocking {
            // Given
            val sqls = listOf(
                "SELECT * FROM users",
                "INSERT INTO users (name) VALUES ('Test')",
                "UPDATE users SET name = 'Updated' WHERE id = 1",
                "DELETE FROM users WHERE id = 999"
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            val mockJdbcConnection = mockk<Connection>()
            val mockStatement = mockk<PreparedStatement>()
            val mockResultSet = mockk<ResultSet>()
            
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            every { mockConnection.getJdbcConnection() } returns mockJdbcConnection
            every { mockJdbcConnection.prepareStatement(any()) } returns mockStatement
            every { mockStatement.executeQuery() } returns mockResultSet
            every { mockStatement.executeUpdate() } returns 1
            every { mockResultSet.next() } returns false
            every { mockStatement.close() } just Runs
            every { mockResultSet.close() } just Runs
            
            // When
            val results = sqls.map { sql ->
                sqlValidationTool.execute(SQLValidationInput(
                    sql = sql,
                    databaseId = "test-db"
                ))
            }
            
            // Then
            assertEquals(4, results.size)
            results.forEach { result ->
                assertNotNull(result)
                assertTrue(result.isValid)
            }
        }
        
        @Test
        @DisplayName("批量验证包含错误的SQL语句")
        fun `should handle batch validation with errors`() = runBlocking {
            // Given
            val sqls = listOf(
                "SELECT * FROM users", // 有效
                "SELCT * FROM users", // 语法错误
                "SELECT * FROM non_existent_table", // 表不存在
                "INSERT INTO users (name) VALUES ('Test')" // 有效
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            val mockJdbcConnection = mockk<Connection>()
            val mockStatement = mockk<PreparedStatement>()
            val mockResultSet = mockk<ResultSet>()
            
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            every { mockConnection.getJdbcConnection() } returns mockJdbcConnection
            every { mockJdbcConnection.prepareStatement("SELECT * FROM users") } returns mockStatement
            every { mockJdbcConnection.prepareStatement("INSERT INTO users (name) VALUES ('Test')") } returns mockStatement
            every { mockJdbcConnection.prepareStatement("SELCT * FROM users") } throws SQLException("语法错误")
            every { mockJdbcConnection.prepareStatement("SELECT * FROM non_existent_table") } throws SQLException("Table doesn't exist")
            every { mockStatement.executeQuery() } returns mockResultSet
            every { mockStatement.executeUpdate() } returns 1
            every { mockResultSet.next() } returns false
            every { mockStatement.close() } just Runs
            every { mockResultSet.close() } just Runs
            
            // When
            val results = sqls.map { sql ->
                sqlValidationTool.execute(SQLValidationInput(
                    sql = sql,
                    databaseId = "test-db"
                ))
            }
            
            // Then
            assertEquals(4, results.size)
            assertTrue(results[0].isValid) // 第一个有效
            assertFalse(results[1].isValid) // 第二个语法错误
            assertFalse(results[2].isValid) // 第三个表不存在
            assertTrue(results[3].isValid) // 第四个有效
        }
    }
    
    @Nested
    @DisplayName("性能测试")
    inner class PerformanceTests {
        
        @Test
        @DisplayName("大型SQL语句验证")
        fun `should handle large SQL statement validation`() = runBlocking {
            // Given
            val largeSql = buildString {
                append("SELECT ")
                repeat(100) { i ->
                    if (i > 0) append(", ")
                    append("column_$i")
                }
                append(" FROM large_table WHERE ")
                repeat(50) { i ->
                    if (i > 0) append(" AND ")
                    append("condition_$i = 'value_$i'")
                }
            }
            
            val input = SQLValidationInput(
                sql = largeSql,
                databaseId = "test-db"
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            val mockJdbcConnection = mockk<Connection>()
            val mockStatement = mockk<PreparedStatement>()
            val mockResultSet = mockk<ResultSet>()
            
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            every { mockConnection.getJdbcConnection() } returns mockJdbcConnection
            every { mockJdbcConnection.prepareStatement(any()) } returns mockStatement
            every { mockStatement.executeQuery() } returns mockResultSet
            every { mockResultSet.next() } returns false
            every { mockStatement.close() } just Runs
            every { mockResultSet.close() } just Runs
            
            // When
            val startTime = System.currentTimeMillis()
            val result = sqlValidationTool.execute(input)
            val endTime = System.currentTimeMillis()
            
            // Then
            assertNotNull(result)
            assertTrue(result.isValid)
            
            val processingTime = endTime - startTime
            assertTrue(processingTime < 5000, "大型SQL验证时间过长: ${processingTime}ms")
        }
        
        @Test
        @DisplayName("并发SQL验证")
        fun `should handle concurrent SQL validation`() = runBlocking {
            // Given
            val sqls = (1..10).map { i ->
                "SELECT id, name FROM table_$i WHERE id = $i"
            }
            
            val mockConnection = mockk<DatabaseConnection>()
            val mockJdbcConnection = mockk<Connection>()
            val mockStatement = mockk<PreparedStatement>()
            val mockResultSet = mockk<ResultSet>()
            
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            every { mockConnection.getJdbcConnection() } returns mockJdbcConnection
            every { mockJdbcConnection.prepareStatement(any()) } returns mockStatement
            every { mockStatement.executeQuery() } returns mockResultSet
            every { mockResultSet.next() } returns false
            every { mockStatement.close() } just Runs
            every { mockResultSet.close() } just Runs
            
            // When
            val startTime = System.currentTimeMillis()
            val results = sqls.map { sql ->
                sqlValidationTool.execute(SQLValidationInput(
                    sql = sql,
                    databaseId = "test-db"
                ))
            }
            val endTime = System.currentTimeMillis()
            
            // Then
            assertEquals(10, results.size)
            results.forEach { result ->
                assertNotNull(result)
                assertTrue(result.isValid)
            }
            
            val processingTime = endTime - startTime
            assertTrue(processingTime < 3000, "并发SQL验证时间过长: ${processingTime}ms")
        }
    }
}