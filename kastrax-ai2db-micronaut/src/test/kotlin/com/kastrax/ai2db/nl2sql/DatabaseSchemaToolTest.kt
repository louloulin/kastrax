package com.kastrax.ai2db.nl2sql

import com.kastrax.ai2db.connection.manager.ConnectionManager
import com.kastrax.ai2db.connection.model.DatabaseConnection
import com.kastrax.ai2db.nl2sql.tool.DatabaseSchemaTool
import com.kastrax.ai2db.nl2sql.tool.SchemaInput
import com.kastrax.ai2db.schema.manager.SchemaManager
import com.kastrax.ai2db.schema.model.DatabaseSchema
import com.kastrax.ai2db.schema.model.TableInfo
import com.kastrax.ai2db.schema.model.ColumnInfo
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet

/**
 * 数据库模式工具测试
 */
@MicronautTest
class DatabaseSchemaToolTest {
    
    private lateinit var mockConnectionManager: ConnectionManager
    private lateinit var mockSchemaManager: SchemaManager
    private lateinit var databaseSchemaTool: DatabaseSchemaTool
    
    @BeforeEach
    fun setup() {
        mockConnectionManager = mockk()
        mockSchemaManager = mockk()
        databaseSchemaTool = DatabaseSchemaTool(mockConnectionManager, mockSchemaManager)
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
            assertEquals("database-schema", databaseSchemaTool.id)
            assertEquals("数据库模式", databaseSchemaTool.name)
            assertNotNull(databaseSchemaTool.description)
            assertNotNull(databaseSchemaTool.inputSchema)
            assertNotNull(databaseSchemaTool.outputSchema)
        }
        
