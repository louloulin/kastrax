package ai.kastrax.datasource.filesystem.plugin.storage

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.plugin.DataStorage
import ai.kastrax.core.plugin.Event
import ai.kastrax.core.plugin.EventStorage
import ai.kastrax.core.workflow.state.WorkflowRunInfo
import ai.kastrax.core.workflow.state.WorkflowState
import ai.kastrax.core.workflow.state.WorkflowStateStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.io.ByteArrayOutputStream
import java.net.URI
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * S3文件存储工厂，用于创建基于S3的存储实现。
 */
object S3FileStorage {
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
        endpoint: String,
        region: String,
        bucket: String,
        accessKey: String,
        secretKey: String,
        prefix: String = ""
    ): WorkflowStateStorage {
        return S3WorkflowStateStorage(
            id = id,
            name = name,
            endpoint = endpoint,
            region = region,
            bucket = bucket,
            accessKey = accessKey,
            secretKey = secretKey,
            prefix = prefix
        )
    }

    /**
     * 创建事件存储。
     */
    fun createEventStorage(
        id: String,
        name: String,
        endpoint: String,
        region: String,
        bucket: String,
        accessKey: String,
        secretKey: String,
        prefix: String = ""
    ): EventStorage {
        return S3EventStorage(
            id = id,
            name = name,
            endpoint = endpoint,
            region = region,
            bucket = bucket,
            accessKey = accessKey,
            secretKey = secretKey,
            prefix = prefix
        )
    }

    /**
     * 创建数据存储。
     */
    fun createDataStorage(
        id: String,
        name: String,
        endpoint: String,
        region: String,
        bucket: String,
        accessKey: String,
        secretKey: String,
        prefix: String = ""
    ): DataStorage {
        return S3DataStorage(
            id = id,
            name = name,
            endpoint = endpoint,
            region = region,
            bucket = bucket,
            accessKey = accessKey,
            secretKey = secretKey,
            prefix = prefix
        )
    }
}

/**
 * S3工作流状态存储实现。
 */
