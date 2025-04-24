package ai.kastrax.datasource.filesystem.plugin.storage

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.plugin.DataStorage
import ai.kastrax.core.plugin.Event
import ai.kastrax.core.plugin.EventStorage
import ai.kastrax.core.workflow.state.WorkflowState
import ai.kastrax.core.workflow.state.WorkflowStateStorage
import ai.kastrax.core.workflow.state.WorkflowRunInfo
import ai.kastrax.core.workflow.state.WorkflowStateStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
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
    
    private fun getStateKey(workflowId: String, runId: String): String {
        return "$workflowId:$runId"
    }
    
    private fun getStateFilePath(workflowId: String, runId: String): Path {
        return statesDir.resolve(workflowId).resolve("$runId.json")
    }
    
    override suspend fun saveWorkflowState(workflowId: String, runId: String, state: WorkflowState): Boolean {
        logger.debug { "保存工作流状态: $workflowId, 运行ID: $runId" }
        
        return try {
            val stateJson = LocalFileStorage.json.encodeToString(state)
            val filePath = getStateFilePath(workflowId, runId)
            
            withContext(Dispatchers.IO) {
                Files.createDirectories(filePath.parent)
                Files.writeString(filePath, stateJson)
            }
            
            cache[getStateKey(workflowId, runId)] = state
            true
        } catch (e: Exception) {
            logger.error(e) { "保存工作流状态失败: $workflowId, 运行ID: $runId" }
            false
        }
    }
    
    override suspend fun getWorkflowState(workflowId: String, runId: String): WorkflowState? {
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
            
            val state = LocalFileStorage.json.decodeFromString<WorkflowState>(stateJson)
            cache[key] = state
            state
        } catch (e: Exception) {
            logger.error(e) { "获取工作流状态失败: $workflowId, 运行ID: $runId" }
            null
        }
    }
    
    override suspend fun deleteWorkflowState(workflowId: String, runId: String): Boolean {
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
    
    override suspend fun getWorkflowRuns(workflowId: String, limit: Int, offset: Int): List<WorkflowRunInfo> {
        logger.debug { "获取工作流运行信息: $workflowId, 限制: $limit, 偏移: $offset" }
        
        return try {
            val workflowDir = statesDir.resolve(workflowId)
            
            if (!Files.exists(workflowDir)) {
                return emptyList()
            }
            
            withContext(Dispatchers.IO) {
                Files.list(workflowDir)
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
                    .map { path ->
                        try {
                            val runId = path.fileName.toString().removeSuffix(".json")
                            val stateJson = Files.readString(path)
                            val state = LocalFileStorage.json.decodeFromString<WorkflowState>(stateJson)
                            
                            WorkflowRunInfo(
                                runId = state.runId,
                                workflowId = state.workflowId,
                                status = state.status,
                                createdAt = state.createdAt,
                                updatedAt = state.updatedAt
                            )
                        } catch (e: Exception) {
                            logger.error(e) { "解析工作流状态文件失败: $path" }
                            null
                        }
                    }
                    .filter { it != null }
                    .map { it!! }
                    .sorted(compareByDescending { it.updatedAt })
                    .skip(offset.toLong())
                    .limit(limit.toLong())
                    .toList()
            }
        } catch (e: Exception) {
            logger.error(e) { "获取工作流运行信息失败: $workflowId" }
            emptyList()
        }
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
    
    private fun getEventFilePath(event: Event): Path {
        return eventsDir.resolve(event.type).resolve("${event.id}.json")
    }
    
    override suspend fun storeEvent(event: Event): Boolean {
        logger.debug { "存储事件: ${event.id}, 类型: ${event.type}" }
        
        return try {
            val eventJson = LocalFileStorage.json.encodeToString(EventWrapper(event))
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
    
    override suspend fun getEvents(type: String?, from: Instant?, to: Instant?, limit: Int): Flow<Event> = flow {
        logger.debug { "获取事件: 类型: $type, 从: $from, 到: $to, 限制: $limit" }
        
        try {
            val typeDir = if (type != null) eventsDir.resolve(type) else eventsDir
            
            if (!Files.exists(typeDir)) {
                return@flow
            }
            
            val events = withContext(Dispatchers.IO) {
                if (type != null) {
                    Files.list(typeDir)
                        .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
                        .map {
                            try {
                                val eventJson = Files.readString(it)
                                LocalFileStorage.json.decodeFromString<EventWrapper>(eventJson).event
                            } catch (e: Exception) {
                                logger.error(e) { "解析事件文件失败: $it" }
                                null
                            }
                        }
                        .filter { it != null }
                        .map { it!! }
                        .toList()
                } else {
                    Files.walk(eventsDir)
                        .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
                        .map {
                            try {
                                val eventJson = Files.readString(it)
                                LocalFileStorage.json.decodeFromString<EventWrapper>(eventJson).event
                            } catch (e: Exception) {
                                logger.error(e) { "解析事件文件失败: $it" }
                                null
                            }
                        }
                        .filter { it != null }
                        .map { it!! }
                        .toList()
                }
            }
            
            // 过滤和排序事件
            val filteredEvents = events
                .filter { event ->
                    (from == null || event.timestamp.isAfter(from)) &&
                    (to == null || event.timestamp.isBefore(to))
                }
                .sortedByDescending { it.timestamp }
                .take(limit)
            
            for (event in filteredEvents) {
                emit(event)
            }
        } catch (e: Exception) {
            logger.error(e) { "获取事件失败: 类型: $type" }
        }
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
    
    private fun getDataFilePath(collection: String, id: String): Path {
        return dataDir.resolve(collection).resolve("$id.json")
    }
    
    override suspend fun storeData(collection: String, id: String, data: Map<String, Any?>): Boolean {
        logger.debug { "存储数据: 集合: $collection, ID: $id" }
        
        return try {
            val dataJson = LocalFileStorage.json.encodeToString(data)
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
    
    override suspend fun getData(collection: String, id: String): Map<String, Any?>? {
        logger.debug { "获取数据: 集合: $collection, ID: $id" }
        
        return try {
            val filePath = getDataFilePath(collection, id)
            
            if (!Files.exists(filePath)) {
                logger.debug { "数据文件不存在: $filePath" }
                return null
            }
            
            val dataJson = withContext(Dispatchers.IO) {
                Files.readString(filePath)
            }
            
            @Suppress("UNCHECKED_CAST")
            LocalFileStorage.json.decodeFromString<Map<String, Any?>>(dataJson)
        } catch (e: Exception) {
            logger.error(e) { "获取数据失败: 集合: $collection, ID: $id" }
            null
        }
    }
    
    override suspend fun deleteData(collection: String, id: String): Boolean {
        logger.debug { "删除数据: 集合: $collection, ID: $id" }
        
        return try {
            val filePath = getDataFilePath(collection, id)
            
            val deleted = withContext(Dispatchers.IO) {
                Files.deleteIfExists(filePath)
            }
            
            deleted
        } catch (e: Exception) {
            logger.error(e) { "删除数据失败: 集合: $collection, ID: $id" }
            false
        }
    }
    
    override suspend fun queryData(collection: String, query: Map<String, Any?>, limit: Int): Flow<Map<String, Any?>> = flow {
        logger.debug { "查询数据: 集合: $collection, 查询: $query, 限制: $limit" }
        
        try {
            val collectionDir = dataDir.resolve(collection)
            
            if (!Files.exists(collectionDir)) {
                return@flow
            }
            
            var count = 0
            
            withContext(Dispatchers.IO) {
                Files.list(collectionDir)
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
                    .forEach {
                        if (count >= limit) return@forEach
                        
                        try {
                            val dataJson = Files.readString(it)
                            @Suppress("UNCHECKED_CAST")
                            val data = LocalFileStorage.json.decodeFromString<Map<String, Any?>>(dataJson)
                            
                            // 简单的查询匹配
                            var matches = true
                            for ((key, value) in query) {
                                if (data[key] != value) {
                                    matches = false
                                    break
                                }
                            }
                            
                            if (matches) {
                                count++
                                emit(data)
                            }
                        } catch (e: Exception) {
                            logger.error(e) { "解析数据文件失败: $it" }
                        }
                    }
            }
        } catch (e: Exception) {
            logger.error(e) { "查询数据失败: 集合: $collection" }
        }
    }
}

/**
 * 事件包装器，用于序列化和反序列化事件。
 */
@kotlinx.serialization.Serializable
data class EventWrapper(
    val event: Event
)
