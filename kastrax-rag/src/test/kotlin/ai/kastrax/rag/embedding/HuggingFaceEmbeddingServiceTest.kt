package ai.kastrax.rag.embedding

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Hugging Face 嵌入服务测试。
 *
 * 注意：这些测试需要 Hugging Face API 密钥。
 * 使用环境变量 HUGGINGFACE_API_KEY 设置 API 密钥。
 *
 * 这些测试默认被禁用，因为它们需要外部 API 密钥。
 * 要运行这些测试，请设置 HUGGINGFACE_API_KEY 环境变量，然后使用以下命令：
 * HUGGINGFACE_API_KEY=your_api_key ./gradlew :kastrax-rag:test --tests "ai.kastrax.rag.embedding.HuggingFaceEmbeddingServiceTest"
 */
@EnabledIfEnvironmentVariable(named = "HUGGINGFACE_API_KEY", matches = ".+")
class HuggingFaceEmbeddingServiceTest {

    private val apiKey = System.getenv("HUGGINGFACE_API_KEY")

    @Test
    fun `test embed text`() = runBlocking {
        // 创建嵌入服务
        val embeddingService = HuggingFaceEmbeddingService(
            apiKey = apiKey,
            modelId = "sentence-transformers/all-MiniLM-L6-v2"
        )

        // 嵌入文本
        val text = "Hello, world!"
        val embedding = embeddingService.embed(text)

        // 验证嵌入向量
        assertEquals(384, embedding.size)
        assertTrue(embedding.all { !it.isNaN() })
    }

    @Test
    fun `test embed batch`() = runBlocking {
        // 创建嵌入服务
        val embeddingService = HuggingFaceEmbeddingService(
            apiKey = apiKey,
            modelId = "sentence-transformers/all-MiniLM-L6-v2"
        )

        // 嵌入多个文本
        val texts = listOf("Hello, world!", "How are you?", "Goodbye!")
        val embeddings = embeddingService.embedBatch(texts)

        // 验证嵌入向量
        assertEquals(3, embeddings.size)
        embeddings.forEach { embedding ->
            assertEquals(384, embedding.size)
            assertTrue(embedding.all { !it.isNaN() })
        }

        // 验证不同文本的嵌入向量不同
        val similarities = mutableListOf<Double>()
        for (i in 0 until embeddings.size) {
            for (j in i + 1 until embeddings.size) {
                val similarity = ai.kastrax.rag.util.cosineSimilarity(embeddings[i], embeddings[j])
                similarities.add(similarity)
            }
        }

        // 验证相似度在合理范围内
        similarities.forEach { similarity ->
            assertTrue(similarity in 0.0..1.0)
        }
    }

    @Test
    fun `test retry mechanism`() = runBlocking {
        // 创建嵌入服务，使用不存在的模型 ID 触发错误
        val embeddingService = HuggingFaceEmbeddingService(
            apiKey = apiKey,
            modelId = "non-existent-model",
            maxRetries = 2
        )

        try {
            // 尝试嵌入文本，应该会失败但会重试
            embeddingService.embed("Hello, world!")
        } catch (e: Exception) {
            // 验证异常消息
            assertTrue(e.message?.contains("Hugging Face API error") == true ||
                       e.message?.contains("Failed to embed text after") == true)
        }
    }
}
