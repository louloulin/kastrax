package com.kastrax.ai2db.connection.connector

import com.kastrax.ai2db.connection.model.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.sql.Statement
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class PostgreSQLConnectorTest {

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

    private lateinit var connector: PostgreSQLConnector

    private val testConfig = ConnectionConfig(
        name = "Test PostgreSQL",
        type = DatabaseType.POSTGRESQL,
        host = "localhost",
        port = 5432,
        database = "test_db",
        username = "test_user",
        password = "test_password"
    )

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        connector = PostgreSQLConnector()

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
        every { metaData.databaseProductName } returns "PostgreSQL"
        every { metaData.databaseProductVersion } returns "14.5"
        every { metaData.driverName } returns "PostgreSQL JDBC Driver"
        every { metaData.driverVersion } returns "42.5.1"
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
            assertTrue(e.message?.contains("Failed to connect to PostgreSQL database") == true)
        }
    }

    @Test
    fun `test getMetadata returns correct structure`() = runBlocking {
        // Setup
        mockkObject(connector)
        every { connector["createDataSource"](any()) } returns dataSource

        // Create a test connection object
        val testConnection = com.kastrax.ai2db.connection.model.Connection(
            config = testConfig,
            dataSource = dataSource
        )

        // Mock JdbcClient query operations for table list
        val tableRowsSlot = slot<RowMapper<Map<String, Any?>>>()
        every {
            jdbcClient.sql(match { it.contains("table_name") })
                .query(capture(tableRowsSlot))
                .list()
        } returns listOf(
            mapOf(
                "table_name" to "users",
                "table_type" to "BASE TABLE",
                "table_comment" to "User table"
            ),
            mapOf(
                "table_name" to "orders",
                "table_type" to "BASE TABLE",
                "table_comment" to "Orders table"
            ),
            mapOf(
                "table_name" to "order_items",
                "table_type" to "VIEW",
                "table_comment" to "Order items view"
            )
        )

        // Mock methods for getting column details, primary keys, foreign keys, and indexes
        every { connector["getTableColumns"](any(), any(), any()) } returns listOf(
            ColumnMetadata(
                name = "id",
                dataType = "bigint",
                typeName = "int8",
                size = 0,
                nullable = false,
                primaryKey = true,
                autoIncrement = true,
                ordinalPosition = 1
            ),
            ColumnMetadata(
                name = "name",
                dataType = "character varying",
                typeName = "varchar",
                size = 255,
                nullable = true,
                primaryKey = false,
                autoIncrement = false,
                ordinalPosition = 2
            )
        )

        every { connector["getPrimaryKeys"](any(), any(), any()) } returns listOf(
            Index(
                name = "pk_users",
                columnNames = listOf("id"),
                unique = true,
                type = "PRIMARY"
            )
        )

        every { connector["getForeignKeys"](any(), any(), any()) } returns listOf(
            ForeignKey(
                name = "fk_user_id",
                columnNames = listOf("user_id"),
                referencedTableName = "users",
                referencedColumnNames = listOf("id"),
                updateRule = "NO ACTION",
                deleteRule = "CASCADE"
            )
        )

        every { connector["getIndexes"](any(), any(), any()) } returns listOf(
            Index(
                name = "idx_name",
                columnNames = listOf("name"),
                unique = false,
                type = "BTREE"
            )
        )

        // Mock relationship analysis
        every { connector["analyzeRelationships"](any(), any()) } returns listOf(
            Relationship(
                sourceTable = "orders",
                sourceColumns = listOf("user_id"),
                targetTable = "users",
                targetColumns = listOf("id"),
                type = RelationshipType.MANY_TO_ONE,
                name = "fk_user_id",
                foreignKeyName = "fk_user_id"
            )
        )

        // Execute the method under test
        val result = connector.getMetadata(testConnection)

        // Verify
        assertNotNull(result)
        assertEquals("test_db", result.databaseName)
        assertEquals("PostgreSQL", result.databaseProductName)
        assertEquals(2, result.tables.size)
        assertEquals(1, result.views.size)
        assertEquals("users", result.tables[0].name)
        assertEquals("orders", result.tables[1].name)
        assertEquals("order_items", result.views[0].name)
    }

    @Test
    fun `test executeQuery returns correct result`() = runBlocking {
        // Setup
        val testConnection = com.kastrax.ai2db.connection.model.Connection(
            config = testConfig,
            dataSource = dataSource
        )

        val testQuery = "SELECT * FROM users WHERE id = ?"
        val testParams = listOf(1)

        // Mock JdbcClient query operations
        val sqlOperationMock = mockk<JdbcClient.StatementSpec>()
        val queryMapperMock = mockk<JdbcClient.ResultSetExtractor<Any>>()

        every { jdbcClient.sql(testQuery) } returns sqlOperationMock
        every { sqlOperationMock.param(1, 1) } returns sqlOperationMock
        every { sqlOperationMock.query(any<(ResultSet, Int) -> Map<String, Any?>>()) } returns queryMapperMock

        // Mock RowMapper invocation
        val columnsSlot = slot<MutableList<ColumnData>>()
        val rowsSlot = slot<MutableList<Map<String, Any?>>>()

        every { queryMapperMock.list() } answers {
            // Simulate RowMapper execution
            val columns = mutableListOf(
                ColumnData("id", "id", "java.lang.Integer", "INTEGER"),
                ColumnData("name", "name", "java.lang.String", "VARCHAR")
            )
            val rows = mutableListOf(
                mapOf("id" to 1, "name" to "Test User")
            )

            // Capture for later assertion
            columnsSlot.captured = columns
            rowsSlot.captured = rows

            // Return list of rows
            rows
        }

        // Execute the method under test
        val result = connector.executeQuery(testConnection, testQuery, testParams, 30000)

        // Verify
        assertNotNull(result)
        assertTrue(result.success)
        assertEquals(1, result.rowCount)
        assertEquals(2, result.columns.size)
        assertEquals("id", result.columns[0].name)
        assertEquals("name", result.columns[1].name)
        assertEquals(1, result.rows[0]["id"])
        assertEquals("Test User", result.rows[0]["name"])
    }

    @Test
    fun `test transaction lifecycle`() = runBlocking {
        // Setup
        val testConnection = com.kastrax.ai2db.connection.model.Connection(
            config = testConfig,
            dataSource = dataSource
        )

        // Mock Connection behavior for transactions
        every { connection.autoCommit = false } just Runs
        every { connection.commit() } just Runs
        every { connection.rollback() } just Runs
        every { connection.autoCommit = true } just Runs

        // Begin transaction
        val transaction = connector.beginTransaction(testConnection)
        assertNotNull(transaction)
        assertEquals(testConnection, transaction.connection)
        assertTrue(transaction.isActive)
        verify { connection.autoCommit = false }

        // Commit transaction
        connector.commitTransaction(transaction)
        verify { connection.commit() }
        verify { connection.autoCommit = true }

        // Create another transaction for rollback test
        every { connection.autoCommit = false } just Runs
        val transaction2 = connector.beginTransaction(testConnection)

        // Rollback transaction
        connector.rollbackTransaction(transaction2)
        verify { connection.rollback() }
        verify { connection.autoCommit = true }
    }
}
