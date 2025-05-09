package ai.kastrax.rag.retriever

import ai.kastrax.rag.query.QueryTransformer
import ai.kastrax.store.document.DocumentSearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

/**
 * 查询增强检索器，使用查询转换器增强检索效果。
 *
 * @property baseRetriever 基础检索器
 * @property queryTransformer 查询转换器
 * @property useOriginalQuery 是否使用原始查询
 * @property combineResults 是否合并结果
 */
class QueryEnhancedRetriever(
    private val baseRetriever: Retriever,
    private val queryTransformer: QueryTransformer,
    private val useOriginalQuery: Boolean = true,
    private val combineResults: Boolean = true
) : Retriever {
    /**
     * 检索文档。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 检索结果列表
     */
    override suspend fun retrieve(
        query: String,
        limit: Int,
        minScore: Double
    ): List<DocumentSearchResult> = withContext(Dispatchers.IO) {
        try {
            // 转换查询
            val transformedQuery = queryTransformer.transform(query)
            
            // 如果转换后的查询与原始查询相同，或者不使用原始查询，则只使用一个查询
            if (transformedQuery == query || !useOriginalQuery) {
                return@withContext baseRetriever.retrieve(transformedQuery, limit, minScore)
            }
            
            // 并行执行原始查询和转换后的查询
            val originalResults = async { baseRetriever.retrieve(query, limit, minScore) }
            val transformedResults = async { baseRetriever.retrieve(transformedQuery, limit, minScore) }
            
            val (original, transformed) = awaitAll(originalResults, transformedResults)
            
            // 合并结果
            return@withContext if (combineResults) {
                mergeResults(
                    original as List<DocumentSearchResult>,
                    transformed as List<DocumentSearchResult>,
                    limit
                )
            } else {
                // 使用转换后的查询结果
                transformed as List<DocumentSearchResult>
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving documents with query enhancement" }
            return@withContext emptyList()
        }
    }
    
    /**
     * 合并检索结果。
     *
     * @param originalResults 原始查询结果
     * @param transformedResults 转换后的查询结果
     * @param limit 返回结果的最大数量
     * @return 合并后的结果列表
     */
    private fun mergeResults(
        originalResults: List<DocumentSearchResult>,
        transformedResults: List<DocumentSearchResult>,
        limit: Int
    ): List<DocumentSearchResult> {
        // 创建 ID 到结果的映射
        val resultMap = mutableMapOf<String, DocumentSearchResult>()
        
        // 处理原始查询结果
        originalResults.forEach { result ->
            resultMap[result.document.id] = result
        }
        
        // 处理转换后的查询结果
        transformedResults.forEach { result ->
            val id = result.document.id
            if (id in resultMap) {
                // 取较高的分数
                val existingResult = resultMap[id]!!
                if (result.score > existingResult.score) {
                    resultMap[id] = result
                }
            } else {
                resultMap[id] = result
            }
        }
        
        // 按分数降序排序并限制数量
        return resultMap.values
            .sortedByDescending { it.score }
            .take(limit)
    }
}
