package ai.kastrax.rag.embedding

import ai.kastrax.rag.document.Document
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.linear.RealVector
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

/**
 * 表示文本的嵌入向量。
 *
 * @property vector 嵌入向量
 */
data class Embedding(val vector: List<Float>) {
    /**
     * 计算与另一个嵌入向量的余弦相似度。
     *
     * @param other 另一个嵌入向量
     * @return 余弦相似度，范围为 [-1, 1]
     */
    fun cosineSimilarity(other: Embedding): Double {
        if (vector.isEmpty() || other.vector.isEmpty()) {
            return 0.0
        }

        if (vector.size != other.vector.size) {
            throw IllegalArgumentException("Vectors must have the same dimension")
        }

        val v1 = ArrayRealVector(vector.map { it.toDouble() }.toDoubleArray())
        val v2 = ArrayRealVector(other.vector.map { it.toDouble() }.toDoubleArray())

        return cosineSimilarity(v1, v2)
    }

    /**
     * 计算与另一个嵌入向量的欧几里得距离。
     *
     * @param other 另一个嵌入向量
     * @return 欧几里得距离
     */
    fun euclideanDistance(other: Embedding): Double {
        if (vector.isEmpty() || other.vector.isEmpty()) {
            return 0.0
        }

        if (vector.size != other.vector.size) {
            throw IllegalArgumentException("Vectors must have the same dimension")
        }

        val v1 = ArrayRealVector(vector.map { it.toDouble() }.toDoubleArray())
        val v2 = ArrayRealVector(other.vector.map { it.toDouble() }.toDoubleArray())

        return v1.getDistance(v2)
    }

    /**
     * 计算与另一个嵌入向量的点积。
     *
     * @param other 另一个嵌入向量
     * @return 点积
     */
    fun dotProduct(other: Embedding): Double {
        if (vector.isEmpty() || other.vector.isEmpty()) {
            return 0.0
        }

        if (vector.size != other.vector.size) {
            throw IllegalArgumentException("Vectors must have the same dimension")
        }

        val v1 = ArrayRealVector(vector.map { it.toDouble() }.toDoubleArray())
        val v2 = ArrayRealVector(other.vector.map { it.toDouble() }.toDoubleArray())

        return v1.dotProduct(v2)
    }

    companion object {
        /**
         * 计算两个向量的余弦相似度。
         *
         * @param v1 第一个向量
         * @param v2 第二个向量
         * @return 余弦相似度，范围为 [-1, 1]
         */
        private fun cosineSimilarity(v1: RealVector, v2: RealVector): Double {
            val dotProduct = v1.dotProduct(v2)
            val normV1 = v1.norm
            val normV2 = v2.norm

            return if (normV1 > 0 && normV2 > 0) {
                dotProduct / (normV1 * normV2)
            } else {
                0.0
            }
        }
    }
}

/**
 * 表示带有嵌入向量的文档。
 *
 * @property document 原始文档
 * @property embedding 文档的嵌入向量
 */
data class EmbeddedDocument(
    val document: Document,
    val embedding: Embedding
)

// EmbeddingService interface moved to EmbeddingService.kt

/**
 * 简单的随机嵌入服务，用于测试和开发。
 *
 * @property dimensions 嵌入向量的维度
 * @property seed 随机数生成器的种子
 */
class RandomEmbeddingService(
    private val dimensions: Int = 1536,
    private val seed: Long = 42
) : ai.kastrax.rag.embedding.EmbeddingService {

    private val random = java.util.Random(seed)

    override suspend fun embed(text: String): FloatArray {
        // 使用文本的哈希码作为随机数生成器的种子，以确保相同的文本生成相同的嵌入
        val textSeed = text.hashCode().toLong()
        val textRandom = java.util.Random(textSeed)

        // 生成随机向量
        val vector = FloatArray(dimensions) {
            // 生成 [-1, 1] 范围内的随机浮点数
            textRandom.nextFloat() * 2 - 1
        }

        // 归一化向量
        val norm = sqrt(vector.sumOf { it * it.toDouble() })
        if (norm > 0) {
            for (i in vector.indices) {
                vector[i] = (vector[i] / norm).toFloat()
            }
        }

        return vector
    }

    override suspend fun embedBatch(texts: List<String>): List<FloatArray> {
        return texts.map { embed(it) }
    }

    override fun dimension(): Int {
        return dimensions
    }

    override fun close() {
        // Nothing to close for random embedding service
    }
}
