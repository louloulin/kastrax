package ai.kastrax.memory.impl

import ai.kastrax.memory.api.Memory
import ai.kastrax.memory.api.MemoryBuilder

/**
 * 创建内存实例的工厂类。
 */
object MemoryFactory {
    /**
     * 创建内存实例。
     */
    fun createMemory(init: MemoryBuilderImpl.() -> Unit): Memory {
        val builder = MemoryBuilderImpl()
        builder.init()
        return builder.build()
    }
    
    /**
     * 创建内存存储。
     */
    fun createInMemoryStorage(): MemoryStorage {
        return InMemoryStorage()
    }
}

/**
 * 创建内存实例的DSL函数。
 */
fun memory(init: MemoryBuilderImpl.() -> Unit): Memory {
    return MemoryFactory.createMemory(init)
}

/**
 * 创建内存存储的DSL函数。
 */
fun inMemoryStorage(): MemoryStorage {
    return MemoryFactory.createInMemoryStorage()
}
