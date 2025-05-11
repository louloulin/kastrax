package ai.kastrax.store.embedding

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 嵌入服务，用于生成文本的嵌入向量。
 */
open class EmbeddingService {
    /**
     * 嵌入向量的维度。
     */
    open val dimension: Int = 1536

    /**
     * 生成文本的嵌入向量。
     *
     * @param text 文本
     * @return 嵌入向量
     */
    open suspend fun embed(text: String): FloatArray {
        logger.debug { "Embedding text: ${text.take(50)}..." }
        return FloatArray(dimension) { 0.0f }
    }

    /**
     * 批量生成文本的嵌入向量。
     *
     * @param texts 文本列表
     * @return 嵌入向量列表
     */
    open suspend fun embedBatch(texts: List<String>): List<FloatArray> {
        logger.debug { "Embedding ${texts.size} texts" }
        return texts.map { FloatArray(dimension) { 0.0f } }
    }

    /**
     * 关闭嵌入服务。
     */
    open fun close() {
        // 默认实现不做任何操作
    }
}
