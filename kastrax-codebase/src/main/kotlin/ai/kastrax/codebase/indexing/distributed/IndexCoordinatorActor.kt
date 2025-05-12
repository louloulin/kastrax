package ai.kastrax.codebase.indexing.distributed

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.fromProducer
import ai.kastrax.codebase.indexing.IndexProcessor
import ai.kastrax.codebase.indexing.IndexTask
import ai.kastrax.codebase.indexing.IndexTaskStatus
import ai.kastrax.codebase.indexing.IndexTaskType
import java.nio.file.Path
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

// 索引协调器消息已移至 IndexCoordinatorMessage.kt

/**
 * 索引任务消息
 */
sealed class IndexTaskMessage {
    /**
     * 处理任务消息
     *
     * @property task 索引任务
     */
    data class ProcessTask(val task: IndexTask) : IndexTaskMessage()

    /**
     * 任务完成消息
     *
     * @property taskId 任务ID
     * @property success 是否成功
     * @property error 错误信息
     */
    data class TaskCompleted(val taskId: String, val success: Boolean, val error: String? = null) : IndexTaskMessage()
}

// 索引协调器配置已移至 DistributedIndexSystemConfig.kt

/**
 * 工作器信息
 *
 * @property pid 工作器PID
 * @property capacity 工作器容量
 * @property activeTaskCount 活动任务数量
 * @property availableSlots 可用槽位数量
 * @property lastHeartbeat 最后心跳时间
 */
data class WorkerInfo(
    val pid: PID,
    val capacity: Int,
    var activeTaskCount: Int = 0,
    var availableSlots: Int = capacity,
    var lastHeartbeat: Instant = Instant.now()
)

/**
 * 任务信息
 *
 * @property task 索引任务
 * @property status 任务状态
 * @property workerId 工作器ID
 * @property retryCount 重试次数
 * @property error 错误信息
 * @property submittedAt 提交时间
 * @property startedAt 开始时间
 * @property completedAt 完成时间
 */
data class TaskInfo(
    val task: IndexTask,
    var status: IndexTaskStatus = IndexTaskStatus.PENDING,
    var workerId: String? = null,
    var retryCount: Int = 0,
    var error: String? = null,
    val submittedAt: Instant = Instant.now(),
    var startedAt: Instant? = null,
    var completedAt: Instant? = null
)

/**
 * 索引协调器 Actor
 *
 * 负责协调索引任务的分发和状态管理
 *
 * @property config 协调器配置
 */
class IndexCoordinatorActor(private val config: IndexCoordinatorConfig = IndexCoordinatorConfig()) : Actor {
    // 任务队列和状态跟踪
    private val pendingTasks = mutableListOf<TaskInfo>()
    private val runningTasks = ConcurrentHashMap<String, TaskInfo>()
    private val completedTasks = ConcurrentHashMap<String, TaskInfo>()
    private val failedTasks = ConcurrentHashMap<String, TaskInfo>()

    // 工作器管理
    private val workers = ConcurrentHashMap<String, WorkerInfo>()

    // 统计信息
    private var totalTasksSubmitted = 0
    private var totalTasksCompleted = 0
    private var totalTasksFailed = 0

