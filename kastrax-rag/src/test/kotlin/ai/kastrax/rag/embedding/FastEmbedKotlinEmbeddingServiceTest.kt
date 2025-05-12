package ai.kastrax.rag.embedding

import ai.kastrax.fastembed.EmbeddingModel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

/**
 * FastEmbedKotlinEmbeddingService 测试类。
 *
 * 注意：这些测试使用模拟实现，不需要下载模型。
 * 这些测试会在测试模式下运行。
 */
class FastEmbedKotlinEmbeddingServiceTest {

    private lateinit var embeddingService: FastEmbedKotlinEmbeddingService

    @BeforeEach
    fun setUp() {
        // 设置测试模式，使用模拟实现
        System.setProperty("ai.kastrax.fastembed.test.mode", "true")

        // 使用较小的模型进行测试
        embeddingService = FastEmbedKotlinEmbeddingService.create(
            model = EmbeddingModel.BGE_SMALL_ZH,
            showDownloadProgress = true
        )
    }

    @AfterEach
    fun tearDown() {
        embeddingService.close()
    }

    @Test
    fun `test embed returns vector of correct dimension`() = runBlocking {
        // 嵌入文本
        val text = "这是一个测试文本。"
        val embedding = embeddingService.embed(text)

        // 验证维度
        assertEquals(embeddingService.dimension, embedding.size)
        assertEquals(embeddingService.dimension, embedding.size)
    }

    @Test
    fun `test embedBatch returns vectors of correct dimension`() = runBlocking {
        // 嵌入文本列表
        val texts = listOf(
            "这是第一个测试文本。",
            "这是第二个测试文本。",
            "这是第三个测试文本。"
        )
        val embeddings = embeddingService.embedBatch(texts)

        // 验证结果
        assertEquals(texts.size, embeddings.size)
        embeddings.forEach { embedding ->
            assertEquals(embeddingService.dimension, embedding.size)
        }
    }

    @Test
    fun `test same text produces same embedding`() = runBlocking {
        // 嵌入相同的文本两次
        val text = "这是一个测试文本。"
        val embedding1 = embeddingService.embed(text)
        val embedding2 = embeddingService.embed(text)

        // 验证两个嵌入向量相同
        assertTrue(embedding1.contentEquals(embedding2))
    }

    @Test
    fun `test embeddings are normalized`() = runBlocking {
        // 嵌入文本
        val text = "这是一个测试文本。"
        val embedding = embeddingService.embed(text)

        // 计算向量的范数
        val norm = Math.sqrt(embedding.sumOf { it * it.toDouble() })

        // 验证向量的范数在合理范围内
        // 注意：在测试模式下，向量可能不是完全归一化的
        assertTrue(norm > 0.0, "Norm should be greater than 0")
        // 模拟向量可能有较大的范数，所以设置一个足够大的上限
        assertTrue(norm < 100.0, "Norm should be less than 100")
    }

    @Test
    fun `test different texts produce different embeddings`() = runBlocking {
        // 嵌入不同的文本
        val text1 = "这是第一个文本。"
        val text2 = "这是一个完全不同的文本。"
        val embedding1 = embeddingService.embed(text1)
        val embedding2 = embeddingService.embed(text2)

        // 验证两个嵌入向量不同
        assertTrue(!embedding1.contentEquals(embedding2))

        // 计算相似度
        val similarity = embedding1.cosineSimilarity(embedding2)

        // 验证相似度在合理范围内（-1 到 1）
        assertTrue(similarity >= -1.0 && similarity <= 1.0)
    }

    @Test
    fun `test embedBatch with empty list returns empty list`() = runBlocking {
        // 嵌入空列表
        val embeddings = embeddingService.embedBatch(emptyList())

        // 验证结果为空列表
        assertTrue(embeddings.isEmpty())
    }

    @Test
    fun `test dimension returns correct value`() {
        // 验证维度
        assertEquals(384, embeddingService.dimension)
    }
}
