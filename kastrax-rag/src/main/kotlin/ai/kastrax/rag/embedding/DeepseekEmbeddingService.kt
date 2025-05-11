package ai.kastrax.rag.embedding

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.pow

private val logger = KotlinLogging.logger {}

/**
 * Deepseek 嵌入服务，使用 Deepseek API 生成文本的嵌入向量。
 *
 * @property apiKey Deepseek API 密钥
 * @property model 嵌入模型名称，默认为 "text-embedding-v1"
 * @property batchSize 批处理大小，默认为 16
 * @property maxRetries 最大重试次数，默认为 3
 * @property timeout 请求超时时间（毫秒），默认为 30000
 * @property dimensions 嵌入向量的维度，默认为 1536
 */
class DeepseekEmbeddingService(
    private val apiKey: String,
    private val model: String = "text-embedding-v1",
    private val batchSize: Int = 16,
    private val maxRetries: Int = 3,
    private val timeout: Long = 30000,
    private val dimensions: Int = 1536
) : EmbeddingService() {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = false
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = timeout
            connectTimeoutMillis = timeout
            socketTimeoutMillis = timeout
        }
    }

    /**
     * 计算文本的嵌入向量。
     *
     * @param text 输入文本
     * @return 嵌入向量
     */
    override suspend fun embed(text: String): FloatArray = withContext(Dispatchers.IO) {
        try {
            logger.debug { "Generating embedding for text of length: ${text.length}" }

            val request = DeepseekEmbeddingRequest(
                model = model,
                input = listOf(text)
            )

            val response = client.post("https://api.deepseek.com/v1/embeddings") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody(request)
            }

            val embeddingResponse = response.body<DeepseekEmbeddingResponse>()
            val embeddingList = embeddingResponse.data.firstOrNull()?.embedding ?: emptyList()
            val embedding = FloatArray(embeddingList.size) { i -> embeddingList[i] }

            logger.debug { "Generated embedding with dimension: ${embedding.size}" }
            return@withContext embedding
        } catch (e: Exception) {
            logger.error(e) { "Error generating embedding" }
            throw e
        }
    }

    /**
     * 批量计算文本的嵌入向量。
     *
     * @param texts 输入文本列表
     * @return 嵌入向量列表
     */
    override suspend fun embedBatch(texts: List<String>): List<FloatArray> = withContext(Dispatchers.IO) {
        if (texts.isEmpty()) {
            return@withContext emptyList()
        }

        // 将文本分成批次
        val batches = texts.chunked(batchSize)
        val embeddings = mutableListOf<FloatArray>()

        for (batch in batches) {
            val batchEmbeddings = embedBatchWithRetry(batch)
            embeddings.addAll(batchEmbeddings)
        }

        return@withContext embeddings
    }

    /**
     * 嵌入向量的维度。
     */
    override val dimension: Int
        get() = dimensions

    /**
     * 关闭资源。
     */
    override fun close() {
        client.close()
    }

    /**
     * 计算两个文本的相似度。
     *
     * @param text1 第一个文本
     * @param text2 第二个文本
     * @return 相似度，范围为 [0, 1]
     */
    suspend fun similarity(text1: String, text2: String): Double = withContext(Dispatchers.IO) {
        val embedding1 = embed(text1)
        val embedding2 = embed(text2)
        return@withContext embedding1.cosineSimilarity(embedding2)
    }

    /**
     * 带重试的批量嵌入。
     *
     * @param texts 输入文本列表
     * @param retryCount 当前重试次数
     * @return 嵌入向量列表
     */
    private suspend fun embedBatchWithRetry(texts: List<String>, retryCount: Int = 0): List<FloatArray> {
        try {
            val request = DeepseekEmbeddingRequest(
                model = model,
                input = texts
            )

            val response = client.post("https://api.deepseek.com/v1/embeddings") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody(request)
            }

            val embeddingResponse = response.body<DeepseekEmbeddingResponse>()

            // 按索引排序，确保顺序与输入一致
            val sortedData = embeddingResponse.data.sortedBy { it.index }
            return sortedData.map { data ->
                FloatArray(data.embedding.size) { i -> data.embedding[i] }
            }
        } catch (e: Exception) {
            if (retryCount < maxRetries) {
                // 指数退避重试
                val backoffTime = (2.0.pow(retryCount.toDouble()) * 1000).toLong()
                logger.warn { "Error embedding batch, retrying in ${backoffTime}ms: ${e.message}" }
                delay(backoffTime)
                return embedBatchWithRetry(texts, retryCount + 1)
            } else {
                logger.error(e) { "Failed to embed batch after $maxRetries retries" }
                throw e
            }
        }
    }
}

/**
 * Deepseek 嵌入请求。
 *
 * @property model 模型名称
 * @property input 输入文本列表
 */
@Serializable
data class DeepseekEmbeddingRequest(
    val model: String,
    val input: List<String>
)

/**
 * Deepseek 嵌入响应。
 *
 * @property data 嵌入数据列表
 * @property model 模型名称
 * @property usage 使用情况
 */
@Serializable
data class DeepseekEmbeddingResponse(
    val data: List<DeepseekEmbeddingData>,
    val model: String,
    val usage: DeepseekEmbeddingUsage
)

/**
 * Deepseek 嵌入数据。
 *
 * @property embedding 嵌入向量
 * @property index 索引
 */
@Serializable
data class DeepseekEmbeddingData(
    val embedding: List<Float>,
    val index: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeepseekEmbeddingData

        if (embedding != other.embedding) return false
        if (index != other.index) return false

        return true
    }

    override fun hashCode(): Int {
        var result = embedding.hashCode()
        result = 31 * result + index
        return result
    }
}

/**
 * Deepseek 嵌入使用情况。
 *
 * @property promptTokens 提示令牌数
 * @property totalTokens 总令牌数
 */
@Serializable
data class DeepseekEmbeddingUsage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int
)
