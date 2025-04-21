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
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.min
import kotlin.math.pow

private val logger = KotlinLogging.logger {}

/**
 * OpenAI 嵌入服务，使用 OpenAI API 生成文本的嵌入向量。
 *
 * @property apiKey OpenAI API 密钥
 * @property model 嵌入模型名称，默认为 "text-embedding-ada-002"
 * @property batchSize 批处理大小，默认为 16
 * @property maxRetries 最大重试次数，默认为 3
 * @property timeout 请求超时时间（毫秒），默认为 30000
 */
class OpenAIEmbeddingService(
    private val apiKey: String,
    private val model: String = "text-embedding-ada-002",
    private val batchSize: Int = 16,
    private val maxRetries: Int = 3,
    private val timeout: Long = 30000,
    private val dimensions: Int = 1536 // Default dimension for text-embedding-ada-002
) : EmbeddingService {

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

    override suspend fun embed(text: String): FloatArray {
        return embedBatch(listOf(text)).first()
    }

    override suspend fun embedBatch(texts: List<String>): List<FloatArray> {
        if (texts.isEmpty()) {
            return emptyList()
        }
        return embedBatchInternal(texts)
    }

    override fun dimension(): Int {
        return dimensions
    }

    override fun close() {
        client.close()
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

            return embeddingResponse.data.map { data ->
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

    @Serializable
    private data class OpenAIEmbeddingRequest(
        val model: String,
        val input: List<String>
    )

    @Serializable
    private data class OpenAIEmbeddingResponse(
        val data: List<OpenAIEmbeddingData>,
        val model: String,
        val usage: OpenAIUsage
    )

    @Serializable
    private data class OpenAIEmbeddingData(
        val embedding: List<Float>,
        val index: Int,
        @SerialName("object") val objectType: String
    )

    @Serializable
    private data class OpenAIUsage(
        @SerialName("prompt_tokens") val promptTokens: Int,
        @SerialName("total_tokens") val totalTokens: Int
    )
}
