package ai.kastrax.store.memory

import ai.kastrax.store.embedding.EmbeddingService
import ai.kastrax.store.model.SearchResult
import ai.kastrax.store.SimilarityMetric
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class InMemoryVectorStoreTest {

    private lateinit var vectorStore: InMemoryVectorStore
    private lateinit var embeddingService: EmbeddingService

    @BeforeEach
    fun setUp() {
        vectorStore = InMemoryVectorStore(dimension = 3)
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
    fun `test similarity search`() = runBlocking {
        // 创建索引
        vectorStore.createIndex("test_index", 3, SimilarityMetric.COSINE)

        // 添加向量
        val vectors = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, 1f)
        )
        val metadata = listOf(
            mapOf("name" to "vector1", "category" to "A"),
            mapOf("name" to "vector2", "category" to "B"),
            mapOf("name" to "vector3", "category" to "A")
        )
        vectorStore.upsert("test_index", vectors, metadata)

        // 模拟嵌入服务
        `when`(embeddingService.embed("query")).thenReturn(floatArrayOf(0.9f, 0.1f, 0f))

        // 相似度搜索
        val results = vectorStore.similaritySearch("test_index", "query", embeddingService, 2)
        assertEquals(2, results.size)
        assertEquals("vector1", results[0].metadata?.get("name"))
        assertTrue(results[0].score > results[1].score)

        // 带过滤器的相似度搜索
        val filteredResults = vectorStore.similaritySearch(
            "test_index",
            "query",
            embeddingService,
            10,
            mapOf("category" to "A")
        )
        assertEquals(2, filteredResults.size)
        assertTrue(filteredResults.all { it.metadata?.get("category") == "A" })

        // 带最小分数的相似度搜索
        val minScoreResults = vectorStore.similaritySearch(
            "test_index",
            "query",
            embeddingService,
            10,
            null,
            0.9
        )
        assertTrue(minScoreResults.all { it.score >= 0.9 })
    }

    @Test
    fun `test batch upsert`() = runBlocking {
        // 创建索引
        vectorStore.createIndex("test_index", 3, SimilarityMetric.COSINE)

        // 准备大量向量
        val vectorCount = 150
        val vectors = List(vectorCount) { i ->
            when (i % 3) {
                0 -> floatArrayOf(1f, 0f, 0f)
                1 -> floatArrayOf(0f, 1f, 0f)
                else -> floatArrayOf(0f, 0f, 1f)
            }
        }
        val metadata = List(vectorCount) { i ->
            mapOf("index" to i)
        }

        // 批量添加向量
        val ids = vectorStore.batchUpsert("test_index", vectors, metadata, batchSize = 50)
        assertEquals(vectorCount, ids.size)

        // 获取索引信息
        val stats = vectorStore.describeIndex("test_index")
        assertEquals(vectorCount, stats.count)

        // 查询向量
        val queryVector = floatArrayOf(1f, 0f, 0f)
        val results = vectorStore.query("test_index", queryVector, 10)
        assertEquals(10, results.size)
        assertTrue(results.all { it.score > 0 })
    }

    @Test
    fun `test update vector`() = runBlocking {
        // 创建索引
        vectorStore.createIndex("test_index", 3, SimilarityMetric.COSINE)

        // 添加向量
        val vectors = listOf(floatArrayOf(1f, 0f, 0f))
        val metadata = listOf(mapOf("name" to "vector1"))
        val ids = vectorStore.upsert("test_index", vectors, metadata)
        val id = ids[0]

        // 更新向量
        val newVector = floatArrayOf(0f, 1f, 0f)
        val newMetadata = mapOf("name" to "updated_vector", "category" to "new")
        val updateResult = vectorStore.updateVector("test_index", id, newVector, newMetadata)
        assertTrue(updateResult)

        // 查询更新后的向量
        val results = vectorStore.query("test_index", newVector, 1, includeVectors = true)
        assertEquals(1, results.size)
        assertEquals(id, results[0].id)
        assertEquals(1.0, results[0].score, 0.001)
        assertEquals("updated_vector", results[0].metadata?.get("name"))
        assertEquals("new", results[0].metadata?.get("category"))
        assertArrayEquals(newVector, results[0].vector)

        // 只更新元数据
        val metadataUpdateResult = vectorStore.updateVector("test_index", id, metadata = mapOf("version" to 2))
        assertTrue(metadataUpdateResult)

        // 查询更新后的向量
        val metadataResults = vectorStore.query("test_index", newVector, 1)
        assertEquals(1, metadataResults.size)
        assertEquals(id, metadataResults[0].id)
        assertEquals("updated_vector", metadataResults[0].metadata?.get("name"))
        assertEquals("new", metadataResults[0].metadata?.get("category"))
        assertEquals(2, metadataResults[0].metadata?.get("version"))

        // 更新不存在的向量
        val nonExistentUpdateResult = vectorStore.updateVector("test_index", "non_existent_id", newVector, newMetadata)
        assertFalse(nonExistentUpdateResult)
    }

    @Test
    fun `test different similarity metrics`() = runBlocking {
        // 创建使用不同相似度度量的索引
        vectorStore.createIndex("cosine_index", 3, SimilarityMetric.COSINE)
        vectorStore.createIndex("euclidean_index", 3, SimilarityMetric.EUCLIDEAN)
        vectorStore.createIndex("dot_product_index", 3, SimilarityMetric.DOT_PRODUCT)

        // 添加相同的向量到所有索引
        val vectors = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0.7f, 0.7f, 0f)
        )
        val metadata = listOf(
            mapOf("name" to "vector1"),
            mapOf("name" to "vector2"),
            mapOf("name" to "vector3")
        )

        vectorStore.upsert("cosine_index", vectors, metadata)
        vectorStore.upsert("euclidean_index", vectors, metadata)
        vectorStore.upsert("dot_product_index", vectors, metadata)

        // 查询向量
        val queryVector = floatArrayOf(0.9f, 0.1f, 0f)

        // 余弦相似度查询
        val cosineResults = vectorStore.query("cosine_index", queryVector, 3)
        assertEquals(3, cosineResults.size)
        assertEquals("vector1", cosineResults[0].metadata?.get("name"))

        // 欧几里得距离查询
        val euclideanResults = vectorStore.query("euclidean_index", queryVector, 3)
        assertEquals(3, euclideanResults.size)
        assertEquals("vector1", euclideanResults[0].metadata?.get("name"))

        // 点积查询
        val dotProductResults = vectorStore.query("dot_product_index", queryVector, 3)
        assertEquals(3, dotProductResults.size)
        assertEquals("vector1", dotProductResults[0].metadata?.get("name"))

        // 验证不同度量方式的分数不同
        assertNotEquals(cosineResults[1].score, euclideanResults[1].score)
        assertNotEquals(cosineResults[1].score, dotProductResults[1].score)
        assertNotEquals(euclideanResults[1].score, dotProductResults[1].score)
    }
}
