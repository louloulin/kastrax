package ai.kastrax.fastembed.integration

import ai.kastrax.fastembed.EmbeddingModel
import ai.kastrax.fastembed.TextEmbedding
import java.io.Closeable
import java.nio.file.Path

/**
 * An embedding service implementation for KastraX using FastEmbed.
 * This class is designed to be integrated with KastraX RAG systems.
 *
 * @property textEmbedding The underlying TextEmbedding instance
 */
class KastraXEmbeddingService private constructor(
    private val textEmbedding: TextEmbedding
) : Closeable {
    
    /**
     * The dimension of the embeddings produced by this service.
     */
    val dimension: Int
        get() = textEmbedding.dimension
    
    /**
     * Generate an embedding for a single text.
     *
     * @param text The text to embed
     * @return The embedding as a FloatArray
     */
    fun embed(text: String): FloatArray {
        return textEmbedding.embed(text).vector
    }
    
    /**
     * Generate embeddings for multiple texts.
     *
     * @param texts The texts to embed
     * @param batchSize The batch size (null for default)
     * @return A list of embeddings, each represented as a FloatArray
     */
    fun embedBatch(texts: List<String>, batchSize: Int? = null): List<FloatArray> {
        return textEmbedding.embed(texts, batchSize).map { it.vector }
    }
    
    /**
     * Calculate the cosine similarity between two texts.
     *
     * @param text1 The first text
     * @param text2 The second text
     * @return The cosine similarity (between -1 and 1)
     */
    fun similarity(text1: String, text2: String): Float {
        return textEmbedding.similarity(text1, text2)
    }
    
    /**
     * Calculate the cosine similarity between two embeddings.
     *
     * @param embedding1 The first embedding
     * @param embedding2 The second embedding
     * @return The cosine similarity (between -1 and 1)
     */
    fun similarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        val e1 = ai.kastrax.fastembed.Embedding(embedding1)
        val e2 = ai.kastrax.fastembed.Embedding(embedding2)
        return e1.cosineSimilarity(e2)
    }
    
    /**
     * Release the model and free its resources.
     * This should be called when the service is no longer needed.
     */
    override fun close() {
        textEmbedding.close()
    }
    
    companion object {
        /**
         * Create a new KastraXEmbeddingService with the specified model.
         *
         * @param model The embedding model to use
         * @param cacheDir The cache directory for model files (null for default)
         * @param showDownloadProgress Whether to show download progress
         * @return A new KastraXEmbeddingService instance
         */
        fun create(
            model: EmbeddingModel = EmbeddingModel.DEFAULT,
            cacheDir: Path? = null,
            showDownloadProgress: Boolean = false
        ): KastraXEmbeddingService {
            val textEmbedding = TextEmbedding.create(model, cacheDir, showDownloadProgress)
            return KastraXEmbeddingService(textEmbedding)
        }
    }
}
