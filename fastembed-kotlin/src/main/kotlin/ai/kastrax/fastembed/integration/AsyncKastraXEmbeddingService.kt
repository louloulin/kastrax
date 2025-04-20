package ai.kastrax.fastembed.integration

import ai.kastrax.fastembed.AsyncTextEmbedding
import ai.kastrax.fastembed.EmbeddingModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.nio.file.Path

/**
 * An asynchronous embedding service implementation for KastraX using FastEmbed.
 * This class is designed to be integrated with KastraX RAG systems that support coroutines.
 *
 * @property asyncTextEmbedding The underlying AsyncTextEmbedding instance
 */
class AsyncKastraXEmbeddingService private constructor(
    private val asyncTextEmbedding: AsyncTextEmbedding
) : Closeable {
    
    /**
     * The dimension of the embeddings produced by this service.
     */
    val dimension: Int
        get() = asyncTextEmbedding.dimension
    
    /**
     * Generate an embedding for a single text asynchronously.
     *
     * @param text The text to embed
     * @return The embedding as a FloatArray
     */
    suspend fun embed(text: String): FloatArray {
        return asyncTextEmbedding.embed(text).vector
    }
    
    /**
     * Generate embeddings for multiple texts asynchronously.
     *
     * @param texts The texts to embed
     * @param batchSize The batch size (null for default)
     * @return A list of embeddings, each represented as a FloatArray
     */
    suspend fun embedBatch(texts: List<String>, batchSize: Int? = null): List<FloatArray> {
        return asyncTextEmbedding.embed(texts, batchSize).map { it.vector }
    }
    
    /**
     * Generate embeddings for multiple texts in parallel using coroutines.
     *
     * @param texts The texts to embed
     * @param chunkSize The number of texts to process in each chunk
     * @param batchSize The batch size for each chunk (null for default)
     * @return A list of embeddings, each represented as a FloatArray
     */
    suspend fun embedParallel(
        texts: List<String>,
        chunkSize: Int = 100,
        batchSize: Int? = null
    ): List<FloatArray> {
        return asyncTextEmbedding.embedParallel(texts, chunkSize, batchSize).map { it.vector }
    }
    
    /**
     * Calculate the cosine similarity between two texts asynchronously.
     *
     * @param text1 The first text
     * @param text2 The second text
     * @return The cosine similarity (between -1 and 1)
     */
    suspend fun similarity(text1: String, text2: String): Float {
        return asyncTextEmbedding.similarity(text1, text2)
    }
    
    /**
     * Calculate the cosine similarity between two embeddings asynchronously.
     *
     * @param embedding1 The first embedding
     * @param embedding2 The second embedding
     * @return The cosine similarity (between -1 and 1)
     */
    suspend fun similarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        return withContext(Dispatchers.Default) {
            val e1 = ai.kastrax.fastembed.Embedding(embedding1)
            val e2 = ai.kastrax.fastembed.Embedding(embedding2)
            e1.cosineSimilarity(e2)
        }
    }
    
    /**
     * Release the model and free its resources.
     * This should be called when the service is no longer needed.
     */
    override fun close() {
        asyncTextEmbedding.close()
    }
    
    companion object {
        /**
         * Create a new AsyncKastraXEmbeddingService with the specified model.
         *
         * @param model The embedding model to use
         * @param cacheDir The cache directory for model files (null for default)
         * @param showDownloadProgress Whether to show download progress
         * @return A new AsyncKastraXEmbeddingService instance
         */
        fun create(
            model: EmbeddingModel = EmbeddingModel.DEFAULT,
            cacheDir: Path? = null,
            showDownloadProgress: Boolean = false
        ): AsyncKastraXEmbeddingService {
            val asyncTextEmbedding = AsyncTextEmbedding.create(model, cacheDir, showDownloadProgress)
            return AsyncKastraXEmbeddingService(asyncTextEmbedding)
        }
    }
}
