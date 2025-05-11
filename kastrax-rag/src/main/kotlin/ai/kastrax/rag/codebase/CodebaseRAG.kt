package ai.kastrax.rag.codebase

import ai.kastrax.codebase.CodebaseIndexManager
import ai.kastrax.codebase.CodebaseIndexManagerConfig
import ai.kastrax.codebase.filesystem.FileFilterConfig
import ai.kastrax.codebase.filesystem.FileSystemMonitorConfig
import ai.kastrax.codebase.git.GitBranchMonitorConfig
import ai.kastrax.codebase.indexing.BatchProcessorConfig
import ai.kastrax.codebase.indexing.IncrementalIndexerConfig
import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.context.ContextBuilder
import ai.kastrax.rag.context.ContextBuilderConfig
import ai.kastrax.rag.model.RetrieveContextResult
import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.rag.reranker.Reranker
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.readText

private val logger = KotlinLogging.logger {}

/**
 * CodebaseRAG 配置选项
 *
 * @property fileSystemMonitorConfig 文件系统监控配置
 * @property gitBranchMonitorConfig Git 分支监控配置
 * @property fileFilterConfig 文件过滤配置
 * @property incrementalIndexerConfig 增量索引器配置
 * @property batchProcessorConfig 批处理器配置
 * @property enableGitMonitoring 是否启用 Git 监控
 * @property userId 用户 ID（用于个性化索引）
 * @property ragProcessOptions RAG 处理选项
 */
data class CodebaseRagConfig(
    val fileSystemMonitorConfig: FileSystemMonitorConfig = FileSystemMonitorConfig(),
    val gitBranchMonitorConfig: GitBranchMonitorConfig = GitBranchMonitorConfig(),
    val fileFilterConfig: FileFilterConfig = FileFilterConfig(
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
        )
    ),
    val incrementalIndexerConfig: IncrementalIndexerConfig = IncrementalIndexerConfig(),
    val batchProcessorConfig: BatchProcessorConfig = BatchProcessorConfig(),
    val enableGitMonitoring: Boolean = true,
    val userId: String? = null,
    val ragProcessOptions: RagProcessOptions = RagProcessOptions(
        contextOptions = ContextBuilderConfig(
            maxTokens = 4000,
            includeMetadata = true,
            metadataFields = listOf("path", "language", "lastModified")
        )
    )
)

/**
 * 代码库 RAG 系统，扩展基本 RAG 功能，添加代码库理解能力。
 *
 * @property rag 基础 RAG 系统
 * @property codebaseIndexManager 代码库索引管理器
 * @property config 代码库 RAG 配置
 */
class CodebaseRAG(
    private val rag: RAG,
    private val codebaseIndexManager: CodebaseIndexManager,
    private val config: CodebaseRagConfig = CodebaseRagConfig()
) {
    /**
     * 启动代码库索引
     */
    suspend fun start() = withContext(Dispatchers.IO) {
        logger.info { "启动代码库 RAG 系统" }
        
        // 启动代码库索引管理器
        codebaseIndexManager.start()
        
        // 监听索引事件
        launch {
            codebaseIndexManager.indexEvents.collect { event ->
                logger.debug { "索引事件: $event" }
            }
        }
    }
    
    /**
     * 停止代码库索引
     */
    suspend fun stop() = withContext(Dispatchers.IO) {
        logger.info { "停止代码库 RAG 系统" }
        
        // 停止代码库索引管理器
        codebaseIndexManager.stop()
    }
    
    /**
     * 搜索代码库
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param options RAG 处理选项，如果为 null，则使用默认选项
     * @return 搜索结果列表
     */
    suspend fun search(
        query: String,
        limit: Int = 5,
        minScore: Double = 0.0,
        options: RagProcessOptions? = null
    ): List<DocumentSearchResult> {
        return rag.search(query, limit, minScore, options ?: config.ragProcessOptions)
    }
    
    /**
     * 生成上下文
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param options RAG 处理选项，如果为 null，则使用默认选项
     * @return 生成的上下文
     */
    suspend fun generateContext(
        query: String,
        limit: Int = 5,
        minScore: Double = 0.0,
        options: RagProcessOptions? = null
    ): String {
        return rag.generateContext(query, limit, minScore, options ?: config.ragProcessOptions)
    }
    
    /**
     * 检索上下文
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param options RAG 处理选项，如果为 null，则使用默认选项
     * @return 检索上下文结果
     */
    suspend fun retrieveContext(
        query: String,
        limit: Int = 5,
        minScore: Double = 0.0,
        options: RagProcessOptions? = null
    ): RetrieveContextResult {
        return rag.retrieveContext(query, limit, minScore, options ?: config.ragProcessOptions)
    }
    
    /**
     * 请求重新索引代码库
     */
    suspend fun requestReindex() = withContext(Dispatchers.IO) {
        logger.info { "请求重新索引代码库" }
        
        // 请求重新索引
        codebaseIndexManager.requestReindex()
    }
    
    companion object {
        /**
         * 创建代码库 RAG 系统
         *
         * @param documentStore 文档向量存储
         * @param embeddingService 嵌入服务
         * @param rootPath 代码库根路径
         * @param reranker 重排序器，默认为 IdentityReranker
         * @param config 代码库 RAG 配置
         * @return 代码库 RAG 系统
         */
        suspend fun create(
            documentStore: DocumentVectorStore,
            embeddingService: EmbeddingService,
            rootPath: Path,
            reranker: Reranker = IdentityReranker(),
            config: CodebaseRagConfig = CodebaseRagConfig()
        ): CodebaseRAG {
            // 创建基础 RAG 系统
            val rag = RAG(
                documentStore = documentStore,
                embeddingService = embeddingService,
                reranker = reranker,
                defaultOptions = config.ragProcessOptions
            )
            
            // 创建代码库索引处理器
            val indexTaskProcessor = CodebaseIndexTaskProcessor(
                documentStore = documentStore,
                embeddingService = embeddingService
            )
            
            // 创建代码库索引管理器配置
            val indexManagerConfig = CodebaseIndexManagerConfig(
                fileSystemMonitorConfig = config.fileSystemMonitorConfig,
                gitBranchMonitorConfig = config.gitBranchMonitorConfig,
                fileFilterConfig = config.fileFilterConfig,
                incrementalIndexerConfig = config.incrementalIndexerConfig,
                batchProcessorConfig = config.batchProcessorConfig,
                enableGitMonitoring = config.enableGitMonitoring,
                userId = config.userId
            )
            
            // 创建代码库索引管理器
            val codebaseIndexManager = CodebaseIndexManager(
                rootPath = rootPath,
                config = indexManagerConfig,
                indexTaskProcessor = indexTaskProcessor
            )
            
            // 创建代码库 RAG 系统
            return CodebaseRAG(
                rag = rag,
                codebaseIndexManager = codebaseIndexManager,
                config = config
            )
        }
    }
}
