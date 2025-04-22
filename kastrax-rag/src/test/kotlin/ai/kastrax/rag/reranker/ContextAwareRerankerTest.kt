package ai.kastrax.rag.reranker

import ai.kastrax.rag.vectorstore.RagDocument
import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.vectorstore.SearchResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContextAwareRerankerTest {

    // 模拟嵌入服务，根据文本内容生成嵌入向量
    private class MockEmbeddingService : EmbeddingService {
        override suspend fun embed(text: String): FloatArray {
            // 简单的模拟嵌入逻辑：根据文本中的关键词生成嵌入向量
            val vector = FloatArray(5) { 0f }

            // 查询相关的关键词
            if (text.contains("人工智能", ignoreCase = true)) vector[0] = 1f
            if (text.contains("机器学习", ignoreCase = true)) vector[1] = 1f

            // 上下文相关的关键词
            if (text.contains("深度学习", ignoreCase = true)) vector[2] = 1f
            if (text.contains("神经网络", ignoreCase = true)) vector[3] = 1f

            // 其他关键词
            if (text.contains("数据", ignoreCase = true)) vector[4] = 1f

            return vector
        }

        override suspend fun embedBatch(texts: List<String>): List<FloatArray> {
            return texts.map { embed(it) }
        }

        override fun dimension(): Int {
            return 5
        }

        override fun close() {
            // 模拟实现，不需要关闭任何资源
        }
    }

    @Test
    fun `test rerank with context`() = runBlocking {
        // 创建测试数据
        val query = "人工智能和机器学习"
        val context = "我正在研究深度学习和神经网络"

        val doc1 = RagDocument(
            id = "1",
            content = "人工智能是计算机科学的一个分支，它关注于开发能够执行通常需要人类智能的任务的系统。",
            metadata = mapOf("source" to "wiki", "id" to "1")
        )

        val doc2 = RagDocument(
            id = "2",
            content = "机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习。",
            metadata = mapOf("source" to "textbook", "id" to "2")
        )

        val doc3 = RagDocument(
            id = "3",
            content = "深度学习是机器学习的一种方法，它使用神经网络来模拟人类大脑的学习过程。",
            metadata = mapOf("source" to "article", "id" to "3")
        )

        val results = listOf(
            SearchResult(doc1, 0.8),
            SearchResult(doc2, 0.7),
            SearchResult(doc3, 0.6)
        )

        // 创建重排序器
        val reranker = ContextAwareReranker(
            embeddingService = MockEmbeddingService(),
            config = ContextAwareRerankerConfig(
                contextWeight = 0.6,
                queryWeight = 0.4,
                originalScoreWeight = 0.3
            )
        )

        // 使用上下文进行重排序
        val rerankedResults = reranker.rerank(query, results, context)

        // 验证结果
        assertEquals(3, rerankedResults.size, "应该返回所有结果")

        // 由于 doc3 包含上下文中的关键词（深度学习和神经网络），它应该排在前面
        assertEquals("3", rerankedResults[0].document.metadata["id"], "包含上下文关键词的文档应该排在前面")

        // 验证分数
        assertTrue(rerankedResults[0].score > rerankedResults[1].score, "第一个结果的分数应该高于第二个")
        assertTrue(rerankedResults[1].score > rerankedResults[2].score, "第二个结果的分数应该高于第三个")
    }

    @Test
    fun `test rerank without context`() = runBlocking {
        // 创建测试数据
        val query = "人工智能和机器学习"

        val doc1 = RagDocument(
            id = "1",
            content = "人工智能是计算机科学的一个分支，它关注于开发能够执行通常需要人类智能的任务的系统。",
            metadata = mapOf("source" to "wiki", "id" to "1")
        )

        val doc2 = RagDocument(
            id = "2",
            content = "机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习。",
            metadata = mapOf("source" to "textbook", "id" to "2")
        )

        val doc3 = RagDocument(
            id = "3",
            content = "深度学习是机器学习的一种方法，它使用神经网络来模拟人类大脑的学习过程。",
            metadata = mapOf("source" to "article", "id" to "3")
        )

        val results = listOf(
            SearchResult(doc1, 0.8),
            SearchResult(doc2, 0.7),
            SearchResult(doc3, 0.6)
        )

        // 创建重排序器
        val reranker = ContextAwareReranker(
            embeddingService = MockEmbeddingService(),
            config = ContextAwareRerankerConfig(
                queryWeight = 0.7,
                originalScoreWeight = 0.3
            )
        )

        // 不使用上下文进行重排序
        val rerankedResults = reranker.rerank(query, results)

        // 验证结果
        assertEquals(3, rerankedResults.size, "应该返回所有结果")

        // 由于 doc2 包含查询中的两个关键词（人工智能和机器学习），它应该排在前面
        assertEquals("2", rerankedResults[0].document.metadata["id"], "包含查询关键词最多的文档应该排在前面")

        // 验证分数
        assertTrue(rerankedResults[0].score > rerankedResults[1].score, "第一个结果的分数应该高于第二个")
        assertTrue(rerankedResults[1].score > rerankedResults[2].score, "第二个结果的分数应该高于第三个")
    }

    @Test
    fun `test rerank with empty results`() = runBlocking {
        // 创建测试数据
        val query = "人工智能"
        val context = "深度学习"
        val results = emptyList<SearchResult>()

        // 创建重排序器
        val reranker = ContextAwareReranker(
            embeddingService = MockEmbeddingService()
        )

        // 使用上下文进行重排序
        val rerankedResults = reranker.rerank(query, results, context)

        // 验证结果
        assertTrue(rerankedResults.isEmpty(), "空结果应该返回空列表")
    }

    @Test
    fun `test rerank with empty context`() = runBlocking {
        // 创建测试数据
        val query = "人工智能"
        val context = ""

        val doc1 = RagDocument(
            id = "1",
            content = "人工智能是计算机科学的一个分支。",
            metadata = mapOf("source" to "wiki", "id" to "1")
        )

        val results = listOf(
            SearchResult(doc1, 0.8)
        )

        // 创建重排序器
        val reranker = ContextAwareReranker(
            embeddingService = MockEmbeddingService()
        )

        // 使用空上下文进行重排序
        val rerankedResults = reranker.rerank(query, results, context)

        // 验证结果
        assertEquals(1, rerankedResults.size, "应该返回所有结果")
        assertEquals("1", rerankedResults[0].document.metadata["id"], "结果应该保持不变")
    }
}
