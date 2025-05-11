package ai.kastrax.codebase.filesystem

import io.github.oshai.kotlinlogging.KotlinLogging
import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryChangeListener
import io.methvin.watcher.DirectoryWatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/**
 * 文件系统变更事件类型
 */
enum class FileChangeType {
    CREATE,
    MODIFY,
    DELETE
}

/**
 * 文件系统变更事件
 *
 * @property path 文件路径
 * @property type 变更类型
 * @property timestamp 时间戳
 */
data class FileChangeEvent(
    val path: Path,
    val type: FileChangeType,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 文件系统监控器配置
 *
 * @property excludePatterns 排除的文件模式（正则表达式）
 * @property excludeExtensions 排除的文件扩展名
 * @property excludeDirectories 排除的目录名
 * @property pollIntervalMs 轮询间隔（毫秒）
 */
data class FileSystemMonitorConfig(
    val excludePatterns: Set<Regex> = setOf(
        Regex("\\.git/.*"),
        Regex("\\.idea/.*"),
        Regex("build/.*"),
        Regex("target/.*"),
        Regex("node_modules/.*"),
        Regex("\\.gradle/.*")
    ),
    val excludeExtensions: Set<String> = setOf(
        "class", "jar", "war", "zip", "tar", "gz", "rar",
        "jpg", "jpeg", "png", "gif", "bmp", "ico", "svg",
        "mp3", "mp4", "avi", "mov", "wmv", "flv", "wav",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
    ),
    val excludeDirectories: Set<String> = setOf(
        ".git", ".idea", "build", "target", "node_modules", ".gradle"
    ),
    val pollIntervalMs: Long = 1000
)

/**
 * 文件系统监控器
 *
 * 监控文件系统变更，并发出变更事件
 *
 * @property rootPath 根路径
 * @property config 配置
 */
class FileSystemMonitor(
    private val rootPath: Path,
    private val config: FileSystemMonitorConfig = FileSystemMonitorConfig()
) : AutoCloseable {
    private val logger = KotlinLogging.logger {}
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 文件变更事件流
    private val _fileChanges = MutableSharedFlow<FileChangeEvent>(extraBufferCapacity = 100)
    val fileChanges: SharedFlow<FileChangeEvent> = _fileChanges
    
    // 活跃的监控器
    private val activeWatchers = ConcurrentHashMap<Path, DirectoryWatcher>()
    
    // 目录变更监听器
    private val directoryChangeListener = object : DirectoryChangeListener {
        override fun onEvent(event: DirectoryChangeEvent) {
            val path = event.path()
            
            // 检查是否应该排除该文件
            if (shouldExcludeFile(path)) {
                return
            }
            
            // 处理目录变更
            if (path.isDirectory()) {
                when (event.eventType()) {
                    DirectoryChangeEvent.EventType.CREATE -> {
                        logger.debug { "目录创建: $path" }
                        watchDirectory(path)
                    }
                    DirectoryChangeEvent.EventType.DELETE -> {
                        logger.debug { "目录删除: $path" }
                        unwatchDirectory(path)
                    }
                    else -> {
                        // 目录修改，不需要特殊处理
                    }
                }
                return
            }
            
            // 处理文件变更
            if (path.isRegularFile()) {
                val changeType = when (event.eventType()) {
                    DirectoryChangeEvent.EventType.CREATE -> FileChangeType.CREATE
                    DirectoryChangeEvent.EventType.MODIFY -> FileChangeType.MODIFY
                    DirectoryChangeEvent.EventType.DELETE -> FileChangeType.DELETE
                    else -> return // 忽略未知事件类型
                }
                
                val changeEvent = FileChangeEvent(path, changeType)
                scope.launch {
                    _fileChanges.emit(changeEvent)
                    logger.debug { "文件变更: $changeEvent" }
                }
            }
        }
    }
    
    /**
     * 启动监控
     */
    fun start() {
        logger.info { "开始监控文件系统: $rootPath" }
        watchDirectory(rootPath)
    }
    
    /**
     * 停止监控
     */
    fun stop() {
        logger.info { "停止监控文件系统: $rootPath" }
        activeWatchers.values.forEach { it.close() }
        activeWatchers.clear()
    }
    
    /**
     * 监控目录
     */
    private fun watchDirectory(directory: Path) {
        if (!directory.isDirectory() || shouldExcludeDirectory(directory)) {
            return
        }
        
        try {
            // 如果已经在监控，则跳过
            if (activeWatchers.containsKey(directory)) {
                return
            }
            
            // 创建并启动目录监控器
            val watcher = DirectoryWatcher.builder()
                .path(directory)
                .listener(directoryChangeListener)
                .build()
            
            activeWatchers[directory] = watcher
            
            // 在后台线程中启动监控
            scope.launch {
                try {
                    watcher.watch()
                } catch (e: Exception) {
                    logger.error(e) { "监控目录时出错: $directory" }
                }
            }
            
            logger.debug { "开始监控目录: $directory" }
            
            // 递归监控子目录
            directory.toFile().listFiles()
                ?.filter { it.isDirectory }
                ?.map { it.toPath() }
                ?.forEach { watchDirectory(it) }
        } catch (e: Exception) {
            logger.error(e) { "设置目录监控时出错: $directory" }
        }
    }
    
    /**
     * 取消监控目录
     */
    private fun unwatchDirectory(directory: Path) {
        activeWatchers[directory]?.close()
        activeWatchers.remove(directory)
        logger.debug { "停止监控目录: $directory" }
    }
    
    /**
     * 检查是否应该排除文件
     */
    private fun shouldExcludeFile(path: Path): Boolean {
        // 检查文件扩展名
        val extension = path.extension.lowercase()
        if (extension in config.excludeExtensions) {
            return true
        }
        
        // 检查排除模式
        val pathString = path.toString().replace('\\', '/')
        if (config.excludePatterns.any { it.matches(pathString) }) {
            return true
        }
        
        return false
    }
    
    /**
     * 检查是否应该排除目录
     */
    private fun shouldExcludeDirectory(directory: Path): Boolean {
        val dirName = directory.name
        if (dirName in config.excludeDirectories) {
            return true
        }
        
        // 检查排除模式
        val pathString = directory.toString().replace('\\', '/')
        if (config.excludePatterns.any { it.matches(pathString) }) {
            return true
        }
        
        return false
    }
    
    /**
     * 关闭监控器
     */
    override fun close() {
        stop()
    }
}
