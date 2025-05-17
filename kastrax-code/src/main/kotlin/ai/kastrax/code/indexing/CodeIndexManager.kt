package ai.kastrax.code.indexing

import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.codebase.CodebaseIndexManager
import ai.kastrax.codebase.CodebaseIndexManagerConfig
import ai.kastrax.codebase.CodebaseIndexEvent
import ai.kastrax.codebase.CodebaseIndexStatus
import ai.kastrax.codebase.indexing.IndexTaskProcessor
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 代码索引管理器
 *
 * 管理代码库索引，提供索引状态和进度信息
 */
@Service(Service.Level.PROJECT)
class CodeIndexManager(private val project: Project) : KastraXCodeBase(component = "CODE_INDEX_MANAGER") {

    // 使用 KastraXCodeBase 的 logger

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isRunning = AtomicBoolean(false)

    // 索引事件流
    private val _indexEvents = MutableSharedFlow<IndexEvent>(extraBufferCapacity = 100)
    val indexEvents: SharedFlow<IndexEvent> = _indexEvents.asSharedFlow()

    // 索引状态
    private var status: IndexStatus = IndexStatus.IDLE
        set(value) {
            field = value
            scope.launch {
                _indexEvents.emit(IndexEvent.StatusChanged(value))
            }
        }

    // 索引进度
    private var progress: Float = 0f
        set(value) {
            field = value
            scope.launch {
                _indexEvents.emit(IndexEvent.ProgressUpdated(value))
            }
        }

    // 索引任务处理器
    private val indexTaskProcessor: IndexTaskProcessor = SimpleIndexTaskProcessor()

    // 代码库索引管理器
    private var codebaseIndexManager: CodebaseIndexManager? = null

    /**
     * 启动索引管理器
     *
     * @param rootPath 代码库根路径
     */
    fun start(rootPath: Path = Paths.get(project.basePath ?: "")) {
        if (isRunning.getAndSet(true)) {
            logger.warn { "索引管理器已经在运行" }
            return
        }

        logger.info { "启动代码索引管理器: $rootPath" }

        try {
            // 创建配置
            val config = CodebaseIndexManagerConfig(
                enableGitMonitoring = true
            )

            // 创建代码库索引管理器
            codebaseIndexManager = CodebaseIndexManager(rootPath, config, indexTaskProcessor)

            // 监听索引事件
            scope.launch {
                codebaseIndexManager?.indexEvents?.collect { event ->
                    handleCodebaseIndexEvent(event)
                }
            }

            // 启动代码库索引管理器
            codebaseIndexManager?.start()

            // 更新状态
            status = IndexStatus.INDEXING
        } catch (e: Exception) {
            logger.error(e) { "启动索引管理器时出错" }
            status = IndexStatus.ERROR
            scope.launch {
                _indexEvents.emit(IndexEvent.Error("启动索引管理器时出错: ${e.message}"))
            }
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

        logger.info { "停止代码索引管理器" }

        try {
            // 停止代码库索引管理器
            codebaseIndexManager?.stop()

            // 更新状态
            status = IndexStatus.IDLE
        } catch (e: Exception) {
            logger.error(e) { "停止索引管理器时出错" }
            scope.launch {
                _indexEvents.emit(IndexEvent.Error("停止索引管理器时出错: ${e.message}"))
            }
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

        logger.info { "请求重新索引代码库" }

        try {
            // 请求重新索引
            codebaseIndexManager?.requestReindex()

            // 更新状态
            status = IndexStatus.INDEXING
        } catch (e: Exception) {
            logger.error(e) { "请求重新索引时出错" }
            scope.launch {
                _indexEvents.emit(IndexEvent.Error("请求重新索引时出错: ${e.message}"))
            }
        }
    }

    /**
     * 获取当前索引状态
     *
     * @return 索引状态
     */
    fun getStatus(): IndexStatus {
        return status
    }

    /**
     * 获取当前索引进度
     *
     * @return 索引进度（0-1）
     */
    fun getProgress(): Float {
        return progress
    }

    /**
     * 处理代码库索引事件
     *
     * @param event 代码库索引事件
     */
    private fun handleCodebaseIndexEvent(event: CodebaseIndexEvent) {
        when (event) {
            is CodebaseIndexEvent.StatusChanged -> {
                // 转换状态
                status = when (event.status) {
                    CodebaseIndexStatus.INITIALIZING -> IndexStatus.IDLE
                    CodebaseIndexStatus.INDEXING -> IndexStatus.INDEXING
                    CodebaseIndexStatus.READY -> IndexStatus.READY
                    CodebaseIndexStatus.ERROR -> IndexStatus.ERROR
                    CodebaseIndexStatus.STOPPED -> IndexStatus.IDLE
                }

                logger.info { "索引状态变更: ${event.status}" }
            }
            is CodebaseIndexEvent.Progress -> {
                val progressValue = event.current.toFloat() / event.total.toFloat()
                progress = progressValue
                logger.debug { "索引进度更新: ${progressValue * 100}%" }
            }
            is CodebaseIndexEvent.Error -> {
                logger.error { "索引错误: ${event.error}" }
                scope.launch {
                    _indexEvents.emit(IndexEvent.Error(event.error))
                }
            }
            else -> {
                // 忽略其他事件
            }
        }
    }

    companion object {
        /**
         * 获取项目的代码索引管理器实例
         *
         * @param project 项目
         * @return 代码索引管理器实例
         */
        fun getInstance(project: Project): CodeIndexManager {
            return project.service<CodeIndexManager>()
        }
    }
}

/**
 * 索引状态
 */
enum class IndexStatus {
    /**
     * 空闲状态
     */
    IDLE,

    /**
     * 正在索引
     */
    INDEXING,

    /**
     * 索引就绪
     */
    READY,

    /**
     * 索引错误
     */
    ERROR
}

/**
 * 索引事件
 */
sealed class IndexEvent {
    /**
     * 状态变更事件
     *
     * @property status 新状态
     */
    data class StatusChanged(val status: IndexStatus) : IndexEvent()

    /**
     * 进度更新事件
     *
     * @property progress 进度（0-1）
     */
    data class ProgressUpdated(val progress: Float) : IndexEvent()

    /**
     * 错误事件
     *
     * @property message 错误消息
     */
    data class Error(val message: String) : IndexEvent()
}
