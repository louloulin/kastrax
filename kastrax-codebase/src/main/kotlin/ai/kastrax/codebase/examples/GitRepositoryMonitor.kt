package ai.kastrax.codebase.examples

import ai.kastrax.codebase.filesystem.FileChangeEvent
import ai.kastrax.codebase.filesystem.FileChangeType
import ai.kastrax.codebase.filesystem.FileSystemMonitor
import ai.kastrax.codebase.filesystem.FileSystemMonitorConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * Git仓库监控器示例程序
 */
object GitRepositoryMonitor {
    private val logger = KotlinLogging.logger {}
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val branchChanges = AtomicInteger(0)
    private val fileChanges = AtomicInteger(0)
    private val currentBranch = ConcurrentHashMap<Path, String>()

    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 获取Git仓库路径
        val repoPath = if (args.isNotEmpty()) {
            Paths.get(args[0])
        } else {
            Paths.get("").toAbsolutePath()
        }

        // 检查是否是Git仓库
        val gitDir = repoPath.resolve(".git")
        if (!gitDir.exists() || !gitDir.isDirectory()) {
            println("错误: 指定的路径不是Git仓库: $repoPath")
            return@runBlocking
        }

        println("开始监控Git仓库: $repoPath")

        // 获取当前分支
        val headFile = gitDir.resolve("HEAD")
        val initialBranch = getCurrentBranch(headFile)
        currentBranch[repoPath] = initialBranch
        println("当前分支: $initialBranch")

        // 创建文件系统监控器配置
        val config = FileSystemMonitorConfig(
            pollIntervalMs = 100, // 更快的轮询间隔，用于演示
            eventThrottleMs = 50, // 更短的节流时间，用于演示
            batchProcessingIntervalMs = 50, // 更短的批处理间隔，用于演示
            adaptiveBatchSizing = true, // 启用自适应批处理大小
            minBatchSize = 5, // 最小批处理大小
            maxBatchSize = 50, // 最大批处理大小
            adaptiveThrottling = true, // 启用自适应节流
            minThrottleMs = 10, // 最小节流时间
            maxThrottleMs = 100, // 最大节流时间
            enableGitIntegration = true, // 启用Git集成
            gitIntegrationIntervalMs = 500, // Git集成检查间隔
            excludePatterns = setOf(
                Regex("\\.git/objects/.*"), Regex("\\.git/refs/remotes/.*"), Regex("\\.git/logs/.*"),
                Regex(".*\\.pack"), Regex(".*\\.idx"), Regex(".*\\.lock")
            ) // 排除模式
        )

        // 创建文件系统监控器
        val monitor = FileSystemMonitor(repoPath, config)

        // 启动文件系统监控器
        monitor.start()
        println("Git仓库监控器已启动")

        // 收集文件变更事件
        val job = scope.launch {
            monitor.fileChanges.collect { event ->
                handleGitEvent(event, gitDir)
            }
        }

        // 等待用户输入退出
        println("正在监控Git仓库变更，按回车键退出...")
        readlnOrNull()

        // 关闭资源
        monitor.close()
        job.cancel()

        // 输出统计信息
        println("=== Git仓库监控统计 ===")
        println("分支变更次数: ${branchChanges.get()}")
        println("文件变更次数: ${fileChanges.get()}")
        println("当前分支: ${currentBranch[repoPath]}")
        println("========================")
    }

    /**
     * 处理Git事件
     */
    private fun handleGitEvent(event: FileChangeEvent, gitDir: Path) {
        val path = event.path
        val repoRoot = gitDir.parent

        // 检查是否是HEAD文件变更（分支切换）
        if (path.name == "HEAD" && path.parent == gitDir && event.type == FileChangeType.MODIFY) {
            val newBranch = getCurrentBranch(path)
            val oldBranch = currentBranch.put(repoRoot, newBranch)

            if (newBranch != oldBranch) {
                branchChanges.incrementAndGet()
                println("分支变更: $oldBranch -> $newBranch")
            }
        }
        // 检查是否是工作目录文件变更
        else if (!path.startsWith(gitDir)) {
            fileChanges.incrementAndGet()
            logger.debug { "文件变更: ${event.type} - ${event.path}" }
        }
    }

    /**
     * 获取当前分支
     */
    private fun getCurrentBranch(headFile: Path): String {
        if (!headFile.exists()) {
            return "unknown"
        }

        val headContent = headFile.readText().trim()
        return if (headContent.startsWith("ref: refs/heads/")) {
            headContent.removePrefix("ref: refs/heads/")
        } else {
            // 分离头指针状态
            "detached-${headContent.take(7)}"
        }
    }
}
