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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.Path
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.time.Duration.Companion.milliseconds

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
 * @property eventBufferCapacity 事件缓冲区容量
 * @property eventThrottleMs 事件节流时间（毫秒）
 * @property watcherThreads 监控线程数
 * @property enableRecursiveWatching 是否启用递归监控
 * @property enableFastStartup 是否启用快速启动
 */
/**
 * 文件系统监控器配置
 *
 * @property excludePatterns 排除的文件模式（正则表达式）
 * @property excludeExtensions 排除的文件扩展名
 * @property excludeDirectories 排除的目录名
 * @property pollIntervalMs 轮询间隔（毫秒）
 * @property eventBufferCapacity 事件缓冲区容量
 * @property eventThrottleMs 事件节流时间（毫秒）
 * @property watcherThreads 监控线程数
 * @property enableRecursiveWatching 是否启用递归监控
 * @property enableFastStartup 是否启用快速启动
 * @property batchProcessingEnabled 是否启用批处理
 * @property batchProcessingIntervalMs 批处理间隔（毫秒）
 * @property batchSize 批处理大小
 * @property detectRefactoring 是否检测大规模重构
 * @property refactoringThreshold 重构检测阈值（短时间内变更的文件数）
 * @property refactoringTimeWindowMs 重构检测时间窗口（毫秒）
 * @property prioritizeActiveFiles 是否优先处理活跃文件
 * @property activeFileTimeWindowMs 活跃文件时间窗口（毫秒）
 * @property maxConcurrentWatchers 最大并发监控器数量
 * @property watcherRestartDelayMs 监控器重启延迟（毫秒）
 * @property enableWatcherHealthCheck 是否启用监控器健康检查
 * @property watcherHealthCheckIntervalMs 监控器健康检查间隔（毫秒）
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
    val pollIntervalMs: Long = 100, // 降低轮询间隔以提高实时性（Augment级别）
    val eventBufferCapacity: Int = 10000, // 增加事件缓冲区容量，支持大型代码库
    val eventThrottleMs: Long = 20, // 降低事件节流时间，提高实时性
    val watcherThreads: Int = Runtime.getRuntime().availableProcessors(), // 使用所有可用处理器
    val enableRecursiveWatching: Boolean = true, // 启用递归监控
    val enableFastStartup: Boolean = true, // 启用快速启动
    val batchProcessingEnabled: Boolean = true, // 启用批处理
    val batchProcessingIntervalMs: Long = 50, // 批处理间隔，降低以提高实时性
    val batchSize: Int = 200, // 批处理大小，增加以提高吞吐量
    val detectRefactoring: Boolean = true, // 检测大规模重构
    val refactoringThreshold: Int = 15, // 重构检测阈值（短时间内变更的文件数）
    val refactoringTimeWindowMs: Long = 1000, // 重构检测时间窗口
    val prioritizeActiveFiles: Boolean = true, // 优先处理活跃文件
    val activeFileTimeWindowMs: Long = 30000, // 活跃文件时间窗口（30秒）
    val maxConcurrentWatchers: Int = 100, // 最大并发监控器数量
    val watcherRestartDelayMs: Long = 500, // 监控器重启延迟
    val enableWatcherHealthCheck: Boolean = true, // 启用监控器健康检查
    val watcherHealthCheckIntervalMs: Long = 10000 // 监控器健康检查间隔（10秒）
)

/**
 * 文件系统监控器
 *
 * 监控文件系统变更，并发出变更事件
 *
 * @property rootPath 根路径
 * @property config 配置
 */
/**
 * 文件变更监听器
 */
interface FileChangeListener {
    /**
     * 处理文件变更事件
     *
     * @param event 文件变更事件
     */
    suspend fun onFileChange(event: FileChangeEvent)
}

/**
 * 文件系统监控器
 *
 * 监控文件系统变更，并发出变更事件
 *
 * @property rootPath 根路径
 * @property config 配置
 */
