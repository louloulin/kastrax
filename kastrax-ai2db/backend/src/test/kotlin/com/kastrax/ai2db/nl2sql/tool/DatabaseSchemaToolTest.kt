package com.kastrax.ai2db.nl2sql.tool

import com.kastrax.ai2db.connection.manager.ConnectionManager
import com.kastrax.ai2db.connection.model.DatabaseType
import com.kastrax.ai2db.schema.model.DatabaseSchema
import com.kastrax.ai2db.schema.model.TableInfo
import com.kastrax.ai2db.schema.model.ColumnInfo
import com.kastrax.core.tool.ToolExecutionContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DatabaseSchemaToolTest {
    
    private lateinit var connectionManager: ConnectionManager
    private lateinit var databaseSchemaTool: DatabaseSchemaTool
    private lateinit var mockConnection: Connection
    private lateinit var mockMetaData: DatabaseMetaData
    private lateinit var mockTablesResultSet: ResultSet
    private lateinit var mockColumnsResultSet: ResultSet
    private lateinit var mockPrimaryKeysResultSet: ResultSet
    private lateinit var mockForeignKeysResultSet: ResultSet
    private lateinit var mockIndexesResultSet: ResultSet
    
    @BeforeEach
    fun setUp() {
        connectionManager = mockk()
        mockConnection = mockk()
        mockMetaData = mockk()
        mockTablesResultSet = mockk()
        mockColumnsResultSet = mockk()
        mockPrimaryKeysResultSet = mockk()
        mockForeignKeysResultSet = mockk()
        mockIndexesResultSet = mockk()
        
        databaseSchemaTool = DatabaseSchemaTool(connectionManager)
    }
    
    @Test
    fun `should retrieve database schema successfully`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("connectionId" to "test-connection")
        )
        
        setupMockConnection()
        setupMockMetaData()
        setupMockResultSets()
        
        // When
        val result = databaseSchemaTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.data)
        
        val schema = result.data as DatabaseSchema
        assertEquals("test_schema", schema.name)
        assertEquals(DatabaseType.MYSQL, schema.databaseType)
        assertEquals(1, schema.tables.size)
        
        val table = schema.tables.first()
        assertEquals("users", table.name)
        assertEquals(3, table.columns.size)
        assertEquals(listOf("id"), table.primaryKeys)
        
        verify { connectionManager.getConnection("test-connection") }
    }
    
    @Test
    fun `should handle missing connection id parameter`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = emptyMap()
        )
        
        // When
        val result = databaseSchemaTool.execute(context)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.error!!.contains("connectionId parameter is required"))
    }
    
    @Test
    fun `should handle connection failure`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("connectionId" to "invalid-connection")
        )
        
        every { connectionManager.getConnection("invalid-connection") } throws RuntimeException("Connection not found")
        
        // When
        val result = databaseSchemaTool.execute(context)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.error!!.contains("Connection not found"))
    }
    
    @Test
    fun `should handle database metadata retrieval failure`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("connectionId" to "test-connection")
        )
        
        every { connectionManager.getConnection("test-connection") } returns mockConnection
        every { mockConnection.metaData } throws RuntimeException("Metadata access failed")
        
        // When
        val result = databaseSchemaTool.execute(context)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.error!!.contains("Metadata access failed"))
    }
    
    @Test
    fun `should retrieve schema with multiple tables`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("connectionId" to "test-connection")
        )
        
        setupMockConnection()
        setupMockMetaData()
        setupMockResultSetsForMultipleTables()
        
        // When
        val result = databaseSchemaTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val schema = result.data as DatabaseSchema
        assertEquals(2, schema.tables.size)
        
        val userTable = schema.tables.find { it.name == "users" }
        val orderTable = schema.tables.find { it.name == "orders" }
        
        assertNotNull(userTable)
        assertNotNull(orderTable)
        assertEquals(3, userTable.columns.size)
        assertEquals(2, orderTable.columns.size)
    }
    
    @Test
    fun `should handle empty database schema`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("connectionId" to "test-connection")
        )
        
        setupMockConnection()
        setupMockMetaData()
        setupEmptyMockResultSets()
        
        // When
        val result = databaseSchemaTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val schema = result.data as DatabaseSchema
        assertEquals(0, schema.tables.size)
    }
    
    @Test
    fun `should detect database type correctly`() = runTest {
        // Given
        val context = ToolExecutionContext(
            sessionId = "test-session",
            userId = "test-user",
            parameters = mapOf("connectionId" to "test-connection")
        )
        
        setupMockConnection()
        every { mockConnection.metaData } returns mockMetaData
        every { mockMetaData.databaseProductName } returns "PostgreSQL"
        every { mockMetaData.catalogs } returns mockk()
        every { mockMetaData.getTables(any(), any(), any(), any()) } returns mockTablesResultSet
        
        setupEmptyMockResultSets()
        
        // When
        val result = databaseSchemaTool.execute(context)
        
        // Then
        assertTrue(result.isSuccess)
        val schema = result.data as DatabaseSchema
        assertEquals(DatabaseType.POSTGRESQL, schema.databaseType)
    }
    
    private fun setupMockConnection() {
        every { connectionManager.getConnection("test-connection") } returns mockConnection
        every { mockConnection.metaData } returns mockMetaData
    }
    
    private fun setupMockMetaData() {
        every { mockMetaData.databaseProductName } returns "MySQL"
        every { mockMetaData.catalogs } returns mockk {
            every { next() } returnsMany listOf(true, false)
            every { getString("TABLE_CAT") } returns "test_schema"
        }
        every { mockMetaData.getTables(any(), any(), any(), any()) } returns mockTablesResultSet
        every { mockMetaData.getColumns(any(), any(), any(), any()) } returns mockColumnsResultSet
        every { mockMetaData.getPrimaryKeys(any(), any(), any()) } returns mockPrimaryKeysResultSet
        every { mockMetaData.getImportedKeys(any(), any(), any()) } returns mockForeignKeysResultSet
        every { mockMetaData.getIndexInfo(any(), any(), any(), any(), any()) } returns mockIndexesResultSet
    }
    
    private fun setupMockResultSets() {
        // Tables ResultSet
        every { mockTablesResultSet.next() } returnsMany listOf(true, false)
        every { mockTablesResultSet.getString("TABLE_NAME") } returns "users"
        every { mockTablesResultSet.getString("TABLE_TYPE") } returns "TABLE"
        
        // Columns ResultSet
        every { mockColumnsResultSet.next() } returnsMany listOf(true, true, true, false)
        every { mockColumnsResultSet.getString("COLUMN_NAME") } returnsMany listOf("id", "name", "email")
        every { mockColumnsResultSet.getString("TYPE_NAME") } returnsMany listOf("INT", "VARCHAR", "VARCHAR")
        every { mockColumnsResultSet.getInt("COLUMN_SIZE") } returnsMany listOf(11, 255, 255)
        every { mockColumnsResultSet.getInt("NULLABLE") } returnsMany listOf(0, 1, 1)
        every { mockColumnsResultSet.getString("IS_AUTOINCREMENT") } returnsMany listOf("YES", "NO", "NO")
        
        // Primary Keys ResultSet
        every { mockPrimaryKeysResultSet.next() } returnsMany listOf(true, false)
        every { mockPrimaryKeysResultSet.getString("COLUMN_NAME") } returns "id"
        
        // Foreign Keys ResultSet
        every { mockForeignKeysResultSet.next() } returns false
        
        // Indexes ResultSet
        every { mockIndexesResultSet.next() } returns false
    }
    
    private fun setupMockResultSetsForMultipleTables() {
        // Tables ResultSet
        every { mockTablesResultSet.next() } returnsMany listOf(true, true, false)
        every { mockTablesResultSet.getString("TABLE_NAME") } returnsMany listOf("users", "orders")
        every { mockTablesResultSet.getString("TABLE_TYPE") } returnsMany listOf("TABLE", "TABLE")
        
        // Columns ResultSet
        every { mockColumnsResultSet.next() } returnsMany listOf(true, true, true, true, true, false)
        every { mockColumnsResultSet.getString("TABLE_NAME") } returnsMany listOf("users", "users", "users", "orders", "orders")
        every { mockColumnsResultSet.getString("COLUMN_NAME") } returnsMany listOf("id", "name", "email", "id", "user_id")
        every { mockColumnsResultSet.getString("TYPE_NAME") } returnsMany listOf("INT", "VARCHAR", "VARCHAR", "INT", "INT")
        every { mockColumnsResultSet.getInt("COLUMN_SIZE") } returnsMany listOf(11, 255, 255, 11, 11)
        every { mockColumnsResultSet.getInt("NULLABLE") } returnsMany listOf(0, 1, 1, 0, 0)
        every { mockColumnsResultSet.getString("IS_AUTOINCREMENT") } returnsMany listOf("YES", "NO", "NO", "YES", "NO")
        
        // Primary Keys ResultSet
        every { mockPrimaryKeysResultSet.next() } returnsMany listOf(true, true, false)
        every { mockPrimaryKeysResultSet.getString("TABLE_NAME") } returnsMany listOf("users", "orders")
        every { mockPrimaryKeysResultSet.getString("COLUMN_NAME") } returnsMany listOf("id", "id")
        
        // Foreign Keys ResultSet
        every { mockForeignKeysResultSet.next() } returnsMany listOf(true, false)
        every { mockForeignKeysResultSet.getString("FKTABLE_NAME") } returns "orders"
        every { mockForeignKeysResultSet.getString("FKCOLUMN_NAME") } returns "user_id"
        every { mockForeignKeysResultSet.getString("PKTABLE_NAME") } returns "users"
        every { mockForeignKeysResultSet.getString("PKCOLUMN_NAME") } returns "id"
        
        // Indexes ResultSet
        every { mockIndexesResultSet.next() } returns false
    }
    
    private fun setupEmptyMockResultSets() {
        every { mockTablesResultSet.next() } returns false
        every { mockColumnsResultSet.next() } returns false
        every { mockPrimaryKeysResultSet.next() } returns false
        every { mockForeignKeysResultSet.next() } returns false
        every { mockIndexesResultSet.next() } returns false
    }
}