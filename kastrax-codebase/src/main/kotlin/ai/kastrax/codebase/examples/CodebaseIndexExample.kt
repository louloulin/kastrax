package ai.kastrax.codebase.examples

import ai.kastrax.codebase.CodebaseIndexEvent
import ai.kastrax.codebase.CodebaseIndexManager
import ai.kastrax.codebase.CodebaseIndexManagerConfig
import ai.kastrax.codebase.CodebaseIndexStatus
import ai.kastrax.codebase.filesystem.FileFilterConfig
import ai.kastrax.codebase.filesystem.FileSystemMonitorConfig
import ai.kastrax.codebase.git.GitBranchMonitorConfig
import ai.kastrax.codebase.indexing.BatchProcessorConfig
import ai.kastrax.codebase.indexing.IncrementalIndexerConfig
import ai.kastrax.codebase.indexing.SimpleIndexTaskProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds

/**
 * 代码库索引示例
 */
object CodebaseIndexExample {
    
    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 获取要索引的目录路径
        val directoryPath = if (args.isNotEmpty()) {
            Path(args[0])
        } else {
            // 默认使用当前目录
            Path(".")
        }
        
        println("开始索引目录: $directoryPath")
        
        // 创建索引管理器配置
        val config = CodebaseIndexManagerConfig(
            fileSystemMonitorConfig = FileSystemMonitorConfig(
                pollIntervalMs = 500 // 更快的轮询间隔，用于演示
            ),
            gitBranchMonitorConfig = GitBranchMonitorConfig(
                pollIntervalSeconds = 2 // 更快的轮询间隔，用于演示
            ),
            fileFilterConfig = FileFilterConfig(
                // 使用默认配置
            ),
            incrementalIndexerConfig = IncrementalIndexerConfig(
                batchSize = 50, // 较小的批处理大小，用于演示
                deduplicationWindowMs = 500 // 较短的去重窗口，用于演示
            ),
            batchProcessorConfig = BatchProcessorConfig(
                maxConcurrentBatches = 2 // 较少的并发批处理，用于演示
            ),
            enableGitMonitoring = true,
            userId = "demo-user" // 用于演示的用户ID
        )
        
        // 创建简单的索引任务处理器
        val indexTaskProcessor = SimpleIndexTaskProcessor()
        
        // 创建索引管理器
        val indexManager = CodebaseIndexManager(
            rootPath = directoryPath,
            config = config,
            indexTaskProcessor = indexTaskProcessor
        )
        
        // 创建协程作用域
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        
        // 监听索引事件
        val eventJob = scope.launch {
            indexManager.indexEvents.collect { event ->
                when (event) {
                    is CodebaseIndexEvent.StatusChanged -> {
                        println("索引状态变更: ${event.status}${event.message?.let { ", $it" } ?: ""}")
                        
                        // 如果索引就绪，则打印消息
                        if (event.status == CodebaseIndexStatus.READY) {
                            println("索引已就绪，可以开始使用")
                        }
                    }
                    is CodebaseIndexEvent.Progress -> {
                        val percentage = if (event.total > 0) {
                            (event.current.toDouble() / event.total.toDouble() * 100).toInt()
                        } else {
                            0
                        }
                        println("索引进度: $percentage% ($${event.current}/${event.total})${event.message?.let { ", $it" } ?: ""}")
                    }
                    is CodebaseIndexEvent.Error -> {
                        println("索引错误: ${event.error}${if (event.fatal) " (致命)" else ""}")
                    }
                }
            }
        }
        
        try {
            // 启动索引管理器
            indexManager.start()
            
            // 等待一段时间，让索引进行
            println("等待索引进行中...")
            delay(30.seconds)
            
            // 请求重新索引
            println("请求重新索引...")
            indexManager.requestReindex()
            
            // 再等待一段时间
            delay(10.seconds)
            
            // 获取当前状态
            val status = indexManager.getStatus()
            println("当前索引状态: $status")
            
        } finally {
            // 停止索引管理器
            indexManager.stop()
            
            // 取消事件监听
            eventJob.cancel()
            
            println("索引示例完成")
        }
    }
}
