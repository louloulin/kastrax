package ai.kastrax.rag

import ai.kastrax.rag.document.DocumentLoader
import ai.kastrax.rag.document.DocumentSplitter
import ai.kastrax.rag.reranker.Reranker
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

class RAGTest {

    private lateinit var mockDocumentStore: DocumentVectorStore
    private lateinit var mockEmbeddingService: EmbeddingService
    private lateinit var mockReranker: Reranker
    private lateinit var rag: RAG

    @BeforeEach
    fun setUp() {
        mockDocumentStore = mockk(relaxed = true)
        mockEmbeddingService = mockk(relaxed = true)
        mockReranker = mockk(relaxed = true)

        rag = RAG(
            documentStore = mockDocumentStore,
            embeddingService = mockEmbeddingService,
            reranker = mockReranker
        )
    }

    @Test
    fun `test loadDocuments loads and stores documents correctly`() = runBlocking {
        // 准备测试数据
        val mockLoader = mockk<DocumentLoader>()
        val mockSplitter = mockk<DocumentSplitter>()

        val doc1 = Document("1", "This is a test document", mapOf("source" to "test"))
        val doc2 = Document("2", "Another test document", mapOf("source" to "test"))

        val loadedDocs = listOf(doc1, doc2)
        val splitDocs = listOf(
            Document("1-1", "This is a", mapOf("source" to "test", "parent_id" to "1")),
            Document("1-2", "test document", mapOf("source" to "test", "parent_id" to "1")),
            Document("2-1", "Another test", mapOf("source" to "test", "parent_id" to "2")),
            Document("2-2", "document", mapOf("source" to "test", "parent_id" to "2"))
        )

        // 设置模拟行为
        coEvery { mockLoader.load() } returns loadedDocs
        every { mockSplitter.split(doc1) } returns splitDocs.subList(0, 2)
        every { mockSplitter.split(doc2) } returns splitDocs.subList(2, 4)
        coEvery { mockDocumentStore.addDocuments(splitDocs, mockEmbeddingService) } returns true

        // 执行加载文档
        val count = rag.loadDocuments(mockLoader, mockSplitter)

        // 验证结果
        assertEquals(4, count)
    }

    @Test
    fun `test search returns expected results`() = runBlocking {
        // 准备测试数据
        val query = "test query"
        val doc1 = Document("1", "This is a test document", mapOf("source" to "test"))
        val doc2 = Document("2", "Another test document", mapOf("source" to "test"))

        val searchResults = listOf(
            DocumentSearchResult(doc1, 0.9),
            DocumentSearchResult(doc2, 0.8)
        )

        val rerankedResults = listOf(
            DocumentSearchResult(doc2, 0.85),
            DocumentSearchResult(doc1, 0.8)
        )

        // 设置模拟行为
        coEvery { mockDocumentStore.similaritySearch(query, mockEmbeddingService, 5) } returns searchResults
        coEvery { mockReranker.rerank(query, searchResults) } returns rerankedResults

        // 执行搜索
        val results = rag.search(query)

        // 验证结果
        assertEquals(2, results.size)
        assertEquals("2", results[0].document.id)
        assertEquals(0.85, results[0].score)
        assertEquals("1", results[1].document.id)
        assertEquals(0.8, results[1].score)
    }

    @Test
    fun `test generateContext returns expected context`() = runBlocking {
        // 准备测试数据
        val query = "test query"
        val doc1 = Document("1", "This is a test document", mapOf("source" to "test"))
        val doc2 = Document("2", "Another test document", mapOf("source" to "test"))

        val searchResults = listOf(
            DocumentSearchResult(doc1, 0.9),
            DocumentSearchResult(doc2, 0.8)
        )

        val rerankedResults = listOf(
            DocumentSearchResult(doc2, 0.85),
            DocumentSearchResult(doc1, 0.8)
        )

        val expectedContext = "Another test document\n\nThis is a test document"

        // 设置模拟行为
        coEvery { mockDocumentStore.similaritySearch(query, mockEmbeddingService, 5) } returns searchResults
        coEvery { mockReranker.rerank(query, searchResults) } returns rerankedResults

        // 执行生成上下文
        val context = rag.generateContext(query)

        // 验证结果
        assertEquals(expectedContext, context)
    }

    @Test
    fun `test retrieveContext returns expected result`() = runBlocking {
        // 准备测试数据
        val query = "test query"
        val doc1 = Document("1", "This is a test document", mapOf("source" to "test"))
        val doc2 = Document("2", "Another test document", mapOf("source" to "test"))

        val searchResults = listOf(
            DocumentSearchResult(doc1, 0.9),
            DocumentSearchResult(doc2, 0.8)
        )

        val rerankedResults = listOf(
            DocumentSearchResult(doc2, 0.85),
            DocumentSearchResult(doc1, 0.8)
        )

        val expectedContext = "Another test document\n\nThis is a test document"

        // 设置模拟行为
        coEvery { mockDocumentStore.similaritySearch(query, mockEmbeddingService, 5) } returns searchResults
        coEvery { mockReranker.rerank(query, searchResults) } returns rerankedResults

        // 执行检索上下文
        val result = rag.retrieveContext(query)

        // 验证结果
        assertEquals(expectedContext, result.context)
        assertEquals(2, result.documents.size)
        assertEquals("2", result.documents[0].id)
        assertEquals("1", result.documents[1].id)
    }
}
