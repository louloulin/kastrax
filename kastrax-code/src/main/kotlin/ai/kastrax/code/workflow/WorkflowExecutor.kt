package ai.kastrax.code.workflow

import ai.kastrax.code.model.Context
import kotlinx.coroutines.flow.Flow

/**
 * 工作流执行器接口
 *
 * 定义工作流执行器的基本操作
 */
interface WorkflowExecutor {
    /**
     * 执行工作流
     *
     * @param workflow 工作流
     * @param context 初始上下文
     * @return 工作流结果流
     */
    fun executeWorkflow(workflow: CodeWorkflow, context: Context): Flow<WorkflowExecutionEvent>
    
    /**
     * 暂停工作流
     *
     * @param workflowId 工作流ID
     * @return 是否成功暂停
     */
    suspend fun pauseWorkflow(workflowId: String): Boolean
    
    /**
     * 恢复工作流
     *
     * @param workflowId 工作流ID
     * @return 是否成功恢复
     */
    suspend fun resumeWorkflow(workflowId: String): Boolean
    
    /**
     * 取消工作流
     *
     * @param workflowId 工作流ID
     * @return 是否成功取消
     */
    suspend fun cancelWorkflow(workflowId: String): Boolean
    
    /**
     * 获取工作流状态
     *
     * @param workflowId 工作流ID
     * @return 工作流状态
     */
    suspend fun getWorkflowStatus(workflowId: String): WorkflowStatus?
    
    /**
     * 获取正在运行的工作流
     *
     * @return 工作流列表
     */
    suspend fun getRunningWorkflows(): List<CodeWorkflow>
    
    /**
     * 获取工作流执行历史
     *
     * @param workflowId 工作流ID
     * @return 工作流执行事件列表
     */
    suspend fun getWorkflowExecutionHistory(workflowId: String): List<WorkflowExecutionEvent>
}

/**
 * 工作流执行事件
 */
sealed class WorkflowExecutionEvent {
    /**
     * 工作流ID
     */
    abstract val workflowId: String
    
    /**
     * 时间戳
     */
    abstract val timestamp: Long
    
    /**
     * 工作流开始事件
     *
     * @property workflowId 工作流ID
     * @property timestamp 时间戳
     * @property context 初始上下文
     */
    data class WorkflowStarted(
        override val workflowId: String,
        override val timestamp: Long,
        val context: Context
    ) : WorkflowExecutionEvent()
    
    /**
     * 工作流完成事件
     *
     * @property workflowId 工作流ID
     * @property timestamp 时间戳
     * @property result 工作流结果
     */
    data class WorkflowCompleted(
        override val workflowId: String,
        override val timestamp: Long,
        val result: WorkflowResult
    ) : WorkflowExecutionEvent()
    
    /**
     * 工作流失败事件
     *
     * @property workflowId 工作流ID
     * @property timestamp 时间戳
     * @property error 错误信息
     * @property context 当前上下文
     */
    data class WorkflowFailed(
        override val workflowId: String,
        override val timestamp: Long,
        val error: String,
        val context: Context
    ) : WorkflowExecutionEvent()
    
    /**
     * 工作流暂停事件
     *
     * @property workflowId 工作流ID
     * @property timestamp 时间戳
     * @property context 当前上下文
     */
    data class WorkflowPaused(
        override val workflowId: String,
        override val timestamp: Long,
        val context: Context
    ) : WorkflowExecutionEvent()
    
    /**
     * 工作流恢复事件
     *
     * @property workflowId 工作流ID
     * @property timestamp 时间戳
     * @property context 当前上下文
     */
    data class WorkflowResumed(
        override val workflowId: String,
        override val timestamp: Long,
        val context: Context
    ) : WorkflowExecutionEvent()
    
    /**
     * 工作流取消事件
     *
     * @property workflowId 工作流ID
     * @property timestamp 时间戳
     * @property context 当前上下文
     */
    data class WorkflowCancelled(
        override val workflowId: String,
        override val timestamp: Long,
        val context: Context
    ) : WorkflowExecutionEvent()
    
    /**
     * 步骤开始事件
     *
     * @property workflowId 工作流ID
     * @property timestamp 时间戳
     * @property stepId 步骤ID
     * @property stepName 步骤名称
     * @property context 当前上下文
     */
    data class StepStarted(
        override val workflowId: String,
        override val timestamp: Long,
        val stepId: String,
        val stepName: String,
        val context: Context
    ) : WorkflowExecutionEvent()
    
    /**
     * 步骤完成事件
     *
     * @property workflowId 工作流ID
     * @property timestamp 时间戳
     * @property stepId 步骤ID
     * @property stepName 步骤名称
     * @property result 步骤结果
     */
    data class StepCompleted(
        override val workflowId: String,
        override val timestamp: Long,
        val stepId: String,
        val stepName: String,
        val result: StepResult
    ) : WorkflowExecutionEvent()
    
    /**
     * 步骤失败事件
     *
     * @property workflowId 工作流ID
     * @property timestamp 时间戳
     * @property stepId 步骤ID
     * @property stepName 步骤名称
     * @property error 错误信息
     * @property context 当前上下文
     */
    data class StepFailed(
        override val workflowId: String,
        override val timestamp: Long,
        val stepId: String,
        val stepName: String,
        val error: String,
        val context: Context
    ) : WorkflowExecutionEvent()
    
    /**
     * 步骤跳过事件
     *
     * @property workflowId 工作流ID
     * @property timestamp 时间戳
     * @property stepId 步骤ID
     * @property stepName 步骤名称
     * @property reason 跳过原因
     * @property context 当前上下文
     */
    data class StepSkipped(
        override val workflowId: String,
        override val timestamp: Long,
        val stepId: String,
        val stepName: String,
        val reason: String,
        val context: Context
    ) : WorkflowExecutionEvent()
    
    /**
     * 检查点创建事件
     *
     * @property workflowId 工作流ID
     * @property timestamp 时间戳
     * @property checkpointId 检查点ID
     * @property checkpointName 检查点名称
     */
    data class CheckpointCreated(
        override val workflowId: String,
        override val timestamp: Long,
        val checkpointId: String,
        val checkpointName: String
    ) : WorkflowExecutionEvent()
    
    /**
     * 检查点恢复事件
     *
     * @property workflowId 工作流ID
     * @property timestamp 时间戳
     * @property checkpointId 检查点ID
     * @property checkpointName 检查点名称
     */
    data class CheckpointRestored(
        override val workflowId: String,
        override val timestamp: Long,
        val checkpointId: String,
        val checkpointName: String
    ) : WorkflowExecutionEvent()
}
