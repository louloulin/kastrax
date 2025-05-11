package ai.kastrax.codebase

import ai.kastrax.codebase.filesystem.FileChangeEvent
import ai.kastrax.codebase.filesystem.FileFilter
import ai.kastrax.codebase.filesystem.FileFilterConfig
import ai.kastrax.codebase.filesystem.FileSystemMonitor
import ai.kastrax.codebase.filesystem.FileSystemMonitorConfig
import ai.kastrax.codebase.git.GitBranchChangeEvent
import ai.kastrax.codebase.git.GitBranchMonitor
import ai.kastrax.codebase.git.GitBranchMonitorConfig
import ai.kastrax.codebase.indexing.BatchProcessor
import ai.kastrax.codebase.indexing.BatchProcessorConfig
import ai.kastrax.codebase.indexing.IndexTask
import ai.kastrax.codebase.indexing.IndexTaskProcessor
import ai.kastrax.codebase.indexing.IndexTaskType
import ai.kastrax.codebase.indexing.IncrementalIndexer
import ai.kastrax.codebase.indexing.IncrementalIndexerConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 代码库索引管理器配置
 *
 * @property fileSystemMonitorConfig 文件系统监控器配置
 * @property gitBranchMonitorConfig Git 分支监控器配置
 * @property fileFilterConfig 文件过滤器配置
 * @property incrementalIndexerConfig 增量索引器配置
 * @property batchProcessorConfig 批处理器配置
 * @property enableGitMonitoring 是否启用 Git 监控
 * @property userId 用户ID（用于个性化索引）
 */
data class CodebaseIndexManagerConfig(
    val fileSystemMonitorConfig: FileSystemMonitorConfig = FileSystemMonitorConfig(),
    val gitBranchMonitorConfig: GitBranchMonitorConfig = GitBranchMonitorConfig(),
    val fileFilterConfig: FileFilterConfig = FileFilterConfig(),
    val incrementalIndexerConfig: IncrementalIndexerConfig = IncrementalIndexerConfig(),
    val batchProcessorConfig: BatchProcessorConfig = BatchProcessorConfig(),
    val enableGitMonitoring: Boolean = true,
    val userId: String? = null
)

/**
 * 代码库索引状态
 */
enum class CodebaseIndexStatus {
    INITIALIZING,
    INDEXING,
    READY,
    ERROR,
    STOPPED
}

/**
 * 代码库索引事件
 */
sealed class CodebaseIndexEvent {
    /**
     * 索引状态变更事件
     *
     * @property status 状态
     * @property message 消息
     */
    data class StatusChanged(
        val status: CodebaseIndexStatus,
        val message: String? = null
    ) : CodebaseIndexEvent()
    
    /**
     * 索引进度事件
     *
     * @property current 当前进度
     * @property total 总进度
     * @property message 消息
     */
    data class Progress(
        val current: Int,
        val total: Int,
        val message: String? = null
    ) : CodebaseIndexEvent()
    
    /**
     * 索引错误事件
     *
     * @property error 错误信息
     * @property fatal 是否致命错误
     */
    data class Error(
        val error: String,
        val fatal: Boolean = false
    ) : CodebaseIndexEvent()
}

/**
 * 代码库索引管理器
 *
 * 管理代码库索引的主要组件
 *
 * @property rootPath 根路径
 * @property config 配置
 * @property indexTaskProcessor 索引任务处理器
 */
