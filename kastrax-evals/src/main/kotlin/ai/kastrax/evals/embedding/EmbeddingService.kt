package ai.kastrax.evals.embedding

/**
 * 嵌入服务接口，用于生成文本的嵌入向量。
 */
interface EmbeddingService {
    /**
     * 生成文本的嵌入向量。
     *
     * @param text 文本
     * @return 嵌入向量
     */
    suspend fun embed(text: String): FloatArray
    
    /**
     * 批量生成文本的嵌入向量。
     *
     * @param texts 文本列表
     * @return 嵌入向量列表
     */
    suspend fun embedBatch(texts: List<String>): List<FloatArray> {
        return texts.map { embed(it) }
    }
    
    /**
     * 获取嵌入向量的维度。
     *
     * @return 嵌入向量的维度
     */
    fun dimension(): Int
    
    /**
     * 关闭嵌入服务，释放资源。
     */
    fun close()
}

/**
 * 随机嵌入服务，用于测试。
 *
 * @param dimensions 嵌入向量的维度
 */
class RandomEmbeddingService(
    private val dimensions: Int = 384
) : EmbeddingService {
    
    override suspend fun embed(text: String): FloatArray {
        return FloatArray(dimensions) { (Math.random() * 2 - 1).toFloat() }
    }
    
    override fun dimension(): Int {
        return dimensions
    }
    
    override fun close() {
        // 随机嵌入服务不需要关闭任何资源
    }
}
