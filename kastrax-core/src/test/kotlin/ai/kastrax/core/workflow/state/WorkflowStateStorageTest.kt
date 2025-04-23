package ai.kastrax.core.workflow.state

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * 工作流状态存储测试。
 */
class WorkflowStateStorageTest {
    
    private lateinit var storage: WorkflowStateStorage
    
    @BeforeEach
    fun setUp() {
        storage = InMemoryWorkflowStateStorage()
    }
    
    @Test
    fun testSaveAndGetWorkflowState() = runBlocking {
        // 创建测试数据
        val workflowId = "test-workflow"
        val runId = UUID.randomUUID().toString()
        val state = createTestWorkflowState(workflowId, runId)
        
        // 保存状态
        val saveResult = storage.saveWorkflowState(workflowId, runId, state)
        assertTrue(saveResult)
        
        // 获取状态
        val retrievedState = storage.getWorkflowState(workflowId, runId)
        assertNotNull(retrievedState)
        assertEquals(workflowId, retrievedState?.workflowId)
        assertEquals(runId, retrievedState?.runId)
        assertEquals(WorkflowStateStatus.RUNNING, retrievedState?.status)
        assertEquals(2, retrievedState?.steps?.size)
    }
    
    @Test
    fun testGetNonExistentWorkflowState() = runBlocking {
        val workflowId = "non-existent-workflow"
        val runId = UUID.randomUUID().toString()
        
        val state = storage.getWorkflowState(workflowId, runId)
        assertNull(state)
    }
    
    @Test
    fun testGetWorkflowRuns() = runBlocking {
        // 创建测试数据
        val workflowId = "test-workflow"
        val runId1 = UUID.randomUUID().toString()
        val runId2 = UUID.randomUUID().toString()
        val runId3 = UUID.randomUUID().toString()
        
        val state1 = createTestWorkflowState(workflowId, runId1)
        val state2 = createTestWorkflowState(workflowId, runId2)
        val state3 = createTestWorkflowState(workflowId, runId3)
        
        // 保存状态
        storage.saveWorkflowState(workflowId, runId1, state1)
        storage.saveWorkflowState(workflowId, runId2, state2)
        storage.saveWorkflowState(workflowId, runId3, state3)
        
        // 获取运行状态
        val runs = storage.getWorkflowRuns(workflowId)
        assertEquals(3, runs.size)
        
        // 测试分页
        val runsPage1 = storage.getWorkflowRuns(workflowId, limit = 2, offset = 0)
        assertEquals(2, runsPage1.size)
        
        val runsPage2 = storage.getWorkflowRuns(workflowId, limit = 2, offset = 2)
        assertEquals(1, runsPage2.size)
    }
    
    @Test
    fun testDeleteWorkflowState() = runBlocking {
        // 创建测试数据
        val workflowId = "test-workflow"
        val runId = UUID.randomUUID().toString()
        val state = createTestWorkflowState(workflowId, runId)
        
        // 保存状态
        storage.saveWorkflowState(workflowId, runId, state)
        
        // 删除状态
        val deleteResult = storage.deleteWorkflowState(workflowId, runId)
        assertTrue(deleteResult)
        
        // 验证状态已删除
        val retrievedState = storage.getWorkflowState(workflowId, runId)
        assertNull(retrievedState)
        
        // 尝试删除不存在的状态
        val deleteNonExistentResult = storage.deleteWorkflowState(workflowId, runId)
        assertFalse(deleteNonExistentResult)
    }
    
    /**
     * 创建测试工作流状态。
     */
    private fun createTestWorkflowState(workflowId: String, runId: String): WorkflowState {
        return WorkflowState(
            runId = runId,
            workflowId = workflowId,
            status = WorkflowStateStatus.RUNNING,
            context = emptyMap(),
            steps = mapOf(
                "step1" to StepState(
                    stepId = "step1",
                    status = StepStateStatus.COMPLETED,
                    output = mapOf("result" to JsonPrimitive("Step 1 result")),
                    executionTime = 100
                ),
                "step2" to StepState(
                    stepId = "step2",
                    status = StepStateStatus.SUSPENDED,
                    output = emptyMap(),
                    suspendPayload = JsonPrimitive("Waiting for input")
                )
            ),
            suspendedSteps = setOf("step2")
        )
    }
}
