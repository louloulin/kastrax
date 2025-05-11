package ai.kastrax.codebase.store

import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.embedding.MockEmbeddingService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CodeVectorStoreTest {
    
    private lateinit var mockEmbeddingService: MockEmbeddingService
    private lateinit var codeVectorStore: CodeVectorStore
    
    @BeforeEach
    fun setUp() {
        // 创建嵌入服务
        mockEmbeddingService = MockEmbeddingService()
        
        // 创建向量存储
        val baseVectorStore = VectorStoreFactory.createInMemoryVectorStore()
        
        // 创建代码向量存储
        codeVectorStore = CodeVectorStore(
            baseVectorStore = baseVectorStore,
            config = CodeVectorStoreConfig(
                maxVectors = 1000,
                dimension = mockEmbeddingService.dimension,
                distanceThreshold = 0.5
            )
        )
    }
    
    @Test
    fun `test adding and retrieving vectors`() = runBlocking {
        // 创建测试向量
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f)
        val metadata = mapOf("key" to "value")
        
        // 添加向量
        val id = codeVectorStore.addVector(vector, metadata)
        
        // 验证ID不为空
        assertNotNull(id)
        
        // 获取向量
        val retrieved = codeVectorStore.getVector(id)
        
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
        val ids = codeVectorStore.addVectors(vectors, metadata)
        
        // 验证ID数量
        assertEquals(vectors.size, ids.size)
        
        // 批量获取向量
        val retrieved = codeVectorStore.getVectors(ids)
        
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
        val ids = codeVectorStore.addEmbeddings(texts, metadata, mockEmbeddingService)
        
        // 验证ID数量
        assertEquals(texts.size, ids.size)
        
        // 搜索嵌入
        val query = "class TestClass"
        val results = codeVectorStore.searchEmbedding(query, limit = 2, minScore = 0.0, embeddingService = mockEmbeddingService)
        
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
        val id = codeVectorStore.addVector(vector, metadata)
        
        // 验证向量存在
        val retrieved = codeVectorStore.getVector(id)
        assertNotNull(retrieved)
        
        // 删除向量
        val success = codeVectorStore.deleteVector(id)
        assertTrue(success)
        
        // 验证向量不存在
        val retrievedAfterDelete = codeVectorStore.getVector(id)
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
        val ids = codeVectorStore.addVectors(vectors)
        
        // 验证向量存在
        val retrieved = codeVectorStore.getVectors(ids)
        assertEquals(vectors.size, retrieved.size)
        
        // 批量删除向量
        val success = codeVectorStore.deleteVectors(ids)
        assertTrue(success)
        
        // 验证向量不存在
        val retrievedAfterDelete = codeVectorStore.getVectors(ids)
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
        codeVectorStore.addVectors(vectors)
        
        // 验证向量数量
        assertEquals(vectors.size, codeVectorStore.getVectorCount())
        
        // 清空存储
        val success = codeVectorStore.clear()
        assertTrue(success)
        
        // 验证向量数量为 0
        assertEquals(0, codeVectorStore.getVectorCount())
    }
}
