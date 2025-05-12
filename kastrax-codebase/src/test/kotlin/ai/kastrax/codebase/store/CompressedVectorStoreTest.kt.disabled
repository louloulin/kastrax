package ai.kastrax.codebase.store

import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.embedding.MockEmbeddingService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CompressedVectorStoreTest {
    
    private lateinit var mockEmbeddingService: MockEmbeddingService
    private lateinit var compressedStore: CompressedVectorStore
    
    @BeforeEach
    fun setUp() {
        // 创建嵌入服务
        mockEmbeddingService = MockEmbeddingService()
        
        // 创建压缩向量存储
        compressedStore = CompressedVectorStore(
            baseVectorStore = VectorStoreFactory.createInMemoryVectorStore(),
            config = CompressedVectorStoreConfig(
                compressionMethod = CompressionMethod.SCALAR_QUANTIZATION,
                quantizationBits = 8
            )
        )
    }
    
    @Test
    fun `test adding and retrieving vectors with scalar quantization`() = runBlocking {
        // 创建测试向量
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f)
        val metadata = mapOf("key" to "value")
        
        // 添加向量
        val id = compressedStore.addVector(vector, metadata)
        
        // 验证ID不为空
        assertNotNull(id)
        
        // 获取向量
        val retrieved = compressedStore.getVector(id)
        
        // 验证向量
        assertNotNull(retrieved)
        assertEquals(id, retrieved.id)
        
        // 由于压缩，向量可能不完全相同，但应该相似
        for (i in vector.indices) {
            assertTrue(Math.abs(vector[i] - retrieved.vector[i]) < 0.1f)
        }
        
        assertEquals(metadata, retrieved.metadata)
    }
    
    @Test
    fun `test adding and retrieving vectors with binarization`() = runBlocking {
        // 创建使用二值化的压缩向量存储
        val binarizedStore = CompressedVectorStore(
            baseVectorStore = VectorStoreFactory.createInMemoryVectorStore(),
            config = CompressedVectorStoreConfig(
                compressionMethod = CompressionMethod.BINARIZATION
            )
        )
        
        // 创建测试向量
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f)
        val metadata = mapOf("key" to "value")
        
        // 添加向量
        val id = binarizedStore.addVector(vector, metadata)
        
        // 验证ID不为空
        assertNotNull(id)
        
        // 获取向量
        val retrieved = binarizedStore.getVector(id)
        
        // 验证向量
        assertNotNull(retrieved)
        assertEquals(id, retrieved.id)
        assertEquals(metadata, retrieved.metadata)
        
        // 二值化后的向量应该只包含 -1.0 和 1.0
        retrieved.vector.forEach { value ->
            assertTrue(value == -1.0f || value == 1.0f)
        }
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
        val ids = compressedStore.addVectors(vectors, metadata)
        
        // 验证ID数量
        assertEquals(vectors.size, ids.size)
        
        // 批量获取向量
        val retrieved = compressedStore.getVectors(ids)
        
        // 验证向量数量
        assertEquals(vectors.size, retrieved.size)
        
        // 验证每个向量
        for (i in vectors.indices) {
            val original = vectors[i]
            val retrievedVector = retrieved.find { it.id == ids[i] }
            
            assertNotNull(retrievedVector)
            assertEquals(metadata[i], retrievedVector.metadata)
            
            // 由于压缩，向量可能不完全相同，但应该相似
            for (j in original.indices) {
                assertTrue(Math.abs(original[j] - retrievedVector.vector[j]) < 0.1f)
            }
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
        val ids = compressedStore.addEmbeddings(texts, metadata, mockEmbeddingService)
        
        // 验证ID数量
        assertEquals(texts.size, ids.size)
        
        // 搜索嵌入
        val query = "class TestClass"
        val results = compressedStore.searchEmbedding(query, limit = 2, minScore = 0.0, embeddingService = mockEmbeddingService)
        
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
        val id = compressedStore.addVector(vector, metadata)
        
        // 验证向量存在
        val retrieved = compressedStore.getVector(id)
        assertNotNull(retrieved)
        
        // 删除向量
        val success = compressedStore.deleteVector(id)
        assertTrue(success)
        
        // 验证向量不存在
        val retrievedAfterDelete = compressedStore.getVector(id)
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
        val ids = compressedStore.addVectors(vectors)
        
        // 验证向量存在
        val retrieved = compressedStore.getVectors(ids)
        assertEquals(vectors.size, retrieved.size)
        
        // 批量删除向量
        val success = compressedStore.deleteVectors(ids)
        assertTrue(success)
        
        // 验证向量不存在
        val retrievedAfterDelete = compressedStore.getVectors(ids)
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
        compressedStore.addVectors(vectors)
        
        // 验证向量数量
        assertEquals(vectors.size, compressedStore.getVectorCount())
        
        // 清空存储
        val success = compressedStore.clear()
        assertTrue(success)
        
        // 验证向量数量为 0
        assertEquals(0, compressedStore.getVectorCount())
    }
    
    @Test
    fun `test compression ratio`() = runBlocking {
        // 创建测试向量
        val vectors = listOf(
            floatArrayOf(0.1f, 0.2f, 0.3f),
            floatArrayOf(0.4f, 0.5f, 0.6f),
            floatArrayOf(0.7f, 0.8f, 0.9f)
        )
        
        // 批量添加向量
        compressedStore.addVectors(vectors)
        
        // 获取压缩率
        val compressionRatio = compressedStore.getCompressionRatio()
        
        // 验证压缩率大于 1.0（表示有压缩）
        assertTrue(compressionRatio > 1.0)
        
        // 验证压缩方法
        assertEquals(CompressionMethod.SCALAR_QUANTIZATION, compressedStore.getCompressionMethod())
    }
}
