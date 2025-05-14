package ai.kastrax.codebase.search

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class PatternSearcherTest {

    private lateinit var patternSearcher: PatternSearcher
    
    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        patternSearcher = PatternSearcher(
            PatternSearcherConfig(
                enableCaching = true,
                maxCacheSize = 10,
                followSymlinks = false,
                maxDepth = 10,
                excludePatterns = listOf("*.log"),
                includePatterns = emptyList(),
                respectGitignore = true,
                hiddenFiles = false
            )
        )
        
        // 创建测试文件
        createTestFiles()
    }
    
    private fun createTestFiles() {
        // 创建普通文件
        Files.writeString(tempDir.resolve("test1.txt"), "This is a test file")
        Files.writeString(tempDir.resolve("test2.txt"), "Another test file")
        Files.writeString(tempDir.resolve("example.java"), "public class Example {}")
        Files.writeString(tempDir.resolve("sample.kt"), "class Sample {}")
        
        // 创建子目录
        val subDir = Files.createDirectory(tempDir.resolve("subdir"))
        Files.writeString(subDir.resolve("test3.txt"), "Test file in subdir")
        Files.writeString(subDir.resolve("example2.java"), "public class Example2 {}")
        
        // 创建隐藏文件
        Files.writeString(tempDir.resolve(".hidden.txt"), "Hidden file")
        
        // 创建日志文件
        Files.writeString(tempDir.resolve("app.log"), "Log file")
        
        // 创建 .gitignore 文件
        Files.writeString(tempDir.resolve(".gitignore"), "*.log\n.hidden*")
    }
    
    @Test
    fun testFindFilesByRegex() = runBlocking {
        // 查找所有 .txt 文件
        val txtFiles = patternSearcher.findFilesByRegex(tempDir, ".*\\.txt")
        
        // 验证结果
        assertEquals(2, txtFiles.size)
        assertTrue(txtFiles.any { it.fileName.toString() == "test1.txt" })
        assertTrue(txtFiles.any { it.fileName.toString() == "test2.txt" })
        assertFalse(txtFiles.any { it.fileName.toString() == ".hidden.txt" }) // 隐藏文件应该被排除
    }
    
    @Test
    fun testFindFilesByRegexWithHiddenFiles() = runBlocking {
        // 查找所有 .txt 文件，包括隐藏文件
        val txtFiles = patternSearcher.findFilesByRegex(
            tempDir, 
            ".*\\.txt", 
            mapOf("hiddenFiles" to true)
        )
        
        // 验证结果
        assertEquals(3, txtFiles.size)
        assertTrue(txtFiles.any { it.fileName.toString() == "test1.txt" })
        assertTrue(txtFiles.any { it.fileName.toString() == "test2.txt" })
        assertTrue(txtFiles.any { it.fileName.toString() == ".hidden.txt" }) // 隐藏文件应该被包含
    }
    
    @Test
    fun testFindFilesByRegexWithExcludePatterns() = runBlocking {
        // 查找所有文件，排除 .java 文件
        val files = patternSearcher.findFilesByRegex(
            tempDir, 
            ".*", 
            mapOf("excludePatterns" to listOf(".*\\.java"))
        )
        
        // 验证结果
        assertFalse(files.any { it.fileName.toString().endsWith(".java") })
        assertTrue(files.any { it.fileName.toString() == "test1.txt" })
        assertTrue(files.any { it.fileName.toString() == "test2.txt" })
        assertTrue(files.any { it.fileName.toString() == "sample.kt" })
    }
    
    @Test
    fun testFindFilesByRegexWithIncludePatterns() = runBlocking {
        // 查找所有文件，只包含 .java 和 .kt 文件
        val files = patternSearcher.findFilesByRegex(
            tempDir, 
            ".*", 
            mapOf("includePatterns" to listOf(".*\\.(java|kt)"))
        )
        
        // 验证结果
        assertTrue(files.all { it.fileName.toString().endsWith(".java") || it.fileName.toString().endsWith(".kt") })
        assertFalse(files.any { it.fileName.toString().endsWith(".txt") })
    }
    
    @Test
    fun testFindFilesByRegexWithMaxDepth() = runBlocking {
        // 查找所有文件，最大深度为 0（只查找当前目录）
        val files = patternSearcher.findFilesByRegex(
            tempDir, 
            ".*", 
            mapOf("maxDepth" to 0)
        )
        
        // 验证结果
        assertFalse(files.any { it.toString().contains("subdir") })
        assertTrue(files.any { it.fileName.toString() == "test1.txt" })
        assertTrue(files.any { it.fileName.toString() == "test2.txt" })
    }
    
    @Test
    fun testCaching() = runBlocking {
        // 第一次查找
        val firstResult = patternSearcher.findFilesByRegex(tempDir, ".*\\.txt")
        
        // 第二次查找（应该从缓存中获取）
        val secondResult = patternSearcher.findFilesByRegex(tempDir, ".*\\.txt")
        
        // 验证结果
        assertEquals(firstResult, secondResult)
        
        // 清除缓存
        patternSearcher.clearCache()
        
        // 创建新文件
        Files.writeString(tempDir.resolve("test4.txt"), "New test file")
        
        // 第三次查找（应该重新搜索）
        val thirdResult = patternSearcher.findFilesByRegex(tempDir, ".*\\.txt")
        
        // 验证结果
        assertNotEquals(firstResult, thirdResult)
        assertEquals(3, thirdResult.size)
    }
}
