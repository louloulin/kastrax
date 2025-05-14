package ai.kastrax.codebase.context

import ai.kastrax.codebase.flow.CodeFlowAnalyzerAdapter
import ai.kastrax.codebase.semantic.flow.CodeFlowAnalyzerConfig
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Visibility
import ai.kastrax.codebase.vector.CodeSearchResult
import ai.kastrax.codebase.vector.CodeVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths

class FlowAwareContextBuilderTest {

    private lateinit var vectorStore: CodeVectorStore
    private lateinit var embeddingService: EmbeddingService
    private lateinit var flowAnalyzer: CodeFlowAnalyzerAdapter
    private lateinit var contextBuilder: FlowAwareContextBuilder

    @BeforeEach
    fun setUp() {
        // 创建模拟对象
        vectorStore = mockk()
        embeddingService = mockk()
        flowAnalyzer = mockk()

        // 配置模拟对象行为
        every { vectorStore.getAllIds() } returns listOf("test-method", "test-class")
        every { vectorStore.getElement("test-method") } returns createTestMethodElement()
        every { vectorStore.getElement("test-class") } returns createTestClassElement()

        coEvery { embeddingService.embed(any()) } returns FloatArray(128) { 0.1f }

        coEvery { vectorStore.similaritySearch(any(), any(), any(), any()) } returns listOf(
            CodeSearchResult(
                element = createTestMethodElement(),
                score = 0.9
            ),
            CodeSearchResult(
                element = createTestClassElement(),
                score = 0.8
            )
        )

        coEvery { flowAnalyzer.analyzeControlFlow(any()) } returns null
        coEvery { flowAnalyzer.analyzeDataFlow(any()) } returns null

        // 创建上下文构建器
        contextBuilder = FlowAwareContextBuilder(
            vectorStore = vectorStore,
            embeddingService = embeddingService,
            flowAnalyzer = flowAnalyzer,
            config = FlowAwareContextBuilderConfig(
                includeControlFlow = true,
                includeDataFlow = true
            )
        )
    }

    @Test
    fun `test build context`() = runBlocking {
        // 构建上下文
        val context = contextBuilder.buildContext(
            query = "test method",
            maxElements = 5,
            minScore = 0.5
        )

        // 验证结果
        assertNotNull(context)
        assertTrue(context.elements.isNotEmpty())
        assertEquals("test method", context.query)
        assertTrue(context.metadata.containsKey("includeControlFlow"))
        assertTrue(context.metadata.containsKey("includeDataFlow"))
    }

    @Test
    fun `test build file context`() = runBlocking {
        // 配置模拟对象行为
        every { vectorStore.getAllIds() } returns listOf("test-method", "test-class")
        every { vectorStore.getElement(any()) } returns createTestMethodElement()

        // 构建文件上下文
        val context = contextBuilder.buildFileContext(
            filePath = Paths.get("src/test/resources/TestClass.java"),
            maxElements = 5
        )

        // 验证结果
        assertNotNull(context)
        assertTrue(context.elements.isNotEmpty())
        assertTrue(context.query.startsWith("file:"))
        assertTrue(context.metadata.containsKey("includeControlFlow"))
        assertTrue(context.metadata.containsKey("includeDataFlow"))
    }

    @Test
    fun `test build symbol context`() = runBlocking {
        // 构建符号上下文
        val context = contextBuilder.buildSymbolContext(
            symbolName = "testMethod",
            maxElements = 5
        )

        // 验证结果
        assertNotNull(context)
        assertTrue(context.elements.isNotEmpty())
        assertTrue(context.query.startsWith("symbol:"))
        assertTrue(context.metadata.containsKey("includeControlFlow"))
        assertTrue(context.metadata.containsKey("includeDataFlow"))
    }

    /**
     * 创建测试方法元素
     *
     * @return 方法元素
     */
    private fun createTestMethodElement(): CodeElement {
        val classElement = createTestClassElement()

        val methodElement = CodeElement(
            id = "test-method",
            name = "testMethod",
            qualifiedName = "com.example.TestClass.testMethod",
            type = CodeElementType.METHOD,
            location = Location(
                filePath = Paths.get("src/test/resources/TestClass.java").toString(),
                startLine = 10,
                startColumn = 1,
                endLine = 20,
                endColumn = 1
            ),
            visibility = Visibility.PUBLIC,
            parent = classElement
        )

        // 添加方法体
        methodElement.metadata["body"] = """
            public void testMethod(int a, String b) {
                if (a > 0) {
                    System.out.println("Positive: " + a);
                } else {
                    System.out.println("Non-positive: " + a);
                }

                for (int i = 0; i < a; i++) {
                    System.out.println("Loop: " + i);
                }

                try {
                    int result = a / 0;
                } catch (Exception e) {
                    System.out.println("Exception: " + e.getMessage());
                }

                return;
            }
        """.trimIndent()

        return methodElement
    }

    /**
     * 创建测试类元素
     *
     * @return 类元素
     */
    private fun createTestClassElement(): CodeElement {
        return CodeElement(
            id = "test-class",
            name = "TestClass",
            qualifiedName = "com.example.TestClass",
            type = CodeElementType.CLASS,
            location = Location(
                filePath = Paths.get("src/test/resources/TestClass.java").toString(),
                startLine = 1,
                startColumn = 1,
                endLine = 30,
                endColumn = 1
            ),
            visibility = Visibility.PUBLIC
        )
    }
}
