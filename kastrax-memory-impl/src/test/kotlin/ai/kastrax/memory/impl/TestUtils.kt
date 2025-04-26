package ai.kastrax.memory.impl

import ai.kastrax.memory.api.Memory
import ai.kastrax.memory.api.MemoryBuilder
import ai.kastrax.memory.api.MemoryCompressor
import ai.kastrax.memory.api.MemoryCompressionConfig
import ai.kastrax.memory.api.MemoryPriorityConfig
import ai.kastrax.memory.api.MemoryProcessor
import ai.kastrax.memory.api.MemoryProcessorOptions
import ai.kastrax.memory.api.EmbeddingGenerator
import ai.kastrax.memory.api.StructuredMemoryConfig
import ai.kastrax.memory.api.VectorStorage
import ai.kastrax.memory.api.WorkingMemoryConfig

/**
 * 创建增强型内存的DSL函数，用于测试。
 */
fun createTestMemory(init: TestEnhancedMemoryBuilder.() -> Unit): Memory {
    val builder = TestEnhancedMemoryBuilder()
    builder.init()
    val memory = builder.build()
    println(
        "Created test memory: $memory with storage: ${builder.getStorage()}, " +
        "processors: ${builder.getProcessors().size}, " +
        "workingMemory: ${builder.getWorkingMemoryConfig() != null}"
    )
    return memory
}

/**
 * 测试用增强型内存构建器。
 */
class TestEnhancedMemoryBuilder : MemoryBuilder {
    private var storage: Any? = null
    private var lastMessagesCount: Int = 10
    private var semanticRecallEnabled: Boolean = false
    private var embeddingGenerator: EmbeddingGenerator? = null
    private var vectorStorage: VectorStorage? = null
    private val processors = mutableListOf<MemoryProcessor>()
    private var workingMemoryConfig: WorkingMemoryConfig? = null
    private var memoryCompressor: MemoryCompressor? = null
    private var tagManagerEnabled: Boolean = false
    private var threadSharingEnabled: Boolean = false
    private var priorityConfig: MemoryPriorityConfig = MemoryPriorityConfig()
    private var structuredMemoryConfig: StructuredMemoryConfig = StructuredMemoryConfig()

    // Getter methods for debugging
    fun getStorage(): Any? = storage
    fun getLastMessagesCount(): Int = lastMessagesCount
    fun getSemanticRecallEnabled(): Boolean = semanticRecallEnabled
    fun getEmbeddingGenerator(): EmbeddingGenerator? = embeddingGenerator
    fun getVectorStorage(): VectorStorage? = vectorStorage
    fun getProcessors(): List<MemoryProcessor> = processors
    fun getWorkingMemoryConfig(): WorkingMemoryConfig? = workingMemoryConfig
    fun getMemoryCompressor(): MemoryCompressor? = memoryCompressor
    fun getTagManagerEnabled(): Boolean = tagManagerEnabled
    fun getThreadSharingEnabled(): Boolean = threadSharingEnabled
    fun getPriorityConfig(): MemoryPriorityConfig = priorityConfig
    fun getStructuredMemoryConfig(): StructuredMemoryConfig = structuredMemoryConfig

    override fun storage(storage: Any): MemoryBuilder {
        this.storage = storage
        return this
    }

    override fun lastMessages(count: Int): MemoryBuilder {
        this.lastMessagesCount = count
        return this
    }

    override fun semanticRecall(enabled: Boolean): MemoryBuilder {
        this.semanticRecallEnabled = enabled
        return this
    }

    override fun embeddingGenerator(generator: EmbeddingGenerator): MemoryBuilder {
        this.embeddingGenerator = generator
        return this
    }

    override fun vectorStorage(storage: VectorStorage): MemoryBuilder {
        this.vectorStorage = storage
        return this
    }

    override fun processor(processor: MemoryProcessor): MemoryBuilder {
        this.processors.add(processor)
        return this
    }

    override fun workingMemory(config: WorkingMemoryConfig): MemoryBuilder {
        this.workingMemoryConfig = config
        return this
    }

    override fun priorityConfig(config: MemoryPriorityConfig): MemoryBuilder {
        this.priorityConfig = config
        return this
    }

    override fun structuredMemoryConfig(config: StructuredMemoryConfig): MemoryBuilder {
        this.structuredMemoryConfig = config
        return this
    }

    /**
     * 设置内存压缩器。
     */
    fun memoryCompressor(compressor: MemoryCompressor): TestEnhancedMemoryBuilder {
        this.memoryCompressor = compressor
        return this
    }

    fun compressionConfig(@Suppress("UNUSED_PARAMETER") config: ai.kastrax.memory.api.MemoryCompressionConfig): TestEnhancedMemoryBuilder {
        // 在测试中，我们只需要设置 memoryCompressor，不需要单独设置 compressionConfig
        // 因为 MockMemoryCompressor 会使用传入的 config
        return this
    }

    /**
     * 启用标签管理器。
     */
    fun tagManager(enabled: Boolean): TestEnhancedMemoryBuilder {
        println("Setting tagManagerEnabled to $enabled")
        this.tagManagerEnabled = enabled
        return this
    }

    /**
     * 启用线程共享。
     */
    fun threadSharing(enabled: Boolean): TestEnhancedMemoryBuilder {
        this.threadSharingEnabled = enabled
        return this
    }

    override fun build(): Memory {
        println(
            "Building EnhancedMemory with tagManagerEnabled: $tagManagerEnabled, " +
            "threadSharingEnabled: $threadSharingEnabled"
        )
        return EnhancedMemory(
            storage = storage ?: InMemoryStorage(),
            lastMessagesCount = lastMessagesCount,
            semanticRecallEnabled = semanticRecallEnabled,
            embeddingGenerator = embeddingGenerator,
            vectorStorage = vectorStorage,
            processors = processors,
            workingMemoryConfig = workingMemoryConfig,
            memoryCompressor = memoryCompressor,
            compressionConfig = MemoryCompressionConfig(),
            tagManagerEnabled = tagManagerEnabled,
            threadSharingEnabled = threadSharingEnabled,
            priorityConfig = priorityConfig,
            structuredMemoryConfig = structuredMemoryConfig
        )
    }
}
