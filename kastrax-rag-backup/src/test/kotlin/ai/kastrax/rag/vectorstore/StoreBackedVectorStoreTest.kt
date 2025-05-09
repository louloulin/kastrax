package ai.kastrax.rag.vectorstore

import ai.kastrax.rag.embedding.EmbeddingService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class StoreBackedVectorStoreTest {

    private lateinit var vectorStore: StoreBackedVectorStore
    private lateinit var embeddingService: EmbeddingService

    @BeforeEach
    fun setUp() {
        vectorStore = StoreBackedVectorStore(StoreType.MEMORY)
        embeddingService = mock(EmbeddingService::class.java)
    }

    @Test
    fun `test add document and similarity search`() = runBlocking {
        // 模拟嵌入服务
        `when`(embeddingService.embed("document1")).thenReturn(floatArrayOf(1f, 0f, 0f))
        `when`(embeddingService.embed("document2")).thenReturn(floatArrayOf(0f, 1f, 0f))
        `when`(embeddingService.embed("query")).thenReturn(floatArrayOf(0.9f, 0.1f, 0f))

        // 添加文档
        val id1 = vectorStore.addDocument("document1", embeddingService, mapOf("tag" to "doc1"))
        val id2 = vectorStore.addDocument("document2", embeddingService, mapOf("tag" to "doc2"))

        // 相似度搜索
        val results = vectorStore.similaritySearch("query", embeddingService, 2)
        assertEquals(2, results.size)
        assertEquals("document1", results[0].document.content)
        assertTrue(results[0].score > results[1].score)

        // 获取文档
        val doc = vectorStore.getDocument(id1)
        assertNotNull(doc)
        assertEquals("document1", doc?.content)
        assertEquals("doc1", doc?.metadata?.get("tag"))

        // 获取嵌入向量
        val embedding = vectorStore.getEmbedding(id1)
        assertNotNull(embedding)
        assertArrayEquals(floatArrayOf(1f, 0f, 0f), embedding)

        // 删除文档
        val deleteResult = vectorStore.deleteDocument(id1)
        assertTrue(deleteResult)

        // 再次获取文档应该返回 null
        val deletedDoc = vectorStore.getDocument(id1)
        assertNull(deletedDoc)
    }

    @Test
    fun `test add documents batch`() = runBlocking {
        // 模拟嵌入服务
        `when`(embeddingService.embed("document1")).thenReturn(floatArrayOf(1f, 0f, 0f))
        `when`(embeddingService.embed("document2")).thenReturn(floatArrayOf(0f, 1f, 0f))
        `when`(embeddingService.embedBatch(listOf("document1", "document2")))
            .thenReturn(listOf(floatArrayOf(1f, 0f, 0f), floatArrayOf(0f, 1f, 0f)))

        // 批量添加文档
        val docs = listOf("document1", "document2")
        val metadata = listOf(
            mapOf("tag" to "doc1"),
            mapOf("tag" to "doc2")
        )
        val ids = vectorStore.addDocuments(docs, embeddingService, metadata)
        assertEquals(2, ids.size)

        // 验证文档是否已添加
        val doc1 = vectorStore.getDocument(ids[0])
        assertNotNull(doc1)
        assertEquals("document1", doc1?.content)
        assertEquals("doc1", doc1?.metadata?.get("tag"))

        val doc2 = vectorStore.getDocument(ids[1])
        assertNotNull(doc2)
        assertEquals("document2", doc2?.content)
        assertEquals("doc2", doc2?.metadata?.get("tag"))
    }

    @Test
    fun `test keyword search`() = runBlocking {
        // 添加文档
        val embeddings = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, 1f)
        )
        val docs = listOf(
            "apple banana orange",
            "cat dog bird",
            "apple cat"
        )
        val metadata = List(3) { emptyMap<String, String>() }
        vectorStore.addDocuments(docs, embeddings, metadata)

        // 关键词搜索
        val results = vectorStore.keywordSearch(listOf("apple", "banana"), 10)
        assertEquals(2, results.size)
        assertEquals("apple banana orange", results[0].document.content)
        assertEquals("apple cat", results[1].document.content)
        assertTrue(results[0].score > results[1].score)
    }

    @Test
    fun `test metadata search`() = runBlocking {
        // 添加文档
        val embeddings = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, 1f)
        )
        val docs = listOf(
            "document1",
            "document2",
            "document3"
        )
        val metadata = listOf(
            mapOf("category" to "A", "tag" to "doc1"),
            mapOf("category" to "B", "tag" to "doc2"),
            mapOf("category" to "A", "tag" to "doc3")
        )
        vectorStore.addDocuments(docs, embeddings, metadata)

        // 元数据搜索
        val results = vectorStore.metadataSearch(mapOf("category" to "A"), 10)
        assertEquals(2, results.size)
        assertTrue(results.all { it.document.metadata["category"] == "A" })
    }

    @Test
    fun `test clear`() = runBlocking {
        // 添加文档
        val embeddings = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f)
        )
        val docs = listOf("document1", "document2")
        val metadata = List(2) { emptyMap<String, String>() }
        vectorStore.addDocuments(docs, embeddings, metadata)

        // 验证文档已添加
        assertEquals(2, vectorStore.size())

        // 清空向量存储
        vectorStore.clear()

        // 验证向量存储已清空
        assertEquals(0, vectorStore.size())
    }
}
