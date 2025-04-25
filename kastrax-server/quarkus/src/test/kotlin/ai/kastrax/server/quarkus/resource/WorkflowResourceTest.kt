package ai.kastrax.server.quarkus.resource

import ai.kastrax.server.common.api.WorkflowApi
import ai.kastrax.server.common.model.Edge
import ai.kastrax.server.common.model.Node
import ai.kastrax.server.common.model.Position
import ai.kastrax.server.common.model.Workflow
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectMock
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test

import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@QuarkusTest
class WorkflowResourceTest {

    @InjectMock
    lateinit var workflowApi: WorkflowApi

    @Test
    fun `test get workflow`() {
        // 准备测试数据
        val workflowId = UUID.randomUUID().toString()
        val workflow = createTestWorkflow(workflowId)

        // 模拟API调用
        whenever(workflowApi.getWorkflow(workflowId)).thenReturn(CompletableFuture.completedFuture(workflow))

        // 执行测试
        given()
            .pathParam("id", workflowId)
            .`when`().get("/workflows/{id}")
            .then()
            .statusCode(200)
            .body(containsString(workflowId))
            .body(containsString("Test Workflow"))
    }

    @Test
    fun `test create workflow`() {
        // 准备测试数据
        val workflowId = UUID.randomUUID().toString()
        val workflow = createTestWorkflow(workflowId)

        // 模拟API调用
        whenever(workflowApi.createWorkflow(any())).thenReturn(CompletableFuture.completedFuture(workflow))

        // 执行测试
        given()
            .contentType(ContentType.JSON)
            .body(workflow)
            .`when`().post("/workflows")
            .then()
            .statusCode(201)
            .body(containsString(workflowId))
            .body(containsString("Test Workflow"))
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
