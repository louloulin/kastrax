package ai.kastrax.codebase.indexing.distributed

import ai.kastrax.codebase.actor.ActorSystem
import ai.kastrax.codebase.actor.PID
import ai.kastrax.codebase.indexing.IndexProcessor
import ai.kastrax.codebase.indexing.IndexTask
import ai.kastrax.codebase.indexing.IndexTaskStatus
import ai.kastrax.codebase.indexing.IndexTaskType
import java.nio.file.Path
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/**
 * 分布式索引系统配置
 *
 * @property coordinatorConfig 协调器配置
 * @property workerConfig 工作器配置
 * @property shardManagerConfig 分片管理器配置
 * @property localWorkerCount 本地工作器数量
 * @property taskSubmitTimeout 任务提交超时时间
 */
// 配置类已移至DistributedIndexSystemConfig.kt文件

/**
 * 索引协调器事件
 */
sealed class IndexCoordinatorEvent {
    /**
     * 任务状态变更事件
     *
     * @property taskId 任务ID
     * @property status 状态
     * @property error 错误信息
     */
    data class TaskStatusChanged(
        val taskId: String,
        val status: IndexTaskStatus,
        val error: String? = null
    ) : IndexCoordinatorEvent()

    /**
     * 工作器状态变更事件
     *
     * @property workerId 工作器ID
     * @property isActive 是否活跃
     */
    data class WorkerStatusChanged(
        val workerId: String,
        val isActive: Boolean
    ) : IndexCoordinatorEvent()

    /**
     * 系统状态变更事件
     *
     * @property status 系统状态
     */
    data class SystemStatusChanged(
        val status: SystemStatus
    ) : IndexCoordinatorEvent()
}

/**
 * 分布式索引系统
 *
 * 提供统一的接口来管理分布式索引系统
 *
 * @property actorSystem Actor系统
 * @property indexProcessor 索引处理器
 * @property config 系统配置
 */
