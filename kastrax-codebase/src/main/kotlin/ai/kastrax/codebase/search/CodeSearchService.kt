package ai.kastrax.codebase.search

import ai.kastrax.codebase.embedding.CodeEmbeddingService
import ai.kastrax.codebase.indexing.CodeIndexer
import ai.kastrax.codebase.retrieval.HybridRetriever
import ai.kastrax.codebase.retrieval.KeywordSearcher
import ai.kastrax.codebase.retrieval.model.RetrievalResult
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.vector.CodeVectorStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

/**
 * 代码搜索服务配置
 *
 * @property defaultLimit 默认限制结果数量
 * @property defaultMinScore 默认最小分数
 * @property vectorWeight 向量搜索权重
 * @property keywordWeight 关键词搜索权重
 * @property enableHybridSearch 是否启用混合搜索
 * @property enableFuzzySearch 是否启用模糊搜索
 * @property enableTypeFiltering 是否启用类型过滤
 */
data class CodeSearchServiceConfig(
    val defaultLimit: Int = 20,
    val defaultMinScore: Double = 0.5,
    val vectorWeight: Double = 0.7,
    val keywordWeight: Double = 0.3,
    val enableHybridSearch: Boolean = true,
    val enableFuzzySearch: Boolean = true,
    val enableTypeFiltering: Boolean = true
)

/**
 * 代码搜索服务
 *
 * 提供代码搜索功能，支持语义搜索、关键词搜索和混合搜索
 *
 * @property codeIndexer 代码索引器
 * @property vectorStore 代码向量存储
 * @property embeddingService 代码嵌入服务
 * @property config 配置
 */
class CodeSearchService(
    private val codeIndexer: CodeIndexer,
    private val vectorStore: CodeVectorStore,
    private val embeddingService: CodeEmbeddingService,
    private val config: CodeSearchServiceConfig = CodeSearchServiceConfig()
) : Closeable {
    // 搜索门面
    private val searchFacade = SearchFacade(
        codeIndexer = codeIndexer,
        vectorStore = vectorStore,
        embeddingService = embeddingService,
        config = SearchFacadeConfig(
            defaultLimit = config.defaultLimit,
            defaultMinScore = config.defaultMinScore,
            enableReranking = true
        )
    )
    
    // 混合检索器
    private val hybridRetriever = HybridRetriever(
        vectorStore = vectorStore,
        embeddingService = embeddingService,
        keywordSearcher = KeywordSearcher(codeIndexer)
    )
    
    /**
     * 搜索代码
     *
     * @param query 查询字符串
     * @param limit 限制结果数量
     * @param minScore 最小分数
     * @param types 元素类型过滤
     * @param searchMode 搜索模式
     * @return 检索结果列表
     */
    suspend fun search(
        query: String,
        limit: Int = config.defaultLimit,
        minScore: Double = config.defaultMinScore,
        types: Set<CodeElementType>? = null,
        searchMode: SearchMode = SearchMode.HYBRID
    ): List<RetrievalResult> = withContext(Dispatchers.Default) {
        logger.info { "搜索代码: $query (模式: $searchMode, 限制: $limit, 最小分数: $minScore)" }
        
        if (query.isBlank()) {
            return@withContext emptyList()
        }
        
        val results = when (searchMode) {
            SearchMode.SEMANTIC -> searchSemantic(query, limit, minScore)
            SearchMode.KEYWORD -> searchKeyword(query, limit, minScore)
            SearchMode.HYBRID -> searchHybrid(query, limit, minScore)
        }
        
        // 应用类型过滤
        val filteredResults = if (types != null && types.isNotEmpty() && config.enableTypeFiltering) {
            results.filter { it.element.type in types }
        } else {
            results
        }
        
        // 限制结果数量
        return@withContext filteredResults.take(limit)
    }
    
    /**
     * 语义搜索
     *
     * @param query 查询字符串
     * @param limit 限制结果数量
     * @param minScore 最小分数
     * @return 检索结果列表
     */
    private suspend fun searchSemantic(
        query: String,
        limit: Int,
        minScore: Double
    ): List<RetrievalResult> = withContext(Dispatchers.Default) {
        try {
            // 使用搜索门面执行向量搜索
            val request = SearchRequest(
                query = query,
                paths = emptyList(),
                type = SearchType.VECTOR,
                options = mapOf(
                    "limit" to limit,
                    "minScore" to minScore
                )
            )
            
            return@withContext searchFacade.search(request).results
        } catch (e: Exception) {
            logger.error(e) { "语义搜索失败: $query" }
            return@withContext emptyList()
        }
    }
    
    /**
     * 关键词搜索
     *
     * @param query 查询字符串
     * @param limit 限制结果数量
     * @param minScore 最小分数
     * @return 检索结果列表
     */
    private suspend fun searchKeyword(
        query: String,
        limit: Int,
        minScore: Double
    ): List<RetrievalResult> = withContext(Dispatchers.Default) {
        try {
            // 使用搜索门面执行符号搜索
            val request = SearchRequest(
                query = query,
                paths = emptyList(),
                type = SearchType.SYMBOL,
                options = mapOf(
                    "limit" to limit,
                    "minScore" to minScore
                )
            )
            
            return@withContext searchFacade.search(request).results
        } catch (e: Exception) {
            logger.error(e) { "关键词搜索失败: $query" }
            return@withContext emptyList()
        }
    }
    
    /**
     * 混合搜索
     *
     * @param query 查询字符串
     * @param limit 限制结果数量
     * @param minScore 最小分数
     * @return 检索结果列表
     */
    private suspend fun searchHybrid(
        query: String,
        limit: Int,
        minScore: Double
    ): List<RetrievalResult> = withContext(Dispatchers.Default) {
        try {
            if (!config.enableHybridSearch) {
                // 如果禁用混合搜索，则回退到语义搜索
                return@withContext searchSemantic(query, limit, minScore)
            }
            
            // 使用搜索门面执行混合搜索
            val request = SearchRequest(
                query = query,
                paths = emptyList(),
                type = SearchType.HYBRID,
                options = mapOf(
                    "limit" to limit,
                    "minScore" to minScore
                )
            )
            
            return@withContext searchFacade.search(request).results
        } catch (e: Exception) {
            logger.error(e) { "混合搜索失败: $query" }
            return@withContext emptyList()
        }
    }
    
    /**
     * 按文件路径搜索
     *
     * @param filePath 文件路径
     * @return 元素列表
     */
    suspend fun searchByFilePath(filePath: Path): List<CodeElement> = withContext(Dispatchers.Default) {
        return@withContext searchFacade.searchByFilePath(filePath)
    }
    
    /**
     * 按元素类型搜索
     *
     * @param type 元素类型
     * @param limit 限制结果数量
     * @return 元素列表
     */
    suspend fun searchByType(type: CodeElementType, limit: Int = config.defaultLimit): List<CodeElement> = withContext(Dispatchers.Default) {
        return@withContext searchFacade.searchByType(type, limit)
    }
    
    /**
     * 按元素名称搜索
     *
     * @param name 元素名称
     * @param exactMatch 是否精确匹配
     * @param limit 限制结果数量
     * @return 元素列表
     */
    suspend fun searchByName(name: String, exactMatch: Boolean = false, limit: Int = config.defaultLimit): List<CodeElement> = withContext(Dispatchers.Default) {
        return@withContext searchFacade.searchByName(name, exactMatch, limit)
    }
    
    /**
     * 关闭资源
     */
    override fun close() {
        embeddingService.close()
    }
}
