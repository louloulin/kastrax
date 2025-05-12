package ai.kastrax.codebase.context

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Visibility
import ai.kastrax.codebase.vector.CodeVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.nio.file.Path
import java.util.UUID

class ContextBuilderTest {

    private lateinit var mockVectorStore: CodeVectorStore
    private lateinit var mockEmbeddingService: EmbeddingService
    private lateinit var contextBuilder: ContextBuilder

    @BeforeEach
    fun setUp() {
        mockVectorStore = mock(CodeVectorStore::class.java)
        mockEmbeddingService = mock(EmbeddingService::class.java)
        
        contextBuilder = ContextBuilder(
            vectorStore = mockVectorStore,
            embeddingService = mockEmbeddingService,
            config = ContextBuilderConfig(
                maxCacheSize = 10,
                defaultMaxElements = 5,
                defaultMinScore = 0.5f
            )
        )
    }

    @Test
    fun `test buildContext`() = runBlocking {
        // 准备测试数据
        val query = "find method to process data"
        val queryVector = FloatArray(1536) { 0.1f }
        
        val element1 = createTestElement(
            name = "processData",
            type = CodeElementType.METHOD
        )
        val element2 = createTestElement(
            name = "DataProcessor",
            type = CodeElementType.CLASS
        )
        
        val searchResults = listOf(
            ai.kastrax.codebase.vector.CodeSearchResult(element1, 0.8),
            ai.kastrax.codebase.vector.CodeSearchResult(element2, 0.7)
        )
        
        // 设置模拟行为
        whenever(mockEmbeddingService.embed(any())).thenReturn(queryVector)
        whenever(mockVectorStore.similaritySearch(any(), any(), any(), any())).thenReturn(searchResults)
        whenever(mockVectorStore.getElement(element1.id)).thenReturn(element1)
        whenever(mockVectorStore.getElement(element2.id)).thenReturn(element2)
        
        // 调用被测试方法
        val context = contextBuilder.buildContext(query)
        
        // 验证结果
        assertNotNull(context)
        assertEquals(query, context.query)
        assertEquals(2, context.elements.size)
        assertEquals(element1.id, context.elements[0].element.id)
        assertEquals(element2.id, context.elements[1].element.id)
    }

    @Test
    fun `test buildFileContext`() = runBlocking {
        // 准备测试数据
        val filePath = Path.of("TestFile.java")
        
        val element1 = createTestElement(
            name = "TestClass",
            type = CodeElementType.CLASS,
            filePath = filePath
        )
        val element2 = createTestElement(
            name = "testMethod",
            type = CodeElementType.METHOD,
            filePath = filePath
        )
        
        val fileElements = listOf(element1, element2)
        
        // 设置模拟行为
        whenever(mockVectorStore.getAllIds()).thenReturn(listOf(element1.id, element2.id))
        whenever(mockVectorStore.getElement(element1.id)).thenReturn(element1)
        whenever(mockVectorStore.getElement(element2.id)).thenReturn(element2)
        
        // 调用被测试方法
        val context = contextBuilder.buildFileContext(filePath)
        
        // 验证结果
        assertNotNull(context)
        assertEquals("file:$filePath", context.query)
        assertEquals(2, context.elements.size)
    }

    @Test
    fun `test mergeContexts`() {
        // 准备测试数据
        val element1 = createTestElement(
            name = "TestClass1",
            type = CodeElementType.CLASS
        )
        val element2 = createTestElement(
            name = "TestClass2",
            type = CodeElementType.CLASS
        )
        
        val context1 = Context(
            elements = listOf(
                ContextElement(
                    element = element1,
                    level = ContextLevel.CLASS,
                    relevance = 0.8f,
                    content = "class TestClass1 {}"
                )
            ),
            query = "query1"
        )
        
        val context2 = Context(
            elements = listOf(
                ContextElement(
                    element = element2,
                    level = ContextLevel.CLASS,
                    relevance = 0.7f,
                    content = "class TestClass2 {}"
                )
            ),
            query = "query2"
        )
        
        // 调用被测试方法
        val mergedContext = contextBuilder.mergeContexts(listOf(context1, context2))
        
        // 验证结果
        assertNotNull(mergedContext)
        assertEquals("query1; query2", mergedContext.query)
        assertEquals(2, mergedContext.elements.size)
        assertEquals(element1.id, mergedContext.elements[0].element.id)
        assertEquals(element2.id, mergedContext.elements[1].element.id)
    }

    private fun createTestElement(
        id: String = UUID.randomUUID().toString(),
        name: String = "TestElement",
        type: CodeElementType = CodeElementType.CLASS,
        filePath: Path = Path.of("$name.java")
    ): CodeElement {
        return CodeElement(
            id = id,
            name = name,
            qualifiedName = "com.example.$name",
            type = type,
            location = Location(
                filePath = filePath,
                startLine = 1,
                startColumn = 1,
                endLine = 10,
                endColumn = 1
            ),
            visibility = Visibility.PUBLIC,
            documentation = "This is a test element."
        )
    }
}
