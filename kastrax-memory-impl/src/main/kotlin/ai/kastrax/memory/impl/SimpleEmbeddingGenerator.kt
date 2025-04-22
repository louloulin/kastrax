package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.memory.api.EmbeddingGenerator
import kotlin.math.sqrt

/**
 * 简单的嵌入生成器实现，用于测试和演示。
 * 注意：这不是生产环境中应该使用的实现，仅用于测试。
 */
class SimpleEmbeddingGenerator : EmbeddingGenerator, KastraXBase(component = "EMBEDDING_GENERATOR", name = "simple") {
    private val dimension = 128
    
    override suspend fun generateEmbedding(text: String): List<Float> {
        // 简单的哈希函数，将文本转换为固定维度的向量
        val vector = FloatArray(dimension) { 0f }
        
        // 使用文本的哈希值初始化向量
        val hash = text.hashCode()
        val random = java.util.Random(hash.toLong())
        
        // 填充向量
        for (i in 0 until dimension) {
            vector[i] = random.nextFloat() * 2 - 1 // 范围：[-1, 1]
        }
        
        // 归一化向量
        val norm = sqrt(vector.map { it * it }.sum())
        return vector.map { it / norm }.toList()
    }
    
    override suspend fun generateEmbeddings(texts: List<String>): List<List<Float>> {
        return texts.map { generateEmbedding(it) }
    }
}
