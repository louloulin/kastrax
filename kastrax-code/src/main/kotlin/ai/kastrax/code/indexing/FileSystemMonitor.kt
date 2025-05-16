package ai.kastrax.code.indexing

import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.codebase.filesystem.FileChangeEvent
import ai.kastrax.codebase.filesystem.FileChangeType
import ai.kastrax.codebase.filesystem.FileSystemMonitorConfig
import ai.kastrax.codebase.filesystem.FileSystemMonitor as CodebaseFileSystemMonitor
import ai.kastrax.codebase.filesystem.config.FileSystemMonitorConfig
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import io.github.oshai.kotlinlogging.KotlinLogging
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
 * 文件系统监控器
 *
 * 监控文件系统变更，发送文件变更事件
 */
class FileSystemMonitor(
    private val project: Project,
    private val config: FileSystemMonitorConfig = FileSystemMonitorConfig()
) : KastraXCodeBase(component = "FILE_SYSTEM_MONITOR") {
    
    override val logger = KotlinLogging.logger {}
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isRunning = AtomicBoolean(false)
    
    // 文件变更事件流
    private val _fileChanges = MutableSharedFlow<FileChangeEvent>(extraBufferCapacity = 100)
    val fileChanges: SharedFlow<FileChangeEvent> = _fileChanges.asSharedFlow()
    
    // 代码库文件系统监控器
    private var codebaseFileSystemMonitor: CodebaseFileSystemMonitor? = null
    
    // IntelliJ 文件监听器
    private val intellijFileListener = object : VirtualFileListener {
        override fun fileCreated(event: VirtualFileEvent) {
            handleFileEvent(event, FileChangeType.CREATE)
        }
        
        override fun fileDeleted(event: VirtualFileEvent) {
            handleFileEvent(event, FileChangeType.DELETE)
        }
        
        override fun contentsChanged(event: VirtualFileEvent) {
            handleFileEvent(event, FileChangeType.MODIFY)
        }
    }
    
    /**
     * 启动监控器
     *
     * @param rootPath 代码库根路径
     */
    fun start(rootPath: Path = Paths.get(project.basePath ?: "")) {
        if (isRunning.getAndSet(true)) {
            logger.warn { "文件系统监控器已经在运行" }
            return
        }
        
        logger.info { "启动文件系统监控器: $rootPath" }
        
        try {
            // 创建代码库文件系统监控器
            codebaseFileSystemMonitor = CodebaseFileSystemMonitor(rootPath, config)
            
            // 监听代码库文件系统监控器的文件变更事件
            scope.launch {
                codebaseFileSystemMonitor?.fileChanges?.collect { event ->
                    _fileChanges.emit(event)
                }
            }
            
            // 启动代码库文件系统监控器
            codebaseFileSystemMonitor?.start()
            
            // 注册 IntelliJ 文件监听器
            VirtualFileManager.getInstance().addVirtualFileListener(intellijFileListener)
        } catch (e: Exception) {
            logger.error(e) { "启动文件系统监控器时出错" }
            isRunning.set(false)
        }
    }
    
    /**
     * 停止监控器
     */
    fun stop() {
        if (!isRunning.getAndSet(false)) {
            logger.warn { "文件系统监控器已经停止" }
            return
        }
        
        logger.info { "停止文件系统监控器" }
        
        try {
            // 停止代码库文件系统监控器
            codebaseFileSystemMonitor?.stop()
            
            // 移除 IntelliJ 文件监听器
            VirtualFileManager.getInstance().removeVirtualFileListener(intellijFileListener)
        } catch (e: Exception) {
            logger.error(e) { "停止文件系统监控器时出错" }
        }
    }
    
    /**
     * 处理 IntelliJ 文件事件
     *
     * @param event 文件事件
     * @param type 变更类型
     */
    private fun handleFileEvent(event: VirtualFileEvent, type: FileChangeType) {
        if (!isRunning.get()) {
            return
        }
        
        val file = event.file
        val path = Paths.get(file.path)
        
        // 创建文件变更事件
        val fileChangeEvent = FileChangeEvent(path, type)
        
        // 发送文件变更事件
        scope.launch {
            _fileChanges.emit(fileChangeEvent)
        }
    }
}
