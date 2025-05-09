package ai.kastrax.rag.embedding

import java.io.Closeable
import kotlin.math.sqrt

/**
 * 嵌入服务接口，用于计算文本的嵌入向量。
 */
interface EmbeddingService : Closeable {
    /**
     * 计算文本的嵌入向量。
     *
     * @param text 输入文本
     * @return 嵌入向量
     */
    suspend fun embed(text: String): FloatArray
    
    /**
     * 批量计算文本的嵌入向量。
     *
     * @param texts 输入文本列表
     * @return 嵌入向量列表
     */
    suspend fun embedBatch(texts: List<String>): List<FloatArray>
    
    /**
     * 获取嵌入向量的维度。
     *
     * @return 嵌入向量的维度
     */
    fun dimension(): Int
    
    /**
     * 关闭资源的默认实现。
     */
    override fun close() {
        // 默认实现，子类可以覆盖
    }
}

/**
 * 计算两个向量的余弦相似度。
 *
 * @param other 另一个向量
 * @return 余弦相似度，范围为 [-1, 1]
 */
fun FloatArray.cosineSimilarity(other: FloatArray): Double {
    require(this.size == other.size) { "Vectors must have the same dimension" }
    
    var dotProduct = 0.0
    var normA = 0.0
    var normB = 0.0
    
    for (i in this.indices) {
        dotProduct += this[i] * other[i]
        normA += this[i] * this[i]
        normB += other[i] * other[i]
    }
    
    if (normA <= 0.0 || normB <= 0.0) {
        return 0.0
    }
    
    return dotProduct / (sqrt(normA) * sqrt(normB))
}
