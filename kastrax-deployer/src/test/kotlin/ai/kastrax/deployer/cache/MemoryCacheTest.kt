package ai.kastrax.deployer.cache

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MemoryCacheTest {
    
    @Test
    fun `test get and set`() = runBlocking {
        val cache = MemoryCache<String, String>("test-cache")
        
        // 初始状态
        assertNull(cache.get("key1"), "Cache should be empty initially")
        
        // 设置值
        cache.set("key1", "value1")
        assertEquals("value1", cache.get("key1"), "Cache should return the set value")
        
        // 更新值
        cache.set("key1", "value2")
        assertEquals("value2", cache.get("key1"), "Cache should return the updated value")
    }
    
    @Test
    fun `test expiration`() = runBlocking {
        val cache = MemoryCache<String, String>("test-cache")
        
        // 设置带过期时间的值
        cache.set("key1", "value1", Duration.ofMillis(100))
        assertEquals("value1", cache.get("key1"), "Cache should return the value before expiration")
        
        // 等待过期
        Thread.sleep(200)
        
        // 过期后应该返回 null
        assertNull(cache.get("key1"), "Cache should return null after expiration")
    }
    
    @Test
    fun `test delete`() = runBlocking {
        val cache = MemoryCache<String, String>("test-cache")
        
        // 设置值
        cache.set("key1", "value1")
        assertEquals("value1", cache.get("key1"), "Cache should return the set value")
        
        // 删除值
        val deleted = cache.delete("key1")
        assertTrue(deleted, "Delete should return true for existing key")
        assertNull(cache.get("key1"), "Cache should return null after deletion")
        
        // 删除不存在的值
        val notDeleted = cache.delete("key2")
        assertFalse(notDeleted, "Delete should return false for non-existing key")
    }
    
    @Test
    fun `test exists`() = runBlocking {
        val cache = MemoryCache<String, String>("test-cache")
        
        // 初始状态
        assertFalse(cache.exists("key1"), "Cache should not contain the key initially")
        
        // 设置值
        cache.set("key1", "value1")
        assertTrue(cache.exists("key1"), "Cache should contain the key after setting")
        
        // 删除值
        cache.delete("key1")
        assertFalse(cache.exists("key1"), "Cache should not contain the key after deletion")
    }
    
    @Test
    fun `test clear`() = runBlocking {
        val cache = MemoryCache<String, String>("test-cache")
        
        // 设置多个值
        cache.set("key1", "value1")
        cache.set("key2", "value2")
        
        assertEquals(2, cache.size(), "Cache should contain 2 items")
        
        // 清空缓存
        cache.clear()
        
        assertEquals(0, cache.size(), "Cache should be empty after clear")
        assertNull(cache.get("key1"), "Cache should return null after clear")
        assertNull(cache.get("key2"), "Cache should return null after clear")
    }
    
    @Test
    fun `test size and keys`() = runBlocking {
        val cache = MemoryCache<String, String>("test-cache")
        
        // 初始状态
        assertEquals(0, cache.size(), "Cache should be empty initially")
        assertEquals(emptySet<String>(), cache.keys(), "Cache should have no keys initially")
        
        // 设置多个值
        cache.set("key1", "value1")
        cache.set("key2", "value2")
        
        assertEquals(2, cache.size(), "Cache should contain 2 items")
        assertEquals(setOf("key1", "key2"), cache.keys(), "Cache should have the correct keys")
        
        // 删除一个值
        cache.delete("key1")
        
        assertEquals(1, cache.size(), "Cache should contain 1 item after deletion")
        assertEquals(setOf("key2"), cache.keys(), "Cache should have the correct keys after deletion")
    }
    
    @Test
    fun `test max size`() = runBlocking {
        val cache = MemoryCache<String, String>("test-cache", maxSize = 2)
        
        // 设置多个值
        cache.set("key1", "value1")
        cache.set("key2", "value2")
        
        assertEquals(2, cache.size(), "Cache should contain 2 items")
        
        // 设置第三个值，应该驱逐最旧的值
        cache.set("key3", "value3")
        
        assertEquals(2, cache.size(), "Cache should still contain 2 items due to max size")
        assertNull(cache.get("key1"), "Oldest key should be evicted")
        assertEquals("value2", cache.get("key2"), "Second key should still exist")
        assertEquals("value3", cache.get("key3"), "Newest key should exist")
    }
    
    @Test
    fun `test stats`() = runBlocking {
        val cache = MemoryCache<String, String>("test-cache")
        
        // 初始状态
        val initialStats = cache.stats()
        assertEquals(0, initialStats.hitCount, "Hit count should be 0 initially")
        assertEquals(0, initialStats.missCount, "Miss count should be 0 initially")
        
        // 设置值
        cache.set("key1", "value1")
        
        // 命中
        cache.get("key1")
        
        // 未命中
        cache.get("key2")
        
        val stats = cache.stats()
        assertEquals(1, stats.hitCount, "Hit count should be 1")
        assertEquals(1, stats.missCount, "Miss count should be 1")
        assertEquals(0.5, stats.hitRate(), "Hit rate should be 0.5")
    }
}
