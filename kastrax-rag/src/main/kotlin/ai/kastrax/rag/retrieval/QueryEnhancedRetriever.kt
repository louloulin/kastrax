package ai.kastrax.rag.retrieval

import ai.kastrax.rag.query.QueryTransformer
import ai.kastrax.rag.query.NoOpQueryTransformer
import ai.kastrax.rag.vectorstore.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 查询增强检索器配置。
 *
 * @property useMultiQuery 是否使用多查询策略
 * @property mergeStrategy 结果合并策略
 * @property maxQueriesPerRequest 每个请求的最大查询数
 */
data class QueryEnhancedRetrieverConfig(
    val useMultiQuery: Boolean = false,
    val mergeStrategy: MergeStrategy = MergeStrategy.INTERLEAVE,
    val maxQueriesPerRequest: Int = 3
)

/**
 * 结果合并策略。
 */
enum class MergeStrategy {
    /**
     * 交错合并，从每个查询结果中依次选择一个文档。
     */
    INTERLEAVE,
    
    /**
     * 按分数合并，选择分数最高的文档。
     */
    BY_SCORE,
    
    /**
     * 按多样性合并，确保结果的多样性。
     */
    DIVERSITY
}

/**
 * 查询增强检索器，使用查询转换器来增强检索效果。
 *
 * @property baseRetriever 基础检索器
 * @property queryTransformer 查询转换器
 * @property config 配置
 */
