package ai.kastrax.codebase.indexing.distributed

import ai.kastrax.codebase.indexing.IndexTask
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * 索引任务队列配置
 *
 * @property queueCapacity 队列容量
 * @property priorityLevels 优先级级别数量
 * @property taskTimeoutMs 任务超时时间（毫秒）
 */
data class IndexTaskQueueConfig(
    val queueCapacity: Int = 10000,
    val priorityLevels: Int = 5,
    val taskTimeoutMs: Long = 60000 // 1分钟
)

/**
 * 索引任务队列状态
 */
data class IndexTaskQueueStatus(
    val queuedTaskCount: Int,
    val processingTaskCount: Int,
    val completedTaskCount: Long,
    val failedTaskCount: Long,
    val priorityQueueSizes: Map<Int, Int>
)

/**
 * 索引任务队列事件
 */
sealed class IndexTaskQueueEvent {
    /**
     * 任务入队事件
     *
     * @property task 索引任务
     */
    data class TaskEnqueued(val task: IndexTask) : IndexTaskQueueEvent()
    
    /**
     * 任务出队事件
     *
     * @property task 索引任务
     * @property workerId 工作器ID
     */
    data class TaskDequeued(val task: IndexTask, val workerId: String) : IndexTaskQueueEvent()
    
    /**
     * 任务完成事件
     *
     * @property task 索引任务
     * @property workerId 工作器ID
     * @property durationMs 处理时间（毫秒）
     */
    data class TaskCompleted(val task: IndexTask, val workerId: String, val durationMs: Long) : IndexTaskQueueEvent()
    
    /**
     * 任务失败事件
     *
     * @property task 索引任务
     * @property workerId 工作器ID
     * @property error 错误信息
     */
    data class TaskFailed(val task: IndexTask, val workerId: String, val error: String) : IndexTaskQueueEvent()
    
    /**
     * 任务超时事件
     *
     * @property task 索引任务
     * @property workerId 工作器ID
     */
    data class TaskTimedOut(val task: IndexTask, val workerId: String) : IndexTaskQueueEvent()
}

/**
 * 索引任务队列
 *
 * 基于优先级的索引任务队列，支持任务分发和状态跟踪
 *
 * @property config 队列配置
 */
