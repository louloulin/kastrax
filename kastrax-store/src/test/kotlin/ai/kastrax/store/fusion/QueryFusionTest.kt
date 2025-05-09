package ai.kastrax.store.fusion

import ai.kastrax.store.embedding.EmbeddingService
import ai.kastrax.store.model.SearchResult
import ai.kastrax.store.SimilarityMetric
import ai.kastrax.store.VectorStore
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class QueryFusionTest {

    private lateinit var mockVectorStore: VectorStore
    private lateinit var embeddingService: EmbeddingService

    @BeforeEach
    fun setUp() {
        mockVectorStore = mock(VectorStore::class.java)
        embeddingService = mock(EmbeddingService::class.java)
    }

    @Test
    fun `test weighted fusion strategy`() = runBlocking {
        // 准备测试数据
        val indexName = "test_index"
        val queries = listOf("query1", "query2")
        val queryEmbeddings = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f)
        )
        val topK = 3

        // 模拟嵌入服务
        `when`(embeddingService.embed("query1")).thenReturn(queryEmbeddings[0])
        `when`(embeddingService.embed("query2")).thenReturn(queryEmbeddings[1])

        // 模拟向量存储查询结果
        val query1Results = listOf(
            SearchResult("1", 0.9, null, mapOf("name" to "doc1")),
            SearchResult("2", 0.8, null, mapOf("name" to "doc2")),
            SearchResult("3", 0.7, null, mapOf("name" to "doc3"))
        )
        val query2Results = listOf(
            SearchResult("2", 0.9, null, mapOf("name" to "doc2")),
            SearchResult("3", 0.8, null, mapOf("name" to "doc3")),
            SearchResult("4", 0.7, null, mapOf("name" to "doc4"))
        )

        `when`(mockVectorStore.query(indexName, queryEmbeddings[0], topK * 2)).thenReturn(query1Results)
        `when`(mockVectorStore.query(indexName, queryEmbeddings[1], topK * 2)).thenReturn(query2Results)

        // 执行加权融合
        val options = FusionOptions(
            strategy = FusionStrategy.WEIGHTED,
            weights = listOf(0.6, 0.4)
        )
        val results = QueryFusion.fuseQueries(
            vectorStore = mockVectorStore,
            indexName = indexName,
            queries = queries,
            embeddingService = embeddingService,
            topK = topK,
            options = options
        )

        // 验证结果
        assertEquals(topK, results.size)

        // 文档2应该排在第一位，因为它在两个查询中都有高分
        assertEquals("2", results[0].id)

        // 验证分数计算
        // 文档2的分数应该是 0.8 * 0.6 + 0.9 * 0.4 = 0.48 + 0.36 = 0.84
        assertTrue(Math.abs(results[0].score - 0.84) < 0.01)
    }

    @Test
    fun `test max score fusion strategy`() = runBlocking {
        // 准备测试数据
        val indexName = "test_index"
        val queryEmbeddings = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f)
        )
        val topK = 3

        // 模拟向量存储查询结果
        val query1Results = listOf(
            SearchResult("1", 0.9, null, mapOf("name" to "doc1")),
            SearchResult("2", 0.7, null, mapOf("name" to "doc2")),
            SearchResult("3", 0.5, null, mapOf("name" to "doc3"))
        )
        val query2Results = listOf(
            SearchResult("2", 0.8, null, mapOf("name" to "doc2")),
            SearchResult("3", 0.6, null, mapOf("name" to "doc3")),
            SearchResult("4", 0.7, null, mapOf("name" to "doc4"))
        )

        `when`(mockVectorStore.query(indexName, queryEmbeddings[0], topK * 2)).thenReturn(query1Results)
        `when`(mockVectorStore.query(indexName, queryEmbeddings[1], topK * 2)).thenReturn(query2Results)

        // 执行最大分数融合
        val options = FusionOptions(strategy = FusionStrategy.MAX_SCORE)
        val results = QueryFusion.fuseQueryEmbeddings(
            vectorStore = mockVectorStore,
            indexName = indexName,
            queryEmbeddings = queryEmbeddings,
            topK = topK,
            options = options
        )

        // 验证结果
        assertEquals(topK, results.size)

        // 文档1应该排在第一位，因为它有最高分
        assertEquals("1", results[0].id)
        assertEquals(0.9, results[0].score)

        // 文档2应该排在第二位，取其最高分
        assertEquals("2", results[1].id)
        assertEquals(0.8, results[1].score)
    }

    @Test
    fun `test average fusion strategy`() = runBlocking {
        // 准备测试数据
        val indexName = "test_index"
        val queryEmbeddings = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f)
        )
        val topK = 3

        // 模拟向量存储查询结果
        val query1Results = listOf(
            SearchResult("1", 0.9, null, mapOf("name" to "doc1")),
            SearchResult("2", 0.7, null, mapOf("name" to "doc2")),
            SearchResult("3", 0.5, null, mapOf("name" to "doc3"))
        )
        val query2Results = listOf(
            SearchResult("2", 0.8, null, mapOf("name" to "doc2")),
            SearchResult("3", 0.6, null, mapOf("name" to "doc3")),
            SearchResult("4", 0.7, null, mapOf("name" to "doc4"))
        )

        `when`(mockVectorStore.query(indexName, queryEmbeddings[0], topK * 2)).thenReturn(query1Results)
        `when`(mockVectorStore.query(indexName, queryEmbeddings[1], topK * 2)).thenReturn(query2Results)

        // 执行平均分数融合
        val options = FusionOptions(strategy = FusionStrategy.AVERAGE)
        val results = QueryFusion.fuseQueryEmbeddings(
            vectorStore = mockVectorStore,
            indexName = indexName,
            queryEmbeddings = queryEmbeddings,
            topK = topK,
            options = options
        )

        // 验证结果
        assertEquals(topK, results.size)

        // 文档2应该排在第一位，因为它在两个查询中都有高分
        assertEquals("2", results[0].id)

        // 验证分数计算
        // 文档2的分数应该是 (0.7 + 0.8) / 2 = 0.75
        assertTrue(Math.abs(results[0].score - 0.75) < 0.01)
    }

    @Test
    fun `test recursive fusion strategy`() = runBlocking {
        // 准备测试数据
        val indexName = "test_index"
        val queryEmbeddings = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f)
        )
        val topK = 2

        // 模拟向量存储查询结果
        val query1Results = listOf(
            SearchResult("1", 0.9, floatArrayOf(0.9f, 0.1f, 0f), mapOf("name" to "doc1")),
            SearchResult("2", 0.7, floatArrayOf(0.7f, 0.3f, 0f), mapOf("name" to "doc2"))
        )
        val query2Results = listOf(
            SearchResult("2", 0.8, floatArrayOf(0.7f, 0.3f, 0f), mapOf("name" to "doc2")),
            SearchResult("3", 0.6, floatArrayOf(0.3f, 0.7f, 0f), mapOf("name" to "doc3"))
        )
        val recursiveResults = listOf(
            SearchResult("1", 0.95, floatArrayOf(0.9f, 0.1f, 0f), mapOf("name" to "doc1")),
            SearchResult("2", 0.85, floatArrayOf(0.7f, 0.3f, 0f), mapOf("name" to "doc2"))
        )

        `when`(mockVectorStore.query(indexName, queryEmbeddings[0], topK)).thenReturn(query1Results)
        `when`(mockVectorStore.query(indexName, queryEmbeddings[1], topK)).thenReturn(query2Results)
        `when`(mockVectorStore.query(eq(indexName), any(FloatArray::class.java), eq(topK))).thenReturn(recursiveResults)

        // 执行递归融合
        val options = FusionOptions(
            strategy = FusionStrategy.RECURSIVE,
            recursiveDepth = 2
        )
        val results = QueryFusion.fuseQueryEmbeddings(
            vectorStore = mockVectorStore,
            indexName = indexName,
            queryEmbeddings = queryEmbeddings,
            topK = topK,
            options = options
        )

        // 验证结果
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `test fusion with empty queries`() = runBlocking {
        // 准备测试数据
        val indexName = "test_index"
        val queries = emptyList<String>()
        val topK = 3

        // 执行融合
        val results = QueryFusion.fuseQueries(
            vectorStore = mockVectorStore,
            indexName = indexName,
            queries = queries,
            embeddingService = embeddingService,
            topK = topK
        )

        // 验证结果
        assertTrue(results.isEmpty())
    }

    @Test
    fun `test fusion with min score filter`() = runBlocking {
        // 准备测试数据
        val indexName = "test_index"
        val queryEmbeddings = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f)
        )
        val topK = 3
        val minScore = 0.7

        // 模拟向量存储查询结果
        val query1Results = listOf(
            SearchResult("1", 0.9, null, mapOf("name" to "doc1")),
            SearchResult("2", 0.7, null, mapOf("name" to "doc2")),
            SearchResult("3", 0.5, null, mapOf("name" to "doc3"))
        )
        val query2Results = listOf(
            SearchResult("2", 0.8, null, mapOf("name" to "doc2")),
            SearchResult("3", 0.6, null, mapOf("name" to "doc3")),
            SearchResult("4", 0.7, null, mapOf("name" to "doc4"))
        )

        `when`(mockVectorStore.query(indexName, queryEmbeddings[0], topK * 2)).thenReturn(query1Results)
        `when`(mockVectorStore.query(indexName, queryEmbeddings[1], topK * 2)).thenReturn(query2Results)

        // 执行加权融合
        val options = FusionOptions(
            strategy = FusionStrategy.WEIGHTED,
            minScore = minScore
        )
        val results = QueryFusion.fuseQueryEmbeddings(
            vectorStore = mockVectorStore,
            indexName = indexName,
            queryEmbeddings = queryEmbeddings,
            topK = topK,
            options = options
        )

        // 验证结果
        assertTrue(results.all { it.score >= minScore })
    }
}
