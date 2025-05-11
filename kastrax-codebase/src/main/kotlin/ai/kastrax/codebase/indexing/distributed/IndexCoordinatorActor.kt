package ai.kastrax.codebase.indexing.distributed

// TODO: 暂时注释掉Actor相关代码，等待kactor依赖问题解决

// 空实现以避免语法错误
class IndexCoordinatorActor

/*
import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.fromProducer
import ai.kastrax.codebase.indexing.IndexTask
import ai.kastrax.codebase.indexing.IndexTaskProcessor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/**
 * 索引协调者消息
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
