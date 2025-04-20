package ai.kastrax.fastembed.rag

import ai.kastrax.fastembed.AsyncTextEmbedding
import ai.kastrax.fastembed.Embedding
import ai.kastrax.fastembed.vectorstore.SearchResult
import ai.kastrax.fastembed.vectorstore.VectorStore
import ai.kastrax.fastembed.vectorstore.VectorStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable

/**
 * An asynchronous simple RAG (Retrieval-Augmented Generation) system.
 * This class demonstrates how to use FastEmbed Kotlin for retrieval in a RAG system with coroutines.
 *
 * @property embeddingModel The asynchronous text embedding model
 * @property vectorStore The vector store for document storage and retrieval
 */
class AsyncSimpleRAG(
    private val embeddingModel: AsyncTextEmbedding,
    private val vectorStore: VectorStore = VectorStoreFactory.createInMemoryStore(embeddingModel.dimension)
) : Closeable {
    
    /**
     * Add a document to the RAG system asynchronously.
     *
     * @param id The document ID
     * @param text The document text
     * @param metadata Additional metadata for the document
     * @return True if the document was added successfully
     */
    suspend fun addDocument(id: String, text: String, metadata: Map<String, String> = emptyMap()): Boolean {
        val embedding = embeddingModel.embed(text)
        return withContext(Dispatchers.IO) {
            vectorStore.addItem(id, embedding, metadata + ("text" to text))
        }
    }
    
    /**
     * Add multiple documents to the RAG system asynchronously.
     * This method uses parallel embedding generation for better performance.
     *
     * @param documents The documents to add, each as a triple of (id, text, metadata)
     * @return The number of documents added successfully
     */
    suspend fun addDocuments(documents: List<Triple<String, String, Map<String, String>>>): Int {
        // Generate embeddings in parallel
        val embeddings = embeddingModel.embedParallel(documents.map { it.second })
        
        return withContext(Dispatchers.IO) {
            val items = documents.zip(embeddings) { (id, text, metadata), embedding ->
                Triple(id, embedding, metadata + ("text" to text))
            }
            
            vectorStore.addItems(items)
        }
    }
    
    /**
     * Query the RAG system asynchronously.
     *
     * @param query The query text
     * @param topK The number of results to return
     * @return A list of search results
     */
    suspend fun query(query: String, topK: Int = 5): List<SearchResult> {
        val queryEmbedding = embeddingModel.embed(query)
        return withContext(Dispatchers.IO) {
            vectorStore.search(queryEmbedding, topK)
        }
    }
    
    /**
     * Query the RAG system with a pre-computed embedding asynchronously.
     *
     * @param queryEmbedding The query embedding
     * @param topK The number of results to return
     * @return A list of search results
     */
    suspend fun query(queryEmbedding: Embedding, topK: Int = 5): List<SearchResult> {
        return withContext(Dispatchers.IO) {
            vectorStore.search(queryEmbedding, topK)
        }
    }
    
    /**
     * Delete a document from the RAG system asynchronously.
     *
     * @param id The document ID
     * @return True if the document was deleted successfully
     */
    suspend fun deleteDocument(id: String): Boolean {
        return withContext(Dispatchers.IO) {
            vectorStore.delete(id)
        }
    }
    
    /**
     * Get the number of documents in the RAG system asynchronously.
     *
     * @return The number of documents
     */
    suspend fun documentCount(): Int {
        return withContext(Dispatchers.IO) {
            vectorStore.count()
        }
    }
    
    /**
     * Clear all documents from the RAG system asynchronously.
     *
     * @return True if the RAG system was cleared successfully
     */
    suspend fun clearDocuments(): Boolean {
        return withContext(Dispatchers.IO) {
            vectorStore.clear()
        }
    }
    
    /**
     * Release resources.
     */
    override fun close() {
        vectorStore.close()
        embeddingModel.close()
    }
}
