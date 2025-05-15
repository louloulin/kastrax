package ai.kastrax.codebase.indexing.distributed

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.fromProducer
import actor.proto.spawn
import ai.kastrax.codebase.indexing.IndexTask
import ai.kastrax.codebase.indexing.IndexTaskProcessor
import ai.kastrax.codebase.indexing.IndexTaskStatus
import ai.kastrax.codebase.indexing.IndexTaskType
import ai.kastrax.codebase.indexing.IncrementalIndexTask
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/**
 * 索引任务处理器消息
 */
sealed class IndexTaskProcessorMessage {
    /**
     * 处理任务消息
     *
     * @property task 索引任务
     */
    data class ProcessTask(val task: IncrementalIndexTask) : IndexTaskProcessorMessage()

    /**
     * 批量处理任务消息
     *
     * @property tasks 索引任务列表
     */
    data class ProcessBatchTasks(val tasks: List<IncrementalIndexTask>) : IndexTaskProcessorMessage()

    /**
     * 任务状态更新消息
     *
     * @property taskId 任务ID
     * @property status 任务状态
     * @property error 错误信息
     */
    data class TaskStatusUpdate(
        val taskId: String,
        val status: IndexTaskStatus,
        val error: String? = null
    ) : IndexTaskProcessorMessage()

    /**
     * 获取状态消息
     */
    object GetStatus : IndexTaskProcessorMessage()

    /**
     * 状态响应消息
     *
     * @property activeTaskCount 活动任务数量
     * @property completedTaskCount 已完成任务数量
     * @property failedTaskCount 失败任务数量
     */
    data class StatusResponse(
        val activeTaskCount: Int,
        val completedTaskCount: Int,
        val failedTaskCount: Int
    ) : IndexTaskProcessorMessage()
}

/**
 * 索引任务处理器配置
 *
 * @property maxConcurrentTasks 最大并发任务数
 * @property taskTimeout 任务超时时间
 * @property maxRetries 最大重试次数
 * @property batchSize 批处理大小
 */
data class IndexTaskProcessorConfig(
    val maxConcurrentTasks: Int = 10,
    val taskTimeout: Duration = 60.seconds,
    val maxRetries: Int = 3,
    val batchSize: Int = 100
)

/**
 * 基于 Actor 的索引任务处理器
 *
 * 使用 Actor 模型实现的索引任务处理器，支持高效处理大量索引任务
 *
 * @property actorSystem Actor 系统
 * @property documentStore 文档向量存储
 * @property embeddingService 嵌入服务
 * @property config 配置
 */
