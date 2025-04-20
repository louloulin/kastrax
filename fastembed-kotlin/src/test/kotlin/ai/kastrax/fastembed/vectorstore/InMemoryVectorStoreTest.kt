package ai.kastrax.fastembed.vectorstore

import ai.kastrax.fastembed.Embedding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryVectorStoreTest {
    
    private fun createEmbedding(values: FloatArray): Embedding {
        return Embedding(values)
    }
    
    @Test
    fun testAddAndRetrieveItem() {
        val store = InMemoryVectorStore(3)
        val embedding = createEmbedding(floatArrayOf(1f, 2f, 3f))
        val metadata = mapOf("text" to "Hello, world!")
        
        // Add item
        assertTrue(store.addItem("doc1", embedding, metadata))
        
        // Check count
        assertEquals(1, store.count())
        
        // Check exists
        assertTrue(store.exists("doc1"))
        
        // Get item
        val document = store.get("doc1")
        assertNotNull(document)
        assertEquals("doc1", document.id)
        assertEquals(embedding.vector.toList(), document.embedding.vector.toList())
        assertEquals(metadata, document.metadata)
    }
    
    @Test
    fun testSearch() {
        val store = InMemoryVectorStore(3)
        
        // Add items
        store.addItem("doc1", createEmbedding(floatArrayOf(1f, 0f, 0f)), mapOf("text" to "Document 1"))
        store.addItem("doc2", createEmbedding(floatArrayOf(0.5f, 0.5f, 0f)), mapOf("text" to "Document 2"))
        store.addItem("doc3", createEmbedding(floatArrayOf(0f, 1f, 0f)), mapOf("text" to "Document 3"))
        
        // Search
        val query = createEmbedding(floatArrayOf(0.7f, 0.3f, 0f))
        val results = store.search(query, topK = 2)
        
        // Check results
        assertEquals(2, results.size)
        assertEquals("doc1", results[0].id)  // Most similar to doc1
        assertEquals("doc2", results[1].id)  // Then doc2
        
        // Check scores
        assertTrue(results[0].score > results[1].score)
        
        // Check metadata
        assertEquals("Document 1", results[0].metadata["text"])
    }
    
    @Test
    fun testDelete() {
        val store = InMemoryVectorStore(3)
        store.addItem("doc1", createEmbedding(floatArrayOf(1f, 0f, 0f)))
        
        // Delete item
        assertTrue(store.delete("doc1"))
        
        // Check count
        assertEquals(0, store.count())
        
        // Check exists
        assertFalse(store.exists("doc1"))
        
        // Get item
        assertNull(store.get("doc1"))
    }
    
    @Test
    fun testClear() {
        val store = InMemoryVectorStore(3)
        store.addItem("doc1", createEmbedding(floatArrayOf(1f, 0f, 0f)))
        store.addItem("doc2", createEmbedding(floatArrayOf(0f, 1f, 0f)))
        
        // Clear store
        assertTrue(store.clear())
        
        // Check count
        assertEquals(0, store.count())
    }
    
    @Test
    fun testGetAllIds() {
        val store = InMemoryVectorStore(3)
        store.addItem("doc1", createEmbedding(floatArrayOf(1f, 0f, 0f)))
        store.addItem("doc2", createEmbedding(floatArrayOf(0f, 1f, 0f)))
        
        // Get all IDs
        val ids = store.getAllIds()
        
        // Check IDs
        assertEquals(2, ids.size)
        assertTrue(ids.contains("doc1"))
        assertTrue(ids.contains("doc2"))
    }
}
