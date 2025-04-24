package ai.kastrax.datasource.filesystem.plugin.storage

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.event.Event
import ai.kastrax.core.plugin.DataStorage
import ai.kastrax.core.plugin.EventStorage
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

    override suspend fun getAllWorkflowStates(): Flow<WorkflowState> = flow {
        logger.debug { "获取所有工作流状态" }

        try {
            val basePath = if (prefix.isNotEmpty()) "$prefix/" else ""
            val prefix = "${basePath}states/"

            val listObjectsRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build()

            val response = withContext(Dispatchers.IO) {
                s3Client.listObjectsV2(listObjectsRequest)
            }

            val states = response.contents().map { s3Object ->
                val objectKey = s3Object.key()

                val stateJson = withContext(Dispatchers.IO) {
                    val getResponse = s3Client.getObject { builder ->
                        builder.bucket(bucket).key(objectKey)
                    }

                    getResponse.use { s3ObjectContent ->
                        s3ObjectContent.readAllBytes().toString(Charsets.UTF_8)
                    }
                }

                S3FileStorage.json.decodeFromString<WorkflowState>(stateJson)
            }

            // 按更新时间排序
            val sortedStates = states.sortedByDescending { it.updatedAt }

            for (state in sortedStates) {
                emit(state)
            }
        } catch (e: Exception) {
            logger.error(e) { "获取所有工作流状态失败" }
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

    private fun getEventObjectKey(event: Event): String {
        val basePath = if (prefix.isNotEmpty()) "$prefix/" else ""
        return "${basePath}events/${event.type}/${event.id}.json"
    }

    override suspend fun storeEvent(event: Event): Boolean {
        logger.debug { "存储事件: ${event.id}, 类型: ${event.type}" }

        return try {
            val eventJson = S3FileStorage.json.encodeToString(EventWrapper(event))
            val objectKey = getEventObjectKey(event)

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

    override suspend fun getEvents(type: String?, from: Instant?, to: Instant?, limit: Int): Flow<Event> = flow {
        logger.debug { "获取事件: 类型: $type, 从: $from, 到: $to, 限制: $limit" }

        try {
            val basePath = if (prefix.isNotEmpty()) "$prefix/" else ""
            val prefix = if (type != null) "${basePath}events/$type/" else "${basePath}events/"

            val listObjectsRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build()

            val response = withContext(Dispatchers.IO) {
                s3Client.listObjectsV2(listObjectsRequest)
            }

            val events = response.contents().map { s3Object ->
                val objectKey = s3Object.key()

                val eventJson = withContext(Dispatchers.IO) {
                    val getResponse = s3Client.getObject { builder ->
                        builder.bucket(bucket).key(objectKey)
                    }

                    getResponse.use { s3ObjectContent ->
                        s3ObjectContent.readAllBytes().toString(Charsets.UTF_8)
                    }
                }

                S3FileStorage.json.decodeFromString<EventWrapper>(eventJson).event
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

    override suspend fun queryData(collection: String, query: Map<String, Any?>, limit: Int): Flow<Map<String, Any?>> = flow {
        logger.debug { "查询数据: 集合: $collection, 查询: $query, 限制: $limit" }

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

            var count = 0

            for (s3Object in response.contents()) {
                if (count >= limit) break

                val objectKey = s3Object.key()

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
                    emit(data)
                    count++
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
