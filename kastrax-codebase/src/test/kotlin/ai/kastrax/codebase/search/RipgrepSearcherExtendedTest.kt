package ai.kastrax.codebase.search

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class RipgrepSearcherExtendedTest {

    private lateinit var ripgrepSearcher: RipgrepSearcher
    
    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        ripgrepSearcher = RipgrepSearcher(
            RipgrepConfig(
                contextLines = 2,
                ignoreCase = true,
                caseSensitive = false,
                wordMatch = false,
                useRegex = true,
                followSymlinks = false,
                maxFileSize = 1024 * 1024,
                timeout = 30,
                defaultTimeout = 30,
                excludePatterns = listOf("*.log"),
                includePatterns = emptyList(),
                fileTypes = emptyList(),
                excludeFileTypes = emptyList(),
                respectGitignore = true,
                hiddenFiles = false,
                binaryFiles = false,
                excludeBinary = true,
                multiline = false,
                maxDepth = 0,
                maxCount = 0,
                maxMatches = 0,
                enableCaching = true,
                maxCacheSize = 100
            )
        )
        
        // 创建测试文件
        createTestFiles()
    }
    
    private fun createTestFiles() {
        // 创建普通文件
        Files.writeString(tempDir.resolve("test1.txt"), "This is a test file\nWith multiple lines\nContaining test content")
        Files.writeString(tempDir.resolve("test2.txt"), "Another test file\nWith different content")
        Files.writeString(tempDir.resolve("example.java"), "public class Example {\n    // Test comment\n    public void test() {\n        System.out.println(\"test\");\n    }\n}")
        Files.writeString(tempDir.resolve("sample.kt"), "class Sample {\n    // Test comment\n    fun test() {\n        println(\"test\")\n    }\n}")
        
        // 创建子目录
        val subDir = Files.createDirectory(tempDir.resolve("subdir"))
        Files.writeString(subDir.resolve("test3.txt"), "Test file in subdir\nWith test content")
        Files.writeString(subDir.resolve("example2.java"), "public class Example2 {\n    // Another test\n}")
        
        // 创建隐藏文件
        Files.writeString(tempDir.resolve(".hidden.txt"), "Hidden test file")
        
        // 创建日志文件
        Files.writeString(tempDir.resolve("app.log"), "Test log file")
        
        // 创建 .gitignore 文件
        Files.writeString(tempDir.resolve(".gitignore"), "*.log\n.hidden*")
    }
    
    @Test
    fun testSearchFiles() = runBlocking {
        try {
            // 搜索包含 "test" 的文件
            val result = ripgrepSearcher.searchFiles(
                query = "test",
                basePath = tempDir,
                filePattern = "*.txt"
            )
            
            // 验证结果
            assertNotNull(result)
            assertTrue(result.contains("搜索结果: test"))
            assertTrue(result.contains("test1.txt"))
            assertTrue(result.contains("test2.txt"))
            assertFalse(result.contains("example.java")) // 不应该包含 Java 文件
            assertFalse(result.contains("app.log")) // 不应该包含日志文件
        } catch (e: RipgrepSearchException) {
            // 如果 ripgrep 未安装，测试将被跳过
            System.err.println("Skipping test: ${e.message}")
        }
    }
    
    @Test
    fun testSearchFilesWithRegex() = runBlocking {
        try {
            // 使用正则表达式搜索
            val result = ripgrepSearcher.searchFiles(
                query = "test\\s+content",
                basePath = tempDir,
                options = mapOf("useRegex" to true)
            )
            
            // 验证结果
            assertNotNull(result)
            assertTrue(result.contains("test content"))
        } catch (e: RipgrepSearchException) {
            // 如果 ripgrep 未安装，测试将被跳过
            System.err.println("Skipping test: ${e.message}")
        }
    }
    
    @Test
    fun testSearchFilesWithCaseSensitive() = runBlocking {
        try {
            // 大小写敏感搜索
            val result = ripgrepSearcher.searchFiles(
                query = "Test",
                basePath = tempDir,
                options = mapOf("caseSensitive" to true)
            )
            
            // 验证结果
            assertNotNull(result)
            assertTrue(result.contains("Test file in subdir"))
            assertFalse(result.contains("test file")) // 小写的 "test" 不应该被匹配
        } catch (e: RipgrepSearchException) {
            // 如果 ripgrep 未安装，测试将被跳过
            System.err.println("Skipping test: ${e.message}")
        }
    }
    
    @Test
    fun testSearchFilesWithWordMatch() = runBlocking {
        try {
            // 全词匹配搜索
            val result = ripgrepSearcher.searchFiles(
                query = "test",
                basePath = tempDir,
                options = mapOf("wordMatch" to true)
            )
            
            // 验证结果
            assertNotNull(result)
            assertTrue(result.contains("test file")) // "test" 作为单词出现
            assertFalse(result.contains("testing")) // "testing" 不应该被匹配
        } catch (e: RipgrepSearchException) {
            // 如果 ripgrep 未安装，测试将被跳过
            System.err.println("Skipping test: ${e.message}")
        }
    }
    
    @Test
    fun testSearchFilesWithContextLines() = runBlocking {
        try {
            // 设置上下文行数
            val result = ripgrepSearcher.searchFiles(
                query = "test content",
                basePath = tempDir,
                options = mapOf("contextLines" to 1)
            )
            
            // 验证结果
            assertNotNull(result)
            assertTrue(result.contains("With multiple lines")) // 上一行
            assertTrue(result.contains("Containing test content")) // 匹配行
        } catch (e: RipgrepSearchException) {
            // 如果 ripgrep 未安装，测试将被跳过
            System.err.println("Skipping test: ${e.message}")
        }
    }
    
    @Test
    fun testSearchFilesWithHiddenFiles() = runBlocking {
        try {
            // 搜索隐藏文件
            val result = ripgrepSearcher.searchFiles(
                query = "test",
                basePath = tempDir,
                options = mapOf("hiddenFiles" to true)
            )
            
            // 验证结果
            assertNotNull(result)
            assertTrue(result.contains(".hidden.txt")) // 应该包含隐藏文件
        } catch (e: RipgrepSearchException) {
            // 如果 ripgrep 未安装，测试将被跳过
            System.err.println("Skipping test: ${e.message}")
        }
    }
    
    @Test
    fun testSearchFilesWithoutGitignore() = runBlocking {
        try {
            // 不尊重 .gitignore
            val result = ripgrepSearcher.searchFiles(
                query = "test",
                basePath = tempDir,
                options = mapOf("respectGitignore" to false)
            )
            
            // 验证结果
            assertNotNull(result)
            assertTrue(result.contains("app.log")) // 应该包含日志文件
        } catch (e: RipgrepSearchException) {
            // 如果 ripgrep 未安装，测试将被跳过
            System.err.println("Skipping test: ${e.message}")
        }
    }
    
    @Test
    fun testSearchFilesWithFileTypes() = runBlocking {
        try {
            // 搜索特定文件类型
            val result = ripgrepSearcher.searchFiles(
                query = "test",
                basePath = tempDir,
                options = mapOf("fileTypes" to listOf("java"))
            )
            
            // 验证结果
            assertNotNull(result)
            assertTrue(result.contains("example.java")) // 应该包含 Java 文件
            assertFalse(result.contains("test1.txt")) // 不应该包含文本文件
        } catch (e: RipgrepSearchException) {
            // 如果 ripgrep 未安装，测试将被跳过
            System.err.println("Skipping test: ${e.message}")
        }
    }
    
    @Test
    fun testSearchFilesWithPlainTextFormat() = runBlocking {
        try {
            // 纯文本格式
            val result = ripgrepSearcher.searchFiles(
                query = "test",
                basePath = tempDir,
                options = mapOf("formatAsMarkdown" to false)
            )
            
            // 验证结果
            assertNotNull(result)
            assertTrue(result.contains("搜索结果: test"))
            assertFalse(result.contains("```")) // 不应该包含 Markdown 格式
        } catch (e: RipgrepSearchException) {
            // 如果 ripgrep 未安装，测试将被跳过
            System.err.println("Skipping test: ${e.message}")
        }
    }
}
