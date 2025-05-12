package ai.kastrax.store.embedding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * 模拟嵌入服务，用于测试。
 */
class MockEmbeddingService : EmbeddingService {
    /**
     * 嵌入维度
     */
    override val dimension: Int = 3
    
    /**
     * 嵌入单个文本
     *
     * @param text 文本
     * @return 嵌入向量
     */
    override suspend fun embed(text: String): FloatArray = withContext(Dispatchers.Default) {
        // 简单的模拟嵌入：使用文本长度和特定单词的出现次数生成向量
        val length = text.length.toFloat()
        val codebaseCount = text.lowercase().split("codebase").size - 1
        val testCount = text.lowercase().split("test").size - 1
        
        // 归一化向量
        val vector = floatArrayOf(length, codebaseCount.toFloat(), testCount.toFloat())
        normalize(vector)
        
        return@withContext vector
    }
    
    /**
     * 批量嵌入文本
     *
     * @param texts 文本列表
     * @return 嵌入向量列表
     */
    override suspend fun embedBatch(texts: List<String>): List<FloatArray> = withContext(Dispatchers.Default) {
        return@withContext texts.map { text ->
            embed(text)
        }
    }
    
    /**
     * 归一化向量
     *
     * @param vector 向量
     * @return 归一化后的向量
     */
    private fun normalize(vector: FloatArray): FloatArray {
        val magnitude = sqrt(vector.sumOf { it * it.toDouble() }).toFloat()
        if (magnitude > 0) {
            for (i in vector.indices) {
                vector[i] /= magnitude
            }
        }
        return vector
    }
}
