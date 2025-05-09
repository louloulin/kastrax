package ai.kastrax.rag.realtime

import ai.kastrax.rag.document.Document
import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.rag.vectorstore.InMemoryVectorStore
import ai.kastrax.rag.vectorstore.SearchResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class RealTimeRagTest {

    private lateinit var embeddingService: EmbeddingService
    private lateinit var vectorStore: InMemoryVectorStore
    private lateinit var realTimeRag: RealTimeRag

    @BeforeEach
    fun setup() = runBlocking {
        embeddingService = mockk<EmbeddingService>()
        vectorStore = InMemoryVectorStore()

        // 清空向量存储
        vectorStore.clear()

        // 配置嵌入服务模拟
        coEvery { embeddingService.embed(any<String>()) } returns floatArrayOf(1.0f, 0.0f, 0.0f)

        // 创建实时 RAG 实例
        realTimeRag = RealTimeRag(
            vectorStore = vectorStore,
            embeddingService = embeddingService,
            reranker = IdentityReranker(),
            config = RealTimeRagConfig(
                streamingEnabled = false,
                updateInterval = 50 // 50ms，用于测试
            )
        )

        // 停止先前的实例（如果有）
        realTimeRag.stop()

        // 启动实时 RAG 系统
        realTimeRag.start()
    }

    @Test
    fun `test add document`() = runBlocking {
        // 准备测试数据
        val document = Document(
            content = "这是一个测试文档",
            metadata = mapOf("source" to "test").mapValues { it.value.toString() }
        )

        // 添加文档
        val result = realTimeRag.addDocument(document)

        // 等待处理完成
        delay(1000)

        // 验证结果
        assertTrue(result)
        coVerify { embeddingService.embed(document.content) }

        // 验证文档已添加到向量存储
        val count = realTimeRag.size()
        assertEquals(1, count)
    }

    @Test
    fun `test update document`() = runBlocking {
        // 准备测试数据
        val document1 = Document(
            content = "这是原始文档",
            metadata = mapOf("source" to "test").mapValues { it.value.toString() }
        )
        val document2 = Document(
            content = "这是更新后的文档",
            metadata = mapOf("source" to "test", "updated" to "true").mapValues { it.value.toString() }
        )

        // 添加原始文档
        realTimeRag.addDocument(document1)
        delay(1000)

        // 更新文档
        val result = realTimeRag.updateDocument(document2)
        delay(1000)

        // 验证结果
        assertTrue(result)
        coVerify { embeddingService.embed(document2.content) }

        // 验证文档已更新
        val count = realTimeRag.size()
        assertEquals(1, count)

        // 验证可以检索到更新后的文档
        val searchResults = realTimeRag.search("更新")
        assertEquals(1, searchResults.size)
        // 检查文档内容和元数据
        assertEquals("这是更新后的文档", searchResults[0].document.content)
        assertEquals("true", searchResults[0].document.metadata["updated"])
    }

    @Test
    fun `test delete document`() = runBlocking {
        // 准备测试数据
        val document = Document(
            content = "这是一个测试文档",
            metadata = mapOf("source" to "test").mapValues { it.value.toString() }
        )

        // 添加文档
        realTimeRag.addDocument(document)
        delay(1000)

        // 验证文档已添加
        assertEquals(1, realTimeRag.size())

        // 删除文档
        val result = realTimeRag.deleteDocument(document)
        delay(1000)

        // 验证结果
        assertTrue(result)

        // 验证文档已删除
        assertEquals(0, realTimeRag.size())
    }

    @Test
    fun `test search`() = runBlocking {
        // 准备测试数据
        val documents = listOf(
            Document(
                content = "苹果是一种常见的水果",
                metadata = mapOf("source" to "test", "category" to "水果").mapValues { it.value.toString() }
            ),
            Document(
                content = "香蕉是一种热带水果",
                metadata = mapOf("source" to "test", "category" to "水果").mapValues { it.value.toString() }
            ),
            Document(
                content = "电脑是一种电子设备",
                metadata = mapOf("source" to "test", "category" to "电子").mapValues { it.value.toString() }
            )
        )

        // 模拟不同的嵌入向量
        coEvery { embeddingService.embed("苹果是一种常见的水果") } returns floatArrayOf(1.0f, 0.0f, 0.0f)
        coEvery { embeddingService.embed("香蕉是一种热带水果") } returns floatArrayOf(0.8f, 0.2f, 0.0f)
        coEvery { embeddingService.embed("电脑是一种电子设备") } returns floatArrayOf(0.0f, 0.0f, 1.0f)
        coEvery { embeddingService.embed("水果") } returns floatArrayOf(0.9f, 0.1f, 0.0f)
        coEvery { embeddingService.embed("电子") } returns floatArrayOf(0.1f, 0.1f, 0.8f)

        // 添加文档
        documents.forEach { realTimeRag.addDocument(it) }
        delay(1000)

        // 搜索水果相关文档
        val fruitResults = realTimeRag.search("水果", limit = 5)

        // 验证结果
        assertTrue(fruitResults.size >= 1)
        assertTrue(fruitResults.all { it.document.content.contains("水果") })

        // 搜索电子相关文档
        val electronicResults = realTimeRag.search("电子", limit = 5)

        // 验证结果
        assertTrue(electronicResults.size >= 1)
        assertTrue(electronicResults.all { it.document.content.contains("电子") })
    }

    @Test
    fun `test generate context`() = runBlocking {
        // 准备测试数据
        val documents = listOf(
            Document(
                content = "苹果是一种常见的水果，富含维生素和纤维素。",
                metadata = mapOf("source" to "水果百科", "category" to "水果").mapValues { it.value.toString() }
            ),
            Document(
                content = "香蕉是一种热带水果，富含钾元素，对心脏健康有益。",
                metadata = mapOf("source" to "水果百科", "category" to "水果").mapValues { it.value.toString() }
            )
        )

        // 模拟不同的嵌入向量
        coEvery { embeddingService.embed("苹果是一种常见的水果，富含维生素和纤维素。") } returns floatArrayOf(1.0f, 0.0f, 0.0f)
        coEvery { embeddingService.embed("香蕉是一种热带水果，富含钾元素，对心脏健康有益。") } returns floatArrayOf(0.8f, 0.2f, 0.0f)
        coEvery { embeddingService.embed("水果的营养价值") } returns floatArrayOf(0.9f, 0.1f, 0.0f)

        // 添加文档
        documents.forEach { realTimeRag.addDocument(it) }
        delay(1000)

        // 生成上下文
        val context = realTimeRag.generateContext("水果的营养价值")

        // 验证结果
        assertNotNull(context)
        assertTrue(context.isNotEmpty())
        assertTrue(context.contains("维生素") || context.contains("纤维素") || context.contains("钾元素"))
    }

    @Test
    fun `test retrieve context`() = runBlocking {
        // 准备测试数据
        val documents = listOf(
            Document(
                content = "苹果是一种常见的水果，富含维生素和纤维素。",
                metadata = mapOf("source" to "水果百科", "category" to "水果").mapValues { it.value.toString() }
            ),
            Document(
                content = "香蕉是一种热带水果，富含钾元素，对心脏健康有益。",
                metadata = mapOf("source" to "水果百科", "category" to "水果").mapValues { it.value.toString() }
            )
        )

        // 模拟不同的嵌入向量
        coEvery { embeddingService.embed("苹果是一种常见的水果，富含维生素和纤维素。") } returns floatArrayOf(1.0f, 0.0f, 0.0f)
        coEvery { embeddingService.embed("香蕉是一种热带水果，富含钾元素，对心脏健康有益。") } returns floatArrayOf(0.8f, 0.2f, 0.0f)
        coEvery { embeddingService.embed("水果的营养价值") } returns floatArrayOf(0.9f, 0.1f, 0.0f)

        // 添加文档
        documents.forEach { realTimeRag.addDocument(it) }
        delay(1000)

        // 检索上下文
        val result = realTimeRag.retrieveContext("水果的营养价值")

        // 验证结果
        assertNotNull(result)
        assertEquals(2, result.sourceDocuments.size)
        assertNotNull(result.context)
        assertTrue(result.context.isNotEmpty())
    }
}
