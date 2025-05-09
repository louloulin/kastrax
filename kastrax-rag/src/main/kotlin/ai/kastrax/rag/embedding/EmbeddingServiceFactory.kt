package ai.kastrax.rag.embedding

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 嵌入服务工厂，用于创建不同类型的嵌入服务。
 */
object EmbeddingServiceFactory {
    /**
     * 创建随机嵌入服务。
     *
     * @param dimensions 嵌入向量的维度
     * @param seed 随机数生成器的种子
     * @return 随机嵌入服务
     */
    fun createRandomEmbeddingService(
        dimensions: Int = 1536,
        seed: Long = 42
    ): EmbeddingService {
        logger.debug { "Creating random embedding service with dimensions: $dimensions" }
        return RandomEmbeddingService(dimensions, seed)
    }

    /**
     * 创建 OpenAI 嵌入服务。
     *
     * @param apiKey OpenAI API 密钥
     * @param model 模型名称
     * @param batchSize 批处理大小
     * @param maxRetries 最大重试次数
     * @param timeout 超时时间（毫秒）
     * @return OpenAI 嵌入服务
     */
    fun createOpenAIEmbeddingService(
        apiKey: String,
        model: String = "text-embedding-3-small",
        batchSize: Int = 16,
        maxRetries: Int = 3,
        timeout: Long = 30000
    ): EmbeddingService {
        logger.debug { "Creating OpenAI embedding service with model: $model" }
        return OpenAIEmbeddingService(apiKey, model, batchSize, maxRetries, timeout)
    }

    /**
     * 创建 Deepseek 嵌入服务。
     *
     * @param apiKey Deepseek API 密钥
     * @param model 模型名称
     * @param batchSize 批处理大小
     * @param maxRetries 最大重试次数
     * @param timeout 超时时间（毫秒）
     * @return Deepseek 嵌入服务
     */
    fun createDeepseekEmbeddingService(
        apiKey: String,
        model: String = "text-embedding-v1",
        batchSize: Int = 16,
        maxRetries: Int = 3,
        timeout: Long = 30000
    ): EmbeddingService {
        logger.debug { "Creating Deepseek embedding service with model: $model" }
        return DeepseekEmbeddingService(apiKey, model, batchSize, maxRetries, timeout)
    }

    /**
     * 创建缓存嵌入服务。
     *
     * @param delegate 委托的嵌入服务
     * @param cacheSize 缓存大小
     * @return 缓存嵌入服务
     */
    fun withCache(
        delegate: EmbeddingService,
        cacheSize: Int = 1000
    ): EmbeddingService {
        logger.debug { "Creating cached embedding service with cache size: $cacheSize" }
        return CachedEmbeddingService(delegate, cacheSize)
    }
}
