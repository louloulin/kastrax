package ai.kastrax.core.workflow.event

import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.state.WorkflowState
import java.time.Instant

/**
 * 工作流事件类型枚举。
 */
enum class WorkflowEventType {
    // 工作流生命周期事件
    WORKFLOW_STARTED,
    WORKFLOW_COMPLETED,
    WORKFLOW_FAILED,
    WORKFLOW_SUSPENDED,
    WORKFLOW_RESUMED,
    WORKFLOW_CANCELED,
    
    // 步骤生命周期事件
    STEP_STARTED,
    STEP_COMPLETED,
    STEP_FAILED,
    STEP_SUSPENDED,
    STEP_RESUMED,
    STEP_SKIPPED,
    
    // 错误处理事件
    ERROR_OCCURRED,
    RETRY_STARTED,
    RECOVERY_APPLIED,
    
    // 数据流事件
    VARIABLE_CHANGED,
    
    // 自定义事件
    CUSTOM
}

/**
 * 工作流事件接口。
 */
interface WorkflowEvent {
    /**
     * 事件类型。
     */
    val type: WorkflowEventType
    
    /**
     * 事件发生时间。
     */
    val timestamp: Long
    
    /**
     * 工作流ID。
     */
    val workflowId: String
    
    /**
     * 运行ID。
     */
    val runId: String
    
    /**
     * 事件相关的步骤ID（如果适用）。
     */
    val stepId: String?
    
    /**
     * 事件相关的数据。
     */
    val data: Map<String, Any?>
}

/**
 * 工作流事件基类。
 */
