package ai.kastrax.server.quarkus.service

import ai.kastrax.server.common.model.Edge
import ai.kastrax.server.common.model.Node
import ai.kastrax.server.common.model.Position
import ai.kastrax.server.common.model.Workflow
import ai.kastrax.server.quarkus.repository.WorkflowRepository
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.Instant
import java.util.UUID
import jakarta.inject.Inject

@QuarkusTest
class QuarkusWorkflowServiceTest {

    private lateinit var workflowRepository: WorkflowRepository

    @Inject
    private lateinit var workflowService: QuarkusWorkflowService

    @BeforeEach
    fun setUp() {
        workflowRepository = mock(WorkflowRepository::class.java)
        workflowService.workflowRepository = workflowRepository
    }

    @Test
    fun `test create workflow`() {
        // 准备测试数据
        val workflow = createTestWorkflow()

        // 模拟存储库调用
        `when`(workflowRepository.save(workflow)).thenReturn(workflow)

        // 执行测试
        val result = workflowService.createWorkflow(workflow).get()

        // 验证结果
        assertNotNull(result)
        assertEquals(workflow.name, result.name)
        assertEquals(workflow.description, result.description)

        // 验证存储库调用
        verify(workflowRepository).save(workflow)
    }

    @Test
    fun `test get workflow`() {
        // 准备测试数据
        val workflowId = UUID.randomUUID().toString()
        val workflow = createTestWorkflow(workflowId)

        // 模拟存储库调用
        `when`(workflowRepository.findById(workflowId)).thenReturn(workflow)

        // 执行测试
        val result = workflowService.getWorkflow(workflowId).get()

        // 验证结果
        assertNotNull(result)
        assertEquals(workflowId, result.id)
        assertEquals(workflow.name, result.name)

        // 验证存储库调用
        verify(workflowRepository).findById(workflowId)
    }

    @Test
    fun `test get workflow not found`() {
        // 准备测试数据
        val workflowId = UUID.randomUUID().toString()

        // 模拟存储库调用
        `when`(workflowRepository.findById(workflowId)).thenReturn(null)

        // 执行测试并验证异常
        val exception = assertThrows<NoSuchElementException> {
            workflowService.getWorkflow(workflowId).get()
        }

        // 验证异常消息
        assertTrue(exception.message!!.contains(workflowId))

        // 验证存储库调用
        verify(workflowRepository).findById(workflowId)
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
