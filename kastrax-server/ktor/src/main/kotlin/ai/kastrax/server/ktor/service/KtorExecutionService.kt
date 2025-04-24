package ai.kastrax.server.ktor.service

import ai.kastrax.core.workflow.WorkflowResult
import ai.kastrax.server.common.api.ExecutionApi
import ai.kastrax.server.common.model.Execution
import ai.kastrax.server.common.model.ExecutionStatus
import ai.kastrax.server.common.model.NodeExecution
import ai.kastrax.server.ktor.repository.ExecutionRepository
import ai.kastrax.server.ktor.repository.WorkflowRepository
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

class KtorExecutionService(
    private val workflowRepository: WorkflowRepository,
    private val executionRepository: ExecutionRepository
) : ExecutionApi {

    override fun executeWorkflow(workflowId: String, input: Map<String, Any>): CompletableFuture<Execution> {
        val workflow = workflowRepository.findById(workflowId)
            ?: throw NoSuchElementException("Workflow not found: $workflowId")

        // 创建执行记录
        val executionId = UUID.randomUUID().toString()
        val execution = Execution(
            id = executionId,
            workflowId = workflowId,
            status = ExecutionStatus.RUNNING,
            input = input,
            output = emptyMap(),
            error = null,
            startTime = Instant.now(),
            endTime = null,
            nodeExecutions = emptyMap()
        )

        // 创建一个映射，用于存储每个节点的输入
        val resolvedVariables = mutableMapOf<String, Map<String, Any>>()
        workflow.nodes.forEach { node ->
            resolvedVariables[node.id] = input
        }

        executionRepository.save(execution)

        // 异步执行工作流
        CompletableFuture.runAsync {
            try {
                // 这里应该调用KastraX Core的工作流执行引擎
                // 为了简化示例，我们直接模拟执行结果
                val result = simulateWorkflowExecution(workflow, input)

                // 更新执行记录
                val updatedExecution = execution.copy(
                    status = if (result.success) ExecutionStatus.COMPLETED else ExecutionStatus.FAILED,
                    output = result.output.filterValues { it != null } as Map<String, Any>,
                    error = result.error,
                    endTime = Instant.now(),
                    nodeExecutions = result.steps.mapValues { (nodeId, stepResult) ->
                        NodeExecution(
                            nodeId = nodeId,
                            status = if (stepResult.success) ExecutionStatus.COMPLETED else ExecutionStatus.FAILED,
                            input = resolvedVariables.getOrDefault(nodeId, emptyMap()),
                            output = stepResult.output.filterValues { it != null } as Map<String, Any>,
                            error = stepResult.error,
                            startTime = Instant.now().minusSeconds(10), // 模拟开始时间
                            endTime = Instant.now()
                        )
                    }
                )

                executionRepository.save(updatedExecution)
            } catch (e: Exception) {
                // 更新执行记录为失败状态
                val failedExecution = execution.copy(
                    status = ExecutionStatus.FAILED,
                    error = e.message,
                    endTime = Instant.now()
                )

                executionRepository.save(failedExecution)
            }
        }

        return CompletableFuture.completedFuture(execution)
    }

    override fun getExecution(id: String): CompletableFuture<Execution> {
        return CompletableFuture.completedFuture(executionRepository.findById(id)
            ?: throw NoSuchElementException("Execution not found: $id"))
    }

    override fun cancelExecution(id: String): CompletableFuture<Boolean> {
        val execution = executionRepository.findById(id)
            ?: throw NoSuchElementException("Execution not found: $id")

        if (execution.status != ExecutionStatus.RUNNING && execution.status != ExecutionStatus.PENDING) {
            return CompletableFuture.completedFuture(false)
        }

        val canceledExecution = execution.copy(
            status = ExecutionStatus.CANCELED,
            endTime = Instant.now()
        )

        executionRepository.save(canceledExecution)

        return CompletableFuture.completedFuture(true)
    }

    override fun getExecutionHistory(workflowId: String, page: Int, size: Int): CompletableFuture<List<Execution>> {
        return CompletableFuture.completedFuture(executionRepository.findByWorkflowId(workflowId, page, size))
    }

    // 模拟工作流执行
    private fun simulateWorkflowExecution(workflow: ai.kastrax.server.common.model.Workflow, input: Map<String, Any>): WorkflowResult {
        // 在实际实现中，这里应该调用KastraX Core的工作流执行引擎
        // 为了简化示例，我们直接返回一个模拟的执行结果
        return WorkflowResult(
            success = true,
            output = mapOf("result" to "Simulated result for workflow ${workflow.name}"),
            steps = workflow.nodes.associate { node ->
                node.id to ai.kastrax.core.workflow.WorkflowStepResult(
                    stepId = node.id,
                    success = true,
                    output = mapOf("output" to "Simulated output for step ${node.label}"),
                    error = null
                )
            },
            executionTime = 1000,
            runId = UUID.randomUUID().toString()
        )
    }
}
