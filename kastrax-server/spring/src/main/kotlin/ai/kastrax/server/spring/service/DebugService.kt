package ai.kastrax.server.spring.service

import ai.kastrax.server.common.api.DebugApi
import ai.kastrax.server.common.model.Breakpoint
import ai.kastrax.server.common.model.DebugSession
import ai.kastrax.server.common.model.DebugSessionStatus
import ai.kastrax.server.common.model.Execution
import ai.kastrax.server.common.model.ExecutionStatus
import ai.kastrax.server.common.model.NodeExecution
import ai.kastrax.server.spring.repository.BreakpointRepository
import ai.kastrax.server.spring.repository.DebugSessionRepository
import ai.kastrax.server.spring.repository.WorkflowRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

@Service
class DebugService(
    private val workflowRepository: WorkflowRepository,
    private val breakpointRepository: BreakpointRepository,
    private val debugSessionRepository: DebugSessionRepository
) : DebugApi {
    
    override fun setBreakpoint(workflowId: String, breakpoint: Breakpoint): CompletableFuture<Breakpoint> {
        val workflow = workflowRepository.findById(workflowId)
            ?: throw NoSuchElementException("Workflow not found: $workflowId")
        
        // 验证节点ID是否存在
        if (workflow.nodes.none { it.id == breakpoint.nodeId }) {
            throw IllegalArgumentException("Node not found: ${breakpoint.nodeId}")
        }
        
        val newBreakpoint = breakpoint.copy(
            id = UUID.randomUUID().toString(),
            workflowId = workflowId
        )
        
        return CompletableFuture.completedFuture(breakpointRepository.save(newBreakpoint))
    }
    
    override fun getBreakpoints(workflowId: String): CompletableFuture<List<Breakpoint>> {
        return CompletableFuture.completedFuture(breakpointRepository.findByWorkflowId(workflowId))
    }
    
    override fun deleteBreakpoint(workflowId: String, breakpointId: String): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(breakpointRepository.deleteById(breakpointId))
    }
    
    override fun createDebugSession(workflowId: String, input: Map<String, Any>): CompletableFuture<DebugSession> {
        val workflow = workflowRepository.findById(workflowId)
            ?: throw NoSuchElementException("Workflow not found: $workflowId")
        
        val breakpoints = breakpointRepository.findByWorkflowId(workflowId)
        
        // 创建执行记录
        val executionId = UUID.randomUUID().toString()
        val execution = Execution(
            id = executionId,
            workflowId = workflowId,
            status = ExecutionStatus.PENDING,
            input = input,
            output = emptyMap(),
            error = null,
            startTime = Instant.now(),
            endTime = null,
            nodeExecutions = emptyMap()
        )
        
        // 创建调试会话
        val sessionId = UUID.randomUUID().toString()
        val session = DebugSession(
            id = sessionId,
            workflowId = workflowId,
            executionId = executionId,
            status = DebugSessionStatus.ACTIVE,
            breakpoints = breakpoints,
            currentNodeId = null
        )
        
        return CompletableFuture.completedFuture(debugSessionRepository.save(session))
    }
    
    override fun getDebugSession(id: String): CompletableFuture<DebugSession> {
        return CompletableFuture.completedFuture(debugSessionRepository.findById(id)
            ?: throw NoSuchElementException("Debug session not found: $id"))
    }
    
    override fun stepExecution(sessionId: String): CompletableFuture<Execution> {
        val session = debugSessionRepository.findById(sessionId)
            ?: throw NoSuchElementException("Debug session not found: $sessionId")
        
        val workflow = workflowRepository.findById(session.workflowId)
            ?: throw NoSuchElementException("Workflow not found: ${session.workflowId}")
        
        // 模拟单步执行
        // 在实际实现中，这里应该调用KastraX Core的工作流执行引擎进行单步执行
        
        // 找到当前节点或第一个节点
        val currentNodeId = session.currentNodeId
        val nextNodeId = if (currentNodeId == null) {
            // 如果没有当前节点，则使用第一个节点
            workflow.nodes.firstOrNull()?.id
        } else {
            // 如果有当前节点，则找到下一个节点
            val currentIndex = workflow.nodes.indexOfFirst { it.id == currentNodeId }
            if (currentIndex >= 0 && currentIndex < workflow.nodes.size - 1) {
                workflow.nodes[currentIndex + 1].id
            } else {
                null
            }
        }
        
        // 更新调试会话
        val updatedSession = session.copy(
            currentNodeId = nextNodeId,
            status = if (nextNodeId == null) DebugSessionStatus.COMPLETED else DebugSessionStatus.PAUSED
        )
        
        debugSessionRepository.save(updatedSession)
        
        // 创建执行结果
        val execution = Execution(
            id = session.executionId,
            workflowId = session.workflowId,
            status = if (nextNodeId == null) ExecutionStatus.COMPLETED else ExecutionStatus.SUSPENDED,
            input = emptyMap(), // 在实际实现中，这里应该是真实的输入数据
            output = if (nextNodeId == null) mapOf("result" to "Debug session completed") else emptyMap(),
            error = null,
            startTime = Instant.now().minusSeconds(10), // 模拟开始时间
            endTime = if (nextNodeId == null) Instant.now() else null,
            nodeExecutions = workflow.nodes
                .filter { it.id == currentNodeId || (currentNodeId == null && it.id == nextNodeId) }
                .associate { node ->
                    node.id to NodeExecution(
                        nodeId = node.id,
                        status = ExecutionStatus.COMPLETED,
                        input = emptyMap(), // 在实际实现中，这里应该是真实的输入数据
                        output = mapOf("output" to "Simulated output for step ${node.label}"),
                        error = null,
                        startTime = Instant.now().minusSeconds(5), // 模拟开始时间
                        endTime = Instant.now()
                    )
                }
        )
        
        return CompletableFuture.completedFuture(execution)
    }
    
    override fun continueExecution(sessionId: String): CompletableFuture<Execution> {
        val session = debugSessionRepository.findById(sessionId)
            ?: throw NoSuchElementException("Debug session not found: $sessionId")
        
        val workflow = workflowRepository.findById(session.workflowId)
            ?: throw NoSuchElementException("Workflow not found: ${session.workflowId}")
        
        // 模拟继续执行
        // 在实际实现中，这里应该调用KastraX Core的工作流执行引擎继续执行
        
        // 更新调试会话
        val updatedSession = session.copy(
            currentNodeId = null,
            status = DebugSessionStatus.COMPLETED
        )
        
        debugSessionRepository.save(updatedSession)
        
        // 创建执行结果
        val execution = Execution(
            id = session.executionId,
            workflowId = session.workflowId,
            status = ExecutionStatus.COMPLETED,
            input = emptyMap(), // 在实际实现中，这里应该是真实的输入数据
            output = mapOf("result" to "Debug session completed"),
            error = null,
            startTime = Instant.now().minusSeconds(10), // 模拟开始时间
            endTime = Instant.now(),
            nodeExecutions = workflow.nodes.associate { node ->
                node.id to NodeExecution(
                    nodeId = node.id,
                    status = ExecutionStatus.COMPLETED,
                    input = emptyMap(), // 在实际实现中，这里应该是真实的输入数据
                    output = mapOf("output" to "Simulated output for step ${node.label}"),
                    error = null,
                    startTime = Instant.now().minusSeconds(5), // 模拟开始时间
                    endTime = Instant.now()
                )
            }
        )
        
        return CompletableFuture.completedFuture(execution)
    }
    
    override fun stopDebugSession(id: String): CompletableFuture<Boolean> {
        val session = debugSessionRepository.findById(id)
            ?: throw NoSuchElementException("Debug session not found: $id")
        
        if (session.status == DebugSessionStatus.COMPLETED || 
            session.status == DebugSessionStatus.FAILED ||
            session.status == DebugSessionStatus.CANCELED) {
            return CompletableFuture.completedFuture(false)
        }
        
        val updatedSession = session.copy(
            status = DebugSessionStatus.CANCELED
        )
        
        debugSessionRepository.save(updatedSession)
        
        return CompletableFuture.completedFuture(true)
    }
}
