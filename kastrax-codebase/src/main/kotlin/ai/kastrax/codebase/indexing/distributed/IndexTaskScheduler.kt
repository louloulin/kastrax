package ai.kastrax.codebase.indexing.distributed

import ai.kastrax.codebase.indexing.IndexTask
import ai.kastrax.codebase.indexing.IndexTaskProcessor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/**
 * 索引任务调度器配置
 *
 * @property maxConcurrentTasks 最大并发任务数
 * @property workerCount 工作器数量
 * @property taskTimeoutCheckIntervalMs 任务超时检查间隔（毫秒）
 * @property taskRetryCount 任务重试次数
 * @property taskRetryDelayMs 任务重试延迟（毫秒）
 */
data class IndexTaskSchedulerConfig(
    val maxConcurrentTasks: Int = Runtime.getRuntime().availableProcessors(),
    val workerCount: Int = Runtime.getRuntime().availableProcessors(),
    val taskTimeoutCheckIntervalMs: Long = 10000, // 10秒
    val taskRetryCount: Int = 3,
    val taskRetryDelayMs: Long = 1000 // 1秒
)

/**
 * 索引任务调度器事件
 */
sealed class IndexTaskSchedulerEvent {
    /**
     * 调度器启动事件
     *
     * @property workerCount 工作器数量
     */
    data class SchedulerStarted(val workerCount: Int) : IndexTaskSchedulerEvent()
    
    /**
     * 调度器停止事件
     */
    object SchedulerStopped : IndexTaskSchedulerEvent()
    
    /**
     * 工作器启动事件
     *
     * @property workerId 工作器ID
     */
    data class WorkerStarted(val workerId: String) : IndexTaskSchedulerEvent()
    
    /**
     * 工作器停止事件
     *
     * @property workerId 工作器ID
     */
    data class WorkerStopped(val workerId: String) : IndexTaskSchedulerEvent()
    
    /**
     * 任务分配事件
     *
     * @property task 索引任务
     * @property workerId 工作器ID
     */
    data class TaskAssigned(val task: IndexTask, val workerId: String) : IndexTaskSchedulerEvent()
    
    /**
     * 任务重试事件
     *
     * @property task 索引任务
     * @property workerId 工作器ID
     * @property retryCount 重试次数
     * @property error 错误信息
     */
    data class TaskRetried(val task: IndexTask, val workerId: String, val retryCount: Int, val error: String) : IndexTaskSchedulerEvent()
}

/**
 * 索引任务调度器
 *
 * 负责调度索引任务，将任务分配给工作器处理
 *
 * @property taskQueue 任务队列
 * @property taskProcessor 任务处理器
 * @property config 调度器配置
 */
