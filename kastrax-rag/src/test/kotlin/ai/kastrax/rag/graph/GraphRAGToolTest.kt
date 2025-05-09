package ai.kastrax.rag.graph

import ai.kastrax.store.document.Document
import ai.kastrax.rag.document.DocumentLoader
import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.retrieval.SearchResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GraphRAGToolTest {

    private lateinit var documentLoader: DocumentLoader
    private lateinit var embeddingService: EmbeddingService
    private lateinit var graphRAGTool: GraphRAGTool

    @BeforeEach
    fun setup() {
        // 创建模拟的文档加载器
        documentLoader = mockk<DocumentLoader>()

        // 创建模拟的嵌入服务
        embeddingService = mockk<EmbeddingService>()
        every { embeddingService.dimension() } returns 3

        // 创建 GraphRAGTool
        graphRAGTool = GraphRAGTool(documentLoader, embeddingService)
    }

    @Test
    fun `test execute with documents`() = runBlocking {
        // 创建模拟文档
        val documents = listOf(
            Document(
                id = "1",
                content = "苹果是一种常见的水果",
                metadata = mapOf("source" to "水果百科")
            ),
            Document(
                id = "2",
                content = "香蕉是一种热带水果",
                metadata = mapOf("source" to "水果百科")
            ),
            Document(
                id = "3",
                content = "电脑是一种电子设备",
                metadata = mapOf("source" to "电子百科")
            )
        )

        // 创建模拟嵌入向量
        val embeddings = listOf(
            floatArrayOf(1.0f, 0.0f, 0.0f),
            floatArrayOf(0.8f, 0.2f, 0.0f),
            floatArrayOf(0.0f, 0.0f, 1.0f)
        )

        // 设置模拟行为
        coEvery { documentLoader.load() } returns documents
        coEvery { embeddingService.embed("苹果是一种常见的水果") } returns embeddings[0]
        coEvery { embeddingService.embed("香蕉是一种热带水果") } returns embeddings[1]
        coEvery { embeddingService.embed("电脑是一种电子设备") } returns embeddings[2]
        coEvery { embeddingService.embed("水果") } returns floatArrayOf(0.9f, 0.1f, 0.0f)

        // 执行查询
        val result = graphRAGTool.execute("水果")

        // 验证结果
        assertTrue(result is JsonPrimitive)
        assertTrue(result.content.contains("水果"))
    }

    @Test
    fun `test execute with no documents`() = runBlocking {
        // 设置模拟行为
        coEvery { documentLoader.load() } returns emptyList()

        // 执行查询
        val result = graphRAGTool.execute("水果")

        // 验证结果
        assertTrue(result is JsonPrimitive)
        assertTrue(result.content.contains("未找到任何文档"))
    }

    @Test
    fun `test execute with error`() = runBlocking {
        // 设置模拟行为
        coEvery { documentLoader.load() } throws RuntimeException("测试异常")

        // 执行查询
        val result = graphRAGTool.execute("水果")

        // 验证结果
        assertTrue(result is JsonPrimitive)
        assertTrue(result.content.contains("检索失败"))
    }
}
