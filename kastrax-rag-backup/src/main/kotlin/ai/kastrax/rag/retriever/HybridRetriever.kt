package ai.kastrax.rag.retriever

import ai.kastrax.rag.document.RagDocument
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 混合检索器。
 * 结合向量检索和关键词检索的结果。
 *
 * @property vectorRetriever 向量检索器
 * @property keywordRetriever 关键词检索器
 * @property vectorWeight 向量权重
 * @property keywordWeight 关键词权重
 */
class HybridRetriever(
    private val vectorRetriever: VectorStoreRetriever,
    private val keywordRetriever: KeywordRetriever,
    private val vectorWeight: Double = 0.7,
    private val keywordWeight: Double = 0.3
) : Retriever {

    init {
        require(vectorWeight + keywordWeight > 0) { "Sum of weights must be positive" }
    }

    /**
     * 检索文档。
     *
     * @param query 查询
     * @param limit 返回结果数量
     * @return 文档列表
     */
    override suspend fun retrieve(query: String, limit: Int): List<RagDocument> {
        logger.debug { "Hybrid retrieving documents for query: $query, limit: $limit" }
        
        // 获取向量检索结果
        val vectorResults = vectorRetriever.retrieve(query, limit * 2)
        
        // 获取关键词检索结果
        val keywordResults = keywordRetriever.retrieve(query, limit * 2)
        
        // 合并结果
        val mergedResults = mergeResults(vectorResults, keywordResults, limit)
        
        logger.debug { "Hybrid retrieved ${mergedResults.size} documents" }
        return mergedResults
    }

    /**
     * 合并结果。
     *
     * @param vectorResults 向量检索结果
     * @param keywordResults 关键词检索结果
     * @param limit 返回结果数量
     * @return 合并后的文档列表
     */
    private fun mergeResults(
        vectorResults: List<RagDocument>,
        keywordResults: List<RagDocument>,
        limit: Int
    ): List<RagDocument> {
        // 创建 ID 到文档的映射
        val idToDoc = mutableMapOf<String, RagDocument>()
        val idToScore = mutableMapOf<String, Double>()
        
        // 处理向量检索结果
        vectorResults.forEach { doc ->
            val id = doc.id
            idToDoc[id] = doc
            idToScore[id] = (doc.metadata["score"] as? Double ?: 0.0) * vectorWeight
        }
        
        // 处理关键词检索结果
        keywordResults.forEach { doc ->
            val id = doc.id
            if (id in idToDoc) {
                // 如果文档已存在，则更新分数
                val existingScore = idToScore[id] ?: 0.0
                val keywordScore = (doc.metadata["score"] as? Double ?: 0.0) * keywordWeight
                idToScore[id] = existingScore + keywordScore
            } else {
                // 如果文档不存在，则添加
                idToDoc[id] = doc
                idToScore[id] = (doc.metadata["score"] as? Double ?: 0.0) * keywordWeight
            }
        }
        
        // 按分数排序并限制结果数量
        return idToDoc.keys
            .sortedByDescending { idToScore[it] ?: 0.0 }
            .take(limit)
            .map { id ->
                val doc = idToDoc[id]!!
                // 更新文档的分数
                val updatedMetadata = doc.metadata.toMutableMap()
                updatedMetadata["score"] = idToScore[id] ?: 0.0
                doc.copy(metadata = updatedMetadata)
            }
    }
}
