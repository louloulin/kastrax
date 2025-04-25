package ai.kastrax.server.ktor.routes

import ai.kastrax.server.common.api.WorkflowApi
import ai.kastrax.server.common.model.Edge
import ai.kastrax.server.common.model.Node
import ai.kastrax.server.common.model.Position
import ai.kastrax.server.common.model.Workflow
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkflowRoutesTest : KoinTest {

    private val workflowApi = mock(WorkflowApi::class.java)

    @Test
    fun `test get workflow`() = testApplication {
        // 设置Koin
        startKoin {
            modules(
                module {
                    single { workflowApi }
                }
            )
        }

        try {
            // 准备测试数据
            val workflowId = UUID.randomUUID().toString()
            val workflow = createTestWorkflow(workflowId)

            // 模拟API调用
            `when`(workflowApi.getWorkflow(workflowId)).thenReturn(CompletableFuture.completedFuture(workflow))

            // 执行测试
            val response = client.get("/workflows/$workflowId")

            // 验证结果
            assertEquals(HttpStatusCode.OK, response.status)
            val responseText = response.bodyAsText()
            assertTrue(responseText.contains(workflowId))
            assertTrue(responseText.contains("Test Workflow"))
        } finally {
            // 清理Koin
            stopKoin()
        }
    }

    @Test
    fun `test create workflow`() = testApplication {
        // 设置Koin
        startKoin {
            modules(
                module {
                    single { workflowApi }
                }
            )
        }

        try {
            // 准备测试数据
            val workflowId = UUID.randomUUID().toString()
            val workflow = createTestWorkflow(workflowId)

            // 模拟API调用
            `when`(workflowApi.createWorkflow(workflow)).thenReturn(CompletableFuture.completedFuture(workflow))

            // 执行测试
            val response = client.post("/workflows") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(workflow))
            }

            // 验证结果
            assertEquals(HttpStatusCode.Created, response.status)
            val responseText = response.bodyAsText()
            assertTrue(responseText.contains(workflowId))
            assertTrue(responseText.contains("Test Workflow"))
        } finally {
            // 清理Koin
            stopKoin()
        }
    }

    // 创建测试工作流
    private fun createTestWorkflow(id: String = UUID.randomUUID().toString()): Workflow {
        return Workflow(
            id = id,
            name = "Test Workflow",
            description = "Test Description",
            version = "1.0.0",
            nodes = listOf(
                Node(
                    id = "node1",
                    type = "task",
                    label = "Task 1",
                    position = Position(x = 100.0, y = 100.0),
                    data = buildJsonObject { put("key", "value") },
                    style = buildJsonObject { put("color", "blue") }
                )
            ),
            edges = listOf(
                Edge(
                    id = "edge1",
                    source = "node1",
                    target = "node2",
                    label = "Edge 1",
                    data = buildJsonObject { put("key", "value") },
                    style = buildJsonObject { put("color", "blue") }
                )
            ),
            metadata = buildJsonObject { put("key", "value") },
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}
