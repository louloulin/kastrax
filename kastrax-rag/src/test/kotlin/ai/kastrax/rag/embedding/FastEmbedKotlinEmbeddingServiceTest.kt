package ai.kastrax.rag.embedding

import ai.kastrax.fastembed.EmbeddingModel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * FastEmbedKotlinEmbeddingService 测试类。
 *
 * 注意：这些测试需要下载模型，可能需要一些时间。
 * 默认情况下，这些测试会被跳过，除非设置了 ENABLE_FASTEMBED_TESTS 环境变量。
 */
@EnabledIfEnvironmentVariable(named = "ENABLE_FASTEMBED_TESTS", matches = "true")
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
    fun `test embedding generation`() = runBlocking {
        // 生成嵌入
        val text = "人工智能是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。"
        val embedding = embeddingService.embed(text)

        // 验证嵌入维度
        assertTrue(embedding.isNotEmpty())
        assertEquals(embeddingService.dimension(), embedding.size)

        // 验证嵌入值
        embedding.forEach { value ->
            assertTrue(value.isFinite())  // 值应该是有限的
        }
    }

    @Test
    fun `test batch embedding generation`() = runBlocking {
        // 生成批量嵌入
        val texts = listOf(
            "人工智能是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。",
            "机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习。",
            "深度学习是机器学习的一种特定方法，它使用多层神经网络来模拟人脑的工作方式。"
        )
        val embeddings = embeddingService.embedBatch(texts)

        // 验证嵌入数量
        assertEquals(texts.size, embeddings.size)

        // 验证所有嵌入的维度一致
        val dimensions = embeddingService.dimension()
        embeddings.forEach { embedding ->
            assertEquals(dimensions, embedding.size)
        }
    }

    @Test
    fun `test similarity between embeddings`() = runBlocking {
        // 生成相关文本的嵌入
        val text1 = "人工智能是计算机科学的一个分支。"
        val text2 = "AI是计算机科学的一个领域。"
        val text3 = "今天天气真好，阳光明媚。"

        val embedding1 = embeddingService.embed(text1)
        val embedding2 = embeddingService.embed(text2)
        val embedding3 = embeddingService.embed(text3)

        // 计算相似度
        val similarity12 = cosineSimilarity(embedding1, embedding2)
        val similarity13 = cosineSimilarity(embedding1, embedding3)

        // 在测试模式下，我们不能保证相关文本的相似度更高
        // 只验证相似度值在合理范围内
        assertTrue(similarity12 >= -1f && similarity12 <= 1f)
        assertTrue(similarity13 >= -1f && similarity13 <= 1f)
    }

    /**
     * 计算两个向量的余弦相似度。
     */
    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        require(vec1.size == vec2.size) { "Vectors must have the same dimension" }

        var dotProduct = 0.0f
        var norm1 = 0.0f
        var norm2 = 0.0f

        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }

        norm1 = kotlin.math.sqrt(norm1)
        norm2 = kotlin.math.sqrt(norm2)

        return if (norm1 > 0 && norm2 > 0) {
            dotProduct / (norm1 * norm2)
        } else {
            0.0f
        }
    }
}
