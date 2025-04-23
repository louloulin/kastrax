package ai.kastrax.core.workflow.engine

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowExecuteOptions
import ai.kastrax.core.workflow.WorkflowResult
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.state.InMemoryWorkflowStateStorage
import ai.kastrax.core.workflow.state.StepState
import ai.kastrax.core.workflow.state.StepStateStatus
import ai.kastrax.core.workflow.state.WorkflowState
import ai.kastrax.core.workflow.state.WorkflowStateStatus
import ai.kastrax.core.workflow.state.WorkflowStateStorage
import ai.kastrax.core.workflow.state.toWorkflowState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import mu.KotlinLogging
import java.util.UUID

/**
 * 工作流引擎，负责执行和管理工作流。
 */
class WorkflowEngine(
    private val workflows: Map<String, Workflow>,
    private val stateStorage: WorkflowStateStorage = InMemoryWorkflowStateStorage()
) : KastraXBase(component = "WORKFLOW_ENGINE", name = "default") {
    // 使用父类的 logger

    // 状态更新流
    private val _stateUpdates = MutableSharedFlow<WorkflowState>(replay = 0)
    val stateUpdates = _stateUpdates.asSharedFlow()

    /**
     * 执行工作流。
     *
     * @param workflowId 工作流ID
     * @param input 输入数据
     * @param options 执行选项
     * @return 工作流执行结果
     */
    suspend fun executeWorkflow(
        workflowId: String,
        input: Map<String, Any?>,
        options: WorkflowExecuteOptions = WorkflowExecuteOptions()
    ): WorkflowResult {
        val workflow = workflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        // 生成运行ID
        val runId = UUID.randomUUID().toString()
        logger.info { "执行工作流: $workflowId, 运行ID: $runId" }

        // 执行工作流
        val result = workflow.execute(input, options)

        // 保存工作流状态
        val state = result.toWorkflowState(
            runId = runId,
            workflowId = workflowId
        )
        stateStorage.saveWorkflowState(workflowId, runId, state)

        // 发送状态更新
        _stateUpdates.emit(state)

        return result
    }

    /**
     * 恢复工作流。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param stepId 步骤ID
     * @param input 输入数据
     * @param options 执行选项
     * @return 工作流执行结果
     */
    suspend fun resumeWorkflow(
        workflowId: String,
        runId: String,
        stepId: String,
        input: Map<String, Any?>,
        options: WorkflowExecuteOptions = WorkflowExecuteOptions()
    ): WorkflowResult {
        val workflow = workflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        // 获取工作流状态
        val state = stateStorage.getWorkflowState(workflowId, runId)
            ?: throw IllegalArgumentException("Workflow state not found: $workflowId, $runId")

        // 检查工作流状态
        if (state.status != WorkflowStateStatus.SUSPENDED) {
            throw IllegalStateException("Workflow is not suspended: $workflowId, $runId")
        }

        // 检查步骤状态
        val stepState = state.steps[stepId]
            ?: throw IllegalArgumentException("Step not found: $stepId")

        if (stepState.status != StepStateStatus.SUSPENDED) {
            throw IllegalStateException("Step is not suspended: $stepId")
        }

        // 更新工作流状态
        val updatedState = state.copy(
            status = WorkflowStateStatus.RUNNING,
            suspendedSteps = state.suspendedSteps - stepId,
            updatedAt = System.currentTimeMillis()
        )
        stateStorage.saveWorkflowState(workflowId, runId, updatedState)

        // 发送状态更新
        _stateUpdates.emit(updatedState)

        // 执行工作流
        val result = workflow.execute(input, options)

        // 保存工作流状态
        val finalState = result.toWorkflowState(
            runId = runId,
            workflowId = workflowId,
            suspendedSteps = updatedState.suspendedSteps
        )
        stateStorage.saveWorkflowState(workflowId, runId, finalState)

        // 发送状态更新
        _stateUpdates.emit(finalState)

        return result
    }

    /**
     * 获取工作流状态。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @return 工作流状态
     */
    suspend fun getWorkflowState(workflowId: String, runId: String): WorkflowState? {
        return stateStorage.getWorkflowState(workflowId, runId)
    }

    /**
     * 获取工作流的所有运行状态。
     *
     * @param workflowId 工作流ID
     * @param limit 限制返回的数量
     * @param offset 偏移量
     * @return 工作流运行状态列表
     */
    suspend fun getWorkflowRuns(workflowId: String, limit: Int = 10, offset: Int = 0) {
        stateStorage.getWorkflowRuns(workflowId, limit, offset)
    }

    /**
     * 删除工作流状态。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @return 是否删除成功
     */
    suspend fun deleteWorkflowState(workflowId: String, runId: String): Boolean {
        return stateStorage.deleteWorkflowState(workflowId, runId)
    }
}
