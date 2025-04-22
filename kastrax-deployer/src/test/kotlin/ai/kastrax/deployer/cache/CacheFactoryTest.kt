package ai.kastrax.deployer.cache

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CacheFactoryTest {
    
    @Serializable
    data class TestValue(val name: String, val value: Int)
    
    @Test
    fun `test create memory cache`() = runBlocking {
        val cache = CacheFactory.createCache<String, String>(
            type = CacheType.MEMORY,
            name = "test-cache"
        )
        
        assertNotNull(cache, "Cache should be created")
        assertEquals("test-cache", cache.name, "Cache name should match")
        assertEquals(0, cache.size(), "Cache should be empty initially")
        
        // 测试基本操作
        cache.set("key1", "value1")
        assertEquals("value1", cache.get("key1"), "Cache should return the set value")
    }
    
    @Test
    fun `test create memory cache with serializable objects`() = runBlocking {
        val cache = CacheFactory.createCache<String, TestValue>(
            type = CacheType.MEMORY,
            name = "test-object-cache"
        )
        
        assertNotNull(cache, "Cache should be created")
        
        // 测试对象缓存
        val testValue = TestValue("test", 123)
        cache.set("key1", testValue)
        
        val retrievedValue = cache.get("key1")
        assertNotNull(retrievedValue, "Retrieved value should not be null")
        assertEquals("test", retrievedValue.name, "Name should match")
        assertEquals(123, retrievedValue.value, "Value should match")
    }
}
