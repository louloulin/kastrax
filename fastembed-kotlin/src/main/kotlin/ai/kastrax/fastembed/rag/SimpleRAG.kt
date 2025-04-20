package ai.kastrax.fastembed.rag

import ai.kastrax.fastembed.Embedding
import ai.kastrax.fastembed.TextEmbedding
import ai.kastrax.fastembed.vectorstore.SearchResult
import ai.kastrax.fastembed.vectorstore.VectorStore
import ai.kastrax.fastembed.vectorstore.VectorStoreFactory
import java.io.Closeable

/**
 * A simple RAG (Retrieval-Augmented Generation) system.
 * This class demonstrates how to use FastEmbed Kotlin for retrieval in a RAG system.
 *
 * @property embeddingModel The text embedding model
 * @property vectorStore The vector store for document storage and retrieval
 */
class SimpleRAG(
    private val embeddingModel: TextEmbedding,
    private val vectorStore: VectorStore = VectorStoreFactory.createInMemoryStore(embeddingModel)
) : Closeable {
    
    /**
     * Add a document to the RAG system.
     *
     * @param id The document ID
     * @param text The document text
     * @param metadata Additional metadata for the document
     * @return True if the document was added successfully
     */
    fun addDocument(id: String, text: String, metadata: Map<String, String> = emptyMap()): Boolean {
        val embedding = embeddingModel.embed(text)
        return vectorStore.addItem(id, embedding, metadata + ("text" to text))
    }
    
    /**
     * Add multiple documents to the RAG system.
     *
     * @param documents The documents to add, each as a triple of (id, text, metadata)
     * @return The number of documents added successfully
     */
    fun addDocuments(documents: List<Triple<String, String, Map<String, String>>>): Int {
        val embeddings = embeddingModel.embed(documents.map { it.second })
        
        val items = documents.zip(embeddings) { (id, text, metadata), embedding ->
            Triple(id, embedding, metadata + ("text" to text))
        }
        
        return vectorStore.addItems(items)
    }
    
    /**
     * Query the RAG system.
     *
     * @param query The query text
     * @param topK The number of results to return
     * @return A list of search results
     */
    fun query(query: String, topK: Int = 5): List<SearchResult> {
        val queryEmbedding = embeddingModel.embed(query)
        return vectorStore.search(queryEmbedding, topK)
    }
    
    /**
     * Query the RAG system with a pre-computed embedding.
     *
     * @param queryEmbedding The query embedding
     * @param topK The number of results to return
     * @return A list of search results
     */
    fun query(queryEmbedding: Embedding, topK: Int = 5): List<SearchResult> {
        return vectorStore.search(queryEmbedding, topK)
    }
    
    /**
     * Delete a document from the RAG system.
     *
     * @param id The document ID
     * @return True if the document was deleted successfully
     */
    fun deleteDocument(id: String): Boolean {
        return vectorStore.delete(id)
    }
    
    /**
     * Get the number of documents in the RAG system.
     *
     * @return The number of documents
     */
    fun documentCount(): Int {
        return vectorStore.count()
    }
    
    /**
     * Clear all documents from the RAG system.
     *
     * @return True if the RAG system was cleared successfully
     */
    fun clearDocuments(): Boolean {
        return vectorStore.clear()
    }
    
    /**
     * Release resources.
     */
    override fun close() {
        vectorStore.close()
        embeddingModel.close()
    }
}
