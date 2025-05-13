package ai.kastrax.codebase.context

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.vector.CodeSearchResult
import ai.kastrax.codebase.vector.CodeVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 上下文级别
 */
enum class ContextLevel {
    /**
     * 文件级别
     */
    FILE,

    /**
     * 类级别
     */
    CLASS,

    /**
     * 方法级别
     */
    METHOD,

    /**
     * 语句级别
     */
    STATEMENT,

    /**
     * 表达式级别
     */
    EXPRESSION
}

/**
 * 上下文元素
 *
 * @property element 代码元素
 * @property level 上下文级别
 * @property relevance 相关性分数
 * @property content 内容
 */
data class ContextElement(
    val element: CodeElement,
    val level: ContextLevel,
    val relevance: Float,
    val content: String
)

/**
 * 上下文
 *
 * @property elements 上下文元素列表
 * @property query 查询
 * @property metadata 元数据
 */
data class Context(
    val elements: List<ContextElement>,
    val query: String,
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * 获取上下文文本
     *
     * @return 上下文文本
     */
    fun getText(): String {
        val sb = StringBuilder()

        elements.forEach { element ->
            sb.append("// ${element.level.name} (${element.element.type.name}, relevance: ${element.relevance})\n")
            sb.append(element.content)
            sb.append("\n\n")
        }

        return sb.toString()
    }

    /**
     * 获取指定级别的上下文元素
     *
     * @param level 上下文级别
     * @return 上下文元素列表
     */
    fun getElementsByLevel(level: ContextLevel): List<ContextElement> {
        return elements.filter { it.level == level }
    }

    /**
     * 获取指定类型的上下文元素
     *
     * @param type 代码元素类型
     * @return 上下文元素列表
     */
    fun getElementsByType(type: CodeElementType): List<ContextElement> {
        return elements.filter { it.element.type == type }
    }

    /**
     * 获取指定文件的上下文元素
     *
     * @param filePath 文件路径
     * @return 上下文元素列表
     */
    fun getElementsByFilePath(filePath: Path): List<ContextElement> {
        return elements.filter { it.element.location.filePath == filePath }
    }

    /**
     * 获取最相关的上下文元素
     *
     * @param limit 数量限制
     * @return 上下文元素列表
     */
    fun getMostRelevantElements(limit: Int): List<ContextElement> {
        return elements.sortedByDescending { it.relevance }.take(limit)
    }
}

/**
 * 上下文构建器配置
 *
 * @property maxCacheSize 最大缓存大小
 * @property defaultMaxElements 默认最大元素数量
 * @property defaultMinScore 默认最小分数
 */
data class ContextBuilderConfig(
    val maxCacheSize: Int = 100,
    val defaultMaxElements: Int = 20,
    val defaultMinScore: Float = 0.5f
)

/**
 * 上下文构建器
 *
 * 用于构建多级上下文
 *
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 * @property config 配置
 * @property contextCache 上下文缓存
 */
