package ai.kastrax.fastembed.vectorstore

import ai.kastrax.fastembed.TextEmbedding

/**
 * Factory for creating vector stores.
 */
object VectorStoreFactory {
    
    /**
     * Create an in-memory vector store.
     *
     * @param dimension The dimension of the embeddings
     * @return An in-memory vector store
     */
    fun createInMemoryStore(dimension: Int): VectorStore {
        return InMemoryVectorStore(dimension)
    }
    
    /**
     * Create an in-memory vector store with the same dimension as the embedding model.
     *
     * @param embeddingModel The embedding model
     * @return An in-memory vector store
     */
    fun createInMemoryStore(embeddingModel: TextEmbedding): VectorStore {
        return InMemoryVectorStore(embeddingModel.dimension)
    }
}
