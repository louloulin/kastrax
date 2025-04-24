package ai.kastrax.core.workflow.statemachine

import ai.kastrax.core.workflow.WorkflowStatus
import ai.kastrax.core.workflow.WorkflowStatusUpdate
import ai.kastrax.core.workflow.WorkflowStepResult
import kotlinx.coroutines.flow.FlowCollector

/**
 * 工作流状态更新扩展函数。
 */
object WorkflowStatusUpdateExtensions {
    /**
     * 发送工作流开始状态。
     */
    suspend fun FlowCollector<WorkflowStatusUpdate>.emitStarted(
        workflowId: String,
        runId: String
    ) {
        emit(WorkflowStatusUpdate(
            status = WorkflowStatus.STARTED,
            message = "Starting workflow execution",
            stepId = null,
            progress = 0
        ))
    }
    
    /**
     * 发送工作流完成状态。
     */
    suspend fun FlowCollector<WorkflowStatusUpdate>.emitCompleted(
        workflowId: String,
        runId: String,
        output: Map<String, Any?>
    ) {
        emit(WorkflowStatusUpdate(
            status = WorkflowStatus.COMPLETED,
            message = "Workflow execution completed",
            stepId = null,
            progress = 100
        ))
    }
    
    /**
     * 发送工作流失败状态。
     */
    suspend fun FlowCollector<WorkflowStatusUpdate>.emitFailed(
        workflowId: String,
        runId: String,
        error: String
    ) {
        emit(WorkflowStatusUpdate(
            status = WorkflowStatus.FAILED,
            message = "Workflow execution failed: $error",
            stepId = null,
            progress = 0
        ))
    }
    
    /**
     * 发送步骤开始状态。
     */
    suspend fun FlowCollector<WorkflowStatusUpdate>.emitStepStarted(
        workflowId: String,
        runId: String,
        stepId: String,
        stepIndex: Int,
        totalSteps: Int
    ) {
        emit(WorkflowStatusUpdate(
            status = WorkflowStatus.IN_PROGRESS,
            message = "Executing step $stepId",
            stepId = stepId,
            progress = ((stepIndex.toDouble() / totalSteps) * 100).toInt()
        ))
    }
    
    /**
     * 发送步骤完成状态。
     */
    suspend fun FlowCollector<WorkflowStatusUpdate>.emitStepCompleted(
        workflowId: String,
        runId: String,
        stepId: String,
        stepIndex: Int,
        totalSteps: Int,
        output: Map<String, Any?>
    ) {
        emit(WorkflowStatusUpdate(
            status = WorkflowStatus.IN_PROGRESS,
            message = "Step $stepId completed",
            stepId = stepId,
            progress = ((stepIndex + 1.0) / totalSteps * 100).toInt(),
            result = WorkflowStepResult(
                stepId = stepId,
                success = true,
                output = output
            )
        ))
    }
    
    /**
     * 发送步骤失败状态。
     */
    suspend fun FlowCollector<WorkflowStatusUpdate>.emitStepFailed(
        workflowId: String,
        runId: String,
        stepId: String,
        stepIndex: Int,
        totalSteps: Int,
        error: String
    ) {
        emit(WorkflowStatusUpdate(
            status = WorkflowStatus.FAILED,
            message = "Step $stepId failed: $error",
            stepId = stepId,
            progress = ((stepIndex.toDouble() / totalSteps) * 100).toInt(),
            result = WorkflowStepResult(
                stepId = stepId,
                success = false,
                output = emptyMap(),
                error = error
            )
        ))
    }
}
