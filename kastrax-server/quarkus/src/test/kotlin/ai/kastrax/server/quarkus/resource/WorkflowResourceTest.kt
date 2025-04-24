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
import org.mockito.Mockito.`when`
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

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
        `when`(workflowApi.getWorkflow(workflowId)).thenReturn(CompletableFuture.completedFuture(workflow))
        
        // 执行测试
        given()
            .pathParam("id", workflowId)
            .`when`().get("/api/workflows/{id}")
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
        `when`(workflowApi.createWorkflow(workflow)).thenReturn(CompletableFuture.completedFuture(workflow))
        
        // 执行测试
        given()
            .contentType(ContentType.JSON)
            .body(workflow)
            .`when`().post("/api/workflows")
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
                    data = mapOf("key" to "value"),
                    style = mapOf("color" to "blue")
                )
            ),
            edges = listOf(
                Edge(
                    id = "edge1",
                    source = "node1",
                    target = "node2",
                    label = "Edge 1",
                    data = mapOf("key" to "value"),
                    style = mapOf("color" to "blue")
                )
            ),
            metadata = mapOf("key" to "value"),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}
