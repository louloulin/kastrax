package ai.kastrax.ragx.retrieval

import ai.kastrax.store.VectorStore
import ai.kastrax.store.embedding.EmbeddingService

/**
 * 检索器工厂类，用于创建各种检索器。
 */
object RetrieverFactory {
    /**
     * 创建 TopK 检索器。
     *
     * @param vectorStore 向量存储
     * @param embeddingService 嵌入服务
     * @param indexName 索引名称
     * @return TopK 检索器
     */
    fun createTopKRetriever(
        vectorStore: VectorStore,
        embeddingService: EmbeddingService,
        indexName: String = "default"
    ): Retriever {
        return TopKRetriever(vectorStore, embeddingService, indexName)
    }
    
    /**
     * 创建混合检索器。
     *
     * @param vectorStore 向量存储
     * @param embeddingService 嵌入服务
     * @param indexName 索引名称
     * @param vectorWeight 向量权重
     * @param keywordWeight 关键词权重
     * @return 混合检索器
     */
    fun createHybridRetriever(
        vectorStore: VectorStore,
        embeddingService: EmbeddingService,
        indexName: String = "default",
        vectorWeight: Double = 0.7,
        keywordWeight: Double = 0.3
    ): Retriever {
        return HybridRetriever(vectorStore, embeddingService, indexName, vectorWeight, keywordWeight)
    }
}
