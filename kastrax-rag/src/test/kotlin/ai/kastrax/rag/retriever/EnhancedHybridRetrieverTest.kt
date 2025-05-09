package ai.kastrax.rag.retriever

import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EnhancedHybridRetrieverTest {

    private lateinit var mockVectorRetriever: Retriever
    private lateinit var mockKeywordRetriever: Retriever
    private lateinit var mockMetadataRetriever: Retriever
    private lateinit var hybridRetriever: EnhancedHybridRetriever
    
    @BeforeEach
    fun setUp() {
        mockVectorRetriever = mockk()
        mockKeywordRetriever = mockk()
        mockMetadataRetriever = mockk()
        
        hybridRetriever = EnhancedHybridRetriever(
            vectorRetriever = mockVectorRetriever,
            keywordRetriever = mockKeywordRetriever,
            metadataRetriever = mockMetadataRetriever,
            config = EnhancedHybridRetrieverConfig(
                vectorWeight = 0.6,
                keywordWeight = 0.3,
                metadataWeight = 0.1,
                hybridStrategy = HybridStrategy.WEIGHTED_AVERAGE
            )
        )
    }
    
    @Test
    fun `test retrieve with weighted average strategy`() = runBlocking {
        // Create test documents
        val doc1 = Document("1", "Document 1", mapOf("source" to "test"))
        val doc2 = Document("2", "Document 2", mapOf("source" to "test"))
        val doc3 = Document("3", "Document 3", mapOf("source" to "test"))
        
        // Create test results
        val vectorResults = listOf(
            DocumentSearchResult(doc1, 0.9),
            DocumentSearchResult(doc2, 0.8)
        )
        
        val keywordResults = listOf(
            DocumentSearchResult(doc2, 0.7),
            DocumentSearchResult(doc3, 0.6)
        )
        
        val metadataResults = listOf(
            DocumentSearchResult(doc1, 0.5),
            DocumentSearchResult(doc3, 0.4)
        )
        
        // Mock retrievers
        coEvery { mockVectorRetriever.retrieve("test query", 10, 0.0) } returns vectorResults
        coEvery { mockKeywordRetriever.retrieve("test query", 10, 0.0) } returns keywordResults
        coEvery { mockMetadataRetriever.retrieve("test query", 10, 0.0) } returns metadataResults
        
        // Retrieve documents
        val results = hybridRetriever.retrieve("test query", 5, 0.0)
        
        // Verify results
        assertEquals(3, results.size)
        
        // Calculate expected scores
        val doc1Score = (0.9 * 0.6) + (0.0 * 0.3) + (0.5 * 0.1) // 0.59
        val doc2Score = (0.8 * 0.6) + (0.7 * 0.3) + (0.0 * 0.1) // 0.69
        val doc3Score = (0.0 * 0.6) + (0.6 * 0.3) + (0.4 * 0.1) // 0.22
        
        // Verify order (by score)
        assertEquals("2", results[0].document.id) // doc2 has highest score
        assertEquals("1", results[1].document.id) // doc1 has second highest score
        assertEquals("3", results[2].document.id) // doc3 has lowest score
        
        // Verify scores (with small delta for floating point comparison)
        assertEquals(doc2Score, results[0].score, 0.01)
        assertEquals(doc1Score, results[1].score, 0.01)
        assertEquals(doc3Score, results[2].score, 0.01)
    }
    
    @Test
    fun `test retrieve with max strategy`() = runBlocking {
        // Create test documents
        val doc1 = Document("1", "Document 1", mapOf("source" to "test"))
        val doc2 = Document("2", "Document 2", mapOf("source" to "test"))
        
        // Create test results
        val vectorResults = listOf(
            DocumentSearchResult(doc1, 0.9),
            DocumentSearchResult(doc2, 0.8)
        )
        
        val keywordResults = listOf(
            DocumentSearchResult(doc1, 0.7),
            DocumentSearchResult(doc2, 0.6)
        )
        
        // Mock retrievers
        coEvery { mockVectorRetriever.retrieve("test query", 10, 0.0) } returns vectorResults
        coEvery { mockKeywordRetriever.retrieve("test query", 10, 0.0) } returns keywordResults
        coEvery { mockMetadataRetriever.retrieve("test query", 10, 0.0) } returns emptyList()
        
        // Create retriever with MAX strategy
        val maxRetriever = EnhancedHybridRetriever(
            vectorRetriever = mockVectorRetriever,
            keywordRetriever = mockKeywordRetriever,
            metadataRetriever = null,
            config = EnhancedHybridRetrieverConfig(
                vectorWeight = 0.6,
                keywordWeight = 0.4,
                metadataWeight = 0.0,
                hybridStrategy = HybridStrategy.MAX
            )
        )
        
        // Retrieve documents
        val results = maxRetriever.retrieve("test query", 5, 0.0)
        
        // Verify results
        assertEquals(2, results.size)
        
        // Calculate expected scores
        val doc1VectorScore = 0.9 * 0.6 // 0.54
        val doc1KeywordScore = 0.7 * 0.4 // 0.28
        val doc1MaxScore = Math.max(doc1VectorScore, doc1KeywordScore) // 0.54
        
        val doc2VectorScore = 0.8 * 0.6 // 0.48
        val doc2KeywordScore = 0.6 * 0.4 // 0.24
        val doc2MaxScore = Math.max(doc2VectorScore, doc2KeywordScore) // 0.48
        
        // Verify order (by score)
        assertEquals("1", results[0].document.id) // doc1 has highest max score
        assertEquals("2", results[1].document.id) // doc2 has second highest max score
        
        // Verify scores (with small delta for floating point comparison)
        assertEquals(doc1MaxScore, results[0].score, 0.01)
        assertEquals(doc2MaxScore, results[1].score, 0.01)
    }
}