class ActorBasedIndexTaskProcessor(
    private val actorSystem: ActorSystem,
    private val documentStore: DocumentVectorStore,
    private val embeddingService: EmbeddingService,
    private val config: IndexTaskProcessorConfig = IndexTaskProcessorConfig()
) : IndexTaskProcessor {

    // 文件路径到文档 ID 的映射
    private val pathToId = ConcurrentHashMap<String, String>()

    // 处理器 Actor PID
    private lateinit var processorPid: PID

    // 事件流
    private val _events = MutableSharedFlow<IndexTaskProcessorMessage.TaskStatusUpdate>(extraBufferCapacity = 100)
    val events: SharedFlow<IndexTaskProcessorMessage.TaskStatusUpdate> = _events.asSharedFlow()

    // 活动任务计数
    private val activeTaskCount = AtomicInteger(0)

    // 统计信息
    private var completedTaskCount = 0
    private var failedTaskCount = 0

    // 是否已启动
    private val isStarted = AtomicBoolean(false)

    /**
     * 启动处理器
     */
    fun start() {
        if (isStarted.getAndSet(true)) {
            logger.info("索引任务处理器已经启动")
            return
        }

        logger.info("启动索引任务处理器")

        // 创建处理器 Actor
        val props = fromProducer { ProcessorActor() }
        processorPid = actorSystem.root.spawnNamed(props, "processor")
    }

    /**
     * 停止处理器
     */
    suspend fun stop() {
        if (!isStarted.getAndSet(false)) {
            logger.info("索引任务处理器尚未启动")
            return
        }

        logger.info("停止索引任务处理器")

        // 停止 Actor
        actorSystem.stop(processorPid)
    }

    /**
     * 处理索引任务
     *
     * @param task 索引任务
     */
    override suspend fun processTask(task: IncrementalIndexTask) = withContext(Dispatchers.Default) {
        // 检查是否已启动
        if (!isStarted.get()) {
            logger.info("索引任务处理器尚未启动")
            return@withContext
        }

        logger.debug { "提交索引任务: ${task.id}, 类型: ${task.type}, 路径: ${task.path}" }

        // 发送处理任务消息
        actorSystem.send(processorPid, IndexTaskProcessorMessage.ProcessTask(task))
    }

    /**
     * 批量处理任务
     *
     * @param tasks 任务列表
     */
    suspend fun processBatchTasks(tasks: List<IncrementalIndexTask>) = withContext(Dispatchers.Default) {
        // 检查是否已启动
        if (!isStarted.get()) {
            logger.info("索引任务处理器尚未启动")
            return@withContext
        }

        logger.debug { "批量提交索引任务: ${tasks.size} 个任务" }

        // 发送批量处理任务消息
        actorSystem.send(processorPid, IndexTaskProcessorMessage.ProcessBatchTasks(tasks))
    }

    /**
     * 获取状态
     *
     * @return 状态响应
     */
    suspend fun getStatus(): IndexTaskProcessorMessage.StatusResponse = withContext(Dispatchers.Default) {
        // 检查是否已启动
        if (!isStarted.get()) {
            logger.info("索引任务处理器尚未启动")
            return@withContext IndexTaskProcessorMessage.StatusResponse(0, 0, 0)
        }

        try {
            // 发送获取状态消息
            return@withContext actorSystem.requestAsync<IndexTaskProcessorMessage.StatusResponse>(
                processorPid,
                IndexTaskProcessorMessage.GetStatus,
                java.time.Duration.ofSeconds(5)
            )
        } catch (e: Exception) {
            logger.error("获取状态时出错: ${e.message}")
            return@withContext IndexTaskProcessorMessage.StatusResponse(0, 0, 0)
        }
    }

    /**
     * 处理器 Actor
     */
    inner class ProcessorActor : Actor {
        // 活动任务
        private val activeTasks = ConcurrentHashMap<String, IncrementalIndexTask>()

        // 任务重试计数
        private val taskRetries = ConcurrentHashMap<String, Int>()

        /**
         * 接收消息
         */
        override suspend fun Context.receive(msg: Any) {
            when (msg) {
                is IndexTaskProcessorMessage.ProcessTask -> handleProcessTask(msg.task)
                is IndexTaskProcessorMessage.ProcessBatchTasks -> handleProcessBatchTasks(msg.tasks)
                is IndexTaskProcessorMessage.TaskStatusUpdate -> handleTaskStatusUpdate(msg.taskId, msg.status, msg.error)
                is IndexTaskProcessorMessage.GetStatus -> handleGetStatus()
                "started" -> {
                    logger.info("索引任务处理器 Actor 已启动")
                }
                else -> logger.info("未知消息类型: ${msg::class.simpleName}")
            }
        }

        /**
         * 处理任务
         *
         * @param task 任务
         */
        private suspend fun Context.handleProcessTask(task: IncrementalIndexTask) {
            // 检查是否有可用容量
            if (activeTaskCount.get() >= config.maxConcurrentTasks) {
                logger.info("处理器容量已满，拒绝任务: ${task.id}")
                return
            }

            // 添加到活动任务
            activeTasks[task.id] = task
            activeTaskCount.incrementAndGet()

            // 异步处理任务
            val taskProps = fromProducer { TaskProcessorActor(task) }
            spawnNamed(taskProps, "task-${task.id}")
        }

        /**
         * 批量处理任务
         *
         * @param tasks 任务列表
         */
        private suspend fun Context.handleProcessBatchTasks(tasks: List<IncrementalIndexTask>) {
            // 分批处理任务
            val batches = tasks.chunked(config.batchSize)

            for (batch in batches) {
                for (task in batch) {
                    handleProcessTask(task)
                }
            }
        }

        /**
         * 处理任务状态更新
         *
         * @param taskId 任务ID
         * @param status 状态
         * @param error 错误信息
         */
        private suspend fun Context.handleTaskStatusUpdate(taskId: String, status: IndexTaskStatus, error: String?) {
            val task = activeTasks[taskId]
            if (task == null) {
                logger.info("任务不存在: $taskId")
                return
            }

            when (status) {
                IndexTaskStatus.COMPLETED -> {
                    // 任务完成
                    logger.debug("任务完成: $taskId")

                    // 更新统计信息
                    completedTaskCount++

                    // 从活动任务中移除
                    activeTasks.remove(taskId)
                    activeTaskCount.decrementAndGet()

                    // 清除重试计数
                    taskRetries.remove(taskId)
                }
                IndexTaskStatus.FAILED -> {
                    // 任务失败
                    logger.error("任务失败: $taskId, 错误: $error")

                    // 检查是否需要重试
                    val retryCount = taskRetries.getOrDefault(taskId, 0)
                    if (retryCount < config.maxRetries) {
                        // 增加重试计数
                        taskRetries[taskId] = retryCount + 1

                        logger.info("任务失败，重试: $taskId, 重试次数: ${retryCount + 1}")

                        // 重试任务
                        handleProcessTask(task)
                    } else {
                        // 超过最大重试次数
                        logger.error("任务失败，超过最大重试次数: $taskId")

                        // 更新统计信息
                        failedTaskCount++

                        // 从活动任务中移除
                        activeTasks.remove(taskId)
                        activeTaskCount.decrementAndGet()

                        // 清除重试计数
                        taskRetries.remove(taskId)
                    }
                }
                else -> {
                    // 其他状态
                    logger.debug("任务状态更新: $taskId, 状态: $status")
                }
            }

            // 发送事件
            _events.emit(IndexTaskProcessorMessage.TaskStatusUpdate(taskId, status, error))
        }

        /**
         * 处理获取状态
         */
        private suspend fun Context.handleGetStatus() {
            val status = IndexTaskProcessorMessage.StatusResponse(
                activeTaskCount = activeTaskCount.get(),
                completedTaskCount = completedTaskCount,
                failedTaskCount = failedTaskCount
            )

            respond(status)
        }
    }

    /**
     * 任务处理器 Actor
     *
     * @property task 任务
     */
    inner class TaskProcessorActor(private val task: IncrementalIndexTask) : Actor {
        /**
         * 接收消息
         */
        override suspend fun Context.receive(msg: Any) {
            when (msg) {
                "start" -> processTask()
                else -> logger.info("未知消息类型: ${msg::class.simpleName}")
            }
        }

        /**
         * 处理任务
         */
        private suspend fun Context.processTask() {
            try {
                // 使用超时机制执行任务
                withTimeout(config.taskTimeout.inWholeMilliseconds) {
                    when (task.type) {
                        IndexTaskType.ADD, IndexTaskType.UPDATE -> {
                            processAddOrUpdateTask(task.path)
                        }
                        IndexTaskType.DELETE -> {
                            processDeleteTask(task.path)
                        }
                        IndexTaskType.BRANCH_CHANGE -> {
                            processBranchChangeTask(task)
                        }
                        IndexTaskType.FULL_REINDEX -> {
                            processFullReindexTask(task.path)
                        }
                    }
                }

                // 发送任务完成消息
                send(
                    processorPid,
                    IndexTaskProcessorMessage.TaskStatusUpdate(
                        taskId = task.id,
                        status = IndexTaskStatus.COMPLETED
                    )
                )
            } catch (e: Exception) {
                logger.error("处理任务失败: ${task.id}", e)

                // 发送任务失败消息
                send(
                    processorPid,
                    IndexTaskProcessorMessage.TaskStatusUpdate(
                        taskId = task.id,
                        status = IndexTaskStatus.FAILED,
                        error = e.message
                    )
                )
            } finally {
                // 停止 Actor
                actorSystem.stop(self)
            }
        }
    }

    /**
     * 处理添加或更新任务
     *
     * @param path 文件路径
     */
    private suspend fun processAddOrUpdateTask(path: Path) {
        try {
            // 检查文件是否存在且是常规文件
            if (!path.isRegularFile()) {
                logger.info("文件不存在或不是常规文件: $path")
                return
            }

            // 读取文件内容
            val content = path.readText()

            // 创建文档元数据
            val metadata = createFileMetadata(path)

            // 检查文件是否已存在
            val pathString = path.toString()
            val existingId = pathToId[pathString]

            if (existingId != null) {
                // 更新文档
                updateDocument(existingId, content, metadata)
            } else {
                // 添加文档
                addDocument(pathString, content, metadata)
            }
        } catch (e: Exception) {
            logger.error("处理文件时出错: $path", e)
            throw e
        }
    }

    /**
     * 处理删除任务
     *
     * @param path 文件路径
     */
    private suspend fun processDeleteTask(path: Path) {
        try {
            // 获取文件路径
            val pathString = path.toString()

            // 检查文件是否已索引
            val documentId = pathToId[pathString]
            if (documentId != null) {
                // 从向量存储中删除文档
                documentStore.deleteDocuments(listOf(documentId))

                // 从映射中删除
                pathToId.remove(pathString)

                logger.debug("从索引中删除文件: $path")
            } else {
                logger.info("文件未索引，无法删除: $path")
            }
        } catch (e: Exception) {
            logger.error("删除文件索引时出错: $path", e)
            throw e
        }
    }

    /**
     * 处理分支变更任务
     *
     * @param task 索引任务
     */
    private suspend fun processBranchChangeTask(task: IncrementalIndexTask) {
        try {
            val previousBranch = task.metadata["previousBranch"]
            val currentBranch = task.metadata["currentBranch"]

            logger.info { "处理分支变更: $previousBranch -> $currentBranch" }

            // 清空当前索引
            clearIndex()

            // 重新索引当前分支
            processFullReindexTask(task.path)
        } catch (e: Exception) {
            logger.error("处理分支变更时出错: ${task.path}", e)
            throw e
        }
    }

    /**
     * 处理完全重新索引任务
     *
     * @param rootPath 根路径
     */
    private suspend fun processFullReindexTask(rootPath: Path) {
        try {
            logger.info { "开始完全重新索引: $rootPath" }

            // 清空当前索引
            clearIndex()

            // 遍历所有文件并索引
            Files.walk(rootPath)
                .filter { it.isRegularFile() }
                .forEach { path ->
                    try {
                        // 使用协程上下文调用挂起函数
                        runBlocking {
                            processAddOrUpdateTask(path)
                        }
                    } catch (e: Exception) {
                        logger.error("索引文件时出错: $path", e)
                    }
                }

            logger.info("完全重新索引完成: $rootPath")
        } catch (e: Exception) {
            logger.error("完全重新索引时出错: $rootPath", e)
            throw e
        }
    }

    /**
     * 清空索引
     */
    private suspend fun clearIndex() {
        try {
            // 获取所有文档 ID
            val documentIds = pathToId.values.toList()

            // 从向量存储中删除所有文档
            if (documentIds.isNotEmpty()) {
                documentStore.deleteDocuments(documentIds)
            }

            // 清空映射
            pathToId.clear()

            logger.debug("清空索引")
        } catch (e: Exception) {
            logger.error("清空索引时出错", e)
            throw e
        }
    }

    /**
     * 添加文档
     *
     * @param pathString 文件路径字符串
     * @param content 文件内容
     * @param metadata 文件元数据
     */
    private suspend fun addDocument(pathString: String, content: String, metadata: Map<String, Any>) {
        // 创建文档
        val document = Document(
            id = UUID.randomUUID().toString(),
            content = content,
            metadata = metadata
        )

        // 添加文档到向量存储
        val success = documentStore.addDocuments(listOf(document), embeddingService)

        if (success) {
            // 添加到映射
            pathToId[pathString] = document.id
            logger.debug("添加文档: $pathString -> ${document.id}")
        } else {
            logger.info("添加文档失败: $pathString")
        }
    }

    /**
     * 更新文档
     *
     * @param documentId 文档 ID
     * @param content 文件内容
     * @param metadata 文件元数据
     */
    private suspend fun updateDocument(documentId: String, content: String, metadata: Map<String, Any>) {
        // 创建文档
        val document = Document(
            id = documentId,
            content = content,
            metadata = metadata
        )

        // 从向量存储中删除文档
        documentStore.deleteDocuments(listOf(documentId))

        // 添加文档到向量存储
        val success = documentStore.addDocuments(listOf(document), embeddingService)

        if (success) {
            logger.debug("更新文档: $documentId")
        } else {
            logger.info("更新文档失败: $documentId")
        }
    }

    /**
     * 创建文件元数据
     *
     * @param path 文件路径
     * @return 文件元数据
     */
    private fun createFileMetadata(path: Path): Map<String, Any> {
        val file = path.toFile()
        val extension = path.extension.lowercase()

        // 确定文件语言
        val language = determineLanguage(extension)

        return mapOf(
            "path" to path.toString(),
            "filename" to path.name,
            "extension" to extension,
            "language" to language,
            "size" to file.length(),
            "lastModified" to file.lastModified()
        )
    }

    /**
     * 确定文件语言
     *
     * @param extension 文件扩展名
     * @return 文件语言
     */
    private fun determineLanguage(extension: String): String {
        return when (extension) {
            "java" -> "Java"
            "kt", "kts" -> "Kotlin"
            "scala" -> "Scala"
            "groovy" -> "Groovy"
            "py" -> "Python"
            "js" -> "JavaScript"
            "ts" -> "TypeScript"
            "jsx" -> "React JSX"
            "tsx" -> "React TSX"
            "html" -> "HTML"
            "css" -> "CSS"
            "scss", "sass" -> "SASS"
            "less" -> "LESS"
            "c" -> "C"
            "cpp", "cc" -> "C++"
            "h", "hpp" -> "C/C++ Header"
            "cs" -> "C#"
            "go" -> "Go"
            "rs" -> "Rust"
            "php" -> "PHP"
            "rb" -> "Ruby"
            "swift" -> "Swift"
            "m", "mm" -> "Objective-C"
            "xml" -> "XML"
            "json" -> "JSON"
            "yaml", "yml" -> "YAML"
            "toml" -> "TOML"
            "properties" -> "Properties"
            "md" -> "Markdown"
            "txt" -> "Text"
            "rst" -> "reStructuredText"
            else -> "Unknown"
        }
    }
}
