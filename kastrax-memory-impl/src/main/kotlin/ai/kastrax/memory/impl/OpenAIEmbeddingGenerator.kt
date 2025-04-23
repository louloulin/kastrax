package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.memory.api.EmbeddingGenerator
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * OpenAI嵌入生成器，使用OpenAI API生成文本嵌入。
 *
 * @property apiKey OpenAI API密钥
 * @property model 使用的模型，默认为"text-embedding-3-small"
 * @property dimensions 嵌入向量的维度，默认为1536
 * @property batchSize 批处理大小，默认为16
 */
class OpenAIEmbeddingGenerator(
    private val apiKey: String,
    private val model: String = "text-embedding-3-small",
    private val dimensions: Int = 1536,
    private val batchSize: Int = 16
) : EmbeddingGenerator, KastraXBase(component = "EMBEDDING_GENERATOR", name = "openai") {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    @Serializable
    private data class EmbeddingRequest(
        val model: String,
        val input: List<String>,
        val dimensions: Int? = null
    )

    @Serializable
    private data class EmbeddingResponse(
        val data: List<EmbeddingData>,
        val model: String,
        val usage: EmbeddingUsage
    )

    @Serializable
    private data class EmbeddingData(
        val embedding: List<Float>,
        val index: Int
    )

    @Serializable
    private data class EmbeddingUsage(
        @Suppress("PropertyName") val prompt_tokens: Int,
        @Suppress("PropertyName") val total_tokens: Int
    )

    override suspend fun generateEmbedding(text: String): List<Float> {
        return generateEmbeddings(listOf(text)).first()
    }

    override suspend fun generateEmbeddings(texts: List<String>): List<List<Float>> {
        if (texts.isEmpty()) {
            return emptyList()
        }

        // 批处理请求
        val batches = texts.chunked(batchSize)
        val results = mutableListOf<List<Float>>()

        for (batch in batches) {
            try {
                val response = client.post("https://api.openai.com/v1/embeddings") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $apiKey")
                    setBody(EmbeddingRequest(model, batch, dimensions))
                }

                val embeddingResponse = response.body<EmbeddingResponse>()

                // 按索引排序，确保顺序与输入一致
                val sortedEmbeddings = embeddingResponse.data
                    .sortedBy { it.index }
                    .map { it.embedding }

                results.addAll(sortedEmbeddings)

                logger.debug { "生成了${batch.size}个嵌入向量，使用了${embeddingResponse.usage.total_tokens}个令牌" }
            } catch (e: Exception) {
                logger.error(e) { "生成嵌入向量失败: ${e.message}" }
                throw e
            }
        }

        return results
    }

    /**
     * 关闭HTTP客户端。
     */
    fun close() {
        client.close()
    }
}
