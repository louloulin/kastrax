package ai.kastrax.rag

import ai.kastrax.rag.document.Document
import ai.kastrax.rag.document.RecursiveCharacterTextSplitter
import ai.kastrax.rag.embedding.RandomEmbeddingService
import ai.kastrax.rag.reranker.KeywordMatchReranker
import ai.kastrax.rag.vectorstore.InMemoryVectorStore
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class RAGTest {

    private lateinit var rag: RAG
    private lateinit var vectorStore: InMemoryVectorStore
    private lateinit var embeddingService: RandomEmbeddingService

    @BeforeEach
    fun setUp() {
        vectorStore = InMemoryVectorStore()
        embeddingService = RandomEmbeddingService(dimensions = 128)
        rag = RAG(vectorStore, embeddingService)
    }

    @Test
    fun `test reranking with keyword match reranker`() = runBlocking {
        // 创建测试文档
        val documents = listOf(
            Document("人工智能是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。",
                mapOf("title" to "人工智能简介", "category" to "技术")),
            Document("机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习。",
                mapOf("title" to "机器学习基础", "category" to "技术")),
            Document("深度学习是机器学习的一种特定方法，它使用多层神经网络来模拟人脑的工作方式。",
                mapOf("title" to "深度学习入门", "category" to "技术")),
            Document("自然语言处理是人工智能的一个分支，专注于使计算机理解和生成人类语言。",
                mapOf("title" to "自然语言处理概述", "category" to "技术")),
            Document("计算机视觉是人工智能的一个领域，它使计算机能够从图像或视频中获取信息。",
                mapOf("title" to "计算机视觉技术", "category" to "技术"))
        )

        // 添加文档到向量存储
        vectorStore.addDocuments(documents, embeddingService)

        // 创建带有关键词重排序器的 RAG
        val keywordReranker = KeywordMatchReranker(keywordWeight = 0.7, originalScoreWeight = 0.3)
        val ragWithReranker = RAG(vectorStore, embeddingService, keywordReranker)

        // 使用不同的查询测试重排序
        val query1 = "机器学习和深度学习"
        val resultsWithoutReranking = rag.search(query1, applyReranking = false)
        val resultsWithReranking = ragWithReranker.search(query1)

        // 验证结果数量相同
        assertEquals(resultsWithoutReranking.size, resultsWithReranking.size)

        // 验证重排序结果包含关键词的文档排在前面
        val titles = resultsWithReranking.map { it.document.metadata["title"] as String }

        // 打印标题顺序以便调试
        println("Titles order: $titles")

        // 验证包含关键词的文档在前三位
        val topThreeTitles = titles.take(3)
        assertTrue(
            topThreeTitles.contains("机器学习基础") ||
            topThreeTitles.contains("深度学习入门"),
            "重排序结果应该将包含关键词的文档排在前三位"
        )

        // 测试上下文生成
        val contextWithReranking = ragWithReranker.generateContext(query1, limit = 3)
        assertNotNull(contextWithReranking)
        assertTrue(contextWithReranking.isNotEmpty())

        // 测试带元数据的上下文生成
        val contextWithMetadata = ragWithReranker.generateContextWithMetadata(
            query1,
            limit = 3,
            includeMetadata = true,
            metadataKeys = listOf("title")
        )
        assertNotNull(contextWithMetadata)
        assertTrue(contextWithMetadata.isNotEmpty())
        assertTrue(contextWithMetadata.contains("[Source: title:"))
    }

    @Test
    fun `test adding and searching documents`() = runBlocking {
        // 创建测试文档
        val documents = listOf(
            Document("人工智能是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。",
                mapOf("title" to "人工智能简介", "category" to "技术")),
            Document("机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习。",
                mapOf("title" to "机器学习基础", "category" to "技术")),
            Document("深度学习是机器学习的一种特定方法，它使用多层神经网络来模拟人脑的工作方式。",
                mapOf("title" to "深度学习入门", "category" to "技术")),
            Document("自然语言处理是人工智能的一个分支，专注于使计算机理解和生成人类语言。",
                mapOf("title" to "自然语言处理概述", "category" to "技术")),
            Document("计算机视觉是人工智能的一个领域，它使计算机能够从图像或视频中获取信息。",
                mapOf("title" to "计算机视觉技术", "category" to "技术"))
        )

        // 添加文档到向量存储
        val addedCount = vectorStore.addDocuments(documents, embeddingService)
        assertEquals(5, addedCount)
        assertEquals(5, vectorStore.count())

        // 搜索相关文档
        val results = rag.search("机器学习和深度学习", limit = 5)
        assertTrue(results.size > 0, "搜索结果不应为空")
        assertTrue(results.size <= 5, "搜索结果不应超过限制")

        // 验证搜索结果包含相关文档
        val titles = results.map { it.document.metadata["title"] as String }
        assertTrue(titles.contains("机器学习基础") || titles.contains("深度学习入门"))
    }

    @Test
    fun `test document splitting and context generation`() = runBlocking {
        // 创建一个长文档
        val longDocument = Document("""
            人工智能（Artificial Intelligence，简称AI）是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。

            机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习。机器学习算法通过分析大量数据来识别模式，并使用这些模式进行预测或决策。

            深度学习是机器学习的一种特定方法，它使用多层神经网络来模拟人脑的工作方式。深度学习在图像识别、语音识别和自然语言处理等领域取得了显著成功。

            自然语言处理（NLP）是人工智能的一个分支，专注于使计算机理解和生成人类语言。NLP技术被用于机器翻译、情感分析、文本摘要等应用。

            计算机视觉是人工智能的一个领域，它使计算机能够从图像或视频中获取信息。计算机视觉技术被用于人脸识别、物体检测、自动驾驶等应用。

            强化学习是机器学习的一种方法，它通过与环境交互来学习如何做出决策。强化学习算法通过尝试不同的行动并接收反馈来优化其行为。
        """.trimIndent())

        // 创建文档分割器
        val splitter = RecursiveCharacterTextSplitter(
            chunkSize = 100,
            chunkOverlap = 20
        )

        // 分割文档
        val chunks = splitter.split(longDocument)
        assertTrue(chunks.size > 1)

        // 添加分割后的文档到向量存储
        val addedCount = vectorStore.addDocuments(chunks, embeddingService)
        assertEquals(chunks.size, addedCount)

        // 生成上下文
        val context = rag.generateContext("深度学习和神经网络", limit = 2)
        assertNotNull(context)
        assertTrue(context.isNotEmpty())

        // 生成带元数据的上下文
        val contextWithMetadata = rag.generateContextWithMetadata(
            "自然语言处理",
            limit = 2,
            includeMetadata = true
        )
        assertNotNull(contextWithMetadata)
        assertTrue(contextWithMetadata.isNotEmpty())
        assertTrue(contextWithMetadata.contains("[Source:"))
    }
}
