package ai.kastrax.fastembed.vectorstore

import ai.kastrax.fastembed.Embedding
import java.util.concurrent.ConcurrentHashMap

/**
 * An in-memory implementation of a vector store.
 * This is useful for testing and small-scale applications.
 *
 * @property dimension The dimension of the embeddings
 */
class InMemoryVectorStore(private val dimension: Int) : VectorStore {
    
    private val documents = ConcurrentHashMap<String, Document>()
    
    override fun addItem(id: String, embedding: Embedding, metadata: Map<String, String>): Boolean {
        if (embedding.dimension != dimension) {
            throw IllegalArgumentException(
                "Embedding dimension mismatch: expected $dimension, got ${embedding.dimension}"
            )
        }
        
        documents[id] = Document(id, embedding, metadata)
        return true
    }
    
    override fun search(
        queryEmbedding: Embedding,
        topK: Int,
        scoreThreshold: Float?
    ): List<SearchResult> {
        if (queryEmbedding.dimension != dimension) {
            throw IllegalArgumentException(
                "Query embedding dimension mismatch: expected $dimension, got ${queryEmbedding.dimension}"
            )
        }
        
        if (documents.isEmpty()) {
            return emptyList()
        }
        
        val results = documents.values.map { document ->
            val score = queryEmbedding.cosineSimilarity(document.embedding)
            SearchResult(document.id, score, document.metadata)
        }
        
        val filteredResults = if (scoreThreshold != null) {
            results.filter { it.score >= scoreThreshold }
        } else {
            results
        }
        
        return filteredResults.sortedByDescending { it.score }.take(topK)
    }
    
    override fun delete(id: String): Boolean {
        return documents.remove(id) != null
    }
    
    override fun count(): Int {
        return documents.size
    }
    
    override fun clear(): Boolean {
        documents.clear()
        return true
    }
    
    override fun get(id: String): Document? {
        return documents[id]
    }
    
    override fun exists(id: String): Boolean {
        return documents.containsKey(id)
    }
    
    override fun getAllIds(): List<String> {
        return documents.keys.toList()
    }
    
    override fun close() {
        // Nothing to close for in-memory store
    }
}
