package ai.kastrax.rag.retrieval

import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.vectorstore.RagDocument
import ai.kastrax.rag.vectorstore.RagVectorStore
import ai.kastrax.rag.vectorstore.SearchResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EnhancedHybridRetrieverTest {

    private lateinit var vectorStore: RagVectorStore
    private lateinit var embeddingService: EmbeddingService
    private lateinit var keywordExtractor: KeywordExtractor
    private lateinit var retriever: EnhancedHybridRetriever

    @BeforeEach
    fun setUp() {
        vectorStore = mockk()
        embeddingService = mockk()
        keywordExtractor = mockk()

        // 设置默认配置
        val config = EnhancedHybridRetrieverConfig(
            vectorWeight = 0.7,
            keywordWeight = 0.3,
            metadataWeight = 0.0,
            hybridStrategy = HybridStrategy.WEIGHTED
        )

        retriever = EnhancedHybridRetriever(vectorStore, embeddingService, keywordExtractor, config)
    }

    @Test
    fun `test weighted hybrid strategy`() = runBlocking {
        // 准备测试数据
        val query = "人工智能"
        val keywords = listOf("人工智能", "AI")
        val limit = 3
        val minScore = 0.5

        // 模拟向量搜索结果
        val vectorResults = listOf(
            SearchResult(RagDocument("1", "人工智能简介", mapOf("category" to "AI")), 0.9),
            SearchResult(RagDocument("2", "机器学习是人工智能的一个子领域", mapOf("category" to "ML")), 0.8),
            SearchResult(RagDocument("3", "深度学习是机器学习的一个子领域", mapOf("category" to "DL")), 0.7)
        )

        // 模拟关键词搜索结果
        val keywordResults = listOf(
            SearchResult(RagDocument("1", "人工智能简介", mapOf("category" to "AI")), 0.95),
            SearchResult(RagDocument("4", "AI的发展历史", mapOf("category" to "AI")), 0.85),
            SearchResult(RagDocument("5", "人工智能的应用", mapOf("category" to "AI")), 0.75)
        )

        // 设置模拟行为
        coEvery { embeddingService.embed(any()) } returns floatArrayOf(0.1f, 0.2f, 0.3f)
        coEvery { keywordExtractor.extractKeywords(any(), any()) } returns keywords
        coEvery { vectorStore.similaritySearch(query, embeddingService, any(), minScore) } returns vectorResults
        coEvery { vectorStore.keywordSearch(keywords, any()) } returns keywordResults

        // 执行测试
        val results = retriever.retrieve(query, limit, minScore)

        // 验证结果
        assertEquals(limit, results.size)

        // 文档1应该排在第一位，因为它在两个结果集中都有高分
        assertEquals("1", results[0].document.id)

        // 验证分数计算
        val doc1Score = 0.9 * 0.7 + 0.95 * 0.3
        assertEquals(doc1Score, results[0].score, 0.001)
    }

    @Test
    fun `test interleave hybrid strategy`() = runBlocking {
        // 准备测试数据
        val query = "人工智能"
        val keywords = listOf("人工智能", "AI")
        val limit = 4
        val minScore = 0.5

        // 设置交错策略
        val config = EnhancedHybridRetrieverConfig(
            hybridStrategy = HybridStrategy.INTERLEAVE
        )
        retriever = EnhancedHybridRetriever(vectorStore, embeddingService, keywordExtractor, config)

        // 模拟向量搜索结果
        val vectorResults = listOf(
            SearchResult(RagDocument("1", "人工智能简介", mapOf("category" to "AI")), 0.9),
            SearchResult(RagDocument("2", "机器学习是人工智能的一个子领域", mapOf("category" to "ML")), 0.8),
            SearchResult(RagDocument("3", "深度学习是机器学习的一个子领域", mapOf("category" to "DL")), 0.7)
        )

        // 模拟关键词搜索结果
        val keywordResults = listOf(
            SearchResult(RagDocument("4", "AI的发展历史", mapOf("category" to "AI")), 0.85),
            SearchResult(RagDocument("5", "人工智能的应用", mapOf("category" to "AI")), 0.75),
            SearchResult(RagDocument("6", "AI与机器人", mapOf("category" to "AI")), 0.65)
        )

        // 设置模拟行为
        coEvery { embeddingService.embed(any()) } returns floatArrayOf(0.1f, 0.2f, 0.3f)
        coEvery { keywordExtractor.extractKeywords(any(), any()) } returns keywords
        coEvery { vectorStore.similaritySearch(query, embeddingService, any(), minScore) } returns vectorResults
        coEvery { vectorStore.keywordSearch(keywords, any()) } returns keywordResults

        // 执行测试
        val results = retriever.retrieve(query, limit, minScore)

        // 验证结果
        assertEquals(limit, results.size)

        // 交错策略应该从每个结果集中依次选择一个文档
        assertEquals("1", results[0].document.id) // 从向量结果中选择
        assertEquals("4", results[1].document.id) // 从关键词结果中选择
        assertEquals("2", results[2].document.id) // 从向量结果中选择
        assertEquals("5", results[3].document.id) // 从关键词结果中选择
    }

    @Test
    fun `test hierarchical hybrid strategy`() = runBlocking {
        // 准备测试数据
        val query = "人工智能"
        val keywords = listOf("人工智能", "AI")
        val limit = 5
        val minScore = 0.5

        // 设置分层策略
        val config = EnhancedHybridRetrieverConfig(
            hybridStrategy = HybridStrategy.HIERARCHICAL
        )
        retriever = EnhancedHybridRetriever(vectorStore, embeddingService, keywordExtractor, config)

        // 模拟向量搜索结果
        val vectorResults = listOf(
            SearchResult(RagDocument("1", "人工智能简介", mapOf("category" to "AI")), 0.9),
            SearchResult(RagDocument("2", "机器学习是人工智能的一个子领域", mapOf("category" to "ML")), 0.8)
        )

        // 模拟关键词搜索结果
        val keywordResults = listOf(
            SearchResult(RagDocument("3", "AI的发展历史", mapOf("category" to "AI")), 0.85),
            SearchResult(RagDocument("4", "人工智能的应用", mapOf("category" to "AI")), 0.75),
            SearchResult(RagDocument("5", "AI与机器人", mapOf("category" to "AI")), 0.65)
        )

        // 设置模拟行为
        coEvery { embeddingService.embed(any()) } returns floatArrayOf(0.1f, 0.2f, 0.3f)
        coEvery { keywordExtractor.extractKeywords(any(), any()) } returns keywords
        coEvery { vectorStore.similaritySearch(query, embeddingService, any(), minScore) } returns vectorResults
        coEvery { vectorStore.keywordSearch(keywords, any()) } returns keywordResults

        // 执行测试
        val results = retriever.retrieve(query, limit, minScore)

        // 验证结果
        assertEquals(limit, results.size)

        // 分层策略应该先使用向量结果，然后使用关键词结果
        assertEquals("1", results[0].document.id) // 从向量结果中选择
        assertEquals("2", results[1].document.id) // 从向量结果中选择
        assertEquals("3", results[2].document.id) // 从关键词结果中选择
        assertEquals("4", results[3].document.id) // 从关键词结果中选择
        assertEquals("5", results[4].document.id) // 从关键词结果中选择
    }

    @Test
    fun `test union hybrid strategy`() = runBlocking {
        // 准备测试数据
        val query = "人工智能"
        val keywords = listOf("人工智能", "AI")
        val limit = 4
        val minScore = 0.5

        // 设置并集策略
        val config = EnhancedHybridRetrieverConfig(
            hybridStrategy = HybridStrategy.UNION
        )
        retriever = EnhancedHybridRetriever(vectorStore, embeddingService, keywordExtractor, config)

        // 模拟向量搜索结果
        val vectorResults = listOf(
            SearchResult(RagDocument("1", "人工智能简介", mapOf("category" to "AI")), 0.9),
            SearchResult(RagDocument("2", "机器学习是人工智能的一个子领域", mapOf("category" to "ML")), 0.8),
            SearchResult(RagDocument("3", "深度学习是机器学习的一个子领域", mapOf("category" to "DL")), 0.7)
        )

        // 模拟关键词搜索结果
        val keywordResults = listOf(
            SearchResult(RagDocument("1", "人工智能简介", mapOf("category" to "AI")), 0.85), // 重复的文档，但分数不同
            SearchResult(RagDocument("4", "AI的发展历史", mapOf("category" to "AI")), 0.95), // 新文档，高分
            SearchResult(RagDocument("5", "人工智能的应用", mapOf("category" to "AI")), 0.75) // 新文档
        )

        // 设置模拟行为
        coEvery { embeddingService.embed(any()) } returns floatArrayOf(0.1f, 0.2f, 0.3f)
        coEvery { keywordExtractor.extractKeywords(any(), any()) } returns keywords
        coEvery { vectorStore.similaritySearch(query, embeddingService, any(), minScore) } returns vectorResults
        coEvery { vectorStore.keywordSearch(keywords, any()) } returns keywordResults

        // 执行测试
        val results = retriever.retrieve(query, limit, minScore)

        // 验证结果
        assertEquals(limit, results.size)

        // 并集策略应该合并所有结果，保留分数较高的版本，并按分数排序
        assertEquals("4", results[0].document.id) // 分数最高的文档
        assertEquals("1", results[1].document.id) // 重复文档，保留分数较高的版本
        assertEquals("2", results[2].document.id)
        assertEquals("5", results[3].document.id)
    }

    @Test
    fun `test intersection hybrid strategy`() = runBlocking {
        // 准备测试数据
        val query = "人工智能"
        val keywords = listOf("人工智能", "AI")
        val limit = 2
        val minScore = 0.5

        // 设置交集策略
        val config = EnhancedHybridRetrieverConfig(
            hybridStrategy = HybridStrategy.INTERSECTION
        )
        retriever = EnhancedHybridRetriever(vectorStore, embeddingService, keywordExtractor, config)

        // 模拟向量搜索结果
        val vectorResults = listOf(
            SearchResult(RagDocument("1", "人工智能简介", mapOf("category" to "AI")), 0.9),
            SearchResult(RagDocument("2", "机器学习是人工智能的一个子领域", mapOf("category" to "ML")), 0.8),
            SearchResult(RagDocument("3", "深度学习是机器学习的一个子领域", mapOf("category" to "DL")), 0.7)
        )

        // 模拟关键词搜索结果
        val keywordResults = listOf(
            SearchResult(RagDocument("1", "人工智能简介", mapOf("category" to "AI")), 0.85), // 重复的文档
            SearchResult(RagDocument("2", "机器学习是人工智能的一个子领域", mapOf("category" to "ML")), 0.75), // 重复的文档
            SearchResult(RagDocument("4", "AI的发展历史", mapOf("category" to "AI")), 0.95) // 新文档
        )

        // 设置模拟行为
        coEvery { embeddingService.embed(any()) } returns floatArrayOf(0.1f, 0.2f, 0.3f)
        coEvery { keywordExtractor.extractKeywords(any(), any()) } returns keywords
        coEvery { vectorStore.similaritySearch(query, embeddingService, any(), minScore) } returns vectorResults
        coEvery { vectorStore.keywordSearch(keywords, any()) } returns keywordResults

        // 执行测试
        val results = retriever.retrieve(query, limit, minScore)

        // 验证结果
        assertEquals(2, results.size)

        // 交集策略应该只返回在所有结果集中都出现的文档
        assertEquals("1", results[0].document.id) // 在两个结果集中都出现，平均分数为 (0.9 + 0.85) / 2 = 0.875
        assertEquals("2", results[1].document.id) // 在两个结果集中都出现，平均分数为 (0.8 + 0.75) / 2 = 0.775
    }
}
