package ai.kastrax.codebase.filesystem

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.seconds

/**
 * 文件系统监控器演示程序
 */
object FileSystemMonitorDemo {
    
    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 创建临时目录
        val tempDir = Files.createTempDirectory("kastrax-monitor-demo")
        println("创建临时目录: $tempDir")
        
        try {
            // 创建文件系统监控器配置
            val config = FileSystemMonitorConfig(
                pollIntervalMs = 100, // 更快的轮询间隔，用于演示
                eventThrottleMs = 50, // 更短的节流时间，用于演示
                batchProcessingIntervalMs = 50, // 更短的批处理间隔，用于演示
                refactoringThreshold = 5, // 更小的重构阈值，用于演示
                refactoringTimeWindowMs = 1000, // 更短的重构检测窗口，用于演示
                adaptiveBatchSizing = true, // 启用自适应批处理大小
                minBatchSize = 2, // 最小批处理大小
                maxBatchSize = 20, // 最大批处理大小
                adaptiveThrottling = true, // 启用自适应节流
                minThrottleMs = 10, // 最小节流时间
                maxThrottleMs = 100, // 最大节流时间
                enableWatcherLoadBalancing = true, // 启用监控器负载均衡
                loadBalancingIntervalMs = 500, // 负载均衡间隔
                enableDirectoryPrioritization = true, // 启用目录优先级
                highPriorityDirectories = setOf("src", "lib", "app") // 高优先级目录
            )
            
            // 创建文件系统监控器
            val monitor = FileSystemMonitor(tempDir, config)
            
            // 启动监控器
            monitor.start()
            println("启动文件系统监控器")
            
            // 收集事件
            val events = mutableListOf<FileChangeEvent>()
            val job = launch {
                withTimeout(10.seconds) {
                    monitor.fileChanges.take(5).toList(events)
                }
            }
            
            // 创建文件
            println("创建测试文件")
            val testFile = tempDir.resolve("test.txt")
            testFile.writeText("Hello, World!")
            
            // 创建目录
            println("创建测试目录")
            val testDir = tempDir.resolve("src")
            Files.createDirectory(testDir)
            
            // 在目录中创建文件
            println("在测试目录中创建文件")
            val testDirFile = testDir.resolve("test.txt")
            testDirFile.writeText("Hello from directory!")
            
            // 修改文件
            println("修改测试文件")
            testFile.writeText("Modified content")
            
            // 删除文件
            println("删除测试文件")
            Files.delete(testFile)
            
            // 等待事件收集完成
            job.join()
            
            // 输出收集到的事件
            println("收集到 ${events.size} 个事件:")
            events.forEachIndexed { index, event ->
                println("事件 ${index + 1}: ${event.type} - ${event.path}")
            }
            
            // 关闭监控器
            monitor.close()
            println("关闭文件系统监控器")
            
        } finally {
            // 删除临时目录
            deleteRecursively(tempDir)
            println("删除临时目录")
        }
    }
    
    /**
     * 递归删除目录
     */
    private fun deleteRecursively(path: Path) {
        if (Files.isDirectory(path)) {
            Files.list(path).forEach { deleteRecursively(it) }
        }
        Files.deleteIfExists(path)
    }
}
