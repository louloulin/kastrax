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
 */
data class GitBranchMonitorConfig(
    val pollIntervalSeconds: Long = 5
)

/**
 * Git 分支监控器
 *
 * 监控 Git 仓库的分支变更
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
    private val _branchChanges = MutableSharedFlow<GitBranchChangeEvent>(extraBufferCapacity = 10)
    val branchChanges: SharedFlow<GitBranchChangeEvent> = _branchChanges
    
    // 当前分支
    private var currentBranch: String? = null
    
    // 监控任务是否运行
    private var isRunning = false
    
    /**
     * 启动监控
     */
    fun start() {
        if (isRunning) {
            return
        }
        
        isRunning = true
        logger.info { "开始监控 Git 分支: $repositoryPath" }
        
        // 初始化当前分支
        currentBranch = getCurrentBranch()
        logger.debug { "初始分支: $currentBranch" }
        
        // 启动监控任务
        scope.launch {
            while (isActive && isRunning) {
                try {
                    checkBranchChange()
                    delay(config.pollIntervalSeconds.seconds)
                } catch (e: Exception) {
                    logger.error(e) { "监控 Git 分支时出错" }
                    delay(config.pollIntervalSeconds.seconds)
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
            val event = GitBranchChangeEvent(
                repositoryPath = repositoryPath,
                previousBranch = currentBranch!!,
                currentBranch = newBranch
            )
            
            _branchChanges.emit(event)
            logger.info { "Git 分支变更: ${currentBranch} -> ${newBranch}" }
            
            currentBranch = newBranch
        } else if (newBranch != null && currentBranch == null) {
            // 首次检测到分支
            currentBranch = newBranch
            logger.debug { "检测到 Git 分支: $newBranch" }
        }
    }
    
    /**
     * 获取当前分支
     */
    private fun getCurrentBranch(): String? {
        try {
            val builder = FileRepositoryBuilder()
            val repository: Repository = builder.setGitDir(repositoryPath.resolve(".git").toFile())
                .readEnvironment()
                .findGitDir()
                .build()
            
            Git(repository).use { git ->
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
    }
}
