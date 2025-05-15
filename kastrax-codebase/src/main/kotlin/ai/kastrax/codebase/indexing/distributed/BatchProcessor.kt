package ai.kastrax.codebase.indexing.distributed

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.Props
import actor.proto.fromProducer
import actor.proto.spawn
import ai.kastrax.codebase.indexing.IndexTask
import ai.kastrax.codebase.indexing.IndexTaskProcessor
import ai.kastrax.codebase.indexing.IndexTaskType
import ai.kastrax.codebase.indexing.IncrementalIndexTask
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/**
 * 批处理状态
 */
enum class BatchProcessingStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

/**
 * 批处理结果
 *
 * @property batchId 批处理ID
 * @property status 状态
 * @property tasksTotal 总任务数
 * @property tasksCompleted 已完成任务数
 * @property tasksFailed 失败任务数
 * @property startTime 开始时间
 * @property endTime 结束时间
 * @property error 错误信息
 */
data class BatchProcessingResult(
    val batchId: String,
    val status: BatchProcessingStatus,
    val tasksTotal: Int,
    val tasksCompleted: Int = 0,
    val tasksFailed: Int = 0,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val error: String? = null
)

/**
 * 批处理器配置
 *
 * @property maxConcurrentBatches 最大并发批处理数
 * @property maxTasksPerBatch 每批最大任务数
 * @property maxRetries 最大重试次数
 * @property batchTimeout 批处理超时时间
 * @property taskAssignmentInterval 任务分配间隔
 * @property statusCheckInterval 状态检查间隔
 */
data class BatchProcessorConfig(
    val maxConcurrentBatches: Int = 3,
    val maxTasksPerBatch: Int = 1000,
    val maxRetries: Int = 3,
    val batchTimeout: Duration = 60.seconds,
    val taskAssignmentInterval: Duration = 1.seconds,
    val statusCheckInterval: Duration = 5.seconds
)

/**
 * 批处理器消息
 */
sealed class BatchProcessorMessage {
    /**
     * 处理批次消息
     *
     * @property tasks 任务列表
     */
    data class ProcessBatch(val tasks: List<IncrementalIndexTask>) : BatchProcessorMessage()

    /**
     * 任务完成消息
     *
     * @property batchId 批处理ID
     * @property taskId 任务ID
     * @property success 是否成功
     * @property error 错误信息
     */
    data class TaskCompleted(
        val batchId: String,
        val taskId: String,
        val success: Boolean,
        val error: String? = null
    ) : BatchProcessorMessage()

    /**
     * 获取状态消息
     */
    object GetStatus : BatchProcessorMessage()

    /**
     * 状态响应消息
     *
     * @property activeBatches 活跃批处理数
     * @property completedBatches 已完成批处理数
     * @property failedBatches 失败批处理数
     * @property totalTasksProcessed 总处理任务数
     */
    data class StatusResponse(
        val activeBatches: Int,
        val completedBatches: Int,
        val failedBatches: Int,
        val totalTasksProcessed: Int
    ) : BatchProcessorMessage()
}

/**
 * 基于 Actor 的批处理器
 *
 * 使用 Actor 模型实现的批处理器，支持高效处理大量索引任务
 *
 * @property actorSystem Actor 系统
 * @property indexTaskProcessor 索引任务处理器
 * @property config 配置
 */
