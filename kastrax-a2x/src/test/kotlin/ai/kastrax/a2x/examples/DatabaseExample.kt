package ai.kastrax.a2x.examples

import ai.kastrax.a2x.a2x
import ai.kastrax.a2x.model.EntityType
import ai.kastrax.a2x.model.InvokeRequest
import ai.kastrax.datasource.common.DatabaseConnector
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import io.mockk.coEvery
import io.mockk.mockk

/**
 * 数据库示例
 */
fun main() = runBlocking {
    // 创建模拟数据库连接器
    val mockDatabaseConnector = mockk<DatabaseConnector>()

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

    // 创建 A2X 实例
    val a2xInstance = a2x {
        // 注册数据库连接器
        database(mockDatabaseConnector)

        // 配置服务器
        server {
            // 使用随机可用端口
            port = ai.kastrax.core.utils.NetworkUtils.findAvailablePort()
            enableCors = true
        }
    }

    // 获取数据库实体
    val databaseEntity = a2xInstance.getEntity("test-database")
    if (databaseEntity != null) {
        println("数据库实体: ${databaseEntity.getEntityCard().name}")
        println("数据库能力: ${databaseEntity.getCapabilities().map { it.name }}")

        // 创建查询请求
        val queryRequest = InvokeRequest(
            id = "query-request",
            source = a2xInstance.createLocalEntityReference("test-client", EntityType.AGENT),
            target = a2xInstance.createLocalEntityReference("test-database", EntityType.SYSTEM),
            capabilityId = "query",
            parameters = mapOf(
                "sql" to JsonPrimitive("SELECT * FROM users")
            )
        )

        // 调用查询能力
        val queryResponse = databaseEntity.invoke(queryRequest)
        println("查询响应: ${queryResponse.result}")

        // 创建执行请求
        val executeRequest = InvokeRequest(
            id = "execute-request",
            source = a2xInstance.createLocalEntityReference("test-client", EntityType.AGENT),
            target = a2xInstance.createLocalEntityReference("test-database", EntityType.SYSTEM),
            capabilityId = "execute",
            parameters = mapOf(
                "sql" to JsonPrimitive("UPDATE users SET name = 'New Name' WHERE id = 1")
            )
        )

        // 调用执行能力
        val executeResponse = databaseEntity.invoke(executeRequest)
        println("执行响应: ${executeResponse.result}")

        // 创建元数据请求
        val metadataRequest = InvokeRequest(
            id = "metadata-request",
            source = a2xInstance.createLocalEntityReference("test-client", EntityType.AGENT),
            target = a2xInstance.createLocalEntityReference("test-database", EntityType.SYSTEM),
            capabilityId = "metadata",
            parameters = mapOf(
                "type" to JsonPrimitive("tables")
            )
        )

        // 调用元数据能力
        val metadataResponse = databaseEntity.invoke(metadataRequest)
        println("元数据响应: ${metadataResponse.result}")
    } else {
        println("未找到数据库实体")
    }

    // 停止服务器
    a2xInstance.stopServer()
}
