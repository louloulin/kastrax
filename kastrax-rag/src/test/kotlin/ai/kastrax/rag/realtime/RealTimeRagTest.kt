package ai.kastrax.rag.realtime

import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RealTimeRagTest {
    private lateinit var documentStore: DocumentVectorStore
    private lateinit var embeddingService: EmbeddingService
    private lateinit var reranker: IdentityReranker
    private lateinit var realTimeRag: RealTimeRag
    private lateinit var config: RealTimeRagConfig

    @BeforeEach
    fun setUp() {
        documentStore = mockk(relaxed = true)
        embeddingService = mockk(relaxed = true)
        reranker = mockk(relaxed = true)

        // 创建测试配置，使用较小的更新间隔以便测试
        config = RealTimeRagConfig(
            updateInterval = 50,
            streamingEnabled = true,
            maxBatchSize = 10,
            maxLatency = 100
        )

        realTimeRag = RealTimeRag(documentStore, embeddingService, reranker, config)
    }

    @Test
    fun `test add document`() = runBlocking {
        // 准备测试数据
        val document = Document(
            id = "test-id",
            content = "This is a test document",
            metadata = mapOf("key" to "value")
        )

        // 模拟嵌入服务
        coEvery { embeddingService.embed(any()) } returns FloatArray(3) { 0.1f }

        // 模拟文档存储
        coEvery { documentStore.addDocuments(any(), any()) } returns true

        // 启动实时 RAG
        realTimeRag.start()

        // 添加文档
        val result = realTimeRag.addDocument(document)

        // 验证结果
        assertTrue(result)

        // 等待处理完成
        kotlinx.coroutines.delay(100)

        // 验证调用
        coVerify { documentStore.addDocuments(any(), any()) }

        // 停止实时 RAG
        realTimeRag.stop()
    }

    @Test
    fun `test update document`() = runBlocking {
        // 准备测试数据
        val document = Document(
            id = "test-id",
            content = "This is an updated document",
            metadata = mapOf("key" to "updated-value")
        )

        // 模拟嵌入服务
        coEvery { embeddingService.embed(any()) } returns FloatArray(3) { 0.2f }

        // 模拟文档存储
        coEvery { documentStore.deleteDocuments(any()) } returns true
        coEvery { documentStore.addDocuments(any(), any()) } returns true

        // 启动实时 RAG
        realTimeRag.start()

        // 更新文档
        val result = realTimeRag.updateDocument(document)

        // 验证结果
        assertTrue(result)

        // 等待处理完成
        kotlinx.coroutines.delay(100)

        // 验证调用
        coVerify { documentStore.deleteDocuments(any()) }
        coVerify { documentStore.addDocuments(any(), any()) }

        // 停止实时 RAG
        realTimeRag.stop()
    }

    @Test
    fun `test delete document`() = runBlocking {
        // 准备测试数据
        val document = Document(
            id = "test-id",
            content = "This is a document to delete",
            metadata = mapOf("key" to "value")
        )

        // 模拟文档存储
        coEvery { documentStore.deleteDocuments(any()) } returns true

        // 启动实时 RAG
        realTimeRag.start()

        // 删除文档
        val result = realTimeRag.deleteDocument(document)

        // 验证结果
        assertTrue(result)

        // 等待处理完成
        kotlinx.coroutines.delay(100)

        // 验证调用
        coVerify { documentStore.deleteDocuments(any()) }

        // 停止实时 RAG
        realTimeRag.stop()
    }

    @Test
    fun `test search`() = runBlocking {
        // 准备测试数据
        val query = "test query"
        val document = Document(
            id = "test-id",
            content = "This is a test document",
            metadata = mapOf("key" to "value")
        )

        // 模拟嵌入服务
        coEvery { embeddingService.embed(any()) } returns FloatArray(3) { 0.3f }

        // 模拟文档存储
        coEvery { documentStore.similaritySearch(any(), any(), any()) } returns listOf(
            ai.kastrax.store.document.DocumentSearchResult(document, 0.9)
        )

        // 模拟 RAG 类
        coEvery { reranker.rerank(any(), any()) } returnsArgument 1

        // 执行搜索
        val results = realTimeRag.search(query, 5, 0.0)

        // 验证结果
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `test generate context`() = runBlocking {
        // 准备测试数据
        val query = "test query"
        val document = Document(
            id = "test-id",
            content = "This is a test document",
            metadata = mapOf("key" to "value")
        )

        // 模拟嵌入服务
        coEvery { embeddingService.embed(any()) } returns FloatArray(3) { 0.3f }

        // 模拟文档存储
        coEvery { documentStore.similaritySearch(any(), any(), any()) } returns listOf(
            ai.kastrax.store.document.DocumentSearchResult(document, 0.9)
        )

        // 模拟 RAG 类
        coEvery { reranker.rerank(any(), any()) } returnsArgument 1

        // 执行上下文生成
        val context = realTimeRag.generateContext(query, 5, 0.0)

        // 验证结果
        assertNotNull(context)
    }

    @Test
    fun `test retrieve context`() = runBlocking {
        // 准备测试数据
        val query = "test query"
        val document = Document(
            id = "test-id",
            content = "This is a test document",
            metadata = mapOf("key" to "value")
        )

        // 模拟嵌入服务
        coEvery { embeddingService.embed(any()) } returns FloatArray(3) { 0.3f }

        // 模拟文档存储
        coEvery { documentStore.similaritySearch(any(), any(), any()) } returns listOf(
            ai.kastrax.store.document.DocumentSearchResult(document, 0.9)
        )

        // 模拟 RAG 类
        coEvery { reranker.rerank(any(), any()) } returnsArgument 1

        // 执行上下文检索
        val result = realTimeRag.retrieveContext(query, 5, 0.0)

        // 验证结果
        assertNotNull(result)
    }
}
