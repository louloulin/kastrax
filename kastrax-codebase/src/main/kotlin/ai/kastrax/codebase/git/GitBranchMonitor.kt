package ai.kastrax.codebase.git

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

/**
 * Git 分支变更事件
 *
 * @property repositoryPath 仓库路径
 * @property previousBranch 之前的分支
 * @property currentBranch 当前分支
 * @property timestamp 时间戳
 */
data class GitBranchChangeEvent(
    val repositoryPath: Path,
    val previousBranch: String,
    val currentBranch: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Git 分支监控器配置
 *
 * @property pollIntervalSeconds 轮询间隔（秒）
 * @property initialDelaySeconds 初始延迟（秒）
 * @property retryDelaySeconds 重试延迟（秒）
 * @property maxRetries 最大重试次数
 * @property enableHeadFileWatching 是否启用 HEAD 文件监控
 * @property headFileCheckIntervalMs HEAD 文件检查间隔（毫秒）
 * @property enableReflogMonitoring 是否启用 reflog 监控
 * @property reflogCheckIntervalSeconds reflog 检查间隔（秒）
 * @property eventBufferCapacity 事件缓冲区容量
 * @property enableFastBranchDetection 是否启用快速分支检测
 */
data class GitBranchMonitorConfig(
    val pollIntervalSeconds: Long = 2, // 降低轮询间隔以提高实时性
    val initialDelaySeconds: Long = 1, // 初始延迟
    val retryDelaySeconds: Long = 3, // 重试延迟
    val maxRetries: Int = 3, // 最大重试次数
    val enableHeadFileWatching: Boolean = true, // 启用 HEAD 文件监控
    val headFileCheckIntervalMs: Long = 500, // HEAD 文件检查间隔
    val enableReflogMonitoring: Boolean = true, // 启用 reflog 监控
    val reflogCheckIntervalSeconds: Long = 5, // reflog 检查间隔
    val eventBufferCapacity: Int = 20, // 事件缓冲区容量
    val enableFastBranchDetection: Boolean = true // 启用快速分支检测
)

/**
 * Git 分支监控器
 *
 * 监控 Git 仓库的分支变更，支持多种检测方式以提高可靠性和实时性
 *
 * @property repositoryPath 仓库路径
 * @property config 配置
 */
class GitBranchMonitor(
    private val repositoryPath: Path,
    private val config: GitBranchMonitorConfig = GitBranchMonitorConfig()
) : AutoCloseable {
    private val logger = KotlinLogging.logger {}
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 分支变更事件流
    private val _branchChanges = MutableSharedFlow<GitBranchChangeEvent>(extraBufferCapacity = config.eventBufferCapacity)
    val branchChanges: SharedFlow<GitBranchChangeEvent> = _branchChanges

    // 当前分支
    private var currentBranch: String? = null

    // 监控任务是否运行
    private var isRunning = false

    // HEAD 文件的最后修改时间
    private var lastHeadModified: Long = 0

    // 最后一次 reflog 条目
    private var lastReflogEntry: String? = null

    // Git 仓库
    private var repository: Repository? = null

    // 重试计数
    private var retryCount = 0

    /**
     * 初始化 Git 仓库
     */
    private fun initRepository(): Repository? {
        try {
            val gitDir = repositoryPath.resolve(".git").toFile()
            if (!gitDir.exists()) {
                logger.warn { "Git 目录不存在: $gitDir" }
                return null
            }

            val builder = FileRepositoryBuilder()
            val repo = builder.setGitDir(gitDir)
                .readEnvironment()
                .findGitDir()
                .build()

            return repo
        } catch (e: Exception) {
            logger.error(e) { "初始化 Git 仓库时出错" }
            return null
        }
    }

    /**
     * 启动监控
     */
    fun start() {
        if (isRunning) {
            logger.warn { "Git 分支监控器已经在运行" }
            return
        }

        isRunning = true
        logger.info { "开始监控 Git 分支: $repositoryPath" }

        // 初始化 Git 仓库
        repository = initRepository()
        if (repository == null) {
            logger.error { "无法初始化 Git 仓库，分支监控将不可用" }
            return
        }

        // 初始化当前分支
        currentBranch = getCurrentBranch()
        logger.debug { "初始分支: $currentBranch" }

        // 初始化 HEAD 文件的最后修改时间
        if (config.enableHeadFileWatching) {
            updateHeadFileTimestamp()
        }

        // 初始化最后一次 reflog 条目
        if (config.enableReflogMonitoring) {
            lastReflogEntry = getLatestReflogEntry()
        }

        // 启动轮询监控任务
        scope.launch {
            delay(config.initialDelaySeconds.seconds) // 初始延迟

            while (isActive && isRunning) {
                try {
                    checkBranchChange()
                    retryCount = 0 // 重置重试计数
                    delay(config.pollIntervalSeconds.seconds)
                } catch (e: Exception) {
                    logger.error(e) { "监控 Git 分支时出错" }

                    // 增加重试计数
                    retryCount++
                    if (retryCount > config.maxRetries) {
                        logger.warn { "Git 分支监控重试次数过多，尝试重新初始化仓库" }
                        repository?.close()
                        repository = initRepository()
                        retryCount = 0
                    }

                    delay(config.retryDelaySeconds.seconds)
                }
            }
        }

        // 启动 HEAD 文件监控任务
        if (config.enableHeadFileWatching) {
            scope.launch {
                while (isActive && isRunning) {
                    try {
                        checkHeadFileChange()
                        delay(config.headFileCheckIntervalMs)
                    } catch (e: Exception) {
                        logger.error(e) { "监控 HEAD 文件时出错" }
                        delay(config.retryDelaySeconds.seconds)
                    }
                }
            }
        }

        // 启动 reflog 监控任务
        if (config.enableReflogMonitoring) {
            scope.launch {
                delay(config.initialDelaySeconds.seconds) // 初始延迟

                while (isActive && isRunning) {
                    try {
                        checkReflogChange()
                        delay(config.reflogCheckIntervalSeconds.seconds)
                    } catch (e: Exception) {
                        logger.error(e) { "监控 Git reflog 时出错" }
                        delay(config.retryDelaySeconds.seconds)
                    }
                }
            }
        }
    }

    /**
     * 停止监控
     */
    fun stop() {
        isRunning = false
        logger.info { "停止监控 Git 分支: $repositoryPath" }
    }

    /**
     * 检查分支变更
     */
    private suspend fun checkBranchChange() {
        val newBranch = getCurrentBranch()

        if (newBranch != null && currentBranch != null && newBranch != currentBranch) {
            emitBranchChangeEvent(currentBranch!!, newBranch)
            currentBranch = newBranch
        } else if (newBranch != null && currentBranch == null) {
            // 首次检测到分支
            currentBranch = newBranch
            logger.debug { "检测到 Git 分支: $newBranch" }
        }
    }

    /**
     * 检查 HEAD 文件变更
     */
    private suspend fun checkHeadFileChange() {
        if (!config.enableFastBranchDetection) {
            return
        }

        try {
            val headFile = repositoryPath.resolve(".git/HEAD").toFile()
            if (!headFile.exists()) {
                return
            }

            val lastModified = headFile.lastModified()
            if (lastModified > lastHeadModified) {
                // HEAD 文件已更新，可能发生了分支切换
                lastHeadModified = lastModified

                // 检查分支是否变更
                val newBranch = getCurrentBranch()
                if (newBranch != null && currentBranch != null && newBranch != currentBranch) {
                    emitBranchChangeEvent(currentBranch!!, newBranch)
                    currentBranch = newBranch
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "检查 HEAD 文件变更时出错" }
        }
    }

    /**
     * 更新 HEAD 文件时间戳
     */
    private fun updateHeadFileTimestamp() {
        try {
            val headFile = repositoryPath.resolve(".git/HEAD").toFile()
            if (headFile.exists()) {
                lastHeadModified = headFile.lastModified()
            }
        } catch (e: Exception) {
            logger.error(e) { "更新 HEAD 文件时间戳时出错" }
        }
    }

    /**
     * 检查 reflog 变更
     */
    private suspend fun checkReflogChange() {
        try {
            val latestEntry = getLatestReflogEntry()
            if (latestEntry != null && lastReflogEntry != null && latestEntry != lastReflogEntry) {
                // reflog 已更新，可能发生了分支切换
                lastReflogEntry = latestEntry

                // 检查分支是否变更
                val newBranch = getCurrentBranch()
                if (newBranch != null && currentBranch != null && newBranch != currentBranch) {
                    emitBranchChangeEvent(currentBranch!!, newBranch)
                    currentBranch = newBranch
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "检查 reflog 变更时出错" }
        }
    }

    /**
     * 获取最新的 reflog 条目
     */
    private fun getLatestReflogEntry(): String? {
        try {
            val repo = repository ?: return null
            Git(repo).use { git ->
                val reflog = git.reflog().call()
                return reflog.firstOrNull()?.toString()
            }
        } catch (e: Exception) {
            logger.error(e) { "获取最新 reflog 条目时出错" }
            return null
        }
    }

    /**
     * 发送分支变更事件
     */
    private suspend fun emitBranchChangeEvent(previousBranch: String, currentBranch: String) {
        val event = GitBranchChangeEvent(
            repositoryPath = repositoryPath,
            previousBranch = previousBranch,
            currentBranch = currentBranch
        )

        _branchChanges.emit(event)
        logger.info { "Git 分支变更: $previousBranch -> $currentBranch" }
    }

    /**
     * 获取当前分支
     */
    private fun getCurrentBranch(): String? {
        try {
            val repo = repository
            if (repo == null) {
                // 如果仓库未初始化或已关闭，尝试重新初始化
                repository = initRepository()
                if (repository == null) {
                    return null
                }
            }

            Git(repository!!).use { git ->
                val branch = git.repository.branch
                return branch
            }
        } catch (e: Exception) {
            logger.error(e) { "获取当前 Git 分支时出错" }
            return null
        }
    }

    /**
     * 关闭监控器
     */
    override fun close() {
        stop()
        repository?.close()
        repository = null
    }
}
