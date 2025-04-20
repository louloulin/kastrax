package ai.kastrax.fastembed

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.nio.file.Path

/**
 * An asynchronous wrapper for TextEmbedding that provides coroutine-based API.
 * This class allows embedding generation to be performed in a non-blocking way.
 *
 * @property textEmbedding The underlying TextEmbedding instance
 */
class AsyncTextEmbedding private constructor(
    private val textEmbedding: TextEmbedding
) : Closeable {
    
    /**
     * The dimension of the embeddings produced by this model.
     */
    val dimension: Int
        get() = textEmbedding.dimension
    
    /**
     * Generate embeddings for multiple texts asynchronously.
     *
     * @param texts The texts to embed
     * @param batchSize The batch size (null for default)
     * @return A list of Embedding objects
     */
    suspend fun embed(texts: List<String>, batchSize: Int? = null): List<Embedding> = 
        withContext(Dispatchers.Default) {
            textEmbedding.embed(texts, batchSize)
        }
    
    /**
     * Generate an embedding for a single text asynchronously.
     *
     * @param text The text to embed
     * @return An Embedding object
     */
    suspend fun embed(text: String): Embedding = 
        withContext(Dispatchers.Default) {
            textEmbedding.embed(text)
        }
    
    /**
     * Calculate the cosine similarity between two texts asynchronously.
     *
     * @param text1 The first text
     * @param text2 The second text
     * @return The cosine similarity (between -1 and 1)
     */
    suspend fun similarity(text1: String, text2: String): Float = 
        withContext(Dispatchers.Default) {
            textEmbedding.similarity(text1, text2)
        }
    
    /**
     * Calculate the cosine similarity between two embeddings asynchronously.
     *
     * @param embedding1 The first embedding
     * @param embedding2 The second embedding
     * @return The cosine similarity (between -1 and 1)
     */
    suspend fun similarity(embedding1: Embedding, embedding2: Embedding): Float = 
        withContext(Dispatchers.Default) {
            textEmbedding.similarity(embedding1, embedding2)
        }
    
    /**
     * Find the most similar texts to a query text asynchronously.
     *
     * @param query The query text
     * @param candidates The candidate texts to compare against
     * @param topK The number of top results to return (null for all)
     * @return A list of pairs containing the index and similarity score, sorted by similarity (highest first)
     */
    suspend fun findSimilar(query: String, candidates: List<String>, topK: Int? = null): List<Pair<Int, Float>> = 
        withContext(Dispatchers.Default) {
            textEmbedding.findSimilar(query, candidates, topK)
        }
    
    /**
     * Find the most similar embeddings to a query embedding asynchronously.
     *
     * @param queryEmbedding The query embedding
     * @param candidateEmbeddings The candidate embeddings to compare against
     * @param topK The number of top results to return (null for all)
     * @return A list of pairs containing the index and similarity score, sorted by similarity (highest first)
     */
    suspend fun findSimilar(
        queryEmbedding: Embedding, 
        candidateEmbeddings: List<Embedding>, 
        topK: Int? = null
    ): List<Pair<Int, Float>> = 
        withContext(Dispatchers.Default) {
            textEmbedding.findSimilar(queryEmbedding, candidateEmbeddings, topK)
        }
    
    /**
     * Generate embeddings for multiple texts in parallel using coroutines.
     * This method splits the input into chunks and processes them concurrently.
     *
     * @param texts The texts to embed
     * @param chunkSize The number of texts to process in each chunk
     * @param batchSize The batch size for each chunk (null for default)
     * @return A list of Embedding objects
     */
    suspend fun embedParallel(
        texts: List<String>, 
        chunkSize: Int = 100,
        batchSize: Int? = null
    ): List<Embedding> = coroutineScope {
        if (texts.isEmpty()) {
            return@coroutineScope emptyList()
        }
        
        // Split texts into chunks for parallel processing
        val chunks = texts.chunked(chunkSize)
        
        // Process each chunk in parallel
        val deferredResults = chunks.map { chunk ->
            async(Dispatchers.Default) {
                textEmbedding.embed(chunk, batchSize)
            }
        }
        
        // Await all results and flatten the list
        deferredResults.awaitAll().flatten()
    }
    
    /**
     * Release the model and free its resources.
     * This should be called when the model is no longer needed.
     */
    override fun close() {
        textEmbedding.close()
    }
    
    companion object {
        /**
         * Create a new asynchronous text embedding model.
         *
         * @param model The embedding model to use
         * @param cacheDir The cache directory for model files (null for default)
         * @param showDownloadProgress Whether to show download progress
         * @return A new AsyncTextEmbedding instance
         */
        fun create(
            model: EmbeddingModel = EmbeddingModel.DEFAULT,
            cacheDir: Path? = null,
            showDownloadProgress: Boolean = false
        ): AsyncTextEmbedding {
            val textEmbedding = TextEmbedding.create(model, cacheDir, showDownloadProgress)
            return AsyncTextEmbedding(textEmbedding)
        }
    }
}
