package ai.kastrax.memory.impl

import ai.kastrax.memory.api.EmbeddingGenerator
import ai.kastrax.memory.api.Memory
import ai.kastrax.memory.api.MemoryBuilder
import ai.kastrax.memory.api.MemoryCompressor
import ai.kastrax.memory.api.MemoryCompressionConfig
import ai.kastrax.memory.api.MemoryPriorityConfig
import ai.kastrax.memory.api.MemoryProcessor
import ai.kastrax.memory.api.StructuredMemoryConfig
import ai.kastrax.memory.api.VectorStorage
import ai.kastrax.memory.api.WorkingMemoryConfig

/**
 * 增强型内存构建器。
 */
class EnhancedMemoryBuilder : MemoryBuilder {
    private var storage: Any = InMemoryStorage()
    private var lastMessagesCount: Int = 10
    private var semanticRecallEnabled: Boolean = false
    private var embeddingGenerator: EmbeddingGenerator? = null
    private var vectorStorage: VectorStorage? = null
    private var processors: MutableList<MemoryProcessor> = mutableListOf()
    private var workingMemoryConfig: WorkingMemoryConfig? = null
    private var memoryCompressor: MemoryCompressor? = null
    private var compressionConfig: MemoryCompressionConfig = MemoryCompressionConfig()
    private var useHybridSearch: Boolean = false
    private var keywordWeight: Float = 0.3f
    private var semanticWeight: Float = 0.7f
    private var reranker: RelevanceReranker? = null
    private var tagManagerEnabled: Boolean = false
    private var threadSharingEnabled: Boolean = false
    private var priorityConfig: MemoryPriorityConfig = MemoryPriorityConfig()
    private var structuredMemoryConfig: StructuredMemoryConfig = StructuredMemoryConfig()

    override fun storage(storage: Any): EnhancedMemoryBuilder {
        this.storage = storage
        return this
    }

    override fun lastMessages(count: Int): EnhancedMemoryBuilder {
        this.lastMessagesCount = count
        return this
    }

    override fun semanticRecall(enabled: Boolean): EnhancedMemoryBuilder {
        this.semanticRecallEnabled = enabled
        return this
    }

    override fun embeddingGenerator(generator: EmbeddingGenerator): EnhancedMemoryBuilder {
        this.embeddingGenerator = generator
        return this
    }

    override fun vectorStorage(storage: VectorStorage): EnhancedMemoryBuilder {
        this.vectorStorage = storage
        return this
    }

    override fun processor(processor: MemoryProcessor): EnhancedMemoryBuilder {
        this.processors.add(processor)
        return this
    }

    override fun workingMemory(config: WorkingMemoryConfig): EnhancedMemoryBuilder {
        this.workingMemoryConfig = config
        return this
    }

    override fun priorityConfig(config: MemoryPriorityConfig): EnhancedMemoryBuilder {
        this.priorityConfig = config
        return this
    }

    override fun structuredMemoryConfig(config: StructuredMemoryConfig): EnhancedMemoryBuilder {
        this.structuredMemoryConfig = config
        return this
    }

    /**
     * 设置内存压缩器。
     */
    fun memoryCompressor(compressor: MemoryCompressor): EnhancedMemoryBuilder {
        this.memoryCompressor = compressor
        return this
    }

    /**
     * 设置内存压缩配置。
     */
    fun compressionConfig(config: MemoryCompressionConfig): EnhancedMemoryBuilder {
        this.compressionConfig = config
        return this
    }

    /**
     * 启用混合搜索。
     */
    fun hybridSearch(enabled: Boolean): EnhancedMemoryBuilder {
        this.useHybridSearch = enabled
        return this
    }

    /**
     * 设置搜索权重。
     */
    fun searchWeights(keywordWeight: Float, semanticWeight: Float): EnhancedMemoryBuilder {
        require(keywordWeight + semanticWeight == 1.0f) {
            "关键词权重和语义权重之和必须为1.0"
        }
        this.keywordWeight = keywordWeight
        this.semanticWeight = semanticWeight
        return this
    }

    /**
     * 设置重排序器。
     */
    fun reranker(reranker: RelevanceReranker): EnhancedMemoryBuilder {
        this.reranker = reranker
        return this
    }

    /**
     * 使用标准重排序器。
     */
    fun standardReranker(): EnhancedMemoryBuilder {
        this.reranker = SemanticSearchFactory.createStandardReranker()
        return this
    }

    /**
     * 启用标签管理器。
     */
    fun tagManager(enabled: Boolean): EnhancedMemoryBuilder {
        this.tagManagerEnabled = enabled
        return this
    }

    /**
     * 启用线程共享。
     */
    fun threadSharing(enabled: Boolean): EnhancedMemoryBuilder {
        this.threadSharingEnabled = enabled
        return this
    }

    override fun build(): Memory {
        // 创建基础内存
        val baseMemory = EnhancedMemory(
            storage = storage,
            lastMessagesCount = lastMessagesCount,
            semanticRecallEnabled = semanticRecallEnabled,
            embeddingGenerator = embeddingGenerator,
            vectorStorage = vectorStorage,
            processors = processors,
            workingMemoryConfig = workingMemoryConfig,
            memoryCompressor = memoryCompressor,
            compressionConfig = compressionConfig,
            tagManagerEnabled = tagManagerEnabled,
            threadSharingEnabled = threadSharingEnabled,
            priorityConfig = priorityConfig,
            structuredMemoryConfig = structuredMemoryConfig
        )

        // 如果启用了混合搜索，创建混合搜索内存
        val canUseHybridSearch = useHybridSearch && semanticRecallEnabled
        val hasRequiredComponents = embeddingGenerator != null && vectorStorage != null

        if (canUseHybridSearch && hasRequiredComponents) {
            val hybridMemory = SemanticSearchFactory.createHybridSearchMemory(
                baseMemory = baseMemory,
                embeddingGenerator = embeddingGenerator!!,
                vectorStorage = vectorStorage!!,
                keywordWeight = keywordWeight,
                semanticWeight = semanticWeight
            )

            // 如果有重排序器，添加重排序器
            if (reranker != null && hybridMemory is HybridSearchMemoryImpl) {
                // TODO: 实现重排序器支持
            }

            return hybridMemory
        }

        return baseMemory
    }
}

/**
 * 创建增强型内存的DSL函数。
 */
fun enhancedMemory(init: EnhancedMemoryBuilder.() -> Unit): Memory {
    val builder = EnhancedMemoryBuilder()
    builder.init()
    return builder.build()
}
