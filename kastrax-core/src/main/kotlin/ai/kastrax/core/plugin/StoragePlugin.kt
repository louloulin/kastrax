package ai.kastrax.core.plugin

import ai.kastrax.core.workflow.state.WorkflowState
import ai.kastrax.core.workflow.state.WorkflowStateStorage

/**
 * 存储插件接口，用于提供自定义存储实现。
 */
interface StoragePlugin : Plugin {
    /**
     * 获取此插件提供的存储类型列表。
     *
     * @return 存储类型列表
     */
    fun getStorageTypes(): List<StorageType>
    
    /**
     * 创建工作流状态存储实例。
     *
     * @param storageType 存储类型
     * @param config 存储配置
     * @return 工作流状态存储实例
     */
    fun createWorkflowStateStorage(storageType: String, config: Map<String, Any?>): WorkflowStateStorage?
    
    /**
     * 创建事件存储实例。
     *
     * @param storageType 存储类型
     * @param config 存储配置
     * @return 事件存储实例
     */
    fun createEventStorage(storageType: String, config: Map<String, Any?>): EventStorage?
    
    /**
     * 创建数据存储实例。
     *
     * @param storageType 存储类型
     * @param config 存储配置
     * @return 数据存储实例
     */
    fun createDataStorage(storageType: String, config: Map<String, Any?>): DataStorage?
}

/**
 * 存储类型，描述了一种存储的类型。
 */
data class StorageType(
    /**
     * 存储类型的唯一标识符。
     */
    val id: String,
    
    /**
     * 存储类型的名称。
     */
    val name: String,
    
    /**
     * 存储类型的描述。
     */
    val description: String,
    
    /**
     * 存储类型的配置模式，描述了存储配置的结构。
     */
    val configSchema: Map<String, ConfigField> = emptyMap(),
    
    /**
     * 存储类型支持的功能。
     */
    val capabilities: Set<StorageCapability> = emptySet(),
    
    /**
     * 存储类型的分类。
     */
    val category: String = "其他",
    
    /**
     * 存储类型的标签。
     */
    val tags: List<String> = emptyList()
)

/**
 * 存储能力枚举，描述了存储支持的功能。
 */
enum class StorageCapability {
    /**
     * 支持工作流状态存储。
     */
    WORKFLOW_STATE,
    
    /**
     * 支持事件存储。
     */
    EVENT,
    
    /**
     * 支持数据存储。
     */
    DATA,
    
    /**
     * 支持事务。
     */
    TRANSACTION,
    
    /**
     * 支持查询。
     */
    QUERY,
    
    /**
     * 支持索引。
     */
    INDEX,
    
    /**
     * 支持分页。
     */
    PAGINATION,
    
    /**
     * 支持排序。
     */
    SORTING,
    
    /**
     * 支持过滤。
     */
    FILTERING,
    
    /**
     * 支持聚合。
     */
    AGGREGATION,
    
    /**
     * 支持全文搜索。
     */
    FULL_TEXT_SEARCH,
    
    /**
     * 支持地理空间查询。
     */
    GEOSPATIAL
}

/**
 * 事件存储接口，用于存储和检索事件。
 */
interface EventStorage {
    /**
     * 存储事件。
     *
     * @param event 事件
     * @return 存储是否成功
     */
    suspend fun storeEvent(event: Event): Boolean
    
    /**
     * 批量存储事件。
     *
     * @param events 事件列表
     * @return 存储是否成功
     */
    suspend fun storeEvents(events: List<Event>): Boolean
    
    /**
     * 获取事件。
     *
     * @param eventId 事件ID
     * @return 事件，如果不存在则返回null
     */
    suspend fun getEvent(eventId: String): Event?
    
    /**
     * 获取指定类型的事件。
     *
     * @param eventType 事件类型
     * @param limit 限制数量
     * @param offset 偏移量
     * @return 事件列表
     */
    suspend fun getEventsByType(eventType: String, limit: Int = 100, offset: Int = 0): List<Event>
    
    /**
     * 获取指定工作流的事件。
     *
     * @param workflowId 工作流ID
     * @param limit 限制数量
     * @param offset 偏移量
     * @return 事件列表
     */
    suspend fun getEventsByWorkflow(workflowId: String, limit: Int = 100, offset: Int = 0): List<Event>
    
