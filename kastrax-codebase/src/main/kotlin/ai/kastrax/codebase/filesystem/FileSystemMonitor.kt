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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.time.Duration.Companion.milliseconds
import java.nio.file.Paths

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
        Regex("\\.gradle/.*"),
        Regex("\\.DS_Store"),
        Regex("\\.vscode/.*"),
        Regex("\\.vs/.*"),
        Regex("dist/.*"),
        Regex("out/.*"),
        Regex("bin/.*"),
        Regex("obj/.*"),
        Regex("logs/.*"),
        Regex("tmp/.*"),
        Regex("temp/.*")
    ),
    val excludeExtensions: Set<String> = setOf(
        // 二进制文件
        "class", "jar", "war", "zip", "tar", "gz", "rar", "7z", "exe", "dll", "so", "dylib",
        // 图像文件
        "jpg", "jpeg", "png", "gif", "bmp", "ico", "svg", "webp", "tiff", "psd",
        // 媒体文件
        "mp3", "mp4", "avi", "mov", "wmv", "flv", "wav", "ogg", "webm", "mkv",
        // 文档文件
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp",
        // 数据文件
        "db", "sqlite", "mdb", "accdb", "frm", "ibd", "myd", "myi",
        // 日志和临时文件
        "log", "tmp", "temp", "swp", "swo", "bak", "backup"
    ),
    val excludeDirectories: Set<String> = setOf(
        ".git", ".idea", "build", "target", "node_modules", ".gradle",
        ".vscode", ".vs", "dist", "out", "bin", "obj", "logs", "tmp", "temp",
        "coverage", "__pycache__", ".pytest_cache", ".mypy_cache", ".ruff_cache",
        ".next", ".nuxt", ".output", ".cache", ".parcel-cache"
    ),
    val pollIntervalMs: Long = 50, // 降低轮询间隔以提高实时性（Augment级别）
    val eventBufferCapacity: Int = 20000, // 增加事件缓冲区容量，支持大型代码库
    val eventThrottleMs: Long = 10, // 降低事件节流时间，提高实时性
    val watcherThreads: Int = Runtime.getRuntime().availableProcessors(), // 使用所有可用处理器
    val enableRecursiveWatching: Boolean = true, // 启用递归监控
    val enableFastStartup: Boolean = true, // 启用快速启动
    val enableAsyncWatching: Boolean = true, // 启用异步监控
    val enableParallelProcessing: Boolean = true, // 启用并行处理
    val batchProcessingEnabled: Boolean = true, // 启用批处理
    val batchProcessingIntervalMs: Long = 25, // 批处理间隔，降低以提高实时性
    val batchSize: Int = 500, // 批处理大小，增加以提高吞吐量
    val adaptiveBatchSizing: Boolean = true, // 自适应批处理大小
    val minBatchSize: Int = 50, // 最小批处理大小
    val maxBatchSize: Int = 2000, // 最大批处理大小
    val adaptiveThrottling: Boolean = true, // 自适应节流
    val minThrottleMs: Long = 5, // 最小节流时间（毫秒）
    val maxThrottleMs: Long = 100, // 最大节流时间（毫秒）
    val detectRefactoring: Boolean = true, // 检测大规模重构
    val refactoringThreshold: Int = 30, // 重构检测阈值（短时间内变更的文件数）
    val refactoringTimeWindowMs: Long = 1000, // 重构检测时间窗口
    val prioritizeActiveFiles: Boolean = true, // 优先处理活跃文件
    val activeFileTimeWindowMs: Long = 30000, // 活跃文件时间窗口（30秒）
    val maxConcurrentWatchers: Int = 500, // 最大并发监控器数量
    val watcherRestartDelayMs: Long = 300, // 监控器重启延迟
    val enableWatcherHealthCheck: Boolean = true, // 启用监控器健康检查
    val watcherHealthCheckIntervalMs: Long = 5000, // 监控器健康检查间隔（5秒）
    val enableWatcherLoadBalancing: Boolean = true, // 启用监控器负载均衡
    val loadBalancingIntervalMs: Long = 10000, // 负载均衡间隔（10秒）
    val enableSmartPolling: Boolean = true, // 启用智能轮询
    val smartPollingBaseIntervalMs: Long = 1000, // 智能轮询基础间隔（1秒）
    val smartPollingMaxIntervalMs: Long = 10000, // 智能轮询最大间隔（10秒）
    val enableDirectoryPrioritization: Boolean = true, // 启用目录优先级
    val highPriorityDirectories: Set<String> = setOf("src", "lib", "app"), // 高优先级目录
    val highPriorityPollingIntervalMs: Long = 30, // 高优先级轮询间隔（30毫秒）
    val lowPriorityPollingIntervalMs: Long = 2000, // 低优先级轮询间隔（2秒）
    val enableGitIntegration: Boolean = true, // 启用Git集成
    val gitIntegrationIntervalMs: Long = 2000, // Git集成间隔（2秒）
    val supportedVersionControlSystems: Set<String> = setOf("git", "svn", "mercurial") // 支持的版本控制系统
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

    // 监控器负载状态
    private val watcherLoad = ConcurrentHashMap<Path, Int>()

    // 目录优先级
    private val directoryPriority = ConcurrentHashMap<Path, Int>()

    // 当前批处理大小（自适应）
    private val currentBatchSize = AtomicInteger(config.batchSize)

    // 当前节流时间（自适应）
    private val currentThrottleMs = AtomicLong(config.eventThrottleMs)

    // 事件处理统计
    private val eventStats = ConcurrentHashMap<String, Long>()

    // 版本控制系统集成
    private val vcsIntegration = ConcurrentHashMap<String, Any>()

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
        watcherLoad.clear()
        directoryPriority.clear()
        eventStats.clear()
        vcsIntegration.clear()

        // 初始化目录优先级
        if (config.enableDirectoryPrioritization) {
            initializeDirectoryPriorities()
        }

        // 初始化版本控制系统集成
        if (config.enableGitIntegration) {
            initializeVcsIntegration()
        }

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

            // 启动监控器负载均衡
            if (config.enableWatcherLoadBalancing) {
                startWatcherLoadBalancing()
            }

            // 启动自适应批处理
            if (config.adaptiveBatchSizing) {
                startAdaptiveBatchSizing()
            }

            // 启动自适应节流
            if (config.adaptiveThrottling) {
                startAdaptiveThrottling()
            }

            // 启动版本控制系统集成
            if (config.enableGitIntegration) {
                startVcsIntegration()
            }
        }
    }

    /**
     * 初始化目录优先级
     */
    private fun initializeDirectoryPriorities() {
        try {
            // 高优先级目录
            val highPriorityDirs = config.highPriorityDirectories

            // 遍历根目录下的所有目录
            rootPath.toFile().walk()
                .filter { it.isDirectory }
                .forEach { dir ->
                    val dirPath = dir.toPath()
                    val dirName = dirPath.fileName.toString()

                    // 设置优先级（0=低，1=高）
                    val priority = if (dirName in highPriorityDirs) 1 else 0
                    directoryPriority[dirPath] = priority
                }

            logger.debug { "初始化目录优先级完成，高优先级目录数量: ${directoryPriority.count { it.value == 1 }}" }
        } catch (e: Exception) {
            logger.error(e) { "初始化目录优先级时出错" }
        }
    }

    /**
     * 初始化版本控制系统集成
     */
    private fun initializeVcsIntegration() {
        try {
            // 检测是否是Git仓库
            val gitDir = rootPath.resolve(".git")
            if (gitDir.toFile().exists() && gitDir.toFile().isDirectory) {
                vcsIntegration["type"] = "git"
                vcsIntegration["rootDir"] = gitDir.toString()
                logger.debug { "检测到Git仓库: $rootPath" }
            }

            // 检测是否是SVN仓库
            val svnDir = rootPath.resolve(".svn")
            if (svnDir.toFile().exists() && svnDir.toFile().isDirectory) {
                vcsIntegration["type"] = "svn"
                vcsIntegration["rootDir"] = svnDir.toString()
                logger.debug { "检测到SVN仓库: $rootPath" }
            }

            // 检测是否是Mercurial仓库
            val hgDir = rootPath.resolve(".hg")
            if (hgDir.toFile().exists() && hgDir.toFile().isDirectory) {
                vcsIntegration["type"] = "mercurial"
                vcsIntegration["rootDir"] = hgDir.toString()
                logger.debug { "检测到Mercurial仓库: $rootPath" }
            }
        } catch (e: Exception) {
            logger.error(e) { "初始化版本控制系统集成时出错" }
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

        // 获取当前批处理大小（自适应）
        val batchSize = if (config.adaptiveBatchSizing) {
            currentBatchSize.get()
        } else {
            config.batchSize
        }

        // 获取当前批次的事件
        val batch = synchronized(batchQueue) {
            val currentBatch = batchQueue.take(batchSize)
            batchQueue.removeAll(currentBatch)
            currentBatch
        }

        // 如果批次为空，则跳过
        if (batch.isEmpty()) {
            return
        }

        // 记录开始时间（用于性能统计）
        val startTime = System.currentTimeMillis()

        logger.debug { "处理批量事件: ${batch.size} 个事件" }

        // 去重（保留每个文件的最新事件）
        val deduplicatedEvents = batch
            .groupBy { event -> event.path }
            .mapValues { entry -> entry.value.maxByOrNull { event -> event.timestamp }!! }
            .values
            .toList()

        // 根据多种因素进行排序
        val sortedEvents = deduplicatedEvents.sortedByDescending { event ->
            var score = 0.0

            // 1. 活跃文件优先级
            if (config.prioritizeActiveFiles) {
                activeFiles[event.path]?.let { lastAccessTime ->
                    val now = System.currentTimeMillis()
                    val age = now - lastAccessTime
                    if (age <= config.activeFileTimeWindowMs) {
                        // 活跃文件分数
                        score += 1.0 - (age.toDouble() / config.activeFileTimeWindowMs / 2)
                    } else {
                        // 非活跃文件分数
                        score += 0.5 * (1.0 - ((age - config.activeFileTimeWindowMs).toDouble() / config.activeFileTimeWindowMs).coerceAtMost(1.0))
                    }
                }
            }

            // 2. 目录优先级
            if (config.enableDirectoryPrioritization) {
                // 获取文件所在目录
                val parentDir = event.path.getParent()
                val priority = directoryPriority[parentDir] ?: 0

                // 高优先级目录加分
                if (priority > 0) {
                    score += 0.5
                }
            }

            // 3. 事件类型优先级（创建 > 修改 > 删除）
            score += when (event.type) {
                FileChangeType.CREATE -> 0.3
                FileChangeType.MODIFY -> 0.2
                FileChangeType.DELETE -> 0.1
            }

            score
        }

        // 发送事件
        for (event in sortedEvents) {
            _fileChanges.emit(event)
            notifyListeners(event)

            // 更新活跃文件记录
            if (config.prioritizeActiveFiles) {
                activeFiles[event.path] = System.currentTimeMillis()
            }

            // 更新事件统计
            val eventType = event.type.toString()
            eventStats[eventType] = (eventStats[eventType] ?: 0) + 1
        }

        // 记录处理时间（用于性能统计和自适应批处理）
        val processingTime = System.currentTimeMillis() - startTime
        eventStats["lastBatchProcessingTime"] = processingTime
        eventStats["lastBatchSize"] = batch.size.toLong()

        // 更新平均处理时间
        val avgProcessingTime = eventStats["avgBatchProcessingTime"] ?: processingTime
        eventStats["avgBatchProcessingTime"] = (avgProcessingTime * 0.9 + processingTime * 0.1).toLong()

        logger.debug { "批处理完成: ${batch.size} 个事件, 处理时间: ${processingTime}ms" }
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
     * 启动监控器负载均衡
     */
    private fun startWatcherLoadBalancing() {
        scope.launch {
            while (isRunning.get()) {
                try {
                    // 计算每个监控器的负载
                    val watcherEventCounts = ConcurrentHashMap<Path, Int>()

                    // 收集每个监控器的事件数量
                    activeWatchers.keys.forEach { path ->
                        // 获取当前负载
                        val currentLoad = watcherLoad[path] ?: 0

                        // 负载衰减（每个周期负载减半）
                        val newLoad = currentLoad / 2
                        watcherLoad[path] = newLoad
                        watcherEventCounts[path] = newLoad
                    }

                    // 计算负载统计
                    val totalLoad = watcherLoad.values.sum()
                    val avgLoad = if (watcherLoad.isNotEmpty()) totalLoad / watcherLoad.size else 0
                    val maxLoad = watcherLoad.values.maxOrNull() ?: 0
                    val minLoad = watcherLoad.values.minOrNull() ?: 0

                    // 记录负载统计
                    eventStats["totalWatcherLoad"] = totalLoad.toLong()
                    eventStats["avgWatcherLoad"] = avgLoad.toLong()
                    eventStats["maxWatcherLoad"] = maxLoad.toLong()
                    eventStats["minWatcherLoad"] = minLoad.toLong()

                    // 检测负载不均衡情况
                    if (maxLoad > avgLoad * 3 && watcherLoad.size > 1) {
                        logger.info { "检测到负载不均衡: 最大负载 $maxLoad, 平均负载 $avgLoad" }

                        // 找出负载最高的监控器
                        val highestLoadWatcher = watcherLoad.entries
                            .filter { it.value == maxLoad }
                            .map { it.key }
                            .firstOrNull()

                        // 重启负载过高的监控器
                        if (highestLoadWatcher != null) {
                            logger.info { "重启负载过高的监控器: $highestLoadWatcher (负载: $maxLoad)" }
                            restartWatcher(highestLoadWatcher)
                        }
                    }

                    // 等待下一次负载均衡
                    kotlinx.coroutines.delay(config.loadBalancingIntervalMs)
                } catch (e: Exception) {
                    logger.error(e) { "监控器负载均衡时出错" }
                }
            }
        }
    }

    /**
     * 启动自适应批处理大小
     */
    private fun startAdaptiveBatchSizing() {
        scope.launch {
            while (isRunning.get()) {
                try {
                    // 获取当前批处理统计
                    val lastBatchSize = eventStats["lastBatchSize"] ?: config.batchSize.toLong()
                    val lastProcessingTime = eventStats["lastBatchProcessingTime"] ?: 0L
                    val queueSize = batchQueue.size

                    // 计算目标处理时间（我们希望每个批次处理时间在 50-200ms 之间）
                    val targetProcessingTime = 100L // 目标处理时间（毫秒）

                    // 当前批处理大小
                    val currentSize = currentBatchSize.get()

                    // 计算新的批处理大小
                    var newBatchSize = currentSize

                    if (lastProcessingTime > 0 && lastBatchSize > 0) {
                        // 根据处理时间调整批大小
                        if (lastProcessingTime > targetProcessingTime * 2) {
                            // 处理时间过长，减小批大小
                            newBatchSize = (currentSize * 0.8).toInt().coerceAtLeast(config.minBatchSize)
                        } else if (lastProcessingTime < targetProcessingTime / 2 && queueSize > currentSize) {
                            // 处理时间过短，增加批大小
                            newBatchSize = (currentSize * 1.2).toInt().coerceAtMost(config.maxBatchSize)
                        }
                    } else if (queueSize > currentSize * 2) {
                        // 队列积压，增加批大小
                        newBatchSize = (currentSize * 1.5).toInt().coerceAtMost(config.maxBatchSize)
                    }

                    // 更新批处理大小
                    if (newBatchSize != currentSize) {
                        currentBatchSize.set(newBatchSize)
                        logger.debug { "自适应调整批处理大小: $currentSize -> $newBatchSize (处理时间: ${lastProcessingTime}ms, 队列大小: $queueSize)" }
                    }

                    // 等待下一次调整
                    kotlinx.coroutines.delay(5000) // 5秒调整一次
                } catch (e: Exception) {
                    logger.error(e) { "自适应批处理大小调整时出错" }
                }
            }
        }
    }

    /**
     * 启动自适应节流
     */
    private fun startAdaptiveThrottling() {
        scope.launch {
            while (isRunning.get()) {
                try {
                    // 获取当前队列大小和处理统计
                    val queueSize = batchQueue.size
                    val avgProcessingTime = eventStats["avgBatchProcessingTime"] ?: 0L

                    // 当前节流时间
                    val currentThrottle = currentThrottleMs.get()

                    // 计算新的节流时间
                    var newThrottle = currentThrottle

                    if (queueSize > currentBatchSize.get() * 5) {
                        // 队列积压，减少节流（增加吞吐量）
                        newThrottle = (currentThrottle * 0.8).toLong().coerceAtLeast(config.minThrottleMs)
                    } else if (queueSize < currentBatchSize.get() / 2 && avgProcessingTime < 50) {
                        // 队列空闲，增加节流（减少资源消耗）
                        newThrottle = (currentThrottle * 1.2).toLong().coerceAtMost(config.maxThrottleMs)
                    }

                    // 更新节流时间
                    if (newThrottle != currentThrottle) {
                        currentThrottleMs.set(newThrottle)
                        logger.debug { "自适应调整节流时间: ${currentThrottle}ms -> ${newThrottle}ms (队列大小: $queueSize)" }
                    }

                    // 等待下一次调整
                    kotlinx.coroutines.delay(3000) // 3秒调整一次
                } catch (e: Exception) {
                    logger.error(e) { "自适应节流调整时出错" }
                }
            }
        }
    }

    /**
     * 启动版本控制系统集成
     */
    private fun startVcsIntegration() {
        // 如果没有检测到版本控制系统，则跳过
        if (vcsIntegration["type"] == null) {
            logger.debug { "未检测到支持的版本控制系统，跳过集成" }
            return
        }

        val vcsType = vcsIntegration["type"] as String
        logger.info { "启动 $vcsType 版本控制系统集成" }

        scope.launch {
            while (isRunning.get()) {
                try {
                    when (vcsType) {
                        "git" -> monitorGitRepository()
                        "svn" -> monitorSvnRepository()
                        "mercurial" -> monitorMercurialRepository()
                    }

                    // 等待下一次检查
                    kotlinx.coroutines.delay(config.gitIntegrationIntervalMs)
                } catch (e: Exception) {
                    logger.error(e) { "监控版本控制系统时出错" }
                }
            }
        }
    }

    /**
     * 监控 Git 仓库
     */
    private suspend fun monitorGitRepository() {
        // 检查 HEAD 文件变更
        val headFile = rootPath.resolve(".git/HEAD").toFile()
        if (headFile.exists()) {
            val lastModified = headFile.lastModified()
            val lastChecked = vcsIntegration["lastGitHeadCheck"] as? Long ?: 0L

            if (lastModified > lastChecked) {
                logger.info { "检测到 Git HEAD 文件变更，可能发生了分支切换" }

                // 触发分支变更事件
                val branchChangeEvent = FileChangeEvent(
                    path = rootPath,
                    type = FileChangeType.MODIFY,
                    timestamp = System.currentTimeMillis()
                )

                _fileChanges.emit(branchChangeEvent)
                notifyListeners(branchChangeEvent)

                // 更新检查时间
                vcsIntegration["lastGitHeadCheck"] = System.currentTimeMillis()
            }
        }
    }

    /**
     * 监控 SVN 仓库
     */
    private suspend fun monitorSvnRepository() {
        // SVN 监控实现
        // 由于 SVN 的特性，可能需要使用命令行工具来检测变更
        // 这里的实现是简化的
        val svnDir = rootPath.resolve(".svn").toFile()
        if (svnDir.exists()) {
            val lastModified = svnDir.lastModified()
            val lastChecked = vcsIntegration["lastSvnCheck"] as? Long ?: 0L

            if (lastModified > lastChecked) {
                logger.info { "检测到 SVN 目录变更" }

                // 更新检查时间
                vcsIntegration["lastSvnCheck"] = System.currentTimeMillis()
            }
        }
    }

    /**
     * 监控 Mercurial 仓库
     */
    private suspend fun monitorMercurialRepository() {
        // Mercurial 监控实现
        val hgDir = rootPath.resolve(".hg").toFile()
        if (hgDir.exists()) {
            val lastModified = hgDir.lastModified()
            val lastChecked = vcsIntegration["lastHgCheck"] as? Long ?: 0L

            if (lastModified > lastChecked) {
                logger.info { "检测到 Mercurial 目录变更" }

                // 更新检查时间
                vcsIntegration["lastHgCheck"] = System.currentTimeMillis()
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
                    // 如果启用了负载均衡，尝试移除低优先级目录的监控器
                    if (config.enableWatcherLoadBalancing) {
                        val lowPriorityWatcher = findLowPriorityWatcher()
                        if (lowPriorityWatcher != null) {
                            // 关闭低优先级监控器
                            activeWatchers[lowPriorityWatcher]?.close()
                            activeWatchers.remove(lowPriorityWatcher)
                            watcherHealth.remove(lowPriorityWatcher)
                            watcherLoad.remove(lowPriorityWatcher)
                            logger.debug { "移除低优先级监控器以节省资源: $lowPriorityWatcher" }
                        } else {
                            logger.warn { "已达到最大并发监控器数量: ${config.maxConcurrentWatchers}，跳过监控: $directory" }
                            return@withLock
                        }
                    } else {
                        logger.warn { "已达到最大并发监控器数量: ${config.maxConcurrentWatchers}，跳过监控: $directory" }
                        return@withLock
                    }
                }

                // 创建并启动目录监控器
                val watcherBuilder = DirectoryWatcher.builder()
                    .path(directory)
                    .listener(directoryChangeListener)

                // 设置轮询间隔
                // 注意：由于 API 变更，暂时不设置轮询间隔
                // 如果需要调整，请查阅 directory-watcher 最新文档

                // 如果启用了目录优先级，设置不同的轮询间隔
                if (config.enableDirectoryPrioritization) {
                    val dirName = directory.getFileName().toString()
                    val isHighPriority = dirName in config.highPriorityDirectories

                    // 设置目录优先级
                    directoryPriority[directory] = if (isHighPriority) 1 else 0

                    // 记录在日志中
                    if (isHighPriority) {
                        logger.debug { "高优先级目录: $directory" }
                    }
                }

                val watcher = watcherBuilder.build()

                activeWatchers[directory] = watcher
                watcherHealth[directory] = true
                watcherLoad[directory] = 0 // 初始负载为0

                // 在后台线程中启动监控
                scope.launch {
                    try {
                        watcher.watch()
                    } catch (e: Exception) {
                        logger.error(e) { "监控目录时出错: $directory" }

                        // 从活跃监控器中移除
                        watcherMutex.withLock {
                            activeWatchers.remove(directory)
                            watcherHealth.remove(directory)
                            watcherLoad.remove(directory)
                        }

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
     * 查找低优先级监控器
     *
     * @return 低优先级监控器路径，如果没有则返回 null
     */
    private fun findLowPriorityWatcher(): Path? {
        // 首先尝试找到低优先级目录的监控器
        return activeWatchers.keys
            .filter { directoryPriority[it] == 0 } // 低优先级目录
            .minByOrNull { watcherLoad[it] ?: 0 } // 选择负载最低的
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
