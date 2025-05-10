package ai.kastrax.store.adapter

import ai.kastrax.store.embedding.EmbeddingService
import ai.kastrax.store.document.RagDocument
import ai.kastrax.store.model.SearchResult
import ai.kastrax.store.SimilarityMetric
import ai.kastrax.store.VectorStore
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class RagVectorStoreAdapterTest {

    private lateinit var mockVectorStore: VectorStore
    private lateinit var adapter: RagVectorStoreAdapter
    private lateinit var embeddingService: EmbeddingService
    private val indexName = "test_index"
    private val dimension = 3

    @BeforeEach
    fun setUp() {
        mockVectorStore = mock(VectorStore::class.java)
        adapter = RagVectorStoreAdapter(mockVectorStore, indexName, dimension)
        embeddingService = mock(EmbeddingService::class.java)

        runBlocking {
            `when`(mockVectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)).thenReturn(true)
        }
    }

    @Test
    fun `test add document`() = runBlocking {
        // 准备
        val document = "Test document"
        val embedding = floatArrayOf(1f, 0f, 0f)
        val metadata = mapOf("key" to "value")
        val id = "doc_1"

        `when`(mockVectorStore.upsert(
            indexName,
            listOf(embedding),
            listOf(mapOf("key" to "value")),
            null
        )).thenReturn(listOf(id))

        // 执行
        val result = adapter.addDocument(document, embedding, metadata)

        // 验证
        assertEquals(id, result)
        verify(mockVectorStore).upsert(
            indexName,
            listOf(embedding),
            listOf(mapOf("key" to "value")),
            null
        )

        // 验证文档是否已存储
        val retrievedDoc = adapter.getDocument(id)
        assertNotNull(retrievedDoc)
        assertEquals(document, retrievedDoc?.content)
        assertEquals(metadata, retrievedDoc?.metadata)
    }

    @Test
    fun `test add document with embedding service`() = runBlocking {
        // 准备
        val document = "Test document"
        val embedding = floatArrayOf(1f, 0f, 0f)
        val metadata = mapOf("key" to "value")
        val id = "doc_1"

        `when`(embeddingService.embed(document)).thenReturn(embedding)
        `when`(mockVectorStore.upsert(
            indexName,
            listOf(embedding),
            listOf(mapOf("key" to "value")),
            null
        )).thenReturn(listOf(id))

        // 执行
        val result = adapter.addDocument(document, embeddingService, metadata)

        // 验证
        assertEquals(id, result)
        verify(embeddingService).embed(document)
        verify(mockVectorStore).upsert(
            indexName,
            listOf(embedding),
            listOf(mapOf("key" to "value")),
            null
        )
    }

    @Test
    fun `test add documents`() = runBlocking {
        // 准备
        val documents = listOf("Doc 1", "Doc 2")
        val embeddings = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f)
        )
        val metadata = listOf(
            mapOf("key1" to "value1"),
            mapOf("key2" to "value2")
        )
        val ids = listOf("doc_1", "doc_2")

        `when`(mockVectorStore.batchUpsert(
            indexName,
            embeddings,
            listOf(
                mapOf("key1" to "value1"),
                mapOf("key2" to "value2")
            ),
            null,
            100
        )).thenReturn(ids)

        // 执行
        val result = adapter.addDocuments(documents, embeddings, metadata)

        // 验证
        assertEquals(ids, result)
        verify(mockVectorStore).batchUpsert(
            indexName,
            embeddings,
            listOf(
                mapOf("key1" to "value1"),
                mapOf("key2" to "value2")
            ),
            null,
            100
        )

        // 验证文档是否已存储
        val retrievedDoc1 = adapter.getDocument(ids[0])
        assertNotNull(retrievedDoc1)
        assertEquals(documents[0], retrievedDoc1?.content)
        assertEquals(metadata[0], retrievedDoc1?.metadata)

        val retrievedDoc2 = adapter.getDocument(ids[1])
        assertNotNull(retrievedDoc2)
        assertEquals(documents[1], retrievedDoc2?.content)
        assertEquals(metadata[1], retrievedDoc2?.metadata)
    }

    @Test
    fun `test similarity search`() = runBlocking {
        // 准备
        val query = "Test query"
        val queryEmbedding = floatArrayOf(0.5f, 0.5f, 0f)
        val limit = 2
        val minScore = 0.5

        `when`(embeddingService.embed(query)).thenReturn(queryEmbedding)

        val searchResults = listOf(
            SearchResult("doc_1", 0.8, null, mapOf("key1" to "value1")),
            SearchResult("doc_2", 0.6, null, mapOf("key2" to "value2")),
            SearchResult("doc_3", 0.4, null, mapOf("key3" to "value3"))
        )

        `when`(mockVectorStore.query(
            indexName,
            queryEmbedding,
            limit,
            null,
            false
        )).thenReturn(searchResults)

        // 添加文档到适配器
        val docs = listOf(
            RagDocument("doc_1", "Document 1", mapOf("key1" to "value1")),
            RagDocument("doc_2", "Document 2", mapOf("key2" to "value2")),
            RagDocument("doc_3", "Document 3", mapOf("key3" to "value3"))
        )

        // 使用反射设置文档
        val documentsField = RagVectorStoreAdapter::class.java.getDeclaredField("documents")
        documentsField.isAccessible = true
        val documents = documentsField.get(adapter) as MutableMap<String, RagDocument>
        docs.forEach { documents[it.id] = it }

        // 执行
        val results = adapter.similaritySearch(query, embeddingService, limit, minScore)

        // 验证
        assertEquals(2, results.size)
        assertEquals("doc_1", results[0].document.id)
        assertEquals(0.8, results[0].score)
        assertEquals("doc_2", results[1].document.id)
        assertEquals(0.6, results[1].score)
        verify(embeddingService).embed(query)
        verify(mockVectorStore).query(
            indexName,
            queryEmbedding,
            limit,
            null,
            false
        )
    }

    @Test
    fun `test delete document`() = runBlocking {
        // 准备
        val id = "doc_1"
        val document = RagDocument(id, "Document 1", mapOf("key" to "value"))

        // 使用反射设置文档
        val documentsField = RagVectorStoreAdapter::class.java.getDeclaredField("documentCache")
        documentsField.isAccessible = true
        val documents = documentsField.get(adapter) as MutableMap<String, RagDocument>
        documents[id] = document

        val contentToIdField = RagVectorStoreAdapter::class.java.getDeclaredField("contentToId")
        contentToIdField.isAccessible = true
        val contentToId = contentToIdField.get(adapter) as MutableMap<String, String>
        contentToId[document.content] = id

        `when`(mockVectorStore.deleteVectors(indexName, listOf(id))).thenReturn(true)

        // 执行
        val result = adapter.deleteDocument(id)

        // 验证
        assertTrue(result)
        verify(mockVectorStore).deleteVectors(indexName, listOf(id))
        assertNull(adapter.getDocument(id))
        assertNull(adapter.getDocumentByContent(document.content))
    }

    @Test
    fun `test clear`() = runBlocking {
        // 准备
        val docs = listOf(
            RagDocument("doc_1", "Document 1", mapOf("key1" to "value1")),
            RagDocument("doc_2", "Document 2", mapOf("key2" to "value2"))
        )

        // 使用反射设置文档
        val documentsField = RagVectorStoreAdapter::class.java.getDeclaredField("documentCache")
        documentsField.isAccessible = true
        val documents = documentsField.get(adapter) as MutableMap<String, RagDocument>
        docs.forEach { documents[it.id] = it }

        val contentToIdField = RagVectorStoreAdapter::class.java.getDeclaredField("contentToId")
        contentToIdField.isAccessible = true
        val contentToId = contentToIdField.get(adapter) as MutableMap<String, String>
        docs.forEach { contentToId[it.content] = it.id }

        `when`(mockVectorStore.deleteIndex(indexName)).thenReturn(true)
        `when`(mockVectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)).thenReturn(true)

        // 执行
        adapter.clear()

        // 验证
        verify(mockVectorStore).deleteIndex(indexName)
        verify(mockVectorStore, times(2)).createIndex(indexName, dimension, SimilarityMetric.COSINE)
        assertEquals(0, documents.size)
        assertEquals(0, contentToId.size)
    }
}
