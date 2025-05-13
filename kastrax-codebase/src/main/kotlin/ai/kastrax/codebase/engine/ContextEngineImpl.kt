package ai.kastrax.codebase.engine

import ai.kastrax.codebase.CodebaseIndexManager
import ai.kastrax.codebase.CodebaseIndexManagerConfig
import ai.kastrax.codebase.CodebaseIndexStatus
import ai.kastrax.codebase.context.Context
import ai.kastrax.codebase.context.ContextBuilder
import ai.kastrax.codebase.context.ContextBuilderConfig
import ai.kastrax.codebase.context.FlowAwareContextBuilder
import ai.kastrax.codebase.context.FlowAwareContextBuilderConfig
import ai.kastrax.codebase.context.ContextLevel
import ai.kastrax.codebase.embedding.CodeEmbeddingService
import ai.kastrax.codebase.embedding.CodeEmbeddingServiceConfig
import ai.kastrax.codebase.indexing.IncrementalIndexTask
import ai.kastrax.codebase.indexing.IndexTaskProcessor
import ai.kastrax.codebase.indexing.IndexTaskType
import ai.kastrax.codebase.semantic.CodeSemanticAnalyzer
import ai.kastrax.codebase.semantic.CodeSemanticAnalyzerConfig
import ai.kastrax.codebase.semantic.flow.CodeFlowAnalyzer
import ai.kastrax.codebase.semantic.flow.CodeFlowAnalyzerConfig
import ai.kastrax.codebase.semantic.flow.CodeFlowAnalyzerFactory
import ai.kastrax.codebase.semantic.flow.FlowType
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.parser.CodeParserFactory
import ai.kastrax.codebase.semantic.relation.CodeRelation
import ai.kastrax.codebase.semantic.relation.CodeRelationAnalyzer
import ai.kastrax.codebase.semantic.relation.CodeRelationAnalyzerConfig
import ai.kastrax.codebase.vector.CodeSearchResult
import ai.kastrax.codebase.vector.CodeVectorStore
import ai.kastrax.store.VectorStore
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

/**
 * 上下文引擎配置
 *
 * @property enableFileSystemMonitoring 是否启用文件系统监控
 * @property enableGitMonitoring 是否启用Git分支监控
 * @property enableIncrementalIndexing 是否启用增量索引
 * @property enableEventNotifications 是否启用事件通知
 * @property maxConcurrentTasks 最大并发任务数
 * @property embeddingDimension 嵌入向量维度
 * @property contextBuilderConfig 上下文构建器配置
 * @property semanticAnalyzerConfig 语义分析器配置
 * @property codeFlowAnalyzerConfig 代码流分析器配置
 * @property codeRelationAnalyzerConfig 代码关系分析器配置
 * @property enableCodeFlowAnalysis 是否启用代码流分析
 * @property enableCodeRelationAnalysis 是否启用代码关系分析
 * @property enableTreeSitterParsing 是否启用 Tree-sitter 解析
 * @property enableCaching 是否启用缓存
 */
data class ContextEngineConfig(
    val enableFileSystemMonitoring: Boolean = true,
    val enableGitMonitoring: Boolean = true,
    val enableIncrementalIndexing: Boolean = true,
    val enableEventNotifications: Boolean = true,
    val maxConcurrentTasks: Int = 10,
    val embeddingDimension: Int = 1536,
    val contextBuilderConfig: ContextBuilderConfig = ContextBuilderConfig(),
    val semanticAnalyzerConfig: CodeSemanticAnalyzerConfig = CodeSemanticAnalyzerConfig(),
    val codeFlowAnalyzerConfig: CodeFlowAnalyzerConfig = CodeFlowAnalyzerConfig(),
    val codeRelationAnalyzerConfig: CodeRelationAnalyzerConfig = CodeRelationAnalyzerConfig(),
    val enableCodeFlowAnalysis: Boolean = true,
    val enableCodeRelationAnalysis: Boolean = true,
    val enableTreeSitterParsing: Boolean = true,
    val enableCaching: Boolean = true
)

/**
 * 上下文引擎事件类型
 */