/**
 * 文件系统监控器
 *
 * 监控文件系统变更，并发出变更事件。优化设计以实现毫秒级响应，支持大型代码库和高频变更。
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
    private val _fileChanges = MutableSharedFlow<FileChangeEvent>(extraBufferCapacity = config.eventBufferCapacity)
    val fileChanges: SharedFlow<FileChangeEvent> = _fileChanges

    // 批处理事件队列
    private val batchQueue = Collections.synchronizedList(mutableListOf<FileChangeEvent>())

    // 活跃的监控器
    private val activeWatchers = ConcurrentHashMap<Path, DirectoryWatcher>()

    // 最近处理的事件（用于去重）
    private val recentEvents = ConcurrentHashMap<String, Long>()

    // 重构检测 - 记录短时间内的文件变更
    private val recentFileChanges = Collections.synchronizedList(mutableListOf<FileChangeEvent>())

    // 活跃文件集合 - 记录最近访问的文件
    private val activeFiles = ConcurrentHashMap<Path, Long>()

    // 监控器互斥锁
    private val watcherMutex = Mutex()

    // 是否正在运行
    private val isRunning = AtomicBoolean(false)

    // 文件变更监听器列表
    private val changeListeners = ConcurrentHashMap.newKeySet<FileChangeListener>()

    // 监控器健康状态
    private val watcherHealth = ConcurrentHashMap<Path, Boolean>()

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
                        scope.launch {
                            watchDirectory(path)
                        }
                    }
                    DirectoryChangeEvent.EventType.DELETE -> {
                        logger.debug { "目录删除: $path" }
                        scope.launch {
                            unwatchDirectory(path)
                        }
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

                // 检查是否是重复事件
                val eventKey = "${path}:${changeType}"
                val now = System.currentTimeMillis()
                val lastEventTime = recentEvents.put(eventKey, now)

                // 如果是重复事件且在节流时间内，则忽略
                if (lastEventTime != null && now - lastEventTime < config.eventThrottleMs) {
                    return
                }

                val changeEvent = FileChangeEvent(path, changeType)
                scope.launch {
                    // 如果启用了批处理，则添加到批处理队列
                    if (config.batchProcessingEnabled) {
                        batchQueue.add(changeEvent)
                    } else {
                        // 否则直接发送事件
                        _fileChanges.emit(changeEvent)
                        notifyListeners(changeEvent)
                    }

                    // 如果启用了重构检测，则添加到最近文件变更列表
                    if (config.detectRefactoring) {
                        synchronized(recentFileChanges) {
                            recentFileChanges.add(changeEvent)
                        }
                    }

                    logger.debug { "文件变更: $changeEvent" }
                }
            }
        }
    }

    /**
     * 启动监控
     */
    fun start() {
        if (isRunning.getAndSet(true)) {
            logger.warn { "文件系统监控器已经在运行" }
            return
        }

        logger.info { "开始监控文件系统: $rootPath" }

        // 清理旧的事件记录
        recentEvents.clear()
        recentFileChanges.clear()
        batchQueue.clear()
        activeFiles.clear()
        watcherHealth.clear()

        // 启动监控
        scope.launch {
            watchDirectory(rootPath)

            // 定期清理过期的事件记录
            startEventCleanup()

            // 启动批处理
            if (config.batchProcessingEnabled) {
                startBatchProcessing()
            }

            // 启动重构检测
            if (config.detectRefactoring) {
                startRefactoringDetection()
            }

            // 启动活跃文件跟踪
            if (config.prioritizeActiveFiles) {
                startActiveFileTracking()
            }

            // 启动监控器健康检查
            if (config.enableWatcherHealthCheck) {
                startWatcherHealthCheck()
            }
        }
    }

    /**
     * 停止监控
     */
    fun stop() {
        if (!isRunning.getAndSet(false)) {
            logger.warn { "文件系统监控器已经停止" }
            return
        }

        logger.info { "停止监控文件系统: $rootPath" }

        // 关闭所有监控器
        scope.launch {
            watcherMutex.withLock {
                activeWatchers.values.forEach { it.close() }
                activeWatchers.clear()
            }
        }
    }

    /**
     * 注册文件变更监听器
     *
     * @param listener 监听器
     */
    fun registerChangeListener(listener: FileChangeListener) {
        changeListeners.add(listener)
        logger.debug { "注册文件变更监听器: $listener" }
    }

    /**
     * 取消注册文件变更监听器
     *
     * @param listener 监听器
     */
    fun unregisterChangeListener(listener: FileChangeListener) {
        changeListeners.remove(listener)
        logger.debug { "取消注册文件变更监听器: $listener" }
    }

    /**
     * 通知所有监听器
     *
     * @param event 文件变更事件
     */
    private suspend fun notifyListeners(event: FileChangeEvent) {
        changeListeners.forEach { listener ->
            try {
                listener.onFileChange(event)
            } catch (e: Exception) {
                logger.error(e) { "通知监听器时出错: $listener" }
            }
        }
    }

    /**
     * 启动事件清理
     */
    private fun startEventCleanup() {
        scope.launch {
            while (isRunning.get()) {
                try {
                    // 清理过期的事件记录
                    val now = System.currentTimeMillis()
                    val expireTime = now - (config.eventThrottleMs * 10) // 10倍节流时间

                    recentEvents.entries.removeIf { (_, timestamp) -> timestamp < expireTime }

                    // 等待一段时间
                    kotlinx.coroutines.delay(config.eventThrottleMs * 10)
                } catch (e: Exception) {
                    logger.error(e) { "清理过期事件记录时出错" }
                }
            }
        }
    }

    /**
     * 启动批处理
     */
    private fun startBatchProcessing() {
        scope.launch {
            while (isRunning.get()) {
                try {
                    // 处理批量事件
                    processBatch()

                    // 等待下一个批处理周期
                    kotlinx.coroutines.delay(config.batchProcessingIntervalMs)
                } catch (e: Exception) {
                    logger.error(e) { "批处理事件时出错" }
                }
            }
        }
    }

    /**
     * 处理批量事件
     */
    private suspend fun processBatch() {
        // 如果队列为空，则跳过
        if (batchQueue.isEmpty()) {
            return
        }

        // 获取当前批次的事件（最多 batchSize 个）
        val batch = synchronized(batchQueue) {
            val currentBatch = batchQueue.take(config.batchSize)
            batchQueue.removeAll(currentBatch)
            currentBatch
        }

        // 如果批次为空，则跳过
        if (batch.isEmpty()) {
            return
        }

        logger.debug { "处理批量事件: ${batch.size} 个事件" }

        // 去重（保留每个文件的最新事件）
        val deduplicatedEvents = batch
            .groupBy { event -> event.path }
            .mapValues { entry -> entry.value.maxByOrNull { event -> event.timestamp }!! }
            .values
            .toList()

        // 如果启用了活跃文件优先级，则按优先级排序
        val sortedEvents = if (config.prioritizeActiveFiles) {
            deduplicatedEvents.sortedByDescending { event ->
                // 活跃文件优先
                activeFiles[event.path]?.let { lastAccessTime ->
                    // 返回活跃度分数（最近访问的文件分数更高）
                    val now = System.currentTimeMillis()
                    val age = now - lastAccessTime
                    if (age <= config.activeFileTimeWindowMs) {
                        // 活跃文件，分数为 1.0 到 0.5，越近越高
                        1.0 - (age.toDouble() / config.activeFileTimeWindowMs / 2)
                    } else {
                        // 非活跃文件，分数为 0.5 到 0.0
                        0.5 * (1.0 - ((age - config.activeFileTimeWindowMs).toDouble() / config.activeFileTimeWindowMs).coerceAtMost(1.0))
                    }
                } ?: 0.0 // 未记录的文件最低优先级
            }
        } else {
            deduplicatedEvents
        }

        // 发送事件
        for (event in sortedEvents) {
            _fileChanges.emit(event)
            notifyListeners(event)

            // 更新活跃文件记录
            if (config.prioritizeActiveFiles) {
                activeFiles[event.path] = System.currentTimeMillis()
            }
        }
    }

    /**
     * 启动重构检测
     */
    private fun startRefactoringDetection() {
        scope.launch {
            while (isRunning.get()) {
                try {
                    // 检测重构
                    detectRefactoring()

                    // 清理过期的文件变更记录
                    cleanupRecentFileChanges()

                    // 等待一段时间
                    kotlinx.coroutines.delay(config.refactoringTimeWindowMs / 2)
                } catch (e: Exception) {
                    logger.error(e) { "检测重构时出错" }
                }
            }
        }
    }

    /**
     * 检测重构
     */
    private suspend fun detectRefactoring() {
        val now = System.currentTimeMillis()

        // 获取时间窗口内的文件变更
        val changesInWindow = synchronized(recentFileChanges) {
            recentFileChanges.filter { event -> now - event.timestamp <= config.refactoringTimeWindowMs }
        }

        // 如果变更数量超过阈值，则认为是重构
        if (changesInWindow.size >= config.refactoringThreshold) {
            val uniqueFiles = changesInWindow.map { event -> event.path }.toSet()

            logger.info { "检测到可能的重构操作: ${uniqueFiles.size} 个文件在 ${config.refactoringTimeWindowMs}ms 内变更" }

            // 发送重构事件
            val refactoringEvent = FileChangeEvent(
                path = rootPath,
                type = FileChangeType.MODIFY, // 使用 MODIFY 类型表示重构
                timestamp = now
            )

            _fileChanges.emit(refactoringEvent)
            notifyListeners(refactoringEvent)
        }
    }

    /**
     * 清理过期的文件变更记录
     */
    private fun cleanupRecentFileChanges() {
        val now = System.currentTimeMillis()
        val expireTime = now - config.refactoringTimeWindowMs

        synchronized(recentFileChanges) {
            recentFileChanges.removeIf { event -> event.timestamp < expireTime }
        }
    }

    /**
     * 启动活跃文件跟踪
     */
    private fun startActiveFileTracking() {
        scope.launch {
            while (isRunning.get()) {
                try {
                    // 清理过期的活跃文件记录
                    val now = System.currentTimeMillis()
                    val expireTime = now - (config.activeFileTimeWindowMs * 2) // 2倍活跃窗口时间

                    activeFiles.entries.removeIf { (_, timestamp) -> timestamp < expireTime }

                    // 等待一段时间
                    kotlinx.coroutines.delay(config.activeFileTimeWindowMs / 2)
                } catch (e: Exception) {
                    logger.error(e) { "清理活跃文件记录时出错" }
                }
            }
        }
    }

    /**
     * 启动监控器健康检查
     */
    private fun startWatcherHealthCheck() {
        scope.launch {
            while (isRunning.get()) {
                try {
                    // 检查所有监控器的健康状态
                    val unhealthyWatchers = mutableListOf<Path>()

                    watcherMutex.withLock {
                        activeWatchers.forEach { (path, _) ->
                            val isHealthy = watcherHealth.getOrDefault(path, true)
                            if (!isHealthy) {
                                unhealthyWatchers.add(path)
                            }
                        }
                    }

                    // 重启不健康的监控器
                    unhealthyWatchers.forEach { path ->
                        logger.warn { "检测到不健康的监控器: $path，正在重启" }
                        restartWatcher(path)
                    }

                    // 等待下一次检查
                    kotlinx.coroutines.delay(config.watcherHealthCheckIntervalMs)
                } catch (e: Exception) {
                    logger.error(e) { "监控器健康检查时出错" }
                }
            }
        }
    }

    /**
     * 重启监控器
     */
    private suspend fun restartWatcher(directory: Path) {
        try {
            // 停止并移除旧的监控器
            unwatchDirectory(directory)

            // 等待一段时间
            kotlinx.coroutines.delay(config.watcherRestartDelayMs)

            // 启动新的监控器
            watchDirectory(directory)

            // 更新健康状态
            watcherHealth[directory] = true

            logger.info { "成功重启监控器: $directory" }
        } catch (e: Exception) {
            logger.error(e) { "重启监控器时出错: $directory" }
            watcherHealth[directory] = false
        }
    }

    /**
     * 监控目录
     */
    private suspend fun watchDirectory(directory: Path) {
        if (!directory.isDirectory() || shouldExcludeDirectory(directory)) {
            return
        }

        try {
            watcherMutex.withLock {
                // 如果已经在监控，则跳过
                if (activeWatchers.containsKey(directory)) {
                    return@withLock
                }

                // 检查是否超过最大并发监控器数量
                if (activeWatchers.size >= config.maxConcurrentWatchers) {
                    logger.warn { "已达到最大并发监控器数量: ${config.maxConcurrentWatchers}，跳过监控: $directory" }
                    return@withLock
                }

                // 创建并启动目录监控器
                val watcherBuilder = DirectoryWatcher.builder()
                    .path(directory)
                    .listener(directoryChangeListener)

                // 设置轮询间隔
                // 注意：由于 API 变更，暂时不设置轮询间隔
                // 如果需要调整，请查阅 directory-watcher 最新文档

                val watcher = watcherBuilder.build()

                activeWatchers[directory] = watcher
                watcherHealth[directory] = true

                // 在后台线程中启动监控
                scope.launch {
                    try {
                        watcher.watch()
                    } catch (e: Exception) {
                        logger.error(e) { "监控目录时出错: $directory" }

                        // 从活跃监控器中移除
                        watcherMutex.withLock {
                            activeWatchers.remove(directory)
                        }

                        // 标记为不健康
                        watcherHealth[directory] = false

                        // 尝试重新监控
                        if (isRunning.get()) {
                            kotlinx.coroutines.delay(config.watcherRestartDelayMs) // 等待后重试
                            watchDirectory(directory)
                        }
                    }
                }

                logger.debug { "开始监控目录: $directory" }
            }

            // 递归监控子目录
            if (config.enableRecursiveWatching) {
                directory.toFile().listFiles()
                    ?.filter { it.isDirectory }
                    ?.filter { !shouldExcludeDirectory(it.toPath()) }
                    ?.map { it.toPath() }
                    ?.forEach { subDir ->
                        // 并行监控子目录
                        scope.launch {
                            watchDirectory(subDir)
                        }
                    }
            }
        } catch (e: Exception) {
            logger.error(e) { "设置目录监控时出错: $directory" }
            watcherHealth[directory] = false
        }
    }

    /**
     * 取消监控目录
     */
    private suspend fun unwatchDirectory(directory: Path) {
        watcherMutex.withLock {
            activeWatchers[directory]?.close()
            activeWatchers.remove(directory)
            logger.debug { "停止监控目录: $directory" }
        }
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
