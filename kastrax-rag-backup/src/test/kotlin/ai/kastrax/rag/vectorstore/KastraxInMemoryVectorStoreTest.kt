package ai.kastrax.rag.vectorstore

import ai.kastrax.rag.embedding.EmbeddingService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class KastraxInMemoryVectorStoreTest {

    private lateinit var vectorStore: KastraxInMemoryVectorStore
    private lateinit var embeddingService: EmbeddingService

    @BeforeEach
    fun setUp() {
        vectorStore = KastraxInMemoryVectorStore()
        embeddingService = mock(EmbeddingService::class.java)
    }

    @Test
    fun `test create index`() = runBlocking {
        // 创建索引
        val result = vectorStore.createIndex("test_index", 3, SimilarityMetric.COSINE)
        assertTrue(result)

        // 再次创建同名索引应该返回 false
        val result2 = vectorStore.createIndex("test_index", 3, SimilarityMetric.COSINE)
        assertFalse(result2)

        // 创建不同维度的同名索引应该抛出异常
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                vectorStore.createIndex("test_index", 4, SimilarityMetric.COSINE)
            }
        }

        // 列出索引
        val indexes = vectorStore.listIndexes()
        assertEquals(1, indexes.size)
        assertEquals("test_index", indexes[0])

        // 获取索引信息
        val stats = vectorStore.describeIndex("test_index")
        assertEquals(3, stats.dimension)
        assertEquals(0, stats.count)
        assertEquals(SimilarityMetric.COSINE, stats.metric)
    }

    @Test
    fun `test upsert and query`() = runBlocking {
        // 创建索引
        vectorStore.createIndex("test_index", 3, SimilarityMetric.COSINE)

        // 添加向量
        val vectors = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, 1f)
        )
        val metadata = listOf(
            mapOf("name" to "vector1"),
            mapOf("name" to "vector2"),
            mapOf("name" to "vector3")
        )
        val ids = vectorStore.upsert("test_index", vectors, metadata)
        assertEquals(3, ids.size)

        // 查询向量
        val queryVector = floatArrayOf(1f, 0f, 0f)
        val results = vectorStore.query("test_index", queryVector, 2)
        assertEquals(2, results.size)
        assertEquals(ids[0], results[0].id)
        assertEquals(1.0, results[0].score, 0.001)
        assertEquals("vector1", results[0].metadata?.get("name"))

        // 使用过滤器查询
        val filteredResults = vectorStore.query(
            "test_index",
            queryVector,
            10,
            mapOf("name" to "vector2")
        )
        assertEquals(1, filteredResults.size)
        assertEquals(ids[1], filteredResults[0].id)
        assertEquals("vector2", filteredResults[0].metadata?.get("name"))

        // 包含向量的查询
        val resultsWithVectors = vectorStore.query(
            "test_index",
            queryVector,
            2,
            null,
            true
        )
        assertEquals(2, resultsWithVectors.size)
        assertNotNull(resultsWithVectors[0].vector)
        assertArrayEquals(vectors[0], resultsWithVectors[0].vector)
    }

    @Test
    fun `test delete vectors`() = runBlocking {
        // 创建索引
        vectorStore.createIndex("test_index", 3, SimilarityMetric.COSINE)

        // 添加向量
        val vectors = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, 1f)
        )
        val metadata = listOf(
            mapOf("name" to "vector1"),
            mapOf("name" to "vector2"),
            mapOf("name" to "vector3")
        )
        val ids = vectorStore.upsert("test_index", vectors, metadata)
        assertEquals(3, ids.size)

        // 删除向量
        val deleteResult = vectorStore.deleteVectors("test_index", listOf(ids[0], ids[1]))
        assertTrue(deleteResult)

        // 查询向量
        val queryVector = floatArrayOf(1f, 0f, 0f)
        val results = vectorStore.query("test_index", queryVector, 10)
        assertEquals(1, results.size)
        assertEquals(ids[2], results[0].id)

        // 获取索引信息
        val stats = vectorStore.describeIndex("test_index")
        assertEquals(1, stats.count)
    }

    @Test
    fun `test delete index`() = runBlocking {
        // 创建索引
        vectorStore.createIndex("test_index", 3, SimilarityMetric.COSINE)

        // 删除索引
        val deleteResult = vectorStore.deleteIndex("test_index")
        assertTrue(deleteResult)

        // 列出索引
        val indexes = vectorStore.listIndexes()
        assertEquals(0, indexes.size)

        // 删除不存在的索引
        val deleteResult2 = vectorStore.deleteIndex("non_existent_index")
        assertFalse(deleteResult2)
    }

    @Test
    fun `test add document and similarity search`() = runBlocking {
        // 模拟嵌入服务
        `when`(embeddingService.embed("document1")).thenReturn(floatArrayOf(1f, 0f, 0f))
        `when`(embeddingService.embed("document2")).thenReturn(floatArrayOf(0f, 1f, 0f))
        `when`(embeddingService.embed("query")).thenReturn(floatArrayOf(0.9f, 0.1f, 0f))

        // 添加文档
        val id1 = vectorStore.addDocument("document1", embeddingService, mapOf("tag" to "doc1"))
        val id2 = vectorStore.addDocument("document2", embeddingService, mapOf("tag" to "doc2"))

        // 相似度搜索
        val results = vectorStore.similaritySearch("query", embeddingService, 2)
        assertEquals(2, results.size)
        assertEquals("document1", results[0].document.content)
        assertTrue(results[0].score > results[1].score)

        // 获取文档
        val doc = vectorStore.getDocument(id1)
        assertNotNull(doc)
        assertEquals("document1", doc?.content)
        assertEquals("doc1", doc?.metadata?.get("tag"))

        // 获取嵌入向量
        val embedding = vectorStore.getEmbedding(id1)
        assertNotNull(embedding)
        assertArrayEquals(floatArrayOf(1f, 0f, 0f), embedding)

        // 删除文档
        val deleteResult = vectorStore.deleteDocument(id1)
        assertTrue(deleteResult)

        // 再次获取文档应该返回 null
        val deletedDoc = vectorStore.getDocument(id1)
        assertNull(deletedDoc)
    }

    @Test
    fun `test keyword search`() = runBlocking {
        // 添加文档
        vectorStore.addDocument("apple banana orange", floatArrayOf(1f, 0f, 0f), mapOf("tag" to "fruits"))
        vectorStore.addDocument("cat dog bird", floatArrayOf(0f, 1f, 0f), mapOf("tag" to "animals"))
        vectorStore.addDocument("apple cat", floatArrayOf(0f, 0f, 1f), mapOf("tag" to "mixed"))

        // 关键词搜索
        val results = vectorStore.keywordSearch(listOf("apple", "banana"), 10)
        assertEquals(2, results.size)
        assertEquals("apple banana orange", results[0].document.content)
        assertEquals("apple cat", results[1].document.content)
        assertTrue(results[0].score > results[1].score)
    }

    @Test
    fun `test metadata search`() = runBlocking {
        // 添加文档
        vectorStore.addDocument("document1", floatArrayOf(1f, 0f, 0f), mapOf("tag" to "doc1", "category" to "A"))
        vectorStore.addDocument("document2", floatArrayOf(0f, 1f, 0f), mapOf("tag" to "doc2", "category" to "B"))
        vectorStore.addDocument("document3", floatArrayOf(0f, 0f, 1f), mapOf("tag" to "doc3", "category" to "A"))

        // 元数据搜索
        val results = vectorStore.metadataSearch(mapOf("category" to "A"), 10)
        assertEquals(2, results.size)
        assertTrue(results.all { it.document.metadata["category"] == "A" })
    }

    @Test
    fun `test clear`() = runBlocking {
        // 创建索引
        vectorStore.createIndex("test_index", 3, SimilarityMetric.COSINE)

        // 添加向量
        val vectors = listOf(floatArrayOf(1f, 0f, 0f))
        val metadata = listOf(mapOf("name" to "vector1"))
        vectorStore.upsert("test_index", vectors, metadata)

        // 添加文档
        vectorStore.addDocument("document1", floatArrayOf(1f, 0f, 0f), mapOf("tag" to "doc1"))

        // 清空向量存储
        vectorStore.clear()

        // 列出索引
        val indexes = vectorStore.listIndexes()
        assertEquals(0, indexes.size)

        // 获取文档数量
        val size = vectorStore.size()
        assertEquals(0, size)
    }
}
