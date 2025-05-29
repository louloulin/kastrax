package com.kastrax.ai2db.nl2sql

import com.kastrax.ai2db.nl2sql.model.*
import com.kastrax.ai2db.connection.model.DatabaseConnection
import com.kastrax.ai2db.connection.model.ConnectionConfig
import com.kastrax.ai2db.schema.model.DatabaseSchema
import com.kastrax.ai2db.schema.model.TableSchema
import com.kastrax.ai2db.schema.model.ColumnSchema
import com.kastrax.ai2db.schema.SchemaManager
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.*
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.ResultSetMetaData

/**
 * NL2SQL转换器测试
 */
@MicronautTest
class NL2SQLConverterTest {
    
    @Inject
    lateinit var nl2sqlConverter: NL2SQLConverter
    
    private lateinit var mockConnection: DatabaseConnection
    private lateinit var mockJdbcConnection: Connection
    private lateinit var mockSchemaManager: SchemaManager
    private lateinit var testSchema: DatabaseSchema
    
    @BeforeEach
    fun setup() {
        // 创建测试数据库模式
        testSchema = createTestSchema()
        
        // 模拟JDBC连接
        mockJdbcConnection = mockk<Connection>()
        
        // 模拟数据库连接
        mockConnection = mockk<DatabaseConnection> {
            every { id } returns "test-connection-1"
            every { config } returns ConnectionConfig(
                id = "test-config-1",
                name = "Test Database",
                type = "mysql",
                host = "localhost",
                port = 3306,
                database = "testdb",
                username = "testuser",
                password = "testpass"
            )
            every { jdbcConnection } returns mockJdbcConnection
        }
        
        // 模拟模式管理器
        mockSchemaManager = mockk<SchemaManager> {
            coEvery { getSchema(any(), any()) } returns testSchema
        }
    }
    
    @Nested
    @DisplayName("SQL转换测试")
    inner class SQLConversionTests {
        
        @Test
        @DisplayName("应该成功转换简单查询")
        fun shouldConvertSimpleQuery() = runBlocking {
            // Given
            val naturalQuery = "Show me all users"
            
            // When
            val result = nl2sqlConverter.convertToSQL(
                naturalLanguageQuery = naturalQuery,
                connection = mockConnection
            )
            
            // Then
            assertNotNull(result)
            assertEquals(QueryType.SELECT, result.type)
            assertTrue(result.sql.contains("SELECT", ignoreCase = true))
            assertTrue(result.sql.contains("users", ignoreCase = true))
        }
        
        @Test
        @DisplayName("应该处理复杂的JOIN查询")
        fun shouldHandleComplexJoinQuery() = runBlocking {
            // Given
            val naturalQuery = "Show me all orders with customer names"
            
            // When
            val result = nl2sqlConverter.convertToSQL(
                naturalLanguageQuery = naturalQuery,
                connection = mockConnection
            )
            
            // Then
            assertNotNull(result)
            assertTrue(result.sql.contains("JOIN", ignoreCase = true))
            assertTrue(result.sql.contains("orders", ignoreCase = true))
            assertTrue(result.sql.contains("customers", ignoreCase = true))
        }
        
        @Test
        @DisplayName("应该处理聚合查询")
        fun shouldHandleAggregateQuery() = runBlocking {
            // Given
            val naturalQuery = "Count the number of orders per customer"
            
            // When
            val result = nl2sqlConverter.convertToSQL(
                naturalLanguageQuery = naturalQuery,
                connection = mockConnection
            )
            
            // Then
            assertNotNull(result)
            assertTrue(result.sql.contains("COUNT", ignoreCase = true))
            assertTrue(result.sql.contains("GROUP BY", ignoreCase = true))
        }
        
        @Test
        @DisplayName("应该处理带条件的查询")
        fun shouldHandleQueryWithConditions() = runBlocking {
            // Given
            val naturalQuery = "Show me users created after 2023-01-01"
            
            // When
            val result = nl2sqlConverter.convertToSQL(
                naturalLanguageQuery = naturalQuery,
                connection = mockConnection
            )
            
            // Then
            assertNotNull(result)
            assertTrue(result.sql.contains("WHERE", ignoreCase = true))
            assertTrue(result.sql.contains("2023-01-01", ignoreCase = true))
        }
    }
    
