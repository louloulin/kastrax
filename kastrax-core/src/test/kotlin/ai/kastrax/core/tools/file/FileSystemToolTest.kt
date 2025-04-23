package ai.kastrax.core.tools.file

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FileSystemToolTest {

    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var fileSystemTool: FileSystemTool
    
    @BeforeEach
    fun setUp() {
        fileSystemTool = FileSystemTool(
            rootPath = tempDir.toString(),
            allowAbsolutePaths = false
        )
    }
    
    @AfterEach
    fun tearDown() {
        // 清理临时文件
        Files.list(tempDir).forEach { Files.deleteIfExists(it) }
    }
    
    @Test
    fun `test write and read file`() = runBlocking {
        // 写入文件
        val writeInput = buildJsonObject {
            put("action", "write")
            put("path", "test.txt")
            put("content", "Hello, KastraX!")
        }
        
        val writeResult = fileSystemTool.execute(writeInput)
        assertTrue(writeResult is JsonObject)
        val writeResultObj = writeResult as JsonObject
        
        assertTrue(writeResultObj["success"]?.jsonPrimitive?.content?.toBoolean() ?: false)
        
        // 读取文件
        val readInput = buildJsonObject {
            put("action", "read")
            put("path", "test.txt")
        }
        
        val readResult = fileSystemTool.execute(readInput)
        assertTrue(readResult is JsonObject)
        val readResultObj = readResult as JsonObject
        
        assertTrue(readResultObj["success"]?.jsonPrimitive?.content?.toBoolean() ?: false)
        assertEquals("Hello, KastraX!", readResultObj["content"]?.jsonPrimitive?.content)
    }
    
    @Test
    fun `test append file`() = runBlocking {
        // 写入文件
        val writeInput = buildJsonObject {
            put("action", "write")
            put("path", "append.txt")
            put("content", "First line.")
        }
        
        fileSystemTool.execute(writeInput)
        
        // 追加文件
        val appendInput = buildJsonObject {
            put("action", "append")
            put("path", "append.txt")
            put("content", "Second line.")
        }
        
        val appendResult = fileSystemTool.execute(appendInput)
        assertTrue(appendResult is JsonObject)
        val appendResultObj = appendResult as JsonObject
        
        assertTrue(appendResultObj["success"]?.jsonPrimitive?.content?.toBoolean() ?: false)
        
        // 读取文件
        val readInput = buildJsonObject {
            put("action", "read")
            put("path", "append.txt")
        }
        
        val readResult = fileSystemTool.execute(readInput)
        assertTrue(readResult is JsonObject)
        val readResultObj = readResult as JsonObject
        
        assertEquals("First line.Second line.", readResultObj["content"]?.jsonPrimitive?.content)
    }
    
    @Test
    fun `test list directory`() = runBlocking {
        // 创建一些文件
        Files.writeString(tempDir.resolve("file1.txt"), "Content 1")
        Files.writeString(tempDir.resolve("file2.txt"), "Content 2")
        Files.createDirectory(tempDir.resolve("subdir"))
        
        // 列出目录
        val listInput = buildJsonObject {
            put("action", "list")
            put("path", ".")
        }
        
        val listResult = fileSystemTool.execute(listInput)
        assertTrue(listResult is JsonObject)
        val listResultObj = listResult as JsonObject
        
        assertTrue(listResultObj["success"]?.jsonPrimitive?.content?.toBoolean() ?: false)
        
        val files = listResultObj["files"] as JsonArray
        assertEquals(3, files.size)
        
        // 验证文件列表包含我们创建的文件和目录
        val fileNames = files.map { it.jsonObject["name"]?.jsonPrimitive?.content }
        assertTrue(fileNames.contains("file1.txt"))
        assertTrue(fileNames.contains("file2.txt"))
        assertTrue(fileNames.contains("subdir"))
        
        // 验证目录标志
        val subdirEntry = files.first { it.jsonObject["name"]?.jsonPrimitive?.content == "subdir" }
        assertTrue(subdirEntry.jsonObject["isDirectory"]?.jsonPrimitive?.content?.toBoolean() ?: false)
    }
    
    @Test
    fun `test check exists`() = runBlocking {
        // 创建文件
        Files.writeString(tempDir.resolve("exists.txt"), "I exist")
        
        // 检查存在的文件
        val existsInput = buildJsonObject {
            put("action", "exists")
            put("path", "exists.txt")
        }
        
        val existsResult = fileSystemTool.execute(existsInput)
        assertTrue(existsResult is JsonObject)
        val existsResultObj = existsResult as JsonObject
        
        assertTrue(existsResultObj["success"]?.jsonPrimitive?.content?.toBoolean() ?: false)
        assertTrue(existsResultObj["exists"]?.jsonPrimitive?.content?.toBoolean() ?: false)
        assertFalse(existsResultObj["isDirectory"]?.jsonPrimitive?.content?.toBoolean() ?: true)
        
        // 检查不存在的文件
        val notExistsInput = buildJsonObject {
            put("action", "exists")
            put("path", "not-exists.txt")
        }
        
        val notExistsResult = fileSystemTool.execute(notExistsInput)
        assertTrue(notExistsResult is JsonObject)
        val notExistsResultObj = notExistsResult as JsonObject
        
        assertTrue(notExistsResultObj["success"]?.jsonPrimitive?.content?.toBoolean() ?: false)
        assertFalse(notExistsResultObj["exists"]?.jsonPrimitive?.content?.toBoolean() ?: true)
    }
    
    @Test
    fun `test delete file`() = runBlocking {
        // 创建文件
        Files.writeString(tempDir.resolve("to-delete.txt"), "Delete me")
        
        // 删除文件
        val deleteInput = buildJsonObject {
            put("action", "delete")
            put("path", "to-delete.txt")
        }
        
        val deleteResult = fileSystemTool.execute(deleteInput)
        assertTrue(deleteResult is JsonObject)
        val deleteResultObj = deleteResult as JsonObject
        
        assertTrue(deleteResultObj["success"]?.jsonPrimitive?.content?.toBoolean() ?: false)
        assertTrue(deleteResultObj["deleted"]?.jsonPrimitive?.content?.toBoolean() ?: false)
        
        // 验证文件已被删除
        assertFalse(Files.exists(tempDir.resolve("to-delete.txt")))
    }
}
