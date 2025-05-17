package ai.kastrax.code.context

import ai.kastrax.code.model.CodeElement
import ai.kastrax.code.model.CodeElementType
import ai.kastrax.code.model.Context
import ai.kastrax.code.model.ContextElement
import ai.kastrax.code.model.ContextLevel
import ai.kastrax.code.model.Location
import ai.kastrax.codebase.context.ContextBuilder
import ai.kastrax.codebase.engine.ContextEngine
import ai.kastrax.codebase.engine.ContextEngineConfig
import ai.kastrax.codebase.engine.ContextEngineImpl
import ai.kastrax.codebase.semantic.model.CodeElement as KastraxCodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType as KastraxCodeElementType
import ai.kastrax.codebase.semantic.model.Location as KastraxLocation
import ai.kastrax.code.common.KastraXCodeBase
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 基于 kastrax-codebase 的代码上下文引擎实现
 */
class KastraxCodeContextEngine(
    private val config: CodeContextEngineConfig = CodeContextEngineConfig()
) : KastraXCodeBase(component = "CODE_CONTEXT_ENGINE"), CodeContextEngine {

    // 使用 KastraXCodeBase 的 logger
    private val initialized = AtomicBoolean(false)
    private lateinit var contextEngine: ContextEngine

    /**
     * 初始化上下文引擎
     */
    private suspend fun initialize() {
        if (initialized.getAndSet(true)) {
            return
        }

        logger.info("初始化代码上下文引擎")

        // 创建上下文引擎配置
        val contextEngineConfig = ContextEngineConfig(
            enableCodeRelationAnalysis = config.enableCodeRelationAnalysis,
            enableCodeFlowAnalysis = config.enableCodeFlowAnalysis,
            enableTreeSitterParsing = config.enableTreeSitterParsing,
            enableIncrementalIndexing = config.enableIncrementalIndexing,
            enableFileSystemMonitoring = true,
            enableGitMonitoring = true,
            enableEventNotifications = true,
            maxConcurrentTasks = config.maxIndexingThreads,
            embeddingDimension = config.embeddingDimension
        )

        // 创建上下文引擎
        // 注意：这里的实现是模拟的，实际应该使用 ContextEngineImpl.create 方法
        // 由于我们没有实际的 VectorStore 和 EmbeddingService，所以这里使用一个简单的模拟实现
        contextEngine = object : ContextEngine {
            override suspend fun indexCodebase(path: Path): Boolean {
                logger.info("模拟索引代码库: $path")
                return true
            }

            override suspend fun getQueryContext(
                query: String,
                maxResults: Int,
                minScore: Double,
                includeRelated: Boolean
            ): ai.kastrax.codebase.context.Context {
                logger.info("模拟获取查询上下文: $query")
                return ai.kastrax.codebase.context.Context(
                    elements = emptyList(),
                    query = query,
                    metadata = mapOf("source" to "mock")
                )
            }

            override suspend fun getFileContext(
                filePath: Path,
                maxResults: Int
            ): ai.kastrax.codebase.context.Context {
                logger.info("模拟获取文件上下文: $filePath")
                return ai.kastrax.codebase.context.Context(
                    elements = emptyList(),
                    query = "file:$filePath",
                    metadata = mapOf("source" to "mock")
                )
            }

            override suspend fun getEditContext(
                filePath: Path,
                position: ai.kastrax.codebase.semantic.model.Location,
                maxResults: Int,
                minScore: Double
            ): ai.kastrax.codebase.context.Context {
                logger.info("模拟获取编辑上下文: $filePath, $position")
                return ai.kastrax.codebase.context.Context(
                    elements = emptyList(),
                    query = "edit:$filePath",
                    metadata = mapOf("source" to "mock")
                )
            }

            override suspend fun getSymbolContext(
                symbolName: String,
                maxResults: Int,
                minScore: Double
            ): ai.kastrax.codebase.context.Context {
                logger.info("模拟获取符号上下文: $symbolName")
                return ai.kastrax.codebase.context.Context(
                    elements = emptyList(),
                    query = "symbol:$symbolName",
                    metadata = mapOf("source" to "mock")
                )
            }

            override suspend fun getCodeElement(id: String): ai.kastrax.codebase.semantic.model.CodeElement? {
                return null
            }

            override suspend fun getCodeElements(ids: List<String>): List<ai.kastrax.codebase.semantic.model.CodeElement> {
                return emptyList()
            }

            override suspend fun close() {
                logger.info("模拟关闭上下文引擎")
            }
        }
    }

    /**
     * 索引代码库
     *
     * @param path 代码库路径
     * @return 是否成功索引
     */
    override suspend fun indexCodebase(path: Path): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!initialized.get()) {
                initialize()
            }

            logger.info("索引代码库: $path")
            return@withContext contextEngine.indexCodebase(path)
        } catch (e: Exception) {
            logger.error("索引代码库失败: $path", e)
            return@withContext false
        }
    }

    /**
     * 获取查询上下文
     *
     * @param query 查询文本
     * @param maxResults 最大结果数量
     * @param minScore 最小相似度分数
     * @param includeRelated 是否包含相关元素
     * @return 上下文
     */
    override suspend fun getQueryContext(
        query: String,
        maxResults: Int,
        minScore: Double,
        includeRelated: Boolean
    ): Context = withContext(Dispatchers.IO) {
        try {
            if (!initialized.get()) {
                initialize()
            }

            logger.debug("获取查询上下文: $query")

            val kastraxContext = contextEngine.getQueryContext(
                query = query,
                maxResults = maxResults,
                minScore = minScore.toFloat(),
                includeRelated = includeRelated
            )

            return@withContext convertContext(kastraxContext, query)
        } catch (e: Exception) {
            logger.error("获取查询上下文失败: $query", e)
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
            if (!initialized.get()) {
                initialize()
            }

            logger.debug("获取文件上下文: $filePath")

            val kastraxContext = contextEngine.getFileContext(
                filePath = filePath,
                maxResults = maxResults
            )

            return@withContext convertContext(kastraxContext, "file:$filePath")
        } catch (e: Exception) {
            logger.error("获取文件上下文失败: $filePath", e)
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
    override suspend fun getEditContext(
        filePath: Path,
        position: Location,
        maxResults: Int,
        minScore: Double
    ): Context = withContext(Dispatchers.IO) {
        try {
            if (!initialized.get()) {
                initialize()
            }

            logger.debug("获取编辑上下文: $filePath, $position")

            val kastraxLocation = KastraxLocation(
                startLine = position.startLine,
                startColumn = position.startColumn,
                endLine = position.endLine,
                endColumn = position.endColumn
            )

            val kastraxContext = contextEngine.getEditContext(
                filePath = filePath,
                position = kastraxLocation,
                maxResults = maxResults,
                minScore = minScore.toFloat()
            )

            return@withContext convertContext(kastraxContext, "edit:$filePath")
        } catch (e: Exception) {
            logger.error("获取编辑上下文失败: $filePath, $position", e)
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
    override suspend fun getSymbolContext(
        symbolName: String,
        maxResults: Int,
        minScore: Double
    ): Context = withContext(Dispatchers.IO) {
        try {
            if (!initialized.get()) {
                initialize()
            }

            logger.debug("获取符号上下文: $symbolName")

            val kastraxContext = contextEngine.getSymbolContext(
                symbolName = symbolName,
                maxResults = maxResults,
                minScore = minScore.toFloat()
            )

            return@withContext convertContext(kastraxContext, "symbol:$symbolName")
        } catch (e: Exception) {
            logger.error("获取符号上下文失败: $symbolName", e)
            return@withContext Context(
                elements = emptyList(),
                query = "symbol:$symbolName",
                metadata = mapOf("error" to e.message.toString())
            )
        }
    }

    /**
     * 关闭上下文引擎
     */
    override suspend fun close() {
        if (initialized.getAndSet(false)) {
            logger.info("关闭代码上下文引擎")
            contextEngine.close()
        }
    }

    /**
     * 转换上下文
     *
     * @param kastraxContext kastrax-codebase 的上下文
     * @param query 查询文本
     * @return 转换后的上下文
     */
    private fun convertContext(
        kastraxContext: ai.kastrax.codebase.context.Context,
        query: String
    ): Context {
        val elements = kastraxContext.elements.map { kastraxElement ->
            ContextElement(
                element = convertCodeElement(kastraxElement.element),
                level = convertContextLevel(kastraxElement.level),
                relevance = kastraxElement.relevance,
                content = kastraxElement.content
            )
        }

        return Context(
            elements = elements,
            query = query,
            metadata = kastraxContext.metadata
        )
    }

    /**
     * 转换代码元素
     *
     * @param kastraxElement kastrax-codebase 的代码元素
     * @return 转换后的代码元素
     */
    private fun convertCodeElement(kastraxElement: KastraxCodeElement): CodeElement {
        return CodeElement(
            id = kastraxElement.id,
            type = convertCodeElementType(kastraxElement.type),
            name = kastraxElement.name,
            path = kastraxElement.path,
            location = convertLocation(kastraxElement.location),
            content = kastraxElement.content,
            metadata = kastraxElement.metadata,
            children = kastraxElement.children.map { convertCodeElement(it) }
        )
    }

    /**
     * 转换代码元素类型
     *
     * @param kastraxType kastrax-codebase 的代码元素类型
     * @return 转换后的代码元素类型
     */
    private fun convertCodeElementType(kastraxType: KastraxCodeElementType): CodeElementType {
        return when (kastraxType) {
            KastraxCodeElementType.FILE -> CodeElementType.FILE
            KastraxCodeElementType.PACKAGE -> CodeElementType.PACKAGE
            KastraxCodeElementType.CLASS -> CodeElementType.CLASS
            KastraxCodeElementType.INTERFACE -> CodeElementType.INTERFACE
            KastraxCodeElementType.ENUM -> CodeElementType.ENUM
            KastraxCodeElementType.METHOD -> CodeElementType.METHOD
            KastraxCodeElementType.CONSTRUCTOR -> CodeElementType.CONSTRUCTOR
            KastraxCodeElementType.FIELD -> CodeElementType.FIELD
            KastraxCodeElementType.PROPERTY -> CodeElementType.PROPERTY
            KastraxCodeElementType.PARAMETER -> CodeElementType.PARAMETER
            KastraxCodeElementType.LOCAL_VARIABLE -> CodeElementType.LOCAL_VARIABLE
            KastraxCodeElementType.ANNOTATION -> CodeElementType.ANNOTATION
            KastraxCodeElementType.COMMENT -> CodeElementType.COMMENT
            KastraxCodeElementType.IMPORT -> CodeElementType.IMPORT
            KastraxCodeElementType.BLOCK -> CodeElementType.BLOCK
            KastraxCodeElementType.STATEMENT -> CodeElementType.STATEMENT
            KastraxCodeElementType.EXPRESSION -> CodeElementType.EXPRESSION
            else -> CodeElementType.FILE
        }
    }

    /**
     * 转换上下文级别
     *
     * @param kastraxLevel kastrax-codebase 的上下文级别
     * @return 转换后的上下文级别
     */
    private fun convertContextLevel(kastraxLevel: ai.kastrax.codebase.context.ContextLevel): ContextLevel {
        return when (kastraxLevel) {
            ai.kastrax.codebase.context.ContextLevel.FILE -> ContextLevel.FILE
            ai.kastrax.codebase.context.ContextLevel.CLASS -> ContextLevel.CLASS
            ai.kastrax.codebase.context.ContextLevel.METHOD -> ContextLevel.METHOD
            ai.kastrax.codebase.context.ContextLevel.VARIABLE -> ContextLevel.VARIABLE
            ai.kastrax.codebase.context.ContextLevel.BLOCK -> ContextLevel.BLOCK
            else -> ContextLevel.FILE
        }
    }

    /**
     * 转换位置
     *
     * @param kastraxLocation kastrax-codebase 的位置
     * @return 转换后的位置
     */
    private fun convertLocation(kastraxLocation: KastraxLocation): Location {
        return Location(
            startLine = kastraxLocation.startLine,
            startColumn = kastraxLocation.startColumn,
            endLine = kastraxLocation.endLine,
            endColumn = kastraxLocation.endColumn
        )
    }
}

/**
 * 代码上下文引擎配置
 */
data class CodeContextEngineConfig(
    // 代码分析配置
    val enableCodeRelationAnalysis: Boolean = true,
    val enableCodeFlowAnalysis: Boolean = true,
    val enableTreeSitterParsing: Boolean = false,

    // 索引配置
    val enableIncrementalIndexing: Boolean = true,
    val enableDistributedIndexing: Boolean = false,
    val maxFileSizeBytes: Long = 1024 * 1024, // 1MB
    val maxIndexingThreads: Int = 4,

    // 嵌入配置
    val embeddingModelName: String = "all-MiniLM-L6-v2",
    val embeddingDimension: Int = 384,

    // 记忆增强配置
    val enableMemoryEnhancement: Boolean = true
)