    /**
     * 接收消息
     */
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is IndexCoordinatorMessage.SubmitTask -> handleSubmitTask(msg.task)
            is IndexCoordinatorMessage.SubmitTasks -> handleSubmitTasks(msg.tasks)
            is IndexCoordinatorMessage.TaskStatusUpdate -> handleTaskStatusUpdate(msg.taskId, msg.status, msg.error)
            is IndexCoordinatorMessage.RegisterWorker -> handleRegisterWorker(msg.workerId, msg.capacity, msg.pid)
            is IndexCoordinatorMessage.WorkerHeartbeat -> handleWorkerHeartbeat(msg.workerId, msg.activeTaskCount, msg.availableSlots)
            is IndexCoordinatorMessage.GetStatus -> handleGetStatus()
            "started" -> startPeriodicTasks()
            else -> logger.warning("未知消息类型: ${msg::class.simpleName}")
        }
    }

    /**
     * 处理提交任务消息
     *
     * @param task 索引任务
     */
    private suspend fun Context.handleSubmitTask(task: IndexTask) {
        logger.debug("接收到任务: ${task.id}, 类型: ${task.type}, 路径: ${task.path}")

        // 创建任务信息
        val taskInfo = TaskInfo(task)

        // 添加到待处理队列
        synchronized(pendingTasks) {
            pendingTasks.add(taskInfo)
            // 按优先级排序
            pendingTasks.sortByDescending { it.task.priority }
        }

        totalTasksSubmitted++

        // 尝试分配任务
        assignPendingTasks()

        // 响应提交成功
        respond(true)
    }

    /**
     * 处理批量提交任务消息
     *
     * @param tasks 索引任务列表
     */
    private suspend fun Context.handleSubmitTasks(tasks: List<IndexTask>) {
        logger.debug("接收到批量任务: ${tasks.size} 个任务")

        // 创建任务信息并添加到待处理队列
        val taskInfos = tasks.map { TaskInfo(it) }

        synchronized(pendingTasks) {
            pendingTasks.addAll(taskInfos)
            // 按优先级排序
            pendingTasks.sortByDescending { it.task.priority }
        }

        totalTasksSubmitted += tasks.size

        // 尝试分配任务
        assignPendingTasks()

        // 响应提交成功
        respond(true)
    }

    /**
     * 处理任务状态更新消息
     *
     * @param taskId 任务ID
     * @param status 任务状态
     * @param error 错误信息
     */
    private suspend fun Context.handleTaskStatusUpdate(taskId: String, status: IndexTaskStatus, error: String?) {
        logger.debug("任务状态更新: $taskId, 状态: $status, 错误: $error")

        // 查找任务
        val taskInfo = runningTasks[taskId]

        if (taskInfo != null) {
            when (status) {
                IndexTaskStatus.COMPLETED -> {
                    // 更新任务状态
                    taskInfo.status = status
                    taskInfo.completedAt = Instant.now()

                    // 从运行中任务移动到已完成任务
                    runningTasks.remove(taskId)
                    completedTasks[taskId] = taskInfo

                    totalTasksCompleted++

                    // 更新工作器状态
                    taskInfo.workerId?.let { workerId ->
                        val worker = workers[workerId]
                        if (worker != null) {
                            worker.activeTaskCount--
                            worker.availableSlots++
                        }
                    }

                    logger.info("任务完成: $taskId")
                }
                IndexTaskStatus.FAILED -> {
                    // 更新任务状态
                    taskInfo.status = status
                    taskInfo.error = error
                    taskInfo.completedAt = Instant.now()

                    // 检查是否需要重试
                    if (taskInfo.retryCount < config.maxRetries) {
                        // 增加重试次数
                        taskInfo.retryCount++
                        taskInfo.status = IndexTaskStatus.PENDING
                        taskInfo.workerId = null
                        taskInfo.startedAt = null
                        taskInfo.completedAt = null

                        // 重新加入待处理队列
                        synchronized(pendingTasks) {
                            pendingTasks.add(taskInfo)
                            pendingTasks.sortByDescending { it.task.priority }
                        }

                        logger.warning("任务重试: $taskId, 重试次数: ${taskInfo.retryCount}")
                    } else {
                        // 达到最大重试次数，标记为失败
                        runningTasks.remove(taskId)
                        failedTasks[taskId] = taskInfo

                        totalTasksFailed++

                        logger.error("任务失败: $taskId, 错误: $error")
                    }

                    // 更新工作器状态
                    taskInfo.workerId?.let { workerId ->
                        val worker = workers[workerId]
                        if (worker != null) {
                            worker.activeTaskCount--
                            worker.availableSlots++
                        }
                    }
                }
                else -> {
                    // 其他状态更新
                    taskInfo.status = status
                    logger.debug("任务状态更新: $taskId, 新状态: $status")
                }
            }

            // 尝试分配更多任务
            assignPendingTasks()
        } else {
            logger.warning("收到未知任务的状态更新: $taskId")
        }
    }

    /**
     * 处理工作器注册消息
     *
     * @param workerId 工作器ID
     * @param capacity 工作器容量
     * @param pid 工作器PID
     */
    private suspend fun Context.handleRegisterWorker(workerId: String, capacity: Int, pid: PID) {
        logger.info("工作器注册: $workerId, 容量: $capacity")

        // 创建工作器信息
        val workerInfo = WorkerInfo(
            pid = pid,
            capacity = capacity,
            activeTaskCount = 0,
            availableSlots = capacity,
            lastHeartbeat = Instant.now()
        )

        // 添加到工作器列表
        workers[workerId] = workerInfo

        // 尝试分配任务
        assignPendingTasks()

        // 响应注册成功
        respond(true)
    }

    /**
     * 处理工作器心跳消息
     *
     * @param workerId 工作器ID
     * @param activeTaskCount 活动任务数量
     * @param availableSlots 可用槽位数量
     */
    private suspend fun Context.handleWorkerHeartbeat(workerId: String, activeTaskCount: Int, availableSlots: Int) {
        val worker = workers[workerId]

        if (worker != null) {
            // 更新工作器状态
            worker.activeTaskCount = activeTaskCount
            worker.availableSlots = availableSlots
            worker.lastHeartbeat = Instant.now()

            logger.debug("工作器心跳: $workerId, 活动任务: $activeTaskCount, 可用槽位: $availableSlots")

            // 尝试分配任务
            assignPendingTasks()
        } else {
            logger.warning("收到未注册工作器的心跳: $workerId")

            // 要求工作器重新注册
            respond(false)
        }
    }

    /**
     * 处理获取状态消息
     */
    private suspend fun Context.handleGetStatus() {
        // 计算总容量和可用容量
        val totalCapacity = workers.values.sumOf { it.capacity }
        val availableCapacity = workers.values.sumOf { it.availableSlots }

        // 创建状态响应
        val status = IndexCoordinatorMessage.StatusResponse(
            pendingTaskCount = pendingTasks.size,
            runningTaskCount = runningTasks.size,
            completedTaskCount = completedTasks.size,
            failedTaskCount = failedTasks.size,
            workerCount = workers.size,
            totalCapacity = totalCapacity,
            availableCapacity = availableCapacity
        )

        // 响应状态
        respond(status)
    }

    /**
     * 分配待处理任务
     */
    private suspend fun Context.assignPendingTasks() {
        // 查找可用工作器
        val availableWorkers = workers.entries
            .filter { it.value.availableSlots > 0 }
            .sortedByDescending { it.value.availableSlots }
            .toList()

        if (availableWorkers.isEmpty() || pendingTasks.isEmpty()) {
            return
        }

        logger.debug("尝试分配任务, 待处理任务: ${pendingTasks.size}, 可用工作器: ${availableWorkers.size}")

        // 分配任务
        var assignedCount = 0
        val tasksToAssign = synchronized(pendingTasks) {
            val tasks = pendingTasks.take(config.taskAssignmentBatchSize)
            pendingTasks.removeAll(tasks)
            tasks
        }

        for (taskInfo in tasksToAssign) {
            // 循环分配给可用工作器
            val workerIndex = assignedCount % availableWorkers.size
            val (workerId, worker) = availableWorkers[workerIndex]

            if (worker.availableSlots > 0) {
                // 更新任务状态
                taskInfo.status = IndexTaskStatus.RUNNING
                taskInfo.workerId = workerId
                taskInfo.startedAt = Instant.now()

                // 更新工作器状态
                worker.activeTaskCount++
                worker.availableSlots--

                // 添加到运行中任务
                runningTasks[taskInfo.task.id] = taskInfo

                // 发送任务到工作器
                send(worker.pid, IndexWorkerMessage.ProcessTask(taskInfo.task))

                assignedCount++

                logger.debug("分配任务: ${taskInfo.task.id} 到工作器: $workerId")
            }
        }

        logger.debug("分配了 $assignedCount 个任务")
    }

    /**
     * 检查工作器健康状态
     */
    private suspend fun Context.checkWorkerHealth() {
        val now = Instant.now()
        val timeoutThreshold = now.minusMillis(config.workerTimeoutDuration.inWholeMilliseconds)

        // 查找超时的工作器
        val timedOutWorkers = workers.entries
            .filter { it.value.lastHeartbeat.isBefore(timeoutThreshold) }
            .map { it.key }

        for (workerId in timedOutWorkers) {
            logger.warning("工作器超时: $workerId")

            // 移除工作器
            val worker = workers.remove(workerId)

            // 重新分配该工作器的任务
            if (worker != null) {
                val tasksToReassign = runningTasks.values
                    .filter { it.workerId == workerId }
                    .map { it.task.id }

                for (taskId in tasksToReassign) {
                    val taskInfo = runningTasks[taskId]
                    if (taskInfo != null) {
                        // 重置任务状态
                        taskInfo.status = IndexTaskStatus.PENDING
                        taskInfo.workerId = null
                        taskInfo.startedAt = null

                        // 从运行中任务移除
                        runningTasks.remove(taskId)

                        // 添加到待处理队列
                        synchronized(pendingTasks) {
                            pendingTasks.add(taskInfo)
                            pendingTasks.sortByDescending { it.task.priority }
                        }

                        logger.warning("重新分配任务: $taskId (工作器超时)")
                    }
                }
            }
        }
    }

    /**
     * 启动定时任务
     */
    private suspend fun Context.startPeriodicTasks() {
        // 定期检查工作器健康状态
        val props = fromProducer {
            object : Actor {
                override suspend fun Context.receive(msg: Any) {
                    checkWorkerHealth()
                    delay(config.heartbeatInterval.inWholeMilliseconds)
                    send(self, "continue")
                }
            }
        }
        val healthChecker = spawnNamed(props, "health-checker")
        send(healthChecker, "continue")
    }

    companion object {
        /**
         * 创建 Props
         *
         * @param config 协调器配置
         * @return Props
         */
        fun props(config: IndexCoordinatorConfig = IndexCoordinatorConfig()): Props {
            return fromProducer { IndexCoordinatorActor(config) }
        }
    }
}

// 索引工作器消息已移至 IndexWorkerMessage.kt



