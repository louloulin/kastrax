package ai.kastrax.code.indexing

import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.codebase.git.GitBranchChangeEvent
import ai.kastrax.codebase.git.GitBranchMonitorConfig
import ai.kastrax.codebase.git.GitBranchMonitor as CodebaseGitBranchMonitor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.BranchChangeListener
import git4idea.branch.GitBranchUtil
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
 * Git分支监控器
 *
 * 监控Git分支变更，发送分支变更事件
 */
class GitBranchMonitor(
    private val project: Project,
    private val config: GitBranchMonitorConfig = GitBranchMonitorConfig()
) : KastraXCodeBase(component = "GIT_BRANCH_MONITOR") {

    // 使用 KastraXCodeBase 的 logger

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isRunning = AtomicBoolean(false)

    // 分支变更事件流
    private val _branchChanges = MutableSharedFlow<GitBranchChangeEvent>(extraBufferCapacity = 10)
    val branchChanges: SharedFlow<GitBranchChangeEvent> = _branchChanges.asSharedFlow()

    // 代码库Git分支监控器
    private var codebaseGitBranchMonitor: CodebaseGitBranchMonitor? = null

    // 当前分支
    private var currentBranch: String? = null

    // IntelliJ 分支变更监听器
    private val intellijBranchChangeListener = object : BranchChangeListener {
        override fun branchWillChange(branchName: String) {
            // 分支即将变更，不处理
        }

        override fun branchHasChanged(branchName: String) {
            handleBranchChange(branchName)
        }
    }

    /**
     * 启动监控器
     *
     * @param rootPath 代码库根路径
     */
    fun start(rootPath: Path = Paths.get(project.basePath ?: "")) {
        if (isRunning.getAndSet(true)) {
            logger.warn { "Git分支监控器已经在运行" }
            return
        }

        logger.info { "启动Git分支监控器: $rootPath" }

        try {
            // 创建代码库Git分支监控器
            codebaseGitBranchMonitor = CodebaseGitBranchMonitor(rootPath, config)

            // 监听代码库Git分支监控器的分支变更事件
            scope.launch {
                codebaseGitBranchMonitor?.branchChanges?.collect { event ->
                    _branchChanges.emit(event)
                }
            }

            // 启动代码库Git分支监控器
            codebaseGitBranchMonitor?.start()

            // 获取当前分支
            currentBranch = getCurrentBranch()

            // 注册 IntelliJ 分支变更监听器
            project.messageBus.connect().subscribe(BranchChangeListener.VCS_BRANCH_CHANGED, intellijBranchChangeListener)
        } catch (e: Exception) {
            logger.error(e) { "启动Git分支监控器时出错" }
            isRunning.set(false)
        }
    }

    /**
     * 停止监控器
     */
    fun stop() {
        if (!isRunning.getAndSet(false)) {
            logger.warn { "Git分支监控器已经停止" }
            return
        }

        logger.info { "停止Git分支监控器" }

        try {
            // 停止代码库Git分支监控器
            codebaseGitBranchMonitor?.stop()
        } catch (e: Exception) {
            logger.error(e) { "停止Git分支监控器时出错" }
        }
    }

    /**
     * 处理分支变更
     *
     * @param newBranch 新分支
     */
    private fun handleBranchChange(newBranch: String) {
        if (!isRunning.get()) {
            return
        }

        val oldBranch = currentBranch
        currentBranch = newBranch

        logger.info { "Git分支变更: $oldBranch -> $newBranch" }

        // 创建分支变更事件
        val branchChangeEvent = GitBranchChangeEvent(
            repositoryPath = Paths.get(project.basePath ?: ""),
            previousBranch = oldBranch ?: "",
            currentBranch = newBranch
        )

        // 发送分支变更事件
        scope.launch {
            _branchChanges.emit(branchChangeEvent)
        }
    }

    /**
     * 获取当前分支
     *
     * @return 当前分支名称
     */
    private fun getCurrentBranch(): String? {
        return try {
            // 使用 git4idea 获取当前分支
            // 注意：由于 git4idea 依赖问题，暂时返回 null
            // 实际实现应该使用 GitBranchUtil.getCurrentRepository(project)?.currentBranch?.name
            null
        } catch (e: Exception) {
            logger.error(e) { "获取当前分支时出错" }
            null
        }
    }
}
