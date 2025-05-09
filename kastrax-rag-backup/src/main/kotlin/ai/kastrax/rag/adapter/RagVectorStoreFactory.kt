package ai.kastrax.rag.adapter

import ai.kastrax.rag.vectorstore.RagVectorStore
import ai.kastrax.store.VectorStore
import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.inmemory.InMemoryVectorStore

/**
 * RagVectorStore 工厂类，用于创建 RagVectorStore 实例。
 */
object RagVectorStoreFactory {

    /**
     * 创建内存向量存储。
     *
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @return RagVectorStore 实例
     */
    fun createInMemoryVectorStore(
        indexName: String = "rag_index",
        dimension: Int = 1536
    ): RagVectorStore {
        val vectorStore = VectorStoreFactory.createInMemoryVectorStore()
        return RagVectorStoreAdapter(vectorStore, indexName, dimension)
    }

    /**
     * 将 VectorStore 适配为 RagVectorStore。
     *
     * @param vectorStore VectorStore 实例
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @return RagVectorStore 实例
     */
    fun adaptVectorStore(
        vectorStore: VectorStore,
        indexName: String = "rag_index",
        dimension: Int = 1536
    ): RagVectorStore {
        return RagVectorStoreAdapter(vectorStore, indexName, dimension)
    }
}
