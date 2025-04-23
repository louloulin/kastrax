package ai.kastrax.core.workflow.state

import ai.kastrax.core.workflow.WorkflowResult
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * 工作流状态存储接口，用于保存和恢复工作流执行状态。
 */
interface WorkflowStateStorage {
    /**
     * 保存工作流状态。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param state 工作流状态
     * @return 是否保存成功
     */
    suspend fun saveWorkflowState(workflowId: String, runId: String, state: WorkflowState): Boolean

    /**
     * 获取工作流状态。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @return 工作流状态，如果不存在则返回null
     */
    suspend fun getWorkflowState(workflowId: String, runId: String): WorkflowState?

    /**
     * 获取工作流的所有运行状态。
     *
     * @param workflowId 工作流ID
     * @param limit 限制返回的数量
     * @param offset 偏移量
     * @return 工作流运行状态列表
     */
    suspend fun getWorkflowRuns(workflowId: String, limit: Int = 10, offset: Int = 0): List<WorkflowRunInfo>

    /**
     * 删除工作流状态。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @return 是否删除成功
     */
    suspend fun deleteWorkflowState(workflowId: String, runId: String): Boolean
}

/**
 * 工作流状态。
 *
 * @property runId 运行ID
 * @property workflowId 工作流ID
 * @property status 状态
 * @property context 上下文数据
 * @property steps 步骤状态
 * @property suspendedSteps 暂停的步骤
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 */
@Serializable
data class WorkflowState(
    val runId: String,
    val workflowId: String,
    val status: WorkflowStateStatus,
    val context: Map<String, JsonElement>,
    val steps: Map<String, StepState>,
    val suspendedSteps: Set<String> = emptySet(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 工作流状态状态枚举。
 */
enum class WorkflowStateStatus {
    /**
     * 运行中。
     */
    RUNNING,

    /**
     * 已完成。
     */
    COMPLETED,

    /**
     * 失败。
     */
    FAILED,

    /**
     * 已暂停。
     */
    SUSPENDED,

    /**
     * 等待输入。
     */
    WAITING,

    /**
     * 已取消。
     */
    CANCELED
}

/**
 * 步骤状态。
 *
 * @property stepId 步骤ID
 * @property status 状态
 * @property output 输出数据
 * @property error 错误信息
 * @property executionTime 执行时间
 * @property suspendPayload 暂停时的附加数据
 */
@Serializable
data class StepState(
    val stepId: String,
    val status: StepStateStatus,
    val output: Map<String, JsonElement> = emptyMap(),
    val error: String? = null,
    val executionTime: Long = 0,
    @Contextual val suspendPayload: Any? = null
)

/**
 * 步骤状态状态枚举。
 */
enum class StepStateStatus {
    /**
     * 已完成。
     */
    COMPLETED,

    /**
     * 失败。
     */
    FAILED,

    /**
     * 已暂停。
     */
    SUSPENDED,

    /**
     * 等待输入。
     */
    WAITING,

    /**
     * 已跳过。
     */
    SKIPPED
}

/**
 * 工作流运行信息。
 *
 * @property runId 运行ID
 * @property workflowId 工作流ID
 * @property status 状态
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 */
@Serializable
data class WorkflowRunInfo(
    val runId: String,
    val workflowId: String,
    val status: WorkflowStateStatus,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * 工作流结果转换为工作流状态。
 */
fun WorkflowResult.toWorkflowState(
    runId: String,
    workflowId: String,
    context: Map<String, JsonElement> = emptyMap(),
    suspendedSteps: Set<String> = emptySet()
): WorkflowState {
    val status = when {
        !success -> WorkflowStateStatus.FAILED
        suspendedSteps.isNotEmpty() -> WorkflowStateStatus.SUSPENDED
        else -> WorkflowStateStatus.COMPLETED
    }

    val stepStates = steps.mapValues { (stepId, result) ->
        StepState(
            stepId = stepId,
            status = when {
                !result.success -> StepStateStatus.FAILED
                stepId in suspendedSteps -> StepStateStatus.SUSPENDED
                else -> StepStateStatus.COMPLETED
            },
            output = result.output.mapValues { (_, value) ->
                if (value is JsonElement) value else JsonPrimitive(value.toString())
            },
            error = result.error,
            executionTime = result.executionTime,
            suspendPayload = if (stepId in suspendedSteps) result.suspendPayload else null
        )
    }

    return WorkflowState(
        runId = runId,
        workflowId = workflowId,
        status = status,
        context = context,
        steps = stepStates,
        suspendedSteps = suspendedSteps,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}
