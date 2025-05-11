package ai.kastrax.store.memory

import ai.kastrax.store.VectorStore

/**
 * 内存向量存储工厂
 */
object MemoryVectorStoreFactory {
    /**
     * 创建内存向量存储
     *
     * @param options 配置选项
     * @return 内存向量存储
     */
    fun createVectorStore(options: Map<String, Any> = emptyMap()): VectorStore {
        val dimension = options["dimension"] as? Int ?: 1536
        val metric = options["metric"] as? String ?: "cosine"
        
        return InMemoryVectorStore()
    }
}
