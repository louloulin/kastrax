package ai.kastrax.codebase.indexing

import ai.kastrax.codebase.filesystem.FileChangeEvent
import ai.kastrax.codebase.filesystem.FileChangeType
import ai.kastrax.codebase.filesystem.FileFilter
import ai.kastrax.codebase.filesystem.FileFilterConfig
import ai.kastrax.codebase.git.GitBranchChangeEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 索引任务类型
 */
enum class IndexTaskType {
    ADD,
    UPDATE,
    DELETE,
    BRANCH_CHANGE,
    FULL_REINDEX
}

/**
 * 索引任务
 *
 * @property id 任务ID
 * @property type 任务类型
 * @property path 文件路径
 * @property timestamp 时间戳
 * @property priority 优先级（越小越高）
 * @property metadata 元数据
 */
data class IndexTask(
    val id: String = generateTaskId(),
    val type: IndexTaskType,
    val path: Path,
    val timestamp: Long = System.currentTimeMillis(),
    val priority: Int = getPriorityForType(type),
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        private val taskCounter = AtomicLong(0)
        
        /**
         * 生成任务ID
         */
        private fun generateTaskId(): String {
            return "task-${taskCounter.incrementAndGet()}"
        }
        
        /**
         * 根据任务类型获取优先级
         */
        private fun getPriorityForType(type: IndexTaskType): Int {
            return when (type) {
                IndexTaskType.BRANCH_CHANGE -> 0 // 最高优先级
                IndexTaskType.DELETE -> 1
                IndexTaskType.UPDATE -> 2
                IndexTaskType.ADD -> 3
                IndexTaskType.FULL_REINDEX -> 4 // 最低优先级
            }
        }
    }
}

/**
 * 增量索引器配置
 *
 * @property batchSize 批处理大小
 * @property maxQueueSize 最大队列大小
 * @property deduplicationWindowMs 去重窗口（毫秒）
 */
data class IncrementalIndexerConfig(
    val batchSize: Int = 100,
    val maxQueueSize: Int = 10000,
    val deduplicationWindowMs: Long = 1000 // 1秒
)

/**
 * 增量索引器
 *
 * 处理文件变更事件，生成索引任务
 *
 * @property config 配置
 * @property fileFilter 文件过滤器
 */
class IncrementalIndexer(
    private val config: IncrementalIndexerConfig = IncrementalIndexerConfig(),
    private val fileFilter: FileFilter = FileFilter(FileFilterConfig())
) {
    private val logger = KotlinLogging.logger {}
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // 索引任务流
    private val _indexTasks = MutableSharedFlow<List<IndexTask>>(extraBufferCapacity = 100)
    val indexTasks: SharedFlow<List<IndexTask>> = _indexTasks
    
    // 最近处理的文件变更（用于去重）
    private val recentChanges = ConcurrentHashMap<Path, Long>()
    
    // 待处理的任务队列
    private val pendingTasks = ConcurrentHashMap<String, IndexTask>()
    
    /**
     * 处理文件变更事件
     *
     * @param event 文件变更事件
     */
    fun processFileChange(event: FileChangeEvent) {
        val path = event.path
        
        // 检查是否应该索引该文件
        if (event.type != FileChangeType.DELETE && !fileFilter.shouldIndexFile(path)) {
            logger.debug { "跳过不需要索引的文件: $path" }
            return
        }
        
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
        
        val task = IndexTask(
            type = taskType,
            path = path,
            timestamp = event.timestamp
        )
        
        // 添加到待处理队列
        addTaskToQueue(task)
    }
    
    /**
     * 处理分支变更事件
     *
     * @param event 分支变更事件
     */
    fun processBranchChange(event: GitBranchChangeEvent) {
        logger.info { "处理分支变更: ${event.previousBranch} -> ${event.currentBranch}" }
        
        // 创建分支变更任务
        val task = IndexTask(
            type = IndexTaskType.BRANCH_CHANGE,
            path = event.repositoryPath,
            timestamp = event.timestamp,
            metadata = mapOf(
                "previousBranch" to event.previousBranch,
                "currentBranch" to event.currentBranch
            )
        )
        
        // 添加到待处理队列
        addTaskToQueue(task)
    }
    
    /**
     * 请求完全重新索引
     *
     * @param rootPath 根路径
     */
    fun requestFullReindex(rootPath: Path) {
        logger.info { "请求完全重新索引: $rootPath" }
        
        // 创建完全重新索引任务
        val task = IndexTask(
            type = IndexTaskType.FULL_REINDEX,
            path = rootPath
        )
        
        // 添加到待处理队列
        addTaskToQueue(task)
    }
    
    /**
     * 添加任务到队列
     *
     * @param task 索引任务
     */
    private fun addTaskToQueue(task: IndexTask) {
        // 如果队列已满，则移除最低优先级的任务
        if (pendingTasks.size >= config.maxQueueSize) {
            val lowestPriorityTask = pendingTasks.values
                .maxByOrNull { it.priority }
            
            if (lowestPriorityTask != null && lowestPriorityTask.priority > task.priority) {
                pendingTasks.remove(lowestPriorityTask.id)
                logger.debug { "队列已满，移除低优先级任务: ${lowestPriorityTask.id}" }
            } else {
                logger.warn { "队列已满，无法添加新任务: ${task.id}" }
                return
            }
        }
        
        // 添加任务到队列
        pendingTasks[task.id] = task
        logger.debug { "添加任务到队列: ${task.id}, 类型: ${task.type}, 路径: ${task.path}" }
        
        // 如果队列中的任务数量达到批处理大小，则发送批处理
        if (pendingTasks.size >= config.batchSize) {
            processBatch()
        }
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
}
