package ai.kastrax.codebase.store

import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.embedding.MockEmbeddingService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ShardedVectorStoreTest {
    
    private lateinit var mockEmbeddingService: MockEmbeddingService
    private lateinit var shardedStore: ShardedVectorStore
    
    @BeforeEach
    fun setUp() {
        // 创建嵌入服务
        mockEmbeddingService = MockEmbeddingService()
        
        // 创建分片向量存储
        shardedStore = ShardedVectorStore(
            shardStoreFactory = { shardId ->
                VectorStoreFactory.createInMemoryVectorStore()
            },
            config = ShardedVectorStoreConfig(
                shardCount = 2,
                replicaCount = 1,
                maxVectorsPerShard = 100,
                consistencyLevel = ConsistencyLevel.ALL
            )
        )
    }
    
    @Test
    fun `test adding and retrieving vectors`() = runBlocking {
        // 创建测试向量
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f)
        val metadata = mapOf("key" to "value")
        
        // 添加向量
        val id = shardedStore.addVector(vector, metadata)
        
        // 验证ID不为空
        assertNotNull(id)
        
        // 获取向量
        val retrieved = shardedStore.getVector(id)
        
        // 验证向量
        assertNotNull(retrieved)
        assertEquals(id, retrieved.id)
        assertTrue(vector.contentEquals(retrieved.vector))
        assertEquals(metadata, retrieved.metadata)
    }
    
    @Test
    fun `test batch adding and retrieving vectors`() = runBlocking {
        // 创建测试向量
        val vectors = listOf(
            floatArrayOf(0.1f, 0.2f, 0.3f),
            floatArrayOf(0.4f, 0.5f, 0.6f),
            floatArrayOf(0.7f, 0.8f, 0.9f)
        )
        val metadata = listOf(
            mapOf("key" to "value1"),
            mapOf("key" to "value2"),
            mapOf("key" to "value3")
        )
        
        // 批量添加向量
        val ids = shardedStore.addVectors(vectors, metadata)
        
        // 验证ID数量
        assertEquals(vectors.size, ids.size)
        
        // 批量获取向量
        val retrieved = shardedStore.getVectors(ids)
        
        // 验证向量数量
        assertEquals(vectors.size, retrieved.size)
        
        // 验证每个向量
        for (i in vectors.indices) {
            val original = vectors[i]
            val retrievedVector = retrieved.find { it.id == ids[i] }
            
            assertNotNull(retrievedVector)
            assertTrue(original.contentEquals(retrievedVector.vector))
            assertEquals(metadata[i], retrievedVector.metadata)
        }
    }
    
    @Test
    fun `test adding and searching embeddings`() = runBlocking {
        // 创建测试文本
        val texts = listOf(
            "public class TestClass { void testMethod() { } }",
            "function testFunction() { return true; }",
            "def test_function(): pass"
        )
        val metadata = listOf(
            mapOf("language" to "java"),
            mapOf("language" to "javascript"),
            mapOf("language" to "python")
        )
        
        // 添加嵌入
        val ids = shardedStore.addEmbeddings(texts, metadata, mockEmbeddingService)
        
        // 验证ID数量
        assertEquals(texts.size, ids.size)
        
        // 搜索嵌入
        val query = "class TestClass"
        val results = shardedStore.searchEmbedding(query, limit = 2, minScore = 0.0, embeddingService = mockEmbeddingService)
        
        // 验证结果数量
        assertTrue(results.isNotEmpty())
        assertTrue(results.size <= 2)
        
        // 验证结果分数
        results.forEach { result ->
            assertTrue(result.score >= 0.0)
            assertTrue(result.score <= 1.0)
        }
        
        // 验证结果排序
        for (i in 0 until results.size - 1) {
            assertTrue(results[i].score >= results[i + 1].score)
        }
    }
    
    @Test
    fun `test deleting vectors`() = runBlocking {
        // 创建测试向量
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f)
        val metadata = mapOf("key" to "value")
        
        // 添加向量
        val id = shardedStore.addVector(vector, metadata)
        
        // 验证向量存在
        val retrieved = shardedStore.getVector(id)
        assertNotNull(retrieved)
        
        // 删除向量
        val success = shardedStore.deleteVector(id)
        assertTrue(success)
        
        // 验证向量不存在
        val retrievedAfterDelete = shardedStore.getVector(id)
        assertEquals(null, retrievedAfterDelete)
    }
    
    @Test
    fun `test batch deleting vectors`() = runBlocking {
        // 创建测试向量
        val vectors = listOf(
            floatArrayOf(0.1f, 0.2f, 0.3f),
            floatArrayOf(0.4f, 0.5f, 0.6f),
            floatArrayOf(0.7f, 0.8f, 0.9f)
        )
        
        // 批量添加向量
        val ids = shardedStore.addVectors(vectors)
        
        // 验证向量存在
        val retrieved = shardedStore.getVectors(ids)
        assertEquals(vectors.size, retrieved.size)
        
        // 批量删除向量
        val success = shardedStore.deleteVectors(ids)
        assertTrue(success)
        
        // 验证向量不存在
        val retrievedAfterDelete = shardedStore.getVectors(ids)
        assertEquals(0, retrievedAfterDelete.size)
    }
    
    @Test
    fun `test vector count and clear`() = runBlocking {
        // 创建测试向量
        val vectors = listOf(
            floatArrayOf(0.1f, 0.2f, 0.3f),
            floatArrayOf(0.4f, 0.5f, 0.6f),
            floatArrayOf(0.7f, 0.8f, 0.9f)
        )
        
        // 批量添加向量
        shardedStore.addVectors(vectors)
        
        // 验证向量数量
        assertEquals(vectors.size, shardedStore.getVectorCount())
        
        // 清空存储
        val success = shardedStore.clear()
        assertTrue(success)
        
        // 验证向量数量为 0
        assertEquals(0, shardedStore.getVectorCount())
    }
    
    @Test
    fun `test shard info`() = runBlocking {
        // 创建测试向量
        val vectors = listOf(
            floatArrayOf(0.1f, 0.2f, 0.3f),
            floatArrayOf(0.4f, 0.5f, 0.6f),
            floatArrayOf(0.7f, 0.8f, 0.9f),
            floatArrayOf(0.1f, 0.2f, 0.3f),
            floatArrayOf(0.4f, 0.5f, 0.6f)
        )
        
        // 批量添加向量
        shardedStore.addVectors(vectors)
        
        // 获取分片信息
        val shardInfo = shardedStore.getShardInfo()
        
        // 验证分片数量
        assertEquals(2, shardInfo.size)
        
        // 验证向量总数
        val totalVectors = shardInfo.sumOf { it.vectorCount }
        assertEquals(vectors.size, totalVectors)
    }
}
