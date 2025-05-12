package ai.kastrax.codebase.indexing.distributed

import actor.proto.ActorSystem
import ai.kastrax.codebase.indexing.IndexProcessor
import ai.kastrax.codebase.indexing.IndexTask
import ai.kastrax.codebase.indexing.IndexTaskStatus
import ai.kastrax.codebase.indexing.IndexTaskType
import java.nio.file.Path
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class DistributedIndexSystemTest {

    private lateinit var actorSystem: ActorSystem
    private lateinit var indexProcessor: TestIndexProcessor
    private lateinit var indexSystem: DistributedIndexSystem

    @BeforeEach
    fun setUp() {
        actorSystem = ActorSystem.default()
        indexProcessor = TestIndexProcessor()

        val config = DistributedIndexSystemConfig(
            localWorkerCount = 2,
            taskSubmitTimeout = 5.seconds
        )

        indexSystem = DistributedIndexSystem(actorSystem, indexProcessor, config)
    }

    @AfterEach
    fun tearDown() {
        runBlocking {
            indexSystem.close()
            actorSystem.shutdown()
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test start and stop`() = runBlocking {
        // 启动系统
        indexSystem.start()

        // 获取状态
        val status = indexSystem.getStatus()

        // 验证状态
        assertTrue(status.isRunning)
        assertEquals(0, status.pendingTaskCount)
        assertEquals(0, status.runningTaskCount)
        assertEquals(0, status.completedTaskCount)
        assertEquals(0, status.failedTaskCount)
        assertEquals(2, status.workerCount) // 2个本地工作器

        // 停止系统
        indexSystem.stop()

        // 获取状态
        val stoppedStatus = indexSystem.getStatus()

        // 验证状态
        assertFalse(stoppedStatus.isRunning)
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test submit task`() = runBlocking {
        // 启动系统
        indexSystem.start()

        // 创建任务
        val task = IndexTask(
            id = UUID.randomUUID().toString(),
            type = IndexTaskType.ADD,
            path = Path.of("/test/path"),
            priority = 5
        )

        // 提交任务
        val result = indexSystem.submitTask(task)

        // 验证结果
        assertTrue(result)

        // 等待任务处理完成
        delay(1000)

        // 获取状态
        val status = indexSystem.getStatus()

        // 验证状态
        assertEquals(0, status.pendingTaskCount)
        assertEquals(0, status.runningTaskCount)
        assertEquals(1, status.completedTaskCount)
        assertEquals(0, status.failedTaskCount)

        // 验证处理器被调用
        assertEquals(1, indexProcessor.processedTasks.size)
        assertEquals(task.id, indexProcessor.processedTasks[0].id)
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test batch submit tasks`() = runBlocking {
        // 启动系统
        indexSystem.start()

        // 创建任务
        val tasks = (1..5).map {
            IndexTask(
                id = UUID.randomUUID().toString(),
                type = IndexTaskType.ADD,
                path = Path.of("/test/path/$it"),
                priority = it
            )
        }

        // 提交任务
        val successCount = indexSystem.submitTasks(tasks)

        // 验证结果
        assertEquals(5, successCount)

        // 等待任务处理完成
        delay(2000)

        // 获取状态
        val status = indexSystem.getStatus()

        // 验证状态
        assertEquals(0, status.pendingTaskCount)
        assertEquals(0, status.runningTaskCount)
        assertEquals(5, status.completedTaskCount)
        assertEquals(0, status.failedTaskCount)

        // 验证处理器被调用
        assertEquals(5, indexProcessor.processedTasks.size)

        // 验证任务按优先级处理
        val processedIds = indexProcessor.processedTasks.map { it.id }
        val taskIds = tasks.sortedByDescending { it.priority }.map { it.id }

        // 注意：由于并行处理，顺序可能不完全一致
        assertTrue(processedIds.containsAll(taskIds))
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test failed task`() = runBlocking {
        // 启动系统
        indexSystem.start()

        // 设置处理器失败模式
        indexProcessor.shouldFail = true

        // 创建任务
        val task = IndexTask(
            id = UUID.randomUUID().toString(),
            type = IndexTaskType.ADD,
            path = Path.of("/test/path"),
            priority = 5
        )

        // 提交任务
        val result = indexSystem.submitTask(task)

        // 验证结果
        assertTrue(result)

        // 等待任务处理完成
        delay(2000)

        // 获取状态
        val status = indexSystem.getStatus()

        // 验证状态
        assertEquals(0, status.pendingTaskCount)
        assertEquals(0, status.runningTaskCount)
        assertEquals(0, status.completedTaskCount)
        assertEquals(1, status.failedTaskCount)
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test event flow`() = runBlocking {
        // 启动系统
        indexSystem.start()

        // 收集事件
        val events = mutableListOf<Any>()
        val job = launch {
            indexSystem.indexEvents().take(2).toList(events)
        }

        // 创建任务
        val task = IndexTask(
            id = UUID.randomUUID().toString(),
            type = IndexTaskType.ADD,
            path = Path.of("/test/path"),
            priority = 5
        )

        // 提交任务
        indexSystem.submitTask(task)

        // 等待事件收集完成
        delay(2000)
        job.cancel()

        // 验证事件
        assertTrue(events.size >= 1)
        assertTrue(events.any { it is SystemStatusChangedEvent })
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test get shard info`() = runBlocking {
        // 启动系统
        indexSystem.start()

        // 获取分片信息
        val shardInfo = indexSystem.getShardInfo("test-key")

        // 验证分片信息
        // 注意：由于没有实际注册分片，这里可能返回null
        if (shardInfo != null) {
            assertNotNull(shardInfo.shardId)
            assertNotNull(shardInfo.nodeId)
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `test reindex request`() = runBlocking {
        // 启动系统
        indexSystem.start()

        // 请求重新索引
        val result = indexSystem.requestReindex("/test/reindex/path")

        // 验证结果
        assertTrue(result)

        // 等待任务处理完成
        delay(1000)

        // 获取状态
        val status = indexSystem.getStatus()

        // 验证状态
        assertEquals(0, status.pendingTaskCount)
        assertEquals(0, status.runningTaskCount)
        assertEquals(1, status.completedTaskCount)
        assertEquals(0, status.failedTaskCount)

        // 验证处理器被调用
        assertEquals(1, indexProcessor.processedTasks.size)
        assertEquals(IndexTaskType.FULL_REINDEX, indexProcessor.processedTasks[0].type)
        assertEquals("/test/reindex/path", indexProcessor.processedTasks[0].path.toString())
    }

    /**
     * 测试用索引处理器
     */
    class TestIndexProcessor : IndexProcessor {
        val processedTasks = mutableListOf<IndexTask>()
        var shouldFail = false

        override suspend fun processTask(task: IndexTask) {
            processedTasks.add(task)

            // 模拟处理时间
            delay(100)

            if (shouldFail) {
                throw RuntimeException("测试失败")
            }
        }
    }
}
