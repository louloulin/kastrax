package ai.kastrax.store.lancedb

import ai.kastrax.store.SimilarityMetric
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * LanceDBVectorStore 的单元测试。
 */
class LanceDBVectorStoreTest {

    private lateinit var vectorStore: LanceDBVectorStore
    private lateinit var tempDir: String

    @BeforeEach
    fun setUp(@TempDir tempDir: Path) {
        this.tempDir = tempDir.toString()
        vectorStore = LanceDBVectorStore(this.tempDir)
    }

    @AfterEach
    fun tearDown() {
        vectorStore.close()
        File(tempDir).deleteRecursively()
    }

    @Test
    fun `test create and delete index`() = runBlocking {
        // 创建索引
        val indexName = "test_index"
        val dimension = 3
        val created = vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)
        assertTrue(created)

        // 列出索引
        val indexes = vectorStore.listIndexes()
        assertTrue(indexes.contains(indexName))

        // 删除索引
        val deleted = vectorStore.deleteIndex(indexName)
        assertTrue(deleted)

        // 再次列出索引
        val indexesAfterDelete = vectorStore.listIndexes()
        assertFalse(indexesAfterDelete.contains(indexName))
    }

    @Test
    fun `test upsert and query vectors`() = runBlocking {
        // 创建索引
        val indexName = "test_vectors"
        val dimension = 3
        vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)

        // 添加向量
        val vectors = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, 1f),
            floatArrayOf(0.5f, 0.5f, 0f)
        )
        val metadata = listOf(
            mapOf("name" to "vector1", "category" to "A"),
            mapOf("name" to "vector2", "category" to "B"),
            mapOf("name" to "vector3", "category" to "A"),
            mapOf("name" to "vector4", "category" to "B")
        )
        val ids = vectorStore.upsert(indexName, vectors, metadata)
        assertEquals(4, ids.size)

        // 查询向量
        val queryVector = floatArrayOf(1f, 0f, 0f)
        val results = vectorStore.query(indexName, queryVector, 2)
        assertEquals(2, results.size)
        assertEquals(ids[0], results[0].id)
        assertTrue(results[0].score > 0.9) // 相似度应该很高

        // 使用过滤器查询
        val filteredResults = vectorStore.query(
            indexName = indexName,
            queryVector = queryVector,
            topK = 10,
            filter = mapOf("category" to "A")
        )
        assertTrue(filteredResults.isNotEmpty())
        assertEquals("A", filteredResults[0].metadata["category"])
    }

    @Test
    fun `test update and delete vectors`() = runBlocking {
        // 创建索引
        val indexName = "test_update_delete"
        val dimension = 3
        vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)

        // 添加向量
        val vectors = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f)
        )
        val metadata = listOf(
            mapOf("name" to "vector1"),
            mapOf("name" to "vector2")
        )
        val ids = vectorStore.upsert(indexName, vectors, metadata)
        assertEquals(2, ids.size)

        // 更新向量
        val updated = vectorStore.updateVector(
            indexName = indexName,
            id = ids[0],
            vector = floatArrayOf(0.5f, 0.5f, 0f),
            metadata = mapOf("name" to "updated_vector", "category" to "C")
        )
        assertTrue(updated)

        // 查询更新后的向量
        val queryVector = floatArrayOf(0.5f, 0.5f, 0f)
        val results = vectorStore.query(indexName, queryVector, 1)
        assertEquals(1, results.size)
        assertEquals(ids[0], results[0].id)
        assertEquals("updated_vector", results[0].metadata["name"])
        assertEquals("C", results[0].metadata["category"])

        // 删除向量
        val deleted = vectorStore.deleteVectors(indexName, listOf(ids[0]))
        assertTrue(deleted)

        // 查询删除后的向量
        val resultsAfterDelete = vectorStore.query(indexName, queryVector, 10)
        assertTrue(resultsAfterDelete.none { it.id == ids[0] })
    }

    @Test
    fun `test batch upsert`() = runBlocking {
        // 创建索引
        val indexName = "test_batch"
        val dimension = 3
        vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)

        // 创建大量向量
        val vectorCount = 100
        val vectors = List(vectorCount) { i ->
            when (i % 3) {
                0 -> floatArrayOf(1f, 0f, 0f)
                1 -> floatArrayOf(0f, 1f, 0f)
                else -> floatArrayOf(0f, 0f, 1f)
            }
        }
        val metadata = List(vectorCount) { i ->
            mapOf("index" to i, "group" to (i % 5))
        }

        // 批量添加向量
        val batchSize = 20
        val ids = vectorStore.batchUpsert(indexName, vectors, metadata, batchSize = batchSize)
        assertEquals(vectorCount, ids.size)

        // 查询向量
        val queryVector = floatArrayOf(1f, 0f, 0f)
        val results = vectorStore.query(
            indexName = indexName,
            queryVector = queryVector,
            topK = 10,
            filter = mapOf("group" to 2)
        )
        assertTrue(results.isNotEmpty())
        results.forEach {
            assertEquals(2, it.metadata["group"])
        }
    }

    @Test
    fun `test create ANN index`() = runBlocking {
        // 创建索引
        val indexName = "test_ann"
        val dimension = 3
        vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)

        // 添加向量
        val vectorCount = 50
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
        vectorStore.upsert(indexName, vectors, metadata)

        // 创建 ANN 索引
        val annCreated = vectorStore.createAnnIndex(
            indexName = indexName,
            indexType = "ivf_pq",
            params = mapOf("num_partitions" to 2, "num_sub_vectors" to 1)
        )
        assertTrue(annCreated)

        // 查询向量
        val queryVector = floatArrayOf(1f, 0f, 0f)
        val results = vectorStore.query(indexName, queryVector, 10)
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `test describe index`() = runBlocking {
        // 创建索引
        val indexName = "test_describe"
        val dimension = 3
        vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)

        // 添加向量
        val vectors = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f)
        )
        val metadata = listOf(
            mapOf("name" to "vector1"),
            mapOf("name" to "vector2")
        )
        vectorStore.upsert(indexName, vectors, metadata)

        // 获取索引信息
        val stats = vectorStore.describeIndex(indexName)
        assertEquals(indexName, stats.name)
        assertEquals(2, stats.count)
    }
}