    /**
     * 获取指定工作流运行的事件。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @param limit 限制数量
     * @param offset 偏移量
     * @return 事件列表
     */
    suspend fun getEventsByWorkflowRun(workflowId: String, runId: String, limit: Int = 100, offset: Int = 0): List<Event>
    
    /**
     * 删除事件。
     *
     * @param eventId 事件ID
     * @return 删除是否成功
     */
    suspend fun deleteEvent(eventId: String): Boolean
    
    /**
     * 删除指定工作流的所有事件。
     *
     * @param workflowId 工作流ID
     * @return 删除是否成功
     */
    suspend fun deleteEventsByWorkflow(workflowId: String): Boolean
    
    /**
     * 删除指定工作流运行的所有事件。
     *
     * @param workflowId 工作流ID
     * @param runId 运行ID
     * @return 删除是否成功
     */
    suspend fun deleteEventsByWorkflowRun(workflowId: String, runId: String): Boolean
}

/**
 * 事件接口，描述了一个事件。
 */
interface Event {
    /**
     * 事件的唯一标识符。
     */
    val id: String
    
    /**
     * 事件的类型。
     */
    val type: String
    
    /**
     * 事件的时间戳。
     */
    val timestamp: Long
    
    /**
     * 事件的来源。
     */
    val source: String
    
    /**
     * 事件的数据。
     */
    val data: Map<String, Any?>
    
    /**
     * 事件的元数据。
     */
    val metadata: Map<String, Any?>
}

/**
 * 数据存储接口，用于存储和检索数据。
 */
interface DataStorage {
    /**
     * 存储数据。
     *
     * @param collection 集合名称
     * @param id 数据ID
     * @param data 数据
     * @return 存储是否成功
     */
    suspend fun storeData(collection: String, id: String, data: Map<String, Any?>): Boolean
    
    /**
     * 批量存储数据。
     *
     * @param collection 集合名称
     * @param dataList 数据列表，每个数据是一个ID和数据的对
     * @return 存储是否成功
     */
    suspend fun storeDataBatch(collection: String, dataList: List<Pair<String, Map<String, Any?>>>): Boolean
    
    /**
     * 获取数据。
     *
     * @param collection 集合名称
     * @param id 数据ID
     * @return 数据，如果不存在则返回null
     */
    suspend fun getData(collection: String, id: String): Map<String, Any?>?
    
    /**
     * 获取集合中的所有数据。
     *
     * @param collection 集合名称
     * @param limit 限制数量
     * @param offset 偏移量
     * @return 数据列表
     */
    suspend fun getAllData(collection: String, limit: Int = 100, offset: Int = 0): List<Pair<String, Map<String, Any?>>>
    
    /**
     * 查询数据。
     *
     * @param collection 集合名称
     * @param query 查询条件
     * @param limit 限制数量
     * @param offset 偏移量
     * @return 数据列表
     */
    suspend fun queryData(collection: String, query: Map<String, Any?>, limit: Int = 100, offset: Int = 0): List<Pair<String, Map<String, Any?>>>
    
    /**
     * 更新数据。
     *
     * @param collection 集合名称
     * @param id 数据ID
     * @param data 数据
     * @return 更新是否成功
     */
    suspend fun updateData(collection: String, id: String, data: Map<String, Any?>): Boolean
    
    /**
     * 删除数据。
     *
     * @param collection 集合名称
     * @param id 数据ID
     * @return 删除是否成功
     */
    suspend fun deleteData(collection: String, id: String): Boolean
    
    /**
     * 删除集合中的所有数据。
     *
     * @param collection 集合名称
     * @return 删除是否成功
     */
    suspend fun deleteAllData(collection: String): Boolean
}

/**
 * 抽象存储插件基类，提供了StoragePlugin接口的基本实现。
 */
abstract class AbstractStoragePlugin(
    id: String,
    name: String,
    description: String,
    version: String,
    author: String,
    config: PluginConfig = DefaultPluginConfig()
) : AbstractPlugin(
    id = id,
    name = name,
    description = description,
    version = version,
    author = author,
    type = PluginType.STORAGE,
    config = config
), StoragePlugin
