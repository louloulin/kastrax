package ai.kastrax.rag.retrieval

import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.vectorstore.RagVectorStore
import ai.kastrax.rag.vectorstore.StoreType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class RetrieverFactoryTest {

    private lateinit var mockVectorStore: RagVectorStore
    private lateinit var embeddingService: EmbeddingService

    @BeforeEach
    fun setUp() {
        mockVectorStore = mock(RagVectorStore::class.java)
        embeddingService = mock(EmbeddingService::class.java)
    }

    @Test
    fun `test create top-k retriever`() {
        val retriever = RetrieverFactory.createTopKRetriever(mockVectorStore, embeddingService)
        assertNotNull(retriever)
        assertTrue(retriever is TopKRetriever)
    }

    @Test
    fun `test create hybrid retriever`() {
        val retriever = RetrieverFactory.createHybridRetriever(mockVectorStore, embeddingService)
        assertNotNull(retriever)
        assertTrue(retriever is HybridRetriever)
    }

    @Test
    fun `test create vector store retriever`() {
        val retriever = RetrieverFactory.createVectorStoreRetriever(mockVectorStore, embeddingService)
        assertNotNull(retriever)
        assertTrue(retriever is VectorStoreRetriever)
    }

    @Test
    fun `test create with vector store`() {
        // 测试 TOP_K 类型
        val topKRetriever = RetrieverFactory.createWithVectorStore(
            mockVectorStore,
            embeddingService,
            RetrieverType.TOP_K
        )
        assertNotNull(topKRetriever)
        assertTrue(topKRetriever is TopKRetriever)

        // 测试 HYBRID 类型
        val hybridRetriever = RetrieverFactory.createWithVectorStore(
            mockVectorStore,
            embeddingService,
            RetrieverType.HYBRID
        )
        assertNotNull(hybridRetriever)
        assertTrue(hybridRetriever is HybridRetriever)

        // 测试 VECTOR_STORE 类型
        val vectorStoreRetriever = RetrieverFactory.createWithVectorStore(
            mockVectorStore,
            embeddingService,
            RetrieverType.VECTOR_STORE
        )
        assertNotNull(vectorStoreRetriever)
        assertTrue(vectorStoreRetriever is VectorStoreRetriever)
    }

    @Test
    fun `test create with store type`() {
        // 测试 MEMORY 类型
        val memoryRetriever = RetrieverFactory.createWithStoreType(
            StoreType.MEMORY,
            embeddingService,
            RetrieverType.TOP_K
        )
        assertNotNull(memoryRetriever)
        assertTrue(memoryRetriever is TopKRetriever)

        // 测试 CHROMA 类型
        val chromaRetriever = RetrieverFactory.createWithStoreType(
            StoreType.CHROMA,
            embeddingService,
            RetrieverType.HYBRID,
            mapOf("host" to "localhost", "port" to 8000)
        )
        assertNotNull(chromaRetriever)
        assertTrue(chromaRetriever is HybridRetriever)
    }
}
