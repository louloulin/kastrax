package ai.kastrax.core.workflow.statemachine

import java.util.concurrent.ConcurrentHashMap

/**
 * 工作流状态。
 */
enum class WorkflowState {
    /**
     * 初始状态。
     */
    INITIAL,

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
 * 工作流事件。
 */
sealed class WorkflowEvent {
    /**
     * 开始事件。
     */
    object Start : WorkflowEvent()

    /**
     * 完成事件。
     *
     * @property output 输出数据
     */
    data class Complete(val output: Map<String, Any?>) : WorkflowEvent()

    /**
     * 失败事件。
     *
     * @property error 错误信息
     */
    data class Fail(val error: String) : WorkflowEvent()

    /**
     * 暂停事件。
     *
     * @property stepId 步骤ID
     */
    data class Suspend(val stepId: String) : WorkflowEvent()

    /**
     * 恢复事件。
     *
     * @property stepId 步骤ID
     * @property input 输入数据
     */
    data class Resume(val stepId: String, val input: Map<String, Any?>) : WorkflowEvent()

    /**
     * 等待事件。
     *
     * @property stepId 步骤ID
     */
    data class Wait(val stepId: String) : WorkflowEvent()

    /**
     * 取消事件。
     */
    object Cancel : WorkflowEvent()
}

/**
 * 状态转换结果。
 *
 * @property nextState 下一个状态
 * @property output 输出数据
 * @property error 错误信息
 */
data class StateTransitionResult(
    val nextState: WorkflowState,
    val output: Map<String, Any?> = emptyMap(),
    val error: String? = null
)

/**
 * 简单状态机。
 */
class SimpleStateMachine {
    private val stateTransitions = ConcurrentHashMap<String, MutableMap<WorkflowState, WorkflowState>>()
    private val stateData = ConcurrentHashMap<String, MutableMap<String, Any?>>()

    /**
     * 初始化状态。
     *
     * @param machineId 状态机ID
     * @param input 输入数据
     */
    fun initializeState(machineId: String, input: Map<String, Any?>) {
        // 初始化状态转换
        val transitions = mutableMapOf<WorkflowState, WorkflowState>()
        transitions[WorkflowState.INITIAL] = WorkflowState.RUNNING
        transitions[WorkflowState.RUNNING] = WorkflowState.COMPLETED
        stateTransitions[machineId] = transitions
        
        // 初始化状态数据
        stateData[machineId] = input.toMutableMap()
    }

    /**
     * 清理状态。
     *
     * @param machineId 状态机ID
     */
    fun cleanupState(machineId: String) {
        stateTransitions.remove(machineId)
        stateData.remove(machineId)
    }

    /**
     * 状态转换。
     *
     * @param machineId 状态机ID
     * @param currentState 当前状态
     * @param event 事件
     * @return 状态转换结果
     */
    fun transition(machineId: String, currentState: WorkflowState, event: WorkflowEvent): StateTransitionResult {
        val transitions = stateTransitions[machineId] ?: return StateTransitionResult(currentState)
        val data = stateData[machineId] ?: mutableMapOf()
        
        return when (event) {
            is WorkflowEvent.Start -> {
                val nextState = transitions[currentState] ?: currentState
                StateTransitionResult(nextState)
            }
            is WorkflowEvent.Complete -> {
                val nextState = transitions[currentState] ?: currentState
                data.putAll(event.output)
                StateTransitionResult(nextState, event.output)
            }
            is WorkflowEvent.Fail -> {
                StateTransitionResult(WorkflowState.FAILED, error = event.error)
            }
            is WorkflowEvent.Suspend -> {
                StateTransitionResult(WorkflowState.SUSPENDED)
            }
            is WorkflowEvent.Resume -> {
                data.putAll(event.input)
                StateTransitionResult(WorkflowState.RUNNING, event.input)
            }
            is WorkflowEvent.Wait -> {
                StateTransitionResult(WorkflowState.WAITING)
            }
            is WorkflowEvent.Cancel -> {
                StateTransitionResult(WorkflowState.CANCELED)
            }
        }
    }
}