class QueryEnhancedRetriever(
    private val baseRetriever: Retriever,
    private val queryTransformer: QueryTransformer = NoOpQueryTransformer(),
    private val config: QueryEnhancedRetrieverConfig = QueryEnhancedRetrieverConfig()
) : Retriever {
    
    /**
     * 使用查询文本检索文档。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 检索结果列表，按相似度降序排序
     */
    override suspend fun retrieve(query: String, limit: Int, minScore: Double): List<SearchResult> {
        logger.debug { "使用查询增强检索器检索文档，查询: $query, 限制: $limit, 最小分数: $minScore" }
        
        return try {
            if (config.useMultiQuery) {
                // 使用多查询策略
                retrieveWithMultipleQueries(query, limit, minScore)
            } else {
                // 使用单一转换查询
                val transformedQuery = queryTransformer.transform(query)
                logger.debug { "转换后的查询: $transformedQuery" }
                
                if (transformedQuery == query) {
                    // 如果查询没有变化，直接使用基础检索器
                    baseRetriever.retrieve(query, limit, minScore)
                } else {
                    // 使用转换后的查询
                    baseRetriever.retrieve(transformedQuery, limit, minScore)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "查询增强检索失败，查询: $query" }
            // 出错时回退到基础检索器
            baseRetriever.retrieve(query, limit, minScore)
        }
    }
    
    /**
     * 使用多个查询变体检索文档。
     *
     * @param query 原始查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 合并后的检索结果列表
     */
    private suspend fun retrieveWithMultipleQueries(
        query: String,
        limit: Int,
        minScore: Double
    ): List<SearchResult> {
        // 生成查询变体
        val queries = queryTransformer.transformToMultiple(query)
            .distinct()
            .take(config.maxQueriesPerRequest)
        
        logger.debug { "生成的查询变体: $queries" }
        
        if (queries.size <= 1) {
            // 如果只有一个查询，直接使用基础检索器
            return baseRetriever.retrieve(queries.firstOrNull() ?: query, limit, minScore)
        }
        
        // 为每个查询变体检索文档
        val allResults = mutableListOf<Pair<String, List<SearchResult>>>()
        
        for (q in queries) {
            val results = baseRetriever.retrieve(q, limit, minScore)
            allResults.add(q to results)
        }
        
        // 合并结果
        return mergeResults(allResults, limit, minScore)
    }
    
    /**
     * 合并多个查询的结果。
     *
     * @param queryResults 查询结果对列表，每个对包含查询文本和对应的检索结果
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 合并后的检索结果列表
     */
    private fun mergeResults(
        queryResults: List<Pair<String, List<SearchResult>>>,
        limit: Int,
        minScore: Double
    ): List<SearchResult> {
        // 如果没有结果，返回空列表
        if (queryResults.isEmpty() || queryResults.all { it.second.isEmpty() }) {
            return emptyList()
        }
        
        return when (config.mergeStrategy) {
            MergeStrategy.INTERLEAVE -> mergeByInterleaving(queryResults, limit)
            MergeStrategy.BY_SCORE -> mergeByScore(queryResults, limit, minScore)
            MergeStrategy.DIVERSITY -> mergeByDiversity(queryResults, limit)
        }
    }
    
    /**
     * 通过交错方式合并结果。
     *
     * @param queryResults 查询结果对列表
     * @param limit 返回结果的最大数量
     * @return 合并后的检索结果列表
     */
    private fun mergeByInterleaving(
        queryResults: List<Pair<String, List<SearchResult>>>,
        limit: Int
    ): List<SearchResult> {
        val merged = mutableListOf<SearchResult>()
        val seenDocIds = mutableSetOf<String>()
        var index = 0
        
        // 交错合并结果
        while (merged.size < limit) {
            var addedAny = false
            
            for ((_, results) in queryResults) {
                if (index < results.size) {
                    val result = results[index]
                    val docId = result.document.id
                    
                    if (docId !in seenDocIds) {
                        merged.add(result)
                        seenDocIds.add(docId)
                        addedAny = true
                        
                        if (merged.size >= limit) {
                            break
                        }
                    }
                }
            }
            
            if (!addedAny) {
                break
            }
            
            index++
        }
        
        return merged
    }
    
    /**
     * 按分数合并结果。
     *
     * @param queryResults 查询结果对列表
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 合并后的检索结果列表
     */
    private fun mergeByScore(
        queryResults: List<Pair<String, List<SearchResult>>>,
        limit: Int,
        minScore: Double
    ): List<SearchResult> {
        val allResults = mutableListOf<SearchResult>()
        val seenDocIds = mutableSetOf<String>()
        
        // 收集所有结果
        for ((_, results) in queryResults) {
            for (result in results) {
                val docId = result.document.id
                
                if (docId !in seenDocIds && result.score >= minScore) {
                    allResults.add(result)
                    seenDocIds.add(docId)
                }
            }
        }
        
        // 按分数排序并限制数量
        return allResults.sortedByDescending { it.score }.take(limit)
    }
    
    /**
     * 按多样性合并结果。
     *
     * @param queryResults 查询结果对列表
     * @param limit 返回结果的最大数量
     * @return 合并后的检索结果列表
     */
    private fun mergeByDiversity(
        queryResults: List<Pair<String, List<SearchResult>>>,
        limit: Int
    ): List<SearchResult> {
        val merged = mutableListOf<SearchResult>()
        val seenDocIds = mutableSetOf<String>()
        
        // 为每个查询选择最佳结果
        val queriesWithResults = queryResults.filter { it.second.isNotEmpty() }
        
        // 首先，从每个查询中选择最佳结果
        for ((_, results) in queriesWithResults) {
            if (results.isNotEmpty()) {
                val bestResult = results.first()
                val docId = bestResult.document.id
                
                if (docId !in seenDocIds) {
                    merged.add(bestResult)
                    seenDocIds.add(docId)
                    
                    if (merged.size >= limit) {
                        return merged
                    }
                }
            }
        }
        
        // 然后，按分数填充剩余的位置
        val remainingResults = queryResults.flatMap { it.second }
            .filter { it.document.id !in seenDocIds }
            .sortedByDescending { it.score }
            .distinctBy { it.document.id }
        
        for (result in remainingResults) {
            merged.add(result)
            
            if (merged.size >= limit) {
                break
            }
        }
        
        return merged
    }
}
