package ai.kastrax.code.context

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.nio.file.Path
import java.nio.file.Paths

/**
 * KastraxCodeContextEngine 测试类
 */
class KastraxCodeContextEngineTest {

    /**
     * 测试创建 KastraxCodeContextEngine 实例
     */
    @Test
    fun testCreateInstance() {
        // 创建实例
        val engine = KastraxCodeContextEngine()
        
        // 验证实例不为空
        assertNotNull(engine)
    }
    
    /**
     * 测试索引代码库
     */
    @Test
    fun testIndexCodebase() = runBlocking {
        // 创建实例
        val engine = KastraxCodeContextEngine()
        
        // 索引代码库
        val path = Paths.get("src/test/resources/testproject")
        val result = engine.indexCodebase(path)
        
        // 验证结果
        assertTrue(result)
    }
    
    /**
     * 测试获取查询上下文
     */
    @Test
    fun testGetQueryContext() = runBlocking {
        // 创建实例
        val engine = KastraxCodeContextEngine()
        
        // 获取查询上下文
        val context = engine.getQueryContext("test query", 10, 0.5, true)
        
        // 验证结果
        assertNotNull(context)
        assertEquals("test query", context.query)
    }
    
    /**
     * 测试获取文件上下文
     */
    @Test
    fun testGetFileContext() = runBlocking {
        // 创建实例
        val engine = KastraxCodeContextEngine()
        
        // 获取文件上下文
        val path = Paths.get("src/test/resources/testproject/test.kt")
        val context = engine.getFileContext(path, 10)
        
        // 验证结果
        assertNotNull(context)
        assertTrue(context.query.contains("test.kt"))
    }
    
    /**
     * 测试关闭上下文引擎
     */
    @Test
    fun testClose() = runBlocking {
        // 创建实例
        val engine = KastraxCodeContextEngine()
        
        // 关闭上下文引擎
        engine.close()
        
        // 验证没有异常抛出
        assertTrue(true)
    }
}
