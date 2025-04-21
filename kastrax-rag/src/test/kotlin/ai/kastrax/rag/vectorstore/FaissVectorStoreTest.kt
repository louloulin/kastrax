package ai.kastrax.rag.vectorstore

import ai.kastrax.rag.document.Document
import ai.kastrax.rag.embedding.Embedding
import ai.kastrax.rag.embedding.EmbeddedDocument
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * FAISS 向量存储测试。
 *
 * 注意：这些测试需要 FAISS 库和 JNI 绑定。
 * 使用 -Dfaiss.enabled=true 启用这些测试。
 *
 * 这些测试默认被禁用，因为它们需要外部依赖。
 * 要运行这些测试，请安装 FAISS 库和 JNI 绑定，然后使用以下命令：
 * ./gradlew :kastrax-rag:test --tests "ai.kastrax.rag.vectorstore.FaissVectorStoreTest" -Dfaiss.enabled=true
 */
@EnabledIfSystemProperty(named = "faiss.enabled", matches = "true")
class FaissVectorStoreTest {

    private lateinit var vectorStore: FaissVectorStore
    private val dimension = 4
    private val testDir = File("build/tmp/faiss-test")

    @BeforeEach
    fun setUp() {
        // 创建测试目录
        testDir.mkdirs()

        // 创建向量存储
        vectorStore = FaissVectorStore(dimension)
    }

    @AfterEach
    fun tearDown() {
        // 关闭向量存储
        vectorStore.close()

        // 清理测试目录
        testDir.deleteRecursively()
    }

    @Test
    fun `test add and search documents`() = runBlocking {
        // 创建测试文档
        val doc1 = Document("Document 1", mapOf("id" to 1))
        val doc2 = Document("Document 2", mapOf("id" to 2))
        val doc3 = Document("Document 3", mapOf("id" to 3))

        // 创建嵌入向量
        val embedding1 = Embedding(listOf(1.0f, 0.0f, 0.0f, 0.0f))
        val embedding2 = Embedding(listOf(0.0f, 1.0f, 0.0f, 0.0f))
        val embedding3 = Embedding(listOf(0.0f, 0.0f, 1.0f, 0.0f))

        // 创建嵌入文档
        val embeddedDoc1 = EmbeddedDocument(doc1, embedding1)
        val embeddedDoc2 = EmbeddedDocument(doc2, embedding2)
        val embeddedDoc3 = EmbeddedDocument(doc3, embedding3)

        // 添加文档
        val addedCount = vectorStore.addEmbeddedDocuments(listOf(embeddedDoc1, embeddedDoc2, embeddedDoc3))
        assertEquals(3, addedCount)

        // 搜索文档
        val queryEmbedding = Embedding(listOf(1.0f, 0.1f, 0.0f, 0.0f))
        val results = vectorStore.similaritySearch(queryEmbedding, 2, 0.0)

        // 验证结果
        assertEquals(2, results.size)
        assertEquals("Document 1", results[0].document.content)
        assertEquals("Document 2", results[1].document.content)
    }

    @Test
    fun `test save and load index`() = runBlocking {
        // 创建测试文档
        val doc1 = Document("Document 1", mapOf("id" to 1))
        val doc2 = Document("Document 2", mapOf("id" to 2))

        // 创建嵌入向量
        val embedding1 = Embedding(listOf(1.0f, 0.0f, 0.0f, 0.0f))
        val embedding2 = Embedding(listOf(0.0f, 1.0f, 0.0f, 0.0f))

        // 创建嵌入文档
        val embeddedDoc1 = EmbeddedDocument(doc1, embedding1)
        val embeddedDoc2 = EmbeddedDocument(doc2, embedding2)

        // 添加文档
        vectorStore.addEmbeddedDocuments(listOf(embeddedDoc1, embeddedDoc2))

        // 保存索引
        val indexFile = File(testDir, "test-index.faiss")
        vectorStore.saveIndex(indexFile.absolutePath)

        // 创建新的向量存储并加载索引
        val newVectorStore = FaissVectorStore(dimension)
        newVectorStore.loadIndex(indexFile.absolutePath)

        // 搜索文档
        val queryEmbedding = Embedding(listOf(1.0f, 0.1f, 0.0f, 0.0f))
        val results = newVectorStore.similaritySearch(queryEmbedding, 1, 0.0)

        // 验证结果
        assertEquals(1, results.size)
        assertEquals("Document 1", results[0].document.content)

        // 关闭新的向量存储
        newVectorStore.close()
    }

    @Test
    fun `test clear index`() = runBlocking {
        // 创建测试文档
        val doc1 = Document("Document 1", mapOf("id" to 1))
        val embedding1 = Embedding(listOf(1.0f, 0.0f, 0.0f, 0.0f))
        val embeddedDoc1 = EmbeddedDocument(doc1, embedding1)

        // 添加文档
        vectorStore.addEmbeddedDocuments(listOf(embeddedDoc1))
        assertEquals(1, vectorStore.count())

        // 清空索引
        vectorStore.clear()
        assertEquals(0, vectorStore.count())

        // 验证搜索结果为空
        val queryEmbedding = Embedding(listOf(1.0f, 0.0f, 0.0f, 0.0f))
        val results = vectorStore.similaritySearch(queryEmbedding, 1, 0.0)
        assertTrue(results.isEmpty())
    }
}
