package ai.kastrax.rag.retriever

import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.embedding.EmbeddingService

/**
 * 检索器工厂，用于创建各种检索器。
 */
object RetrieverFactory {
    /**
     * 创建向量存储检索器。
     *
     * @param documentStore 文档向量存储
     * @param embeddingService 嵌入服务
     * @return 向量存储检索器
     */
    fun createVectorStoreRetriever(
        documentStore: DocumentVectorStore,
        embeddingService: EmbeddingService
    ): VectorStoreRetriever {
        return VectorStoreRetriever(documentStore, embeddingService)
    }
    
    /**
     * 创建关键词检索器。
     *
     * @param documentStore 文档向量存储
     * @param keywordExtractor 关键词提取器
     * @return 关键词检索器
     */
    fun createKeywordRetriever(
        documentStore: DocumentVectorStore,
        keywordExtractor: KeywordExtractor = SimpleKeywordExtractor()
    ): KeywordRetriever {
        return KeywordRetriever(documentStore, keywordExtractor)
    }
    
    /**
     * 创建混合检索器。
     *
     * @param documentStore 文档向量存储
     * @param embeddingService 嵌入服务
     * @param keywordExtractor 关键词提取器
     * @param vectorWeight 向量权重
     * @param keywordWeight 关键词权重
     * @return 混合检索器
     */
    fun createHybridRetriever(
        documentStore: DocumentVectorStore,
        embeddingService: EmbeddingService,
        keywordExtractor: KeywordExtractor = SimpleKeywordExtractor(),
        vectorWeight: Double = 0.7,
        keywordWeight: Double = 0.3
    ): HybridRetriever {
        val vectorStoreRetriever = createVectorStoreRetriever(documentStore, embeddingService)
        val keywordRetriever = createKeywordRetriever(documentStore, keywordExtractor)
        
        return HybridRetriever(
            vectorStoreRetriever = vectorStoreRetriever,
            keywordRetriever = keywordRetriever,
            vectorWeight = vectorWeight,
            keywordWeight = keywordWeight
        )
    }
}
