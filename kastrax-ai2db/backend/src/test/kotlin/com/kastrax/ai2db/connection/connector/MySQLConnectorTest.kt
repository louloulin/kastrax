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
import java.sql.ResultSet
import java.sql.Statement
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class MySQLConnectorTest {

    @MockK
    private lateinit var dataSource: SingleConnectionDataSource

    @MockK
    private lateinit var connection: Connection

    @MockK
    private lateinit var statement: Statement

    @MockK
    private lateinit var resultSet: ResultSet

    @MockK
    private lateinit var metaData: DatabaseMetaData

    @MockK
    private lateinit var jdbcClient: JdbcClient

    private lateinit var connector: MySQLConnector

    private val testConfig = ConnectionConfig(
        name = "Test MySQL",
        type = DatabaseType.MYSQL,
        host = "localhost",
        port = 3306,
        database = "test_db",
        username = "test_user",
        password = "test_password"
    )

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        connector = MySQLConnector()

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
        every { metaData.databaseProductName } returns "MySQL"
        every { metaData.databaseProductVersion } returns "8.0.28"
        every { metaData.driverName } returns "MySQL Connector/J"
        every { metaData.driverVersion } returns "8.0.28"
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
    fun `test connect throws exception on failure`() = runBlocking {
        // Mock createDataSource to return our mocked dataSource
        mockkObject(connector)
        every { connector["createDataSource"](any()) } returns dataSource

        // Mock JdbcClient to throw exception
        every {
            jdbcClient.sql(any())
                .query(Int::class.java)
                .single()
        } throws RuntimeException("Connection failed")

        // Execute and verify exception is thrown
        try {
            connector.connect(testConfig)
            // If we get here, the test should fail
            assert(false) { "Expected ConnectionException was not thrown" }
        } catch (e: ConnectionException) {
            // Verify the exception has the expected message
            assertTrue(e.message?.contains("Failed to connect to MySQL database") == true)
        }
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
    fun `test testConnection returns CONNECTED on success`() = runBlocking {
        // Mock createDataSource to return our mocked dataSource
        mockkObject(connector)
        every { connector["createDataSource"](any()) } returns dataSource

        // Mock JdbcClient query operations
        every {
            jdbcClient.sql(any())
                .query(Int::class.java)
                .single()
        } returns 1

        every { dataSource.destroy() } just Runs

        // Execute
        val result = connector.testConnection(testConfig)

        // Verify
        assertEquals(ConnectionStatus.CONNECTED, result)
    }

    @Test
    fun `test testConnection returns FAILED on exception`() = runBlocking {
        // Mock createDataSource to return our mocked dataSource
        mockkObject(connector)
        every { connector["createDataSource"](any()) } returns dataSource

        // Mock JdbcClient to throw exception
        every {
            jdbcClient.sql(any())
                .query(Int::class.java)
                .single()
        } throws RuntimeException("Connection failed")

        // Execute
        val result = connector.testConnection(testConfig)

        // Verify
        assertEquals(ConnectionStatus.FAILED, result)
    }
}
