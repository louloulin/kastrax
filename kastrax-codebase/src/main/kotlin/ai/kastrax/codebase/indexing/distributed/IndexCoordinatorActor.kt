package ai.kastrax.codebase.indexing.distributed

import actor.proto.Actor
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import ai.kastrax.codebase.indexing.IndexTask
import ai.kastrax.codebase.indexing.IndexTaskStatus
import ai.kastrax.codebase.indexing.IndexTaskType
import java.nio.file.Path
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/**
 * 索引协调器消息
 */
sealed class IndexCoordinatorMessage {
    /**
     * 提交任务消息
     *
     * @property task 索引任务
     */
    data class SubmitTask(val task: IndexTask) : IndexCoordinatorMessage()

    /**
     * 批量提交任务消息
     *
     * @property tasks 索引任务列表
     */
    data class SubmitTasks(val tasks: List<IndexTask>) : IndexCoordinatorMessage()

    /**
     * 任务状态更新消息
     *
     * @property taskId 任务ID
     * @property status 任务状态
     * @property error 错误信息
     */
    data class TaskStatusUpdate(val taskId: String, val status: IndexTaskStatus, val error: String? = null) : IndexCoordinatorMessage()

    /**
     * 工作器注册消息
     *
     * @property workerId 工作器ID
     * @property capacity 工作器容量
     * @property pid 工作器PID
     */
    data class RegisterWorker(val workerId: String, val capacity: Int, val pid: PID) : IndexCoordinatorMessage()

    /**
     * 工作器心跳消息
     *
     * @property workerId 工作器ID
     * @property activeTaskCount 活动任务数量
     * @property availableSlots 可用槽位数量
     */
    data class WorkerHeartbeat(val workerId: String, val activeTaskCount: Int, val availableSlots: Int) : IndexCoordinatorMessage()

    /**
     * 获取状态消息
     */
    object GetStatus : IndexCoordinatorMessage()

    /**
     * 状态响应消息
     *
     * @property pendingTaskCount 待处理任务数量
     * @property runningTaskCount 运行中任务数量
     * @property completedTaskCount 已完成任务数量
     * @property failedTaskCount 失败任务数量
     * @property workerCount 工作器数量
     * @property totalCapacity 总容量
     * @property availableCapacity 可用容量
     */
    data class StatusResponse(
        val pendingTaskCount: Int,
        val runningTaskCount: Int,
        val completedTaskCount: Int,
        val failedTaskCount: Int,
        val workerCount: Int,
        val totalCapacity: Int,
        val availableCapacity: Int
    ) : IndexCoordinatorMessage()
}

/**
 * 索引协调器配置
 *
 * @property heartbeatInterval 心跳间隔
 * @property workerTimeoutDuration 工作器超时时间
 * @property taskAssignmentBatchSize 任务分配批次大小
 * @property maxRetries 最大重试次数
 */