    @Nested
    @DisplayName("SQL执行测试")
    inner class SQLExecutionTests {
        
        @Test
        @DisplayName("应该成功执行SELECT查询")
        fun shouldExecuteSelectQuery() = runBlocking {
            // Given
            val sqlQuery = SQLQuery(
                sql = "SELECT id, name FROM users LIMIT 10",
                type = QueryType.SELECT,
                originalQuery = "Show me users"
            )
            
            // 模拟ResultSet
            val mockResultSet = mockk<ResultSet>()
            val mockMetaData = mockk<ResultSetMetaData>()
            val mockStatement = mockk<PreparedStatement>()
            
            every { mockJdbcConnection.prepareStatement(any()) } returns mockStatement
            every { mockStatement.executeQuery() } returns mockResultSet
            every { mockResultSet.metaData } returns mockMetaData
            every { mockMetaData.columnCount } returns 2
            every { mockMetaData.getColumnName(1) } returns "id"
            every { mockMetaData.getColumnName(2) } returns "name"
            every { mockMetaData.getColumnTypeName(1) } returns "BIGINT"
            every { mockMetaData.getColumnTypeName(2) } returns "VARCHAR"
            
            // 模拟数据行
            every { mockResultSet.next() } returnsMany listOf(true, true, false)
            every { mockResultSet.getObject(1) } returnsMany listOf(1L, 2L)
            every { mockResultSet.getObject(2) } returnsMany listOf("Alice", "Bob")
            
            every { mockResultSet.close() } just Runs
            every { mockStatement.close() } just Runs
            
            // When
            val result = nl2sqlConverter.executeSQL(
                sqlQuery = sqlQuery,
                connection = mockConnection
            )
            
            // Then
            assertNotNull(result)
            assertEquals(2, result.data.size)
            assertEquals(2, result.columns.size)
            assertEquals("id", result.columns[0].name)
            assertEquals("name", result.columns[1].name)
            assertTrue(result.executionTime > 0)
        }
        
        @Test
        @DisplayName("应该处理空结果集")
        fun shouldHandleEmptyResultSet() = runBlocking {
            // Given
            val sqlQuery = SQLQuery(
                sql = "SELECT * FROM users WHERE id = -1",
                type = QueryType.SELECT,
                originalQuery = "Show me non-existent user"
            )
            
            val mockResultSet = mockk<ResultSet>()
            val mockMetaData = mockk<ResultSetMetaData>()
            val mockStatement = mockk<PreparedStatement>()
            
            every { mockJdbcConnection.prepareStatement(any()) } returns mockStatement
            every { mockStatement.executeQuery() } returns mockResultSet
            every { mockResultSet.metaData } returns mockMetaData
            every { mockMetaData.columnCount } returns 1
            every { mockMetaData.getColumnName(1) } returns "id"
            every { mockMetaData.getColumnTypeName(1) } returns "BIGINT"
            every { mockResultSet.next() } returns false
            
            every { mockResultSet.close() } just Runs
            every { mockStatement.close() } just Runs
            
            // When
            val result = nl2sqlConverter.executeSQL(
                sqlQuery = sqlQuery,
                connection = mockConnection
            )
            
            // Then
            assertNotNull(result)
            assertEquals(0, result.data.size)
            assertEquals(1, result.columns.size)
        }
    }
    
