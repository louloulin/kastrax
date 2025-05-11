package ai.kastrax.codebase.indexing

import ai.kastrax.codebase.filesystem.FileChangeEvent
import ai.kastrax.codebase.filesystem.FileChangeType
import ai.kastrax.codebase.filesystem.FileFilter
import ai.kastrax.codebase.git.GitBranchChangeEvent
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class IncrementalIndexerTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var indexer: IncrementalIndexer
    
    @BeforeEach
    fun setUp() {
        // 创建增量索引器
        indexer = IncrementalIndexer(
            config = IncrementalIndexerConfig(batchSize = 2), // 小批量以便测试
            fileFilter = FileFilter()
        )
    }
    
    @Test
    fun `test processing file change events`() = runBlocking {
        // 创建文件变更事件
        val file1 = tempDir.resolve("file1.txt")
        val file2 = tempDir.resolve("file2.txt")
        val file3 = tempDir.resolve("file3.txt")
        
        val event1 = FileChangeEvent(file1, FileChangeType.CREATE)
        val event2 = FileChangeEvent(file2, FileChangeType.MODIFY)
        val event3 = FileChangeEvent(file3, FileChangeType.DELETE)
        
        // 收集索引任务
        val tasks = mutableListOf<List<IndexTask>>()
        val job = launch {
            withTimeout(5.seconds) {
                indexer.indexTasks.take(2).toList(tasks) // 应该生成 2 个批次
            }
        }
        
        // 处理文件变更事件
        indexer.processFileChange(event1)
        indexer.processFileChange(event2)
        indexer.processFileChange(event3)
        
        // 等待任务收集完成
        job.join()
        
        // 验证任务
        assertEquals(2, tasks.size)
        
        // 第一个批次应该有 2 个任务
        assertEquals(2, tasks[0].size)
        
        // 第二个批次应该有 1 个任务
        assertEquals(1, tasks[1].size)
        
        // 验证任务类型
        val allTasks = tasks.flatten()
        assertTrue(allTasks.any { it.type == IndexTaskType.ADD && it.path == file1 })
        assertTrue(allTasks.any { it.type == IndexTaskType.UPDATE && it.path == file2 })
        assertTrue(allTasks.any { it.type == IndexTaskType.DELETE && it.path == file3 })
    }
    
    @Test
    fun `test processing branch change events`() = runBlocking {
        // 创建分支变更事件
        val event = GitBranchChangeEvent(
            repositoryPath = tempDir,
            previousBranch = "main",
            currentBranch = "feature"
        )
        
        // 收集索引任务
        val tasks = mutableListOf<List<IndexTask>>()
        val job = launch {
            withTimeout(5.seconds) {
                indexer.indexTasks.take(1).toList(tasks)
            }
        }
        
        // 处理分支变更事件
        indexer.processBranchChange(event)
        
        // 等待任务收集完成
        job.join()
        
        // 验证任务
        assertEquals(1, tasks.size)
        assertEquals(1, tasks[0].size)
        assertEquals(IndexTaskType.BRANCH_CHANGE, tasks[0][0].type)
        assertEquals(tempDir, tasks[0][0].path)
        assertEquals("main", tasks[0][0].metadata["previousBranch"])
        assertEquals("feature", tasks[0][0].metadata["currentBranch"])
    }
    
    @Test
    fun `test requesting full reindex`() = runBlocking {
        // 收集索引任务
        val tasks = mutableListOf<List<IndexTask>>()
        val job = launch {
            withTimeout(5.seconds) {
                indexer.indexTasks.take(1).toList(tasks)
            }
        }
        
        // 请求完全重新索引
        indexer.requestFullReindex(tempDir)
        
        // 等待任务收集完成
        job.join()
        
        // 验证任务
        assertEquals(1, tasks.size)
        assertEquals(1, tasks[0].size)
        assertEquals(IndexTaskType.FULL_REINDEX, tasks[0][0].type)
        assertEquals(tempDir, tasks[0][0].path)
    }
    
    @Test
    fun `test task deduplication`() = runBlocking {
        // 创建相同文件的变更事件
        val file = tempDir.resolve("file.txt")
        val event1 = FileChangeEvent(file, FileChangeType.MODIFY, System.currentTimeMillis())
        val event2 = FileChangeEvent(file, FileChangeType.MODIFY, System.currentTimeMillis() + 10)
        
        // 收集索引任务
        val tasks = mutableListOf<List<IndexTask>>()
        val job = launch {
            withTimeout(5.seconds) {
                indexer.indexTasks.take(1).toList(tasks)
            }
        }
        
        // 处理文件变更事件
        indexer.processFileChange(event1)
        indexer.processFileChange(event2) // 这个应该被去重
        
        // 手动处理批处理
        indexer.processBatch()
        
        // 等待任务收集完成
        job.join()
        
        // 验证任务 - 应该只有一个任务
        assertEquals(1, tasks.size)
        assertEquals(1, tasks[0].size)
        assertEquals(IndexTaskType.UPDATE, tasks[0][0].type)
        assertEquals(file, tasks[0][0].path)
    }
    
    @Test
    fun `test task priority`() = runBlocking {
        // 创建不同优先级的任务
        val file1 = tempDir.resolve("file1.txt")
        val file2 = tempDir.resolve("file2.txt")
        
        // 收集索引任务
        val tasks = mutableListOf<List<IndexTask>>()
        val job = launch {
            withTimeout(5.seconds) {
                indexer.indexTasks.take(1).toList(tasks)
            }
        }
        
        // 请求完全重新索引（低优先级）
        indexer.requestFullReindex(tempDir)
        
        // 处理分支变更（高优先级）
        indexer.processBranchChange(
            GitBranchChangeEvent(
                repositoryPath = tempDir,
                previousBranch = "main",
                currentBranch = "feature"
            )
        )
        
        // 手动处理批处理
        indexer.processBatch()
        
        // 等待任务收集完成
        job.join()
        
        // 验证任务 - 分支变更应该在前面（高优先级）
        assertEquals(1, tasks.size)
        assertTrue(tasks[0].size >= 2)
        assertEquals(IndexTaskType.BRANCH_CHANGE, tasks[0][0].type)
    }
}
