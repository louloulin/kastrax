package ai.kastrax.codebase.examples

// TODO: 暂时注释掉Actor相关代码，等待kactor依赖问题解决

// 空实现以避免语法错误
class DistributedIndexSystemExample

/*
import actor.proto.ActorSystem
import ai.kastrax.codebase.embedding.FastEmbeddingService
import ai.kastrax.codebase.indexing.distributed.DistributedIndexSystemFactory
import ai.kastrax.codebase.indexing.distributed.IndexCoordinatorConfig
import ai.kastrax.codebase.indexing.distributed.IndexCoordinatorEvent
import ai.kastrax.codebase.store.CodeVectorStore
import ai.kastrax.codebase.util.TimeExtensions.milliseconds
import ai.kastrax.codebase.util.TimeExtensions.seconds
import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.adapter.DocumentVectorStoreAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * 分布式索引系统示例
 */
object DistributedIndexSystemExample {

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

        // 创建 Actor 系统
        val actorSystem = ActorSystem("distributed-index-example")

        // 创建嵌入服务
        val embeddingService = FastEmbeddingService.create()

        // 创建向量存储
        val vectorStore = VectorStoreFactory.createInMemoryVectorStore()

        // 创建文档向量存储适配器
        val documentStore = DocumentVectorStoreAdapter(
            vectorStore = vectorStore,
            indexName = "distributed_index_example",
            dimension = embeddingService.dimension
        )

        // 创建协调者配置
        val coordinatorConfig = IndexCoordinatorConfig(
            initialWorkerCount = Runtime.getRuntime().availableProcessors(), // 使用所有可用处理器
            taskAssignmentInterval = 500.milliseconds(), // 更快的任务分配以便演示
            workerStatusCheckInterval = 5.seconds() // 更快的工作者状态检查以便演示
        )

        // 创建分布式索引系统
        val indexSystem = DistributedIndexSystemFactory.createDistributedIndexSystem(
            documentStore = documentStore,
            embeddingService = embeddingService,
            rootPath = directoryPath,
            actorSystem = actorSystem,
            coordinatorConfig = coordinatorConfig
        )

        try {
            // 收集事件
            val events = mutableListOf<IndexCoordinatorEvent>()
            val job = launch {
                indexSystem.getEventFlow().take(100).toList(events)
            }

            // 等待一段时间，让系统初始化
            delay(2.seconds)

            // 获取系统状态
            val initialStatus = indexSystem.getStatus()
            println("初始系统状态:")
            println("- 待处理任务数量: ${initialStatus.pendingTaskCount}")
            println("- 活动任务数量: ${initialStatus.activeTaskCount}")
            println("- 已完成任务数量: ${initialStatus.completedTaskCount}")
            println("- 失败任务数量: ${initialStatus.failedTaskCount}")
            println("- 工作者数量: ${initialStatus.workerCount}")

            // 执行完全重新索引
            println("\n开始完全重新索引...")
            val task = ai.kastrax.codebase.indexing.IndexTask(
                type = ai.kastrax.codebase.indexing.IndexTaskType.FULL_REINDEX,
                path = directoryPath
            )
            indexSystem.submitTask(task)

            // 等待索引完成
            println("等待索引完成...")
            delay(30.seconds)

            // 获取系统状态
            val finalStatus = indexSystem.getStatus()
            println("\n最终系统状态:")
            println("- 待处理任务数量: ${finalStatus.pendingTaskCount}")
            println("- 活动任务数量: ${finalStatus.activeTaskCount}")
            println("- 已完成任务数量: ${finalStatus.completedTaskCount}")
            println("- 失败任务数量: ${finalStatus.failedTaskCount}")
            println("- 工作者数量: ${finalStatus.workerCount}")

            // 获取文档数量
            val documentCount = documentStore.getAllDocuments().size
            println("\n已索引文档数量: $documentCount")

            // 打印事件统计
            println("\n事件统计:")
            println("- 任务提交事件: ${events.count { it is IndexCoordinatorEvent.TaskSubmitted }}")
            println("- 任务分配事件: ${events.count { it is IndexCoordinatorEvent.TaskAssigned }}")
            println("- 任务完成事件: ${events.count { it is IndexCoordinatorEvent.TaskCompleted }}")
            println("- 任务失败事件: ${events.count { it is IndexCoordinatorEvent.TaskFailed }}")
            println("- 工作者注册事件: ${events.count { it is IndexCoordinatorEvent.WorkerRegistered }}")
            println("- 工作者注销事件: ${events.count { it is IndexCoordinatorEvent.WorkerUnregistered }}")

            // 取消事件收集
            job.cancel()

        } finally {
            // 关闭分布式索引系统
            indexSystem.stop()

            // 关闭 Actor 系统
            actorSystem.shutdown()

            println("分布式索引系统示例完成")
        }
    }
*/