enum class ContextEngineEventType {
    INDEXING_STARTED,
    INDEXING_COMPLETED,
    INDEXING_FAILED,
    ELEMENT_INDEXED,
    ELEMENT_DELETED,
    QUERY_EXECUTED,
    CONTEXT_BUILT,
    ERROR
}

/**
 * 上下文引擎事件
 *
 * @property type 事件类型
 * @property message 事件消息
 * @property data 事件数据
 */
data class ContextEngineEvent(
    val type: ContextEngineEventType,
    val message: String,
    val data: Map<String, Any> = emptyMap()
)

/**
 * 上下文引擎实现
 *
 * @property rootPath 代码库根路径
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 * @property config 配置
 */
class ContextEngineImpl(
    private val rootPath: Path,
    private val vectorStore: VectorStore,
    private val embeddingService: EmbeddingService,
    private val config: ContextEngineConfig = ContextEngineConfig()
) : ContextEngine {
    // 代码向量存储
    private val codeVectorStore = CodeVectorStore(
        baseVectorStore = vectorStore,
        indexName = "code-elements",
        dimension = config.embeddingDimension
    )

    // 代码嵌入服务
    private val codeEmbeddingService = embeddingService

    // 语义分析器
    private val semanticAnalyzer = CodeSemanticAnalyzer(
        config = config.semanticAnalyzerConfig.copy(
            useTreeSitterParser = config.enableTreeSitterParsing,
            enableSymbolRelationAnalysis = config.enableCodeRelationAnalysis,
            enableCodeFlowAnalysis = config.enableCodeFlowAnalysis,
            enableIncrementalParsing = config.enableIncrementalIndexing,
            enableParallelParsing = true
        )
    )

    // 代码流分析器
    private val codeFlowAnalyzer: CodeFlowAnalyzer? = if (config.enableCodeFlowAnalysis) {
        // 初始化代码流分析器工厂
        CodeFlowAnalyzerFactory.init(config.codeFlowAnalyzerConfig)
        // 获取控制流分析器
        CodeFlowAnalyzerFactory.getAnalyzer(FlowType.CONTROL_FLOW)
    } else null

    // 代码关系分析器
    private val codeRelationAnalyzer: CodeRelationAnalyzer? = if (config.enableCodeRelationAnalysis) {
        CodeRelationAnalyzer(config.codeRelationAnalyzerConfig)
    } else null

    // 上下文构建器
    private val contextBuilder = if (config.enableCodeFlowAnalysis) {
        FlowAwareContextBuilder(
            vectorStore = codeVectorStore,
            embeddingService = codeEmbeddingService,
            flowAnalyzer = codeFlowAnalyzer ?: throw IllegalStateException("代码流分析器未初始化，但启用了代码流分析"),
            config = FlowAwareContextBuilderConfig(
                maxCacheSize = config.contextBuilderConfig.maxCacheSize,
                defaultMaxElements = config.contextBuilderConfig.defaultMaxElements,
                defaultMinScore = config.contextBuilderConfig.defaultMinScore,
                includeRelatedElements = config.contextBuilderConfig.includeRelatedElements,
                maxRelatedElements = config.contextBuilderConfig.maxRelatedElements,
                includeControlFlow = true,
                includeDataFlow = true,
                maxFlowDepth = 3
            ),
            relationAnalyzer = codeRelationAnalyzer
        )
    } else {
        ContextBuilder(
            vectorStore = codeVectorStore,
            embeddingService = codeEmbeddingService,
            config = config.contextBuilderConfig,
            relationAnalyzer = codeRelationAnalyzer
        )
    }

    // 代码库索引管理器
    private val indexManager = CodebaseIndexManager(
        rootPath = rootPath,
        config = CodebaseIndexManagerConfig(),
        indexTaskProcessor = createIndexTaskProcessor()
    )

    // 元素缓存
    private val elementCache = ConcurrentHashMap<String, CodeElement>()

    // 是否已初始化
    private val initialized = AtomicBoolean(false)

    // 是否正在索引
    private val indexing = AtomicBoolean(false)

    // 事件流
    private val _events = MutableSharedFlow<ContextEngineEvent>(extraBufferCapacity = 100)
    val events: SharedFlow<ContextEngineEvent> = _events.asSharedFlow()

    /**
     * 索引代码库
     *
     * @param path 代码库路径
     * @return 是否成功索引
     */
    override suspend fun indexCodebase(path: Path): Boolean = withContext(Dispatchers.IO) {
        if (indexing.getAndSet(true)) {
            logger.warn { "已经在索引中，忽略重复请求" }
            return@withContext false
        }

        try {
            emitEvent(
                ContextEngineEventType.INDEXING_STARTED,
                "开始索引代码库: $path"
            )

            // 初始化代码向量存储
            codeVectorStore.initialize()

            // 启动索引管理器
            indexManager.start()

            // 标记为已初始化
            initialized.set(true)

            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "索引代码库失败: $path, ${e.message}" }
            emitEvent(
                ContextEngineEventType.INDEXING_FAILED,
                "索引代码库失败: ${e.message}",
                mapOf("error" to e.toString())
            )
            return@withContext false
        } finally {
            indexing.set(false)
        }
    }

    /**
     * 获取查询上下文
     *
     * @param query 查询文本
     * @param maxResults 最大结果数量
     * @param minScore 最小相似度分数
     * @return 上下文
     */
    override suspend fun getQueryContext(query: String, maxResults: Int, minScore: Double): Context = withContext(Dispatchers.IO) {
        try {
            checkInitialized()

            // 构建上下文
            val context = contextBuilder.buildContext(
                query = query,
                maxElements = maxResults,
                minScore = minScore.toFloat()
            )

            emitEvent(
                ContextEngineEventType.QUERY_EXECUTED,
                "执行查询: $query",
                mapOf(
                    "query" to query,
                    "maxResults" to maxResults,
                    "minScore" to minScore,
                    "resultCount" to context.elements.size
                )
            )

            return@withContext context
        } catch (e: Exception) {
            logger.error(e) { "获取查询上下文失败: $query, ${e.message}" }
            emitEvent(
                ContextEngineEventType.ERROR,
                "获取查询上下文失败: ${e.message}",
                mapOf("error" to e.toString())
            )
            return@withContext Context(
                elements = emptyList(),
                query = query,
                metadata = mapOf("error" to e.message.toString())
            )
        }
    }

    /**
     * 获取文件上下文
     *
     * @param filePath 文件路径
     * @param maxResults 最大结果数量
     * @return 上下文
     */
    override suspend fun getFileContext(filePath: Path, maxResults: Int): Context = withContext(Dispatchers.IO) {
        try {
            checkInitialized()

            // 构建文件上下文
            val context = contextBuilder.buildFileContext(
                filePath = filePath,
                maxElements = maxResults
            )

            emitEvent(
                ContextEngineEventType.CONTEXT_BUILT,
                "构建文件上下文: $filePath",
                mapOf(
                    "filePath" to filePath.toString(),
                    "maxResults" to maxResults,
                    "resultCount" to context.elements.size
                )
            )

            return@withContext context
        } catch (e: Exception) {
            logger.error(e) { "获取文件上下文失败: $filePath, ${e.message}" }
            emitEvent(
                ContextEngineEventType.ERROR,
                "获取文件上下文失败: ${e.message}",
                mapOf("error" to e.toString())
            )
            return@withContext Context(
                elements = emptyList(),
                query = "file:$filePath",
                metadata = mapOf("error" to e.message.toString())
            )
        }
    }

    /**
     * 获取编辑上下文
     *
     * @param filePath 文件路径
     * @param position 位置
     * @param maxResults 最大结果数量
     * @param minScore 最小相似度分数
     * @return 上下文
     */
    override suspend fun getEditContext(filePath: Path, position: Location, maxResults: Int, minScore: Double): Context = withContext(Dispatchers.IO) {
        try {
            checkInitialized()

            // 构建编辑上下文
            val context = contextBuilder.buildContext(
                query = "edit:$filePath",
                position = position,
                maxElements = maxResults,
                minScore = minScore.toFloat()
            )

            emitEvent(
                ContextEngineEventType.CONTEXT_BUILT,
                "构建编辑上下文: $filePath, $position",
                mapOf(
                    "filePath" to filePath.toString(),
                    "position" to position.toString(),
                    "maxResults" to maxResults,
                    "minScore" to minScore,
                    "resultCount" to context.elements.size
                )
            )

            return@withContext context
        } catch (e: Exception) {
            logger.error(e) { "获取编辑上下文失败: $filePath, $position, ${e.message}" }
            emitEvent(
                ContextEngineEventType.ERROR,
                "获取编辑上下文失败: ${e.message}",
                mapOf("error" to e.toString())
            )
            return@withContext Context(
                elements = emptyList(),
                query = "edit:$filePath",
                metadata = mapOf("error" to e.message.toString())
            )
        }
    }

    /**
     * 获取符号上下文
     *
     * @param symbolName 符号名称
     * @param maxResults 最大结果数量
     * @param minScore 最小相似度分数
     * @return 上下文
     */
    override suspend fun getSymbolContext(symbolName: String, maxResults: Int, minScore: Double): Context = withContext(Dispatchers.IO) {
        try {
            checkInitialized()

            // 构建符号上下文
            val context = contextBuilder.buildSymbolContext(
                symbolName = symbolName,
                maxElements = maxResults,
                minScore = minScore.toFloat()
            )

            emitEvent(
                ContextEngineEventType.CONTEXT_BUILT,
                "构建符号上下文: $symbolName",
                mapOf(
                    "symbolName" to symbolName,
                    "maxResults" to maxResults,
                    "minScore" to minScore,
                    "resultCount" to context.elements.size
                )
            )

            return@withContext context
        } catch (e: Exception) {
            logger.error(e) { "获取符号上下文失败: $symbolName, ${e.message}" }
            emitEvent(
                ContextEngineEventType.ERROR,
                "获取符号上下文失败: ${e.message}",
                mapOf("error" to e.toString())
            )
            return@withContext Context(
                elements = emptyList(),
                query = "symbol:$symbolName",
                metadata = mapOf("error" to e.message.toString())
            )
        }
    }

    /**
     * 获取代码元素
     *
     * @param id 元素ID
     * @return 代码元素，如果不存在则返回null
     */
    override suspend fun getCodeElement(id: String): CodeElement? = withContext(Dispatchers.IO) {
        try {
            checkInitialized()

            // 从缓存中获取
            val cachedElement = elementCache[id]
            if (cachedElement != null) {
                return@withContext cachedElement
            }

            // 从向量存储中获取
            val element = codeVectorStore.getElement(id)
            if (element != null) {
                // 缓存元素
                elementCache[id] = element
            }

            return@withContext element
        } catch (e: Exception) {
            logger.error(e) { "获取代码元素失败: $id, ${e.message}" }
            return@withContext null
        }
    }

    /**
     * 获取代码元素列表
     *
     * @param ids 元素ID列表
     * @return 代码元素列表
     */
    override suspend fun getCodeElements(ids: List<String>): List<CodeElement> = withContext(Dispatchers.IO) {
        try {
            checkInitialized()

            // 获取元素
            return@withContext ids.mapNotNull { getCodeElement(it) }
        } catch (e: Exception) {
            logger.error(e) { "获取代码元素列表失败: $ids, ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 获取状态
     *
     * @return 状态信息
     */
    override suspend fun getStatus(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val status = mutableMapOf<String, Any>()

            // 基本信息
            status["initialized"] = initialized.get()
            status["indexing"] = indexing.get()
            status["rootPath"] = rootPath.toString()

            // 索引状态
            if (initialized.get()) {
                status["indexStatus"] = indexManager.getStatus().name
                status["elementCount"] = codeVectorStore.getElementCount()
                status["cacheSize"] = elementCache.size

                // 增强功能状态
                status["enableCodeFlowAnalysis"] = config.enableCodeFlowAnalysis
                status["enableCodeRelationAnalysis"] = config.enableCodeRelationAnalysis
                status["enableTreeSitterParsing"] = config.enableTreeSitterParsing
                status["enableCaching"] = config.enableCaching

                // 如果启用了代码流分析，添加代码流分析器状态
                if (config.enableCodeFlowAnalysis && codeFlowAnalyzer != null) {
                    status["flowAnalyzerTypes"] = codeFlowAnalyzer.getSupportedFlowTypes().map { it.name }
                    status["flowAnalyzerElementTypes"] = codeFlowAnalyzer.getSupportedElementTypes().map { it.name }
                }

                // 如果启用了代码关系分析，添加代码关系分析器状态
                if (config.enableCodeRelationAnalysis && codeRelationAnalyzer != null) {
                    status["relationAnalyzerConfig"] = mapOf(
                        "analyzeInheritance" to config.codeRelationAnalyzerConfig.analyzeInheritance,
                        "analyzeUsage" to config.codeRelationAnalyzerConfig.analyzeUsage,
                        "analyzeDependency" to config.codeRelationAnalyzerConfig.analyzeDependency,
                        "analyzeOverride" to config.codeRelationAnalyzerConfig.analyzeOverride,
                        "analyzeImplementation" to config.codeRelationAnalyzerConfig.analyzeImplementation
                    )
                }
            }

            return@withContext status
        } catch (e: Exception) {
            logger.error(e) { "获取状态失败: ${e.message}" }
            return@withContext mapOf(
                "error" to e.message.toString()
            )
        }
    }

    /**
     * 关闭上下文引擎
     */
    override suspend fun close() = withContext(Dispatchers.IO) {
        try {
            // 停止索引管理器
            if (initialized.get()) {
                indexManager.stop()
            }

            // 关闭代码嵌入服务
            // 不需要关闭嵌入服务

            // 清空缓存
            elementCache.clear()

            // 清除代码流分析器缓存
            if (config.enableCodeFlowAnalysis && codeFlowAnalyzer != null) {
                codeFlowAnalyzer.clearCache()
            }

            // 清除代码关系分析器缓存
            if (config.enableCodeRelationAnalysis && codeRelationAnalyzer != null) {
                codeRelationAnalyzer.clearCache()
            }

            // 标记为未初始化
            initialized.set(false)

            logger.info { "上下文引擎已关闭" }
        } catch (e: Exception) {
            logger.error(e) { "关闭上下文引擎失败: ${e.message}" }
        }
    }

    /**
     * 创建索引任务处理器
     *
     * @return 索引任务处理器
     */
    private fun createIndexTaskProcessor(): IndexTaskProcessor {
        return object : IndexTaskProcessor {
            override suspend fun processTask(task: IncrementalIndexTask) {
                when (task.type) {
                    IndexTaskType.ADD, IndexTaskType.UPDATE -> {
                        processAddOrUpdateTask(task)
                    }
                    IndexTaskType.DELETE -> {
                        processDeleteTask(task)
                    }
                    IndexTaskType.BRANCH_CHANGE -> {
                        processBranchChangeTask(task)
                    }
                    IndexTaskType.FULL_REINDEX -> {
                        processFullReindexTask(task)
                    }
                }
            }

            /**
             * 处理添加或更新任务
             *
             * @param task 索引任务
             */
            private suspend fun processAddOrUpdateTask(task: IncrementalIndexTask) {
                try {
                    // 解析文件
                    val fileContent = task.path.toFile().readText()
                    val fileElement = semanticAnalyzer.parseFile(task.path, fileContent)

                    // 如果启用了代码流分析，分析代码流
                    if (config.enableCodeFlowAnalysis && codeFlowAnalyzer != null) {
                        try {
                            // 分析控制流
                            val flowGraph = codeFlowAnalyzer.analyzeFlow(fileElement)
                            // 将流图信息添加到元素元数据中
                            fileElement.metadata["flowGraph"] = flowGraph.id
                            fileElement.metadata["hasFlowAnalysis"] = true
                        } catch (e: Exception) {
                            logger.error(e) { "分析代码流失败: ${task.path}" }
                            fileElement.metadata["hasFlowAnalysis"] = false
                        }
                    }

                    // 如果启用了代码关系分析，分析代码关系
                    if (config.enableCodeRelationAnalysis && codeRelationAnalyzer != null) {
                        try {
                            // 分析代码关系
                            val relations = codeRelationAnalyzer.analyzeRelations(fileElement)
                            // 将关系信息添加到元素元数据中
                            fileElement.metadata["relationCount"] = relations.size
                            fileElement.metadata["hasRelationAnalysis"] = true
                        } catch (e: Exception) {
                            logger.error(e) { "分析代码关系失败: ${task.path}" }
                            fileElement.metadata["hasRelationAnalysis"] = false
                        }
                    }

                    // 获取所有子元素
                    val allElements = fileElement.getAllChildren() + fileElement

                    // 为每个元素生成嵌入向量
                    val elementsWithVectors = allElements.map { element ->
                        // 获取元素内容
                        val content = getElementContent(element)
                        // 生成嵌入向量
                        val vector = codeEmbeddingService.embed(content)
                        element to vector
                    }

                    // 批量添加到向量存储
                    val elements = elementsWithVectors.map { it.first }
                    val vectors = elementsWithVectors.map { it.second }
                    val count = codeVectorStore.addElements(elements, vectors)

                    // 缓存元素
                    if (config.enableCaching) {
                        elements.forEach { elementCache[it.id] = it }
                    }

                    // 发送事件
                    emitEvent(
                        ContextEngineEventType.ELEMENT_INDEXED,
                        "索引文件: ${task.path}",
                        mapOf(
                            "path" to task.path.toString(),
                            "elementCount" to count,
                            "hasFlowAnalysis" to (fileElement.metadata["hasFlowAnalysis"] == true),
                            "hasRelationAnalysis" to (fileElement.metadata["hasRelationAnalysis"] == true)
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "处理添加或更新任务失败: ${task.path}, ${e.message}" }
                    emitEvent(
                        ContextEngineEventType.ERROR,
                        "处理添加或更新任务失败: ${e.message}",
                        mapOf("error" to e.toString())
                    )
                }
            }

            /**
             * 处理删除任务
             *
             * @param task 索引任务
             */
            private suspend fun processDeleteTask(task: IncrementalIndexTask) {
                try {
                    // 查找与文件相关的所有元素
                    val elementsToDelete = elementCache.values
                        .filter { it.location.filePath == task.path }
                        .map { it.id }

                    if (elementsToDelete.isNotEmpty()) {
                        // 从向量存储中删除
                        codeVectorStore.deleteElements(elementsToDelete)

                        // 从缓存中删除
                        if (config.enableCaching) {
                            elementsToDelete.forEach { elementCache.remove(it) }
                        }

                        // 清除代码流分析器缓存
                        if (config.enableCodeFlowAnalysis && codeFlowAnalyzer != null) {
                            codeFlowAnalyzer.clearCache()
                        }

                        // 清除代码关系分析器缓存
                        if (config.enableCodeRelationAnalysis && codeRelationAnalyzer != null) {
                            codeRelationAnalyzer.clearCache()
                        }

                        // 发送事件
                        emitEvent(
                            ContextEngineEventType.ELEMENT_DELETED,
                            "删除文件: ${task.path}",
                            mapOf(
                                "path" to task.path.toString(),
                                "elementCount" to elementsToDelete.size
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error(e) { "处理删除任务失败: ${task.path}, ${e.message}" }
                    emitEvent(
                        ContextEngineEventType.ERROR,
                        "处理删除任务失败: ${e.message}",
                        mapOf("error" to e.toString())
                    )
                }
            }

            /**
             * 处理分支变更任务
             *
             * @param task 索引任务
             */
            private suspend fun processBranchChangeTask(task: IncrementalIndexTask) {
                try {
                    // 清空向量存储
                    codeVectorStore.clear()

                    // 清空缓存
                    elementCache.clear()

                    // 清除代码流分析器缓存
                    if (config.enableCodeFlowAnalysis && codeFlowAnalyzer != null) {
                        codeFlowAnalyzer.clearCache()
                    }

                    // 清除代码关系分析器缓存
                    if (config.enableCodeRelationAnalysis && codeRelationAnalyzer != null) {
                        codeRelationAnalyzer.clearCache()
                    }

                    // 发送事件
                    emitEvent(
                        ContextEngineEventType.INDEXING_STARTED,
                        "分支变更，开始重新索引: ${task.metadata["branch"]}",
                        mapOf(
                            "branch" to (task.metadata["branch"] ?: "unknown")
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "处理分支变更任务失败: ${task.metadata["branch"]}, ${e.message}" }
                    emitEvent(
                        ContextEngineEventType.ERROR,
                        "处理分支变更任务失败: ${e.message}",
                        mapOf("error" to e.toString())
                    )
                }
            }

            /**
             * 处理完全重新索引任务
             *
             * @param task 索引任务
             */
            private suspend fun processFullReindexTask(task: IncrementalIndexTask) {
                try {
                    // 清空向量存储
                    codeVectorStore.clear()

                    // 清空缓存
                    elementCache.clear()

                    // 清除代码流分析器缓存
                    if (config.enableCodeFlowAnalysis && codeFlowAnalyzer != null) {
                        codeFlowAnalyzer.clearCache()
                    }

                    // 清除代码关系分析器缓存
                    if (config.enableCodeRelationAnalysis && codeRelationAnalyzer != null) {
                        codeRelationAnalyzer.clearCache()
                    }

                    // 发送事件
                    emitEvent(
                        ContextEngineEventType.INDEXING_STARTED,
                        "开始完全重新索引: ${task.path}",
                        mapOf(
                            "path" to task.path.toString()
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "处理完全重新索引任务失败: ${task.path}, ${e.message}" }
                    emitEvent(
                        ContextEngineEventType.ERROR,
                        "处理完全重新索引任务失败: ${e.message}",
                        mapOf("error" to e.toString())
                    )
                }
            }
        }
    }

    /**
     * 获取元素内容
     *
     * @param element 代码元素
     * @return 元素内容
     */
    private fun getElementContent(element: CodeElement): String {
        // 如果元素有文档注释，添加到内容中
        val docString = if (element.documentation.isNotEmpty()) {
            "/**\n * ${element.documentation.replace("\n", "\n * ")}\n */\n"
        } else {
            ""
        }

        // 根据元素类型构建内容
        val content = when (element.type) {
            CodeElementType.FILE -> {
                try {
                    element.location.filePath.toFile().readText()
                } catch (e: Exception) {
                    logger.error(e) { "读取文件内容失败: ${element.location.filePath}, ${e.message}" }
                    ""
                }
            }
            CodeElementType.CLASS, CodeElementType.INTERFACE, CodeElementType.ENUM, CodeElementType.ANNOTATION -> {
                val modifiers = element.modifiers.joinToString(" ") { it.name.lowercase() }
                val visibility = element.visibility.name.lowercase()
                "$docString$visibility $modifiers ${element.type.name.lowercase()} ${element.name} {\n    // ...\n}"
            }
            CodeElementType.METHOD, CodeElementType.CONSTRUCTOR -> {
                val modifiers = element.modifiers.joinToString(" ") { it.name.lowercase() }
                val visibility = element.visibility.name.lowercase()
                val returnType = element.metadata["returnType"] ?: "void"
                "$docString$visibility $modifiers $returnType ${element.name}() {\n    // ...\n}"
            }
            CodeElementType.FIELD, CodeElementType.PROPERTY -> {
                val modifiers = element.modifiers.joinToString(" ") { it.name.lowercase() }
                val visibility = element.visibility.name.lowercase()
                val type = element.metadata["type"] ?: "Object"
                "$docString$visibility $modifiers $type ${element.name};"
            }
            else -> {
                element.name
            }
        }

        return content
    }

    /**
     * 检查是否已初始化
     */
    private fun checkInitialized() {
        if (!initialized.get()) {
            throw IllegalStateException("上下文引擎未初始化")
        }
    }

    /**
     * 发送事件
     *
     * @param type 事件类型
     * @param message 事件消息
     * @param data 事件数据
     */
    private suspend fun emitEvent(type: ContextEngineEventType, message: String, data: Map<String, Any> = emptyMap()) {
        if (!config.enableEventNotifications) {
            return
        }

        try {
            val event = ContextEngineEvent(type, message, data)
            _events.emit(event)
        } catch (e: Exception) {
            logger.error(e) { "发送事件失败: $type, $message, ${e.message}" }
        }
    }

    companion object {
        /**
         * 创建上下文引擎
         *
         * @param rootPath 代码库根路径
         * @param vectorStore 向量存储
         * @param embeddingService 嵌入服务
         * @param config 配置
         * @return 上下文引擎
         */
        fun create(
            rootPath: Path,
            vectorStore: VectorStore,
            embeddingService: EmbeddingService,
            config: ContextEngineConfig = ContextEngineConfig()
        ): ContextEngine {
            return ContextEngineImpl(
                rootPath = rootPath,
                vectorStore = vectorStore,
                embeddingService = embeddingService,
                config = config
            )
        }
    }
}
