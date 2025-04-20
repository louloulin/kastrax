package ai.kastrax.fastembed

/**
 * Represents a vector embedding.
 * 
 * @property vector The embedding vector as a FloatArray
 */
class Embedding(val vector: FloatArray) {
    
    /**
     * The dimension of the embedding.
     */
    val dimension: Int
        get() = vector.size
    
    /**
     * Calculate the cosine similarity between this embedding and another.
     * 
     * @param other The other embedding
     * @return The cosine similarity (between -1 and 1)
     */
    fun cosineSimilarity(other: Embedding): Float {
        require(dimension == other.dimension) {
            "Cannot calculate similarity between embeddings of different dimensions: $dimension vs ${other.dimension}"
        }
        
        return TextEmbeddingNative.cosineSimilarity(vector, other.vector)
    }
    
    /**
     * Calculate the dot product between this embedding and another.
     * 
     * @param other The other embedding
     * @return The dot product
     */
    fun dotProduct(other: Embedding): Float {
        require(dimension == other.dimension) {
            "Cannot calculate dot product between embeddings of different dimensions: $dimension vs ${other.dimension}"
        }
        
        var result = 0f
        for (i in vector.indices) {
            result += vector[i] * other.vector[i]
        }
        return result
    }
    
    /**
     * Calculate the Euclidean distance between this embedding and another.
     * 
     * @param other The other embedding
     * @return The Euclidean distance
     */
    fun euclideanDistance(other: Embedding): Float {
        require(dimension == other.dimension) {
            "Cannot calculate distance between embeddings of different dimensions: $dimension vs ${other.dimension}"
        }
        
        var sum = 0f
        for (i in vector.indices) {
            val diff = vector[i] - other.vector[i]
            sum += diff * diff
        }
        return kotlin.math.sqrt(sum)
    }
    
    /**
     * Normalize this embedding to unit length.
     * 
     * @return A new normalized embedding
     */
    fun normalize(): Embedding {
        val norm = kotlin.math.sqrt(vector.fold(0f) { acc, value -> acc + value * value })
        if (norm == 0f) {
            return this
        }
        
        val normalized = FloatArray(dimension) { i -> vector[i] / norm }
        return Embedding(normalized)
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as Embedding
        
        if (!vector.contentEquals(other.vector)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        return vector.contentHashCode()
    }
    
    override fun toString(): String {
        val prefix = if (dimension <= 6) vector.joinToString(", ") 
                     else vector.take(3).joinToString(", ") + ", ..., " + vector.takeLast(3).joinToString(", ")
        return "Embedding(dimension=$dimension, vector=[$prefix])"
    }
}
