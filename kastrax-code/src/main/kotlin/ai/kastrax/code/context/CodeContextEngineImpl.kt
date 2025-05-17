package ai.kastrax.code.context

import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.code.indexing.CodeIndexManager
import ai.kastrax.code.model.Context
import ai.kastrax.code.model.ContextElement
import ai.kastrax.code.model.Location
// 使用本地的检索请求和结果
import ai.kastrax.code.retrieval.RetrievalRequest
import ai.kastrax.code.retrieval.RetrievalResult
import ai.kastrax.codebase.search.CodeSearchService
import ai.kastrax.codebase.search.SearchFacade
import ai.kastrax.codebase.search.SearchMode
import ai.kastrax.codebase.semantic.model.CodeElementType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 代码上下文引擎实现
 *
 * 基于 kastrax-codebase 实现代码上下文引擎
 */
@Service(Service.Level.PROJECT)
class CodeContextEngineImpl(
    private val project: Project,
    private val config: CodeContextEngineConfig = CodeContextEngineConfig()
) : CodeContextEngine, KastraXCodeBase(component = "CODE_CONTEXT_ENGINE") {
    // 兼容性属性
    private val enableIncrementalIndexing: Boolean = config.enableIncrementalIndexing
    private val enableGitIntegration: Boolean = config.enableCodeRelationAnalysis
    private val enableFileSystemMonitoring: Boolean = true
    private val maxContextElements: Int = 20
    private val minRelevanceScore: Double = 0.5

    // 使用父类的logger

    // 代码索引管理器
    private val indexManager by lazy { CodeIndexManager.getInstance(project) }

    // 代码搜索服务
    private val searchService: CodeSearchService by lazy {
        // 从项目服务中获取，如果没有则创建一个新的
        project.getService(CodeSearchService::class.java) ?: project.service<CodeSearchService>()
    }

    // 搜索门面
    private val searchFacade: SearchFacade by lazy {
        // 从项目服务中获取，如果没有则创建一个新的
        project.getService(SearchFacade::class.java) ?: project.service<SearchFacade>()
    }

    /**
     * 索引代码库
     *
     * @param path 代码库路径
     * @return 是否成功索引
     */
    override suspend fun indexCodebase(path: Path): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info("开始索引代码库: $path")

            // 启动索引管理器
            indexManager.start(path)

            return@withContext true
        } catch (e: Exception) {
            logger.error("索引代码库时出错: $path", e)
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
            logger.info("获取查询上下文: $query")

            // 创建检索请求
            val request = RetrievalRequest(
                query = query,
                maxResults = maxResults,
                minScore = minScore
            )

            // 执行混合搜索
            val searchRequest = ai.kastrax.codebase.search.SearchRequest(
                query = query,
                paths = emptyList(),
                type = ai.kastrax.codebase.search.SearchType.HYBRID,
                options = mapOf(
                    "limit" to maxResults,
                    "minScore" to minScore
                )
            )
            val searchResponse = searchFacade.search(searchRequest)
            val results = searchResponse.results.map { RetrievalResult(it.element.id, it.element.content, it.score, mapOf("element" to it.element)) }

            // 转换为上下文元素
            val elements = results.map { convertToContextElement(it) }

            // 如果需要包含相关元素，则获取相关元素
            val relatedElements = if (includeRelated) {
                getRelatedElements(elements, maxResults)
            } else {
                emptyList()
            }

            // 创建上下文
            return@withContext Context(
                elements = elements + relatedElements,
                query = query
            )
        } catch (e: Exception) {
            logger.error("获取查询上下文时出错: $query", e)
            return@withContext Context(
                elements = emptyList(),
                query = query
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
            logger.info("获取文件上下文: $filePath")

            // 创建检索请求
            val request = RetrievalRequest(
                query = "file:${filePath.fileName}",
                maxResults = maxResults,
                filters = mapOf(
                    "exactMatch" to true
                )
            )

            // 执行关键词搜索
            val searchRequest = ai.kastrax.codebase.search.SearchRequest(
                query = "file:${filePath.fileName}",
                paths = emptyList(),
                type = ai.kastrax.codebase.search.SearchType.SYMBOL,
                options = mapOf(
                    "limit" to maxResults,
                    "exactMatch" to true
                )
            )
            val searchResponse = searchFacade.search(searchRequest)
            val results = searchResponse.results.map { RetrievalResult(it.element.id, it.element.content, it.score, mapOf("element" to it.element)) }

            // 转换为上下文元素
            val elements = results.map { convertToContextElement(it) }

            // 创建上下文
            return@withContext Context(
                elements = elements,
                query = "file:${filePath.fileName}"
            )
        } catch (e: Exception) {
            logger.error("获取文件上下文时出错: $filePath", e)
            return@withContext Context(
                elements = emptyList(),
                query = "file:${filePath.fileName}"
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
            logger.info("获取编辑上下文: $filePath, 位置: $position")

            // 获取文件上下文
            val fileContext = getFileContext(filePath, maxResults)

            // 创建检索请求
            val request = RetrievalRequest(
                query = "location:${filePath.fileName}:${position.line}",
                maxResults = maxResults,
                minScore = minScore
            )

            // 执行结构感知搜索
            val searchRequest = ai.kastrax.codebase.search.SearchRequest(
                query = "location:${filePath.fileName}:${position.line}",
                paths = emptyList(),
                type = ai.kastrax.codebase.search.SearchType.HYBRID,
                options = mapOf(
                    "limit" to maxResults,
                    "minScore" to minScore
                )
            )
            val searchResponse = searchFacade.search(searchRequest)
            val results = searchResponse.results.map { RetrievalResult(it.element.id, it.element.content, it.score, mapOf("element" to it.element)) }

            // 转换为上下文元素
            val elements = results.map { convertToContextElement(it) }

            // 创建上下文
            return@withContext Context(
                elements = elements + fileContext.elements,
                query = "location:${filePath.fileName}:${position.line}"
            )
        } catch (e: Exception) {
            logger.error("获取编辑上下文时出错: $filePath, 位置: $position", e)
            return@withContext Context(
                elements = emptyList(),
                query = "location:${filePath.fileName}:${position.line}"
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
            logger.info("获取符号上下文: $symbolName")

            // 创建检索请求
            val request = RetrievalRequest(
                query = "symbol:$symbolName",
                maxResults = maxResults,
                minScore = minScore,
                filters = mapOf(
                    "exactMatch" to true
                )
            )

            // 执行关键词搜索
            val searchRequest = ai.kastrax.codebase.search.SearchRequest(
                query = "symbol:$symbolName",
                paths = emptyList(),
                type = ai.kastrax.codebase.search.SearchType.SYMBOL,
                options = mapOf(
                    "limit" to maxResults,
                    "minScore" to minScore,
                    "exactMatch" to true
                )
            )
            val searchResponse = searchFacade.search(searchRequest)
            val results = searchResponse.results.map { RetrievalResult(it.element.id, it.element.content, it.score, mapOf("element" to it.element)) }

            // 转换为上下文元素
            val elements = results.map { convertToContextElement(it) }

            // 创建上下文
            return@withContext Context(
                elements = elements,
                query = "symbol:$symbolName"
            )
        } catch (e: Exception) {
            logger.error("获取符号上下文时出错: $symbolName", e)
            return@withContext Context(
                elements = emptyList(),
                query = "symbol:$symbolName"
            )
        }
    }

    /**
     * 关闭上下文引擎
     */
    override suspend fun close() {
        try {
            logger.info("关闭上下文引擎")

            // 停止索引管理器
            indexManager.stop()
        } catch (e: Exception) {
            logger.error("关闭上下文引擎时出错", e)
        }
    }

    /**
     * 转换检索结果为上下文元素
     *
     * @param result 检索结果
     * @return 上下文元素
     */
    private fun convertToContextElement(result: RetrievalResult): ContextElement {
        // 从元数据中获取元素
        val element = result.metadata["element"] as? ai.kastrax.codebase.semantic.model.CodeElement
            ?: throw IllegalArgumentException("Result metadata does not contain element")

        // 判断元素类型
        val elementType = try {
            ai.kastrax.code.model.CodeElementType.valueOf(element.type.name)
        } catch (e: Exception) {
            // 如果类型不存在，使用默认类型
            logger.warn("Unknown code element type: ${element.type.name}, using FILE instead")
            ai.kastrax.code.model.CodeElementType.FILE
        }

        // 创建代码元素
        val codeElement = ai.kastrax.code.model.CodeElement(
            id = element.id,
            name = element.name,
            type = elementType,
            content = element.content,
            path = element.location?.filePath ?: "",
            location = element.location?.let {
                Location(
                    line = it.startLine,
                    column = it.startColumn,
                    endLine = it.endLine,
                    endColumn = it.endColumn
                )
            }
        )

        // 创建上下文元素
        return ContextElement(
            element = codeElement,
            level = ai.kastrax.code.model.ContextLevel.FILE,
            relevance = ai.kastrax.code.model.ContextRelevance.HIGH,
            content = element.content,
            score = result.score.toFloat()
        )
    }

    /**
     * 获取相关元素
     *
     * @param elements 上下文元素列表
     * @param maxResults 最大结果数量
     * @return 相关元素列表
     */
    private suspend fun getRelatedElements(elements: List<ContextElement>, maxResults: Int): List<ContextElement> {
        val relatedElements = mutableListOf<ContextElement>()

        // 对每个元素获取相关元素
        for (element in elements) {
            if (relatedElements.size >= maxResults) {
                break
            }

            // 获取相关元素
            val related = getRelatedElementsForElement(element, maxResults - relatedElements.size)

            // 添加到结果列表
            relatedElements.addAll(related)
        }

        return relatedElements
    }

    /**
     * 获取单个元素的相关元素
     *
     * @param element 上下文元素
     * @param maxResults 最大结果数量
     * @return 相关元素列表
     */
    private suspend fun getRelatedElementsForElement(element: ContextElement, maxResults: Int): List<ContextElement> {
        // 如果元素没有名称，则返回空列表
        if (element.element.name.isBlank()) {
            return emptyList()
        }

        // 创建检索请求
        val request = RetrievalRequest(
            query = "related:${element.element.name}",
            maxResults = maxResults,
            filters = mapOf(
                "types" to setOf(
                    CodeElementType.CLASS,
                    CodeElementType.METHOD,
                    CodeElementType.FIELD,
                    CodeElementType.INTERFACE
                )
            )
        )

        // 执行结构感知搜索
        val searchRequest = ai.kastrax.codebase.search.SearchRequest(
            query = "related:${element.element.name}",
            paths = emptyList(),
            type = ai.kastrax.codebase.search.SearchType.HYBRID,
            options = mapOf(
                "limit" to maxResults,
                "types" to setOf(
                    ai.kastrax.codebase.semantic.model.CodeElementType.CLASS,
                    ai.kastrax.codebase.semantic.model.CodeElementType.METHOD,
                    ai.kastrax.codebase.semantic.model.CodeElementType.FIELD,
                    ai.kastrax.codebase.semantic.model.CodeElementType.INTERFACE
                )
            )
        )
        val searchResponse = searchFacade.search(searchRequest)
        val results = searchResponse.results.map { RetrievalResult(it.element.id, it.element.content, it.score, mapOf("element" to it.element)) }

        // 转换为上下文元素
        return results.map { convertToContextElement(it) }
    }

    companion object {
        /**
         * 获取项目的代码上下文引擎实例
         *
         * @param project 项目
         * @return 代码上下文引擎实例
         */
        fun getInstance(project: Project): CodeContextEngine {
            return project.service<CodeContextEngineImpl>()
        }
    }
}

// 使用 KastraxCodeContextEngine.kt 中的 CodeContextEngineConfig 类
