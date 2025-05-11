package ai.kastrax.store.chroma

import ai.kastrax.store.VectorStore

/**
 * Chroma 向量存储工厂
 */
object ChromaVectorStoreFactory {
    /**
     * 创建向量存储
     *
     * @param options 配置选项
     * @return 向量存储
     */
    fun createVectorStore(options: Map<String, Any> = emptyMap()): VectorStore {
        val host = options["host"] as? String ?: "localhost"
        val port = options["port"] as? Int ?: 8000
        val apiPath = options["apiPath"] as? String ?: ""
        val tenant = options["tenant"] as? String ?: "default_tenant"
        val database = options["database"] as? String ?: "default_database"

        return ChromaVectorStore(host, port, apiPath, tenant, database)
    }
}
