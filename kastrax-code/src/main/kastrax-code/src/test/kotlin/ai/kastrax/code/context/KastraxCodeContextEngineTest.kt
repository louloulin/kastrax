package ai.kastrax.code.context

import com.intellij.testFramework.LightPlatformTestCase
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.nio.file.Paths

/**
 * KastraxCodeContextEngine测试
 */
class KastraxCodeContextEngineTest : LightPlatformTestCase() {

    private lateinit var contextEngine: KastraxCodeContextEngine

    override fun setUp() {
        super.setUp()
        contextEngine = KastraxCodeContextEngine()
    }

    /**
     * 测试初始化
     */
    @Test
    fun testInitialize() = runBlocking {
        // 索引代码库，这会触发初始化
        val result = contextEngine.indexCodebase(Paths.get("/test/path"))
        
        // 验证结果
        assertTrue("索引代码库应该成功", result)
    }

    /**
     * 测试获取查询上下文
     */
    @Test
    fun testGetQueryContext() = runBlocking {
        // 获取查询上下文
        val context = contextEngine.getQueryContext("test query")
        
        // 验证结果
        assertNotNull("上下文不应为空", context)
        assertEquals("查询应该匹配", "test query", context.query)
    }

    /**
     * 测试获取文件上下文
     */
    @Test
    fun testGetFileContext() = runBlocking {
        // 获取文件上下文
        val filePath = Paths.get("test.kt")
        val context = contextEngine.getFileContext(filePath)
        
        // 验证结果
        assertNotNull("上下文不应为空", context)
        assertTrue("查询应该包含文件名", context.query.contains("test.kt"))
    }

    /**
     * 测试获取编辑上下文
     */
    @Test
    fun testGetEditContext() = runBlocking {
        // 获取编辑上下文
        val filePath = Paths.get("test.kt")
        val position = Location(1, 1, 1, 1)
        val context = contextEngine.getEditContext(filePath, position)
        
        // 验证结果
        assertNotNull("上下文不应为空", context)
        assertTrue("查询应该包含文件名", context.query.contains("test.kt"))
    }

    /**
     * 测试获取符号上下文
     */
    @Test
    fun testGetSymbolContext() = runBlocking {
        // 获取符号上下文
        val symbolName = "TestClass"
        val context = contextEngine.getSymbolContext(symbolName)
        
        // 验证结果
        assertNotNull("上下文不应为空", context)
        assertTrue("查询应该包含符号名称", context.query.contains(symbolName))
    }

    /**
     * 测试关闭
     */
    @Test
    fun testClose() = runBlocking {
        // 索引代码库，这会触发初始化
        contextEngine.indexCodebase(Paths.get("/test/path"))
        
        // 关闭上下文引擎
        contextEngine.close()
        
        // 再次索引代码库，这会再次触发初始化
        val result = contextEngine.indexCodebase(Paths.get("/test/path"))
        
        // 验证结果
        assertTrue("索引代码库应该成功", result)
    }
}
