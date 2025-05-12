package ai.kastrax.codebase.retrieval

import ai.kastrax.codebase.context.Context
import ai.kastrax.codebase.context.ContextBuilder
import ai.kastrax.codebase.context.ContextLevel
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.vector.CodeVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 检索结果
 *
 * @property element 代码元素
 * @property score 相似度分数
 * @property content 内容
 * @property metadata 元数据
 */
data class RetrievalResult(
    val element: CodeElement,
    val score: Float,
    val content: String,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * 代码检索模型
 *
 * 用于检索代码库中的相关代码
 *
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 * @property contextBuilder 上下文构建器
 * @property queryCache 查询缓存
 */
class CodeRetrievalModel(
    private val vectorStore: CodeVectorStore,
    private val embeddingService: EmbeddingService,
    private val contextBuilder: ContextBuilder,
    private val queryCache: ConcurrentHashMap<String, List<RetrievalResult>> = ConcurrentHashMap()
) {
    /**
     * 检索代码
     *
     * @param query 查询
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @param types 代码元素类型列表
     * @param languages 编程语言列表
     * @param filePaths 文件路径列表
     * @return 检索结果列表
     */
    suspend fun retrieveCode(
        query: String,
        limit: Int = 10,
        minScore: Float = 0.5f,
        types: List<CodeElementType>? = null,
        languages: List<String>? = null,
        filePaths: List<Path>? = null
    ): List<RetrievalResult> {
        // 生成缓存键
        val cacheKey = generateCacheKey(query, limit, minScore, types, languages, filePaths)

        // 检查缓存
        val cachedResults = queryCache[cacheKey]
        if (cachedResults != null) {
            return cachedResults
        }

        try {
            // 生成查询向量
            val queryVector = embeddingService.embed(query).toList()

            // 执行相似度搜索
            val searchResults = vectorStore.similaritySearchMixed(
                vector = queryVector,
                types = types,
                metadataFilter = null,
                language = languages?.firstOrNull(),
                limit = limit * 2,
                minScore = minScore
            )

            // 过滤结果
            val filteredResults = searchResults.filter { result ->
                (languages == null || languages.isEmpty() || languages.any { result.element.language.equals(it, ignoreCase = true) }) &&
                (filePaths == null || filePaths.isEmpty() || filePaths.contains(result.element.location.filePath))
            }

            // 转换为检索结果
            val retrievalResults = filteredResults.map { result ->
                RetrievalResult(
                    element = result.element,
                    score = result.score,
                    content = getElementContent(result.element),
                    metadata = result.metadata
                )
            }.take(limit)

            // 缓存结果
            queryCache[cacheKey] = retrievalResults

            return retrievalResults
        } catch (e: Exception) {
            logger.error(e) { "检索代码时出错: $query" }
            return emptyList()
        }
    }

    /**
     * 检索代码（按上下文）
     *
     * @param query 查询
     * @param position 位置
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @param includeLevels 包含的级别
     * @param excludeTypes 排除的类型
     * @return 上下文
     */
    suspend fun retrieveCodeWithContext(
        query: String,
        position: Location? = null,
        limit: Int = 10,
        minScore: Float = 0.5f,
        includeLevels: Set<ContextLevel> = ContextLevel.values().toSet(),
        excludeTypes: Set<CodeElementType> = emptySet()
    ): Context {
        return contextBuilder.buildContext(
            query = query,
            position = position,
            maxElements = limit,
            minScore = minScore,
            includeLevels = includeLevels,
            excludeTypes = excludeTypes
        )
    }

    /**
     * 检索代码（按文件）
     *
     * @param filePath 文件路径
     * @param limit 返回结果数量限制
     * @return 检索结果列表
     */
    fun retrieveCodeByFile(
        filePath: Path,
        limit: Int = 10
    ): List<RetrievalResult> {
        try {
            // 查找文件中的所有元素
            val fileElements = vectorStore.getAllIds()
                .mapNotNull { vectorStore.getElement(it) }
                .filter { it.location.filePath == filePath }

            // 转换为检索结果
            return fileElements.map { element ->
                RetrievalResult(
                    element = element,
                    score = 1.0f,
                    content = getElementContent(element),
                    metadata = mapOf("filePath" to filePath.toString())
                )
            }.take(limit)
        } catch (e: Exception) {
            logger.error(e) { "按文件检索代码时出错: $filePath" }
            return emptyList()
        }
    }

    /**
     * 检索代码（按类型）
     *
     * @param typeName 类型名称
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 检索结果列表
     */
    suspend fun retrieveCodeByType(
        typeName: String,
        limit: Int = 10,
        minScore: Float = 0.5f
    ): List<RetrievalResult> {
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
                limit = limit,
                minScore = minScore
            )

            // 转换为检索结果
            return searchResults.map { result ->
                RetrievalResult(
                    element = result.element,
                    score = result.score,
                    content = getElementContent(result.element),
                    metadata = result.metadata
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "按类型检索代码时出错: $typeName" }
            return emptyList()
        }
    }

    /**
     * 检索代码（按方法）
     *
     * @param methodName 方法名称
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 检索结果列表
     */
    suspend fun retrieveCodeByMethod(
        methodName: String,
        limit: Int = 10,
        minScore: Float = 0.5f
    ): List<RetrievalResult> {
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
                limit = limit,
                minScore = minScore
            )

            // 转换为检索结果
            return searchResults.map { result ->
                RetrievalResult(
                    element = result.element,
                    score = result.score,
                    content = getElementContent(result.element),
                    metadata = result.metadata
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "按方法检索代码时出错: $methodName" }
            return emptyList()
        }
    }

    /**
     * 检索代码（按语言）
     *
     * @param language 编程语言
     * @param query 查询
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 检索结果列表
     */
    suspend fun retrieveCodeByLanguage(
        language: String,
        query: String,
        limit: Int = 10,
        minScore: Float = 0.5f
    ): List<RetrievalResult> {
        try {
            // 生成查询向量
            val queryVector = embeddingService.embed(query).toList()

            // 执行相似度搜索
            val searchResults = vectorStore.similaritySearchByLanguage(
                vector = queryVector,
                language = language,
                limit = limit,
                minScore = minScore
            )

            // 转换为检索结果
            return searchResults.map { result ->
                RetrievalResult(
                    element = result.element,
                    score = result.score,
                    content = getElementContent(result.element),
                    metadata = result.metadata
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "按语言检索代码时出错: $language, $query" }
            return emptyList()
        }
    }

    /**
     * 检索代码（多查询）
     *
     * @param queries 查询列表
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 检索结果列表
     */
    suspend fun retrieveCodeMultiQuery(
        queries: List<String>,
        limit: Int = 10,
        minScore: Float = 0.5f
    ): List<RetrievalResult> {
        try {
            // 执行每个查询
            val allResults = mutableListOf<RetrievalResult>()

            queries.forEach { query ->
                val results = retrieveCode(
                    query = query,
                    limit = limit,
                    minScore = minScore
                )
                allResults.addAll(results)
            }

            // 去重并按相似度排序
            return allResults
                .groupBy { it.element.id }
                .map { (_, results) -> results.maxByOrNull { it.score }!! }
                .sortedByDescending { it.score }
                .take(limit)
        } catch (e: Exception) {
            logger.error(e) { "多查询检索代码时出错: ${queries.joinToString(", ")}" }
            return emptyList()
        }
    }

    /**
     * 清空查询缓存
     */
    fun clearQueryCache() {
        queryCache.clear()
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
     * 生成缓存键
     *
     * @param query 查询
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @param types 代码元素类型列表
     * @param languages 编程语言列表
     * @param filePaths 文件路径列表
     * @return 缓存键
     */
    private fun generateCacheKey(
        query: String,
        limit: Int,
        minScore: Float,
        types: List<CodeElementType>?,
        languages: List<String>?,
        filePaths: List<Path>?
    ): String {
        val sb = StringBuilder()

        sb.append("query:$query")
        sb.append("|limit:$limit")
        sb.append("|minScore:$minScore")
        sb.append("|types:${types?.joinToString(",") { it.name } ?: "null"}")
        sb.append("|languages:${languages?.joinToString(",") ?: "null"}")
        sb.append("|filePaths:${filePaths?.joinToString(",") { it.toString() } ?: "null"}")

        return sb.toString()
    }
}
