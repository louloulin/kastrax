package ai.kastrax.code.context

import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.mock
import java.nio.file.Path
import java.nio.file.Paths
import ai.kastrax.code.model.Location

/**
 * CodeContextEngineImpl 测试类
 */
class CodeContextEngineImplTest {

    /**
     * 测试创建 CodeContextEngineImpl 实例
     */
    @Test
    fun testCreateInstance() {
        // 创建 Mock Project
        val project = mock(Project::class.java)
        
        // 创建实例
        val engine = CodeContextEngineImpl(project)
        
        // 验证实例不为空
        assertNotNull(engine)
    }
    
    /**
     * 测试获取查询上下文
     */
    @Test
    fun testGetQueryContext() = runBlocking {
        // 创建 Mock Project
        val project = mock(Project::class.java)
        
        // 创建实例
        val engine = CodeContextEngineImpl(project)
        
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
        // 创建 Mock Project
        val project = mock(Project::class.java)
        
        // 创建实例
        val engine = CodeContextEngineImpl(project)
        
        // 获取文件上下文
        val path = Paths.get("src/test/resources/testproject/test.kt")
        val context = engine.getFileContext(path, 10)
        
        // 验证结果
        assertNotNull(context)
        assertTrue(context.query.contains("test.kt"))
    }
    
    /**
     * 测试获取编辑上下文
     */
    @Test
    fun testGetEditContext() = runBlocking {
        // 创建 Mock Project
        val project = mock(Project::class.java)
        
        // 创建实例
        val engine = CodeContextEngineImpl(project)
        
        // 获取编辑上下文
        val path = Paths.get("src/test/resources/testproject/test.kt")
        val position = Location(1, 1, 10, 10)
        val context = engine.getEditContext(path, position, 10, 0.5)
        
        // 验证结果
        assertNotNull(context)
        assertTrue(context.query.contains("test.kt"))
    }
    
    /**
     * 测试获取符号上下文
     */
    @Test
    fun testGetSymbolContext() = runBlocking {
        // 创建 Mock Project
        val project = mock(Project::class.java)
        
        // 创建实例
        val engine = CodeContextEngineImpl(project)
        
        // 获取符号上下文
        val context = engine.getSymbolContext("TestClass", 10, 0.5)
        
        // 验证结果
        assertNotNull(context)
        assertTrue(context.query.contains("TestClass"))
    }
    
    /**
     * 测试关闭上下文引擎
     */
    @Test
    fun testClose() = runBlocking {
        // 创建 Mock Project
        val project = mock(Project::class.java)
        
        // 创建实例
        val engine = CodeContextEngineImpl(project)
        
        // 关闭上下文引擎
        engine.close()
        
        // 验证没有异常抛出
        assertTrue(true)
    }
}
