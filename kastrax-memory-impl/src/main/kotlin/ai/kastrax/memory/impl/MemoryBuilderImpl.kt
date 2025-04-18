package ai.kastrax.memory.impl

import ai.kastrax.memory.api.Memory
import ai.kastrax.memory.api.MemoryBuilder

/**
 * 内存构建器实现。
 */
class MemoryBuilderImpl : MemoryBuilder {
    private var storage: MemoryStorage? = null
    private var lastMessages: Int = 10
    private var semanticRecall: Boolean = false
    
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
    
    override fun build(): Memory {
        val finalStorage = storage ?: InMemoryStorage()
        return MemoryImpl(
            storage = finalStorage,
            lastMessages = lastMessages,
            semanticRecall = semanticRecall
        )
    }
}