data class IndexCoordinatorConfig(
    val heartbeatInterval: Duration = 10.seconds,
    val workerTimeoutDuration: Duration = 30.seconds,
    val taskAssignmentBatchSize: Int = 10,
    val maxRetries: Int = 3
)

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
            else -> logger.warn { "未知消息类型: ${msg::class.simpleName}" }
        }
    }

    /**
     * 处理提交任务消息
     *
     * @param task 索引任务
     */
    private suspend fun Context.handleSubmitTask(task: IndexTask) {
        logger.debug { "接收到任务: ${task.id}, 类型: ${task.type}, 路径: ${task.path}" }

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
        logger.debug { "接收到批量任务: ${tasks.size} 个任务" }

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
        logger.debug { "任务状态更新: $taskId, 状态: $status, 错误: $error" }

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

                    logger.info { "任务完成: $taskId" }
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

                        logger.warn { "任务重试: $taskId, 重试次数: ${taskInfo.retryCount}" }
                    } else {
                        // 达到最大重试次数，标记为失败
                        runningTasks.remove(taskId)
                        failedTasks[taskId] = taskInfo

                        totalTasksFailed++

                        logger.error { "任务失败: $taskId, 错误: $error" }
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
                    logger.debug { "任务状态更新: $taskId, 新状态: $status" }
                }
            }

            // 尝试分配更多任务
            assignPendingTasks()
        } else {
            logger.warn { "收到未知任务的状态更新: $taskId" }
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
        logger.info { "工作器注册: $workerId, 容量: $capacity" }

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

            logger.debug { "工作器心跳: $workerId, 活动任务: $activeTaskCount, 可用槽位: $availableSlots" }

            // 尝试分配任务
            assignPendingTasks()
        } else {
            logger.warn { "收到未注册工作器的心跳: $workerId" }

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

        logger.debug { "尝试分配任务, 待处理任务: ${pendingTasks.size}, 可用工作器: ${availableWorkers.size}" }

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

                logger.debug { "分配任务: ${taskInfo.task.id} 到工作器: $workerId" }
            }
        }

        logger.debug { "分配了 $assignedCount 个任务" }
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
            logger.warn { "工作器超时: $workerId" }

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

                        logger.warn { "重新分配任务: $taskId (工作器超时)" }
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
        spawnNamed("health-checker") {
            while (true) {
                checkWorkerHealth()
                delay(config.heartbeatInterval.inWholeMilliseconds)
            }
        }
    }

    companion object {
        /**
         * 创建 Props
         *
         * @param config 协调器配置
         * @return Props
         */
        fun props(config: IndexCoordinatorConfig = IndexCoordinatorConfig()): Props {
            return Props.fromProducer { IndexCoordinatorActor(config) }
        }
    }
}

/**
 * 索引工作器消息
 */
sealed class IndexWorkerMessage {
    /**
     * 处理任务消息
     *
     * @property task 索引任务
     */
    data class ProcessTask(val task: IndexTask) : IndexWorkerMessage()

    /**
     * 获取状态消息
     */
    object GetStatus : IndexWorkerMessage()

    /**
     * 状态响应消息
     *
     * @property activeTaskCount 活动任务数量
     * @property availableSlots 可用槽位数量
     * @property completedTaskCount 已完成任务数量
     * @property failedTaskCount 失败任务数量
     */
    data class StatusResponse(
        val activeTaskCount: Int,
        val availableSlots: Int,
        val completedTaskCount: Int,
        val failedTaskCount: Int
    ) : IndexWorkerMessage()
}

    /**
     * 批量提交任务消息
     *
     * @property tasks 索引任务列表
     */
    data class SubmitBatch(val tasks: List<IndexTask>) : IndexCoordinatorMessage()

    /**
     * 注册工作者消息
     *
     * @property worker 工作者 PID
     */
    data class RegisterWorker(val worker: PID) : IndexCoordinatorMessage()

    /**
     * 注销工作者消息
     *
     * @property worker 工作者 PID
     */
    data class UnregisterWorker(val worker: PID) : IndexCoordinatorMessage()

    /**
     * 获取状态消息
     */
    object GetStatus : IndexCoordinatorMessage()

    /**
     * 状态响应消息
     *
     * @property pendingTaskCount 待处理任务数量
     * @property activeTaskCount 活动任务数量
     * @property completedTaskCount 已完成任务数量
     * @property failedTaskCount 失败任务数量
     * @property workerCount 工作者数量
     */
    data class StatusResponse(
        val pendingTaskCount: Int,
        val activeTaskCount: Int,
        val completedTaskCount: Int,
        val failedTaskCount: Int,
        val workerCount: Int
    ) : IndexCoordinatorMessage()
}

/**
 * 索引协调者事件
 */
sealed class IndexCoordinatorEvent {
    /**
     * 任务提交事件
     *
     * @property taskId 任务ID
     */
    data class TaskSubmitted(val taskId: String) : IndexCoordinatorEvent()

    /**
     * 任务分配事件
     *
     * @property taskId 任务ID
     * @property workerId 工作者ID
     */
    data class TaskAssigned(val taskId: String, val workerId: String) : IndexCoordinatorEvent()

