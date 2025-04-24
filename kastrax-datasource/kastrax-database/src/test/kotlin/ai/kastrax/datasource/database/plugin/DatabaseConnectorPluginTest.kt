package ai.kastrax.datasource.database.plugin

import ai.kastrax.core.plugin.Connector
import ai.kastrax.core.plugin.ConnectorStatus
import ai.kastrax.datasource.database.plugin.connectors.MongoConnector
import ai.kastrax.datasource.database.plugin.connectors.PostgresConnector
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName

@DisplayName("数据库连接器插件测试")
class DatabaseConnectorPluginTest {
    
    private lateinit var plugin: DatabaseConnectorPlugin
    
    @BeforeEach
    fun setup() {
        plugin = DatabaseConnectorPlugin()
    }
    
    @Test
    @DisplayName("测试获取连接器类型")
    fun testGetConnectorTypes() {
        val types = plugin.getConnectorTypes()
        
        assertEquals(2, types.size, "应该有两种连接器类型")
        
        val postgresType = types.find { it.id == "postgres" }
        assertNotNull(postgresType, "应该有PostgreSQL连接器类型")
        assertEquals("PostgreSQL", postgresType?.name)
        
        val mongoType = types.find { it.id == "mongodb" }
        assertNotNull(mongoType, "应该有MongoDB连接器类型")
        assertEquals("MongoDB", mongoType?.name)
    }
    
    @Test
    @DisplayName("测试创建PostgreSQL连接器")
    fun testCreatePostgresConnector() {
        val config = mapOf(
            "host" to "localhost",
            "port" to "5432",
            "database" to "testdb",
            "username" to "testuser",
            "password" to "testpass",
            "schema" to "public"
        )
        
        val connector = plugin.createConnector("postgres", config)
        
        assertNotNull(connector, "应该创建PostgreSQL连接器")
        assertTrue(connector is PostgresConnector, "应该是PostgreSQL连接器类型")
        
        val postgresConnector = connector as PostgresConnector
        assertEquals("localhost", postgresConnector.host)
        assertEquals(5432, postgresConnector.port)
        assertEquals("testdb", postgresConnector.database)
        assertEquals("testuser", postgresConnector.username)
        assertEquals("testpass", postgresConnector.password)
        assertEquals("public", postgresConnector.schema)
        assertEquals(ConnectorStatus.CREATED, postgresConnector.status)
    }
    
    @Test
    @DisplayName("测试创建MongoDB连接器")
    fun testCreateMongoConnector() {
        val config = mapOf(
            "connectionString" to "mongodb://localhost:27017",
            "database" to "testdb"
        )
        
        val connector = plugin.createConnector("mongodb", config)
        
        assertNotNull(connector, "应该创建MongoDB连接器")
        assertTrue(connector is MongoConnector, "应该是MongoDB连接器类型")
        
        val mongoConnector = connector as MongoConnector
        assertEquals("mongodb://localhost:27017", mongoConnector.connectionString)
        assertEquals("testdb", mongoConnector.database)
        assertEquals(ConnectorStatus.CREATED, mongoConnector.status)
    }
    
    @Test
    @DisplayName("测试创建不支持的连接器类型")
    fun testCreateUnsupportedConnector() {
        val config = mapOf(
            "host" to "localhost",
            "port" to "3306",
            "database" to "testdb",
            "username" to "testuser",
            "password" to "testpass"
        )
        
        val connector = plugin.createConnector("mysql", config)
        
        assertNull(connector, "不支持的连接器类型应该返回null")
    }
    
    @Test
    @DisplayName("测试连接器元数据")
    fun testConnectorMetadata() {
        val config = mapOf(
            "host" to "localhost",
            "port" to "5432",
            "database" to "testdb",
            "username" to "testuser",
            "password" to "testpass",
            "schema" to "public"
        )
        
        val connector = plugin.createConnector("postgres", config) as Connector
        val metadata = connector.getMetadata()
        
        assertEquals(connector.id, metadata.id)
        assertEquals(connector.name, metadata.name)
        assertEquals(connector.description, metadata.description)
        assertEquals(connector.type, metadata.type)
        assertEquals(connector.status, metadata.status)
        
        // 确保敏感信息已被移除
        assertFalse(metadata.config.containsValue("testpass"), "密码不应该出现在元数据中")
    }
    
    @Test
    @DisplayName("测试连接器操作")
    fun testConnectorOperations() {
        val config = mapOf(
            "host" to "localhost",
            "port" to "5432",
            "database" to "testdb",
            "username" to "testuser",
            "password" to "testpass",
            "schema" to "public"
        )
        
        val connector = plugin.createConnector("postgres", config) as Connector
        val operations = connector.getOperations()
        
        assertTrue(operations.isNotEmpty(), "应该有操作")
        
        val queryOp = operations.find { it.id == "query" }
        assertNotNull(queryOp, "应该有查询操作")
        assertTrue(queryOp?.supportsStreaming ?: false, "查询操作应该支持流式执行")
        
        val updateOp = operations.find { it.id == "update" }
        assertNotNull(updateOp, "应该有更新操作")
    }
}
