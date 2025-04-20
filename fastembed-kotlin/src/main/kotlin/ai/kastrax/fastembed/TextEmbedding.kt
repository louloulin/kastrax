package ai.kastrax.fastembed

import java.io.Closeable
import java.nio.file.Path
import kotlin.io.path.absolutePathString

/**
 * A text embedding model that generates vector embeddings for text.
 * This class wraps the fastembed-rs library and provides a Kotlin-friendly API.
 *
 * @property modelId The internal model ID
 * @property dimension The dimension of the embeddings produced by this model
 */
class TextEmbedding private constructor(
    private val modelId: Long,
    val dimension: Int
) : Closeable {

    /**
     * Generate embeddings for multiple texts.
     *
     * @param texts The texts to embed
     * @param batchSize The batch size (null for default)
     * @return A list of Embedding objects
     */
    fun embed(texts: List<String>, batchSize: Int? = null): List<Embedding> {
        if (texts.isEmpty()) {
            return emptyList()
        }

        val actualBatchSize = batchSize ?: 0
        val floatArrays = TextEmbeddingNative.embedTexts(modelId, texts.toTypedArray(), actualBatchSize)
        return floatArrays.map { Embedding(it) }
    }

    /**
     * Generate an embedding for a single text.
     *
     * @param text The text to embed
     * @return An Embedding object
     */
    fun embed(text: String): Embedding {
        val vector = TextEmbeddingNative.embedText(modelId, text)
        return Embedding(vector)
    }

    /**
     * Calculate the cosine similarity between two texts.
     *
     * @param text1 The first text
     * @param text2 The second text
     * @return The cosine similarity (between -1 and 1)
     */
    fun similarity(text1: String, text2: String): Float {
        val embedding1 = embed(text1)
        val embedding2 = embed(text2)
        return embedding1.cosineSimilarity(embedding2)
    }

    /**
     * Calculate the cosine similarity between two embeddings.
     *
     * @param embedding1 The first embedding
     * @param embedding2 The second embedding
     * @return The cosine similarity (between -1 and 1)
     */
    fun similarity(embedding1: Embedding, embedding2: Embedding): Float {
        return embedding1.cosineSimilarity(embedding2)
    }

    /**
     * Find the most similar texts to a query text.
     *
     * @param query The query text
     * @param candidates The candidate texts to compare against
     * @param topK The number of top results to return (null for all)
     * @return A list of pairs containing the index and similarity score, sorted by similarity (highest first)
     */
    fun findSimilar(query: String, candidates: List<String>, topK: Int? = null): List<Pair<Int, Float>> {
        val queryEmbedding = embed(query)
        val candidateEmbeddings = embed(candidates)

        val similarities = candidateEmbeddings.mapIndexed { index, embedding ->
            index to queryEmbedding.cosineSimilarity(embedding)
        }.sortedByDescending { it.second }

        return if (topK != null) similarities.take(topK) else similarities
    }

    /**
     * Find the most similar embeddings to a query embedding.
     *
     * @param queryEmbedding The query embedding
     * @param candidateEmbeddings The candidate embeddings to compare against
     * @param topK The number of top results to return (null for all)
     * @return A list of pairs containing the index and similarity score, sorted by similarity (highest first)
     */
    fun findSimilar(queryEmbedding: Embedding, candidateEmbeddings: List<Embedding>, topK: Int? = null): List<Pair<Int, Float>> {
        val similarities = candidateEmbeddings.mapIndexed { index, embedding ->
            index to queryEmbedding.cosineSimilarity(embedding)
        }.sortedByDescending { it.second }

        return if (topK != null) similarities.take(topK) else similarities
    }

    /**
     * Release the model and free its resources.
     * This should be called when the model is no longer needed.
     */
    override fun close() {
        TextEmbeddingNative.releaseModel(modelId)
    }

    companion object {
        /**
         * Create a new text embedding model.
         *
         * @param model The embedding model to use
         * @param cacheDir The cache directory for model files (null for default)
         * @param showDownloadProgress Whether to show download progress
         * @return A new TextEmbedding instance
         */
        fun create(
            model: EmbeddingModel = EmbeddingModel.DEFAULT,
            cacheDir: Path? = null,
            showDownloadProgress: Boolean = false
        ): TextEmbedding {
            val cacheDirStr = cacheDir?.absolutePathString()
            val modelId = TextEmbeddingNative.createModel(model.id, cacheDirStr, showDownloadProgress)
            val dimension = TextEmbeddingNative.getEmbeddingDimension(modelId)
            return TextEmbedding(modelId, dimension)
        }
    }
}
