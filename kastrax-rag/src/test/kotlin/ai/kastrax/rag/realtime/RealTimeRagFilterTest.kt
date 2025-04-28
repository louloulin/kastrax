package ai.kastrax.rag.realtime

import ai.kastrax.rag.document.Document
import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.rag.vectorstore.InMemoryVectorStore
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 测试 RealTimeRag 类中的过滤功能
 */
class RealTimeRagFilterTest {

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
    fun `test search with filter`() = runBlocking {
        // 准备测试数据
        val documents = listOf(
            Document(
                content = "苹果是一种常见的水果",
                metadata = mapOf("source" to "水果百科", "category" to "水果").mapValues { it.value.toString() }
            ),
            Document(
                content = "香蕉是一种热带水果",
                metadata = mapOf("source" to "水果百科", "category" to "水果").mapValues { it.value.toString() }
            ),
            Document(
                content = "电脑是一种电子设备",
                metadata = mapOf("source" to "电子百科", "category" to "电子").mapValues { it.value.toString() }
            )
        )

        // 添加文档
        documents.forEach { realTimeRag.addDocument(it) }
        delay(1000)

        // 使用过滤搜索
        val filteredResults = realTimeRag.search("水果", filterByQuery = true)
        
        // 验证结果
        assertTrue(filteredResults.isNotEmpty())
        assertTrue(filteredResults.all { it.document.content.contains("水果") })
        assertEquals(2, filteredResults.size)
        
        // 使用不过滤搜索
        val unfilteredResults = realTimeRag.search("水果", filterByQuery = false)
        
        // 验证结果
        assertTrue(unfilteredResults.isNotEmpty())
        // 不过滤时，可能包含不含"水果"的文档
        assertTrue(unfilteredResults.size >= filteredResults.size)
    }

    @Test
    fun `test generate context without filter`() = runBlocking {
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
        assertTrue(context.isNotEmpty())
        assertTrue(context.contains("维生素") || context.contains("纤维素") || context.contains("钾元素"))
    }

    @Test
    fun `test retrieve context without filter`() = runBlocking {
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
        assertEquals(2, result.sourceDocuments.size)
        assertTrue(result.context.isNotEmpty())
    }
}