    /**
     * 任务完成事件
     *
     * @property taskId 任务ID
     * @property workerId 工作者ID
     */
    data class TaskCompleted(val taskId: String, val workerId: String) : IndexCoordinatorEvent()

    /**
     * 任务失败事件
     *
     * @property taskId 任务ID
     * @property workerId 工作者ID
     * @property error 错误信息
     */
    data class TaskFailed(val taskId: String, val workerId: String, val error: String) : IndexCoordinatorEvent()

    /**
     * 工作者注册事件
     *
     * @property workerId 工作者ID
     */
    data class WorkerRegistered(val workerId: String) : IndexCoordinatorEvent()

    /**
     * 工作者注销事件
     *
     * @property workerId 工作者ID
     */
    data class WorkerUnregistered(val workerId: String) : IndexCoordinatorEvent()
}

/**
 * 索引协调者配置
 *
 * @property maxPendingTasks 最大待处理任务数量
 * @property taskAssignmentInterval 任务分配间隔
 * @property workerStatusCheckInterval 工作者状态检查间隔
 * @property initialWorkerCount 初始工作者数量
 */
data class IndexCoordinatorConfig(
    val maxPendingTasks: Int = 10000,
    val taskAssignmentInterval: Duration = 1.seconds,
    val workerStatusCheckInterval: Duration = 10.seconds,
    val initialWorkerCount: Int = Runtime.getRuntime().availableProcessors()
)

/**
 * 索引协调者 Actor
 *
 * 负责协调索引任务的分配和执行
 *
 * @property system Actor 系统
 * @property taskProcessor 任务处理器
 * @property config 协调者配置
 */
