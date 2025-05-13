package ai.kastrax.codebase.engine

import ai.kastrax.codebase.context.ContextBuilderConfig
import ai.kastrax.codebase.context.ContextLevel
import ai.kastrax.codebase.semantic.CodeSemanticAnalyzerConfig
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.parser.ChapiJavaCodeParser
import ai.kastrax.codebase.semantic.parser.CodeParserFactory
import ai.kastrax.store.VectorStore
import ai.kastrax.store.embedding.EmbeddingService
import ai.kastrax.store.memory.InMemoryVectorStore
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ContextEngineTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var vectorStore: VectorStore
    private lateinit var embeddingService: EmbeddingService
    private lateinit var contextEngine: ContextEngine

    @BeforeEach
    fun setUp() {
        // 注册代码解析器
        CodeParserFactory.registerParser(ChapiJavaCodeParser())

        // 创建向量存储
        vectorStore = InMemoryVectorStore()

        // 创建嵌入服务（模拟）
        embeddingService = mockk()
        coEvery { embeddingService.embed(any()) } returns FloatArray(1536) { 0.1f }
        coEvery { embeddingService.embedBatch(any()) } returns List(10) { FloatArray(1536) { 0.1f } }

        // 创建上下文引擎
        contextEngine = ContextEngineImpl.create(
            rootPath = tempDir,
            vectorStore = vectorStore,
            embeddingService = embeddingService,
            config = ContextEngineConfig(
                enableFileSystemMonitoring = false,
                enableGitMonitoring = false,
                contextBuilderConfig = ContextBuilderConfig(),
                semanticAnalyzerConfig = CodeSemanticAnalyzerConfig(
                    excludePatterns = setOf("**/*.class", "**/*.jar", "**/*.git/**"),
                    excludeDirectories = setOf("build", "target", "node_modules")
                )
            )
        )

        // 创建测试文件
        createTestFiles()
    }

    @AfterEach
    fun tearDown() = runBlocking {
        contextEngine.close()
    }

    @Test
    fun `test indexCodebase`() = runBlocking {
        // 索引代码库
        val result = contextEngine.indexCodebase(tempDir)

        // 验证结果
        assertTrue(result)

        // 获取状态
        val status = contextEngine.getStatus()
        assertNotNull(status)
        assertEquals(true, status["initialized"])
    }

    @Test
    fun `test getQueryContext`() = runBlocking {
        // 索引代码库
        contextEngine.indexCodebase(tempDir)

        // 获取查询上下文
        val context = contextEngine.getQueryContext("test", 5, 0.0)

        // 验证结果
        assertNotNull(context)
    }

    @Test
    fun `test getFileContext`() = runBlocking {
        // 索引代码库
        contextEngine.indexCodebase(tempDir)

        // 获取文件上下文
        val filePath = tempDir.resolve("Test.java")
        val context = contextEngine.getFileContext(filePath, 5)

        // 验证结果
        assertNotNull(context)
    }

    @Test
    fun `test getEditContext`() = runBlocking {
        // 索引代码库
        contextEngine.indexCodebase(tempDir)

        // 获取编辑上下文
        val filePath = tempDir.resolve("Test.java")
        val position = Location(
            filePath = filePath,
            startLine = 5,
            startColumn = 1,
            endLine = 5,
            endColumn = 10
        )
        val context = contextEngine.getEditContext(filePath, position, 5, 0.0)

        // 验证结果
        assertNotNull(context)
    }

    @Test
    fun `test getSymbolContext`() = runBlocking {
        // 索引代码库
        contextEngine.indexCodebase(tempDir)

        // 获取符号上下文
        val context = contextEngine.getSymbolContext("Test", 5, 0.0)

        // 验证结果
        assertNotNull(context)
    }

    private fun createTestFiles() {
        // 创建 Java 测试文件
        val javaFile = tempDir.resolve("Test.java")
        Files.writeString(javaFile, """
            public class Test {
                private String name;

                public Test(String name) {
                    this.name = name;
                }

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }
            }
        """.trimIndent())

        // 创建 Kotlin 测试文件
        val kotlinFile = tempDir.resolve("Test.kt")
        Files.writeString(kotlinFile, """
            data class Test(
                val name: String,
                val age: Int
            ) {
                fun greet(): String {
                    return "Hello, ${'$'}name!"
                }
            }
        """.trimIndent())
    }
}
