package ai.kastrax.memory.impl

/**
 * 内存工厂，用于创建测试所需的内存组件。
 */
object MemoryFactory {
    /**
     * 创建内存存储。
     */
    fun createInMemoryStorage(): InMemoryStorage {
        return InMemoryStorage()
    }
}
