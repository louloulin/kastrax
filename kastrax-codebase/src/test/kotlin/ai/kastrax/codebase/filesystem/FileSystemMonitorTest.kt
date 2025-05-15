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
            refactoringTimeWindowMs = 1000, // 更短的重构检测窗口，用于测试
            adaptiveBatchSizing = true, // 启用自适应批处理大小
            minBatchSize = 2, // 最小批处理大小
            maxBatchSize = 20, // 最大批处理大小
            adaptiveThrottling = true, // 启用自适应节流
            minThrottleMs = 10, // 最小节流时间
            maxThrottleMs = 100, // 最大节流时间
            enableWatcherLoadBalancing = true, // 启用监控器负载均衡
            loadBalancingIntervalMs = 500, // 负载均衡间隔
            enableDirectoryPrioritization = true, // 启用目录优先级
            highPriorityDirectories = setOf("src", "lib", "app") // 高优先级目录
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

    @Test
    fun `test adaptive batch sizing`() = runBlocking {
        // 创建一个启用了自适应批处理的监控器
        val adaptiveConfig = FileSystemMonitorConfig(
            pollIntervalMs = 50,
            eventThrottleMs = 20,
            batchProcessingIntervalMs = 30,
            adaptiveBatchSizing = true,
            minBatchSize = 2,
            maxBatchSize = 50,
            batchSize = 10 // 初始批大小
        )
        val adaptiveMonitor = FileSystemMonitor(tempDir, adaptiveConfig)

        try {
            // 启动监控器
            adaptiveMonitor.start()

            // 收集事件
            val events = mutableListOf<FileChangeEvent>()
            val job = scope.launch {
                withTimeout(5.seconds) {
                    // 我们期望收到至少 30 个事件
                    adaptiveMonitor.fileChanges.take(30).toList(events)
                }
            }

            // 创建大量文件，触发自适应批处理
            val files = (1..100).map { i ->
                val file = tempDir.resolve("adaptive$i.txt")
                file.writeText("adaptive content $i")
                file
            }

            // 等待一段时间，让自适应算法有时间调整
            kotlinx.coroutines.delay(2000)

            // 等待事件收集完成
            job.join()

            // 验证收到了事件
            assertTrue(events.isNotEmpty())
            assertTrue(events.size >= 30, "应该收到至少 30 个事件，实际收到 ${events.size} 个")
        } finally {
            adaptiveMonitor.close()
        }
    }

    @Test
    fun `test directory prioritization`() = runBlocking {
        // 创建一个启用了目录优先级的监控器
        val priorityConfig = FileSystemMonitorConfig(
            pollIntervalMs = 50,
            eventThrottleMs = 20,
            enableDirectoryPrioritization = true,
            highPriorityDirectories = setOf("src", "important")
        )
        val priorityMonitor = FileSystemMonitor(tempDir, priorityConfig)

        try {
            // 创建高优先级目录和普通目录
            val highPriorityDir = tempDir.resolve("important")
            val lowPriorityDir = tempDir.resolve("less-important")
            Files.createDirectory(highPriorityDir)
            Files.createDirectory(lowPriorityDir)

            // 启动监控器
            priorityMonitor.start()

            // 收集事件
            val events = mutableListOf<FileChangeEvent>()
            val job = scope.launch {
                withTimeout(5.seconds) {
                    // 我们期望收到至少 4 个事件（2个目录 + 2个文件）
                    priorityMonitor.fileChanges.take(4).toList(events)
                }
            }

            // 同时在两个目录中创建文件
            val highPriorityFile = highPriorityDir.resolve("high.txt")
            val lowPriorityFile = lowPriorityDir.resolve("low.txt")
            highPriorityFile.writeText("high priority content")
            lowPriorityFile.writeText("low priority content")

            // 等待事件收集完成
            job.join()

            // 验证收到了事件
            assertTrue(events.isNotEmpty())

            // 验证事件中包含两个目录和两个文件
            val highDirEvent = events.find { it.path == highPriorityDir }
            val lowDirEvent = events.find { it.path == lowPriorityDir }
            val highFileEvent = events.find { it.path == highPriorityFile }
            val lowFileEvent = events.find { it.path == lowPriorityFile }

            assertTrue(highDirEvent != null, "应该检测到高优先级目录事件")
            assertTrue(lowDirEvent != null, "应该检测到低优先级目录事件")
            assertTrue(highFileEvent != null, "应该检测到高优先级文件事件")
            assertTrue(lowFileEvent != null, "应该检测到低优先级文件事件")
        } finally {
            priorityMonitor.close()
        }
    }

    @Test
    fun `test version control system integration`() = runBlocking {
        // 创建一个模拟 Git 仓库
        val gitDir = tempDir.resolve(".git")
        Files.createDirectory(gitDir)
        val headFile = gitDir.resolve("HEAD")
        headFile.writeText("ref: refs/heads/main")

        // 创建一个启用了 Git 集成的监控器
        val gitConfig = FileSystemMonitorConfig(
            pollIntervalMs = 50,
            eventThrottleMs = 20,
            enableGitIntegration = true,
            gitIntegrationIntervalMs = 100
        )
        val gitMonitor = FileSystemMonitor(tempDir, gitConfig)

        try {
            // 启动监控器
            gitMonitor.start()

            // 收集事件
            val events = mutableListOf<FileChangeEvent>()
            val job = scope.launch {
                withTimeout(5.seconds) {
                    // 我们期望收到至少 1 个事件（分支变更事件）
                    gitMonitor.fileChanges.take(1).toList(events)
                }
            }

            // 等待一下，让监控器初始化
            kotlinx.coroutines.delay(500)

            // 模拟分支切换（修改 HEAD 文件）
            headFile.writeText("ref: refs/heads/feature-branch")

            // 等待事件收集完成
            job.join()

            // 验证收到了事件
            assertTrue(events.isNotEmpty())

            // 验证收到了分支变更事件（根目录的 MODIFY 事件）
            val branchChangeEvent = events.find { it.path == tempDir && it.type == FileChangeType.MODIFY }
            assertTrue(branchChangeEvent != null, "应该检测到分支变更事件")
        } finally {
            gitMonitor.close()
        }
    }
}
