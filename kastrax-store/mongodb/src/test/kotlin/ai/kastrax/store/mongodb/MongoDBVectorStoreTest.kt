package ai.kastrax.store.mongodb

import ai.kastrax.store.SimilarityMetric
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.*

@Testcontainers
class MongoDBVectorStoreTest {

    companion object {
        @Container
        val mongoDBContainer = MongoDBContainer(DockerImageName.parse("mongo:6.0"))
            .withCommand("--setParameter", "enableSearchIndexCreation=true")
    }

    private lateinit var vectorStore: MongoDBVectorStore
    private val databaseName = "test_db"

    @BeforeEach
    fun setUp() {
        // 启动容器
        mongoDBContainer.start()
        
        // 创建向量存储
        val connectionString = mongoDBContainer.connectionString
        vectorStore = MongoDBVectorStore(connectionString, databaseName)
    }

    @AfterEach
    fun tearDown() {
        // 关闭向量存储
        vectorStore.close()
        
        // 停止容器
        mongoDBContainer.stop()
    }

    @Test
    fun `test create and list indexes`() = runBlocking {
        // 创建索引
        val indexName = "test_index"
        val dimension = 3
        val result = vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)
        assertTrue(result)

        // 列出索引
        val indexes = vectorStore.listIndexes()
        assertTrue(indexes.contains(indexName))
    }

    @Test
    fun `test describe index`() = runBlocking {
        // 创建索引
        val indexName = "test_index"
        val dimension = 3
        vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)

        // 获取索引信息
        val stats = vectorStore.describeIndex(indexName)
        assertEquals(dimension, stats.dimension)
        assertEquals(0, stats.count)
        assertEquals(SimilarityMetric.COSINE, stats.metric)
    }

    @Test
    fun `test upsert and query`() = runBlocking {
        // 创建索引
        val indexName = "test_index"
        val dimension = 3
        vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)

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
        val ids = vectorStore.upsert(indexName, vectors, metadata)
        assertEquals(3, ids.size)

        // 查询向量
        val queryVector = floatArrayOf(1f, 0f, 0f)
        val results = vectorStore.query(indexName, queryVector, 2)
        assertEquals(2, results.size)
        assertEquals("vector1", results[0].metadata?.get("name"))
        assertTrue(results[0].score > 0.9) // 余弦相似度应该接近 1
    }

    @Test
    fun `test query with filter`() = runBlocking {
        // 创建索引
        val indexName = "test_index"
        val dimension = 3
        vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)

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
        vectorStore.upsert(indexName, vectors, metadata)

        // 使用过滤器查询
        val queryVector = floatArrayOf(1f, 0f, 0f)
        val results = vectorStore.query(
            indexName = indexName,
            queryVector = queryVector,
            topK = 10,
            filter = mapOf("category" to "A")
        )
        assertEquals(2, results.size)
        assertTrue(results.all { it.metadata?.get("category") == "A" })
    }

    @Test
    fun `test delete vectors`() = runBlocking {
        // 创建索引
        val indexName = "test_index"
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

        // 删除向量
        val deleteResult = vectorStore.deleteVectors(indexName, listOf(ids[0]))
        assertTrue(deleteResult)

        // 查询向量
        val queryVector = floatArrayOf(1f, 0f, 0f)
        val results = vectorStore.query(indexName, queryVector, 2)
        assertEquals(1, results.size)
        assertEquals("vector2", results[0].metadata?.get("name"))
    }

    @Test
    fun `test update vector`() = runBlocking {
        // 创建索引
        val indexName = "test_index"
        val dimension = 3
        vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)

        // 添加向量
        val vectors = listOf(
            floatArrayOf(1f, 0f, 0f)
        )
        val metadata = listOf(
            mapOf("name" to "vector1", "category" to "A")
        )
        val ids = vectorStore.upsert(indexName, vectors, metadata)
        val id = ids[0]

        // 更新向量
        val updateResult = vectorStore.updateVector(
            indexName = indexName,
            id = id,
            vector = floatArrayOf(0f, 1f, 0f),
            metadata = mapOf("name" to "updated_vector", "category" to "B")
        )
        assertTrue(updateResult)

        // 查询向量
        val queryVector = floatArrayOf(0f, 1f, 0f)
        val results = vectorStore.query(indexName, queryVector, 1)
        assertEquals(1, results.size)
        assertEquals("updated_vector", results[0].metadata?.get("name"))
        assertEquals("B", results[0].metadata?.get("category"))
    }

    @Test
    fun `test batch upsert`() = runBlocking {
        // 创建索引
        val indexName = "test_index"
        val dimension = 3
        vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)

        // 准备大量向量
        val vectorCount = 100
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
        val ids = vectorStore.batchUpsert(indexName, vectors, metadata, batchSize = 10)
        assertEquals(vectorCount, ids.size)

        // 查询向量
        val queryVector = floatArrayOf(1f, 0f, 0f)
        val results = vectorStore.query(indexName, queryVector, 10)
        assertEquals(10, results.size)
    }

    @Test
    fun `test delete index`() = runBlocking {
        // 创建索引
        val indexName = "test_index"
        val dimension = 3
        vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)

        // 删除索引
        val deleteResult = vectorStore.deleteIndex(indexName)
        assertTrue(deleteResult)

        // 列出索引
        val indexes = vectorStore.listIndexes()
        assertFalse(indexes.contains(indexName))
    }

    /**
     * 以下测试需要真实的 MongoDB Atlas 连接，仅在提供环境变量时运行。
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "MONGODB_ATLAS_URI", matches = ".+")
    fun `test with real MongoDB Atlas`() = runBlocking {
        val uri = System.getenv("MONGODB_ATLAS_URI") ?: return@runBlocking
        val dbName = System.getenv("MONGODB_ATLAS_DB") ?: "test"

        // 创建真实的向量存储
        val realVectorStore = MongoDBVectorStore(uri, dbName)
        
        try {
            // 创建索引
            val indexName = "test-index-${UUID.randomUUID().toString().substring(0, 8)}"
            realVectorStore.createIndex(indexName, 3, SimilarityMetric.COSINE)
            
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
            val ids = realVectorStore.upsert(indexName, vectors, metadata)
            assertEquals(3, ids.size)
            
            // 查询向量
            val queryVector = floatArrayOf(1f, 0f, 0f)
            val results = realVectorStore.query(indexName, queryVector, 2)
            assertEquals(2, results.size)
            
            // 删除索引
            realVectorStore.deleteIndex(indexName)
        } finally {
            realVectorStore.close()
        }
    }
}
