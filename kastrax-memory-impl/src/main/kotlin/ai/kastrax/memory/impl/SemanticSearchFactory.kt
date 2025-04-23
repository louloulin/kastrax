package ai.kastrax.memory.impl

import ai.kastrax.memory.api.EmbeddingGenerator
import ai.kastrax.memory.api.Memory
import ai.kastrax.memory.api.SemanticMemory
import ai.kastrax.memory.api.VectorStorage

/**
 * 语义搜索工厂，用于创建不同类型的语义搜索实现。
 */
object SemanticSearchFactory {
    /**
     * 创建基本的语义内存实现。
     *
     * @param embeddingGenerator 嵌入生成器
     * @param vectorStorage 向量存储
     * @return 语义内存实现
     */
    fun createBasicSemanticMemory(
        embeddingGenerator: EmbeddingGenerator,
        vectorStorage: VectorStorage
    ): SemanticMemory {
        return InMemorySemanticMemory(embeddingGenerator, vectorStorage)
    }

    /**
     * 创建混合搜索内存实现。
     *
     * @param baseMemory 基础内存实现
     * @param embeddingGenerator 嵌入生成器
     * @param vectorStorage 向量存储
     * @param keywordWeight 关键词匹配权重（0.0-1.0），默认为0.3
     * @param semanticWeight 语义搜索权重（0.0-1.0），默认为0.7
     * @return 混合搜索内存实现
     */
    fun createHybridSearchMemory(
        baseMemory: Memory,
        embeddingGenerator: EmbeddingGenerator,
        vectorStorage: VectorStorage,
        keywordWeight: Float = 0.3f,
        semanticWeight: Float = 0.7f
    ): Memory {
        return HybridSearchMemoryImpl(
            baseMemory = baseMemory,
            embeddingGenerator = embeddingGenerator,
            vectorStorage = vectorStorage,
            keywordWeight = keywordWeight,
            semanticWeight = semanticWeight
        )
    }

    /**
     * 创建多样性重排序器。
     *
     * @param diversityWeight 多样性权重（0.0-1.0），默认为0.3
     * @param similarityThreshold 相似度阈值，默认为0.8
     * @return 多样性重排序器
     */
    fun createDiversityReranker(
        diversityWeight: Float = 0.3f,
        similarityThreshold: Float = 0.8f
    ): RelevanceReranker {
        return DiversityReranker(diversityWeight, similarityThreshold)
    }

    /**
     * 创建上下文感知重排序器。
     *
     * @param contextWeight 上下文权重（0.0-1.0），默认为0.2
     * @return 上下文感知重排序器
     */
    fun createContextAwareReranker(
        contextWeight: Float = 0.2f
    ): RelevanceReranker {
        return ContextAwareReranker(contextWeight)
    }

    /**
     * 创建组合重排序器。
     *
     * @param rerankers 重排序器列表
     * @return 组合重排序器
     */
    fun createCompositeReranker(
        rerankers: List<RelevanceReranker>
    ): RelevanceReranker {
        return CompositeReranker(rerankers)
    }

    /**
     * 创建标准重排序器组合。
     *
     * @return 标准重排序器组合
     */
    fun createStandardReranker(): RelevanceReranker {
        return createCompositeReranker(
            listOf(
                createDiversityReranker(),
                createContextAwareReranker()
            )
        )
    }
}