abstract class BaseWorkflowEvent(
    override val type: WorkflowEventType,
    override val workflowId: String,
    override val runId: String,
    override val stepId: String? = null,
    override val data: Map<String, Any?> = emptyMap()
) : WorkflowEvent {
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 工作流开始事件。
 */
class WorkflowStartedEvent(
    workflowId: String,
    runId: String,
    val input: Map<String, Any?>
) : BaseWorkflowEvent(
    type = WorkflowEventType.WORKFLOW_STARTED,
    workflowId = workflowId,
    runId = runId,
    data = mapOf("input" to input)
)

/**
 * 工作流完成事件。
 */
class WorkflowCompletedEvent(
    workflowId: String,
    runId: String,
    val output: Map<String, Any?>,
    val executionTime: Long
) : BaseWorkflowEvent(
    type = WorkflowEventType.WORKFLOW_COMPLETED,
    workflowId = workflowId,
    runId = runId,
    data = mapOf(
        "output" to output,
        "executionTime" to executionTime
    )
)

/**
 * 工作流失败事件。
 */
class WorkflowFailedEvent(
    workflowId: String,
    runId: String,
    val error: String?,
    val executionTime: Long
) : BaseWorkflowEvent(
    type = WorkflowEventType.WORKFLOW_FAILED,
    workflowId = workflowId,
    runId = runId,
    data = mapOf(
        "error" to error,
        "executionTime" to executionTime
    )
)

/**
 * 工作流暂停事件。
 */
class WorkflowSuspendedEvent(
    workflowId: String,
    runId: String,
    val suspendedStepIds: Set<String>,
    val state: WorkflowState
) : BaseWorkflowEvent(
    type = WorkflowEventType.WORKFLOW_SUSPENDED,
    workflowId = workflowId,
    runId = runId,
    data = mapOf(
        "suspendedStepIds" to suspendedStepIds,
        "state" to state
    )
)

/**
 * 工作流恢复事件。
 */
class WorkflowResumedEvent(
    workflowId: String,
    runId: String,
    val resumedStepId: String,
    val input: Map<String, Any?>
) : BaseWorkflowEvent(
    type = WorkflowEventType.WORKFLOW_RESUMED,
    workflowId = workflowId,
    runId = runId,
    stepId = resumedStepId,
    data = mapOf("input" to input)
)

/**
 * 工作流取消事件。
 */
class WorkflowCanceledEvent(
    workflowId: String,
    runId: String,
    val reason: String?
) : BaseWorkflowEvent(
    type = WorkflowEventType.WORKFLOW_CANCELED,
    workflowId = workflowId,
    runId = runId,
    data = mapOf("reason" to reason)
)

/**
 * 步骤开始事件。
 */
class StepStartedEvent(
    workflowId: String,
    runId: String,
    override val stepId: String,
    val input: Map<String, Any?>
) : BaseWorkflowEvent(
    type = WorkflowEventType.STEP_STARTED,
    workflowId = workflowId,
    runId = runId,
    stepId = stepId,
    data = mapOf("input" to input)
)

/**
 * 步骤完成事件。
 */
class StepCompletedEvent(
    workflowId: String,
    runId: String,
    override val stepId: String,
    val result: WorkflowStepResult
) : BaseWorkflowEvent(
    type = WorkflowEventType.STEP_COMPLETED,
    workflowId = workflowId,
    runId = runId,
    stepId = stepId,
    data = mapOf("result" to result)
)

/**
 * 步骤失败事件。
 */
class StepFailedEvent(
    workflowId: String,
    runId: String,
    override val stepId: String,
    val error: String?,
    val exception: Throwable?
) : BaseWorkflowEvent(
    type = WorkflowEventType.STEP_FAILED,
    workflowId = workflowId,
    runId = runId,
    stepId = stepId,
    data = mapOf(
        "error" to error,
        "exceptionClass" to exception?.javaClass?.name,
        "exceptionMessage" to exception?.message
    )
)

/**
 * 步骤暂停事件。
 */
class StepSuspendedEvent(
    workflowId: String,
    runId: String,
    override val stepId: String,
    val suspensionData: Map<String, Any?>
) : BaseWorkflowEvent(
    type = WorkflowEventType.STEP_SUSPENDED,
    workflowId = workflowId,
    runId = runId,
    stepId = stepId,
    data = mapOf("suspensionData" to suspensionData)
)

/**
 * 步骤恢复事件。
 */
class StepResumedEvent(
    workflowId: String,
    runId: String,
    override val stepId: String,
    val resumeData: Map<String, Any?>
) : BaseWorkflowEvent(
    type = WorkflowEventType.STEP_RESUMED,
    workflowId = workflowId,
    runId = runId,
    stepId = stepId,
    data = mapOf("resumeData" to resumeData)
)

/**
 * 步骤跳过事件。
 */
class StepSkippedEvent(
    workflowId: String,
    runId: String,
    override val stepId: String,
    val reason: String
) : BaseWorkflowEvent(
    type = WorkflowEventType.STEP_SKIPPED,
    workflowId = workflowId,
    runId = runId,
    stepId = stepId,
    data = mapOf("reason" to reason)
)

/**
 * 错误发生事件。
 */
class ErrorOccurredEvent(
    workflowId: String,
    runId: String,
    override val stepId: String?,
    val error: Throwable,
    val context: Map<String, Any?>
) : BaseWorkflowEvent(
    type = WorkflowEventType.ERROR_OCCURRED,
    workflowId = workflowId,
    runId = runId,
    stepId = stepId,
    data = mapOf(
        "errorClass" to error.javaClass.name,
        "errorMessage" to error.message,
        "context" to context
    )
)

/**
 * 重试开始事件。
 */
class RetryStartedEvent(
    workflowId: String,
    runId: String,
    override val stepId: String,
    val attempt: Int,
    val maxRetries: Int,
    val delay: Long
) : BaseWorkflowEvent(
    type = WorkflowEventType.RETRY_STARTED,
    workflowId = workflowId,
    runId = runId,
    stepId = stepId,
    data = mapOf(
        "attempt" to attempt,
        "maxRetries" to maxRetries,
        "delay" to delay
    )
)

/**
 * 恢复策略应用事件。
 */
class RecoveryAppliedEvent(
    workflowId: String,
    runId: String,
    override val stepId: String,
    val strategyType: String,
    val success: Boolean
) : BaseWorkflowEvent(
    type = WorkflowEventType.RECOVERY_APPLIED,
    workflowId = workflowId,
    runId = runId,
    stepId = stepId,
    data = mapOf(
        "strategyType" to strategyType,
        "success" to success
    )
)

/**
 * 变量变更事件。
 */
class VariableChangedEvent(
    workflowId: String,
    runId: String,
    override val stepId: String?,
    val variableName: String,
    val oldValue: Any?,
    val newValue: Any?,
    val scope: String
) : BaseWorkflowEvent(
    type = WorkflowEventType.VARIABLE_CHANGED,
    workflowId = workflowId,
    runId = runId,
    stepId = stepId,
    data = mapOf(
        "variableName" to variableName,
        "oldValue" to oldValue,
        "newValue" to newValue,
        "scope" to scope
    )
)

/**
 * 自定义事件。
 */
class CustomWorkflowEvent(
    workflowId: String,
    runId: String,
    override val stepId: String?,
    val eventName: String,
    val eventData: Map<String, Any?>
) : BaseWorkflowEvent(
    type = WorkflowEventType.CUSTOM,
    workflowId = workflowId,
    runId = runId,
    stepId = stepId,
    data = mapOf(
        "eventName" to eventName,
        "eventData" to eventData
    )
)
