package ai.kastrax.store.embedding

/**
 * 嵌入服务接口，用于将文本转换为向量。
 */
interface EmbeddingService {

    /**
     * 将文本转换为向量。
     *
     * @param text 文本
     * @return 向量
     */
    suspend fun embed(text: String): FloatArray

    /**
     * 批量将文本转换为向量。
     *
     * @param texts 文本列表
     * @return 向量列表
     */
    suspend fun embedBatch(texts: List<String>): List<FloatArray>
}
