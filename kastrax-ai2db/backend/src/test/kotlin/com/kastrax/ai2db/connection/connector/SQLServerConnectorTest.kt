package com.kastrax.ai2db.connection.connector

import com.kastrax.ai2db.connection.model.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class SQLServerConnectorTest {

    @MockK
    private lateinit var dataSource: SingleConnectionDataSource

    @MockK
    private lateinit var connection: Connection

    @MockK
    private lateinit var statement: Statement

    @MockK
    private lateinit var preparedStatement: PreparedStatement

    @MockK
    private lateinit var resultSet: ResultSet

    @MockK
    private lateinit var metaData: DatabaseMetaData

    @MockK
    private lateinit var jdbcClient: JdbcClient

    private lateinit var connector: SQLServerConnector

    private val testConfig = ConnectionConfig(
        name = "Test SQL Server",
        type = DatabaseType.SQLSERVER,
        host = "localhost",
        port = 1433,
        database = "test_db",
        username = "test_user",
        password = "test_password"
    )

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        connector = SQLServerConnector()

        // Mock createDataSource method
        mockkStatic("org.springframework.jdbc.core.simple.JdbcClientKt")
        every { JdbcClient.create(any()) } returns jdbcClient

        // Setup mocks for default behavior
        every { dataSource.connection } returns connection
        every { connection.createStatement() } returns statement
        every { connection.metaData } returns metaData
        every { statement.executeQuery(any()) } returns resultSet
        every { resultSet.next() } returns true andThen false
        every { resultSet.getInt(1) } returns 1

        // Mock specific behaviors for metaData
        every { metaData.databaseProductName } returns "Microsoft SQL Server"
        every { metaData.databaseProductVersion } returns "15.0.4153.1"
        every { metaData.driverName } returns "Microsoft JDBC Driver for SQL Server"
        every { metaData.driverVersion } returns "9.4.1"
    }

    @Test
    fun `test connect successfully`() = runBlocking {
        // Mock createDataSource to return our mocked dataSource
        mockkObject(connector)
        every { connector["createDataSource"](any()) } returns dataSource

        // Mock JdbcClient query operations
        every {
            jdbcClient.sql(any())
                .query(Int::class.java)
                .single()
        } returns 1

        // Execute the method under test
        val result = connector.connect(testConfig)

        // Verify
        assertNotNull(result)
        assertEquals(ConnectionStatus.CONNECTED, result.status)
        assertEquals(testConfig, result.config)
        assertNotNull(result.connectedAt)
    }

    @Test
    fun `test disconnect successfully`() = runBlocking {
        // Setup
        every { dataSource.destroy() } just Runs

        // Create a test connection object
        val testConnection = com.kastrax.ai2db.connection.model.Connection(
            config = testConfig,
            dataSource = dataSource
        )

        // Execute
        val result = connector.disconnect(testConnection)

        // Verify
        assertTrue(result)
        verify { dataSource.destroy() }
    }

    @Test
    fun `test executeUpdate returns correct result`() = runBlocking {
        // Setup for prepared statement
        every {
            connection.prepareStatement(any(), PreparedStatement.RETURN_GENERATED_KEYS)
        } returns preparedStatement
        every { preparedStatement.setObject(any(), any()) } just Runs
        every { preparedStatement.executeUpdate() } returns 2
        every { preparedStatement.updateCount } returns 2
        every { preparedStatement.generatedKeys } returns resultSet
        every { resultSet.next() } returns true andThen true andThen false
        every { resultSet.getObject(1) } returns 100 andThen 101

        // Create a test connection object
        val testConnection = com.kastrax.ai2db.connection.model.Connection(
            config = testConfig,
            dataSource = dataSource
        )

        val updateQuery = "UPDATE users SET name = ? WHERE id = ?"
        val params = listOf("New Name", 1)

        // Execute the update
        val result = connector.executeUpdate(testConnection, updateQuery, params)

        // Verify
        assertNotNull(result)
        assertTrue(result.success)
        assertEquals(2, result.affectedRows)
        assertEquals(2, result.generatedKeys.size)
        assertEquals(100, result.generatedKeys[0])
        assertEquals(101, result.generatedKeys[1])
    }

    @Test
    fun `test executeUpdate handles failure gracefully`() = runBlocking {
        // Setup for failure
        every {
            connection.prepareStatement(any(), PreparedStatement.RETURN_GENERATED_KEYS)
        } returns preparedStatement
        every { preparedStatement.setObject(any(), any()) } just Runs
        every { preparedStatement.executeUpdate() } throws RuntimeException("Update failed")

        // Create a test connection object
        val testConnection = com.kastrax.ai2db.connection.model.Connection(
            config = testConfig,
            dataSource = dataSource
        )

        val updateQuery = "UPDATE invalid_table SET name = ?"
        val params = listOf("New Name")

        // Execute the update
        val result = connector.executeUpdate(testConnection, updateQuery, params)

        // Verify
        assertNotNull(result)
        assertEquals(false, result.success)
        assertEquals("Update failed", result.error)
        assertEquals(0, result.affectedRows)
    }

    @Test
    fun `test testConnection returns appropriate status`() = runBlocking {
        // Mock createDataSource to return our mocked dataSource
        mockkObject(connector)

        // Test success case
        every { connector["createDataSource"](any()) } returns dataSource
        every {
            jdbcClient.sql(any())
                .query(Int::class.java)
                .single()
        } returns 1
        every { dataSource.destroy() } just Runs

        var status = connector.testConnection(testConfig)
        assertEquals(ConnectionStatus.CONNECTED, status)

        // Test failure case
        every {
            jdbcClient.sql(any())
                .query(Int::class.java)
                .single()
        } throws RuntimeException("Connection failed")

        status = connector.testConnection(testConfig)
        assertEquals(ConnectionStatus.FAILED, status)
    }

    @Test
    fun `test transaction management`() = runBlocking {
        // Setup connection behavior for transactions
        every { connection.autoCommit = any() } just Runs
        every { connection.commit() } just Runs
        every { connection.rollback() } just Runs

        val testConnection = com.kastrax.ai2db.connection.model.Connection(
            config = testConfig,
            dataSource = dataSource
        )

        // Test begin transaction
        val tx = connector.beginTransaction(testConnection)
        assertNotNull(tx)
        assertTrue(tx.isActive)
        verify { connection.autoCommit = false }

        // Test commit transaction
        connector.commitTransaction(tx)
        verify { connection.commit() }
        verify { connection.autoCommit = true }

        // Reset for rollback test
        clearMocks(connection)
        every { connection.autoCommit = any() } just Runs
        every { connection.rollback() } just Runs

        // Test rollback transaction
        val tx2 = connector.beginTransaction(testConnection)
        connector.rollbackTransaction(tx2)
        verify { connection.rollback() }
        verify { connection.autoCommit = true }
    }

    @Test
    fun `test executeQuery with parameters`() = runBlocking {
        // Setup for query with parameters
        val sqlOperationMock = mockk<JdbcClient.StatementSpec>()
        val sqlOperationWithParamMock = mockk<JdbcClient.StatementSpec>()
        val queryMapperMock = mockk<JdbcClient.ResultSetExtractor<Any>>()

        // Setup query chain
        every { jdbcClient.sql(any()) } returns sqlOperationMock
        every { sqlOperationMock.param(1, 100) } returns sqlOperationWithParamMock
        every { sqlOperationWithParamMock.query(any<(ResultSet, Int) -> Map<String, Any?>>()) } returns queryMapperMock

        // Capture the row mapper function
        val rowMapperSlot = slot<(ResultSet, Int) -> Map<String, Any?>>()
        every {
            sqlOperationWithParamMock.query(capture(rowMapperSlot))
        } answers {
            // Simulate metadata extraction
            val metaData = mockk<java.sql.ResultSetMetaData>()
            every { resultSet.metaData } returns metaData
            every { metaData.columnCount } returns 2
            every { metaData.getColumnName(1) } returns "id"
            every { metaData.getColumnName(2) } returns "name"
            every { metaData.getColumnLabel(1) } returns "id"
            every { metaData.getColumnLabel(2) } returns "name"
            every { metaData.getColumnClassName(1) } returns "java.lang.Integer"
            every { metaData.getColumnClassName(2) } returns "java.lang.String"
            every { metaData.getColumnTypeName(1) } returns "INT"
            every { metaData.getColumnTypeName(2) } returns "VARCHAR"

            // Simulate column value retrieval
            every { resultSet.getObject("id") } returns 100
            every { resultSet.getObject("name") } returns "Test User"

            // Call the mapper function on the resultSet
            val row = rowMapperSlot.captured(resultSet, 1)

            // Return a list with one row
            queryMapperMock
        }

        every { queryMapperMock.list() } returns listOf(
            mapOf("id" to 100, "name" to "Test User")
        )

        // Create a test connection
        val testConnection = com.kastrax.ai2db.connection.model.Connection(
            config = testConfig,
            dataSource = dataSource
        )

        // Execute query
        val query = "SELECT * FROM users WHERE id = ?"
        val params = listOf(100)
        val result = connector.executeQuery(testConnection, query, params, 30000)

        // Verify
        assertNotNull(result)
        assertTrue(result.success)
        assertEquals(1, result.rowCount)
        assertTrue(result.columns.any { it.name == "id" })
        assertTrue(result.columns.any { it.name == "name" })
        assertEquals(100, result.rows[0]["id"])
        assertEquals("Test User", result.rows[0]["name"])
    }
}
