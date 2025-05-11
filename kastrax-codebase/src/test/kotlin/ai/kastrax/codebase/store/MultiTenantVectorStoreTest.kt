package ai.kastrax.codebase.store

import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.embedding.MockEmbeddingService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MultiTenantVectorStoreTest {
    
    private lateinit var mockEmbeddingService: MockEmbeddingService
    private lateinit var multiTenantStore: MultiTenantVectorStore
    
    @BeforeEach
    fun setUp() {
        // 创建嵌入服务
        mockEmbeddingService = MockEmbeddingService()
        
        // 创建多租户向量存储
        multiTenantStore = MultiTenantVectorStore(
            baseVectorStoreFactory = { tenantId ->
                VectorStoreFactory.createInMemoryVectorStore()
            },
            config = MultiTenantVectorStoreConfig(
                maxTenantsInMemory = 3,
                maxVectorsPerTenant = 100,
                evictionStrategy = EvictionStrategy.LRU
            )
        )
    }
    
    @Test
    fun `test adding and retrieving vectors for different tenants`() = runBlocking {
        // 创建测试向量
        val vector1 = floatArrayOf(0.1f, 0.2f, 0.3f)
        val vector2 = floatArrayOf(0.4f, 0.5f, 0.6f)
        
        // 添加向量到不同租户
        val id1 = multiTenantStore.addVector("tenant1", vector1)
        val id2 = multiTenantStore.addVector("tenant2", vector2)
        
        // 验证ID不为空
        assertNotNull(id1)
        assertNotNull(id2)
        
        // 获取向量
        val result1 = multiTenantStore.searchVector("tenant1", vector1, limit = 1)
        val result2 = multiTenantStore.searchVector("tenant2", vector2, limit = 1)
        
        // 验证结果
        assertEquals(1, result1.size)
        assertEquals(1, result2.size)
        
        // 验证向量
        assertTrue(vector1.contentEquals(result1[0].vector.vector))
        assertTrue(vector2.contentEquals(result2[0].vector.vector))
    }
    
    @Test
    fun `test tenant eviction`() = runBlocking {
        // 创建测试向量
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f)
        
        // 添加向量到多个租户
        val id1 = multiTenantStore.addVector("tenant1", vector)
        val id2 = multiTenantStore.addVector("tenant2", vector)
        val id3 = multiTenantStore.addVector("tenant3", vector)
        
        // 验证租户数量
        val stats1 = multiTenantStore.getTenantStats()
        assertEquals(3, stats1.size)
        
        // 添加向量到新租户，触发驱逐
        val id4 = multiTenantStore.addVector("tenant4", vector)
        
        // 验证租户数量仍然是 3
        val stats2 = multiTenantStore.getTenantStats()
        assertEquals(3, stats2.size)
        
        // 验证最早访问的租户被驱逐
        val tenantIds = stats2.map { it.tenantId }
        assertTrue(tenantIds.contains("tenant4"))
    }
    
    @Test
    fun `test adding and searching embeddings for different tenants`() = runBlocking {
        // 创建测试文本
        val text1 = "public class TestClass { void testMethod() { } }"
        val text2 = "function testFunction() { return true; }"
        
        // 添加嵌入到不同租户
        val id1 = multiTenantStore.addEmbedding("tenant1", text1, embeddingService = mockEmbeddingService)
        val id2 = multiTenantStore.addEmbedding("tenant2", text2, embeddingService = mockEmbeddingService)
        
        // 验证ID不为空
        assertNotNull(id1)
        assertNotNull(id2)
        
        // 搜索嵌入
        val results1 = multiTenantStore.searchEmbedding("tenant1", "class", embeddingService = mockEmbeddingService)
        val results2 = multiTenantStore.searchEmbedding("tenant2", "function", embeddingService = mockEmbeddingService)
        
        // 验证结果
        assertTrue(results1.isNotEmpty())
        assertTrue(results2.isNotEmpty())
    }
    
    @Test
    fun `test clearing tenant store`() = runBlocking {
        // 创建测试向量
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f)
        
        // 添加向量到租户
        val id = multiTenantStore.addVector("tenant1", vector)
        
        // 验证向量存在
        val results = multiTenantStore.searchVector("tenant1", vector, limit = 1)
        assertEquals(1, results.size)
        
        // 清空租户存储
        val success = multiTenantStore.clearTenant("tenant1")
        assertTrue(success)
        
        // 验证向量不存在
        val resultsAfterClear = multiTenantStore.searchVector("tenant1", vector, limit = 1)
        assertEquals(0, resultsAfterClear.size)
    }
    
    @Test
    fun `test tenant statistics`() = runBlocking {
        // 创建测试向量
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f)
        
        // 添加向量到不同租户
        multiTenantStore.addVector("tenant1", vector)
        multiTenantStore.addVector("tenant1", vector)
        multiTenantStore.addVector("tenant2", vector)
        
        // 获取租户统计
        val stats = multiTenantStore.getTenantStats()
        
        // 验证统计
        assertEquals(2, stats.size)
        
        val tenant1Stats = stats.find { it.tenantId == "tenant1" }
        val tenant2Stats = stats.find { it.tenantId == "tenant2" }
        
        assertNotNull(tenant1Stats)
        assertNotNull(tenant2Stats)
        
        assertEquals(2, tenant1Stats.vectorCount)
        assertEquals(1, tenant2Stats.vectorCount)
    }
}
