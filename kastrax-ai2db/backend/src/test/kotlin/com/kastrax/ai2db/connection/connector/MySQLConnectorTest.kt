package com.kastrax.ai2db.connection.connector

import com.kastrax.ai2db.connection.model.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Assertions.*
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.sql.Connection as JavaSqlConnection
import javax.sql.DataSource

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MySQLConnectorTest {
    
    private val logger = LoggerFactory.getLogger(MySQLConnectorTest::class.java)
    
    companion object {
        @Container
        val mySQLContainer = MySQLContainer<Nothing>(
            DockerImageName.parse("mysql:8.0")
        ).apply {
            withDatabaseName("testdb")
            withUsername("testuser")
            withPassword("testpass")
            withInitScript("sql/init-test-mysql.sql")
        }
    }
    
    private lateinit var connector: MySQLConnector
    private lateinit var connectionConfig: ConnectionConfig
    private lateinit var dataSource: DataSource
    
    @BeforeAll
    fun setup() {
        mySQLContainer.start()
        
        connectionConfig = ConnectionConfig(
            name = "Test MySQL Connection",
            type = DatabaseType.MYSQL,
            host = mySQLContainer.host,
            port = mySQLContainer.getMappedPort(3306),
            database = mySQLContainer.databaseName,
            username = mySQLContainer.username,
            password = mySQLContainer.password,
            sslEnabled = false
        )
        
        // Create a test data source
        dataSource = SingleConnectionDataSource().apply {
            url = connectionConfig.toJdbcUrl()
            username = connectionConfig.username
            password = connectionConfig.password
            setSuppressClose(true)
            setDriverClassName("com.mysql.cj.jdbc.Driver")
        }
        
        // Initialize database with test tables and data
        createTestTables(dataSource.connection)
        
        connector = MySQLConnector()
    }
    
    @AfterAll
    fun tearDown() {
        (dataSource as? SingleConnectionDataSource)?.destroy()
        mySQLContainer.stop()
    }
    
    @Test
    fun `test connect to MySQL database`() = runBlocking {
        // Execute
        val connection = connector.connect(connectionConfig)
        
        // Verify
        assertNotNull(connection)
        assertNotNull(connection.id)
        assertEquals(connectionConfig, connection.config)
        assertNotNull(connection.dataSource)
        assertTrue(connection.dataSource is DataSource)
        
        // Cleanup
        connector.disconnect(connection)
    }
    
    @Test
    fun `test get database metadata`() = runBlocking {
        // Setup
        val connection = connector.connect(connectionConfig)
        
        // Execute
        val metadata = connector.getMetadata(connection)
        
        // Verify
        assertNotNull(metadata)
        assertEquals(connectionConfig.database, metadata.databaseName)
        assertEquals(DatabaseType.MYSQL, metadata.databaseType)
        assertTrue(metadata.tables.isNotEmpty())
        
        // Verify users table exists
        val usersTable = metadata.tables.find { it.name == "users" }
        assertNotNull(usersTable)
        assertTrue(usersTable!!.columns.isNotEmpty())
        
        // Verify there's a column named 'id' that's a primary key
        val idColumn = usersTable.columns.find { it.name == "id" }
        assertNotNull(idColumn)
        assertTrue(idColumn!!.isPrimaryKey)
        
        // Cleanup
        connector.disconnect(connection)
    }
    
    @Test
    fun `test execute query`() = runBlocking {
        // Setup
        val connection = connector.connect(connectionConfig)
        
        // Execute
        val result = connector.executeQuery(
            connection = connection,
            query = "SELECT id, username, email FROM users WHERE id = ?",
            parameters = listOf(1)
        )
        
        // Verify
        assertNotNull(result)
        assertEquals(1, result.rowCount)
        assertEquals(3, result.columns.size)
        assertEquals("id", result.columns[0].name.lowercase())
        assertEquals("username", result.columns[1].name.lowercase())
        assertEquals("email", result.columns[2].name.lowercase())
        
        val row = result.rows.first()
        assertEquals(1, row[0])
        assertEquals("testuser1", row[1])
        assertEquals("test1@example.com", row[2])
        
        // Cleanup
        connector.disconnect(connection)
    }
    
    @Test
    fun `test execute update`() = runBlocking {
        // Setup
        val connection = connector.connect(connectionConfig)
        
        // Execute
        val result = connector.executeUpdate(
            connection = connection,
            query = "UPDATE users SET email = ? WHERE id = ?",
            parameters = listOf("updated@example.com", 2)
        )
        
        // Verify
        assertNotNull(result)
        assertEquals(1, result.rowsAffected)
        
        // Verify the update actually worked
        val queryResult = connector.executeQuery(
            connection = connection,
            query = "SELECT email FROM users WHERE id = ?",
            parameters = listOf(2)
        )
        
        assertEquals(1, queryResult.rowCount)
        assertEquals("updated@example.com", queryResult.rows.first()[0])
        
        // Cleanup
        connector.disconnect(connection)
    }
    
    /**
     * Helper function to create test tables and insert test data
     */
    private fun createTestTables(connection: JavaSqlConnection) {
        try {
            connection.createStatement().use { stmt ->
                // Create users table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(100) NOT NULL,
                        email VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """)
                
                // Create posts table with foreign key to users
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS posts (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL,
                        title VARCHAR(255) NOT NULL,
                        content TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id)
                    )
                """)
                
                // Insert test users
                stmt.execute("DELETE FROM posts")
                stmt.execute("DELETE FROM users")
                stmt.execute("INSERT INTO users (id, username, email) VALUES (1, 'testuser1', 'test1@example.com')")
                stmt.execute("INSERT INTO users (id, username, email) VALUES (2, 'testuser2', 'test2@example.com')")
                
                // Insert test posts
                stmt.execute("INSERT INTO posts (user_id, title, content) VALUES (1, 'Test Post 1', 'This is test post 1')")
                stmt.execute("INSERT INTO posts (user_id, title, content) VALUES (1, 'Test Post 2', 'This is test post 2')")
                stmt.execute("INSERT INTO posts (user_id, title, content) VALUES (2, 'Test Post 3', 'This is test post 3')")
            }
        } catch (e: Exception) {
            logger.error("Error creating test tables: ${e.message}", e)
            throw e
        }
    }
} 