class CodebaseIndexManager(
    private val rootPath: Path,
    private val config: CodebaseIndexManagerConfig = CodebaseIndexManagerConfig(),
    private val indexTaskProcessor: IndexTaskProcessor
) : AutoCloseable {
    private val logger = KotlinLogging.logger {}
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // 索引状态
    private var status = CodebaseIndexStatus.INITIALIZING
    
    // 是否正在运行
    private val isRunning = AtomicBoolean(false)
    
    // 文件系统监控器
    private val fileSystemMonitor = FileSystemMonitor(rootPath, config.fileSystemMonitorConfig)
    
    // Git 分支监控器
    private val gitBranchMonitor = if (config.enableGitMonitoring) {
        GitBranchMonitor(rootPath, config.gitBranchMonitorConfig)
    } else {
        null
    }
    
    // 文件过滤器
    private val fileFilter = FileFilter(config.fileFilterConfig)
    
    // 增量索引器
    private val incrementalIndexer = IncrementalIndexer(
        config = config.incrementalIndexerConfig,
        fileFilter = fileFilter
    )
    
    // 批处理器
    private val batchProcessor = BatchProcessor(
        config = config.batchProcessorConfig,
        indexTaskProcessor = indexTaskProcessor
    )
    
    // 索引事件流
    private val _indexEvents = MutableSharedFlow<CodebaseIndexEvent>(extraBufferCapacity = 100)
    val indexEvents: SharedFlow<CodebaseIndexEvent> = _indexEvents
    
    /**
     * 启动索引管理器
     */
    fun start() {
        if (isRunning.getAndSet(true)) {
            logger.warn { "索引管理器已经在运行" }
            return
        }
        
        logger.info { "启动代码库索引管理器: $rootPath" }
        
        // 更新状态
        updateStatus(CodebaseIndexStatus.INITIALIZING, "初始化索引管理器")
        
        try {
            // 启动批处理器
            batchProcessor.start(incrementalIndexer.indexTasks)
            
            // 监听文件系统变更
            scope.launch {
                fileSystemMonitor.fileChanges.collect { event ->
                    handleFileChange(event)
                }
            }
            
            // 监听 Git 分支变更
            if (gitBranchMonitor != null) {
                scope.launch {
                    gitBranchMonitor.branchChanges.collect { event ->
                        handleBranchChange(event)
                    }
                }
            }
            
            // 启动文件系统监控器
            fileSystemMonitor.start()
            
            // 启动 Git 分支监控器
            gitBranchMonitor?.start()
            
            // 请求完全重新索引
            incrementalIndexer.requestFullReindex(rootPath)
            
            // 更新状态
            updateStatus(CodebaseIndexStatus.INDEXING, "开始索引代码库")
        } catch (e: Exception) {
            logger.error(e) { "启动索引管理器时出错" }
            updateStatus(CodebaseIndexStatus.ERROR, "启动索引管理器时出错: ${e.message}")
            emitError("启动索引管理器时出错: ${e.message}", true)
        }
    }
    
    /**
     * 停止索引管理器
     */
    fun stop() {
        if (!isRunning.getAndSet(false)) {
            logger.warn { "索引管理器已经停止" }
            return
        }
        
        logger.info { "停止代码库索引管理器: $rootPath" }
        
        try {
            // 停止文件系统监控器
            fileSystemMonitor.stop()
            
            // 停止 Git 分支监控器
            gitBranchMonitor?.stop()
            
            // 处理剩余的批处理
            incrementalIndexer.processBatch()
            
            // 更新状态
            updateStatus(CodebaseIndexStatus.STOPPED, "索引管理器已停止")
        } catch (e: Exception) {
            logger.error(e) { "停止索引管理器时出错" }
            emitError("停止索引管理器时出错: ${e.message}", false)
        }
    }
    
    /**
     * 处理文件变更
     *
     * @param event 文件变更事件
     */
    private fun handleFileChange(event: FileChangeEvent) {
        if (!isRunning.get()) {
            return
        }
        
        try {
            // 处理文件变更
            incrementalIndexer.processFileChange(event)
        } catch (e: Exception) {
            logger.error(e) { "处理文件变更时出错: $event" }
            emitError("处理文件变更时出错: ${e.message}", false)
        }
    }
    
    /**
     * 处理分支变更
     *
     * @param event 分支变更事件
     */
    private fun handleBranchChange(event: GitBranchChangeEvent) {
        if (!isRunning.get()) {
            return
        }
        
        try {
            // 处理分支变更
            incrementalIndexer.processBranchChange(event)
            
            // 更新状态
            updateStatus(CodebaseIndexStatus.INDEXING, "分支变更: ${event.previousBranch} -> ${event.currentBranch}")
        } catch (e: Exception) {
            logger.error(e) { "处理分支变更时出错: $event" }
            emitError("处理分支变更时出错: ${e.message}", false)
        }
    }
    
    /**
     * 请求重新索引
     */
    fun requestReindex() {
        if (!isRunning.get()) {
            logger.warn { "索引管理器未运行，无法请求重新索引" }
            return
        }
        
        logger.info { "请求重新索引代码库: $rootPath" }
        
        try {
            // 请求完全重新索引
            incrementalIndexer.requestFullReindex(rootPath)
            
            // 更新状态
            updateStatus(CodebaseIndexStatus.INDEXING, "开始重新索引代码库")
        } catch (e: Exception) {
            logger.error(e) { "请求重新索引时出错" }
            emitError("请求重新索引时出错: ${e.message}", false)
        }
    }
    
    /**
     * 更新状态
     *
     * @param newStatus 新状态
     * @param message 消息
     */
    private fun updateStatus(newStatus: CodebaseIndexStatus, message: String? = null) {
        status = newStatus
        logger.info { "索引状态变更: $newStatus${message?.let { ", $it" } ?: ""}" }
        
        // 发送状态变更事件
        scope.launch {
            _indexEvents.emit(CodebaseIndexEvent.StatusChanged(newStatus, message))
        }
    }
    
    /**
     * 发送错误事件
     *
     * @param error 错误信息
     * @param fatal 是否致命错误
     */
    private fun emitError(error: String, fatal: Boolean) {
        logger.error { "索引错误: $error, 致命: $fatal" }
        
        // 发送错误事件
        scope.launch {
            _indexEvents.emit(CodebaseIndexEvent.Error(error, fatal))
        }
        
        // 如果是致命错误，则更新状态
        if (fatal) {
            updateStatus(CodebaseIndexStatus.ERROR, error)
        }
    }
    
    /**
     * 获取当前状态
     *
     * @return 当前状态
     */
    fun getStatus(): CodebaseIndexStatus {
        return status
    }
    
    /**
     * 关闭索引管理器
     */
    override fun close() {
        stop()
    }
}