class IndexTaskScheduler(
    private val taskQueue: IndexTaskQueue,
    private val taskProcessor: IndexTaskProcessor,
    private val config: IndexTaskSchedulerConfig = IndexTaskSchedulerConfig()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // 运行状态
    private val isRunning = AtomicBoolean(false)
    
    // 工作器状态
    private val workers = ConcurrentHashMap<String, Boolean>() // workerId -> isActive
    
    // 任务重试计数
    private val taskRetries = ConcurrentHashMap<String, Int>() // taskId -> retryCount
    
    // 事件流
    private val _events = MutableSharedFlow<IndexTaskSchedulerEvent>(extraBufferCapacity = 100)
    val events: SharedFlow<IndexTaskSchedulerEvent> = _events
    
    /**
     * 启动调度器
     */
    fun start() {
        if (isRunning.getAndSet(true)) {
            logger.warn { "调度器已经在运行" }
            return
        }
        
        logger.info { "启动索引任务调度器，工作器数量: ${config.workerCount}" }
        
        // 发送事件
        scope.launch {
            _events.emit(IndexTaskSchedulerEvent.SchedulerStarted(config.workerCount))
        }
        
        // 启动工作器
        for (i in 0 until config.workerCount) {
            startWorker()
        }
        
        // 启动任务超时检查
        startTimeoutChecker()
    }
    
    /**
     * 停止调度器
     */
    fun stop() {
        if (!isRunning.getAndSet(false)) {
            logger.warn { "调度器已经停止" }
            return
        }
        
        logger.info { "停止索引任务调度器" }
        
        // 标记所有工作器为非活动
        workers.keys.forEach { workers[it] = false }
        
        // 发送事件
        scope.launch {
            _events.emit(IndexTaskSchedulerEvent.SchedulerStopped)
        }
    }
    
    /**
     * 启动工作器
     */
    private fun startWorker() {
        val workerId = UUID.randomUUID().toString()
        workers[workerId] = true
        
        logger.debug { "启动工作器: $workerId" }
        
        // 发送事件
        scope.launch {
            _events.emit(IndexTaskSchedulerEvent.WorkerStarted(workerId))
        }
        
        // 启动工作器协程
        scope.launch {
            try {
                // 获取任务流
                val taskFlow = taskQueue.getTaskFlow()
                
                // 处理任务
                taskFlow.collect { task ->
                    if (!isRunning.get() || !workers[workerId]!!) {
                        return@collect
                    }
                    
                    // 标记任务开始处理
                    taskQueue.markTaskStarted(task, workerId)
                    
                    // 发送事件
                    _events.emit(IndexTaskSchedulerEvent.TaskAssigned(task, workerId))
                    
                    try {
                        // 处理任务
                        taskProcessor.processTask(task)
                        
                        // 标记任务完成
                        taskQueue.markTaskCompleted(task.id, workerId)
                        
                        // 清除重试计数
                        taskRetries.remove(task.id)
                    } catch (e: Exception) {
                        // 获取当前重试次数
                        val retryCount = taskRetries.getOrDefault(task.id, 0)
                        
                        if (retryCount < config.taskRetryCount) {
                            // 增加重试次数
                            taskRetries[task.id] = retryCount + 1
                            
                            // 发送事件
                            _events.emit(IndexTaskSchedulerEvent.TaskRetried(task, workerId, retryCount + 1, e.message ?: "未知错误"))
                            
                            // 延迟后重新入队
                            delay(config.taskRetryDelayMs.milliseconds)
                            taskQueue.enqueue(task)
                            
                            logger.warn { "任务重试: ${task.id}, 重试次数: ${retryCount + 1}, 错误: ${e.message}" }
                        } else {
                            // 标记任务失败
                            taskQueue.markTaskFailed(task.id, workerId, e.message ?: "未知错误")
                            
                            // 清除重试计数
                            taskRetries.remove(task.id)
                            
                            logger.error(e) { "任务失败: ${task.id}, 已达到最大重试次数" }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "工作器异常: $workerId" }
            } finally {
                // 标记工作器为非活动
                workers[workerId] = false
                
                // 发送事件
                scope.launch {
                    _events.emit(IndexTaskSchedulerEvent.WorkerStopped(workerId))
                }
                
                logger.debug { "工作器停止: $workerId" }
                
                // 如果调度器仍在运行，启动新的工作器
                if (isRunning.get()) {
                    startWorker()
                }
            }
        }
    }
    
    /**
     * 启动任务超时检查
     */
    private fun startTimeoutChecker() {
        scope.launch {
            while (isActive && isRunning.get()) {
                try {
                    // 检查超时任务
                    taskQueue.checkTimeoutTasks()
                } catch (e: Exception) {
                    logger.error(e) { "检查超时任务时出错" }
                }
                
                // 延迟
                delay(config.taskTimeoutCheckIntervalMs.milliseconds)
            }
        }
    }
    
    /**
     * 获取活动工作器数量
     *
     * @return 活动工作器数量
     */
    fun getActiveWorkerCount(): Int {
        return workers.values.count { it }
    }
    
    /**
     * 获取任务队列状态
     *
     * @return 任务队列状态
     */
    fun getQueueStatus(): IndexTaskQueueStatus {
        return taskQueue.getStatus()
    }
}
