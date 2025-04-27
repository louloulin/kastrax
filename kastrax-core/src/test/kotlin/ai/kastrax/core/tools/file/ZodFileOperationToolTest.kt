package ai.kastrax.core.tools.file

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class ZodFileOperationToolTest {

    private val fileOperationTool = ZodFileOperationTool.createZodTool()
    
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
        val input = ZodFileOperationTool.FileOperationInput(
            operation = "read",
            path = testFile.absolutePath
        )

        val result = fileOperationTool.execute(input)

        assertTrue(result.success)
        assertEquals("测试内容", result.result)
    }

    @Test
    fun `test write operation`() = runBlocking {
        // 测试写入文件
        val newFile = File(tempDir.toFile(), "new.txt")
        val input = ZodFileOperationTool.FileOperationInput(
            operation = "write",
            path = newFile.absolutePath,
            content = "新内容"
        )

        val result = fileOperationTool.execute(input)

        assertTrue(result.success)
        assertTrue(newFile.exists())
        assertEquals("新内容", newFile.readText())
    }

    @Test
    fun `test exists operation`() = runBlocking {
        // 测试文件存在
        val input = ZodFileOperationTool.FileOperationInput(
            operation = "exists",
            path = testFile.absolutePath
        )

        val result = fileOperationTool.execute(input)

        assertTrue(result.success)
        assertEquals("true", result.result)
        
        // 测试文件不存在
        val nonExistentFile = File(tempDir.toFile(), "nonexistent.txt")
        val input2 = ZodFileOperationTool.FileOperationInput(
            operation = "exists",
            path = nonExistentFile.absolutePath
        )

        val result2 = fileOperationTool.execute(input2)

        assertTrue(result2.success)
        assertEquals("false", result2.result)
    }

    @Test
    fun `test info operation`() = runBlocking {
        // 测试获取文件信息
        val input = ZodFileOperationTool.FileOperationInput(
            operation = "info",
            path = testFile.absolutePath
        )

        val result = fileOperationTool.execute(input)

        assertTrue(result.success)
        assertNotNull(result.fileInfo)
        assertEquals("test.txt", result.fileInfo?.name)
        assertEquals(testFile.absolutePath, result.fileInfo?.path)
        assertEquals(true, result.fileInfo?.isFile)
        assertEquals(false, result.fileInfo?.isDirectory)
    }

    @Test
    fun `test list operation`() = runBlocking {
        // 测试列出目录内容
        val input = ZodFileOperationTool.FileOperationInput(
            operation = "list",
            path = testDir.absolutePath
        )

        val result = fileOperationTool.execute(input)

        assertTrue(result.success)
        assertNotNull(result.fileList)
        assertEquals(2, result.fileList?.size)
        
        // 验证文件名
        val fileNames = result.fileList?.map { it.name }?.toSet() ?: emptySet()
        assertTrue(fileNames.contains("file1.txt"))
        assertTrue(fileNames.contains("file2.txt"))
    }

    @Test
    fun `test mkdir operation`() = runBlocking {
        // 测试创建目录
        val newDir = File(tempDir.toFile(), "newDir")
        val input = ZodFileOperationTool.FileOperationInput(
            operation = "mkdir",
            path = newDir.absolutePath
        )

        val result = fileOperationTool.execute(input)

        assertTrue(result.success)
        assertTrue(newDir.exists())
        assertTrue(newDir.isDirectory)
    }

    @Test
    fun `test delete operation`() = runBlocking {
        // 测试删除文件
        val fileToDelete = File(tempDir.toFile(), "toDelete.txt")
        fileToDelete.writeText("要删除的内容")
        
        val input = ZodFileOperationTool.FileOperationInput(
            operation = "delete",
            path = fileToDelete.absolutePath
        )

        val result = fileOperationTool.execute(input)

        assertTrue(result.success)
        assertTrue(!fileToDelete.exists())
    }

    @Test
    fun `test copy operation`() = runBlocking {
        // 测试复制文件
        val destination = File(tempDir.toFile(), "copy.txt")
        val input = ZodFileOperationTool.FileOperationInput(
            operation = "copy",
            source = testFile.absolutePath,
            destination = destination.absolutePath
        )

        val result = fileOperationTool.execute(input)

        assertTrue(result.success)
        assertTrue(destination.exists())
        assertEquals("测试内容", destination.readText())
    }

    @Test
    fun `test move operation`() = runBlocking {
        // 测试移动文件
        val fileToMove = File(tempDir.toFile(), "toMove.txt")
        fileToMove.writeText("要移动的内容")
        
        val destination = File(tempDir.toFile(), "moved.txt")
        val input = ZodFileOperationTool.FileOperationInput(
            operation = "move",
            source = fileToMove.absolutePath,
            destination = destination.absolutePath
        )

        val result = fileOperationTool.execute(input)

        assertTrue(result.success)
        assertTrue(!fileToMove.exists())
        assertTrue(destination.exists())
        assertEquals("要移动的内容", destination.readText())
    }

    @Test
    fun `test invalid operation`() = runBlocking {
        // 测试无效操作
        val input = ZodFileOperationTool.FileOperationInput(
            operation = "invalid_operation"
        )

        val exception = assertThrows<IllegalArgumentException> {
            runBlocking {
                fileOperationTool.execute(input)
            }
        }
        
        assertTrue(exception.message?.contains("不支持的操作") ?: false)
    }

    @Test
    fun `test missing required parameters`() = runBlocking {
        // 测试缺少必需参数
        val input = ZodFileOperationTool.FileOperationInput(
            operation = "read"
        )

        val exception = assertThrows<IllegalArgumentException> {
            runBlocking {
                fileOperationTool.execute(input)
            }
        }
        
        assertTrue(exception.message?.contains("需要") ?: false)
    }
}
