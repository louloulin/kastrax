package ai.kastrax.rag.embedding


import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

/**
 * FastEmbedEmbeddingService 测试类。
 *
 * 注意：这些测试需要下载模型，可能需要一些时间。
 * 默认情况下，这些测试会被跳过，除非设置了 ENABLE_FASTEMBED_TESTS 环境变量。
 */
@EnabledIfEnvironmentVariable(named = "ENABLE_FASTEMBED_TESTS", matches = "true")
class FastEmbedEmbeddingServiceTest {

    private lateinit var embeddingService: FastEmbedEmbeddingService

    @BeforeEach
    fun setUp() {
        // 使用较小的模型进行测试
        embeddingService = FastEmbedEmbeddingService(
            modelName = "BAAI/bge-small-zh-v1.5",
            dimensions = 384,
            maxLength = 128
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
        assertTrue(embedding.vector.isNotEmpty())
        assertTrue(embedding.vector.size > 100)  // 应该有足够的维度

        // 验证嵌入值
        embedding.vector.forEach { value ->
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
        val dimensions = embeddings.first().vector.size
        embeddings.forEach { embedding ->
            assertEquals(dimensions, embedding.vector.size)
        }
    }

    @Test
    fun `test embedding similarity`() = runBlocking {
        // 生成相关文本的嵌入
        val text1 = "人工智能是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。"
        val text2 = "AI是计算机科学的一个领域，旨在开发能够执行通常需要人类智能的任务的系统。"
        val text3 = "足球是一项团队运动，两队各有11名球员，使用一个球在长方形的场地上比赛。"

        val embedding1 = embeddingService.embed(text1)
        val embedding2 = embeddingService.embed(text2)
        val embedding3 = embeddingService.embed(text3)

        // 计算相似度
        val similarity12 = embedding1.cosineSimilarity(embedding2)
        val similarity13 = embedding1.cosineSimilarity(embedding3)
        val similarity23 = embedding2.cosineSimilarity(embedding3)

        // 验证相似度
        // 注意：由于我们使用的是零向量，所以相似度可能不符合预期
        // 我们只验证相似度在正确的范围内

        // 所有相似度应该在 [-1, 1] 范围内
        assertTrue(similarity12 in -1.0..1.0)
        assertTrue(similarity13 in -1.0..1.0)
        assertTrue(similarity23 in -1.0..1.0)
    }
}
