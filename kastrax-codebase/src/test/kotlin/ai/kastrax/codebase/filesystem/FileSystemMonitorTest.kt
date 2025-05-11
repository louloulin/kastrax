package ai.kastrax.codebase.filesystem

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class FileSystemMonitorTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var monitor: FileSystemMonitor
    
    @BeforeEach
    fun setUp() {
        // 创建文件系统监控器
        monitor = FileSystemMonitor(tempDir)
    }
    
    @AfterEach
    fun tearDown() {
        // 关闭监控器
        monitor.close()
    }
    
    @Test
    fun `test file creation detection`() = runBlocking {
        // 启动监控器
        monitor.start()
        
        // 收集事件
        val events = mutableListOf<FileChangeEvent>()
        val job = launch {
            withTimeout(5.seconds) {
                monitor.fileChanges.take(1).toList(events)
            }
        }
        
        // 创建文件
        val testFile = tempDir.resolve("test.txt")
        testFile.writeText("Hello, World!")
        
        // 等待事件收集完成
        job.join()
        
        // 验证事件
        assertEquals(1, events.size)
        assertEquals(FileChangeType.CREATE, events[0].type)
        assertEquals(testFile, events[0].path)
    }
    
    @Test
    fun `test file modification detection`() = runBlocking {
        // 创建文件
        val testFile = tempDir.resolve("test.txt")
        testFile.writeText("Initial content")
        
        // 启动监控器
        monitor.start()
        
        // 收集事件
        val events = mutableListOf<FileChangeEvent>()
        val job = launch {
            withTimeout(5.seconds) {
                monitor.fileChanges.take(1).toList(events)
            }
        }
        
        // 修改文件
        testFile.writeText("Modified content")
        
        // 等待事件收集完成
        job.join()
        
        // 验证事件
        assertEquals(1, events.size)
        assertEquals(FileChangeType.MODIFY, events[0].type)
        assertEquals(testFile, events[0].path)
    }
    
    @Test
    fun `test file deletion detection`() = runBlocking {
        // 创建文件
        val testFile = tempDir.resolve("test.txt")
        testFile.writeText("Content to delete")
        
        // 启动监控器
        monitor.start()
        
        // 收集事件
        val events = mutableListOf<FileChangeEvent>()
        val job = launch {
            withTimeout(5.seconds) {
                monitor.fileChanges.take(1).toList(events)
            }
        }
        
        // 删除文件
        Files.delete(testFile)
        
        // 等待事件收集完成
        job.join()
        
        // 验证事件
        assertEquals(1, events.size)
        assertEquals(FileChangeType.DELETE, events[0].type)
        assertEquals(testFile, events[0].path)
    }
    
    @Test
    fun `test directory creation and monitoring`() = runBlocking {
        // 启动监控器
        monitor.start()
        
        // 收集事件
        val events = mutableListOf<FileChangeEvent>()
        val job = launch {
            withTimeout(5.seconds) {
                monitor.fileChanges.take(2).toList(events)
            }
        }
        
        // 创建目录
        val testDir = tempDir.resolve("testdir")
        Files.createDirectory(testDir)
        
        // 在新目录中创建文件
        val testFile = testDir.resolve("test.txt")
        testFile.writeText("Hello from new directory!")
        
        // 等待事件收集完成
        job.join()
        
        // 验证事件
        assertEquals(2, events.size)
        assertTrue(events.any { it.path == testFile && it.type == FileChangeType.CREATE })
    }
    
    @Test
    fun `test file filter exclusion`() = runBlocking {
        // 创建带有排除配置的监控器
        val config = FileSystemMonitorConfig(
            excludeExtensions = setOf("bin", "tmp")
        )
        val filteredMonitor = FileSystemMonitor(tempDir, config)
        
        try {
            // 启动监控器
            filteredMonitor.start()
            
            // 收集事件
            val events = mutableListOf<FileChangeEvent>()
            val job = launch {
                withTimeout(5.seconds) {
                    filteredMonitor.fileChanges.take(1).toList(events)
                }
            }
            
            // 创建排除的文件
            val excludedFile = tempDir.resolve("test.bin")
            excludedFile.writeText("Binary content")
            
            // 创建包含的文件
            val includedFile = tempDir.resolve("test.txt")
            includedFile.writeText("Text content")
            
            // 等待事件收集完成
            job.join()
            
            // 验证事件 - 应该只有包含的文件
            assertEquals(1, events.size)
            assertEquals(includedFile, events[0].path)
        } finally {
            filteredMonitor.close()
        }
    }
}
