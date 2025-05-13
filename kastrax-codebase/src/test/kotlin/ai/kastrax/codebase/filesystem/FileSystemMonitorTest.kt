package ai.kastrax.codebase.filesystem

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    private lateinit var config: FileSystemMonitorConfig
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @BeforeEach
    fun setUp() {
        // 创建文件系统监控器配置
        config = FileSystemMonitorConfig(
            pollIntervalMs = 100, // 更快的轮询间隔，用于测试
            eventThrottleMs = 50, // 更短的节流时间，用于测试
            batchProcessingIntervalMs = 50, // 更短的批处理间隔，用于测试
            refactoringThreshold = 5, // 更小的重构阈值，用于测试
            refactoringTimeWindowMs = 1000 // 更短的重构检测窗口，用于测试
        )
        monitor = FileSystemMonitor(tempDir, config)
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

    @Test
    fun `test batch processing`() = runBlocking {
        // 启动监控器
        monitor.start()

        // 收集事件
        val events = mutableListOf<FileChangeEvent>()
        val job = scope.launch {
            withTimeout(5.seconds) {
                // 我们期望收到至少一个事件（可能会收到多个，因为批处理可能会分多次发送）
                monitor.fileChanges.take(1).toList(events)
            }
        }

        // 创建多个文件
        val files = (1..10).map { i ->
            val file = tempDir.resolve("test$i.txt")
            file.writeText("content $i")
            file
        }

        // 等待事件收集完成
        job.join()

        // 验证至少收到了一个事件
        assertTrue(events.isNotEmpty())
        // 验证事件类型是 CREATE
        assertEquals(FileChangeType.CREATE, events[0].type)
        // 验证事件路径是我们创建的文件之一
        assertTrue(files.contains(events[0].path))
    }

    @Test
    fun `test refactoring detection`() = runBlocking {
        // 启动监控器
        monitor.start()

        // 收集事件，包括可能的重构事件
        val events = mutableListOf<FileChangeEvent>()
        val job = scope.launch {
            withTimeout(5.seconds) {
                // 我们期望收到至少 refactoringThreshold 个事件，外加一个重构事件
                monitor.fileChanges.take(config.refactoringThreshold + 1).toList(events)
            }
        }

        // 快速创建多个文件，模拟重构
        val files = (1..config.refactoringThreshold).map { i ->
            val file = tempDir.resolve("refactor$i.txt")
            file.writeText("refactored content $i")
            file
        }

        // 等待事件收集完成
        job.join()

        // 验证收到了足够多的事件
        assertTrue(events.size >= config.refactoringThreshold)

        // 检查是否有重构事件（根路径的 MODIFY 事件）
        val refactoringEvent = events.find { it.path == tempDir && it.type == FileChangeType.MODIFY }
        assertTrue(refactoringEvent != null, "应该检测到重构事件")
    }
}