        @Test
        @DisplayName("获取完整数据库模式")
        fun `should get complete database schema`() = runBlocking {
            // Given
            val input = SchemaInput(
                databaseId = "test-db",
                includeColumns = true,
                includeIndexes = true,
                includeConstraints = true
            )
            
            val mockSchema = DatabaseSchema(
                name = "test_schema",
                tables = listOf(
                    TableInfo(
                        name = "users",
                        columns = listOf(
                            ColumnInfo(
                                name = "id",
                                type = "BIGINT",
                                isPrimaryKey = true,
                                isNullable = false,
                                defaultValue = null,
                                comment = "用户ID"
                            ),
                            ColumnInfo(
                                name = "name",
                                type = "VARCHAR(100)",
                                isPrimaryKey = false,
                                isNullable = false,
                                comment = "用户姓名"
                            ),
                            ColumnInfo(
                                name = "email",
                                type = "VARCHAR(255)",
                                isPrimaryKey = false,
                                isNullable = true,
                                comment = "用户邮箱"
                            )
                        ),
                        comment = "用户表"
                    ),
                    TableInfo(
                        name = "orders",
                        columns = listOf(
                            ColumnInfo(
                                name = "id",
                                type = "BIGINT",
                                isPrimaryKey = true,
                                isNullable = false
                            ),
                            ColumnInfo(
                                name = "user_id",
                                type = "BIGINT",
                                isPrimaryKey = false,
                                isNullable = false
                            ),
                            ColumnInfo(
                                name = "amount",
                                type = "DECIMAL(10,2)",
                                isPrimaryKey = false,
                                isNullable = false
                            )
                        ),
                        comment = "订单表"
                    )
                )
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            coEvery { mockSchemaManager.getSchema(mockConnection, any()) } returns mockSchema
            
            // When
            val result = databaseSchemaTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertEquals("test_schema", result.name)
            assertEquals(2, result.tables.size)
            
            val usersTable = result.tables.find { it.name == "users" }
            assertNotNull(usersTable)
            assertEquals(3, usersTable!!.columns.size)
            assertEquals("用户表", usersTable.comment)
            
            val ordersTable = result.tables.find { it.name == "orders" }
            assertNotNull(ordersTable)
            assertEquals(3, ordersTable!!.columns.size)
            
            coVerify { mockConnectionManager.getConnection("test-db") }
            coVerify { mockSchemaManager.getSchema(mockConnection, any()) }
        }
        
        @Test
        @DisplayName("获取指定表的模式")
        fun `should get specific table schema`() = runBlocking {
            // Given
            val input = SchemaInput(
                databaseId = "test-db",
                tableNames = listOf("users"),
                includeColumns = true
            )
            
            val mockSchema = DatabaseSchema(
                name = "test_schema",
                tables = listOf(
                    TableInfo(
                        name = "users",
                        columns = listOf(
                            ColumnInfo(
                                name = "id",
                                type = "BIGINT",
                                isPrimaryKey = true,
                                isNullable = false
                            ),
                            ColumnInfo(
                                name = "name",
                                type = "VARCHAR(100)",
                                isPrimaryKey = false,
                                isNullable = false
                            )
                        )
                    )
                )
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            coEvery { mockSchemaManager.getSchema(mockConnection, any()) } returns mockSchema
            
            // When
            val result = databaseSchemaTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertEquals(1, result.tables.size)
            assertEquals("users", result.tables.first().name)
            assertEquals(2, result.tables.first().columns.size)
        }
    }
    
    @Nested
    @DisplayName("模式过滤测试")
    inner class SchemaFilteringTests {
        
        @Test
        @DisplayName("只包含表名，不包含列信息")
        fun `should include only table names without columns`() = runBlocking {
            // Given
            val input = SchemaInput(
                databaseId = "test-db",
                includeColumns = false
            )
            
            val mockSchema = DatabaseSchema(
                name = "test_schema",
                tables = listOf(
                    TableInfo(
                        name = "users",
                        columns = emptyList(),
                        comment = "用户表"
                    ),
                    TableInfo(
                        name = "orders",
                        columns = emptyList(),
                        comment = "订单表"
                    )
                )
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            coEvery { mockSchemaManager.getSchema(mockConnection, any()) } returns mockSchema
            
            // When
            val result = databaseSchemaTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertEquals(2, result.tables.size)
            result.tables.forEach { table ->
                assertTrue(table.columns.isEmpty())
                assertNotNull(table.name)
            }
        }
        
        @Test
        @DisplayName("按表名模式过滤")
        fun `should filter tables by name pattern`() = runBlocking {
            // Given
            val input = SchemaInput(
                databaseId = "test-db",
                tableNamePattern = "user%",
                includeColumns = true
            )
            
            val mockSchema = DatabaseSchema(
                name = "test_schema",
                tables = listOf(
                    TableInfo(
                        name = "users",
                        columns = listOf(
                            ColumnInfo(name = "id", type = "BIGINT", isPrimaryKey = true)
                        )
                    ),
                    TableInfo(
                        name = "user_profiles",
                        columns = listOf(
                            ColumnInfo(name = "user_id", type = "BIGINT", isPrimaryKey = true)
                        )
                    )
                )
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            coEvery { mockSchemaManager.getSchema(mockConnection, any()) } returns mockSchema
            
            // When
            val result = databaseSchemaTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertEquals(2, result.tables.size)
            assertTrue(result.tables.all { it.name.startsWith("user") })
        }
        
        @Test
        @DisplayName("限制返回表数量")
        fun `should limit number of returned tables`() = runBlocking {
            // Given
            val input = SchemaInput(
                databaseId = "test-db",
                maxTables = 2,
                includeColumns = false
            )
            
            val mockSchema = DatabaseSchema(
                name = "test_schema",
                tables = (1..5).map { i ->
                    TableInfo(
                        name = "table_$i",
                        columns = emptyList()
                    )
                }
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            coEvery { mockSchemaManager.getSchema(mockConnection, any()) } returns mockSchema
            
            // When
            val result = databaseSchemaTool.execute(input)
            
            // Then
            assertNotNull(result)
            assertTrue(result.tables.size <= 2)
        }
    }
    
    @Nested
    @DisplayName("列信息测试")
    inner class ColumnInfoTests {
        
        @Test
        @DisplayName("获取详细列信息")
        fun `should get detailed column information`() = runBlocking {
            // Given
            val input = SchemaInput(
                databaseId = "test-db",
                tableNames = listOf("users"),
                includeColumns = true,
                includeColumnComments = true
            )
            
            val mockSchema = DatabaseSchema(
                name = "test_schema",
                tables = listOf(
                    TableInfo(
                        name = "users",
                        columns = listOf(
                            ColumnInfo(
                                name = "id",
                                type = "BIGINT",
                                isPrimaryKey = true,
                                isNullable = false,
                                defaultValue = "AUTO_INCREMENT",
                                comment = "主键ID"
                            ),
                            ColumnInfo(
                                name = "name",
                                type = "VARCHAR(100)",
                                isPrimaryKey = false,
                                isNullable = false,
                                defaultValue = null,
                                comment = "用户姓名"
                            ),
                            ColumnInfo(
                                name = "email",
                                type = "VARCHAR(255)",
                                isPrimaryKey = false,
                                isNullable = true,
                                defaultValue = null,
                                comment = "邮箱地址"
                            ),
                            ColumnInfo(
                                name = "created_at",
                                type = "TIMESTAMP",
                                isPrimaryKey = false,
                                isNullable = false,
                                defaultValue = "CURRENT_TIMESTAMP",
                                comment = "创建时间"
                            )
                        )
                    )
                )
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            coEvery { mockSchemaManager.getSchema(mockConnection, any()) } returns mockSchema
            
            // When
            val result = databaseSchemaTool.execute(input)
            
            // Then
            assertNotNull(result)
            val usersTable = result.tables.first()
            assertEquals(4, usersTable.columns.size)
            
            val idColumn = usersTable.columns.find { it.name == "id" }
            assertNotNull(idColumn)
            assertTrue(idColumn!!.isPrimaryKey)
            assertFalse(idColumn.isNullable)
            assertEquals("AUTO_INCREMENT", idColumn.defaultValue)
            assertEquals("主键ID", idColumn.comment)
            
            val emailColumn = usersTable.columns.find { it.name == "email" }
            assertNotNull(emailColumn)
            assertFalse(emailColumn!!.isPrimaryKey)
            assertTrue(emailColumn.isNullable)
            assertNull(emailColumn.defaultValue)
            assertEquals("邮箱地址", emailColumn.comment)
        }
        
        @Test
        @DisplayName("获取主键信息")
        fun `should identify primary key columns`() = runBlocking {
            // Given
            val input = SchemaInput(
                databaseId = "test-db",
                tableNames = listOf("composite_key_table"),
                includeColumns = true
            )
            
            val mockSchema = DatabaseSchema(
                name = "test_schema",
                tables = listOf(
                    TableInfo(
                        name = "composite_key_table",
                        columns = listOf(
                            ColumnInfo(
                                name = "key1",
                                type = "BIGINT",
                                isPrimaryKey = true,
                                isNullable = false
                            ),
                            ColumnInfo(
                                name = "key2",
                                type = "BIGINT",
                                isPrimaryKey = true,
                                isNullable = false
                            ),
                            ColumnInfo(
                                name = "data",
                                type = "VARCHAR(255)",
                                isPrimaryKey = false,
                                isNullable = true
                            )
                        )
                    )
                )
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            coEvery { mockSchemaManager.getSchema(mockConnection, any()) } returns mockSchema
            
            // When
            val result = databaseSchemaTool.execute(input)
            
            // Then
            assertNotNull(result)
            val table = result.tables.first()
            val primaryKeyColumns = table.columns.filter { it.isPrimaryKey }
            assertEquals(2, primaryKeyColumns.size)
            assertTrue(primaryKeyColumns.any { it.name == "key1" })
            assertTrue(primaryKeyColumns.any { it.name == "key2" })
        }
    }
    
    @Nested
    @DisplayName("错误处理测试")
    inner class ErrorHandlingTests {
        
        @Test
        @DisplayName("数据库连接不存在")
        fun `should handle non-existent database connection`() = runBlocking {
            // Given
            val input = SchemaInput(
                databaseId = "non-existent-db"
            )
            
            coEvery { mockConnectionManager.getConnection("non-existent-db") } returns null
            
            // When & Then
            assertThrows<IllegalArgumentException> {
                runBlocking {
                    databaseSchemaTool.execute(input)
                }
            }
        }
        
        @Test
        @DisplayName("数据库连接失败")
        fun `should handle database connection failure`() = runBlocking {
            // Given
            val input = SchemaInput(
                databaseId = "test-db"
            )
            
            coEvery { mockConnectionManager.getConnection("test-db") } throws Exception("连接失败")
            
            // When & Then
            assertThrows<Exception> {
                runBlocking {
                    databaseSchemaTool.execute(input)
                }
            }
        }
        
        @Test
        @DisplayName("模式获取失败")
        fun `should handle schema retrieval failure`() = runBlocking {
            // Given
            val input = SchemaInput(
                databaseId = "test-db"
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            coEvery { mockSchemaManager.getSchema(mockConnection, any()) } throws Exception("模式获取失败")
            
            // When & Then
            assertThrows<Exception> {
                runBlocking {
                    databaseSchemaTool.execute(input)
                }
            }
        }
        
        @Test
        @DisplayName("空数据库ID处理")
        fun `should handle empty database id`() = runBlocking {
            // Given
            val input = SchemaInput(
                databaseId = ""
            )
            
            // When & Then
            assertThrows<IllegalArgumentException> {
                runBlocking {
                    databaseSchemaTool.execute(input)
                }
            }
        }
    }
    
    @Nested
    @DisplayName("性能测试")
    inner class PerformanceTests {
        
        @Test
        @DisplayName("大型数据库模式获取")
        fun `should handle large database schema efficiently`() = runBlocking {
            // Given
            val input = SchemaInput(
                databaseId = "large-db",
                includeColumns = true,
                maxTables = 50
            )
            
            val largeTables = (1..100).map { i ->
                TableInfo(
                    name = "table_$i",
                    columns = (1..20).map { j ->
                        ColumnInfo(
                            name = "column_$j",
                            type = "VARCHAR(255)",
                            isPrimaryKey = j == 1,
                            isNullable = j > 1
                        )
                    }
                )
            }
            
            val mockSchema = DatabaseSchema(
                name = "large_schema",
                tables = largeTables
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            coEvery { mockConnectionManager.getConnection("large-db") } returns mockConnection
            coEvery { mockSchemaManager.getSchema(mockConnection, any()) } returns mockSchema
            
            // When
            val startTime = System.currentTimeMillis()
            val result = databaseSchemaTool.execute(input)
            val endTime = System.currentTimeMillis()
            
            // Then
            assertNotNull(result)
            assertTrue(result.tables.size <= 50)
            
            val processingTime = endTime - startTime
            assertTrue(processingTime < 2000, "大型模式处理时间过长: ${processingTime}ms")
        }
        
        @Test
        @DisplayName("并发模式查询")
        fun `should handle concurrent schema queries`() = runBlocking {
            // Given
            val inputs = (1..5).map { i ->
                SchemaInput(
                    databaseId = "db_$i",
                    includeColumns = true
                )
            }
            
            val mockSchema = DatabaseSchema(
                name = "test_schema",
                tables = listOf(
                    TableInfo(
                        name = "test_table",
                        columns = listOf(
                            ColumnInfo(name = "id", type = "BIGINT", isPrimaryKey = true)
                        )
                    )
                )
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            coEvery { mockConnectionManager.getConnection(any()) } returns mockConnection
            coEvery { mockSchemaManager.getSchema(mockConnection, any()) } returns mockSchema
            
            // When
            val startTime = System.currentTimeMillis()
            val results = inputs.map { input ->
                databaseSchemaTool.execute(input)
            }
            val endTime = System.currentTimeMillis()
            
            // Then
            assertEquals(5, results.size)
            results.forEach { result ->
                assertNotNull(result)
                assertEquals(1, result.tables.size)
            }
            
            val processingTime = endTime - startTime
            assertTrue(processingTime < 3000, "并发查询处理时间过长: ${processingTime}ms")
        }
    }
    
    @Nested
    @DisplayName("缓存测试")
    inner class CacheTests {
        
        @Test
        @DisplayName("模式缓存功能")
        fun `should cache schema results`() = runBlocking {
            // Given
            val input = SchemaInput(
                databaseId = "test-db",
                useCache = true
            )
            
            val mockSchema = DatabaseSchema(
                name = "test_schema",
                tables = listOf(
                    TableInfo(name = "users", columns = emptyList())
                )
            )
            
            val mockConnection = mockk<DatabaseConnection>()
            coEvery { mockConnectionManager.getConnection("test-db") } returns mockConnection
            coEvery { mockSchemaManager.getSchema(mockConnection, any()) } returns mockSchema
            
            // When - 第一次调用
            val result1 = databaseSchemaTool.execute(input)
            // When - 第二次调用
            val result2 = databaseSchemaTool.execute(input)
            
            // Then
            assertNotNull(result1)
            assertNotNull(result2)
            assertEquals(result1.name, result2.name)
            assertEquals(result1.tables.size, result2.tables.size)
            
            // 验证只调用了一次数据库
            coVerify(exactly = 1) { mockSchemaManager.getSchema(mockConnection, any()) }
        }
    }
}