    @Nested
    @DisplayName("SQL验证测试")
    inner class SQLValidationTests {
        
        @Test
        @DisplayName("应该验证有效的SELECT查询")
        fun shouldValidateValidSelectQuery() = runBlocking {
            // Given
            val sqlQuery = SQLQuery(
                sql = "SELECT id, name FROM users WHERE active = 1",
                type = QueryType.SELECT,
                originalQuery = "Show me active users"
            )
            
            // When
            val result = nl2sqlConverter.validateSQL(
                sqlQuery = sqlQuery,
                connection = mockConnection
            )
            
            // Then
            assertNotNull(result)
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
        
        @Test
        @DisplayName("应该检测无效的SQL语法")
        fun shouldDetectInvalidSQLSyntax() = runBlocking {
            // Given
            val sqlQuery = SQLQuery(
                sql = "SELCT id, name FRM users", // 故意的语法错误
                type = QueryType.SELECT,
                originalQuery = "Show me users"
            )
            
            // When
            val result = nl2sqlConverter.validateSQL(
                sqlQuery = sqlQuery,
                connection = mockConnection
            )
            
            // Then
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.isNotEmpty())
            assertEquals(ErrorType.SYNTAX_ERROR, result.errors[0].type)
        }
        
        @Test
        @DisplayName("应该检测不存在的表")
        fun shouldDetectNonExistentTable() = runBlocking {
            // Given
            val sqlQuery = SQLQuery(
                sql = "SELECT * FROM non_existent_table",
                type = QueryType.SELECT,
                originalQuery = "Show me data from non-existent table"
            )
            
            // When
            val result = nl2sqlConverter.validateSQL(
                sqlQuery = sqlQuery,
                connection = mockConnection
            )
            
            // Then
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.type == ErrorType.TABLE_NOT_FOUND })
        }
        
        @Test
        @DisplayName("应该检测危险的SQL操作")
        fun shouldDetectDangerousOperations() = runBlocking {
            // Given
            val sqlQuery = SQLQuery(
                sql = "DELETE FROM users WHERE id > 0",
                type = QueryType.DELETE,
                originalQuery = "Delete all users"
            )
            
            // When
            val result = nl2sqlConverter.validateSQL(
                sqlQuery = sqlQuery,
                connection = mockConnection
            )
            
            // Then
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.type == ErrorType.DANGEROUS_OPERATION })
        }
    }
    
    @Nested
    @DisplayName("查询建议测试")
    inner class QuerySuggestionTests {
        
        @Test
        @DisplayName("应该为部分查询生成建议")
        fun shouldGenerateSuggestionsForPartialQuery() = runBlocking {
            // Given
            val partialQuery = "show me user"
            
            // When
            val suggestions = nl2sqlConverter.generateSuggestions(
                partialQuery = partialQuery,
                schema = testSchema
            )
            
            // Then
            assertNotNull(suggestions)
            assertTrue(suggestions.isNotEmpty())
            assertTrue(suggestions.any { it.text.contains("users", ignoreCase = true) })
        }
        
        @Test
        @DisplayName("应该基于表名生成建议")
        fun shouldGenerateSuggestionsBasedOnTableNames() = runBlocking {
            // Given
            val partialQuery = "order"
            
            // When
            val suggestions = nl2sqlConverter.generateSuggestions(
                partialQuery = partialQuery,
                schema = testSchema
            )
            
            // Then
            assertNotNull(suggestions)
            assertTrue(suggestions.any { it.text.contains("orders", ignoreCase = true) })
        }
    }
    
    /**
     * 创建测试数据库模式
     */
    private fun createTestSchema(): DatabaseSchema {
        val usersTable = TableSchema(
            name = "users",
            type = "TABLE",
            comment = "User accounts",
            columns = mutableListOf(
                ColumnSchema(
                    name = "id",
                    dataType = "BIGINT",
                    isPrimaryKey = true,
                    isNullable = false,
                    isAutoIncrement = true
                ),
                ColumnSchema(
                    name = "name",
                    dataType = "VARCHAR",
                    maxLength = 255,
                    isNullable = false
                ),
                ColumnSchema(
                    name = "email",
                    dataType = "VARCHAR",
                    maxLength = 255,
                    isNullable = false,
                    isUnique = true
                ),
                ColumnSchema(
                    name = "created_at",
                    dataType = "TIMESTAMP",
                    isNullable = false
                ),
                ColumnSchema(
                    name = "active",
                    dataType = "BOOLEAN",
                    isNullable = false,
                    defaultValue = "true"
                )
            )
        )
        
        val ordersTable = TableSchema(
            name = "orders",
            type = "TABLE",
            comment = "Customer orders",
            columns = mutableListOf(
                ColumnSchema(
                    name = "id",
                    dataType = "BIGINT",
                    isPrimaryKey = true,
                    isNullable = false,
                    isAutoIncrement = true
                ),
                ColumnSchema(
                    name = "user_id",
                    dataType = "BIGINT",
                    isNullable = false,
                    isForeignKey = true
                ),
                ColumnSchema(
                    name = "total_amount",
                    dataType = "DECIMAL",
                    precision = 10,
                    scale = 2,
                    isNullable = false
                ),
                ColumnSchema(
                    name = "status",
                    dataType = "VARCHAR",
                    maxLength = 50,
                    isNullable = false
                ),
                ColumnSchema(
                    name = "created_at",
                    dataType = "TIMESTAMP",
                    isNullable = false
                )
            )
        )
        
        val customersTable = TableSchema(
            name = "customers",
            type = "TABLE",
            comment = "Customer information",
            columns = mutableListOf(
                ColumnSchema(
                    name = "id",
                    dataType = "BIGINT",
                    isPrimaryKey = true,
                    isNullable = false,
                    isAutoIncrement = true
                ),
                ColumnSchema(
                    name = "name",
                    dataType = "VARCHAR",
                    maxLength = 255,
                    isNullable = false
                ),
                ColumnSchema(
                    name = "phone",
                    dataType = "VARCHAR",
                    maxLength = 20,
                    isNullable = true
                )
            )
        )
        
        return DatabaseSchema(
            name = "testdb",
            type = "mysql",
            tables = listOf(usersTable, ordersTable, customersTable),
            relationships = emptyList(),
            version = "MySQL 8.0"
        )
    }
}