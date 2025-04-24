package ai.kastrax.core.workflow.statemachine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * 简单状态机测试类。
 */
class SimpleStateMachineTest {
    private lateinit var stateMachine: SimpleStateMachine
    
    @BeforeEach
    fun setUp() {
        stateMachine = SimpleStateMachine()
    }
    
    @Test
    fun `test state transitions`() {
        // 创建状态机ID
        val machineId = UUID.randomUUID().toString()
        
        // 初始化状态
        val input = mapOf("name" to "Test")
        stateMachine.initializeState(machineId, input)
        
        // 初始状态 -> 运行中
        var result = stateMachine.transition(machineId, WorkflowState.INITIAL, WorkflowEvent.Start)
        assertEquals(WorkflowState.RUNNING, result.nextState)
        
        // 运行中 -> 完成
        val output = mapOf("greeting" to "Hello, Test!")
        result = stateMachine.transition(machineId, WorkflowState.RUNNING, WorkflowEvent.Complete(output))
        assertEquals(WorkflowState.COMPLETED, result.nextState)
        assertEquals("Hello, Test!", result.output["greeting"])
        
        // 清理状态
        stateMachine.cleanupState(machineId)
    }
    
    @Test
    fun `test error handling`() {
        // 创建状态机ID
        val machineId = UUID.randomUUID().toString()
        
        // 初始化状态
        val input = mapOf("name" to "Test")
        stateMachine.initializeState(machineId, input)
        
        // 初始状态 -> 运行中
        var result = stateMachine.transition(machineId, WorkflowState.INITIAL, WorkflowEvent.Start)
        assertEquals(WorkflowState.RUNNING, result.nextState)
        
        // 运行中 -> 失败
        result = stateMachine.transition(machineId, WorkflowState.RUNNING, WorkflowEvent.Fail("Test error"))
        assertEquals(WorkflowState.FAILED, result.nextState)
        assertEquals("Test error", result.error)
        
        // 清理状态
        stateMachine.cleanupState(machineId)
    }
    
    @Test
    fun `test suspend and resume`() {
        // 创建状态机ID
        val machineId = UUID.randomUUID().toString()
        
        // 初始化状态
        val input = mapOf("name" to "Test")
        stateMachine.initializeState(machineId, input)
        
        // 初始状态 -> 运行中
        var result = stateMachine.transition(machineId, WorkflowState.INITIAL, WorkflowEvent.Start)
        assertEquals(WorkflowState.RUNNING, result.nextState)
        
        // 运行中 -> 暂停
        result = stateMachine.transition(machineId, WorkflowState.RUNNING, WorkflowEvent.Suspend("step1"))
        assertEquals(WorkflowState.SUSPENDED, result.nextState)
        
        // 暂停 -> 运行中
        val resumeInput = mapOf("approved" to true)
        result = stateMachine.transition(machineId, WorkflowState.SUSPENDED, WorkflowEvent.Resume("step1", resumeInput))
        assertEquals(WorkflowState.RUNNING, result.nextState)
        assertEquals(true, result.output["approved"])
        
        // 清理状态
        stateMachine.cleanupState(machineId)
    }
    
    @Test
    fun `test wait and external event`() {
        // 创建状态机ID
        val machineId = UUID.randomUUID().toString()
        
        // 初始化状态
        val input = mapOf("name" to "Test")
        stateMachine.initializeState(machineId, input)
        
        // 初始状态 -> 运行中
        var result = stateMachine.transition(machineId, WorkflowState.INITIAL, WorkflowEvent.Start)
        assertEquals(WorkflowState.RUNNING, result.nextState)
        
        // 运行中 -> 等待
        result = stateMachine.transition(machineId, WorkflowState.RUNNING, WorkflowEvent.Wait("step1"))
        assertEquals(WorkflowState.WAITING, result.nextState)
        
        // 等待 -> 运行中
        val externalInput = mapOf("data" to "Event data")
        result = stateMachine.transition(machineId, WorkflowState.WAITING, WorkflowEvent.Resume("step1", externalInput))
        assertEquals(WorkflowState.RUNNING, result.nextState)
        assertEquals("Event data", result.output["data"])
        
        // 清理状态
        stateMachine.cleanupState(machineId)
    }
    
    @Test
    fun `test cancel`() {
        // 创建状态机ID
        val machineId = UUID.randomUUID().toString()
        
        // 初始化状态
        val input = mapOf("name" to "Test")
        stateMachine.initializeState(machineId, input)
        
        // 初始状态 -> 运行中
        var result = stateMachine.transition(machineId, WorkflowState.INITIAL, WorkflowEvent.Start)
        assertEquals(WorkflowState.RUNNING, result.nextState)
        
        // 运行中 -> 取消
        result = stateMachine.transition(machineId, WorkflowState.RUNNING, WorkflowEvent.Cancel)
        assertEquals(WorkflowState.CANCELED, result.nextState)
        
        // 清理状态
        stateMachine.cleanupState(machineId)
    }
}
