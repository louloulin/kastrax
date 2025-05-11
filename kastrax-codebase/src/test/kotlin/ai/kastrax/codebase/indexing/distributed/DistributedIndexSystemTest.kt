package ai.kastrax.codebase.indexing.distributed

import actor.proto.ActorSystem
import ai.kastrax.codebase.indexing.IndexTask
import ai.kastrax.codebase.indexing.IndexTaskType
import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.adapter.DocumentVectorStoreAdapter
import ai.kastrax.store.embedding.MockEmbeddingService
import kotlinx.coroutines.delay
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

class DistributedIndexSystemTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var actorSystem: ActorSystem
    private lateinit var embeddingService: MockEmbeddingService
    private lateinit var documentStore: DocumentVectorStoreAdapter
    private lateinit var indexSystem: DistributedIndexSystem
    
    @BeforeEach
    fun setUp() = runBlocking {
        // 创建测试文件
        createTestFiles()
        
        // 创建 Actor 系统
        actorSystem = ActorSystem("test-system")
        
        // 创建嵌入服务
        embeddingService = MockEmbeddingService()
        
        // 创建向量存储
        val vectorStore = VectorStoreFactory.createInMemoryVectorStore()
        
        // 创建文档向量存储适配器
        documentStore = DocumentVectorStoreAdapter(
            vectorStore = vectorStore,
            indexName = "test_index",
            dimension = embeddingService.dimension
        )
        
        // 创建分布式索引系统
        indexSystem = DistributedIndexSystemFactory.createDistributedIndexSystem(
            documentStore = documentStore,
            embeddingService = embeddingService,
            rootPath = tempDir,
            actorSystem = actorSystem,
            coordinatorConfig = IndexCoordinatorConfig(
                initialWorkerCount = 2, // 使用较少的工作者以便测试
                taskAssignmentInterval = kotlin.time.Duration.milliseconds(100) // 更快的任务分配以便测试
            )
        )
    }
    
    @AfterEach
    fun tearDown() = runBlocking {
        // 关闭分布式索引系统
        indexSystem.close()
        
        // 关闭 Actor 系统
        actorSystem.shutdown()
    }
    
    @Test
    fun `test submitting and processing tasks`() = runBlocking {
        // 创建测试任务
        val task1 = IndexTask(
            type = IndexTaskType.ADD,
            path = tempDir.resolve("TestClass.kt")
        )
        
        val task2 = IndexTask(
            type = IndexTaskType.ADD,
            path = tempDir.resolve("TestInterface.java")
        )
        
        // 收集事件
        val events = mutableListOf<IndexCoordinatorEvent>()
        val job = launch {
            withTimeout(5.seconds) {
                indexSystem.getEventFlow().take(6).toList(events) // 提交、分配和完成事件
            }
        }
        
        // 提交任务
        indexSystem.submitTask(task1)
        indexSystem.submitTask(task2)
        
        // 等待事件收集完成
        job.join()
        
        // 验证事件
        assertTrue(events.any { it is IndexCoordinatorEvent.TaskSubmitted && it.taskId == task1.id })
        assertTrue(events.any { it is IndexCoordinatorEvent.TaskSubmitted && it.taskId == task2.id })
        assertTrue(events.any { it is IndexCoordinatorEvent.TaskAssigned && it.taskId == task1.id })
        assertTrue(events.any { it is IndexCoordinatorEvent.TaskAssigned && it.taskId == task2.id })
        assertTrue(events.any { it is IndexCoordinatorEvent.TaskCompleted && it.taskId == task1.id })
        assertTrue(events.any { it is IndexCoordinatorEvent.TaskCompleted && it.taskId == task2.id })
        
        // 等待索引完成
        delay(1.seconds)
        
        // 验证文档是否已添加到向量存储
        val documents = documentStore.getAllDocuments()
        assertEquals(2, documents.size)
        assertTrue(documents.any { it.metadata["filename"] == "TestClass.kt" })
        assertTrue(documents.any { it.metadata["filename"] == "TestInterface.java" })
    }
    
    @Test
    fun `test batch submission`() = runBlocking {
        // 创建测试任务
        val tasks = listOf(
            IndexTask(
                type = IndexTaskType.ADD,
                path = tempDir.resolve("TestClass.kt")
            ),
            IndexTask(
                type = IndexTaskType.ADD,
                path = tempDir.resolve("TestInterface.java")
            ),
            IndexTask(
                type = IndexTaskType.ADD,
                path = tempDir.resolve("test_module.py")
            )
        )
        
        // 收集事件
        val events = mutableListOf<IndexCoordinatorEvent>()
        val job = launch {
            withTimeout(5.seconds) {
                indexSystem.getEventFlow().take(9).toList(events) // 提交、分配和完成事件
            }
        }
        
        // 批量提交任务
        val submittedCount = indexSystem.submitBatch(tasks)
        assertEquals(3, submittedCount)
        
        // 等待事件收集完成
        job.join()
        
        // 验证事件
        assertEquals(9, events.size)
        assertEquals(3, events.count { it is IndexCoordinatorEvent.TaskSubmitted })
        assertEquals(3, events.count { it is IndexCoordinatorEvent.TaskAssigned })
        assertEquals(3, events.count { it is IndexCoordinatorEvent.TaskCompleted })
        
        // 等待索引完成
        delay(1.seconds)
        
        // 验证文档是否已添加到向量存储
        val documents = documentStore.getAllDocuments()
        assertEquals(3, documents.size)
    }
    
    @Test
    fun `test system status`() = runBlocking {
        // 创建测试任务
        val tasks = listOf(
            IndexTask(
                type = IndexTaskType.ADD,
                path = tempDir.resolve("TestClass.kt")
            ),
            IndexTask(
                type = IndexTaskType.ADD,
                path = tempDir.resolve("TestInterface.java")
            )
        )
        
        // 批量提交任务
        indexSystem.submitBatch(tasks)
        
        // 获取系统状态
        val initialStatus = indexSystem.getStatus()
        assertTrue(initialStatus.pendingTaskCount > 0 || initialStatus.activeTaskCount > 0)
        assertEquals(2, initialStatus.workerCount)
        
        // 等待任务完成
        delay(3.seconds)
        
        // 再次获取系统状态
        val finalStatus = indexSystem.getStatus()
        assertEquals(0, finalStatus.pendingTaskCount)
        assertEquals(0, finalStatus.activeTaskCount)
        assertEquals(2, finalStatus.completedTaskCount)
        assertEquals(0, finalStatus.failedTaskCount)
    }
    
    @Test
    fun `test full reindex`() = runBlocking {
        // 创建完全重新索引任务
        val task = IndexTask(
            type = IndexTaskType.FULL_REINDEX,
            path = tempDir
        )
        
        // 提交任务
        indexSystem.submitTask(task)
        
        // 等待索引完成
        delay(5.seconds)
        
        // 验证文档是否已添加到向量存储
        val documents = documentStore.getAllDocuments()
        assertTrue(documents.size >= 5) // 至少应该有 5 个文档（我们创建了 5 个测试文件）
    }
    
    /**
     * 创建测试文件
     */
    private fun createTestFiles() {
        // 创建 Kotlin 文件
        val kotlinFilePath = tempDir.resolve("TestClass.kt")
        kotlinFilePath.writeText("""
            package ai.kastrax.codebase.test
            
            /**
             * 这是一个测试类，用于测试分布式索引系统。
             */
            class TestClass {
                /**
                 * 测试方法
                 */
                fun testMethod() {
                    println("This is a test method.")
                }
                
                /**
                 * 另一个测试方法
                 */
                fun anotherTestMethod() {
                    println("This is another test method.")
                }
            }
        """.trimIndent())
        
        // 创建 Java 文件
        val javaFilePath = tempDir.resolve("TestInterface.java")
        javaFilePath.writeText("""
            package ai.kastrax.codebase.test;
            
            /**
             * 这是一个测试接口，用于测试分布式索引系统。
             */
            public interface TestInterface {
                /**
                 * 测试方法
                 */
                void testMethod();
                
                /**
                 * 另一个测试方法
                 */
                void anotherTestMethod();
            }
        """.trimIndent())
        
        // 创建 Python 文件
        val pythonFilePath = tempDir.resolve("test_module.py")
        pythonFilePath.writeText("""
            """
            这是一个测试模块，用于测试分布式索引系统。
            """
            
            class TestClass:
                """测试类"""
                
                def test_method(self):
                    """测试方法"""
                    print("This is a test method.")
                
                def another_test_method(self):
                    """另一个测试方法"""
                    print("This is another test method.")
        """.trimIndent())
        
        // 创建 Markdown 文件
        val markdownFilePath = tempDir.resolve("README.md")
        markdownFilePath.writeText("""
            # 分布式索引系统测试
            
            这是一个测试文件，用于测试分布式索引系统。
            
            ## 功能
            
            - 分布式索引
            - Actor 模型
            - 任务调度
        """.trimIndent())
        
        // 创建目录结构
        val subDir = tempDir.resolve("subdir")
        Files.createDirectory(subDir)
        
        // 在子目录中创建文件
        val subDirFilePath = subDir.resolve("SubdirClass.kt")
        subDirFilePath.writeText("""
            package ai.kastrax.codebase.test.subdir
            
            /**
             * 这是一个子目录中的测试类，用于测试分布式索引系统。
             */
            class SubdirClass {
                /**
                 * 测试方法
                 */
                fun testMethod() {
                    println("This is a test method in a subdirectory.")
                }
            }
        """.trimIndent())
    }
}
