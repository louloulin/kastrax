package ai.kastrax.core.workflow.statemachine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 状态机接口，定义了状态机的基本操作。
 *
 * @param S 状态类型
 * @param E 事件类型
 * @param C 上下文类型
 */
interface StateMachine<S, E, C> {
    /**
     * 获取当前状态。
     *
     * @return 当前状态
     */
    fun getCurrentState(): S

    /**
     * 获取状态流。
     *
     * @return 状态流
     */
    fun getStateFlow(): StateFlow<S>

    /**
     * 获取当前上下文。
     *
     * @return 当前上下文
     */
    fun getContext(): C

    /**
     * 发送事件。
     *
     * @param event 事件
     * @return 转换后的状态
     */
    suspend fun sendEvent(event: E): S

    /**
     * 流式发送事件，返回状态流。
     *
     * @param event 事件
     * @return 状态流
     */
    suspend fun streamEvent(event: E): Flow<S>

    /**
     * 重置状态机。
     *
     * @param initialState 初始状态
     * @param initialContext 初始上下文
     */
    fun reset(initialState: S, initialContext: C)
}

/**
 * 状态转换器接口，定义了状态转换的方法。
 *
 * @param S 状态类型
 * @param E 事件类型
 * @param C 上下文类型
 */
interface StateTransitioner<S, E, C> {
    /**
     * 转换状态。
     *
     * @param currentState 当前状态
     * @param event 事件
     * @param context 上下文
     * @return 转换结果
     */
    suspend fun transition(currentState: S, event: E, context: C): TransitionResult<S, C>
}

/**
 * 转换结果。
 *
 * @param S 状态类型
 * @param C 上下文类型
 * @property nextState 下一个状态
 * @property updatedContext 更新后的上下文
 * @property sideEffects 副作用
 */
data class TransitionResult<S, C>(
    val nextState: S,
    val updatedContext: C,
    val sideEffects: List<SideEffect<S, C>> = emptyList()
)

/**
 * 副作用接口，定义了副作用的执行方法。
 *
 * @param S 状态类型
 * @param C 上下文类型
 */
interface SideEffect<S, C> {
    /**
     * 执行副作用。
     *
     * @param state 状态
     * @param context 上下文
     */
    suspend fun execute(state: S, context: C)
}

/**
 * 基本状态机实现。
 *
 * @param S 状态类型
 * @param E 事件类型
 * @param C 上下文类型
 * @property initialState 初始状态
 * @property initialContext 初始上下文
 * @property transitioner 状态转换器
 */
class BasicStateMachine<S, E, C>(
    private val initialState: S,
    private val initialContext: C,
    private val transitioner: StateTransitioner<S, E, C>
) : StateMachine<S, E, C> {
    private val stateFlow = MutableStateFlow(initialState)
    private var context = initialContext

    override fun getCurrentState(): S = stateFlow.value

    override fun getStateFlow(): StateFlow<S> = stateFlow.asStateFlow()

    override fun getContext(): C = context

    override suspend fun sendEvent(event: E): S {
        val result = transitioner.transition(stateFlow.value, event, context)
        stateFlow.value = result.nextState
        context = result.updatedContext

        // 执行副作用
        result.sideEffects.forEach { it.execute(result.nextState, result.updatedContext) }

        return result.nextState
    }

    override suspend fun streamEvent(event: E): Flow<S> = flow {
        val result = transitioner.transition(stateFlow.value, event, context)
        stateFlow.value = result.nextState
        context = result.updatedContext
        emit(result.nextState)

        // 执行副作用
        result.sideEffects.forEach { it.execute(result.nextState, result.updatedContext) }
    }

    override fun reset(initialState: S, initialContext: C) {
        stateFlow.value = initialState
        context = initialContext
    }
}

/**
 * 状态机工厂，用于创建和管理状态机实例。
 *
 * @param S 状态类型
 * @param E 事件类型
 * @param C 上下文类型
 */
class StateMachineFactory<S, E, C>(
    private val initialState: S,
    private val initialContext: C,
    private val transitioner: StateTransitioner<S, E, C>
) {
    private val machines = ConcurrentHashMap<String, StateMachine<S, E, C>>()

    /**
     * 创建状态机。
     *
     * @param id 状态机ID，如果为null则自动生成
     * @return 状态机
     */
    fun createMachine(id: String? = null): Pair<String, StateMachine<S, E, C>> {
        val machineId = id ?: UUID.randomUUID().toString()
        val machine = BasicStateMachine(initialState, initialContext, transitioner)
        machines[machineId] = machine
        return machineId to machine
    }

    /**
     * 获取状态机。
     *
     * @param id 状态机ID
     * @return 状态机，如果不存在则返回null
     */
    fun getMachine(id: String): StateMachine<S, E, C>? = machines[id]

    /**
     * 删除状态机。
     *
     * @param id 状态机ID
     * @return 是否删除成功
     */
    fun deleteMachine(id: String): Boolean = machines.remove(id) != null

    /**
     * 获取所有状态机ID。
     *
     * @return 状态机ID列表
     */
    fun getAllMachineIds(): List<String> = machines.keys.toList()
}
