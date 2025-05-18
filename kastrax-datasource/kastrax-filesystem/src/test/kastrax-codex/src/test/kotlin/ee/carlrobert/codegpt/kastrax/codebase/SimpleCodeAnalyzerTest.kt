package ee.carlrobert.codegpt.kastrax.codebase

import ai.kastrax.codebase.CodebaseContext
import ai.kastrax.codebase.CodebaseSearchResult
import ai.kastrax.codebase.analysis.CodeAnalysisOptions
import ai.kastrax.codebase.analysis.CodeAnalysisResult
import ai.kastrax.codebase.analysis.CodeSymbol
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import ee.carlrobert.codegpt.kastrax.KastraxAdapter
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

class SimpleCodeAnalyzerTest {
    private lateinit var project: Project
    private lateinit var kastraxAdapter: KastraxAdapter
    private lateinit var codeAnalyzer: SimpleCodeAnalyzer
    private lateinit var mockCodebaseContext: CodebaseContext
    private lateinit var mockFile: VirtualFile

    @Before
    fun setup() {
        // 创建mock对象
        project = mockk(relaxed = true)
        kastraxAdapter = mockk(relaxed = true)
        mockCodebaseContext = mockk(relaxed = true)
        mockFile = mockk(relaxed = true)

        // 配置mock行为
        every { project.getService(KastraxAdapter::class.java) } returns kastraxAdapter
        every { mockFile.path } returns "src/main/java/example/Test.java"
        every { mockFile.contentsToByteArray() } returns "public class Test { }".toByteArray()

        // 创建SimpleCodeAnalyzer实例
        codeAnalyzer = SimpleCodeAnalyzer(project)
    }

    @Test
    fun testInitialize() {
        // 测试初始化
        codeAnalyzer.initialize()
    }

    @Test
    fun testGetFileContent() {
        // 测试获取文件内容
        val content = codeAnalyzer.getFileContent(mockFile)

        // 验证结果
        assertEquals("public class Test { }", content)
    }

    @Test
    fun testAnalyzeFileWithKastrax() = runBlocking {
        // 模拟分析结果
        val mockSymbols = listOf(
            mockk<CodeSymbol>().apply {
                every { name } returns "Test"
                every { type } returns "class"
                every { documentation } returns "Test class"
            }
        )

        val mockImports = listOf("java.util.List")

        val mockResult = mockk<CodeAnalysisResult>().apply {
            every { language } returns "java"
            every { symbols } returns mockSymbols
            every { imports } returns mockImports
        }

        // 配置mock行为
        val optionsSlot = slot<CodeAnalysisOptions>()
        coEvery { mockCodebaseContext.analyzeFile(capture(optionsSlot)) } returns mockResult

        // 设置codebaseContext字段
        val field = SimpleCodeAnalyzer::class.java.getDeclaredField("codebaseContext")
        field.isAccessible = true
        field.set(codeAnalyzer, mockCodebaseContext)

        val field2 = SimpleCodeAnalyzer::class.java.getDeclaredField("useKastraxCodebase")
        field2.isAccessible = true
        field2.set(codeAnalyzer, true)

        // 测试分析文件
        val result = codeAnalyzer.analyzeFile(mockFile)

        // 验证结果
        assertTrue(result.contains("文件: src/main/java/example/Test.java"))
        assertTrue(result.contains("语言: java"))
        assertTrue(result.contains("符号:"))
        assertTrue(result.contains("- Test (class)"))
        assertTrue(result.contains("导入:"))
        assertTrue(result.contains("- java.util.List"))

        // 验证传递的选项
        // 注意：由于我们使用了Builder模式，无法直接验证选项
        // 只验证结果是否符合预期
    }

    @Test
    fun testSearchCodeWithKastrax() = runBlocking {
        // 模拟搜索结果
        val mockResults = listOf(
            mockk<CodebaseSearchResult>().apply {
                every { filePath } returns "src/main/java/example/Test.java"
                every { content } returns "public class Test { }"
                every { language } returns "java"
                every { score } returns 0.95
            }
        )

        // 配置mock行为
        coEvery { mockCodebaseContext.search(any()) } returns mockResults

        // 设置codebaseContext字段
        val field = SimpleCodeAnalyzer::class.java.getDeclaredField("codebaseContext")
        field.isAccessible = true
        field.set(codeAnalyzer, mockCodebaseContext)

        val field2 = SimpleCodeAnalyzer::class.java.getDeclaredField("useKastraxCodebase")
        field2.isAccessible = true
        field2.set(codeAnalyzer, true)

        // 测试搜索代码
        val results = codeAnalyzer.searchCode("test")

        // 验证结果
        assertEquals(1, results.size)
        assertEquals("src/main/java/example/Test.java", results[0].filePath)
        assertEquals("public class Test { }", results[0].content)
        assertEquals("java", results[0].language)
        assertEquals(0.95, results[0].score, 0.001)
    }
}
