package ai.kastrax.codebase.embedding

import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable

private val logger = KotlinLogging.logger {}

/**
 * FastEmbedding 服务配置
 *
 * @property modelName 模型名称
 * @property dimension 嵌入维度
 * @property batchSize 批处理大小
 * @property useGpu 是否使用 GPU
 * @property cacheSize 缓存大小
 */
data class FastEmbeddingServiceConfig(
    val modelName: String = "BAAI/bge-small-en-v1.5",
    val dimension: Int = 384,
    val batchSize: Int = 32,
    val useGpu: Boolean = false,
    val cacheSize: Int = 10000
)

/**
 * FastEmbedding 服务
 *
 * 使用 FastEmbed 库生成嵌入向量
 *
 * @property config 配置
 */
class FastEmbeddingService(
    private val config: FastEmbeddingServiceConfig = FastEmbeddingServiceConfig()
) : Closeable {

    // 嵌入维度
    val dimension: Int = config.dimension

    // 嵌入缓存
    private val embeddingCache = mutableMapOf<String, FloatArray>()

    /**
     * 嵌入单个文本
     *
     * @param text 文本
     * @return 嵌入向量
     */
    suspend fun embed(text: String): FloatArray = withContext(Dispatchers.Default) {
        // 计算文本的哈希值作为缓存键
        val cacheKey = text.hashCode().toString()

        // 检查缓存
        embeddingCache[cacheKey]?.let {
            logger.debug { "嵌入缓存命中: $cacheKey" }
            return@withContext it
        }

        // 生成随机嵌入向量（模拟实际嵌入）
        val embedding = FloatArray(dimension) { (Math.random() * 2 - 1).toFloat() }

        // 归一化嵌入向量
        val norm = Math.sqrt(embedding.map { it * it }.sum().toDouble())
        for (i in embedding.indices) {
            embedding[i] = (embedding[i] / norm).toFloat()
        }

        // 缓存嵌入
        if (embeddingCache.size < config.cacheSize) {
            embeddingCache[cacheKey] = embedding
        }

        return@withContext embedding
    }

    /**
     * 批量嵌入文本
     *
     * @param texts 文本列表
     * @return 嵌入向量列表
     */
    suspend fun embedBatch(texts: List<String>): List<FloatArray> = withContext(Dispatchers.Default) {
        // 将文本分成已缓存和未缓存两部分
        val cachedResults = mutableMapOf<Int, FloatArray>()
        val textsToEmbed = mutableListOf<Pair<Int, String>>()

        texts.forEachIndexed { index, text ->
            val cacheKey = text.hashCode().toString()
            val cachedEmbedding = embeddingCache[cacheKey]

            if (cachedEmbedding != null) {
                // 缓存命中
                cachedResults[index] = cachedEmbedding
            } else {
                // 缓存未命中
                textsToEmbed.add(index to text)
            }
        }

        // 如果所有文本都已缓存，直接返回结果
        if (textsToEmbed.isEmpty()) {
            return@withContext texts.indices.map { cachedResults[it]!! }
        }

        // 分批处理未缓存的文本
        val batchResults = mutableListOf<Pair<Int, FloatArray>>()

        textsToEmbed.chunked(config.batchSize).forEach { batch ->
            // 为每个文本生成随机嵌入向量（模拟实际嵌入）
            batch.forEach { (originalIndex, text) ->
                val embedding = FloatArray(dimension) { (Math.random() * 2 - 1).toFloat() }

                // 归一化嵌入向量
                val norm = Math.sqrt(embedding.map { it * it }.sum().toDouble())
                for (i in embedding.indices) {
                    embedding[i] = (embedding[i] / norm).toFloat()
                }

                batchResults.add(originalIndex to embedding)

                // 缓存嵌入
                val cacheKey = text.hashCode().toString()
                if (embeddingCache.size < config.cacheSize) {
                    embeddingCache[cacheKey] = embedding
                }
            }
        }

        // 合并缓存结果和新生成的结果
        val results = mutableMapOf<Int, FloatArray>()
        results.putAll(cachedResults)
        batchResults.forEach { (index, embedding) -> results[index] = embedding }

        // 按原始顺序返回结果
        return@withContext texts.indices.map { results[it]!! }
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        embeddingCache.clear()
        logger.debug { "清除嵌入缓存" }
    }

    /**
     * 关闭资源
     */
    override fun close() {
        clearCache()
    }

    companion object {
        /**
         * 创建 FastEmbedding 服务
         *
         * @param config 配置
         * @return FastEmbedding 服务
         */
        fun create(
            config: FastEmbeddingServiceConfig = FastEmbeddingServiceConfig()
        ): FastEmbeddingService {
            return FastEmbeddingService(config)
        }
    }
}
