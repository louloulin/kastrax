package ai.kastrax.code.indexing

import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.codebase.filesystem.FileChangeEvent
import ai.kastrax.codebase.filesystem.FileChangeType
import ai.kastrax.codebase.indexing.IncrementalIndexTask
import ai.kastrax.codebase.indexing.IncrementalIndexerConfig
import ai.kastrax.codebase.indexing.IndexTaskType
import ai.kastrax.codebase.indexing.config.IncrementalIndexerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 增量索引器
 *
 * 处理文件变更事件，生成索引任务
 */
class IncrementalIndexer(
    private val config: IncrementalIndexerConfig = IncrementalIndexerConfig()
) : KastraXCodeBase(component = "INCREMENTAL_INDEXER") {

    // 使用 KastraXCodeBase 的 logger

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 索引任务流
    private val _indexTasks = MutableSharedFlow<List<IncrementalIndexTask>>(extraBufferCapacity = 100)
    val indexTasks: SharedFlow<List<IncrementalIndexTask>> = _indexTasks.asSharedFlow()

    // 最近处理的文件变更（用于去重）
    private val recentChanges = ConcurrentHashMap<Path, Long>()

    // 待处理的任务队列
    private val pendingTasks = ConcurrentHashMap<String, IncrementalIndexTask>()

    // 任务ID生成器
    private val taskIdGenerator = AtomicLong(0)

    /**
     * 处理文件变更事件
     *
     * @param event 文件变更事件
     */
    fun processFileChange(event: FileChangeEvent) {
        val path = event.path

        // 检查是否在去重窗口内
        val now = System.currentTimeMillis()
        val lastChange = recentChanges[path]
        if (lastChange != null && now - lastChange < config.deduplicationWindowMs) {
            logger.debug { "跳过重复的文件变更: $path" }
            return
        }

        // 更新最近变更时间
        recentChanges[path] = now

        // 创建索引任务
        val taskType = when (event.type) {
            FileChangeType.CREATE -> IndexTaskType.ADD
            FileChangeType.MODIFY -> IndexTaskType.UPDATE
            FileChangeType.DELETE -> IndexTaskType.DELETE
        }

        val taskId = "${taskType.name.lowercase()}_${path}_${taskIdGenerator.incrementAndGet()}"
        val task = IncrementalIndexTask(
            id = taskId,
            type = taskType,
            path = path,
            priority = calculatePriority(event),
            timestamp = now
        )

        // 添加到待处理队列
        pendingTasks[taskId] = task

        // 如果队列达到批处理大小，则处理批处理
        if (pendingTasks.size >= config.batchSize) {
            processBatch()
        }
    }

    /**
     * 请求完全重新索引
     *
     * @param rootPath 代码库根路径
     */
    fun requestFullReindex(rootPath: Path) {
        logger.info { "请求完全重新索引: $rootPath" }

        // 清空待处理队列
        pendingTasks.clear()

        // 创建重新索引任务
        val taskId = "reindex_${taskIdGenerator.incrementAndGet()}"
        val task = IncrementalIndexTask(
            id = taskId,
            type = IndexTaskType.REINDEX,
            path = rootPath,
            priority = 0, // 最高优先级
            timestamp = System.currentTimeMillis()
        )

        // 添加到待处理队列
        pendingTasks[taskId] = task

        // 立即处理批处理
        processBatch()
    }

    /**
     * 处理批处理
     */
    fun processBatch() {
        if (pendingTasks.isEmpty()) {
            return
        }

        // 获取待处理任务
        val tasks = pendingTasks.values.toList()
            .sortedBy { it.priority } // 按优先级排序
            .take(config.batchSize)

        // 从队列中移除这些任务
        tasks.forEach { pendingTasks.remove(it.id) }

        // 发送任务批处理
        scope.launch {
            _indexTasks.emit(tasks)
            logger.debug { "发送索引任务批处理，大小: ${tasks.size}" }
        }
    }

    /**
     * 清理过期的变更记录
     */
    fun cleanupExpiredChanges() {
        val now = System.currentTimeMillis()
        val expiredTime = now - config.deduplicationWindowMs * 10 // 10倍去重窗口时间

        // 移除过期的变更记录
        recentChanges.entries.removeIf { (_, timestamp) -> timestamp < expiredTime }
    }

    /**
     * 计算任务优先级
     *
     * @param event 文件变更事件
     * @return 优先级（数字越小优先级越高）
     */
    private fun calculatePriority(event: FileChangeEvent): Int {
        // 根据文件类型和变更类型计算优先级
        return when (event.type) {
            FileChangeType.DELETE -> 1 // 删除操作优先级高
            FileChangeType.CREATE -> 2 // 创建操作次之
            FileChangeType.MODIFY -> 3 // 修改操作优先级低
        }
    }
}