class ContextBuilder(
    private val vectorStore: CodeVectorStore,
    private val embeddingService: EmbeddingService,
    private val config: ContextBuilderConfig = ContextBuilderConfig(),
    private val contextCache: ConcurrentHashMap<String, Context> = ConcurrentHashMap()
) {
    /**
     * 构建上下文
     *
     * @param query 查询
     * @param position 位置
     * @param maxElements 最大元素数量
     * @param minScore 最小相似度分数
     * @param includeLevels 包含的级别
     * @param excludeTypes 排除的类型
     * @return 上下文
     */
    suspend fun buildContext(
        query: String,
        position: Location? = null,
        maxElements: Int = config.defaultMaxElements,
        minScore: Float = config.defaultMinScore,
        includeLevels: Set<ContextLevel> = ContextLevel.values().toSet(),
        excludeTypes: Set<CodeElementType> = emptySet()
    ): Context = withContext(Dispatchers.IO) {
        // 生成缓存键
        val cacheKey = generateCacheKey(query, position, maxElements, minScore, includeLevels, excludeTypes)

        // 检查缓存
        val cachedContext = contextCache[cacheKey]
        if (cachedContext != null) {
            return@withContext cachedContext
        }

        try {
            // 生成查询向量
            val queryVector = embeddingService.embed(query).toList()

            // 执行相似度搜索
            val searchResults = vectorStore.similaritySearch(
                vector = queryVector,
                limit = maxElements * 2, // 获取更多结果，以便后续过滤
                minScore = minScore.toFloat(),
                filter = { _, _, element ->
                    element.type !in excludeTypes
                }
            )

            // 构建上下文元素
            val contextElements = mutableListOf<ContextElement>()

            // 如果指定了位置，优先添加包含该位置的元素
            if (position != null) {
                val elementsAtPosition = findElementsAtPosition(position)
                elementsAtPosition.forEach { element ->
                    val level = getContextLevel(element)
                    if (level in includeLevels) {
                        val content = getElementContent(element)
                        contextElements.add(
                            ContextElement(
                                element = element,
                                level = level,
                                relevance = 1.0f, // 位置匹配的元素具有最高相关性
                                content = content
                            )
                        )
                    }
                }
            }

            // 添加相似度搜索结果
            searchResults.forEach { result ->
                val element = result.element
                val level = getContextLevel(element)

                // 检查是否已经添加过该元素
                if (level in includeLevels && contextElements.none { it.element.id == element.id }) {
                    val content = getElementContent(element)
                    contextElements.add(
                        ContextElement(
                            element = element,
                            level = level,
                            relevance = result.score.toFloat(),
                            content = content
                        )
                    )
                }
            }

            // 限制元素数量
            val finalElements = contextElements.sortedByDescending { it.relevance }.take(maxElements)

            // 创建上下文
            val context = Context(
                elements = finalElements,
                query = query,
                metadata = mapOf(
                    "position" to (position?.toString() ?: ""),
                    "maxElements" to maxElements,
                    "minScore" to minScore,
                    "includeLevels" to includeLevels.map { it.name },
                    "excludeTypes" to excludeTypes.map { it.name }
                )
            )

            // 缓存上下文
            if (contextCache.size < config.maxCacheSize) {
                contextCache[cacheKey] = context
            }

            return@withContext context
        } catch (e: Exception) {
            logger.error(e) { "构建上下文时出错: $query" }
            return@withContext Context(
                elements = emptyList(),
                query = query,
                metadata = mapOf("error" to e.message.toString())
            )
        }
    }

    /**
     * 构建文件上下文
     *
     * @param filePath 文件路径
     * @param maxElements 最大元素数量
     * @return 上下文
     */
    suspend fun buildFileContext(
        filePath: Path,
        maxElements: Int = config.defaultMaxElements
    ): Context = withContext(Dispatchers.IO) {
        try {
            // 查找文件中的所有元素
            val fileElements = findElementsByFilePath(filePath)

            // 构建上下文元素
            val contextElements = mutableListOf<ContextElement>()

            fileElements.forEach { element ->
                val level = getContextLevel(element)
                val content = getElementContent(element)
                contextElements.add(
                    ContextElement(
                        element = element,
                        level = level,
                        relevance = 1.0f,
                        content = content
                    )
                )
            }

            // 限制元素数量
            val finalElements = contextElements.take(maxElements)

            // 创建上下文
            return@withContext Context(
                elements = finalElements,
                query = "file:$filePath",
                metadata = mapOf(
                    "filePath" to filePath.toString(),
                    "maxElements" to maxElements
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "构建文件上下文时出错: $filePath" }
            return@withContext Context(
                elements = emptyList(),
                query = "file:$filePath",
                metadata = mapOf("error" to e.message.toString())
            )
        }
    }

    /**
     * 构建符号上下文
     *
     * @param symbolName 符号名称
     * @param maxElements 最大元素数量
     * @param minScore 最小分数
     * @return 上下文
     */
    suspend fun buildSymbolContext(
        symbolName: String,
        maxElements: Int = config.defaultMaxElements,
        minScore: Float = config.defaultMinScore
    ): Context = withContext(Dispatchers.IO) {
        try {
            // 生成查询向量
            val queryVector = embeddingService.embed(symbolName).toList()

            // 执行相似度搜索
            val searchResults = vectorStore.similaritySearch(
                vector = queryVector,
                limit = maxElements * 2,
                minScore = minScore,
                filter = { _, _, element -> element.name.contains(symbolName, ignoreCase = true) }
            )

            // 构建上下文元素
            val contextElements = mutableListOf<ContextElement>()

            searchResults.forEach { result ->
                val element = result.element
                val level = getContextLevel(element)
                val content = getElementContent(element)
                contextElements.add(
                    ContextElement(
                        element = element,
                        level = level,
                        relevance = result.score.toFloat(),
                        content = content
                    )
                )
            }

            // 限制元素数量
            val finalElements = contextElements.sortedByDescending { it.relevance }.take(maxElements)

            // 创建上下文
            return@withContext Context(
                elements = finalElements,
                query = "symbol:$symbolName",
                metadata = mapOf(
                    "symbolName" to symbolName,
                    "maxElements" to maxElements,
                    "minScore" to minScore
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "构建符号上下文时出错: $symbolName" }
            return@withContext Context(
                elements = emptyList(),
                query = "symbol:$symbolName",
                metadata = mapOf("error" to e.message.toString())
            )
        }
    }

    /**
     * 构建类型上下文
     *
     * @param typeName 类型名称
     * @param maxElements 最大元素数量
     * @param minScore 最小分数
     * @return 上下文
     */
    suspend fun buildTypeContext(
        typeName: String,
        maxElements: Int = config.defaultMaxElements,
        minScore: Float = config.defaultMinScore
    ): Context = withContext(Dispatchers.IO) {
        try {
            // 生成查询向量
            val queryVector = embeddingService.embed(typeName).toList()

            // 执行相似度搜索
            val searchResults = vectorStore.similaritySearchByTypes(
                vector = queryVector,
                types = listOf(
                    CodeElementType.CLASS,
                    CodeElementType.INTERFACE,
                    CodeElementType.ENUM,
                    CodeElementType.ANNOTATION
                ),
                limit = maxElements * 2,
                minScore = minScore.toFloat()
            )

            // 构建上下文元素
            val contextElements = mutableListOf<ContextElement>()

            searchResults.forEach { result ->
                val element = result.element
                val level = getContextLevel(element)
                val content = getElementContent(element)
                contextElements.add(
                    ContextElement(
                        element = element,
                        level = level,
                        relevance = result.score.toFloat(),
                        content = content
                    )
                )
            }

            // 限制元素数量
            val finalElements = contextElements.sortedByDescending { it.relevance }.take(maxElements)

            // 创建上下文
            return@withContext Context(
                elements = finalElements,
                query = "type:$typeName",
                metadata = mapOf(
                    "typeName" to typeName,
                    "maxElements" to maxElements,
                    "minScore" to minScore
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "构建类型上下文时出错: $typeName" }
            return@withContext Context(
                elements = emptyList(),
                query = "type:$typeName",
                metadata = mapOf("error" to e.message.toString())
            )
        }
    }

    /**
     * 构建方法上下文
     *
     * @param methodName 方法名称
     * @param maxElements 最大元素数量
     * @param minScore 最小分数
     * @return 上下文
     */
    suspend fun buildMethodContext(
        methodName: String,
        maxElements: Int = config.defaultMaxElements,
        minScore: Float = config.defaultMinScore
    ): Context = withContext(Dispatchers.IO) {
        try {
            // 生成查询向量
            val queryVector = embeddingService.embed(methodName).toList()

            // 执行相似度搜索
            val searchResults = vectorStore.similaritySearchByTypes(
                vector = queryVector,
                types = listOf(
                    CodeElementType.METHOD,
                    CodeElementType.CONSTRUCTOR
                ),
                limit = maxElements * 2,
                minScore = minScore.toFloat()
            )

            // 构建上下文元素
            val contextElements = mutableListOf<ContextElement>()

            searchResults.forEach { result ->
                val element = result.element
                val level = getContextLevel(element)
                val content = getElementContent(element)
                contextElements.add(
                    ContextElement(
                        element = element,
                        level = level,
                        relevance = result.score.toFloat(),
                        content = content
                    )
                )
            }

            // 限制元素数量
            val finalElements = contextElements.sortedByDescending { it.relevance }.take(maxElements)

            // 创建上下文
            return@withContext Context(
                elements = finalElements,
                query = "method:$methodName",
                metadata = mapOf(
                    "methodName" to methodName,
                    "maxElements" to maxElements,
                    "minScore" to minScore
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "构建方法上下文时出错: $methodName" }
            return@withContext Context(
                elements = emptyList(),
                query = "method:$methodName",
                metadata = mapOf("error" to e.message.toString())
            )
        }
    }

    /**
     * 构建多查询上下文
     *
     * @param queries 查询列表
     * @param maxElementsPerQuery 每个查询的最大元素数量
     * @param minScore 最小分数
     * @param includeLevels 包含的级别
     * @param excludeTypes 排除的类型
     * @return 上下文
     */
    suspend fun buildMultiQueryContext(
        queries: List<String>,
        maxElementsPerQuery: Int = config.defaultMaxElements / 2,
        minScore: Float = config.defaultMinScore,
        includeLevels: Set<ContextLevel> = ContextLevel.values().toSet(),
        excludeTypes: Set<CodeElementType> = emptySet()
    ): Context = withContext(Dispatchers.IO) {
        try {
            // 构建每个查询的上下文
            val contexts = queries.map { query ->
                buildContext(
                    query = query,
                    maxElements = maxElementsPerQuery,
                    minScore = minScore,
                    includeLevels = includeLevels,
                    excludeTypes = excludeTypes
                )
            }

            // 合并上下文元素
            val allElements = mutableListOf<ContextElement>()
            contexts.forEach { context ->
                allElements.addAll(context.elements)
            }

            // 去重并按相关性排序
            val uniqueElements = allElements
                .groupBy { it.element.id }
                .map { (_, elements) -> elements.maxByOrNull { it.relevance }!! }
                .sortedByDescending { it.relevance }
                .take(maxElementsPerQuery * queries.size)

            // 创建上下文
            return@withContext Context(
                elements = uniqueElements,
                query = queries.joinToString("; "),
                metadata = mapOf(
                    "queries" to queries,
                    "maxElementsPerQuery" to maxElementsPerQuery,
                    "minScore" to minScore,
                    "includeLevels" to includeLevels.map { it.name },
                    "excludeTypes" to excludeTypes.map { it.name }
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "构建多查询上下文时出错: ${queries.joinToString("; ")}" }
            return@withContext Context(
                elements = emptyList(),
                query = queries.joinToString("; "),
                metadata = mapOf("error" to e.message.toString())
            )
        }
    }

    /**
     * 合并上下文
     *
     * @param contexts 上下文列表
     * @param maxElements 最大元素数量
     * @return 合并后的上下文
     */
    fun mergeContexts(
        contexts: List<Context>,
        maxElements: Int = config.defaultMaxElements * 2
    ): Context {
        try {
            // 合并上下文元素
            val allElements = mutableListOf<ContextElement>()
            contexts.forEach { context ->
                allElements.addAll(context.elements)
            }

            // 去重并按相关性排序
            val uniqueElements = allElements
                .groupBy { it.element.id }
                .map { (_, elements) -> elements.maxByOrNull { it.relevance }!! }
                .sortedByDescending { it.relevance }
                .take(maxElements)

            // 创建上下文
            return Context(
                elements = uniqueElements,
                query = contexts.joinToString("; ") { it.query },
                metadata = mapOf(
                    "contexts" to contexts.map { it.query },
                    "maxElements" to maxElements
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "合并上下文时出错" }
            return Context(
                elements = emptyList(),
                query = "merged",
                metadata = mapOf("error" to e.message.toString())
            )
        }
    }

    /**
     * 清空上下文缓存
     */
    fun clearContextCache() {
        contextCache.clear()
    }

    /**
     * 获取上下文级别
     *
     * @param element 代码元素
     * @return 上下文级别
     */
    private fun getContextLevel(element: CodeElement): ContextLevel {
        return when (element.type) {
            CodeElementType.FILE -> ContextLevel.FILE
            CodeElementType.CLASS, CodeElementType.INTERFACE, CodeElementType.ENUM, CodeElementType.ANNOTATION -> ContextLevel.CLASS
            CodeElementType.METHOD, CodeElementType.CONSTRUCTOR -> ContextLevel.METHOD
            CodeElementType.STATEMENT -> ContextLevel.STATEMENT
            CodeElementType.EXPRESSION -> ContextLevel.EXPRESSION
            else -> ContextLevel.FILE
        }
    }

    /**
     * 获取元素内容
     *
     * @param element 代码元素
     * @return 元素内容
     */
    private fun getElementContent(element: CodeElement): String {
        // 这里应该从文件中读取元素的实际内容
        // 简单起见，这里只返回元素的详细描述
        return element.getDetailedDescription()
    }

    /**
     * 查找指定位置的元素
     *
     * @param position 位置
     * @return 元素列表
     */
    private fun findElementsAtPosition(position: Location): List<CodeElement> {
        // 查找包含指定位置的所有元素
        return vectorStore.getAllIds()
            .mapNotNull { vectorStore.getElement(it) }
            .filter { it.containsPosition(position) }
    }

    /**
     * 查找指定文件路径的元素
     *
     * @param filePath 文件路径
     * @return 元素列表
     */
    private fun findElementsByFilePath(filePath: Path): List<CodeElement> {
        // 查找指定文件路径的所有元素
        return vectorStore.getAllIds()
            .mapNotNull { vectorStore.getElement(it) }
            .filter { it.location.filePath == filePath }
    }

    /**
     * 生成缓存键
     *
     * @param query 查询
     * @param position 位置
     * @param maxElements 最大元素数量
     * @param minScore 最小分数
     * @param includeLevels 包含的级别
     * @param excludeTypes 排除的类型
     * @return 缓存键
     */
    private fun generateCacheKey(
        query: String,
        position: Location?,
        maxElements: Int,
        minScore: Float,
        includeLevels: Set<ContextLevel>,
        excludeTypes: Set<CodeElementType>
    ): String {
        val sb = StringBuilder()

        sb.append("query:$query")
        sb.append("|position:${position?.toString() ?: "null"}")
        sb.append("|maxElements:$maxElements")
        sb.append("|minScore:$minScore")
        sb.append("|includeLevels:${includeLevels.joinToString(",") { it.name }}")
        sb.append("|excludeTypes:${excludeTypes.joinToString(",") { it.name }}")

        return sb.toString()
    }
}
