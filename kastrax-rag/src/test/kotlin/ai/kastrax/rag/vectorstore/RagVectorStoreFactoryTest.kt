package ai.kastrax.rag.vectorstore

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RagVectorStoreFactoryTest {

    @Test
    fun `test create in-memory vector store`() {
        val vectorStore = RagVectorStoreFactory.createInMemoryVectorStore()
        assertNotNull(vectorStore)
        assertTrue(vectorStore is InMemoryVectorStore)
    }

    @Test
    fun `test create store-backed vector store with memory type`() {
        val vectorStore = RagVectorStoreFactory.createStoreBackedVectorStore(
            storeType = StoreType.MEMORY,
            indexName = "test_index",
            dimension = 128
        )
        assertNotNull(vectorStore)
        assertTrue(vectorStore is StoreBackedVectorStore)
    }

    @Test
    fun `test create chroma vector store`() {
        val vectorStore = RagVectorStoreFactory.createChromaVectorStore(
            host = "localhost",
            port = 8000,
            indexName = "test_index",
            dimension = 128
        )
        assertNotNull(vectorStore)
        assertTrue(vectorStore is StoreBackedVectorStore)
    }

    @Test
    fun `test create qdrant vector store`() {
        val vectorStore = RagVectorStoreFactory.createQdrantVectorStore(
            host = "localhost",
            port = 6333,
            indexName = "test_index",
            dimension = 128
        )
        assertNotNull(vectorStore)
        assertTrue(vectorStore is StoreBackedVectorStore)
    }

    @Test
    fun `test create postgres vector store`() {
        val vectorStore = RagVectorStoreFactory.createPostgresVectorStore(
            jdbcUrl = "jdbc:postgresql://localhost:5432/test",
            username = "test",
            password = "test",
            indexName = "test_index",
            dimension = 128
        )
        assertNotNull(vectorStore)
        assertTrue(vectorStore is StoreBackedVectorStore)
    }

    @Test
    fun `test create pinecone vector store`() {
        val vectorStore = RagVectorStoreFactory.createPineconeVectorStore(
            apiKey = "test-api-key",
            environment = "test-env",
            projectId = "test-project",
            indexName = "test_index",
            dimension = 128
        )
        assertNotNull(vectorStore)
        assertTrue(vectorStore is StoreBackedVectorStore)
    }
}
