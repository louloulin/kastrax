package ai.kastrax.a2x.adapter

import ai.kastrax.a2x.a2x
import ai.kastrax.a2x.model.*
import ai.kastrax.datasource.common.DatabaseConnector
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 数据库实体适配器测试
 */
class DatabaseEntityAdapterTest {
    /**
     * A2X 实例
     */
    private val a2x = a2x {
        // 配置服务器
        server {
            port = 8080
            enableCors = true
        }
    }

    /**
     * 模拟数据库连接器
     */
    private val mockDatabaseConnector = mockk<DatabaseConnector>()

    /**
     * 数据库实体
     */
    private lateinit var databaseEntity: ai.kastrax.a2x.entity.Entity

    @BeforeEach
    fun setUp() {
        // 配置模拟数据库连接器
        coEvery { mockDatabaseConnector.sourceName } returns "test-database"
        coEvery { mockDatabaseConnector.isConnected() } returns true
        coEvery { mockDatabaseConnector.connect() } returns true
        coEvery { mockDatabaseConnector.disconnect() } returns true
        coEvery { mockDatabaseConnector.executeQuery(any(), any()) } returns listOf(
            mapOf(
                "id" to 1,
                "name" to "Test User",
                "email" to "test@example.com"
            ),
            mapOf(
                "id" to 2,
                "name" to "Another User",
                "email" to "another@example.com"
            )
        )
        coEvery { mockDatabaseConnector.executeUpdate(any(), any()) } returns 2

        // 创建数据库实体
        val adapter = DatabaseEntityAdapter()
        databaseEntity = adapter.adapt(mockDatabaseConnector)

        // 启动实体
        databaseEntity.start()
    }

    @AfterEach
    fun tearDown() {
        // 停止实体
        databaseEntity.stop()
    }

    @Test
    fun `test entity card`() {
        // 获取实体卡片
        val entityCard = databaseEntity.getEntityCard()

        // 验证实体卡片
        assertNotNull(entityCard, "实体卡片不应为空")
        assertEquals("test-database", entityCard.id, "实体 ID 应匹配")
        assertEquals("test-database", entityCard.name, "实体名称应匹配")
        assertEquals(EntityType.SYSTEM, entityCard.type, "实体类型应为 SYSTEM")
    }

    @Test
    fun `test capabilities`() {
        // 获取能力
        val capabilities = databaseEntity.getCapabilities()

        // 验证能力
        assertTrue(capabilities.isNotEmpty(), "实体应有能力")
        assertTrue(capabilities.any { it.id == "query" }, "实体应有查询能力")
        assertTrue(capabilities.any { it.id == "execute" }, "实体应有执行能力")
        assertTrue(capabilities.any { it.id == "metadata" }, "实体应有元数据能力")
    }

    @Test
    fun `test query capability`() = runBlocking {
        // 创建查询请求
        val request = InvokeRequest(
            id = "query-request",
            source = a2x.createLocalEntityReference("test-client", EntityType.AGENT),
            target = a2x.createLocalEntityReference("test-database", EntityType.SYSTEM),
            capabilityId = "query",
            parameters = mapOf(
                "sql" to JsonPrimitive("SELECT * FROM users")
            )
        )

        // 调用查询能力
        val response = databaseEntity.invoke(request)

        // 验证响应
        assertNotNull(response, "响应不应为空")
        assertEquals("query-request", response.id, "响应 ID 应匹配")
        assertEquals("test-database", response.source.id, "源实体 ID 应匹配")
        assertEquals("test-client", response.target.id, "目标实体 ID 应匹配")
        assertNotNull(response.result, "响应结果不应为空")
    }

    @Test
    fun `test execute capability`() = runBlocking {
        // 创建执行请求
        val request = InvokeRequest(
            id = "execute-request",
            source = a2x.createLocalEntityReference("test-client", EntityType.AGENT),
            target = a2x.createLocalEntityReference("test-database", EntityType.SYSTEM),
            capabilityId = "execute",
            parameters = mapOf(
                "sql" to JsonPrimitive("UPDATE users SET name = 'New Name' WHERE id = 1")
            )
        )

        // 调用执行能力
        val response = databaseEntity.invoke(request)

        // 验证响应
        assertNotNull(response, "响应不应为空")
        assertEquals("execute-request", response.id, "响应 ID 应匹配")
        assertEquals("test-database", response.source.id, "源实体 ID 应匹配")
        assertEquals("test-client", response.target.id, "目标实体 ID 应匹配")
        assertNotNull(response.result, "响应结果不应为空")
    }

    @Test
    fun `test metadata capability`() = runBlocking {
        // 创建元数据请求
        val request = InvokeRequest(
            id = "metadata-request",
            source = a2x.createLocalEntityReference("test-client", EntityType.AGENT),
            target = a2x.createLocalEntityReference("test-database", EntityType.SYSTEM),
            capabilityId = "metadata",
            parameters = mapOf(
                "type" to JsonPrimitive("tables")
            )
        )

        // 调用元数据能力
        val response = databaseEntity.invoke(request)

        // 验证响应
        assertNotNull(response, "响应不应为空")
        assertEquals("metadata-request", response.id, "响应 ID 应匹配")
        assertEquals("test-database", response.source.id, "源实体 ID 应匹配")
        assertEquals("test-client", response.target.id, "目标实体 ID 应匹配")
        assertNotNull(response.result, "响应结果不应为空")
    }

    @Test
    fun `test query status`() = runBlocking {
        // 创建状态查询请求
        val request = QueryRequest(
            id = "status-request",
            source = a2x.createLocalEntityReference("test-client", EntityType.AGENT),
            target = a2x.createLocalEntityReference("test-database", EntityType.SYSTEM),
            queryType = "status"
        )

        // 查询状态
        val response = databaseEntity.query(request)

        // 验证状态响应
        assertNotNull(response, "状态响应不应为空")
        assertEquals("status-request", response.id, "响应 ID 应匹配")
        assertEquals("test-database", response.source.id, "源实体 ID 应匹配")
        assertEquals("test-client", response.target.id, "目标实体 ID 应匹配")
        assertNotNull(response.result, "响应结果不应为空")
    }
}
