package ai.kastrax.server.spring.controller

import ai.kastrax.server.common.api.WorkflowApi
import ai.kastrax.server.common.model.Edge
import ai.kastrax.server.common.model.Node
import ai.kastrax.server.common.model.Position
import ai.kastrax.server.common.model.Workflow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.hamcrest.Matchers.containsString
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

@WebMvcTest(WorkflowController::class)
class WorkflowControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var workflowApi: WorkflowApi

    @Test
    fun `test get workflow`() {
        // 准备测试数据
        val workflowId = UUID.randomUUID().toString()
        val workflow = createTestWorkflow(workflowId)

        // 模拟API调用
        whenever(workflowApi.getWorkflow(workflowId)).thenReturn(CompletableFuture.completedFuture(workflow))

        // 执行测试
        mockMvc.perform(get("/workflows/$workflowId"))
            .andExpect(request().asyncStarted())
            .andReturn()

        // 等待异步处理完成
        mockMvc.perform(asyncDispatch(mockMvc.perform(get("/workflows/$workflowId"))
            .andExpect(request().asyncStarted())
            .andReturn()))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string(containsString(workflowId)))
            .andExpect(content().string(containsString("Test Workflow")))
    }

    @Test
    fun `test create workflow`() {
        // 准备测试数据
        val workflowId = UUID.randomUUID().toString()
        val workflow = createTestWorkflow(workflowId)

        // 模拟API调用
        whenever(workflowApi.createWorkflow(any())).thenReturn(CompletableFuture.completedFuture(workflow))

        // 执行测试
        val mvcResult = mockMvc.perform(post("/workflows")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "id": "$workflowId",
                        "name": "Test Workflow",
                        "description": "Test Description",
                        "version": "1.0.0",
                        "nodes": [],
                        "edges": [],
                        "metadata": {},
                        "createdAt": "${workflow.createdAt}",
                        "updatedAt": "${workflow.updatedAt}"
                    }
                """))
            .andExpect(request().asyncStarted())
            .andReturn()

        // 等待异步处理完成
        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string(containsString(workflowId)))
            .andExpect(content().string(containsString("Test Workflow")))
    }

    // 创建测试工作流
    private fun createTestWorkflow(id: String): Workflow {
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
