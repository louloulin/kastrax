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
    val json = Json {
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

    private fun getEventFilePath(eventId: String, eventType: String): Path {
        return eventsDir.resolve(eventType).resolve("$eventId.json")
    }

    override suspend fun storeEvent(event: Event): Boolean {
        logger.debug { "存储事件: ${event.id}, 类型: ${event.type}" }

        return try {
            val eventJson = LocalFileStorage.json.encodeToString(event)
            val filePath = getEventFilePath(event.id, event.type)

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

        // 由于我们需要知道事件类型才能找到文件，所以需要遍历所有类型目录
        return try {
            if (!Files.exists(eventsDir)) {
                return null
            }

            var foundEvent: Event? = null

            withContext(Dispatchers.IO) {
                Files.list(eventsDir)
                    .filter { Files.isDirectory(it) }
                    .forEach { typeDir ->
                        val eventFile = typeDir.resolve("$eventId.json")
                        if (Files.exists(eventFile) && foundEvent == null) {
                            val eventJson = Files.readString(eventFile)
                            foundEvent = LocalFileStorage.json.decodeFromString<Event>(eventJson)
                        }
                    }
            }

            foundEvent
        } catch (e: Exception) {
            logger.error(e) { "获取事件失败: $eventId" }
            null
        }
    }

    override suspend fun deleteEvent(eventId: String): Boolean {
        logger.debug { "删除事件: $eventId" }

        // 由于我们需要知道事件类型才能找到文件，所以需要遍历所有类型目录
        return try {
            if (!Files.exists(eventsDir)) {
                return false
            }

            var deleted = false

            withContext(Dispatchers.IO) {
                Files.list(eventsDir)
                    .filter { Files.isDirectory(it) }
                    .forEach { typeDir ->
                        val eventFile = typeDir.resolve("$eventId.json")
                        if (Files.exists(eventFile)) {
                            Files.delete(eventFile)
                            deleted = true
                        }
                    }
            }

            deleted
        } catch (e: Exception) {
            logger.error(e) { "删除事件失败: $eventId" }
            false
        }
    }

    override suspend fun getEventsByType(eventType: String, limit: Int, offset: Int): List<Event> {
        logger.debug { "获取指定类型的事件: 类型: $eventType, 限制: $limit, 偏移: $offset" }

        return try {
            val typeDir = eventsDir.resolve(eventType)

            if (!Files.exists(typeDir)) {
                return emptyList()
            }

            val events = withContext(Dispatchers.IO) {
                Files.list(typeDir)
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
                    .map {
                        try {
                            val eventJson = Files.readString(it)
                            LocalFileStorage.json.decodeFromString<Event>(eventJson)
                        } catch (e: Exception) {
                            logger.error(e) { "解析事件文件失败: $it" }
                            null
                        }
                    }
                    .filter { it != null }
                    .map { it!! }
                    .toList()
            }

            // 按时间戳排序
            val sortedEvents = events.sortedByDescending { it.timestamp }

            // 应用分页
            val startIndex = offset.coerceAtMost(sortedEvents.size)
            val endIndex = (offset + limit).coerceAtMost(sortedEvents.size)

            sortedEvents.subList(startIndex, endIndex)
        } catch (e: Exception) {
            logger.error(e) { "获取指定类型的事件失败: 类型: $eventType" }
            emptyList()
        }
    }

    override suspend fun getEventsByWorkflow(workflowId: String, limit: Int, offset: Int): List<Event> {
        logger.debug { "获取指定工作流的事件: 工作流: $workflowId, 限制: $limit, 偏移: $offset" }

        return try {
            if (!Files.exists(eventsDir)) {
                return emptyList()
            }

            val allEvents = mutableListOf<Event>()

            withContext(Dispatchers.IO) {
                Files.walk(eventsDir)
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
                    .forEach {
                        try {
                            val eventJson = Files.readString(it)
                            val event = LocalFileStorage.json.decodeFromString<Event>(eventJson)

                            // 检查事件是否属于指定工作流
                            if (event.metadata["workflowId"] == workflowId) {
                                allEvents.add(event)
                            }
                        } catch (e: Exception) {
                            logger.error(e) { "解析事件文件失败: $it" }
                        }
                    }
            }

            // 按时间戳排序
            val sortedEvents = allEvents.sortedByDescending { it.timestamp }

            // 应用分页
            val startIndex = offset.coerceAtMost(sortedEvents.size)
            val endIndex = (offset + limit).coerceAtMost(sortedEvents.size)

            sortedEvents.subList(startIndex, endIndex)
        } catch (e: Exception) {
            logger.error(e) { "获取指定工作流的事件失败: 工作流: $workflowId" }
            emptyList()
        }
    }

    override suspend fun getEventsByWorkflowRun(workflowId: String, runId: String, limit: Int, offset: Int): List<Event> {
        logger.debug { "获取指定工作流运行的事件: 工作流: $workflowId, 运行ID: $runId, 限制: $limit, 偏移: $offset" }

        return try {
            if (!Files.exists(eventsDir)) {
                return emptyList()
            }

            val allEvents = mutableListOf<Event>()

            withContext(Dispatchers.IO) {
                Files.walk(eventsDir)
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
                    .forEach {
                        try {
                            val eventJson = Files.readString(it)
                            val event = LocalFileStorage.json.decodeFromString<Event>(eventJson)

                            // 检查事件是否属于指定工作流运行
                            if (event.metadata["workflowId"] == workflowId && event.metadata["runId"] == runId) {
                                allEvents.add(event)
                            }
                        } catch (e: Exception) {
                            logger.error(e) { "解析事件文件失败: $it" }
                        }
                    }
            }

            // 按时间戳排序
            val sortedEvents = allEvents.sortedByDescending { it.timestamp }

            // 应用分页
            val startIndex = offset.coerceAtMost(sortedEvents.size)
            val endIndex = (offset + limit).coerceAtMost(sortedEvents.size)

            sortedEvents.subList(startIndex, endIndex)
        } catch (e: Exception) {
            logger.error(e) { "获取指定工作流运行的事件失败: 工作流: $workflowId, 运行ID: $runId" }
            emptyList()
        }
    }

    override suspend fun deleteEventsByWorkflow(workflowId: String): Boolean {
        logger.debug { "删除指定工作流的所有事件: 工作流: $workflowId" }

        return try {
            if (!Files.exists(eventsDir)) {
                return false
            }

            val eventsToDelete = mutableListOf<Path>()

            withContext(Dispatchers.IO) {
                Files.walk(eventsDir)
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
                    .forEach {
                        try {
                            val eventJson = Files.readString(it)
                            val event = LocalFileStorage.json.decodeFromString<Event>(eventJson)

                            // 检查事件是否属于指定工作流
                            if (event.metadata["workflowId"] == workflowId) {
                                eventsToDelete.add(it)
                            }
                        } catch (e: Exception) {
                            logger.error(e) { "解析事件文件失败: $it" }
                        }
                    }

                // 删除所有匹配的事件
                for (eventFile in eventsToDelete) {
                    Files.delete(eventFile)
                }
            }

            true
        } catch (e: Exception) {
            logger.error(e) { "删除指定工作流的所有事件失败: 工作流: $workflowId" }
            false
        }
    }

    override suspend fun deleteEventsByWorkflowRun(workflowId: String, runId: String): Boolean {
        logger.debug { "删除指定工作流运行的所有事件: 工作流: $workflowId, 运行ID: $runId" }

        return try {
            if (!Files.exists(eventsDir)) {
                return false
            }

            val eventsToDelete = mutableListOf<Path>()

            withContext(Dispatchers.IO) {
                Files.walk(eventsDir)
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
                    .forEach {
                        try {
                            val eventJson = Files.readString(it)
                            val event = LocalFileStorage.json.decodeFromString<Event>(eventJson)

                            // 检查事件是否属于指定工作流运行
                            if (event.metadata["workflowId"] == workflowId && event.metadata["runId"] == runId) {
                                eventsToDelete.add(it)
                            }
                        } catch (e: Exception) {
                            logger.error(e) { "解析事件文件失败: $it" }
                        }
                    }

                // 删除所有匹配的事件
                for (eventFile in eventsToDelete) {
                    Files.delete(eventFile)
                }
            }

            true
        } catch (e: Exception) {
            logger.error(e) { "删除指定工作流运行的所有事件失败: 工作流: $workflowId, 运行ID: $runId" }
            false
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

    override suspend fun updateData(collection: String, id: String, data: Map<String, Any?>): Boolean {
        logger.debug { "更新数据: 集合: $collection, ID: $id" }

        return try {
            val filePath = getDataFilePath(collection, id)

            if (!Files.exists(filePath)) {
                logger.debug { "数据不存在，无法更新: 集合: $collection, ID: $id" }
                return false
            }

            val dataJson = LocalFileStorage.json.encodeToString(data)

            withContext(Dispatchers.IO) {
                Files.writeString(filePath, dataJson)
            }

            true
        } catch (e: Exception) {
            logger.error(e) { "更新数据失败: 集合: $collection, ID: $id" }
            false
        }
    }

    override suspend fun storeDataBatch(collection: String, dataList: List<Pair<String, Map<String, Any?>>>): Boolean {
        logger.debug { "批量存储数据: 集合: $collection, 数量: ${dataList.size}" }

        return try {
            val collectionDir = dataDir.resolve(collection)

            withContext(Dispatchers.IO) {
                Files.createDirectories(collectionDir)
            }

            var success = true

            for ((id, data) in dataList) {
                val filePath = getDataFilePath(collection, id)
                val dataJson = LocalFileStorage.json.encodeToString(data)

                try {
                    withContext(Dispatchers.IO) {
                        Files.writeString(filePath, dataJson)
                    }
                } catch (e: Exception) {
                    logger.error(e) { "存储数据失败: 集合: $collection, ID: $id" }
                    success = false
                }
            }

            success
        } catch (e: Exception) {
            logger.error(e) { "批量存储数据失败: 集合: $collection" }
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

    override suspend fun getAllData(collection: String, limit: Int, offset: Int): List<Pair<String, Map<String, Any?>>> {
        logger.debug { "获取集合中的所有数据: 集合: $collection, 限制: $limit, 偏移: $offset" }

        return try {
            val collectionDir = dataDir.resolve(collection)

            if (!Files.exists(collectionDir)) {
                return emptyList()
            }

            val results = mutableListOf<Pair<String, Map<String, Any?>>>()

            withContext(Dispatchers.IO) {
                Files.list(collectionDir)
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
                    .forEach {
                        try {
                            val id = it.fileName.toString().removeSuffix(".json")
                            val dataJson = Files.readString(it)
                            @Suppress("UNCHECKED_CAST")
                            val data = LocalFileStorage.json.decodeFromString<Map<String, Any?>>(dataJson)
                            results.add(Pair(id, data))
                        } catch (e: Exception) {
                            logger.error(e) { "解析数据文件失败: $it" }
                        }
                    }
            }

            // 应用分页
            val startIndex = offset.coerceAtMost(results.size)
            val endIndex = (offset + limit).coerceAtMost(results.size)

            results.subList(startIndex, endIndex)
        } catch (e: Exception) {
            logger.error(e) { "获取集合中的所有数据失败: 集合: $collection" }
            emptyList()
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

    override suspend fun deleteAllData(collection: String): Boolean {
        logger.debug { "删除集合中的所有数据: 集合: $collection" }

        return try {
            val collectionDir = dataDir.resolve(collection)

            if (!Files.exists(collectionDir)) {
                return true // 集合不存在，视为删除成功
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

    override suspend fun queryData(collection: String, query: Map<String, Any?>, limit: Int, offset: Int): List<Pair<String, Map<String, Any?>>> {
        logger.debug { "查询数据: 集合: $collection, 查询: $query, 限制: $limit, 偏移: $offset" }

        try {
            val collectionDir = dataDir.resolve(collection)

            if (!Files.exists(collectionDir)) {
                return emptyList()
            }

            val results = mutableListOf<Pair<String, Map<String, Any?>>>()

            withContext(Dispatchers.IO) {
                Files.list(collectionDir)
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
                    .forEach {
                        try {
                            val id = it.fileName.toString().removeSuffix(".json")
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
                                results.add(Pair(id, data))
                            }
                        } catch (e: Exception) {
                            logger.error(e) { "解析数据文件失败: $it" }
                        }
                    }
            }

            // 应用分页
            val startIndex = offset.coerceAtMost(results.size)
            val endIndex = (offset + limit).coerceAtMost(results.size)

            return results.subList(startIndex, endIndex)
        } catch (e: Exception) {
            logger.error(e) { "查询数据失败: 集合: $collection" }
            return emptyList()
        }
    }
}

// 事件包装器已移除，直接使用Event类
