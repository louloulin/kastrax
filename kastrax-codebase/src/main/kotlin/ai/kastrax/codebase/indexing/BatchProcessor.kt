package ai.kastrax.codebase.indexing

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

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
 */
data class BatchProcessorConfig(
    val maxConcurrentBatches: Int = 3,
    val maxTasksPerBatch: Int = 1000,
    val maxRetries: Int = 3
)

/**
 * 批处理器
 *
 * 处理索引任务批处理
 *
 * @property config 配置
 * @property indexTaskProcessor 索引任务处理器
 */
class BatchProcessor(
    private val config: BatchProcessorConfig = BatchProcessorConfig(),
    private val indexTaskProcessor: IndexTaskProcessor
) {
    private val logger = KotlinLogging.logger {}
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

    /**
     * 启动批处理器
     *
     * @param indexTasks 索引任务流
     */
    fun start(indexTasks: SharedFlow<List<IncrementalIndexTask>>) {
        logger.info { "启动批处理器" }

        scope.launch {
            indexTasks.collect { tasks ->
                processTasks(tasks)
            }
        }
    }

    /**
     * 处理任务列表
     *
     * @param tasks 任务列表
     */
    private fun processTasks(tasks: List<IncrementalIndexTask>) {
        // 如果没有任务，则跳过
        if (tasks.isEmpty()) {
            return
        }

        // 检查是否达到最大并发批处理数
        if (activeBatchCount.get() >= config.maxConcurrentBatches) {
            logger.warn { "达到最大并发批处理数，等待中: ${tasks.size} 个任务" }
            return
        }

        // 将任务分成多个批次，每批不超过最大任务数
        val batches = tasks.chunked(config.maxTasksPerBatch)

        // 处理每个批次
        batches.forEach { batchTasks ->
            val batchId = "batch-${batchCounter.incrementAndGet()}"

            // 创建批处理结果
            val batchResult = BatchProcessingResult(
                batchId = batchId,
                status = BatchProcessingStatus.PENDING,
                tasksTotal = batchTasks.size
            )

            // 添加到活跃批处理
            activeBatches[batchId] = batchResult

            // 发送批处理结果
            scope.launch {
                _batchResults.emit(batchResult)
            }

            // 启动批处理
            startBatchProcessing(batchId, batchTasks)
        }
    }

    /**
     * 启动批处理
     *
     * @param batchId 批处理ID
     * @param tasks 任务列表
     */
    private fun startBatchProcessing(batchId: String, tasks: List<IncrementalIndexTask>) {
        // 增加活跃批处理数
        activeBatchCount.incrementAndGet()

        // 更新批处理状态
        updateBatchStatus(batchId, BatchProcessingStatus.PROCESSING)

        logger.info { "开始处理批次: $batchId, 任务数: ${tasks.size}" }

        // 在后台处理批次
        scope.launch {
            try {
                // 处理每个任务
                var completed = 0
                var failed = 0

                for (task in tasks) {
                    try {
                        // 处理任务
                        indexTaskProcessor.processTask(task)
                        completed++

                        // 更新批处理进度
                        updateBatchProgress(batchId, completed, failed)
                    } catch (e: Exception) {
                        logger.error(e) { "处理任务失败: ${task.id}" }
                        failed++

                        // 更新批处理进度
                        updateBatchProgress(batchId, completed, failed)
                    }
                }

                // 完成批处理
                completeBatch(batchId, completed, failed)
            } catch (e: Exception) {
                logger.error(e) { "批处理失败: $batchId" }
                failBatch(batchId, e.message ?: "未知错误")
            } finally {
                // 减少活跃批处理数
                activeBatchCount.decrementAndGet()
            }
        }
    }

    /**
     * 更新批处理状态
     *
     * @param batchId 批处理ID
     * @param status 状态
     */
    private fun updateBatchStatus(batchId: String, status: BatchProcessingStatus) {
        val current = activeBatches[batchId] ?: return
        val updated = current.copy(status = status)
        activeBatches[batchId] = updated

        // 发送更新
        scope.launch {
            _batchResults.emit(updated)
        }
    }

    /**
     * 更新批处理进度
     *
     * @param batchId 批处理ID
     * @param completed 已完成任务数
     * @param failed 失败任务数
     */
    private fun updateBatchProgress(batchId: String, completed: Int, failed: Int) {
        val current = activeBatches[batchId] ?: return
        val updated = current.copy(
            tasksCompleted = completed,
            tasksFailed = failed
        )
        activeBatches[batchId] = updated

        // 发送更新
        scope.launch {
            _batchResults.emit(updated)
        }
    }

    /**
     * 完成批处理
     *
     * @param batchId 批处理ID
     * @param completed 已完成任务数
     * @param failed 失败任务数
     */
    private fun completeBatch(batchId: String, completed: Int, failed: Int) {
        val current = activeBatches[batchId] ?: return
        val updated = current.copy(
            status = BatchProcessingStatus.COMPLETED,
            tasksCompleted = completed,
            tasksFailed = failed,
            endTime = System.currentTimeMillis()
        )
        activeBatches[batchId] = updated

        logger.info { "批处理完成: $batchId, 完成: $completed, 失败: $failed" }

        // 发送更新
        scope.launch {
            _batchResults.emit(updated)
        }

        // 从活跃批处理中移除
        activeBatches.remove(batchId)
    }

    /**
     * 批处理失败
     *
     * @param batchId 批处理ID
     * @param error 错误信息
     */
    private fun failBatch(batchId: String, error: String) {
        val current = activeBatches[batchId] ?: return
        val updated = current.copy(
            status = BatchProcessingStatus.FAILED,
            endTime = System.currentTimeMillis(),
            error = error
        )
        activeBatches[batchId] = updated

        logger.error { "批处理失败: $batchId, 错误: $error" }

        // 发送更新
        scope.launch {
            _batchResults.emit(updated)
        }

        // 从活跃批处理中移除
        activeBatches.remove(batchId)
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
}

/**
 * 索引任务处理器接口
 */
interface IndexTaskProcessor {
    /**
     * 处理索引任务
     *
     * @param task 索引任务
     */
    suspend fun processTask(task: IncrementalIndexTask)
}