class ActorBasedBatchProcessor(
    private val actorSystem: ActorSystem,
    private val indexTaskProcessor: IndexTaskProcessor,
    private val config: BatchProcessorConfig = BatchProcessorConfig()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 批处理计数器
    private val batchCounter = AtomicInteger(0)

    // 活跃的批处理
    private val activeBatches = ConcurrentHashMap<String, BatchProcessingResult>()

    // 批处理结果流
    private val _batchResults = MutableSharedFlow<BatchProcessingResult>(extraBufferCapacity = 100)
    val batchResults: SharedFlow<BatchProcessingResult> = _batchResults

    // 当前活跃批处理数
    private val activeBatchCount = AtomicInteger(0)

    // 批处理器 Actor
    private lateinit var batchProcessorPid: PID

    // 是否已启动
    private val isStarted = AtomicBoolean(false)

    // 统计信息
    private var completedBatchCount = 0
    private var failedBatchCount = 0
    private var totalTasksProcessed = 0

    /**
     * 启动批处理器
     */
    fun start() {
        if (isStarted.getAndSet(true)) {
            logger.info("批处理器已经启动")
            return
        }

        logger.info("启动批处理器")

        // 创建批处理器 Actor
        val props = fromProducer { BatchProcessorActor() }
        batchProcessorPid = actorSystem.root.spawnNamed(props, "batchProcessor")
    }

    /**
     * 停止批处理器
     */
    suspend fun stop() {
        if (!isStarted.getAndSet(false)) {
            logger.info("批处理器尚未启动")
            return
        }

        logger.info("停止批处理器")

        // 停止 Actor
        actorSystem.stop(batchProcessorPid)

        // 清理资源
        activeBatches.clear()
        activeBatchCount.set(0)
    }

    /**
     * 处理任务列表
     *
     * @param tasks 任务列表
     * @return 批处理ID
     */
    suspend fun processTasks(tasks: List<IncrementalIndexTask>): String = withContext(Dispatchers.Default) {
        // 如果没有任务，则跳过
        if (tasks.isEmpty()) {
            return@withContext ""
        }

        // 检查是否已启动
        if (!isStarted.get()) {
            logger.info("批处理器尚未启动")
            return@withContext ""
        }

        // 检查是否达到最大并发批处理数
        if (activeBatchCount.get() >= config.maxConcurrentBatches) {
            logger.info("达到最大并发批处理数，等待中: ${tasks.size} 个任务")
            return@withContext ""
        }

        // 生成批处理ID
        val batchId = "batch-${batchCounter.incrementAndGet()}"

        // 创建批处理结果
        val batchResult = BatchProcessingResult(
            batchId = batchId,
            status = BatchProcessingStatus.PENDING,
            tasksTotal = tasks.size
        )

        // 添加到活跃批处理
        activeBatches[batchId] = batchResult
        activeBatchCount.incrementAndGet()

        // 发送批处理结果
        _batchResults.emit(batchResult)

        // 发送处理批次消息
        actorSystem.send(batchProcessorPid, BatchProcessorMessage.ProcessBatch(tasks))

        return@withContext batchId
    }

    /**
     * 获取批处理状态
     *
     * @param batchId 批处理ID
     * @return 批处理结果
     */
    fun getBatchStatus(batchId: String): BatchProcessingResult? {
        return activeBatches[batchId]
    }

    /**
     * 获取所有活跃批处理
     *
     * @return 活跃批处理列表
     */
    fun getActiveBatches(): List<BatchProcessingResult> {
        return activeBatches.values.toList()
    }

    /**
     * 获取批处理器状态
     *
     * @return 状态响应
     */
    suspend fun getStatus(): BatchProcessorMessage.StatusResponse = withContext(Dispatchers.Default) {
        return@withContext BatchProcessorMessage.StatusResponse(
            activeBatches = activeBatchCount.get(),
            completedBatches = completedBatchCount,
            failedBatches = failedBatchCount,
            totalTasksProcessed = totalTasksProcessed
        )
    }

    /**
     * 更新批处理状态
     *
     * @param batchId 批处理ID
     * @param status 状态
     */
    private suspend fun updateBatchStatus(batchId: String, status: BatchProcessingStatus) {
        val current = activeBatches[batchId] ?: return
        val updated = current.copy(status = status)
        activeBatches[batchId] = updated

        // 发送更新
        _batchResults.emit(updated)
    }

    /**
     * 更新批处理进度
     *
     * @param batchId 批处理ID
     * @param completed 已完成任务数
     * @param failed 失败任务数
     */
    private suspend fun updateBatchProgress(batchId: String, completed: Int, failed: Int) {
        val current = activeBatches[batchId] ?: return
        val updated = current.copy(
            tasksCompleted = completed,
            tasksFailed = failed
        )
        activeBatches[batchId] = updated

        // 发送更新
        _batchResults.emit(updated)
    }

    /**
     * 完成批处理
     *
     * @param batchId 批处理ID
     * @param completed 已完成任务数
     * @param failed 失败任务数
     */
    private suspend fun completeBatch(batchId: String, completed: Int, failed: Int) {
        val current = activeBatches[batchId] ?: return
        val updated = current.copy(
            status = BatchProcessingStatus.COMPLETED,
            tasksCompleted = completed,
            tasksFailed = failed,
            endTime = System.currentTimeMillis()
        )
        activeBatches[batchId] = updated

        logger.info { "批处理完成: $batchId, 完成: $completed, 失败: $failed" }

        // 更新统计信息
        completedBatchCount++
        totalTasksProcessed += completed

        // 发送更新
        _batchResults.emit(updated)

        // 从活跃批处理中移除
        activeBatches.remove(batchId)
        activeBatchCount.decrementAndGet()
    }

    /**
     * 批处理失败
     *
     * @param batchId 批处理ID
     * @param error 错误信息
     */
    private suspend fun failBatch(batchId: String, error: String) {
        val current = activeBatches[batchId] ?: return
        val updated = current.copy(
            status = BatchProcessingStatus.FAILED,
            endTime = System.currentTimeMillis(),
            error = error
        )
        activeBatches[batchId] = updated

        logger.error("批处理失败: $batchId, 错误: $error")

        // 更新统计信息
        failedBatchCount++

        // 发送更新
        _batchResults.emit(updated)

        // 从活跃批处理中移除
        activeBatches.remove(batchId)
        activeBatchCount.decrementAndGet()
    }

    /**
     * 批处理器 Actor
     */
    inner class BatchProcessorActor : actor.proto.Actor {
        // 任务计数器
        private val taskCounters = ConcurrentHashMap<String, TaskCounter>()

        // 任务重试计数
        private val taskRetries = ConcurrentHashMap<String, Int>()

        /**
         * 接收消息
         */
        override suspend fun actor.proto.Context.receive(msg: Any) {
            when (msg) {
                is BatchProcessorMessage.ProcessBatch -> handleProcessBatch(msg.tasks)
                is BatchProcessorMessage.TaskCompleted -> handleTaskCompleted(
                    msg.batchId,
                    msg.taskId,
                    msg.success,
                    msg.error
                )
                is BatchProcessorMessage.GetStatus -> handleGetStatus()
                "started" -> {
                    logger.info("批处理器 Actor 已启动")
                    startPeriodicTasks()
                }
                else -> logger.info("未知消息类型: ${msg::class.simpleName}")
            }
        }

        /**
         * 处理批次
         *
         * @param tasks 任务列表
         */
        private suspend fun actor.proto.Context.handleProcessBatch(tasks: List<IncrementalIndexTask>) {
            if (tasks.isEmpty()) {
                return
            }

            // 生成批处理ID
            val batchId = "batch-${UUID.randomUUID()}"

            logger.info("开始处理批次: $batchId, 任务数: ${tasks.size}")

            // 创建任务计数器
            val taskCounter = TaskCounter(tasks.size)
            taskCounters[batchId] = taskCounter

            // 更新批处理状态
            updateBatchStatus(batchId, BatchProcessingStatus.PROCESSING)

            // 处理每个任务
            for (task in tasks) {
                try {
                    // 处理任务
                    processTask(batchId, task)
                } catch (e: Exception) {
                    logger.error("处理任务失败: ${task.id}", e)
                    handleTaskCompleted(batchId, task.id, false, e.message)
                }
            }
        }

        /**
         * 处理任务
         *
         * @param batchId 批处理ID
         * @param task 任务
         */
        private suspend fun actor.proto.Context.processTask(batchId: String, task: IncrementalIndexTask) {
            // 创建任务处理 Actor
            val props = fromProducer { TaskProcessorActor(batchId, task) }
            val taskProcessorPid = spawn(props)

            // 发送开始消息
            send(taskProcessorPid, "start")
        }

        /**
         * 处理任务完成
         *
         * @param batchId 批处理ID
         * @param taskId 任务ID
         * @param success 是否成功
         * @param error 错误信息
         */
        private suspend fun actor.proto.Context.handleTaskCompleted(
            batchId: String,
            taskId: String,
            success: Boolean,
            error: String?
        ) {
            val taskCounter = taskCounters[batchId] ?: return

            if (success) {
                // 增加完成计数
                taskCounter.incrementCompleted()
            } else {
                // 增加失败计数
                taskCounter.incrementFailed()

                // 检查是否需要重试
                val retryCount = taskRetries.getOrDefault(taskId, 0)
                if (retryCount < config.maxRetries) {
                    // 增加重试计数
                    taskRetries[taskId] = retryCount + 1

                    logger.info("任务失败，重试: $taskId, 重试次数: ${retryCount + 1}")

                    // 重试任务
                    // 在实际实现中，这里会重新提交任务
                }
            }

            // 更新批处理进度
            updateBatchProgress(batchId, taskCounter.completed.get(), taskCounter.failed.get())

            // 检查批处理是否完成
            if (taskCounter.isCompleted()) {
                // 完成批处理
                completeBatch(batchId, taskCounter.completed.get(), taskCounter.failed.get())

                // 清理资源
                taskCounters.remove(batchId)
            }
        }

        /**
         * 处理获取状态
         */
        private suspend fun actor.proto.Context.handleGetStatus() {
            val status = BatchProcessorMessage.StatusResponse(
                activeBatches = activeBatchCount.get(),
                completedBatches = completedBatchCount,
                failedBatches = failedBatchCount,
                totalTasksProcessed = totalTasksProcessed
            )

            respond(status)
        }

        /**
         * 启动定期任务
         */
        private suspend fun actor.proto.Context.startPeriodicTasks() {
            // 定期检查批处理状态
            val statusCheckerProps = fromProducer {
                object : actor.proto.Actor {
                    override suspend fun actor.proto.Context.receive(msg: Any) {
                        checkBatchStatus()
                        delay(config.statusCheckInterval.inWholeMilliseconds)
                        send(self, "continue")
                    }
                }
            }
            val statusChecker = spawnNamed(statusCheckerProps, "status-checker")
            send(statusChecker, "continue")
        }

        /**
         * 检查批处理状态
         */
        private suspend fun actor.proto.Context.checkBatchStatus() {
            val now = System.currentTimeMillis()

            // 检查超时的批处理
            for ((batchId, result) in activeBatches) {
                if (result.status == BatchProcessingStatus.PROCESSING) {
                    val duration = now - result.startTime
                    if (duration > config.batchTimeout.inWholeMilliseconds) {
                        logger.info("批处理超时: $batchId")
                        failBatch(batchId, "批处理超时")
                    }
                }
            }
        }
    }

    /**
     * 任务处理器 Actor
     *
     * @property batchId 批处理ID
     * @property task 任务
     */
    inner class TaskProcessorActor(
        private val batchId: String,
        private val task: IncrementalIndexTask
    ) : actor.proto.Actor {
        /**
         * 接收消息
         */
        override suspend fun actor.proto.Context.receive(msg: Any) {
            when (msg) {
                "start" -> processTask()
                else -> logger.info("未知消息类型: ${msg::class.simpleName}")
            }
        }

        /**
         * 处理任务
         */
        private suspend fun actor.proto.Context.processTask() {
            try {
                // 处理任务
                indexTaskProcessor.processTask(task)

                // 发送任务完成消息
                send(
                    batchProcessorPid,
                    BatchProcessorMessage.TaskCompleted(
                        batchId = batchId,
                        taskId = task.id,
                        success = true
                    )
                )
            } catch (e: Exception) {
                logger.error("处理任务失败: ${task.id}", e)

                // 发送任务失败消息
                send(
                    batchProcessorPid,
                    BatchProcessorMessage.TaskCompleted(
                        batchId = batchId,
                        taskId = task.id,
                        success = false,
                        error = e.message
                    )
                )
            } finally {
                // 停止 Actor
                actorSystem.stop(self)
            }
        }
    }

    /**
     * 任务计数器
     *
     * @property total 总任务数
     */
    private class TaskCounter(val total: Int) {
        // 已完成任务数
        val completed = AtomicInteger(0)

        // 失败任务数
        val failed = AtomicInteger(0)

        /**
         * 增加完成计数
         */
        fun incrementCompleted() {
            completed.incrementAndGet()
        }

        /**
         * 增加失败计数
         */
        fun incrementFailed() {
            failed.incrementAndGet()
        }

        /**
         * 是否已完成
         */
        fun isCompleted(): Boolean {
            return completed.get() + failed.get() >= total
        }
    }
}
