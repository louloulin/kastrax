package ai.kastrax.a2x.adapter

import ai.kastrax.a2x.a2x
import ai.kastrax.a2x.model.*
import io.ktor.client.plugins.*
import io.ktor.http.*

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * API 实体适配器测试
 */
class APIEntityAdapterTest {
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
     * API 实体
     */
    private lateinit var apiEntity: ai.kastrax.a2x.entity.Entity

    @BeforeEach
    fun setUp() {
        // 创建 API 配置
        val apiConfig = APIConfig(
            id = "test-api",
            name = "测试 API",
            description = "这是一个测试 API",
            baseUrl = "https://api.example.com",
            endpoints = listOf(
                APIEndpoint(
                    id = "get-user",
                    name = "获取用户",
                    description = "获取用户信息",
                    method = "GET",
                    path = "/users/{id}",
                    parameters = listOf(
                        APIParameter(
                            name = "id",
                            type = "string",
                            description = "用户 ID",
                            required = true,
                            location = "path"
                        )
                    )
                ),
                APIEndpoint(
                    id = "create-user",
                    name = "创建用户",
                    description = "创建新用户",
                    method = "POST",
                    path = "/users",
                    parameters = listOf(
                        APIParameter(
                            name = "name",
                            type = "string",
                            description = "用户名",
                            required = true,
                            location = "body"
                        ),
                        APIParameter(
                            name = "email",
                            type = "string",
                            description = "电子邮件",
                            required = true,
                            location = "body"
                        )
                    )
                ),
                APIEndpoint(
                    id = "update-user",
                    name = "更新用户",
                    description = "更新用户信息",
                    method = "PUT",
                    path = "/users/{id}",
                    parameters = listOf(
                        APIParameter(
                            name = "id",
                            type = "string",
                            description = "用户 ID",
                            required = true,
                            location = "path"
                        ),
                        APIParameter(
                            name = "name",
                            type = "string",
                            description = "用户名",
                            required = false,
                            location = "body"
                        ),
                        APIParameter(
                            name = "email",
                            type = "string",
                            description = "电子邮件",
                            required = false,
                            location = "body"
                        )
                    )
                ),
                APIEndpoint(
                    id = "delete-user",
                    name = "删除用户",
                    description = "删除用户",
                    method = "DELETE",
                    path = "/users/{id}",
                    parameters = listOf(
                        APIParameter(
                            name = "id",
                            type = "string",
                            description = "用户 ID",
                            required = true,
                            location = "path"
                        )
                    )
                )
            ),
            defaultHeaders = mapOf(
                "Content-Type" to "application/json",
                "Accept" to "application/json"
            ),
            authentication = Authentication(
                type = AuthenticationType.API_KEY,
                metadata = mapOf(
                    "key_name" to "api_key",
                    "key_value" to "test_api_key",
                    "key_location" to "header"
                )
            ),
            timeout = 30000
        )

        // 创建 API 实体
        val adapter = APIEntityAdapter()
        apiEntity = adapter.adapt(apiConfig)

        // 启动实体
        apiEntity.start()
    }

    @AfterEach
    fun tearDown() {
        // 停止实体
        apiEntity.stop()
    }

    @Test
    fun `test entity card`() {
        // 获取实体卡片
        val entityCard = apiEntity.getEntityCard()

        // 验证实体卡片
        assertNotNull(entityCard, "实体卡片不应为空")
        assertEquals("test-api", entityCard.id, "实体 ID 应匹配")
        assertEquals("测试 API", entityCard.name, "实体名称应匹配")
        assertEquals(EntityType.SYSTEM, entityCard.type, "实体类型应为 SYSTEM")
        assertEquals("https://api.example.com", entityCard.endpoint, "实体端点应匹配")
    }

    @Test
    fun `test capabilities`() {
        // 获取能力
        val capabilities = apiEntity.getCapabilities()

        // 验证能力
        assertTrue(capabilities.isNotEmpty(), "实体应有能力")
        assertTrue(capabilities.any { it.id == "get-user" }, "实体应有获取用户能力")
        assertTrue(capabilities.any { it.id == "create-user" }, "实体应有创建用户能力")
        assertTrue(capabilities.any { it.id == "update-user" }, "实体应有更新用户能力")
        assertTrue(capabilities.any { it.id == "delete-user" }, "实体应有删除用户能力")
    }

    @Test
    fun `test query status`() = runBlocking {
        // 创建状态查询请求
        val request = QueryRequest(
            id = "status-request",
            source = a2x.createLocalEntityReference("test-client", EntityType.AGENT),
            target = a2x.createLocalEntityReference("test-api", EntityType.SYSTEM),
            queryType = "status"
        )

        // 查询状态
        val response = apiEntity.query(request)

        // 验证状态响应
        assertNotNull(response, "状态响应不应为空")
        assertEquals("status-request", response.id, "响应 ID 应匹配")
        assertEquals("test-api", response.source.id, "源实体 ID 应匹配")
        assertEquals("test-client", response.target.id, "目标实体 ID 应匹配")
        assertNotNull(response.result, "响应结果不应为空")
    }
}
