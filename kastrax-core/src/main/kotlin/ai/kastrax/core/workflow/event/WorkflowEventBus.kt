package ai.kastrax.core.workflow.event

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal
import ai.kastrax.runtime.coroutines.Default
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import mu.KotlinLogging

/**
 * 工作流事件监听器接口。
 */
interface WorkflowEventListener {
    /**
     * 处理工作流事件。
     *
     * @param event 工作流事件
     */
    suspend fun onEvent(event: WorkflowEvent)

    /**
     * 获取监听器支持的事件类型。
     * 如果返回null，则监听所有事件类型。
     *
     * @return 支持的事件类型列表，如果为null则监听所有事件
     */
    fun getSupportedEventTypes(): List<WorkflowEventType>? = null

    /**
     * 获取监听器支持的工作流ID。
     * 如果返回null，则监听所有工作流。
     *
     * @return 支持的工作流ID列表，如果为null则监听所有工作流
     */
    fun getSupportedWorkflowIds(): List<String>? = null

    /**
     * 获取监听器支持的步骤ID。
     * 如果返回null，则监听所有步骤。
     *
     * @return 支持的步骤ID列表，如果为null则监听所有步骤
     */
    fun getSupportedStepIds(): List<String>? = null

    /**
     * 获取监听器的优先级。
     * 优先级越高，越先被调用。
     *
     * @return 监听器优先级
     */
    fun getPriority(): Int = 0
}

/**
 * 工作流事件总线接口。
 */
interface WorkflowEventBus {
    /**
     * 发布事件。
     *
     * @param event 工作流事件
     */
    suspend fun publish(event: WorkflowEvent)

    /**
     * 注册事件监听器。
     *
     * @param listener 工作流事件监听器
     */
    fun registerListener(listener: WorkflowEventListener)

    /**
     * 注销事件监听器。
     *
     * @param listener 工作流事件监听器
     */
    fun unregisterListener(listener: WorkflowEventListener)

    /**
     * 获取事件流。
     *
     * @return 工作流事件流
     */
    fun getEventFlow(): SharedFlow<WorkflowEvent>
}

/**
 * 默认工作流事件总线实现。
 */
class DefaultWorkflowEventBus : WorkflowEventBus, KastraXBase(component = "WORKFLOW_EVENT_BUS", name = "default") {
    private val eventLogger = KotlinLogging.logger {}
    private val listeners = mutableListOf<WorkflowEventListener>()
    private val eventScope = KastraxCoroutineGlobal.getScope("WorkflowEventBus")

    private val _eventFlow = MutableSharedFlow<WorkflowEvent>(replay = 0)
    private val eventFlow = _eventFlow.asSharedFlow()

    override suspend fun publish(event: WorkflowEvent) {
        eventLogger.debug { "发布事件: ${event.type}, 工作流: ${event.workflowId}, 运行ID: ${event.runId}, 步骤ID: ${event.stepId}" }

        // 发布事件到流
        _eventFlow.emit(event)

        // 获取匹配的监听器并按优先级排序
        val matchedListeners = listeners
            .filter { listener -> isListenerMatched(listener, event) }
            .sortedByDescending { it.getPriority() }

        // 异步调用监听器
        for (listener in matchedListeners) {
            eventScope.launch {
                try {
                    listener.onEvent(event)
                } catch (e: Exception) {
                    eventLogger.error(e) { "监听器处理事件失败: ${listener.javaClass.name}" }
                }
            }
        }
    }

    override fun registerListener(listener: WorkflowEventListener) {
        synchronized(listeners) {
            listeners.add(listener)
            eventLogger.debug { "注册监听器: ${listener.javaClass.name}" }
        }
    }

    override fun unregisterListener(listener: WorkflowEventListener) {
        synchronized(listeners) {
            listeners.remove(listener)
            eventLogger.debug { "注销监听器: ${listener.javaClass.name}" }
        }
    }

    override fun getEventFlow(): SharedFlow<WorkflowEvent> {
        return eventFlow
    }

    /**
     * 检查监听器是否匹配事件。
     *
     * @param listener 监听器
     * @param event 事件
     * @return 是否匹配
     */
    private fun isListenerMatched(listener: WorkflowEventListener, event: WorkflowEvent): Boolean {
        // 检查事件类型
        val supportedTypes = listener.getSupportedEventTypes()
        if (supportedTypes != null && !supportedTypes.contains(event.type)) {
            return false
        }

        // 检查工作流ID
        val supportedWorkflowIds = listener.getSupportedWorkflowIds()
        if (supportedWorkflowIds != null && !supportedWorkflowIds.contains(event.workflowId)) {
            return false
        }

        // 检查步骤ID
        val supportedStepIds = listener.getSupportedStepIds()
        if (supportedStepIds != null && event.stepId != null && !supportedStepIds.contains(event.stepId)) {
            return false
        }

        return true
    }
}
