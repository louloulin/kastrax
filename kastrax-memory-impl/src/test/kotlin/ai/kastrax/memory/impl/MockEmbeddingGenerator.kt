package ai.kastrax.memory.impl

import ai.kastrax.memory.api.EmbeddingGenerator
import kotlin.random.Random

/**
 * 模拟嵌入生成器，用于测试。
 */
class MockEmbeddingGenerator : EmbeddingGenerator {
    override suspend fun generateEmbedding(text: String): List<Float> {
        // 生成随机嵌入向量
        val random = Random(text.hashCode())
        return List(128) { random.nextFloat() }
    }

    override suspend fun generateEmbeddings(texts: List<String>): List<List<Float>> {
        // 批量生成嵌入向量
        return texts.map { generateEmbedding(it) }
    }
}
