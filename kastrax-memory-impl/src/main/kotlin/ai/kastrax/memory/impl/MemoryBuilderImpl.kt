package ai.kastrax.memory.impl

import ai.kastrax.memory.api.EmbeddingGenerator
import ai.kastrax.memory.api.Memory
import ai.kastrax.memory.api.MemoryBuilder
import ai.kastrax.memory.api.MemoryProcessor
import ai.kastrax.memory.api.VectorStorage
import ai.kastrax.memory.api.WorkingMemoryConfig

/**
 * 内存构建器实现。
 */
class MemoryBuilderImpl : MemoryBuilder {
    private var storage: MemoryStorage? = null
    private var lastMessages: Int = 10
    private var semanticRecall: Boolean = false
    private var embeddingGenerator: EmbeddingGenerator? = null
    private var vectorStorage: VectorStorage? = null
    private val processors = mutableListOf<MemoryProcessor>()
    private var workingMemoryConfig: WorkingMemoryConfig? = null

    override fun storage(storage: Any): MemoryBuilder {
        if (storage is MemoryStorage) {
            this.storage = storage
        }
        return this
    }

    override fun lastMessages(count: Int): MemoryBuilder {
        this.lastMessages = count
        return this
    }

    override fun semanticRecall(enabled: Boolean): MemoryBuilder {
        this.semanticRecall = enabled
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

    override fun build(): Memory {
        val finalStorage = storage ?: InMemoryStorage()

        // 如果启用了增强功能，使用EnhancedMemory
        if (embeddingGenerator != null || vectorStorage != null || processors.isNotEmpty() || workingMemoryConfig != null) {
            return EnhancedMemory(
                storage = finalStorage,
                lastMessagesCount = lastMessages,
                semanticRecallEnabled = semanticRecall,
                embeddingGenerator = embeddingGenerator,
                vectorStorage = vectorStorage,
                processors = processors,
                workingMemoryConfig = workingMemoryConfig
            )
        }

        // 否则使用基本的MemoryImpl
        return MemoryImpl(
            storage = finalStorage,
            lastMessages = lastMessages,
            semanticRecall = semanticRecall
        )
    }
}
