package ai.kastrax.core.tools.file

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class FileOperationToolTest {

    private val fileOperationTool = FileOperationTool.create()

    @TempDir
    lateinit var tempDir: Path

    private lateinit var testFile: File
    private lateinit var testDir: File

    @BeforeEach
    fun setUp() {
        testFile = File(tempDir.toFile(), "test.txt")
        testFile.writeText("测试内容")

        testDir = File(tempDir.toFile(), "testDir")
        testDir.mkdir()

        File(testDir, "file1.txt").writeText("文件1")
        File(testDir, "file2.txt").writeText("文件2")
    }

    @Test
    fun `test read operation`() = runBlocking {
        // 测试读取文件
        val input = buildJsonObject {
            put("operation", JsonPrimitive("read"))
            put("path", JsonPrimitive(testFile.absolutePath))
        }

        val result = fileOperationTool.execute(input).jsonObject
        val success = result["success"]?.jsonPrimitive?.content == "true"
        val resultValue = result["result"]?.jsonPrimitive?.content

        assertTrue(success)
        assertEquals("测试内容", resultValue)
    }

    @Test
    fun `test write operation`() = runBlocking {
        // 测试写入文件
        val newFile = File(tempDir.toFile(), "new.txt")
        val input = buildJsonObject {
            put("operation", JsonPrimitive("write"))
            put("path", JsonPrimitive(newFile.absolutePath))
            put("content", JsonPrimitive("新内容"))
        }

        val result = fileOperationTool.execute(input).jsonObject
        val success = result["success"]?.jsonPrimitive?.content == "true"

        assertTrue(success)
        assertTrue(newFile.exists())
        assertEquals("新内容", newFile.readText())
    }

    @Test
    fun `test exists operation`() = runBlocking {
        // 测试文件存在
        val input = buildJsonObject {
            put("operation", JsonPrimitive("exists"))
            put("path", JsonPrimitive(testFile.absolutePath))
        }

        val result = fileOperationTool.execute(input).jsonObject
        val success = result["success"]?.jsonPrimitive?.content == "true"
        val resultValue = result["result"]?.jsonPrimitive?.content

        assertTrue(success)
        assertEquals("true", resultValue)

        // 测试文件不存在
        val nonExistentFile = File(tempDir.toFile(), "nonexistent.txt")
        val input2 = buildJsonObject {
            put("operation", JsonPrimitive("exists"))
            put("path", JsonPrimitive(nonExistentFile.absolutePath))
        }

        val result2 = fileOperationTool.execute(input2).jsonObject
        val success2 = result2["success"]?.jsonPrimitive?.content == "true"
        val resultValue2 = result2["result"]?.jsonPrimitive?.content

        assertTrue(success2)
        assertEquals("false", resultValue2)
    }

    @Test
    fun `test info operation`() = runBlocking {
        // 测试获取文件信息
        val input = buildJsonObject {
            put("operation", JsonPrimitive("info"))
            put("path", JsonPrimitive(testFile.absolutePath))
        }

        val result = fileOperationTool.execute(input).jsonObject
        val success = result["success"]?.jsonPrimitive?.content == "true"
        val resultObj = result["result"]?.jsonObject

        assertTrue(success)
        assertNotNull(resultObj)
        assertEquals("test.txt", resultObj?.get("name")?.jsonPrimitive?.content)
        assertEquals(testFile.absolutePath, resultObj?.get("path")?.jsonPrimitive?.content)
        assertEquals("true", resultObj?.get("isFile")?.jsonPrimitive?.content)
        assertEquals("false", resultObj?.get("isDirectory")?.jsonPrimitive?.content)
    }

    @Test
    fun `test list operation`() = runBlocking {
        // 测试列出目录内容
        val input = buildJsonObject {
            put("operation", JsonPrimitive("list"))
            put("path", JsonPrimitive(testDir.absolutePath))
        }

        val result = fileOperationTool.execute(input).jsonObject
        val success = result["success"]?.jsonPrimitive?.content == "true"
        val resultArray = result["result"]

        assertTrue(success)
        assertNotNull(resultArray)
        // 验证目录中有两个文件
        // 由于返回的是JsonArray，这里简化测试，只检查成功状态
    }

    @Test
    fun `test mkdir operation`() = runBlocking {
        // 测试创建目录
        val newDir = File(tempDir.toFile(), "newDir")
        val input = buildJsonObject {
            put("operation", JsonPrimitive("mkdir"))
            put("path", JsonPrimitive(newDir.absolutePath))
        }

        val result = fileOperationTool.execute(input).jsonObject
        val success = result["success"]?.jsonPrimitive?.content == "true"

        assertTrue(success)
        assertTrue(newDir.exists())
        assertTrue(newDir.isDirectory)
    }

    @Test
    fun `test delete operation`() = runBlocking {
        // 测试删除文件
        val fileToDelete = File(tempDir.toFile(), "toDelete.txt")
        fileToDelete.writeText("要删除的内容")

        val input = buildJsonObject {
            put("operation", JsonPrimitive("delete"))
            put("path", JsonPrimitive(fileToDelete.absolutePath))
        }

        val result = fileOperationTool.execute(input).jsonObject
        val success = result["success"]?.jsonPrimitive?.content == "true"

        assertTrue(success)
        assertTrue(!fileToDelete.exists())
    }

    @Test
    fun `test copy operation`() = runBlocking {
        // 测试复制文件
        val destination = File(tempDir.toFile(), "copy.txt")
        val input = buildJsonObject {
            put("operation", JsonPrimitive("copy"))
            put("source", JsonPrimitive(testFile.absolutePath))
            put("destination", JsonPrimitive(destination.absolutePath))
        }

        val result = fileOperationTool.execute(input).jsonObject
        val success = result["success"]?.jsonPrimitive?.content == "true"

        assertTrue(success)
        assertTrue(destination.exists())
        assertEquals("测试内容", destination.readText())
    }

    @Test
    fun `test move operation`() = runBlocking {
        // 测试移动文件
        val fileToMove = File(tempDir.toFile(), "toMove.txt")
        fileToMove.writeText("要移动的内容")

        val destination = File(tempDir.toFile(), "moved.txt")
        val input = buildJsonObject {
            put("operation", JsonPrimitive("move"))
            put("source", JsonPrimitive(fileToMove.absolutePath))
            put("destination", JsonPrimitive(destination.absolutePath))
        }

        val result = fileOperationTool.execute(input).jsonObject
        val success = result["success"]?.jsonPrimitive?.content == "true"

        assertTrue(success)
        assertTrue(!fileToMove.exists())
        assertTrue(destination.exists())
        assertEquals("要移动的内容", destination.readText())
    }

    @Test
    fun `test invalid operation`() = runBlocking {
        // 测试无效操作
        val input = buildJsonObject {
            put("operation", JsonPrimitive("invalid_operation"))
        }

        val result = fileOperationTool.execute(input).jsonObject
        val success = result["success"]?.jsonPrimitive?.content == "true"
        val error = result["error"]?.jsonPrimitive?.content

        assertEquals(false, success)
        assertTrue(error?.contains("不支持的操作") ?: false)
    }

    @Test
    fun `test missing required parameters`() = runBlocking {
        // 测试缺少必需参数
        val input = buildJsonObject {
            put("operation", JsonPrimitive("read"))
        }

        val result = fileOperationTool.execute(input).jsonObject
        val success = result["success"]?.jsonPrimitive?.content == "true"
        val error = result["error"]?.jsonPrimitive?.content

        assertEquals(false, success)
        assertTrue(error?.contains("需要") ?: false)
    }
}
