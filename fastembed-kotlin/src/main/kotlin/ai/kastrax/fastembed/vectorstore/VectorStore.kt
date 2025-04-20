package ai.kastrax.fastembed.vectorstore

import ai.kastrax.fastembed.Embedding
import java.io.Closeable

/**
 * Interface for vector stores that can store and retrieve embeddings.
 */
interface VectorStore : Closeable {
    
    /**
     * Add a document to the vector store.
     *
     * @param id The document ID
     * @param embedding The document embedding
     * @param metadata Additional metadata for the document
     * @return True if the document was added successfully
     */
    fun addItem(id: String, embedding: Embedding, metadata: Map<String, String> = emptyMap()): Boolean
    
    /**
     * Add multiple documents to the vector store.
     *
     * @param items The documents to add, each as a triple of (id, embedding, metadata)
     * @return The number of documents added successfully
     */
    fun addItems(items: List<Triple<String, Embedding, Map<String, String>>>): Int {
        var count = 0
        for ((id, embedding, metadata) in items) {
            if (addItem(id, embedding, metadata)) {
                count++
            }
        }
        return count
    }
    
    /**
     * Search for similar documents.
     *
     * @param queryEmbedding The query embedding
     * @param topK The number of results to return
     * @param scoreThreshold The minimum similarity score (null for no threshold)
     * @return A list of search results
     */
    fun search(
        queryEmbedding: Embedding,
        topK: Int = 10,
        scoreThreshold: Float? = null
    ): List<SearchResult>
    
    /**
     * Delete a document from the vector store.
     *
     * @param id The document ID
     * @return True if the document was deleted successfully
     */
    fun delete(id: String): Boolean
    
    /**
     * Get the number of documents in the vector store.
     *
     * @return The number of documents
     */
    fun count(): Int
    
    /**
     * Clear all documents from the vector store.
     *
     * @return True if the vector store was cleared successfully
     */
    fun clear(): Boolean
    
    /**
     * Get a document by ID.
     *
     * @param id The document ID
     * @return The document, or null if not found
     */
    fun get(id: String): Document?
    
    /**
     * Check if a document exists.
     *
     * @param id The document ID
     * @return True if the document exists
     */
    fun exists(id: String): Boolean
    
    /**
     * Get all document IDs.
     *
     * @return A list of document IDs
     */
    fun getAllIds(): List<String>
}

/**
 * A document in a vector store.
 *
 * @property id The document ID
 * @property embedding The document embedding
 * @property metadata Additional metadata for the document
 */
data class Document(
    val id: String,
    val embedding: Embedding,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * A search result from a vector store.
 *
 * @property id The document ID
 * @property score The similarity score
 * @property metadata Additional metadata for the document
 */
data class SearchResult(
    val id: String,
    val score: Float,
    val metadata: Map<String, String> = emptyMap()
)
