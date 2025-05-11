package ai.kastrax.codebase.indexing.distributed

import actor.proto.ActorSystem
import ai.kastrax.codebase.CodebaseIndexManager
import ai.kastrax.codebase.CodebaseIndexManagerConfig
import ai.kastrax.codebase.filesystem.FileFilterConfig
import ai.kastrax.codebase.filesystem.FileSystemMonitorConfig
import ai.kastrax.codebase.git.GitBranchMonitorConfig
import ai.kastrax.codebase.indexing.BatchProcessorConfig
import ai.kastrax.codebase.indexing.IncrementalIndexerConfig
import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import java.nio.file.Path

/**
 * 分布式索引系统工厂
 *
 * 用于创建和配置分布式索引系统
 */
object DistributedIndexSystemFactory {
    
    /**
     * 创建分布式索引系统
     *
     * @param documentStore 文档向量存储
     * @param embeddingService 嵌入服务
     * @param rootPath 根路径
     * @param actorSystem Actor 系统，如果为 null，则创建新的 Actor 系统
     * @param coordinatorConfig 协调者配置
     * @return 分布式索引系统
     */
    suspend fun createDistributedIndexSystem(
        documentStore: DocumentVectorStore,
        embeddingService: EmbeddingService,
        rootPath: Path,
        actorSystem: ActorSystem? = null,
        coordinatorConfig: IndexCoordinatorConfig = IndexCoordinatorConfig()
    ): DistributedIndexSystem {
        // 创建索引任务处理器
        val taskProcessor = ActorBasedIndexTaskProcessor(
            documentStore = documentStore,
            embeddingService = embeddingService
        )
        
        // 创建分布式索引系统
        return DistributedIndexSystem.create(
            taskProcessor = taskProcessor,
            actorSystem = actorSystem,
            coordinatorConfig = coordinatorConfig
        )
    }
    
    /**
     * 创建基于 Actor 的代码库索引管理器
     *
     * @param documentStore 文档向量存储
     * @param embeddingService 嵌入服务
     * @param rootPath 根路径
     * @param actorSystem Actor 系统，如果为 null，则创建新的 Actor 系统
     * @param config 代码库索引管理器配置
     * @return 代码库索引管理器
     */
    suspend fun createActorBasedCodebaseIndexManager(
        documentStore: DocumentVectorStore,
        embeddingService: EmbeddingService,
        rootPath: Path,
        actorSystem: ActorSystem? = null,
        config: CodebaseIndexManagerConfig = CodebaseIndexManagerConfig()
    ): CodebaseIndexManager {
        // 创建 Actor 系统
        val system = actorSystem ?: ActorSystem("codebase-index-system")
        
        // 创建索引任务处理器
        val taskProcessor = ActorBasedIndexTaskProcessor(
            documentStore = documentStore,
            embeddingService = embeddingService
        )
        
        // 创建代码库索引管理器
        return CodebaseIndexManager(
            rootPath = rootPath,
            config = config,
            indexTaskProcessor = taskProcessor
        )
    }
    
    /**
     * 创建默认的代码库索引管理器配置
     *
     * @param userId 用户 ID
     * @return 代码库索引管理器配置
     */
    fun createDefaultCodebaseIndexManagerConfig(userId: String? = null): CodebaseIndexManagerConfig {
        return CodebaseIndexManagerConfig(
            fileSystemMonitorConfig = FileSystemMonitorConfig(
                pollIntervalMs = 1000, // 1秒
                excludePatterns = setOf(
                    Regex("\\.git/.*"),
                    Regex("\\.idea/.*"),
                    Regex("build/.*"),
                    Regex("target/.*"),
                    Regex("node_modules/.*"),
                    Regex("\\.gradle/.*")
                ),
                excludeExtensions = setOf(
                    "class", "jar", "war", "zip", "tar", "gz", "rar",
                    "jpg", "jpeg", "png", "gif", "bmp", "ico", "svg",
                    "mp3", "mp4", "avi", "mov", "wmv", "flv", "wav",
                    "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
                ),
                excludeDirectories = setOf(
                    ".git", ".idea", "build", "target", "node_modules", ".gradle"
                )
            ),
            gitBranchMonitorConfig = GitBranchMonitorConfig(
                pollIntervalSeconds = 5 // 5秒
            ),
            fileFilterConfig = FileFilterConfig(
                includeExtensions = setOf(
                    // 代码文件
                    "java", "kt", "kts", "scala", "groovy",
                    "py", "js", "ts", "jsx", "tsx",
                    "html", "css", "scss", "less",
                    "c", "cpp", "h", "hpp", "cs", "go", "rs",
                    "php", "rb", "swift", "m", "mm",
                    // 配置文件
                    "xml", "json", "yaml", "yml", "toml", "properties",
                    // 文档文件
                    "md", "txt", "rst"
                ),
                excludeBinaryFiles = true,
                maxFileSizeBytes = 1024 * 1024 // 1MB
            ),
            incrementalIndexerConfig = IncrementalIndexerConfig(
                batchSize = 100,
                maxQueueSize = 10000,
                deduplicationWindowMs = 1000 // 1秒
            ),
            batchProcessorConfig = BatchProcessorConfig(
                maxConcurrentBatches = 3,
                maxTasksPerBatch = 1000,
                maxRetries = 3
            ),
            enableGitMonitoring = true,
            userId = userId
        )
    }
    
    /**
     * 创建默认的协调者配置
     *
     * @return 协调者配置
     */
    fun createDefaultCoordinatorConfig(): IndexCoordinatorConfig {
        return IndexCoordinatorConfig(
            maxPendingTasks = 10000,
            taskAssignmentInterval = kotlin.time.Duration.seconds(1),
            workerStatusCheckInterval = kotlin.time.Duration.seconds(10),
            initialWorkerCount = Runtime.getRuntime().availableProcessors()
        )
    }
}
