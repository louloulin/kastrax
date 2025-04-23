package ai.kastrax.core.workflow.callback

import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult

/**
 * 工作流回调接口。
 */
interface WorkflowCallback {
    /**
     * 工作流开始前回调。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param input 输入数据
     */
    suspend fun beforeWorkflowStart(workflowId: String, runId: String, input: Map<String, Any?>) {}
    
    /**
     * 工作流完成后回调。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param output 输出数据
     * @param executionTime 执行时间（毫秒）
     */
    suspend fun afterWorkflowComplete(workflowId: String, runId: String, output: Map<String, Any?>, executionTime: Long) {}
    
    /**
     * 工作流失败后回调。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param error 错误信息
     * @param executionTime 执行时间（毫秒）
     */
    suspend fun onWorkflowFail(workflowId: String, runId: String, error: String?, executionTime: Long) {}
    
    /**
     * 工作流暂停时回调。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param suspendedStepIds 暂停的步骤ID集合
     */
    suspend fun onWorkflowSuspend(workflowId: String, runId: String, suspendedStepIds: Set<String>) {}
    
    /**
     * 工作流恢复时回调。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param resumedStepId 恢复的步骤ID
     * @param input 输入数据
     */
    suspend fun onWorkflowResume(workflowId: String, runId: String, resumedStepId: String, input: Map<String, Any?>) {}
    
    /**
     * 工作流取消时回调。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param reason 取消原因
     */
    suspend fun onWorkflowCancel(workflowId: String, runId: String, reason: String?) {}
}

/**
 * 步骤回调接口。
 */
interface StepCallback {
    /**
     * 步骤执行前回调。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param stepId 步骤ID
     * @param context 工作流上下文
     * @return 是否继续执行步骤，如果返回false则跳过步骤
     */
    suspend fun beforeStepExecute(workflowId: String, runId: String, stepId: String, context: WorkflowContext): Boolean = true
    
    /**
     * 步骤执行后回调。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param stepId 步骤ID
     * @param result 步骤执行结果
     * @param context 工作流上下文
     * @return 修改后的步骤执行结果，如果不需要修改则返回原结果
     */
    suspend fun afterStepExecute(
        workflowId: String,
        runId: String,
        stepId: String,
        result: WorkflowStepResult,
        context: WorkflowContext
    ): WorkflowStepResult = result
    
    /**
     * 步骤执行失败回调。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param stepId 步骤ID
     * @param error 错误信息
     * @param exception 异常对象
     * @param context 工作流上下文
     * @return 是否处理了错误，如果返回true则工作流引擎不会再处理此错误
     */
    suspend fun onStepFail(
        workflowId: String,
        runId: String,
        stepId: String,
        error: String?,
        exception: Throwable?,
        context: WorkflowContext
    ): Boolean = false
    
    /**
     * 步骤暂停时回调。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param stepId 步骤ID
     * @param suspensionData 暂停数据
     * @param context 工作流上下文
     */
    suspend fun onStepSuspend(
        workflowId: String,
        runId: String,
        stepId: String,
        suspensionData: Map<String, Any?>,
        context: WorkflowContext
    ) {}
    
    /**
     * 步骤恢复时回调。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param stepId 步骤ID
     * @param resumeData 恢复数据
     * @param context 工作流上下文
     */
    suspend fun onStepResume(
        workflowId: String,
        runId: String,
        stepId: String,
        resumeData: Map<String, Any?>,
        context: WorkflowContext
    ) {}
    
    /**
     * 步骤跳过时回调。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param stepId 步骤ID
     * @param reason 跳过原因
     * @param context 工作流上下文
     */
    suspend fun onStepSkip(
        workflowId: String,
        runId: String,
        stepId: String,
        reason: String,
        context: WorkflowContext
    ) {}
}

/**
 * 工作流回调管理器。
 */
class WorkflowCallbackManager {
    private val workflowCallbacks = mutableListOf<WorkflowCallback>()
    private val stepCallbacks = mutableListOf<StepCallback>()
    private val stepSpecificCallbacks = mutableMapOf<String, MutableList<StepCallback>>()
    
    /**
     * 注册工作流回调。
     *
     * @param callback 工作流回调
     */
    fun registerWorkflowCallback(callback: WorkflowCallback) {
        synchronized(workflowCallbacks) {
            workflowCallbacks.add(callback)
        }
    }
    
    /**
     * 注销工作流回调。
     *
     * @param callback 工作流回调
     */
    fun unregisterWorkflowCallback(callback: WorkflowCallback) {
        synchronized(workflowCallbacks) {
            workflowCallbacks.remove(callback)
        }
    }
    
    /**
     * 注册步骤回调。
     *
     * @param callback 步骤回调
     * @param stepId 特定步骤ID，如果为null则适用于所有步骤
     */
    fun registerStepCallback(callback: StepCallback, stepId: String? = null) {
        if (stepId == null) {
            synchronized(stepCallbacks) {
                stepCallbacks.add(callback)
            }
        } else {
            synchronized(stepSpecificCallbacks) {
                val callbacks = stepSpecificCallbacks.getOrPut(stepId) { mutableListOf() }
                callbacks.add(callback)
            }
        }
    }
    
    /**
     * 注销步骤回调。
     *
     * @param callback 步骤回调
     * @param stepId 特定步骤ID，如果为null则从所有步骤中注销
     */
    fun unregisterStepCallback(callback: StepCallback, stepId: String? = null) {
        if (stepId == null) {
            synchronized(stepCallbacks) {
                stepCallbacks.remove(callback)
            }
            
            // 同时从所有特定步骤回调中移除
            synchronized(stepSpecificCallbacks) {
                stepSpecificCallbacks.values.forEach { it.remove(callback) }
            }
        } else {
            synchronized(stepSpecificCallbacks) {
                stepSpecificCallbacks[stepId]?.remove(callback)
            }
        }
    }
    
    /**
     * 获取工作流回调列表。
     *
     * @return 工作流回调列表
     */
    fun getWorkflowCallbacks(): List<WorkflowCallback> {
        return synchronized(workflowCallbacks) {
            workflowCallbacks.toList()
        }
    }
    
    /**
     * 获取步骤回调列表。
     *
     * @param stepId 步骤ID
     * @return 步骤回调列表
     */
    fun getStepCallbacks(stepId: String? = null): List<StepCallback> {
        val globalCallbacks = synchronized(stepCallbacks) {
            stepCallbacks.toList()
        }
        
        if (stepId == null) {
            return globalCallbacks
        }
        
        val specificCallbacks = synchronized(stepSpecificCallbacks) {
            stepSpecificCallbacks[stepId]?.toList() ?: emptyList()
        }
        
        return globalCallbacks + specificCallbacks
    }
    
    /**
     * 清除所有回调。
     */
    fun clearAllCallbacks() {
        synchronized(workflowCallbacks) {
            workflowCallbacks.clear()
        }
        
        synchronized(stepCallbacks) {
            stepCallbacks.clear()
        }
        
        synchronized(stepSpecificCallbacks) {
            stepSpecificCallbacks.clear()
        }
    }
}
