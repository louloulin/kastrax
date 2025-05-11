package ai.kastrax.rag.codebase

import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.adapter.DocumentVectorStoreAdapter
import ai.kastrax.store.embedding.MockEmbeddingService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodebaseRagTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var codebaseRag: CodebaseRAG
    private lateinit var embeddingService: MockEmbeddingService
    private lateinit var documentStore: DocumentVectorStoreAdapter
    
    @BeforeEach
    fun setUp() = runBlocking {
        // 创建测试文件
        createTestFiles()
        
        // 创建嵌入服务
        embeddingService = MockEmbeddingService()
        
        // 创建向量存储
        val vectorStore = VectorStoreFactory.createInMemoryVectorStore()
        
        // 创建文档向量存储适配器
        documentStore = DocumentVectorStoreAdapter(
            vectorStore = vectorStore,
            indexName = "codebase_test",
            dimension = embeddingService.dimension
        )
        
        // 创建代码库 RAG 配置
        val config = CodebaseRagConfig(
            // 使用默认配置
        )
        
        // 创建代码库 RAG
        codebaseRag = CodebaseRAG.create(
            documentStore = documentStore,
            embeddingService = embeddingService,
            rootPath = tempDir,
            config = config
        )
        
        // 启动代码库 RAG
        codebaseRag.start()
    }
    
    @AfterEach
    fun tearDown() = runBlocking {
        // 停止代码库 RAG
        codebaseRag.stop()
    }
    
    @Test
    fun `test search functionality`() = runBlocking {
        // 等待索引完成
        Thread.sleep(2000)
        
        // 执行查询
        val query = "CodebaseRAG"
        val searchResults = codebaseRag.search(query, limit = 2)
        
        // 验证结果
        assertTrue(searchResults.isNotEmpty())
        assertTrue(searchResults[0].document.content.contains("CodebaseRAG"))
    }
    
    @Test
    fun `test context generation`() = runBlocking {
        // 等待索引完成
        Thread.sleep(2000)
        
        // 执行查询
        val query = "CodebaseRAG"
        val context = codebaseRag.generateContext(query, limit = 2)
        
        // 验证结果
        assertTrue(context.isNotEmpty())
        assertTrue(context.contains("CodebaseRAG"))
    }
    
    @Test
    fun `test reindex functionality`() = runBlocking {
        // 等待索引完成
        Thread.sleep(2000)
        
        // 添加新文件
        val newFilePath = tempDir.resolve("NewFile.kt")
        newFilePath.writeText("""
            /**
             * 这是一个新的测试文件，用于测试重新索引功能。
             */
            class NewTestClass {
                fun testMethod() {
                    println("This is a test method in a new file.")
                }
            }
        """.trimIndent())
        
        // 请求重新索引
        codebaseRag.requestReindex()
        
        // 等待重新索引完成
        Thread.sleep(2000)
        
        // 执行查询
        val query = "NewTestClass"
        val searchResults = codebaseRag.search(query, limit = 2)
        
        // 验证结果
        assertTrue(searchResults.isNotEmpty())
        assertTrue(searchResults[0].document.content.contains("NewTestClass"))
    }
    
    /**
     * 创建测试文件
     */
    private fun createTestFiles() {
        // 创建 Kotlin 文件
        val kotlinFilePath = tempDir.resolve("TestClass.kt")
        kotlinFilePath.writeText("""
            package ai.kastrax.rag.codebase
            
            /**
             * 这是一个测试类，用于测试 CodebaseRAG 功能。
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
            package ai.kastrax.rag.codebase;
            
            /**
             * 这是一个测试接口，用于测试 CodebaseRAG 功能。
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
            这是一个测试模块，用于测试 CodebaseRAG 功能。
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
            # CodebaseRAG 测试
            
            这是一个测试文件，用于测试 CodebaseRAG 功能。
            
            ## 功能
            
            - 代码库索引
            - 代码库搜索
            - 上下文生成
        """.trimIndent())
        
        // 创建目录结构
        val subDir = tempDir.resolve("subdir")
        Files.createDirectory(subDir)
        
        // 在子目录中创建文件
        val subDirFilePath = subDir.resolve("SubdirClass.kt")
        subDirFilePath.writeText("""
            package ai.kastrax.rag.codebase.subdir
            
            /**
             * 这是一个子目录中的测试类，用于测试 CodebaseRAG 功能。
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
