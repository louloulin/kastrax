package ai.kastrax.ragx

import ai.kastrax.ragx.document.DocumentLoader
import ai.kastrax.ragx.document.DocumentSplitter
import ai.kastrax.ragx.retrieval.Retriever
import ai.kastrax.ragx.reranker.Reranker
import ai.kastrax.store.VectorStore
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.embedding.EmbeddingService
import ai.kastrax.store.model.SearchResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq

class RagXTest {

    private lateinit var mockVectorStore: VectorStore
    private lateinit var mockEmbeddingService: EmbeddingService
    private lateinit var mockRetriever: Retriever
    private lateinit var mockReranker: Reranker
    private lateinit var ragX: RagX

    @BeforeEach
    fun setUp() {
        mockVectorStore = mock(VectorStore::class.java)
        mockEmbeddingService = mock(EmbeddingService::class.java)
        mockRetriever = mock(Retriever::class.java)
        mockReranker = mock(Reranker::class.java)
        
        ragX = RagX(
            vectorStore = mockVectorStore,
            embeddingService = mockEmbeddingService,
            retriever = mockRetriever,
            reranker = mockReranker
        )
    }

    @Test
    fun `test search returns expected results`() = runBlocking {
        // 准备测试数据
        val query = "test query"
        val doc1 = Document("1", "This is a test document", mapOf("source" to "test"))
        val doc2 = Document("2", "Another test document", mapOf("source" to "test"))
        
        val retrievedResults = listOf(
            DocumentSearchResult(doc1, 0.9),
            DocumentSearchResult(doc2, 0.8)
        )
        
        val rerankedResults = listOf(
            DocumentSearchResult(doc2, 0.85),
            DocumentSearchResult(doc1, 0.8)
        )
        
        // 设置模拟行为
        `when`(mockRetriever.retrieve(eq(query), eq(5), eq(0.0))).thenReturn(retrievedResults)
        `when`(mockReranker.rerank(eq(query), eq(retrievedResults))).thenReturn(rerankedResults)
        
        // 执行搜索
        val results = ragX.search(query)
        
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
        
        val retrievedResults = listOf(
            DocumentSearchResult(doc1, 0.9),
            DocumentSearchResult(doc2, 0.8)
        )
        
        val rerankedResults = listOf(
            DocumentSearchResult(doc2, 0.85),
            DocumentSearchResult(doc1, 0.8)
        )
        
        // 设置模拟行为
        `when`(mockRetriever.retrieve(eq(query), eq(5), eq(0.0))).thenReturn(retrievedResults)
        `when`(mockReranker.rerank(eq(query), eq(retrievedResults))).thenReturn(rerankedResults)
        
        // 执行生成上下文
        val context = ragX.generateContext(query)
        
        // 验证结果
        val expectedContext = "Another test document\n\nThis is a test document"
        assertEquals(expectedContext, context)
    }

    @Test
    fun `test loadDocuments loads and stores documents correctly`() = runBlocking {
        // 准备测试数据
        val mockLoader = mock(DocumentLoader::class.java)
        val mockSplitter = mock(DocumentSplitter::class.java)
        
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
        `when`(mockLoader.load()).thenReturn(loadedDocs)
        `when`(mockSplitter.split(eq(doc1))).thenReturn(splitDocs.subList(0, 2))
        `when`(mockSplitter.split(eq(doc2))).thenReturn(splitDocs.subList(2, 4))
        
        val embeddings = listOf(
            floatArrayOf(0.1f, 0.2f),
            floatArrayOf(0.3f, 0.4f),
            floatArrayOf(0.5f, 0.6f),
            floatArrayOf(0.7f, 0.8f)
        )
        
        `when`(mockEmbeddingService.embedBatch(any())).thenReturn(embeddings)
        `when`(mockVectorStore.upsert(
            eq("default"),
            eq(embeddings),
            any(),
            eq(splitDocs.map { it.id })
        )).thenReturn(splitDocs.map { it.id })
        
        // 执行加载文档
        val count = ragX.loadDocuments(mockLoader, mockSplitter)
        
        // 验证结果
        assertEquals(4, count)
    }
}
