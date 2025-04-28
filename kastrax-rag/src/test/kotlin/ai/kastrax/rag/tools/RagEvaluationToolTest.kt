package ai.kastrax.rag.tools

import ai.kastrax.rag.metrics.MetricResult
import ai.kastrax.rag.metrics.rag.*
import ai.kastrax.rag.llm.LlmClient
import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.vectorstore.Document
import ai.kastrax.rag.vectorstore.RagVectorStore
import ai.kastrax.rag.vectorstore.SearchResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RagEvaluationToolTest {

    private lateinit var mockRag: RAG
    private lateinit var mockVectorStore: RagVectorStore
    private lateinit var mockEmbeddingService: EmbeddingService
    private lateinit var mockMetric: RagMetric
    private lateinit var evaluationTool: RagEvaluationTool

    @BeforeEach
    fun setUp() {
        mockVectorStore = mockk()
        mockEmbeddingService = mockk()
        mockRag = mockk()
        mockMetric = mockk()

        // 设置 mock 行为
        coEvery { mockRag.search(any(), any(), any(), any()) } returns listOf(
            SearchResult(Document("这是测试文档内容", mapOf("source" to "test.txt")), 0.8)
        )

        coEvery { mockRag.generateContext(any(), any(), any(), any()) } returns "这是测试上下文"

        coEvery { mockMetric.calculate(any(), any(), any()) } returns MetricResult(
            score = 0.75,
            details = mapOf("reason" to "测试原因")
        )

        // 创建评估工具
        evaluationTool = RagEvaluationTool(mockRag, null, listOf(mockMetric))
    }

    @Test
    fun `test evaluate`() = runBlocking {
        // 执行评估
        val result = evaluationTool.evaluate(
            query = "测试查询",
            answer = "测试回答",
            options = RagProcessOptions(),
            groundTruth = "参考答案"
        )

        // 验证结果
        assertNotNull(result)
        assertEquals("测试查询", result.query)
        assertEquals("测试回答", result.answer)
        assertEquals("这是测试上下文", result.context)
        assertEquals(0.75, result.overallScore)
        assertEquals(1, result.metricResults.size)
    }

    @Test
    fun `test generateReport`() {
        // 创建评估结果
        val result = RagEvaluationResult(
            query = "测试查询",
            answer = "测试回答",
            context = "测试上下文",
            retrievalResults = listOf(
                RetrievalResult("测试文档内容", 0.8, mapOf("source" to "test.txt"))
            ),
            metricResults = mapOf(
                "TestMetric" to MetricResult(0.75, mapOf("reason" to "测试原因"))
            ),
            overallScore = 0.75
        )

        // 生成报告
        val report = evaluationTool.generateReport(result)

        // 验证报告
        assertNotNull(report)
        assert(report.contains("RAG 评估报告"))
        assert(report.contains("总体分数: 0.75"))
        assert(report.contains("测试查询"))
        assert(report.contains("测试回答"))
    }

    @Test
    fun `test defaultMetrics`() {
        // 获取默认指标
        val metrics = RagEvaluationTool.defaultMetrics()

        // 验证默认指标
        assertEquals(4, metrics.size)
        assert(metrics[0] is RetrievalPrecisionMetric)
        assert(metrics[1] is ContextRelevanceMetric)
        assert(metrics[2] is AnswerQualityMetric)
        assert(metrics[3] is HallucinationMetric)
    }
}