class IndexTaskQueue(private val config: IndexTaskQueueConfig = IndexTaskQueueConfig()) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // 优先级队列
    private val priorityQueues = Array(config.priorityLevels) {
        Channel<IndexTask>(config.queueCapacity / config.priorityLevels)
    }
    
    // 任务状态跟踪
    private val processingTasks = ConcurrentHashMap<String, Pair<IndexTask, Long>>() // taskId -> (task, startTimeMs)
    private val completedTaskCount = AtomicLong(0)
    private val failedTaskCount = AtomicLong(0)
    
    // 事件流
    private val _events = MutableSharedFlow<IndexTaskQueueEvent>(extraBufferCapacity = 100)
    val events: SharedFlow<IndexTaskQueueEvent> = _events
    
    /**
     * 入队任务
     *
     * @param task 索引任务
     * @return 是否成功入队
     */
    suspend fun enqueue(task: IndexTask): Boolean {
        val priority = normalizePriority(task.priority)
        
        return try {
            priorityQueues[priority].send(task)
            
            // 发送事件
            _events.emit(IndexTaskQueueEvent.TaskEnqueued(task))
            
            logger.debug { "任务入队: ${task.id}, 优先级: $priority" }
            true
        } catch (e: Exception) {
            logger.error(e) { "任务入队失败: ${task.id}" }
            false
        }
    }
    
    /**
     * 批量入队任务
     *
     * @param tasks 索引任务列表
     * @return 成功入队的任务数量
     */
    suspend fun enqueueBatch(tasks: List<IndexTask>): Int {
        var successCount = 0
        
        for (task in tasks) {
            if (enqueue(task)) {
                successCount++
            }
        }
        
        return successCount
    }
    
    /**
     * 获取任务流
     *
     * @return 任务流
     */
    fun getTaskFlow(): Flow<IndexTask> {
        // 创建一个合并所有优先级队列的通道
        val mergedChannel = Channel<IndexTask>(Channel.UNLIMITED)
        
        // 启动协程从各个优先级队列中获取任务
        scope.launch {
            while (true) {
                // 从高优先级到低优先级遍历队列
                for (priority in 0 until config.priorityLevels) {
                    // 尝试从当前优先级队列中获取任务
                    val task = priorityQueues[priority].tryReceive().getOrNull()
                    if (task != null) {
                        // 将任务发送到合并通道
                        mergedChannel.send(task)
                        break
                    }
                }
            }
        }
        
        return mergedChannel.receiveAsFlow()
    }
    
    /**
     * 标记任务开始处理
     *
     * @param task 索引任务
     * @param workerId 工作器ID
     */
    suspend fun markTaskStarted(task: IndexTask, workerId: String) {
        processingTasks[task.id] = Pair(task, System.currentTimeMillis())
        
        // 发送事件
        _events.emit(IndexTaskQueueEvent.TaskDequeued(task, workerId))
        
        logger.debug { "任务开始处理: ${task.id}, 工作器: $workerId" }
    }
    
    /**
     * 标记任务完成
     *
     * @param taskId 任务ID
     * @param workerId 工作器ID
     */
    suspend fun markTaskCompleted(taskId: String, workerId: String) {
        val taskInfo = processingTasks.remove(taskId)
        if (taskInfo != null) {
            val (task, startTimeMs) = taskInfo
            val durationMs = System.currentTimeMillis() - startTimeMs
            
            // 增加完成任务计数
            completedTaskCount.incrementAndGet()
            
            // 发送事件
            _events.emit(IndexTaskQueueEvent.TaskCompleted(task, workerId, durationMs))
            
            logger.debug { "任务完成: ${task.id}, 工作器: $workerId, 耗时: ${durationMs}ms" }
        } else {
            logger.warn { "尝试标记未知任务为完成: $taskId, 工作器: $workerId" }
        }
    }
    
    /**
     * 标记任务失败
     *
     * @param taskId 任务ID
     * @param workerId 工作器ID
     * @param error 错误信息
     */
    suspend fun markTaskFailed(taskId: String, workerId: String, error: String) {
        val taskInfo = processingTasks.remove(taskId)
        if (taskInfo != null) {
            val (task, _) = taskInfo
            
            // 增加失败任务计数
            failedTaskCount.incrementAndGet()
            
            // 发送事件
            _events.emit(IndexTaskQueueEvent.TaskFailed(task, workerId, error))
            
            logger.error { "任务失败: ${task.id}, 工作器: $workerId, 错误: $error" }
        } else {
            logger.warn { "尝试标记未知任务为失败: $taskId, 工作器: $workerId" }
        }
    }
    
    /**
     * 检查超时任务
     */
    suspend fun checkTimeoutTasks() {
        val now = System.currentTimeMillis()
        val timeoutTasks = processingTasks.entries
            .filter { (_, value) -> now - value.second > config.taskTimeoutMs }
            .map { it.key to it.value.first }
        
        for ((taskId, task) in timeoutTasks) {
            processingTasks.remove(taskId)
            
            // 增加失败任务计数
            failedTaskCount.incrementAndGet()
            
            // 发送事件
            _events.emit(IndexTaskQueueEvent.TaskTimedOut(task, "unknown"))
            
            logger.warn { "任务超时: ${task.id}" }
        }
    }
    
    /**
     * 获取队列状态
     *
     * @return 队列状态
     */
    fun getStatus(): IndexTaskQueueStatus {
        val priorityQueueSizes = (0 until config.priorityLevels).associateWith { 
            priorityQueues[it].tryReceive().getOrNull()?.let { 1 } ?: 0
        }
        
        return IndexTaskQueueStatus(
            queuedTaskCount = priorityQueueSizes.values.sum(),
            processingTaskCount = processingTasks.size,
            completedTaskCount = completedTaskCount.get(),
            failedTaskCount = failedTaskCount.get(),
            priorityQueueSizes = priorityQueueSizes
        )
    }
    
    /**
     * 规范化优先级
     *
     * @param priority 原始优先级
     * @return 规范化后的优先级（0 到 priorityLevels-1）
     */
    private fun normalizePriority(priority: Int): Int {
        return when {
            priority < 0 -> 0
            priority >= config.priorityLevels -> config.priorityLevels - 1
            else -> priority
        }
    }
}
