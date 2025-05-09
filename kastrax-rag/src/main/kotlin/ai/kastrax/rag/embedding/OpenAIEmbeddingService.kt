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
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

/**
 * OpenAI 嵌入服务，使用 OpenAI API 生成文本的嵌入向量。
 *
 * @property apiKey OpenAI API 密钥
 * @property model 模型名称
 * @property apiUrl API URL
 * @property timeout 超时时间（毫秒）
 * @property maxRetries 最大重试次数
 */
class OpenAIEmbeddingService(
    private val apiKey: String,
    private val model: String = "text-embedding-3-small",
    private val batchSize: Int = 16,
    private val maxRetries: Int = 3,
    private val timeout: Long = 30000,
    private val dimensions: Int = 1536 // Default dimension for text-embedding-3-small
) : EmbeddingService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = timeout
            connectTimeoutMillis = timeout
            socketTimeoutMillis = timeout
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = maxRetries)
            exponentialDelay()
        }
    }

    /**
     * 计算文本的嵌入向量。
     *
     * @param text 输入文本
     * @return 嵌入向量
     */
    override suspend fun embed(text: String): FloatArray {
        return embedBatch(listOf(text)).first()
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
        return@withContext embedBatchInternal(texts)
    }

    private suspend fun embedBatchInternal(texts: List<String>): List<FloatArray> {
        // 将文本分成批次
        val batches = texts.chunked(batchSize)
        val embeddings = mutableListOf<FloatArray>()

        for (batch in batches) {
            val batchEmbeddings = embedBatchWithRetry(batch)
            embeddings.addAll(batchEmbeddings)
        }

        return embeddings
    }

    private suspend fun embedBatchWithRetry(texts: List<String>, retryCount: Int = 0): List<FloatArray> {
        try {
            val request = OpenAIEmbeddingRequest(
                model = model,
                input = texts
            )

            val response = client.post("https://api.openai.com/v1/embeddings") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody(request)
            }

            val embeddingResponse = response.body<OpenAIEmbeddingResponse>()

            return embeddingResponse.data.sortedBy { it.index }.map { data ->
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
        return@withContext cosineSimilarity(embedding1, embedding2)
    }

    /**
     * 获取嵌入向量的维度。
     *
     * @return 嵌入向量的维度
     */
    override fun dimension(): Int {
        return dimensions
    }

    /**
     * 计算余弦相似度。
     *
     * @param v1 第一个向量
     * @param v2 第二个向量
     * @return 余弦相似度，范围为 [0, 1]
     */
    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Double {
        return v1.cosineSimilarity(v2)
    }
}

/**
 * OpenAI 嵌入请求。
 *
 * @property model 模型名称
 * @property input 输入文本列表
 */
@Serializable
data class OpenAIEmbeddingRequest(
    val model: String,
    val input: List<String>
)

/**
 * OpenAI 嵌入响应。
 *
 * @property data 嵌入数据列表
 * @property model 模型名称
 * @property usage 使用情况
 */
@Serializable
data class OpenAIEmbeddingResponse(
    val data: List<OpenAIEmbeddingData>,
    val model: String,
    val usage: OpenAIEmbeddingUsage
)

/**
 * OpenAI 嵌入数据。
 *
 * @property embedding 嵌入向量
 * @property index 索引
 */
@Serializable
data class OpenAIEmbeddingData(
    val embedding: List<Float>,
    val index: Int,
    @SerialName("object") val objectType: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OpenAIEmbeddingData

        if (embedding != other.embedding) return false
        if (index != other.index) return false
        if (objectType != other.objectType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = embedding.hashCode()
        result = 31 * result + index
        result = 31 * result + objectType.hashCode()
        return result
    }
}

/**
 * OpenAI 嵌入使用情况。
 *
 * @property promptTokens 提示令牌数
 * @property totalTokens 总令牌数
 */
@Serializable
data class OpenAIEmbeddingUsage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int
)
