package ai.kastrax.codebase.indexing.distributed

import actor.proto.Actor
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

// 索引工作器配置已移至 DistributedIndexSystemConfig.kt

/**
 * 索引工作器 Actor
 *
 * 负责执行索引任务并向协调器报告状态
 *
 * @property workerId 工作器ID
 * @property coordinatorPid 协调器PID
 * @property indexProcessor 索引处理器
 * @property config 工作器配置
 */
class IndexWorkerActor(
    private val workerId: String = UUID.randomUUID().toString(),
    private val coordinatorPid: PID,
    private val indexProcessor: IndexProcessor,
    private val config: IndexWorkerConfig = IndexWorkerConfig()
) : Actor {
    // 任务状态跟踪
    private val activeTasks = ConcurrentHashMap<String, IndexTask>()
    private val activeTaskCount = AtomicInteger(0)

    // 统计信息
    private var completedTaskCount = 0
    private var failedTaskCount = 0

    // 删除重复的 receive 方法，使用下面的实现

    /**
     * 处理任务消息
     *
     * @param task 索引任务
     */
    private suspend fun Context.handleProcessTask(task: IndexTask) {
        logger.debug("接收到任务: ${task.id}, 类型: ${task.type}, 路径: ${task.path}")

        // 检查是否有可用容量
        if (activeTaskCount.get() >= config.capacity) {
            logger.warning("工作器容量已满，拒绝任务: ${task.id}")

            // 向协调器报告任务失败
            send(coordinatorPid, IndexCoordinatorMessage.TaskStatusUpdate(
                taskId = task.id,
                status = IndexTaskStatus.FAILED,
                error = "工作器容量已满"
            ))

            return
        }

        // 添加到活动任务
        activeTasks[task.id] = task
        activeTaskCount.incrementAndGet()

        // 向协调器报告任务开始
        send(coordinatorPid, IndexCoordinatorMessage.TaskStatusUpdate(
            taskId = task.id,
            status = IndexTaskStatus.RUNNING
        ))

        // 异步处理任务
        val taskProps = fromProducer {
            object : Actor {
                override suspend fun Context.receive(msg: Any) {
                    processTask(task)
                }
            }
        }
        spawnNamed(taskProps, "task-${task.id}")
    }

    /**
     * 处理获取状态消息
     */
    private suspend fun Context.handleGetStatus() {
        val status = IndexWorkerMessage.StatusResponse(
            activeTaskCount = activeTaskCount.get(),
            availableSlots = config.capacity - activeTaskCount.get(),
            completedTaskCount = completedTaskCount,
            failedTaskCount = failedTaskCount
        )

        respond(status)
    }

    /**
     * 处理任务
     *
     * @param task 索引任务
     */
    private suspend fun Context.processTask(task: IndexTask) {
        try {
            // 使用超时机制执行任务
            val result = withTimeout(config.taskTimeout.inWholeMilliseconds) {
                withContext(Dispatchers.Default) {
                    indexProcessor.processTask(task)
                }
            }

            // 任务成功完成
            logger.info("任务完成: ${task.id}")

            // 更新统计信息
            completedTaskCount++

            // 从活动任务中移除
            activeTasks.remove(task.id)
            activeTaskCount.decrementAndGet()

            // 向协调器报告任务完成
            send(coordinatorPid, IndexCoordinatorMessage.TaskStatusUpdate(
                taskId = task.id,
                status = IndexTaskStatus.COMPLETED
            ))
        } catch (e: TimeoutCancellationException) {
            // 任务超时
            logger.error("任务超时: ${task.id}")

            // 更新统计信息
            failedTaskCount++

            // 从活动任务中移除
            activeTasks.remove(task.id)
            activeTaskCount.decrementAndGet()

            // 向协调器报告任务失败
            send(coordinatorPid, IndexCoordinatorMessage.TaskStatusUpdate(
                taskId = task.id,
                status = IndexTaskStatus.FAILED,
                error = "任务超时"
            ))
        } catch (e: Exception) {
            // 任务执行出错
            logger.error("任务执行出错: ${task.id}", e)

            // 更新统计信息
            failedTaskCount++

            // 从活动任务中移除
            activeTasks.remove(task.id)
            activeTaskCount.decrementAndGet()

            // 向协调器报告任务失败
            send(coordinatorPid, IndexCoordinatorMessage.TaskStatusUpdate(
                taskId = task.id,
                status = IndexTaskStatus.FAILED,
                error = e.message ?: "未知错误"
            ))
        }
    }

    /**
     * 发送心跳
     */
    private suspend fun Context.sendHeartbeat() {
        send(coordinatorPid, IndexCoordinatorMessage.WorkerHeartbeat(
            workerId = workerId,
            activeTaskCount = activeTaskCount.get(),
            availableSlots = config.capacity - activeTaskCount.get()
        ))
    }

    /**
     * 启动定时任务
     */
    private suspend fun Context.startPeriodicTasks() {
        // 定期发送心跳
        val heartbeatProps = fromProducer {
            object : Actor {
                override suspend fun Context.receive(msg: Any) {
                    sendHeartbeat()
                    delay(config.heartbeatInterval.inWholeMilliseconds)
                    send(self, "continue")
                }
            }
        }
        val heartbeatSender = spawnNamed(heartbeatProps, "heartbeat-sender")
        send(heartbeatSender, "continue")
    }

    /**
     * 处理启动消息
     */
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is IndexWorkerMessage.ProcessTask -> handleProcessTask(msg.task)
            is IndexWorkerMessage.GetStatus -> handleGetStatus()
            "started" -> {
                logger.info("索引工作器已启动: $workerId")

                // 向协调器注册
                send(coordinatorPid, IndexCoordinatorMessage.RegisterWorker(
                    workerId = workerId,
                    capacity = config.capacity,
                    pid = self
                ))

                // 启动定时任务
                startPeriodicTasks()
            }
            else -> logger.warning("未知消息类型: ${msg::class.simpleName}")
        }
    }

    companion object {
        /**
         * 创建 Props
         *
         * @param workerId 工作器ID
         * @param coordinatorPid 协调器PID
         * @param indexProcessor 索引处理器
         * @param config 工作器配置
         * @return Props
         */
        fun props(
            workerId: String = UUID.randomUUID().toString(),
            coordinatorPid: PID,
            indexProcessor: IndexProcessor,
            config: IndexWorkerConfig = IndexWorkerConfig()
        ): Props {
            return fromProducer { IndexWorkerActor(workerId, coordinatorPid, indexProcessor, config) }
        }
    }
}
