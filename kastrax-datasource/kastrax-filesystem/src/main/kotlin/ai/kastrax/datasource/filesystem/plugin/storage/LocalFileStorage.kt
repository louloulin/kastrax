package ai.kastrax.datasource.filesystem.plugin.storage

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.plugin.DataStorage
import ai.kastrax.core.plugin.Event
import ai.kastrax.core.plugin.EventStorage
import ai.kastrax.core.workflow.state.WorkflowState
import ai.kastrax.core.workflow.state.WorkflowStateStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 本地文件存储工厂，用于创建基于本地文件系统的存储实现。
 */
object LocalFileStorage {
    private val json = Json { 
        prettyPrint = true 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * 创建工作流状态存储。
     */
    fun createStateStorage(
        id: String,
        name: String,
        rootPath: String,
        createIfNotExists: Boolean = true
    ): WorkflowStateStorage {
        return LocalFileWorkflowStateStorage(id, name, rootPath, createIfNotExists)
    }
    
    /**
     * 创建事件存储。
     */
    fun createEventStorage(
        id: String,
        name: String,
        rootPath: String,
        createIfNotExists: Boolean = true
    ): EventStorage {
        return LocalFileEventStorage(id, name, rootPath, createIfNotExists)
    }
    
    /**
     * 创建数据存储。
     */
    fun createDataStorage(
        id: String,
        name: String,
        rootPath: String,
        createIfNotExists: Boolean = true
    ): DataStorage {
        return LocalFileDataStorage(id, name, rootPath, createIfNotExists)
    }
}

/**
 * 本地文件工作流状态存储实现。
 */
class LocalFileWorkflowStateStorage(
    private val id: String,
    name: String,
    private val rootPath: String,
    private val createIfNotExists: Boolean = true
) : WorkflowStateStorage, KastraXBase(component = "STORAGE", name = "LocalFile-$name") {
    
    private val statesDir: Path = Paths.get(rootPath, "workflow-states")
    private val cache = ConcurrentHashMap<String, WorkflowState>()
    
    init {
        if (createIfNotExists) {
            Files.createDirectories(statesDir)
        } else if (!Files.exists(statesDir)) {
            throw IllegalArgumentException("工作流状态目录不存在: $statesDir")
        }
    }
    
    override suspend fun saveState(state: WorkflowState): Boolean {
        logger.debug { "保存工作流状态: ${state.workflowId}, 运行ID: ${state.runId}" }
        
        return try {
            val stateJson = json.encodeToString(state)
            val filePath = getStateFilePath(state.workflowId, state.runId)
            
            withContext(Dispatchers.IO) {
                Files.createDirectories(filePath.parent)
                Files.writeString(filePath, stateJson)
            }
            
            cache[getStateKey(state.workflowId, state.runId)] = state
            true
        } catch (e: Exception) {
            logger.error(e) { "保存工作流状态失败: ${state.workflowId}, 运行ID: ${state.runId}" }
            false
        }
    }
    
    override suspend fun getState(workflowId: String, runId: String): WorkflowState? {
        logger.debug { "获取工作流状态: $workflowId, 运行ID: $runId" }
        
        val key = getStateKey(workflowId, runId)
        val cachedState = cache[key]
        
        if (cachedState != null) {
            return cachedState
        }
        
        return try {
            val filePath = getStateFilePath(workflowId, runId)
            
            if (!Files.exists(filePath)) {
                logger.debug { "工作流状态文件不存在: $filePath" }
                return null
            }
            
            val stateJson = withContext(Dispatchers.IO) {
                Files.readString(filePath)
            }
            
            val state = json.decodeFromString<WorkflowState>(stateJson)
            cache[key] = state
            state
        } catch (e: Exception) {
            logger.error(e) { "获取工作流状态失败: $workflowId, 运行ID: $runId" }
            null
        }
    }
    
    override suspend fun deleteState(workflowId: String, runId: String): Boolean {
        logger.debug { "删除工作流状态: $workflowId, 运行ID: $runId" }
        
        return try {
            val filePath = getStateFilePath(workflowId, runId)
            
            val deleted = withContext(Dispatchers.IO) {
                Files.deleteIfExists(filePath)
            }
            
            if (deleted) {
                cache.remove(getStateKey(workflowId, runId))
            }
            
            deleted
        } catch (e: Exception) {
            logger.error(e) { "删除工作流状态失败: $workflowId, 运行ID: $runId" }
            false
        }
    }
    
    override suspend fun getStates(workflowId: String, limit: Int, offset: Int): List<WorkflowState> {
        logger.debug { "获取工作流状态列表: $workflowId, 限制: $limit, 偏移: $offset" }
        
        return try {
            val workflowDir = statesDir.resolve(workflowId)
            
            if (!Files.exists(workflowDir)) {
                return emptyList()
            }
            
            withContext(Dispatchers.IO) {
                Files.list(workflowDir)
                    .filter { Files.isRegularFile(it) }
                    .map { 
                        try {
                            val stateJson = Files.readString(it)
                            json.decodeFromString<WorkflowState>(stateJson)
                        } catch (e: Exception) {
                            logger.error(e) { "解析工作流状态文件失败: $it" }
                            null
                        }
                    }
                    .filter { it != null }
                    .map { it!! }
                    .sorted(compareByDescending { it.timestamp })
                    .skip(offset.toLong())
                    .limit(limit.toLong())
                    .toList()
            }
        } catch (e: Exception) {
            logger.error(e) { "获取工作流状态列表失败: $workflowId" }
            emptyList()
        }
    }
    
    override suspend fun getAllStates(limit: Int, offset: Int): List<WorkflowState> {
        logger.debug { "获取所有工作流状态: 限制: $limit, 偏移: $offset" }
        
        return try {
            if (!Files.exists(statesDir)) {
                return emptyList()
            }
            
            withContext(Dispatchers.IO) {
                Files.walk(statesDir)
                    .filter { Files.isRegularFile(it) }
                    .map { 
                        try {
                            val stateJson = Files.readString(it)
                            json.decodeFromString<WorkflowState>(stateJson)
                        } catch (e: Exception) {
                            logger.error(e) { "解析工作流状态文件失败: $it" }
                            null
                        }
                    }
                    .filter { it != null }
                    .map { it!! }
                    .sorted(compareByDescending { it.timestamp })
                    .skip(offset.toLong())
                    .limit(limit.toLong())
                    .toList()
            }
        } catch (e: Exception) {
            logger.error(e) { "获取所有工作流状态失败" }
            emptyList()
        }
    }
    
    private fun getStateKey(workflowId: String, runId: String): String {
        return "$workflowId:$runId"
    }
    
    private fun getStateFilePath(workflowId: String, runId: String): Path {
        return statesDir.resolve(workflowId).resolve("$runId.json")
    }
}

/**
 * 本地文件事件存储实现。
 */
class LocalFileEventStorage(
    private val id: String,
    name: String,
    private val rootPath: String,
    private val createIfNotExists: Boolean = true
) : EventStorage, KastraXBase(component = "STORAGE", name = "LocalFile-$name") {
    
    private val eventsDir: Path = Paths.get(rootPath, "events")
    
    init {
        if (createIfNotExists) {
            Files.createDirectories(eventsDir)
        } else if (!Files.exists(eventsDir)) {
            throw IllegalArgumentException("事件目录不存在: $eventsDir")
        }
    }
    
    override suspend fun storeEvent(event: Event): Boolean {
        logger.debug { "存储事件: ${event.id}, 类型: ${event.type}" }
        
        return try {
            val eventJson = json.encodeToString(EventWrapper(event))
            val filePath = getEventFilePath(event)
            
            withContext(Dispatchers.IO) {
                Files.createDirectories(filePath.parent)
                Files.writeString(filePath, eventJson)
            }
            
            true
        } catch (e: Exception) {
            logger.error(e) { "存储事件失败: ${event.id}, 类型: ${event.type}" }
            false
        }
    }
    
    override suspend fun storeEvents(events: List<Event>): Boolean {
        logger.debug { "批量存储事件: ${events.size}个事件" }
        
        var success = true
        
        for (event in events) {
            val result = storeEvent(event)
            if (!result) {
                success = false
            }
        }
        
        return success
    }
    
    override suspend fun getEvent(eventId: String): Event? {
        logger.debug { "获取事件: $eventId" }
        
        return try {
            // 由于我们不知道事件的类型，需要搜索所有可能的位置
            val eventFiles = withContext(Dispatchers.IO) {
                Files.walk(eventsDir)
                    .filter { Files.isRegularFile(it) && it.fileName.toString() == "$eventId.json" }
                    .toList()
            }
            
            if (eventFiles.isEmpty()) {
                logger.debug { "事件文件不存在: $eventId" }
                return null
            }
            
            val eventFile = eventFiles.first()
            val eventJson = withContext(Dispatchers.IO) {
                Files.readString(eventFile)
            }
            
            val eventWrapper = json.decodeFromString<EventWrapper>(eventJson)
            eventWrapper.event
        } catch (e: Exception) {
            logger.error(e) { "获取事件失败: $eventId" }
            null
        }
    }
    
    override suspend fun getEventsByType(eventType: String, limit: Int, offset: Int): List<Event> {
        logger.debug { "获取指定类型的事件: $eventType, 限制: $limit, 偏移: $offset" }
        
        return try {
            val typeDir = eventsDir.resolve("type").resolve(eventType)
            
            if (!Files.exists(typeDir)) {
                return emptyList()
            }
            
            withContext(Dispatchers.IO) {
                Files.list(typeDir)
                    .filter { Files.isRegularFile(it) }
                    .map { 
                        try {
                            val eventJson = Files.readString(it)
                            json.decodeFromString<EventWrapper>(eventJson).event
                        } catch (e: Exception) {
                            logger.error(e) { "解析事件文件失败: $it" }
                            null
                        }
                    }
                    .filter { it != null }
                    .map { it!! }
                    .sorted(compareByDescending { it.timestamp })
                    .skip(offset.toLong())
                    .limit(limit.toLong())
                    .toList()
            }
        } catch (e: Exception) {
            logger.error(e) { "获取指定类型的事件失败: $eventType" }
            emptyList()
        }
    }
    
    override suspend fun getEventsByWorkflow(workflowId: String, limit: Int, offset: Int): List<Event> {
        logger.debug { "获取指定工作流的事件: $workflowId, 限制: $limit, 偏移: $offset" }
        
        return try {
            val workflowDir = eventsDir.resolve("workflow").resolve(workflowId)
            
            if (!Files.exists(workflowDir)) {
                return emptyList()
            }
            
            withContext(Dispatchers.IO) {
                Files.list(workflowDir)
                    .filter { Files.isRegularFile(it) }
                    .map { 
                        try {
                            val eventJson = Files.readString(it)
                            json.decodeFromString<EventWrapper>(eventJson).event
                        } catch (e: Exception) {
                            logger.error(e) { "解析事件文件失败: $it" }
                            null
                        }
                    }
                    .filter { it != null }
                    .map { it!! }
                    .sorted(compareByDescending { it.timestamp })
                    .skip(offset.toLong())
                    .limit(limit.toLong())
                    .toList()
            }
        } catch (e: Exception) {
            logger.error(e) { "获取指定工作流的事件失败: $workflowId" }
            emptyList()
        }
    }
    
    override suspend fun getEventsByWorkflowRun(workflowId: String, runId: String, limit: Int, offset: Int): List<Event> {
        logger.debug { "获取指定工作流运行的事件: $workflowId, 运行ID: $runId, 限制: $limit, 偏移: $offset" }
        
        return try {
            val runDir = eventsDir.resolve("workflow").resolve(workflowId).resolve(runId)
            
            if (!Files.exists(runDir)) {
                return emptyList()
            }
            
            withContext(Dispatchers.IO) {
                Files.list(runDir)
                    .filter { Files.isRegularFile(it) }
                    .map { 
                        try {
                            val eventJson = Files.readString(it)
                            json.decodeFromString<EventWrapper>(eventJson).event
                        } catch (e: Exception) {
                            logger.error(e) { "解析事件文件失败: $it" }
                            null
                        }
                    }
                    .filter { it != null }
                    .map { it!! }
                    .sorted(compareByDescending { it.timestamp })
                    .skip(offset.toLong())
                    .limit(limit.toLong())
                    .toList()
            }
        } catch (e: Exception) {
            logger.error(e) { "获取指定工作流运行的事件失败: $workflowId, 运行ID: $runId" }
            emptyList()
        }
    }
    
    override suspend fun deleteEvent(eventId: String): Boolean {
        logger.debug { "删除事件: $eventId" }
        
        return try {
            // 由于我们不知道事件的类型，需要搜索所有可能的位置
            val eventFiles = withContext(Dispatchers.IO) {
                Files.walk(eventsDir)
                    .filter { Files.isRegularFile(it) && it.fileName.toString() == "$eventId.json" }
                    .toList()
            }
            
            if (eventFiles.isEmpty()) {
                logger.debug { "事件文件不存在: $eventId" }
                return false
            }
            
            var success = true
            
            for (eventFile in eventFiles) {
                val deleted = withContext(Dispatchers.IO) {
                    Files.deleteIfExists(eventFile)
                }
                
                if (!deleted) {
                    success = false
                }
            }
            
            success
        } catch (e: Exception) {
            logger.error(e) { "删除事件失败: $eventId" }
            false
        }
    }
    
    override suspend fun deleteEventsByWorkflow(workflowId: String): Boolean {
        logger.debug { "删除指定工作流的所有事件: $workflowId" }
        
        return try {
            val workflowDir = eventsDir.resolve("workflow").resolve(workflowId)
            
            if (!Files.exists(workflowDir)) {
                return true
            }
            
            withContext(Dispatchers.IO) {
                Files.walk(workflowDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach { Files.delete(it) }
            }
            
            true
        } catch (e: Exception) {
            logger.error(e) { "删除指定工作流的所有事件失败: $workflowId" }
            false
        }
    }
    
    override suspend fun deleteEventsByWorkflowRun(workflowId: String, runId: String): Boolean {
        logger.debug { "删除指定工作流运行的所有事件: $workflowId, 运行ID: $runId" }
        
        return try {
            val runDir = eventsDir.resolve("workflow").resolve(workflowId).resolve(runId)
            
            if (!Files.exists(runDir)) {
                return true
            }
            
            withContext(Dispatchers.IO) {
                Files.walk(runDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach { Files.delete(it) }
            }
            
            true
        } catch (e: Exception) {
            logger.error(e) { "删除指定工作流运行的所有事件失败: $workflowId, 运行ID: $runId" }
            false
        }
    }
    
    private fun getEventFilePath(event: Event): Path {
        // 存储在多个位置以便于不同的查询
        val paths = mutableListOf<Path>()
        
        // 按ID存储
        paths.add(eventsDir.resolve("id").resolve("${event.id}.json"))
        
        // 按类型存储
        paths.add(eventsDir.resolve("type").resolve(event.type).resolve("${event.id}.json"))
        
        // 如果有工作流ID和运行ID，按工作流和运行ID存储
        val metadata = event.metadata
        val workflowId = metadata["workflowId"] as? String
        val runId = metadata["runId"] as? String
        
        if (workflowId != null) {
            paths.add(eventsDir.resolve("workflow").resolve(workflowId).resolve("${event.id}.json"))
            
            if (runId != null) {
                paths.add(eventsDir.resolve("workflow").resolve(workflowId).resolve(runId).resolve("${event.id}.json"))
            }
        }
        
        // 返回主存储路径
        return paths.first()
    }
}

/**
 * 本地文件数据存储实现。
 */
class LocalFileDataStorage(
    private val id: String,
    name: String,
    private val rootPath: String,
    private val createIfNotExists: Boolean = true
) : DataStorage, KastraXBase(component = "STORAGE", name = "LocalFile-$name") {
    
    private val dataDir: Path = Paths.get(rootPath, "data")
    
    init {
        if (createIfNotExists) {
            Files.createDirectories(dataDir)
        } else if (!Files.exists(dataDir)) {
            throw IllegalArgumentException("数据目录不存在: $dataDir")
        }
    }
    
    override suspend fun storeData(collection: String, id: String, data: Map<String, Any?>): Boolean {
        logger.debug { "存储数据: 集合: $collection, ID: $id" }
        
        return try {
            val dataJson = json.encodeToString(data)
            val filePath = getDataFilePath(collection, id)
            
            withContext(Dispatchers.IO) {
                Files.createDirectories(filePath.parent)
                Files.writeString(filePath, dataJson)
            }
            
            true
        } catch (e: Exception) {
            logger.error(e) { "存储数据失败: 集合: $collection, ID: $id" }
            false
        }
    }
    
    override suspend fun storeDataBatch(collection: String, dataList: List<Pair<String, Map<String, Any?>>>): Boolean {
        logger.debug { "批量存储数据: 集合: $collection, 数据数量: ${dataList.size}" }
        
        var success = true
        
        for ((id, data) in dataList) {
            val result = storeData(collection, id, data)
            if (!result) {
                success = false
            }
        }
        
        return success
    }
    
    override suspend fun getData(collection: String, id: String): Map<String, Any?>? {
        logger.debug { "获取数据: 集合: $collection, ID: $id" }
        
        return try {
            val filePath = getDataFilePath(collection, id)
            
            if (!Files.exists(filePath)) {
                logger.debug { "数据文件不存在: 集合: $collection, ID: $id" }
                return null
            }
            
            val dataJson = withContext(Dispatchers.IO) {
                Files.readString(filePath)
            }
            
            @Suppress("UNCHECKED_CAST")
            json.decodeFromString<Map<String, Any?>>(dataJson)
        } catch (e: Exception) {
            logger.error(e) { "获取数据失败: 集合: $collection, ID: $id" }
            null
        }
    }
    
    override suspend fun getAllData(collection: String, limit: Int, offset: Int): List<Pair<String, Map<String, Any?>>> {
        logger.debug { "获取集合中的所有数据: 集合: $collection, 限制: $limit, 偏移: $offset" }
        
        return try {
            val collectionDir = dataDir.resolve(collection)
            
            if (!Files.exists(collectionDir)) {
                return emptyList()
            }
            
            withContext(Dispatchers.IO) {
                Files.list(collectionDir)
                    .filter { Files.isRegularFile(it) }
                    .skip(offset.toLong())
                    .limit(limit.toLong())
                    .map { 
                        try {
                            val id = it.fileName.toString().removeSuffix(".json")
                            val dataJson = Files.readString(it)
                            @Suppress("UNCHECKED_CAST")
                            id to json.decodeFromString<Map<String, Any?>>(dataJson)
                        } catch (e: Exception) {
                            logger.error(e) { "解析数据文件失败: $it" }
                            null
                        }
                    }
                    .filter { it != null }
                    .map { it!! }
                    .toList()
            }
        } catch (e: Exception) {
            logger.error(e) { "获取集合中的所有数据失败: 集合: $collection" }
            emptyList()
        }
    }
    
    override suspend fun queryData(collection: String, query: Map<String, Any?>, limit: Int, offset: Int): List<Pair<String, Map<String, Any?>>> {
        logger.debug { "查询数据: 集合: $collection, 查询: $query, 限制: $limit, 偏移: $offset" }
        
        // 简单实现，加载所有数据并在内存中过滤
        // 实际应用中应该使用更高效的索引和查询机制
        return try {
            val allData = getAllData(collection, Int.MAX_VALUE, 0)
            
            allData.filter { (_, data) ->
                matchesQuery(data, query)
            }.drop(offset).take(limit)
        } catch (e: Exception) {
            logger.error(e) { "查询数据失败: 集合: $collection, 查询: $query" }
            emptyList()
        }
    }
    
    override suspend fun updateData(collection: String, id: String, data: Map<String, Any?>): Boolean {
        // 简单实现，直接覆盖现有数据
        return storeData(collection, id, data)
    }
    
    override suspend fun deleteData(collection: String, id: String): Boolean {
        logger.debug { "删除数据: 集合: $collection, ID: $id" }
        
        return try {
            val filePath = getDataFilePath(collection, id)
            
            withContext(Dispatchers.IO) {
                Files.deleteIfExists(filePath)
            }
        } catch (e: Exception) {
            logger.error(e) { "删除数据失败: 集合: $collection, ID: $id" }
            false
        }
    }
    
    override suspend fun deleteAllData(collection: String): Boolean {
        logger.debug { "删除集合中的所有数据: 集合: $collection" }
        
        return try {
            val collectionDir = dataDir.resolve(collection)
            
            if (!Files.exists(collectionDir)) {
                return true
            }
            
            withContext(Dispatchers.IO) {
                Files.walk(collectionDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach { Files.delete(it) }
            }
            
            true
        } catch (e: Exception) {
            logger.error(e) { "删除集合中的所有数据失败: 集合: $collection" }
            false
        }
    }
    
    private fun getDataFilePath(collection: String, id: String): Path {
        return dataDir.resolve(collection).resolve("$id.json")
    }
    
    private fun matchesQuery(data: Map<String, Any?>, query: Map<String, Any?>): Boolean {
        for ((key, value) in query) {
            val dataValue = data[key]
            
            if (dataValue != value) {
                return false
            }
        }
        
        return true
    }
}

/**
 * 事件包装器，用于序列化和反序列化事件。
 */
@kotlinx.serialization.Serializable
data class EventWrapper(
    val event: Event
)

/**
 * 简单事件实现。
 */
@kotlinx.serialization.Serializable
data class SimpleEvent(
    override val id: String = UUID.randomUUID().toString(),
    override val type: String,
    override val timestamp: Long = Instant.now().toEpochMilli(),
    override val source: String,
    override val data: Map<String, Any?>,
    override val metadata: Map<String, Any?>
) : Event
