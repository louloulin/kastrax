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
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.time.Duration.Companion.seconds

/**
 * 自适应文件系统监控器演示程序
 */
object AdaptiveMonitorDemo {
    private val logger = KotlinLogging.logger {}
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val monitoredFiles = ConcurrentHashMap<Path, Long>()
    private val eventCount = AtomicInteger(0)
    private val createCount = AtomicInteger(0)
    private val modifyCount = AtomicInteger(0)
    private val deleteCount = AtomicInteger(0)

    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 获取当前项目路径或用户指定的路径
        val monitorPath = if (args.isNotEmpty()) {
            Paths.get(args[0])
        } else {
            Paths.get("").toAbsolutePath()
        }

        println("开始监控目录: $monitorPath")

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
            enableWatcherLoadBalancing = true, // 启用监控器负载均衡
            loadBalancingIntervalMs = 500, // 负载均衡间隔
            enableDirectoryPrioritization = true, // 启用目录优先级
            highPriorityDirectories = setOf("src", "main", "kotlin", "java"), // 高优先级目录
            excludePatterns = setOf(
                Regex("\\.git/.*"), Regex("\\.idea/.*"), Regex("build/.*"), Regex("out/.*"),
                Regex("target/.*"), Regex("node_modules/.*"), Regex("\\.gradle/.*"),
                Regex(".*\\.class"), Regex(".*\\.jar"), Regex(".*\\.war"), Regex(".*\\.zip")
            ) // 排除模式
        )

        // 创建文件系统监控器
        val monitor = FileSystemMonitor(monitorPath, config)

        // 启动文件系统监控器
        monitor.start()
        println("文件系统监控器已启动")

        // 收集文件变更事件
        val job = scope.launch {
            monitor.fileChanges.collect { event ->
                handleFileChangeEvent(event)

                // 每收集100个事件打印一次统计信息
                if (eventCount.incrementAndGet() % 100 == 0) {
                    printStats()
                }
            }
        }

        // 启动性能监控
        val statsJob = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000) // 每5秒打印一次监控器状态
                println("监控器状态:")
                println("- 当前批处理大小: ${config.batchSize}")
                println("- 当前节流时间: ${config.eventThrottleMs}ms")
                println("- 活跃监控器数量: (无法访问)")
                println("- 事件队列大小: ${eventCount.get()}")
                println("- 已处理事件总数: ${eventCount.get()}")
                println("- 创建事件: ${createCount.get()}")
                println("- 修改事件: ${modifyCount.get()}")
                println("- 删除事件: ${deleteCount.get()}")
                println("- 监控文件数: ${monitoredFiles.size}")
                println()
            }
        }

        // 等待用户输入退出
        println("正在监控文件变更，按回车键退出...")
        readlnOrNull()

        // 关闭资源
        monitor.close()
        job.cancel()
        statsJob.cancel()

        // 输出最终统计信息
        printStats()
    }

    /**
     * 处理文件变更事件
     */
    private fun handleFileChangeEvent(event: FileChangeEvent) {
        val path = event.path

        // 忽略目录事件
        if (path.isDirectory()) {
            return
        }

        // 只处理常规文件
        if (!path.isRegularFile()) {
            return
        }

        // 根据事件类型更新统计信息
        when (event.type) {
            FileChangeType.CREATE -> {
                monitoredFiles[path] = event.timestamp
                createCount.incrementAndGet()
            }
            FileChangeType.MODIFY -> {
                monitoredFiles[path] = event.timestamp
                modifyCount.incrementAndGet()
            }
            FileChangeType.DELETE -> {
                monitoredFiles.remove(path)
                deleteCount.incrementAndGet()
            }
            else -> {}
        }

        // 记录事件
        logger.debug { "文件变更: ${event.type} - ${event.path}" }
    }

    /**
     * 打印统计信息
     */
    private fun printStats() {
        println("=== 文件监控统计 ===")
        println("总事件数: ${eventCount.get()}")
        println("创建事件: ${createCount.get()}")
        println("修改事件: ${modifyCount.get()}")
        println("删除事件: ${deleteCount.get()}")
        println("监控文件数: ${monitoredFiles.size}")
        println("=====================")
    }
}
