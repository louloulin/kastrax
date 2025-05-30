package com.kastrax.ai2db.nl2sql.tool

import com.kastrax.core.tool.ToolExecutionContext
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SQLValidationToolTest {
    
    private lateinit var sqlValidationTool: SQLValidationTool
    
    @BeforeEach
    fun setUp() {
        sqlValidationTool = SQLValidationTool()
    }
    
    @Test
    fun `should validate simple SELECT query successfully`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("sql" to "SELECT * FROM users WHERE age > 18")
        )
        
        // When
        val result = sqlValidationTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.data)
        
        val validationResult = result.data as Map<String, Any>
        assertTrue(validationResult["isValid"] as Boolean)
        assertTrue((validationResult["confidence"] as Double) > 0.8)
        assertEquals("LOW", validationResult["complexity"])
        assertTrue((validationResult["issues"] as List<*>).isEmpty())
    }
    
    @Test
    fun `should detect dangerous SQL injection patterns`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("sql" to "SELECT * FROM users WHERE id = 1; DROP TABLE users; --")
        )
        
        // When
        val result = sqlValidationTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val validationResult = result.data as Map<String, Any>
        assertFalse(validationResult["isValid"] as Boolean)
        
        val issues = validationResult["issues"] as List<*>
        assertTrue(issues.any { it.toString().contains("DROP") })
        assertTrue(issues.any { it.toString().contains("危险关键字") })
    }
    
    @Test
    fun `should detect SQL injection with UNION attack`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("sql" to "SELECT * FROM users WHERE id = 1 UNION SELECT password FROM admin")
        )
        
        // When
        val result = sqlValidationTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val validationResult = result.data as Map<String, Any>
        assertFalse(validationResult["isValid"] as Boolean)
        
        val issues = validationResult["issues"] as List<*>
        assertTrue(issues.any { it.toString().contains("UNION") })
    }
    
    @Test
    fun `should handle missing sql parameter`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = emptyMap()
        )
        
        // When
        val result = sqlValidationTool.execute(context)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.error!!.contains("sql parameter is required"))
    }
    
    @Test
    fun `should handle empty sql parameter`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("sql" to "")
        )
        
        // When
        val result = sqlValidationTool.execute(context)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.error!!.contains("sql parameter cannot be empty"))
    }
    
    @Test
    fun `should validate INSERT query`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("sql" to "INSERT INTO users (name, email) VALUES ('John', 'john@example.com')")
        )
        
        // When
        val result = sqlValidationTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val validationResult = result.data as Map<String, Any>
        assertTrue(validationResult["isValid"] as Boolean)
        assertEquals("MEDIUM", validationResult["complexity"])
    }
    
    @Test
    fun `should validate UPDATE query`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("sql" to "UPDATE users SET name = 'Jane' WHERE id = 1")
        )
        
        // When
        val result = sqlValidationTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val validationResult = result.data as Map<String, Any>
        assertTrue(validationResult["isValid"] as Boolean)
        assertEquals("MEDIUM", validationResult["complexity"])
    }
    
    @Test
    fun `should validate DELETE query`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("sql" to "DELETE FROM users WHERE id = 1")
        )
        
        // When
        val result = sqlValidationTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val validationResult = result.data as Map<String, Any>
        assertTrue(validationResult["isValid"] as Boolean)
        assertEquals("HIGH", validationResult["complexity"])
    }
    
    @Test
    fun `should detect complex JOIN query`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf(
                "sql" to "SELECT u.name, o.amount FROM users u INNER JOIN orders o ON u.id = o.user_id WHERE u.age > 18"
            )
        )
        
        // When
        val result = sqlValidationTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val validationResult = result.data as Map<String, Any>
        assertTrue(validationResult["isValid"] as Boolean)
        assertEquals("MEDIUM", validationResult["complexity"])
    }
    
    @Test
    fun `should detect very complex query with subqueries`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf(
                "sql" to "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders WHERE amount > (SELECT AVG(amount) FROM orders))"
            )
        )
        
        // When
        val result = sqlValidationTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val validationResult = result.data as Map<String, Any>
        assertTrue(validationResult["isValid"] as Boolean)
        assertEquals("HIGH", validationResult["complexity"])
    }
    
    @Test
    fun `should detect syntax errors`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("sql" to "SELECT * FROM WHERE age > 18")
        )
        
        // When
        val result = sqlValidationTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val validationResult = result.data as Map<String, Any>
        assertFalse(validationResult["isValid"] as Boolean)
        
        val issues = validationResult["issues"] as List<*>
        assertTrue(issues.any { it.toString().contains("语法错误") })
    }
    
    @Test
    fun `should detect missing WHERE clause in DELETE`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("sql" to "DELETE FROM users")
        )
        
        // When
        val result = sqlValidationTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val validationResult = result.data as Map<String, Any>
        assertFalse(validationResult["isValid"] as Boolean)
        
        val issues = validationResult["issues"] as List<*>
        assertTrue(issues.any { it.toString().contains("DELETE语句缺少WHERE条件") })
    }
    
    @Test
    fun `should detect missing WHERE clause in UPDATE`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("sql" to "UPDATE users SET name = 'test'")
        )
        
        // When
        val result = sqlValidationTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val validationResult = result.data as Map<String, Any>
        assertFalse(validationResult["isValid"] as Boolean)
        
        val issues = validationResult["issues"] as List<*>
        assertTrue(issues.any { it.toString().contains("UPDATE语句缺少WHERE条件") })
    }
    
    @Test
    fun `should detect SELECT without FROM clause`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("sql" to "SELECT name, email")
        )
        
        // When
        val result = sqlValidationTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val validationResult = result.data as Map<String, Any>
        assertFalse(validationResult["isValid"] as Boolean)
        
        val issues = validationResult["issues"] as List<*>
        assertTrue(issues.any { it.toString().contains("SELECT语句缺少FROM子句") })
    }
    
    @Test
    fun `should handle case insensitive SQL keywords`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("sql" to "select * from users where age > 18")
        )
        
        // When
        val result = sqlValidationTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val validationResult = result.data as Map<String, Any>
        assertTrue(validationResult["isValid"] as Boolean)
    }
    
    @Test
    fun `should detect multiple dangerous keywords`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("sql" to "SELECT * FROM users; DROP DATABASE test; TRUNCATE TABLE logs;")
        )
        
        // When
        val result = sqlValidationTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val validationResult = result.data as Map<String, Any>
        assertFalse(validationResult["isValid"] as Boolean)
        
        val issues = validationResult["issues"] as List<*>
        assertTrue(issues.size >= 2) // Should detect multiple dangerous keywords
        assertTrue(issues.any { it.toString().contains("DROP") })
        assertTrue(issues.any { it.toString().contains("TRUNCATE") })
    }
    
    @Test
    fun `should calculate confidence based on issues`() = runTest {
        // Given - SQL with minor issues
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("sql" to "SELECT * FROM users WHERE 1=1")
        )
        
        // When
        val result = sqlValidationTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val validationResult = result.data as Map<String, Any>
        val confidence = validationResult["confidence"] as Double
        
        // Should have lower confidence due to suspicious pattern
        assertTrue(confidence < 1.0)
        assertTrue(confidence > 0.0)
    }
    
    @Test
    fun `should handle SQL with comments`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("sql" to "SELECT * FROM users /* this is a comment */ WHERE age > 18")
        )
        
        // When
        val result = sqlValidationTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val validationResult = result.data as Map<String, Any>
        assertTrue(validationResult["isValid"] as Boolean)
    }
}