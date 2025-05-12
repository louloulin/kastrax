package ai.kastrax.codebase.retrieval

import ai.kastrax.codebase.context.Context
import ai.kastrax.codebase.context.ContextBuilder
import ai.kastrax.codebase.context.ContextElement
import ai.kastrax.codebase.context.ContextLevel
import ai.kastrax.codebase.embedding.CodeEmbeddingService
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Visibility
import ai.kastrax.codebase.vector.CodeSearchResult
import ai.kastrax.codebase.vector.CodeVectorStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.nio.file.Path
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class ContextAwareRetrievalEngineTest {

    private lateinit var mockVectorStore: CodeVectorStore
    private lateinit var mockEmbeddingService: CodeEmbeddingService
    private lateinit var mockContextBuilder: ContextBuilder
    private lateinit var retrievalEngine: ContextAwareRetrievalEngine

    @BeforeEach
    fun setUp() {
        mockVectorStore = mock(CodeVectorStore::class.java)
        mockEmbeddingService = mock(CodeEmbeddingService::class.java)
        mockContextBuilder = mock(ContextBuilder::class.java)
        
        retrievalEngine = ContextAwareRetrievalEngine(
            vectorStore = mockVectorStore,
            embeddingService = mockEmbeddingService,
            contextBuilder = mockContextBuilder,
            config = ContextAwareRetrievalEngineConfig(
                engineType = RetrievalEngineType.CONTEXT_AWARE,
                maxContextSize = 5,
                enableEventNotifications = true,
                enableFeedbackLearning = true,
                enableExplanations = true,
                minScore = 0.7
            )
        )
    }

    @Test
    fun `test initialize`() = runBlocking {
        // 调用被测试方法
        retrievalEngine.initialize()
        
        // 验证事件通知
        val event = withTimeout(1.seconds) {
            retrievalEngine.events.first()
        }
        
        assertEquals(RetrievalEngineEventType.INITIALIZED, event.type)
    }

    @Test
    fun `test retrieve`() = runBlocking {
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
            CodeSearchResult(element1, 0.8),
            CodeSearchResult(element2, 0.7)
        )
        
        val context = Context(
            elements = listOf(
                ContextElement(
                    element = element1,
                    level = ContextLevel.METHOD,
                    relevance = 0.9f,
                    content = "void processData() {}"
                ),
                ContextElement(
                    element = element2,
                    level = ContextLevel.CLASS,
                    relevance = 0.8f,
                    content = "class DataProcessor {}"
                )
            ),
            query = query
        )
        
        // 设置模拟行为
        whenever(mockEmbeddingService.embed(any())).thenReturn(queryVector)
        whenever(mockVectorStore.similaritySearch(any(), any(), any(), any())).thenReturn(searchResults)
        whenever(mockContextBuilder.buildContext(any(), any(), any(), any(), any(), any())).thenReturn(context)
        
        // 调用被测试方法
        val results = retrievalEngine.retrieve(query)
        
        // 验证结果
        assertNotNull(results)
        assertEquals(2, results.size)
        assertEquals(element1.id, results[0].element.id)
        assertEquals(element2.id, results[1].element.id)
        
        // 验证事件通知
        val event = withTimeout(1.seconds) {
            retrievalEngine.events.first { it.type == RetrievalEngineEventType.QUERY_EXECUTED }
        }
        
        assertEquals(RetrievalEngineEventType.QUERY_EXECUTED, event.type)
        assertTrue(event.message.contains(query))
    }

    @Test
    fun `test provideFeedback`() = runBlocking {
        // 准备测试数据
        val elementId = "test-element-id"
        val score = 0.9
        val sessionId = "test-session"
        
        // 调用被测试方法
        val result = retrievalEngine.provideFeedback(elementId, score, sessionId)
        
        // 验证结果
        assertTrue(result)
        
        // 验证事件通知
        val event = withTimeout(1.seconds) {
            retrievalEngine.events.first { it.type == RetrievalEngineEventType.FEEDBACK_RECEIVED }
        }
        
        assertEquals(RetrievalEngineEventType.FEEDBACK_RECEIVED, event.type)
        assertTrue(event.message.contains(elementId))
        assertTrue(event.message.contains(score.toString()))
    }

    @Test
    fun `test clearSessionHistory`() {
        // 准备测试数据
        val sessionId = "test-session"
        
        // 调用被测试方法
        val result = retrievalEngine.clearSessionHistory(sessionId)
        
        // 验证结果
        assertTrue(result)
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
