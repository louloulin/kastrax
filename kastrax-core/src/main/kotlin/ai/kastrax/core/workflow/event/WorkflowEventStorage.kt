package ai.kastrax.core.workflow.event

import ai.kastrax.core.common.KastraXBase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * 工作流事件存储接口。
 */
interface WorkflowEventStorage {
    /**
     * 保存工作流事件。
     *
     * @param event 工作流事件
     * @return 是否保存成功
     */
    suspend fun saveEvent(event: WorkflowEvent): Boolean
    
    /**
     * 获取工作流的所有事件。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param limit 限制返回的数量
     * @param offset 偏移量
     * @return 工作流事件列表
     */
    suspend fun getEvents(workflowId: String, runId: String, limit: Int = 100, offset: Int = 0): List<WorkflowEvent>
    
    /**
     * 获取特定类型的工作流事件。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param eventType 事件类型
     * @param limit 限制返回的数量
     * @param offset 偏移量
     * @return 工作流事件列表
     */
    suspend fun getEventsByType(
        workflowId: String,
        runId: String,
        eventType: WorkflowEventType,
        limit: Int = 100,
        offset: Int = 0
    ): List<WorkflowEvent>
    
    /**
     * 获取特定步骤的工作流事件。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param stepId 步骤ID
     * @param limit 限制返回的数量
     * @param offset 偏移量
     * @return 工作流事件列表
     */
    suspend fun getEventsByStep(
        workflowId: String,
        runId: String,
        stepId: String,
        limit: Int = 100,
        offset: Int = 0
    ): List<WorkflowEvent>
    
    /**
     * 删除工作流的所有事件。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @return 是否删除成功
     */
    suspend fun deleteEvents(workflowId: String, runId: String): Boolean
}

/**
 * 内存中的工作流事件存储实现。
 */
class InMemoryWorkflowEventStorage : WorkflowEventStorage, KastraXBase(component = "WORKFLOW_EVENT_STORAGE", name = "in-memory") {
    private val eventLogger = KotlinLogging.logger {}
    private val mutex = Mutex()
    
    // 使用ConcurrentHashMap存储事件，key为workflowId:runId
    private val events = ConcurrentHashMap<String, MutableList<WorkflowEvent>>()
    
    override suspend fun saveEvent(event: WorkflowEvent): Boolean {
        val key = "${event.workflowId}:${event.runId}"
        
        return mutex.withLock {
            val eventList = events.getOrPut(key) { mutableListOf() }
            eventList.add(event)
            eventLogger.debug { "保存事件: ${event.type}, 工作流: ${event.workflowId}, 运行ID: ${event.runId}" }
            true
        }
    }
    
    override suspend fun getEvents(workflowId: String, runId: String, limit: Int, offset: Int): List<WorkflowEvent> {
        val key = "$workflowId:$runId"
        
        return mutex.withLock {
            val eventList = events[key] ?: return emptyList()
            eventList.sortedBy { it.timestamp }.drop(offset).take(limit)
        }
    }
    
    override suspend fun getEventsByType(
        workflowId: String,
        runId: String,
        eventType: WorkflowEventType,
        limit: Int,
        offset: Int
    ): List<WorkflowEvent> {
        val key = "$workflowId:$runId"
        
        return mutex.withLock {
            val eventList = events[key] ?: return emptyList()
            eventList.filter { it.type == eventType }
                .sortedBy { it.timestamp }
                .drop(offset)
                .take(limit)
        }
    }
    
    override suspend fun getEventsByStep(
        workflowId: String,
        runId: String,
        stepId: String,
        limit: Int,
        offset: Int
    ): List<WorkflowEvent> {
        val key = "$workflowId:$runId"
        
        return mutex.withLock {
            val eventList = events[key] ?: return emptyList()
            eventList.filter { it.stepId == stepId }
                .sortedBy { it.timestamp }
                .drop(offset)
                .take(limit)
        }
    }
    
    override suspend fun deleteEvents(workflowId: String, runId: String): Boolean {
        val key = "$workflowId:$runId"
        
        return mutex.withLock {
            val removed = events.remove(key) != null
            if (removed) {
                eventLogger.debug { "删除事件: 工作流: $workflowId, 运行ID: $runId" }
            }
            removed
        }
    }
}

/**
 * 工作流事件重放器。
 */
class WorkflowEventReplayer(
    private val eventStorage: WorkflowEventStorage
) : KastraXBase(component = "WORKFLOW_EVENT_REPLAYER", name = "default") {
    private val replayLogger = KotlinLogging.logger {}
    
    /**
     * 重放工作流事件。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param eventBus 事件总线
     * @param filter 事件过滤器
     */
    suspend fun replayEvents(
        workflowId: String,
        runId: String,
        eventBus: WorkflowEventBus,
        filter: (WorkflowEvent) -> Boolean = { true }
    ) {
        replayLogger.info { "开始重放工作流事件: 工作流: $workflowId, 运行ID: $runId" }
        
        // 获取所有事件
        val events = eventStorage.getEvents(workflowId, runId)
        
        // 按时间戳排序
        val sortedEvents = events.sortedBy { it.timestamp }
        
        // 重放事件
        for (event in sortedEvents) {
            if (filter(event)) {
                replayLogger.debug { "重放事件: ${event.type}, 时间戳: ${event.timestamp}" }
                eventBus.publish(event)
            }
        }
        
        replayLogger.info { "完成重放工作流事件: 工作流: $workflowId, 运行ID: $runId, 总事件数: ${events.size}" }
    }
    
    /**
     * 重放特定类型的工作流事件。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param eventType 事件类型
     * @param eventBus 事件总线
     */
    suspend fun replayEventsByType(
        workflowId: String,
        runId: String,
        eventType: WorkflowEventType,
        eventBus: WorkflowEventBus
    ) {
        replayEvents(workflowId, runId, eventBus) { it.type == eventType }
    }
    
    /**
     * 重放特定步骤的工作流事件。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param stepId 步骤ID
     * @param eventBus 事件总线
     */
    suspend fun replayEventsByStep(
        workflowId: String,
        runId: String,
        stepId: String,
        eventBus: WorkflowEventBus
    ) {
        replayEvents(workflowId, runId, eventBus) { it.stepId == stepId }
    }
}
