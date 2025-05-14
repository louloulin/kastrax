package ai.kastrax.codebase.search

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RipgrepSearcherTest {
    
    @Test
    fun testFindRipgrepBinary() {
        val searcher = RipgrepSearcher()
        
        // 使用反射获取私有方法
        val findRipgrepBinaryMethod = RipgrepSearcher::class.java.getDeclaredMethod("findRipgrepBinary")
        findRipgrepBinaryMethod.isAccessible = true
        
        val rgPath = findRipgrepBinaryMethod.invoke(searcher) as? Path
        
        // 如果找不到 ripgrep，尝试安装它
        if (rgPath == null) {
            runBlocking {
                val installedPath = searcher.installRipgrep()
                println("Installed ripgrep at: $installedPath")
            }
        } else {
            println("Found ripgrep at: $rgPath")
        }
    }
    
    @Test
    fun testBuildCommand(@TempDir tempDir: Path) {
        val searcher = RipgrepSearcher()
        
        // 使用反射获取私有方法
        val buildCommandMethod = RipgrepSearcher::class.java.getDeclaredMethod(
            "buildCommand",
            Path::class.java,
            String::class.java,
            List::class.java,
            Map::class.java
        )
        buildCommandMethod.isAccessible = true
        
        // 创建一个假的 ripgrep 路径
        val fakePath = tempDir.resolve("rg")
        
        // 构建命令
        val command = buildCommandMethod.invoke(
            searcher,
            fakePath,
            "test",
            listOf(tempDir),
            mapOf(
                "ignoreCase" to true,
                "wordMatch" to true,
                "contextLines" to 3
            )
        ) as List<String>
        
        // 验证命令
        assertEquals(fakePath.toString(), command[0])
        assertTrue(command.contains("--json"))
        assertTrue(command.contains("--ignore-case"))
        assertTrue(command.contains("--word-regexp"))
        assertTrue(command.contains("--context"))
        assertTrue(command.contains("3"))
        assertTrue(command.contains("--regexp"))
        assertTrue(command.contains("test"))
        assertTrue(command.contains(tempDir.toString()))
    }
    
    @Test
    fun testSearch(@TempDir tempDir: Path) = runBlocking {
        // 创建测试文件
        val testFile = tempDir.resolve("test.txt")
        Files.write(testFile, """
            This is a test file.
            It contains some test content.
            The word test appears multiple times.
            This is the last line.
        """.trimIndent().toByteArray())
        
        val searcher = RipgrepSearcher()
        
        // 执行搜索
        val results = searcher.search(
            query = "test",
            paths = listOf(tempDir),
            options = mapOf(
                "ignoreCase" to true
            )
        ).toList()
        
        // 验证结果
        assertNotNull(results)
        assertTrue(results.isNotEmpty())
        
        // 检查第一个结果
        val firstResult = results.firstOrNull()
        assertNotNull(firstResult)
        assertEquals(testFile, firstResult.filePath)
        assertTrue(firstResult.matchText.contains("test", ignoreCase = true))
    }
    
    @Test
    fun testConvertToRetrievalResult(@TempDir tempDir: Path) {
        val searcher = RipgrepSearcher()
        
        // 创建测试结果
        val result = RipgrepSearchResult(
            filePath = tempDir.resolve("test.txt"),
            lineNumber = 1,
            columnNumber = 10,
            matchText = "test",
            lineText = "This is a test line.",
            beforeContext = listOf("Before line 1", "Before line 2"),
            afterContext = listOf("After line 1", "After line 2")
        )
        
        // 转换为 RetrievalResult
        val retrievalResult = searcher.convertToRetrievalResult(result)
        
        // 验证结果
        assertNotNull(retrievalResult)
        assertEquals("${tempDir.resolve("test.txt")}:1", retrievalResult.element.id)
        assertEquals("test.txt", retrievalResult.element.name)
        assertEquals(1.0, retrievalResult.score)
    }
}