class IndexCoordinatorActor(
    private val system: ActorSystem,
    private val taskProcessor: IndexTaskProcessor,
    private val config: IndexCoordinatorConfig = IndexCoordinatorConfig()
) : Actor {
    // 待处理任务队列
    private val pendingTasks = mutableListOf<IndexTask>()

    // 活动任务映射
    private val activeTasks = ConcurrentHashMap<String, PID>() // taskId -> workerId

    // 工作者列表
    private val workers = ConcurrentHashMap<String, PID>() // workerId -> PID

    // 工作者负载
    private val workerLoad = ConcurrentHashMap<String, Int>() // workerId -> taskCount

    // 任务统计
    private var completedTaskCount = 0
    private var failedTaskCount = 0

    // 事件流
    private val _events = MutableSharedFlow<IndexCoordinatorEvent>(extraBufferCapacity = 100)
    val events: SharedFlow<IndexCoordinatorEvent> = _events

    /**
     * 接收消息
     */
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is IndexCoordinatorMessage.SubmitTask -> submitTask(msg.task)
            is IndexCoordinatorMessage.SubmitBatch -> submitBatch(msg.tasks)
            is IndexCoordinatorMessage.RegisterWorker -> registerWorker(msg.worker)
            is IndexCoordinatorMessage.UnregisterWorker -> unregisterWorker(msg.worker)
            is IndexCoordinatorMessage.GetStatus -> respondWithStatus()
            is IndexTaskMessage.TaskCompleted -> handleTaskCompleted(msg)
            is actor.proto.Started -> handleStarted()
            else -> logger.warn { "未知消息类型: ${msg::class.simpleName}" }
        }
    }

    /**
     * 处理启动消息
     */
    private suspend fun Context.handleStarted() {
        logger.info { "索引协调者 Actor 已启动: ${self.id}" }

        // 创建初始工作者
        for (i in 0 until config.initialWorkerCount) {
            createWorker()
        }

        // 启动任务分配器
        startTaskAssigner()

        // 启动工作者状态检查器
        startWorkerStatusChecker()
    }

    /**
     * 提交任务
     *
     * @param task 索引任务
     */
    private suspend fun Context.submitTask(task: IndexTask) {
        // 检查是否达到最大待处理任务数量
        if (pendingTasks.size >= config.maxPendingTasks) {
            logger.warn { "待处理任务队列已满，拒绝任务: ${task.id}" }
            return
        }

        // 添加到待处理队列
        synchronized(pendingTasks) {
            pendingTasks.add(task)
        }

        // 发送事件
        _events.emit(IndexCoordinatorEvent.TaskSubmitted(task.id))

        logger.debug { "任务已提交: ${task.id}" }
    }

    /**
     * 批量提交任务
     *
     * @param tasks 任务列表
     */
    private suspend fun Context.submitBatch(tasks: List<IndexTask>) {
        // 检查是否达到最大待处理任务数量
        val availableSlots = config.maxPendingTasks - pendingTasks.size
        val tasksToSubmit = if (tasks.size > availableSlots) {
            logger.warn { "待处理任务队列空间不足，只接受部分任务: ${availableSlots}/${tasks.size}" }
            tasks.take(availableSlots)
        } else {
            tasks
        }

        // 添加到待处理队列
        synchronized(pendingTasks) {
            pendingTasks.addAll(tasksToSubmit)
        }

        // 发送事件
        tasksToSubmit.forEach { task ->
            _events.emit(IndexCoordinatorEvent.TaskSubmitted(task.id))
        }

        logger.debug { "批量提交任务: ${tasksToSubmit.size} 个任务" }
    }

    /**
     * 注册工作者
     *
     * @param worker 工作者 PID
     */
    private suspend fun Context.registerWorker(worker: PID) {
        val workerId = worker.id
        workers[workerId] = worker
        workerLoad[workerId] = 0

        // 监视工作者
        watch(worker)

        // 发送事件
        _events.emit(IndexCoordinatorEvent.WorkerRegistered(workerId))

        logger.info { "工作者已注册: $workerId" }
    }

    /**
     * 注销工作者
     *
     * @param worker 工作者 PID
     */
    private suspend fun Context.unregisterWorker(worker: PID) {
        val workerId = worker.id
        workers.remove(workerId)
        workerLoad.remove(workerId)

        // 取消监视工作者
        unwatch(worker)

        // 发送事件
        _events.emit(IndexCoordinatorEvent.WorkerUnregistered(workerId))

        logger.info { "工作者已注销: $workerId" }

        // 重新分配该工作者的任务
        reassignWorkerTasks(workerId)
    }

    /**
     * 重新分配工作者的任务
     *
     * @param workerId 工作者ID
     */
    private suspend fun Context.reassignWorkerTasks(workerId: String) {
        // 找出该工作者正在处理的任务
        val tasksToReassign = activeTasks.entries
            .filter { it.value.id == workerId }
            .map { it.key }

        // 从活动任务中移除
        tasksToReassign.forEach { taskId ->
            activeTasks.remove(taskId)
        }

        // 将任务重新添加到待处理队列
        val tasks = tasksToReassign.mapNotNull { taskId ->
            pendingTasks.find { it.id == taskId }
        }

        if (tasks.isNotEmpty()) {
            synchronized(pendingTasks) {
                pendingTasks.addAll(tasks)
            }

            logger.info { "重新分配工作者 $workerId 的 ${tasks.size} 个任务" }
        }
    }

    /**
     * 处理任务完成消息
     *
     * @param msg 任务完成消息
     */
    private suspend fun Context.handleTaskCompleted(msg: IndexTaskMessage.TaskCompleted) {
        val taskId = msg.taskId
        val workerId = sender?.id ?: return

        // 从活动任务中移除
        activeTasks.remove(taskId)

        // 减少工作者负载
        workerLoad.computeIfPresent(workerId) { _, load -> maxOf(0, load - 1) }

        if (msg.success) {
            // 任务成功完成
            completedTaskCount++

            // 发送事件
            _events.emit(IndexCoordinatorEvent.TaskCompleted(taskId, workerId))

            logger.debug { "任务完成: $taskId, 工作者: $workerId" }
        } else {
            // 任务失败
            failedTaskCount++

            // 发送事件
            _events.emit(IndexCoordinatorEvent.TaskFailed(taskId, workerId, msg.error ?: "未知错误"))

            logger.error { "任务失败: $taskId, 工作者: $workerId, 错误: ${msg.error}" }
        }
    }

    /**
     * 响应状态请求
     */
    private suspend fun Context.respondWithStatus() {
        val status = IndexCoordinatorMessage.StatusResponse(
            pendingTaskCount = pendingTasks.size,
            activeTaskCount = activeTasks.size,
            completedTaskCount = completedTaskCount,
            failedTaskCount = failedTaskCount,
            workerCount = workers.size
        )

        respond(status)
    }

    /**
     * 启动任务分配器
     */
    private fun Context.startTaskAssigner() {
        // 启动任务分配协程
        launch {
            while (true) {
                try {
                    assignTasks()
                } catch (e: Exception) {
                    logger.error(e) { "分配任务时出错" }
                }

                delay(config.taskAssignmentInterval)
            }
        }
    }

    /**
     * 分配任务
     */
    private suspend fun Context.assignTasks() {
        // 如果没有工作者或待处理任务，则跳过
        if (workers.isEmpty() || pendingTasks.isEmpty()) {
            return
        }

        // 获取可用工作者（负载最小的）
        val availableWorkers = workers.entries
            .filter { (workerId, _) -> workerLoad.getOrDefault(workerId, 0) < 5 } // 每个工作者最多处理 5 个任务
            .sortedBy { (workerId, _) -> workerLoad.getOrDefault(workerId, 0) }
            .map { it.key to it.value }

        if (availableWorkers.isEmpty()) {
            return
        }

        // 分配任务
        var workerIndex = 0
        val tasksToAssign = synchronized(pendingTasks) {
            val tasks = pendingTasks.take(availableWorkers.size * 2) // 每次最多分配工作者数量的 2 倍任务
            pendingTasks.removeAll(tasks)
            tasks
        }

        for (task in tasksToAssign) {
            val (workerId, worker) = availableWorkers[workerIndex]

            // 发送任务到工作者
            send(worker, IndexTaskMessage.ProcessTask(task))

            // 更新活动任务和工作者负载
            activeTasks[task.id] = worker
            workerLoad.compute(workerId) { _, load -> (load ?: 0) + 1 }

            // 发送事件
            _events.emit(IndexCoordinatorEvent.TaskAssigned(task.id, workerId))

            logger.debug { "分配任务: ${task.id} 到工作者: $workerId" }

            // 轮询下一个工作者
            workerIndex = (workerIndex + 1) % availableWorkers.size
        }
    }

    /**
     * 启动工作者状态检查器
     */
    private fun Context.startWorkerStatusChecker() {
        // 启动工作者状态检查协程
        launch {
            while (true) {
                try {
                    checkWorkersStatus()
                } catch (e: Exception) {
                    logger.error(e) { "检查工作者状态时出错" }
                }

                delay(config.workerStatusCheckInterval)
            }
        }
    }

    /**
     * 检查工作者状态
     */
    private suspend fun Context.checkWorkersStatus() {
        // 检查工作者数量，如果不足则创建新工作者
        val targetWorkerCount = config.initialWorkerCount
        val currentWorkerCount = workers.size

        if (currentWorkerCount < targetWorkerCount) {
            val workersToCreate = targetWorkerCount - currentWorkerCount

            for (i in 0 until workersToCreate) {
                createWorker()
            }

            logger.info { "创建 $workersToCreate 个新工作者，当前工作者数量: ${workers.size}" }
        }
    }

    /**
     * 创建工作者
     */
    private suspend fun Context.createWorker() {
        // 创建工作者 Actor
        val props = fromProducer {
            IndexTaskActor(
                taskProcessor = taskProcessor,
                config = IndexTaskActorConfig()
            )
        }

        val worker = spawnChild(props)

        // 注册工作者
        registerWorker(worker)
    }
*/