class DistributedIndexSystem(
    private val actorSystem: ActorSystem,
    private val indexProcessor: IndexProcessor,
    private val config: DistributedIndexSystemConfig = DistributedIndexSystemConfig()
) {
    private val logger = KotlinLogging.logger {}

    // Actor引用
    private lateinit var coordinatorPid: PID
    private lateinit var shardManagerPid: PID
    private val workerPids = mutableListOf<PID>()

    // 系统状态
    private val isStarted = AtomicBoolean(false)
    private val taskResults = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    // 事件流
    private val _eventFlow = MutableSharedFlow<IndexCoordinatorEvent>(replay = 0, extraBufferCapacity = 100)
    private val eventFlow = _eventFlow.asSharedFlow()

    /**
     * 启动系统
     */
    suspend fun start() = withContext(Dispatchers.Default) {
        if (isStarted.getAndSet(true)) {
            logger.warn { "分布式索引系统已经启动" }
            return@withContext
        }

        logger.info { "启动分布式索引系统" }

        try {
            // 启动协调器
            coordinatorPid = actorSystem.spawn(
                IndexCoordinatorActor.props(config.coordinatorConfig),
                "index-coordinator"
            )

            // 启动分片管理器
            shardManagerPid = actorSystem.spawn(
                IndexShardManager.props(config.shardManagerConfig),
                "index-shard-manager"
            )

            // 启动本地工作器
            for (i in 0 until config.localWorkerCount) {
                val workerId = "worker-${UUID.randomUUID()}"
                val workerPid = actorSystem.spawn(
                    IndexWorkerActor.props(
                        workerId = workerId,
                        coordinatorPid = coordinatorPid,
                        indexProcessor = indexProcessor,
                        config = config.workerConfig
                    ),
                    "index-worker-$i"
                )
                workerPids.add(workerPid)
            }

            // 发布系统启动事件
            val status = getStatus()
            _eventFlow.emit(IndexCoordinatorEvent.SystemStatusChanged(status))

            logger.info { "分布式索引系统启动成功" }
        } catch (e: Exception) {
            logger.error(e) { "启动分布式索引系统时出错" }
            isStarted.set(false)
            throw e
        }
    }

    /**
     * 停止系统
     */
    suspend fun stop() = withContext(Dispatchers.Default) {
        if (!isStarted.getAndSet(false)) {
            logger.warn { "分布式索引系统尚未启动" }
            return@withContext
        }

        logger.info { "停止分布式索引系统" }

        try {
            // 停止工作器
            for (workerPid in workerPids) {
                actorSystem.stop(workerPid)
            }
            workerPids.clear()

            // 停止分片管理器
            actorSystem.stop(shardManagerPid)

            // 停止协调器
            actorSystem.stop(coordinatorPid)

            // 发布系统停止事件
            _eventFlow.emit(IndexCoordinatorEvent.SystemStatusChanged(SystemStatus.STOPPED))

            logger.info { "分布式索引系统停止成功" }
        } catch (e: Exception) {
            logger.error(e) { "停止分布式索引系统时出错" }
            throw e
        }
    }

    /**
     * 提交索引任务
     *
     * @param task 索引任务
     * @return 是否成功提交
     */
    suspend fun submitTask(task: IndexTask): Boolean = withContext(Dispatchers.Default) {
        if (!isStarted.get()) {
            logger.warn { "分布式索引系统尚未启动" }
            return@withContext false
        }

        logger.debug { "提交索引任务: ${task.id}" }

        try {
            val result = actorSystem.ask<Boolean>(
                coordinatorPid,
                IndexCoordinatorMessage.SubmitTask(task),
                config.taskSubmitTimeout.inWholeMilliseconds
            )

            return@withContext result
        } catch (e: Exception) {
            logger.error(e) { "提交索引任务时出错: ${task.id}" }
            return@withContext false
        }
    }

    /**
     * 批量提交索引任务
     *
     * @param tasks 索引任务列表
     * @return 成功提交的任务数量
     */
    suspend fun submitTasks(tasks: List<IndexTask>): Int = withContext(Dispatchers.Default) {
        if (!isStarted.get()) {
            logger.warn { "分布式索引系统尚未启动" }
            return@withContext 0
        }

        if (tasks.isEmpty()) {
            return@withContext 0
        }

        logger.debug { "批量提交索引任务: ${tasks.size} 个任务" }

        try {
            // 分批提交任务
            val batchSize = 100
            val batches = tasks.chunked(batchSize)

            var successCount = 0

            for (batch in batches) {
                val result = actorSystem.ask<Boolean>(
                    coordinatorPid,
                    IndexCoordinatorMessage.SubmitTasks(batch),
                    config.taskSubmitTimeout.inWholeMilliseconds
                )

                if (result) {
                    successCount += batch.size
                }
            }

            return@withContext successCount
        } catch (e: Exception) {
            logger.error(e) { "批量提交索引任务时出错" }
            return@withContext 0
        }
    }

    /**
     * 获取系统状态
     *
     * @return 系统状态
     */
    suspend fun getStatus(): SystemStatus = withContext(Dispatchers.Default) {
        if (!isStarted.get()) {
            logger.warn { "分布式索引系统尚未启动" }
            return@withContext SystemStatus.STOPPED
        }

        try {
            // 获取协调器状态
            val coordinatorStatus = actorSystem.ask<IndexCoordinatorMessage.StatusResponse>(
                coordinatorPid,
                IndexCoordinatorMessage.GetStatus,
                config.taskSubmitTimeout.inWholeMilliseconds
            )

            // 获取分片管理器状态
            val shardManagerStatus = actorSystem.ask<IndexShardManagerMessage.GetAllShardsResponse>(
                shardManagerPid,
                IndexShardManagerMessage.GetAllShards,
                config.taskSubmitTimeout.inWholeMilliseconds
            )

            // 获取工作器状态
            val workerStatuses = coroutineScope {
                workerPids.map { workerPid ->
                    async {
                        try {
                            actorSystem.ask<IndexWorkerMessage.StatusResponse>(
                                workerPid,
                                IndexWorkerMessage.GetStatus,
                                config.taskSubmitTimeout.inWholeMilliseconds
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }

            // 返回系统状态
            return@withContext SystemStatus.RUNNING
        } catch (e: Exception) {
            logger.error(e) { "获取系统状态时出错" }
            return@withContext if (isStarted.get()) SystemStatus.RUNNING else SystemStatus.STOPPED
        }
    }

    /**
     * 获取分片信息
     *
     * @param key 键
     * @return 分片信息
     */
    suspend fun getShardInfo(key: String): ShardInfo? = withContext(Dispatchers.Default) {
        if (!isStarted.get()) {
            logger.warn { "分布式索引系统尚未启动" }
            return@withContext null
        }

        try {
            // 获取分片
            val response = actorSystem.ask<IndexShardManagerMessage.GetShardResponse>(
                shardManagerPid,
                IndexShardManagerMessage.GetShard(key),
                config.taskSubmitTimeout.inWholeMilliseconds
            )

            // 获取分片信息
            val allShardsResponse = actorSystem.ask<IndexShardManagerMessage.GetAllShardsResponse>(
                shardManagerPid,
                IndexShardManagerMessage.GetAllShards,
                config.taskSubmitTimeout.inWholeMilliseconds
            )

            return@withContext allShardsResponse.shards[response.shardId]
        } catch (e: Exception) {
            logger.error(e) { "获取分片信息时出错: $key" }
            return@withContext null
        }
    }

    /**
     * 获取索引事件流
     *
     * @return 索引事件流
     */
    fun indexEvents(): Flow<IndexCoordinatorEvent> {
        return eventFlow
    }

    /**
     * 请求重新索引
     *
     * @param path 路径
     * @return 是否成功提交
     */
    suspend fun requestReindex(path: String): Boolean {
        // 创建重新索引任务
        val task = IndexTask(
            id = UUID.randomUUID().toString(),
            type = IndexTaskType.FULL_REINDEX,
            path = Path.of(path)
        )

        return submitTask(task)
    }

    /**
     * 关闭资源
     */
    fun close() {
        runBlocking {
            stop()
        }
    }

    // SystemStatus 已移至单独的文件
}
