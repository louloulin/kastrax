package ai.kastrax.rag.embedding

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.pow

private val logger = KotlinLogging.logger {}

/**
 * 使用 Hugging Face Inference API 的嵌入服务。
 *
 * @property apiKey Hugging Face API 密钥
 * @property modelId 要使用的模型 ID，默认为 "sentence-transformers/all-MiniLM-L6-v2"
 * @property maxRetries 最大重试次数
 * @property timeout 请求超时时间（毫秒）
 */
class HuggingFaceEmbeddingService(
    private val apiKey: String,
    private val modelId: String = "sentence-transformers/all-MiniLM-L6-v2",
    private val maxRetries: Int = 3,
    private val timeout: Long = 30000,
    private val dimensions: Int = 384 // Default dimension for all-MiniLM-L6-v2
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
     * 为文本生成嵌入向量。
     *
     * @param text 要嵌入的文本
     * @return 嵌入向量
     */
    override suspend fun embed(text: String): FloatArray {
        val embedding = embedWithRetry(text)
        return FloatArray(embedding.size) { i -> embedding[i] }
    }

    /**
     * 为多个文本生成嵌入向量。
     *
     * @param texts 要嵌入的文本列表
     * @return 嵌入向量列表
     */
    override suspend fun embedBatch(texts: List<String>): List<FloatArray> {
        val embeddings = embedBatchWithRetry(texts)
        return embeddings.map { embedding -> FloatArray(embedding.size) { i -> embedding[i] } }
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
     * 关闭服务，释放资源。
     */
    override fun close() {
        client.close()
    }

    /**
     * 使用重试机制为文本生成嵌入向量。
     *
     * @param text 要嵌入的文本
     * @param retryCount 当前重试次数
     * @return 嵌入向量
     */
    private suspend fun embedWithRetry(text: String, retryCount: Int = 0): List<Float> {
        try {
            val response = client.post("https://api-inference.huggingface.co/models/$modelId") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody(HuggingFaceEmbeddingRequest(inputs = text))
            }

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                logger.error { "Hugging Face API error: ${response.status} - $errorBody" }

                if (retryCount < maxRetries) {
                    val backoffTime = (2.0.pow(retryCount.toDouble()) * 1000).toLong()
                    logger.warn { "Retrying in ${backoffTime}ms..." }
                    delay(backoffTime)
                    return embedWithRetry(text, retryCount + 1)
                }

                throw Exception("Hugging Face API error: ${response.status} - $errorBody")
            }

            val embeddings = response.body<List<List<Float>>>()
            return embeddings.first()
        } catch (e: Exception) {
            if (retryCount < maxRetries) {
                val backoffTime = (2.0.pow(retryCount.toDouble()) * 1000).toLong()
                logger.warn { "Error embedding text, retrying in ${backoffTime}ms: ${e.message}" }
                delay(backoffTime)
                return embedWithRetry(text, retryCount + 1)
            } else {
                logger.error(e) { "Failed to embed text after $maxRetries retries" }
                throw e
            }
        }
    }

    /**
     * 使用重试机制为多个文本生成嵌入向量。
     *
     * @param texts 要嵌入的文本列表
     * @param retryCount 当前重试次数
     * @return 嵌入向量列表
     */
    private suspend fun embedBatchWithRetry(texts: List<String>, retryCount: Int = 0): List<List<Float>> {
        try {
            val response = client.post("https://api-inference.huggingface.co/models/$modelId") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody(HuggingFaceEmbeddingRequest(inputs = texts))
            }

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                logger.error { "Hugging Face API error: ${response.status} - $errorBody" }

                if (retryCount < maxRetries) {
                    val backoffTime = (2.0.pow(retryCount.toDouble()) * 1000).toLong()
                    logger.warn { "Retrying in ${backoffTime}ms..." }
                    delay(backoffTime)
                    return embedBatchWithRetry(texts, retryCount + 1)
                }

                throw Exception("Hugging Face API error: ${response.status} - $errorBody")
            }

            return response.body<List<List<Float>>>()
        } catch (e: Exception) {
            if (retryCount < maxRetries) {
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
    private data class HuggingFaceEmbeddingRequest(
        @Serializable(with = AnySerializer::class)
        val inputs: Any
    )

    /**
     * 用于序列化 Any 类型的序列化器。
     */
    private object AnySerializer : kotlinx.serialization.KSerializer<Any> {
        override val descriptor: kotlinx.serialization.descriptors.SerialDescriptor =
            kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("Any", kotlinx.serialization.descriptors.PrimitiveKind.STRING)

        override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Any) {
            when (value) {
                is String -> encoder.encodeString(value)
                is List<*> -> {
                    val jsonArray = kotlinx.serialization.json.buildJsonArray {
                        value.forEach { item ->
                            if (item is String) {
                                add(kotlinx.serialization.json.JsonPrimitive(item))
                            }
                        }
                    }
                    encoder.encodeString(jsonArray.toString())
                }
                else -> encoder.encodeString(value.toString())
            }
        }

        override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Any {
            return decoder.decodeString()
        }
    }
}
