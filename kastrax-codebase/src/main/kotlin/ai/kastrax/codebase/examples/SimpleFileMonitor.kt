package ai.kastrax.codebase.examples

import ai.kastrax.codebase.filesystem.FileChangeEvent
import ai.kastrax.codebase.filesystem.FileChangeType
import ai.kastrax.codebase.filesystem.FileSystemMonitor
import ai.kastrax.codebase.filesystem.FileSystemMonitorConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.time.Duration.Companion.seconds

/**
 * 简单文件监控器示例
 * 
 * 这个示例展示了如何使用 FileSystemMonitor 监控文件变更
 */
object SimpleFileMonitor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val fileEvents = ConcurrentHashMap<Path, MutableList<FileChangeEvent>>()
    private val eventCount = AtomicInteger(0)
    
    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 获取监控路径
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
            excludePatterns = setOf(
                Regex("\\.git/.*"), Regex("\\.idea/.*"), Regex("build/.*"), 
                Regex("out/.*"), Regex("target/.*"), Regex("node_modules/.*")
            )
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
            }
        }
        
        // 启动统计信息打印任务
        val statsJob = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000) // 每5秒打印一次统计信息
                printStats()
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
        
        // 记录事件
        fileEvents.computeIfAbsent(path) { mutableListOf() }.add(event)
        eventCount.incrementAndGet()
        
        // 打印事件信息
        val eventType = when (event.type) {
            FileChangeType.CREATE -> "创建"
            FileChangeType.MODIFY -> "修改"
            FileChangeType.DELETE -> "删除"
        }
        
        println("文件${eventType}: ${event.path}")
    }
    
    /**
     * 打印统计信息
     */
    private fun printStats() {
        val createCount = fileEvents.values.flatten().count { it.type == FileChangeType.CREATE }
        val modifyCount = fileEvents.values.flatten().count { it.type == FileChangeType.MODIFY }
        val deleteCount = fileEvents.values.flatten().count { it.type == FileChangeType.DELETE }
        
        println("=== 文件监控统计 ===")
        println("总事件数: ${eventCount.get()}")
        println("监控文件数: ${fileEvents.size}")
        println("创建事件: $createCount")
        println("修改事件: $modifyCount")
        println("删除事件: $deleteCount")
        println("=====================")
    }
}
