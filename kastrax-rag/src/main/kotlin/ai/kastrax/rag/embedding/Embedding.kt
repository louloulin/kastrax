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

/**
 * 嵌入服务接口，用于生成文本的嵌入向量。
 */
interface EmbeddingService {
    /**
     * 为文本生成嵌入向量。
     *
     * @param text 要嵌入的文本
     * @return 嵌入向量
     */
    suspend fun embed(text: String): Embedding
    
    /**
     * 为多个文本生成嵌入向量。
     *
     * @param texts 要嵌入的文本列表
     * @return 嵌入向量列表
     */
    suspend fun embedBatch(texts: List<String>): List<Embedding> {
        return coroutineScope {
            texts.map { text ->
                async { embed(text) }
            }.awaitAll()
        }
    }
    
    /**
     * 为文档生成嵌入向量。
     *
     * @param document 要嵌入的文档
     * @return 带有嵌入向量的文档
     */
    suspend fun embedDocument(document: Document): EmbeddedDocument {
        val embedding = embed(document.content)
        return EmbeddedDocument(document, embedding)
    }
    
    /**
     * 为多个文档生成嵌入向量。
     *
     * @param documents 要嵌入的文档列表
     * @return 带有嵌入向量的文档列表
     */
    suspend fun embedDocuments(documents: List<Document>): List<EmbeddedDocument> {
        return coroutineScope {
            documents.map { document ->
                async { embedDocument(document) }
            }.awaitAll()
        }
    }
}

/**
 * 简单的随机嵌入服务，用于测试和开发。
 *
 * @property dimensions 嵌入向量的维度
 * @property seed 随机数生成器的种子
 */
class RandomEmbeddingService(
    private val dimensions: Int = 1536,
    private val seed: Long = 42
) : EmbeddingService {
    
    private val random = java.util.Random(seed)
    
    override suspend fun embed(text: String): Embedding {
        // 使用文本的哈希码作为随机数生成器的种子，以确保相同的文本生成相同的嵌入
        val textSeed = text.hashCode().toLong()
        val textRandom = java.util.Random(textSeed)
        
        // 生成随机向量
        val vector = List(dimensions) {
            // 生成 [-1, 1] 范围内的随机浮点数
            textRandom.nextFloat() * 2 - 1
        }
        
        // 归一化向量
        val norm = sqrt(vector.sumOf { it * it.toDouble() })
        val normalizedVector = if (norm > 0) {
            vector.map { (it / norm).toFloat() }
        } else {
            vector
        }
        
        return Embedding(normalizedVector)
    }
}
