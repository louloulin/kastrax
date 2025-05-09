package ai.kastrax.rag.retriever

import ai.kastrax.store.document.DocumentSearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

/**
 * 混合检索器，结合向量检索和关键词检索。
 *
 * @property vectorStoreRetriever 向量存储检索器
 * @property keywordRetriever 关键词检索器
 * @property vectorWeight 向量权重
 * @property keywordWeight 关键词权重
 */
class HybridRetriever(
    private val vectorStoreRetriever: VectorStoreRetriever,
    private val keywordRetriever: KeywordRetriever,
    private val vectorWeight: Double = 0.7,
    private val keywordWeight: Double = 0.3
) : Retriever {
    init {
        // 验证权重之和为 1.0
        val sum = vectorWeight + keywordWeight
        require(sum > 0.99 && sum < 1.01) {
            "Vector weight ($vectorWeight) + keyword weight ($keywordWeight) must be approximately 1.0"
        }
    }
    
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
            // 并行执行向量检索和关键词检索
            val vectorResults = async { vectorStoreRetriever.retrieve(query, limit * 2, 0.0) }
            val keywordResults = async { keywordRetriever.retrieve(query, limit * 2, 0.0) }
            
            val (vectorDocs, keywordDocs) = awaitAll(vectorResults, keywordResults)
            
            // 合并结果
            val mergedResults = mergeResults(
                vectorDocs as List<DocumentSearchResult>,
                keywordDocs as List<DocumentSearchResult>,
                limit
            )
            
            // 过滤低分结果
            return@withContext mergedResults.filter { it.score >= minScore }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving documents with hybrid search" }
            return@withContext emptyList()
        }
    }
    
    /**
     * 合并向量检索和关键词检索的结果。
     *
     * @param vectorResults 向量检索结果
     * @param keywordResults 关键词检索结果
     * @param limit 返回结果的最大数量
     * @return 合并后的结果列表
     */
    private fun mergeResults(
        vectorResults: List<DocumentSearchResult>,
        keywordResults: List<DocumentSearchResult>,
        limit: Int
    ): List<DocumentSearchResult> {
        // 创建 ID 到结果的映射
        val resultMap = mutableMapOf<String, MutableMap<String, Any>>()
        
        // 处理向量检索结果
        vectorResults.forEach { result ->
            resultMap[result.document.id] = mutableMapOf(
                "document" to result.document,
                "vectorScore" to result.score,
                "keywordScore" to 0.0,
                "finalScore" to (result.score * vectorWeight)
            )
        }
        
        // 处理关键词检索结果
        keywordResults.forEach { result ->
            val id = result.document.id
            if (id in resultMap) {
                // 更新已存在的结果
                val entry = resultMap[id]!!
                entry["keywordScore"] = result.score
                entry["finalScore"] = (entry["vectorScore"] as Double) * vectorWeight +
                        (result.score * keywordWeight)
            } else {
                // 添加新结果
                resultMap[id] = mutableMapOf(
                    "document" to result.document,
                    "vectorScore" to 0.0,
                    "keywordScore" to result.score,
                    "finalScore" to (result.score * keywordWeight)
                )
            }
        }
        
        // 转换为 DocumentSearchResult 列表并排序
        return resultMap.values
            .map { entry ->
                DocumentSearchResult(
                    document = entry["document"] as ai.kastrax.store.document.Document,
                    score = entry["finalScore"] as Double
                )
            }
            .sortedByDescending { it.score }
            .take(limit)
    }
}