class S3WorkflowStateStorage(
    private val id: String,
    name: String,
    private val endpoint: String,
    private val region: String,
    private val bucket: String,
    private val accessKey: String,
    private val secretKey: String,
    private val prefix: String = ""
) : WorkflowStateStorage, KastraXBase(component = "STORAGE", name = "S3-$name") {

    private val s3Client: S3Client by lazy { createS3Client() }
    private val cache = ConcurrentHashMap<String, WorkflowState>()

    private fun createS3Client(): S3Client {
        val credentials = AwsBasicCredentials.create(accessKey, secretKey)

        return S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build()
    }

    private fun getStateKey(workflowId: String, runId: String): String {
        return "$workflowId:$runId"
    }

    private fun getStateObjectKey(workflowId: String, runId: String): String {
        val basePath = if (prefix.isNotEmpty()) "$prefix/" else ""
        return "${basePath}states/$workflowId/$runId.json"
    }

    override suspend fun saveWorkflowState(workflowId: String, runId: String, state: WorkflowState): Boolean {
        logger.debug { "保存工作流状态: $workflowId, 运行ID: $runId" }

        return try {
            val stateJson = S3FileStorage.json.encodeToString(state)
            val objectKey = getStateObjectKey(workflowId, runId)

            withContext(Dispatchers.IO) {
                s3Client.putObject(
                    PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType("application/json")
                        .build(),
                    RequestBody.fromString(stateJson)
                )
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

        // 首先检查缓存
        val cachedState = cache[getStateKey(workflowId, runId)]
        if (cachedState != null) {
            return cachedState
        }

        return try {
            val objectKey = getStateObjectKey(workflowId, runId)

            val stateJson = withContext(Dispatchers.IO) {
                val response = s3Client.getObject { builder ->
                    builder.bucket(bucket).key(objectKey)
                }

                response.use { s3Object ->
                    s3Object.readAllBytes().toString(Charsets.UTF_8)
                }
            }

            val state = S3FileStorage.json.decodeFromString<WorkflowState>(stateJson)
            cache[getStateKey(workflowId, runId)] = state
            state
        } catch (e: NoSuchKeyException) {
            logger.debug { "工作流状态不存在: $workflowId, 运行ID: $runId" }
            null
        } catch (e: Exception) {
            logger.error(e) { "获取工作流状态失败: $workflowId, 运行ID: $runId" }
            null
        }
    }

    override suspend fun deleteWorkflowState(workflowId: String, runId: String): Boolean {
        logger.debug { "删除工作流状态: $workflowId, 运行ID: $runId" }

        return try {
            val objectKey = getStateObjectKey(workflowId, runId)

            withContext(Dispatchers.IO) {
                s3Client.deleteObject { builder ->
                    builder.bucket(bucket).key(objectKey)
                }
            }

            cache.remove(getStateKey(workflowId, runId))
            true
        } catch (e: Exception) {
            logger.error(e) { "删除工作流状态失败: $workflowId, 运行ID: $runId" }
            false
        }
    }



    override suspend fun getWorkflowRuns(workflowId: String, limit: Int, offset: Int): List<WorkflowRunInfo> {
        logger.debug { "获取工作流运行信息: $workflowId, 限制: $limit, 偏移: $offset" }

        return try {
            val basePath = if (prefix.isNotEmpty()) "$prefix/" else ""
            val prefix = "${basePath}states/$workflowId/"

            val listObjectsRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build()

            val response = withContext(Dispatchers.IO) {
                s3Client.listObjectsV2(listObjectsRequest)
            }

            val runInfos = response.contents().map { s3Object ->
                val objectKey = s3Object.key()
                val runId = objectKey.substringAfterLast('/').removeSuffix(".json")

                try {
                    val stateJson = withContext(Dispatchers.IO) {
                        val getResponse = s3Client.getObject { builder ->
                            builder.bucket(bucket).key(objectKey)
                        }

                        getResponse.use { s3ObjectContent ->
                            s3ObjectContent.readAllBytes().toString(Charsets.UTF_8)
                        }
                    }

                    val state = S3FileStorage.json.decodeFromString<WorkflowState>(stateJson)

                    WorkflowRunInfo(
                        runId = state.runId,
                        workflowId = state.workflowId,
                        status = state.status,
                        createdAt = state.createdAt,
                        updatedAt = state.updatedAt
                    )
                } catch (e: Exception) {
                    logger.error(e) { "解析工作流运行信息失败: $workflowId, 运行ID: $runId" }
                    null
                }
            }.filterNotNull()

            // 按更新时间排序
            val sortedRunInfos = runInfos.sortedByDescending { it.updatedAt }

            // 应用分页
            val startIndex = offset.coerceAtMost(sortedRunInfos.size)
            val endIndex = (offset + limit).coerceAtMost(sortedRunInfos.size)

            sortedRunInfos.subList(startIndex, endIndex)
        } catch (e: Exception) {
            logger.error(e) { "获取工作流运行信息失败: $workflowId" }
            emptyList()
        }
    }


}

/**
 * S3事件存储实现。
 */
class S3EventStorage(
    private val id: String,
    name: String,
    private val endpoint: String,
    private val region: String,
    private val bucket: String,
    private val accessKey: String,
    private val secretKey: String,
    private val prefix: String = ""
) : EventStorage, KastraXBase(component = "STORAGE", name = "S3-$name") {

    private val s3Client: S3Client by lazy { createS3Client() }

    private fun createS3Client(): S3Client {
        val credentials = AwsBasicCredentials.create(accessKey, secretKey)

        return S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build()
    }

    private fun getEventObjectKey(eventId: String, eventType: String): String {
        val basePath = if (prefix.isNotEmpty()) "$prefix/" else ""
        return "${basePath}events/$eventType/$eventId.json"
    }

    override suspend fun storeEvent(event: Event): Boolean {
        logger.debug { "存储事件: ${event.id}, 类型: ${event.type}" }

        return try {
            val eventJson = S3FileStorage.json.encodeToString(event)
            val objectKey = getEventObjectKey(event.id, event.type)

            withContext(Dispatchers.IO) {
                s3Client.putObject(
                    PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType("application/json")
                        .build(),
                    RequestBody.fromString(eventJson)
                )
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
            val basePath = if (prefix.isNotEmpty()) "$prefix/" else ""
            val eventsPrefix = "${basePath}events/"

            val listObjectsRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(eventsPrefix)
                .build()

            val response = withContext(Dispatchers.IO) {
                s3Client.listObjectsV2(listObjectsRequest)
            }

            for (s3Object in response.contents()) {
                val objectKey = s3Object.key()
                if (objectKey.endsWith("$eventId.json")) {
                    val eventJson = withContext(Dispatchers.IO) {
                        val getResponse = s3Client.getObject { builder ->
                            builder.bucket(bucket).key(objectKey)
                        }

                        getResponse.use { s3ObjectContent ->
                            s3ObjectContent.readAllBytes().toString(Charsets.UTF_8)
                        }
                    }

                    return S3FileStorage.json.decodeFromString<Event>(eventJson)
                }
            }

            null
        } catch (e: Exception) {
            logger.error(e) { "获取事件失败: $eventId" }
            null
        }
    }

    override suspend fun deleteEvent(eventId: String): Boolean {
        logger.debug { "删除事件: $eventId" }

        // 由于我们需要知道事件类型才能找到文件，所以需要遍历所有类型目录
        return try {
            val basePath = if (prefix.isNotEmpty()) "$prefix/" else ""
            val eventsPrefix = "${basePath}events/"

            val listObjectsRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(eventsPrefix)
                .build()

            val response = withContext(Dispatchers.IO) {
                s3Client.listObjectsV2(listObjectsRequest)
            }

            var deleted = false

            for (s3Object in response.contents()) {
                val objectKey = s3Object.key()
                if (objectKey.endsWith("$eventId.json")) {
                    withContext(Dispatchers.IO) {
                        s3Client.deleteObject { builder ->
                            builder.bucket(bucket).key(objectKey)
                        }
                    }
                    deleted = true
                    break
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
            val basePath = if (prefix.isNotEmpty()) "$prefix/" else ""
            val typePrefix = "${basePath}events/$eventType/"

            val listObjectsRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(typePrefix)
                .build()

            val response = withContext(Dispatchers.IO) {
                s3Client.listObjectsV2(listObjectsRequest)
            }

            val events = response.contents().map { s3Object ->
                val objectKey = s3Object.key()

                try {
                    val eventJson = withContext(Dispatchers.IO) {
                        val getResponse = s3Client.getObject { builder ->
                            builder.bucket(bucket).key(objectKey)
                        }

                        getResponse.use { s3ObjectContent ->
                            s3ObjectContent.readAllBytes().toString(Charsets.UTF_8)
                        }
                    }

                    S3FileStorage.json.decodeFromString<Event>(eventJson)
                } catch (e: Exception) {
                    logger.error(e) { "解析事件文件失败: $objectKey" }
                    null
                }
            }.filterNotNull()

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
            val basePath = if (prefix.isNotEmpty()) "$prefix/" else ""
            val eventsPrefix = "${basePath}events/"

            val listObjectsRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(eventsPrefix)
                .build()

            val response = withContext(Dispatchers.IO) {
                s3Client.listObjectsV2(listObjectsRequest)
            }

            val allEvents = mutableListOf<Event>()

            for (s3Object in response.contents()) {
                val objectKey = s3Object.key()

                try {
                    val eventJson = withContext(Dispatchers.IO) {
                        val getResponse = s3Client.getObject { builder ->
                            builder.bucket(bucket).key(objectKey)
                        }

                        getResponse.use { s3ObjectContent ->
                            s3ObjectContent.readAllBytes().toString(Charsets.UTF_8)
                        }
                    }

                    val event = S3FileStorage.json.decodeFromString<Event>(eventJson)

                    // 检查事件是否属于指定工作流
                    if (event.metadata["workflowId"] == workflowId) {
                        allEvents.add(event)
                    }
                } catch (e: Exception) {
                    logger.error(e) { "解析事件文件失败: $objectKey" }
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
            val basePath = if (prefix.isNotEmpty()) "$prefix/" else ""
            val eventsPrefix = "${basePath}events/"

            val listObjectsRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(eventsPrefix)
                .build()

            val response = withContext(Dispatchers.IO) {
                s3Client.listObjectsV2(listObjectsRequest)
            }

            val allEvents = mutableListOf<Event>()

            for (s3Object in response.contents()) {
                val objectKey = s3Object.key()

                try {
                    val eventJson = withContext(Dispatchers.IO) {
                        val getResponse = s3Client.getObject { builder ->
                            builder.bucket(bucket).key(objectKey)
                        }

                        getResponse.use { s3ObjectContent ->
                            s3ObjectContent.readAllBytes().toString(Charsets.UTF_8)
                        }
                    }

                    val event = S3FileStorage.json.decodeFromString<Event>(eventJson)

                    // 检查事件是否属于指定工作流运行
                    if (event.metadata["workflowId"] == workflowId && event.metadata["runId"] == runId) {
                        allEvents.add(event)
                    }
                } catch (e: Exception) {
                    logger.error(e) { "解析事件文件失败: $objectKey" }
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
            val basePath = if (prefix.isNotEmpty()) "$prefix/" else ""
            val eventsPrefix = "${basePath}events/"

            val listObjectsRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(eventsPrefix)
                .build()

            val response = withContext(Dispatchers.IO) {
                s3Client.listObjectsV2(listObjectsRequest)
            }

            val objectsToDelete = mutableListOf<String>()

            for (s3Object in response.contents()) {
                val objectKey = s3Object.key()

                try {
                    val eventJson = withContext(Dispatchers.IO) {
                        val getResponse = s3Client.getObject { builder ->
                            builder.bucket(bucket).key(objectKey)
                        }

                        getResponse.use { s3ObjectContent ->
                            s3ObjectContent.readAllBytes().toString(Charsets.UTF_8)
                        }
                    }

                    val event = S3FileStorage.json.decodeFromString<Event>(eventJson)

                    // 检查事件是否属于指定工作流
                    if (event.metadata["workflowId"] == workflowId) {
                        objectsToDelete.add(objectKey)
                    }
                } catch (e: Exception) {
                    logger.error(e) { "解析事件文件失败: $objectKey" }
                }
            }

            // 删除所有匹配的事件
            for (objectKey in objectsToDelete) {
                withContext(Dispatchers.IO) {
                    s3Client.deleteObject { builder ->
                        builder.bucket(bucket).key(objectKey)
                    }
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
            val basePath = if (prefix.isNotEmpty()) "$prefix/" else ""
            val eventsPrefix = "${basePath}events/"

            val listObjectsRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(eventsPrefix)
                .build()

            val response = withContext(Dispatchers.IO) {
                s3Client.listObjectsV2(listObjectsRequest)
            }

            val objectsToDelete = mutableListOf<String>()

            for (s3Object in response.contents()) {
                val objectKey = s3Object.key()

                try {
                    val eventJson = withContext(Dispatchers.IO) {
                        val getResponse = s3Client.getObject { builder ->
                            builder.bucket(bucket).key(objectKey)
                        }

                        getResponse.use { s3ObjectContent ->
                            s3ObjectContent.readAllBytes().toString(Charsets.UTF_8)
                        }
                    }

                    val event = S3FileStorage.json.decodeFromString<Event>(eventJson)

                    // 检查事件是否属于指定工作流运行
                    if (event.metadata["workflowId"] == workflowId && event.metadata["runId"] == runId) {
                        objectsToDelete.add(objectKey)
                    }
                } catch (e: Exception) {
                    logger.error(e) { "解析事件文件失败: $objectKey" }
                }
            }

            // 删除所有匹配的事件
            for (objectKey in objectsToDelete) {
                withContext(Dispatchers.IO) {
                    s3Client.deleteObject { builder ->
                        builder.bucket(bucket).key(objectKey)
                    }
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
 * S3数据存储实现。
 */
class S3DataStorage(
    private val id: String,
    name: String,
    private val endpoint: String,
    private val region: String,
    private val bucket: String,
    private val accessKey: String,
    private val secretKey: String,
    private val prefix: String = ""
) : DataStorage, KastraXBase(component = "STORAGE", name = "S3-$name") {

    private val s3Client: S3Client by lazy { createS3Client() }

    private fun createS3Client(): S3Client {
        val credentials = AwsBasicCredentials.create(accessKey, secretKey)

        return S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build()
    }

    private fun getDataObjectKey(collection: String, id: String): String {
        val basePath = if (prefix.isNotEmpty()) "$prefix/" else ""
        return "${basePath}data/$collection/$id.json"
    }

    override suspend fun storeData(collection: String, id: String, data: Map<String, Any?>): Boolean {
        logger.debug { "存储数据: 集合: $collection, ID: $id" }

        return try {
            val dataJson = S3FileStorage.json.encodeToString(data)
            val objectKey = getDataObjectKey(collection, id)

            withContext(Dispatchers.IO) {
                s3Client.putObject(
                    PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType("application/json")
                        .build(),
                    RequestBody.fromString(dataJson)
                )
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
            val objectKey = getDataObjectKey(collection, id)

            // 检查对象是否存在
            val exists = withContext(Dispatchers.IO) {
                try {
                    s3Client.headObject { builder ->
                        builder.bucket(bucket).key(objectKey)
                    }
                    true
                } catch (e: NoSuchKeyException) {
                    false
                }
            }

            if (!exists) {
                logger.debug { "数据不存在，无法更新: 集合: $collection, ID: $id" }
                return false
            }

            val dataJson = S3FileStorage.json.encodeToString(data)

            withContext(Dispatchers.IO) {
                s3Client.putObject(
                    PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType("application/json")
                        .build(),
                    RequestBody.fromString(dataJson)
                )
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
            var success = true

            for ((id, data) in dataList) {
                val dataJson = S3FileStorage.json.encodeToString(data)
                val objectKey = getDataObjectKey(collection, id)

                try {
                    withContext(Dispatchers.IO) {
                        s3Client.putObject(
                            PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(objectKey)
                                .contentType("application/json")
                                .build(),
                            RequestBody.fromString(dataJson)
                        )
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
            val objectKey = getDataObjectKey(collection, id)

            val dataJson = withContext(Dispatchers.IO) {
                val response = s3Client.getObject { builder ->
                    builder.bucket(bucket).key(objectKey)
                }

                response.use { s3Object ->
                    s3Object.readAllBytes().toString(Charsets.UTF_8)
                }
            }

            @Suppress("UNCHECKED_CAST")
            S3FileStorage.json.decodeFromString<Map<String, Any?>>(dataJson)
        } catch (e: NoSuchKeyException) {
            logger.debug { "数据不存在: 集合: $collection, ID: $id" }
            null
        } catch (e: Exception) {
            logger.error(e) { "获取数据失败: 集合: $collection, ID: $id" }
            null
        }
    }

    override suspend fun deleteData(collection: String, id: String): Boolean {
        logger.debug { "删除数据: 集合: $collection, ID: $id" }

        return try {
            val objectKey = getDataObjectKey(collection, id)

            withContext(Dispatchers.IO) {
                s3Client.deleteObject { builder ->
                    builder.bucket(bucket).key(objectKey)
                }
            }

            true
        } catch (e: Exception) {
            logger.error(e) { "删除数据失败: 集合: $collection, ID: $id" }
            false
        }
    }

    override suspend fun deleteAllData(collection: String): Boolean {
        logger.debug { "删除集合中的所有数据: 集合: $collection" }

        return try {
            val basePath = if (prefix.isNotEmpty()) "$prefix/" else ""
            val collectionPrefix = "${basePath}data/$collection/"

            val listObjectsRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(collectionPrefix)
                .build()

            val response = withContext(Dispatchers.IO) {
                s3Client.listObjectsV2(listObjectsRequest)
            }

            // 删除所有匹配的对象
            for (s3Object in response.contents()) {
                val objectKey = s3Object.key()

                withContext(Dispatchers.IO) {
                    s3Client.deleteObject { builder ->
                        builder.bucket(bucket).key(objectKey)
                    }
                }
            }

            true
        } catch (e: Exception) {
            logger.error(e) { "删除集合中的所有数据失败: 集合: $collection" }
            false
        }
    }

    override suspend fun getAllData(collection: String, limit: Int, offset: Int): List<Pair<String, Map<String, Any?>>> {
        logger.debug { "获取集合中的所有数据: 集合: $collection, 限制: $limit, 偏移: $offset" }

        return try {
            val basePath = if (prefix.isNotEmpty()) "$prefix/" else ""
            val collectionPrefix = "${basePath}data/$collection/"

            val listObjectsRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(collectionPrefix)
                .build()

            val response = withContext(Dispatchers.IO) {
                s3Client.listObjectsV2(listObjectsRequest)
            }

            val results = mutableListOf<Pair<String, Map<String, Any?>>>()

            for (s3Object in response.contents()) {
                val objectKey = s3Object.key()
                val id = objectKey.substringAfterLast('/').removeSuffix(".json")

                try {
                    val dataJson = withContext(Dispatchers.IO) {
                        val getResponse = s3Client.getObject { builder ->
                            builder.bucket(bucket).key(objectKey)
                        }

                        getResponse.use { s3ObjectContent ->
                            s3ObjectContent.readAllBytes().toString(Charsets.UTF_8)
                        }
                    }

                    @Suppress("UNCHECKED_CAST")
                    val data = S3FileStorage.json.decodeFromString<Map<String, Any?>>(dataJson)
                    results.add(Pair(id, data))
                } catch (e: Exception) {
                    logger.error(e) { "解析数据文件失败: $objectKey" }
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

    override suspend fun queryData(collection: String, query: Map<String, Any?>, limit: Int, offset: Int): List<Pair<String, Map<String, Any?>>> {
        logger.debug { "查询数据: 集合: $collection, 查询: $query, 限制: $limit, 偏移: $offset" }

        try {
            val basePath = if (prefix.isNotEmpty()) "$prefix/" else ""
            val prefix = "${basePath}data/$collection/"

            val listObjectsRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build()

            val response = withContext(Dispatchers.IO) {
                s3Client.listObjectsV2(listObjectsRequest)
            }

            val results = mutableListOf<Pair<String, Map<String, Any?>>>()

            for (s3Object in response.contents()) {
                val objectKey = s3Object.key()
                val id = objectKey.substringAfterLast('/').removeSuffix(".json")

                val dataJson = withContext(Dispatchers.IO) {
                    val getResponse = s3Client.getObject { builder ->
                        builder.bucket(bucket).key(objectKey)
                    }

                    getResponse.use { s3ObjectContent ->
                        s3ObjectContent.readAllBytes().toString(Charsets.UTF_8)
                    }
                }

                @Suppress("UNCHECKED_CAST")
                val data = S3FileStorage.json.decodeFromString<Map<String, Any?>>(dataJson)

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
