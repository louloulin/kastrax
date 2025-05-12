package ai.kastrax.rag.examples

import ai.kastrax.store.embedding.EmbeddingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 模拟嵌入服务，用于测试。
 */
class MockEmbeddingService : EmbeddingService() {
    /**
     * 嵌入向量的维度。
     */
    override val dimension: Int = 128

    /**
     * 嵌入文本。
     *
     * @param text 文本
     * @return 嵌入向量
     */
    override suspend fun embed(text: String): FloatArray = withContext(Dispatchers.IO) {
        // 返回一个固定的嵌入向量
        return@withContext FloatArray(dimension) { 0.1f }
    }

    /**
     * 批量嵌入文本。
     *
     * @param texts 文本列表
     * @return 嵌入向量列表
     */
    override suspend fun embedBatch(texts: List<String>): List<FloatArray> = withContext(Dispatchers.IO) {
        // 返回固定的嵌入向量列表
        return@withContext texts.map { FloatArray(dimension) { 0.1f } }
    }
}
