package ai.kastrax.codebase.indexing.distributed

import actor.proto.Actor
import actor.proto.Context
import actor.proto.PID
import ai.kastrax.codebase.indexing.IndexTask
import ai.kastrax.codebase.indexing.IndexTaskProcessor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/**
 * 索引任务处理消息
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
    
    /**
     * 获取状态消息
     */
    object GetStatus : IndexTaskMessage()
    
    /**
     * 状态响应消息
     *
     * @property activeTaskCount 活动任务数量
     * @property completedTaskCount 已完成任务数量
     * @property failedTaskCount 失败任务数量
     */
    data class StatusResponse(
        val activeTaskCount: Int,
        val completedTaskCount: Int,
        val failedTaskCount: Int
    ) : IndexTaskMessage()
}

/**
 * 索引任务 Actor 配置
 *
 * @property taskTimeout 任务超时时间
 * @property maxRetries 最大重试次数
 */
data class IndexTaskActorConfig(
    val taskTimeout: Duration = 60.seconds,
    val maxRetries: Int = 3
)

/**
 * 索引任务 Actor
 *
 * 处理索引任务的 Actor
 *
 * @property taskProcessor 任务处理器
 * @property config Actor 配置
 */
class IndexTaskActor(
    private val taskProcessor: IndexTaskProcessor,
    private val config: IndexTaskActorConfig = IndexTaskActorConfig()
) : Actor {
    // 任务状态跟踪
    private var activeTaskCount = 0
    private var completedTaskCount = 0
    private var failedTaskCount = 0
    
    // 任务重试计数
    private val taskRetries = mutableMapOf<String, Int>()
    
    // 任务协调者
    private var coordinator: PID? = null
    
    /**
     * 接收消息
     */
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is IndexTaskMessage.ProcessTask -> processTask(msg.task)
            is IndexTaskMessage.GetStatus -> respondWithStatus()
            is actor.proto.Started -> handleStarted()
            else -> logger.warn { "未知消息类型: ${msg::class.simpleName}" }
        }
    }
    
    /**
     * 处理启动消息
     */
    private fun Context.handleStarted() {
        logger.info { "索引任务 Actor 已启动: ${self.id}" }
    }
    
    /**
     * 处理任务
     *
     * @param task 索引任务
     */
    private suspend fun Context.processTask(task: IndexTask) {
        logger.debug { "处理任务: ${task.id}, 类型: ${task.type}, 路径: ${task.path}" }
        
        // 记录协调者
        if (coordinator == null) {
            coordinator = sender
        }
        
        // 增加活动任务计数
        activeTaskCount++
        
        try {
            // 使用超时处理任务
            withTimeout(config.taskTimeout) {
                taskProcessor.processTask(task)
            }
            
            // 任务成功完成
            activeTaskCount--
            completedTaskCount++
            
            // 清除重试计数
            taskRetries.remove(task.id)
            
            // 通知协调者任务完成
            coordinator?.let {
                send(it, IndexTaskMessage.TaskCompleted(task.id, true))
            }
            
            logger.debug { "任务完成: ${task.id}" }
        } catch (e: CancellationException) {
            // 任务超时
            handleTaskFailure(task, "任务超时: ${e.message}")
        } catch (e: Exception) {
            // 任务失败
            handleTaskFailure(task, e.message ?: "未知错误")
        }
    }
    
    /**
     * 处理任务失败
     *
     * @param task 索引任务
     * @param error 错误信息
     */
    private suspend fun Context.handleTaskFailure(task: IndexTask, error: String) {
        // 减少活动任务计数
        activeTaskCount--
        
        // 获取当前重试次数
        val retryCount = taskRetries.getOrDefault(task.id, 0)
        
        if (retryCount < config.maxRetries) {
            // 增加重试次数
            taskRetries[task.id] = retryCount + 1
            
            // 重新处理任务
            logger.warn { "任务重试: ${task.id}, 重试次数: ${retryCount + 1}, 错误: $error" }
            processTask(task)
        } else {
            // 任务失败
            failedTaskCount++
            
            // 清除重试计数
            taskRetries.remove(task.id)
            
            // 通知协调者任务失败
            coordinator?.let {
                send(it, IndexTaskMessage.TaskCompleted(task.id, false, error))
            }
            
            logger.error { "任务失败: ${task.id}, 已达到最大重试次数, 错误: $error" }
        }
    }
    
    /**
     * 响应状态请求
     */
    private suspend fun Context.respondWithStatus() {
        val status = IndexTaskMessage.StatusResponse(
            activeTaskCount = activeTaskCount,
            completedTaskCount = completedTaskCount,
            failedTaskCount = failedTaskCount
        )
        
        respond(status)
    }
}
