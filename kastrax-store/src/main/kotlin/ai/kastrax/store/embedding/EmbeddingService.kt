package ai.kastrax.store.embedding

/**
 * 嵌入服务接口，用于将文本转换为嵌入向量。
 */
interface EmbeddingService {

    /**
     * 将文本转换为嵌入向量。
     *
     * @param text 文本
     * @return 嵌入向量
     */
    suspend fun embed(text: String): FloatArray

    /**
     * 将多个文本转换为嵌入向量。
     *
     * @param texts 文本列表
     * @return 嵌入向量列表
     */
    suspend fun embedBatch(texts: List<String>): List<FloatArray>
}
