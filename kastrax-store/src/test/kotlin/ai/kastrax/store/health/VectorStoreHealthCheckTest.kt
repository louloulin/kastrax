package ai.kastrax.store.health

import ai.kastrax.store.IndexStats
import ai.kastrax.store.model.SearchResult
import ai.kastrax.store.SimilarityMetric
import ai.kastrax.store.VectorStore
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class VectorStoreHealthCheckTest {

    private lateinit var mockVectorStore: VectorStore

    @BeforeEach
    fun setUp() {
        mockVectorStore = mock(VectorStore::class.java)
    }

    @Test
    fun `test check health with healthy indexes`() = runBlocking {
        // 模拟向量存储
        `when`(mockVectorStore.listIndexes()).thenReturn(listOf("index1", "index2"))

        // 模拟索引信息
        `when`(mockVectorStore.describeIndex("index1")).thenReturn(
            IndexStats(3, 100, SimilarityMetric.COSINE)
        )
        `when`(mockVectorStore.describeIndex("index2")).thenReturn(
            IndexStats(3, 200, SimilarityMetric.COSINE)
        )

        // 模拟查询结果
        `when`(mockVectorStore.query(eq("index1"), any(FloatArray::class.java), eq(1), isNull(), eq(false)))
            .thenReturn(listOf(SearchResult("1", 0.9, null, mapOf("name" to "vector1"))))
        `when`(mockVectorStore.query(eq("index2"), any(FloatArray::class.java), eq(1), isNull(), eq(false)))
            .thenReturn(listOf(SearchResult("2", 0.8, null, mapOf("name" to "vector2"))))

        // 执行健康检查
        val result = VectorStoreHealthCheck.checkHealth(mockVectorStore)

        // 验证结果
        assertEquals(HealthStatus.HEALTHY, result.status)
        assertEquals("All indexes are healthy", result.message)
        assertEquals(2, (result.details["indexCount"] as Int))

        val indexResults = result.details["indexResults"] as List<*>
        assertEquals(2, indexResults.size)

        val index1Result = indexResults[0] as IndexHealthCheckResult
        assertEquals("index1", index1Result.indexName)
        assertEquals(HealthStatus.HEALTHY, index1Result.status)

        val index2Result = indexResults[1] as IndexHealthCheckResult
        assertEquals("index2", index2Result.indexName)
        assertEquals(HealthStatus.HEALTHY, index2Result.status)
    }

    @Test
    fun `test check health with unhealthy index`() = runBlocking {
        // 模拟向量存储
        `when`(mockVectorStore.listIndexes()).thenReturn(listOf("index1", "index2"))

        // 模拟索引信息
        `when`(mockVectorStore.describeIndex("index1")).thenReturn(
            IndexStats(3, 100, SimilarityMetric.COSINE)
        )
        `when`(mockVectorStore.describeIndex("index2")).thenThrow(RuntimeException("Index not found"))

        // 模拟查询结果
        `when`(mockVectorStore.query(eq("index1"), any(FloatArray::class.java), eq(1), isNull(), eq(false)))
            .thenReturn(listOf(SearchResult("1", 0.9, null, mapOf("name" to "vector1"))))

        // 执行健康检查
        val result = VectorStoreHealthCheck.checkHealth(mockVectorStore)

        // 验证结果
        assertEquals(HealthStatus.UNHEALTHY, result.status)
        assertTrue(result.message.contains("unhealthy"))
        assertEquals(2, (result.details["indexCount"] as Int))

        val indexResults = result.details["indexResults"] as List<*>
        assertEquals(2, indexResults.size)

        val index1Result = indexResults[0] as IndexHealthCheckResult
        assertEquals("index1", index1Result.indexName)
        assertEquals(HealthStatus.HEALTHY, index1Result.status)

        val index2Result = indexResults[1] as IndexHealthCheckResult
        assertEquals("index2", index2Result.indexName)
        assertEquals(HealthStatus.UNHEALTHY, index2Result.status)
        assertTrue(index2Result.message.contains("Error"))
    }

    @Test
    fun `test check health with empty indexes`() = runBlocking {
        // 模拟向量存储
        `when`(mockVectorStore.listIndexes()).thenReturn(emptyList())

        // 执行健康检查
        val result = VectorStoreHealthCheck.checkHealth(mockVectorStore)

        // 验证结果
        assertEquals(HealthStatus.HEALTHY, result.status)
        assertTrue(result.message.contains("no indexes"))
        assertEquals(0, (result.details["indexCount"] as Int))
    }

    @Test
    fun `test check health with exception`() = runBlocking {
        // 模拟向量存储
        `when`(mockVectorStore.listIndexes()).thenThrow(RuntimeException("Connection error"))

        // 执行健康检查
        val result = VectorStoreHealthCheck.checkHealth(mockVectorStore)

        // 验证结果
        assertEquals(HealthStatus.UNHEALTHY, result.status)
        assertTrue(result.message.contains("Error"))
        assertTrue((result.details["error"] as String).contains("Connection error"))
    }

    @Test
    fun `test check index health`() = runBlocking {
        // 模拟索引信息
        `when`(mockVectorStore.describeIndex("index1")).thenReturn(
            IndexStats(3, 100, SimilarityMetric.COSINE)
        )

        // 模拟查询结果
        `when`(mockVectorStore.query(eq("index1"), any(FloatArray::class.java), eq(1), isNull(), eq(false)))
            .thenReturn(listOf(SearchResult("1", 0.9, null, mapOf("name" to "vector1"))))

        // 执行索引健康检查
        val result = VectorStoreHealthCheck.checkIndexHealth(mockVectorStore, "index1")

        // 验证结果
        assertEquals("index1", result.indexName)
        assertEquals(HealthStatus.HEALTHY, result.status)
        assertEquals(3, result.details["dimension"])
        assertEquals(100, result.details["count"])
        assertEquals(SimilarityMetric.COSINE, result.details["metric"])
        assertEquals(1, result.details["queryResultCount"])
    }
}
