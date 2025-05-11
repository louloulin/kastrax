package ai.kastrax.rag.embedding

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

/**
 * 随机嵌入服务，用于测试和开发。
 *
 * @property dimensions 嵌入向量的维度
 * @property seed 随机数生成器的种子
 */
class RandomEmbeddingService(
    private val dimensions: Int = 1536,
    private val seed: Long = 42
) : EmbeddingService() {
    private val random = java.util.Random(seed)

    /**
     * 计算文本的嵌入向量。
     *
     * @param text 输入文本
     * @return 嵌入向量
     */
    override suspend fun embed(text: String): FloatArray = withContext(Dispatchers.IO) {
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

        return@withContext vector
    }

    /**
     * 批量计算文本的嵌入向量。
     *
     * @param texts 输入文本列表
     * @return 嵌入向量列表
     */
    override suspend fun embedBatch(texts: List<String>): List<FloatArray> = withContext(Dispatchers.IO) {
        return@withContext texts.map { embed(it) }
    }

    /**
     * 嵌入向量的维度。
     */
    override val dimension: Int
        get() = dimensions
}
